/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
 package org.eclipse.jdt.internal.ui.dialogs;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;

import org.eclipse.core.resources.IWorkspaceRunnable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.dialogs.SelectionStatusDialog;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.TypeInfo;
import org.eclipse.jdt.internal.corext.util.TypeInfoHistory;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class TypeSelectionDialog2 extends SelectionStatusDialog {

	private boolean fMultipleSelection;
	private IRunnableContext fRunnableContext;
	private IJavaSearchScope fScope;
	private int fElementKind;
	
	private String fInitialFilter;
	private ISelectionStatusValidator fValidator;
	private TypeSelectionComponent fContent;
	
	
	public TypeSelectionDialog2(Shell parent, boolean multi, IRunnableContext context, IJavaSearchScope scope, int elementKinds) {
		super(parent);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		fMultipleSelection= multi;
		fRunnableContext= context;
		fScope= (scope != null) ? scope : SearchEngine.createWorkspaceScope();
		fElementKind= elementKinds;
	}
	
	public void setFilter(String filter) {
		fInitialFilter= filter;
	}
	
	public void setValidator(ISelectionStatusValidator validator) {
		fValidator= validator;
	}
	
	protected TypeInfo[] getSelectedTypes() {
		if (fContent == null || fContent.isDisposed())
			return null;
		return fContent.getSelection();
	}
	
	public void create() {
		super.create();
		fContent.populate();
		getOkButton().setEnabled(fContent.getSelection().length > 0);
	}

	protected Control createDialogArea(Composite parent) {
		Composite area= (Composite)super.createDialogArea(parent);
		fContent= new TypeSelectionComponent(area, SWT.NONE, getMessage(), 
			fMultipleSelection, fScope, fElementKind, fInitialFilter);
		GridData gd= new GridData(GridData.FILL_BOTH);
		fContent.setLayoutData(gd);
		fContent.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				handleDefaultSelected(fContent.getSelection());
			}
			public void widgetSelected(SelectionEvent e) {
				handleWidgetSelected(fContent.getSelection());
			}
		});
		return area;
	}
	
	protected void handleDefaultSelected(TypeInfo[] selection) {
		okPressed();
	}
	
	protected void handleWidgetSelected(TypeInfo[] selection) {
		IStatus status;
		if (selection.length == 0) {
	    	status= new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR, "",null); //$NON-NLS-1$
	    } else {
		    if (fValidator != null) {
		    	status= fValidator.validate(selection);
		    } else {
		    	status= new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, "",null); //$NON-NLS-1$
		    }
	    }
    	updateStatus(status);
	}
	
	public int open() {
		try {
			ensureConsistency();
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, JavaUIMessages.TypeSelectionDialog_error3Title, JavaUIMessages.TypeSelectionDialog_error3Message); 
			return CANCEL;
		} catch (InterruptedException e) {
			// cancelled by user
			return CANCEL;
		}
		return super.open();
	}
	
	public boolean close() {
		TypeInfoHistory.getInstance().save();
		return super.close();
	}
	
	protected void computeResult() {
		TypeInfo[] selected= fContent.getSelection();
		if (selected == null || selected.length == 0) {
			setResult(null);
			return;
		}
		
		TypeInfoHistory history= TypeInfoHistory.getInstance();
		List result= new ArrayList(selected.length);
		if (result != null) {
			for (int i= 0; i < selected.length; i++) {
				try {
					TypeInfo typeInfo= selected[i];
					history.accessed(typeInfo);
					IType type= typeInfo.resolveType(fScope);
					if (type == null) {
						String title= JavaUIMessages.TypeSelectionDialog_errorTitle; 
						String message= Messages.format(JavaUIMessages.TypeSelectionDialog_dialogMessage, typeInfo.getPath()); 
						MessageDialog.openError(getShell(), title, message);
						setResult(null);
					} else {
						result.add(type);
					}
				} catch (JavaModelException e) {
					String title= JavaUIMessages.MultiTypeSelectionDialog_errorTitle; 
					String message= JavaUIMessages.MultiTypeSelectionDialog_errorMessage; 
					ErrorDialog.openError(getShell(), title, message, e.getStatus());
				}
			}
		}
		setResult(result);
	}
	
	private void ensureConsistency() throws InvocationTargetException, InterruptedException {
		final ICompilationUnit[] primaryWorkingCopies= JavaCore.getWorkingCopies(null);
		IWorkspaceRunnable runnable= new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				monitor.beginTask("", IProgressMonitor.UNKNOWN); //$NON-NLS-1$
				monitor.setTaskName(JavaUIMessages.TypeSelectionDialog_progress_reconcling);
				for (int i= 0; i < primaryWorkingCopies.length; i++) {
					ICompilationUnit curr= primaryWorkingCopies[i];
					try {
						JavaModelUtil.reconcile(curr);
					} catch (JavaModelException e) {
						JavaPlugin.log(e);
					}
					if (monitor.isCanceled())
						throw new OperationCanceledException();
				}
				monitor.setTaskName(JavaUIMessages.TypeSelectionDialog_progress_consistency);
				TypeInfoHistory.getInstance().checkConsistency();
				monitor.done();
			}
		};
		ISchedulingRule[] rules= new ISchedulingRule[primaryWorkingCopies.length];
		for (int i= 0; i < primaryWorkingCopies.length; i++) {
			rules[i]= primaryWorkingCopies[i].getSchedulingRule();
		}
		MultiRule rule= new MultiRule(rules);
		IRunnableContext context= fRunnableContext != null 
			? fRunnableContext 
			: PlatformUI.getWorkbench().getProgressService();
		Job currentJob= Platform.getJobManager().currentJob();
		if (currentJob == null) {
			// no job so no rule
			context.run(true, true, new WorkbenchRunnableAdapter(runnable, rule));
		} else {
			ISchedulingRule currentRule= currentJob.getRule();
			if (currentRule == null) {
				// no rule so use computed rule
				context.run(true, true, new WorkbenchRunnableAdapter(runnable, rule));
			} else if (currentRule.contains(rule)) {
				// use the current rule and transfer it sinc it is wider than the computed
				// rule and the current ruls is already active.
				context.run(true, true, new WorkbenchRunnableAdapter(runnable, currentRule, true));
			} else {
				Assert.isTrue(false, "Current scheduling rule conflicts with rule for reconciling working copies"); //$NON-NLS-1$
			}
		}
	}
}