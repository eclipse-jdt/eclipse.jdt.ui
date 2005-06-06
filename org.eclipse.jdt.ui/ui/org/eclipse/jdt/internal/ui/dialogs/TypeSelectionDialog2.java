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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;

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
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.dialogs.SelectionStatusDialog;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;

import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.TypeInfo;
import org.eclipse.jdt.internal.corext.util.TypeInfoHistory;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class TypeSelectionDialog2 extends SelectionStatusDialog {

	private String fTitle;
	
	private boolean fMultipleSelection;
	private IRunnableContext fRunnableContext;
	private IJavaSearchScope fScope;
	private int fElementKind;
	
	private String fInitialFilter;
	private int fSelectionMode;
	private ISelectionStatusValidator fValidator;
	private TypeSelectionComponent fContent;
	
	public static final int NONE= TypeSelectionComponent.NONE;
	public static final int CARET_BEGINNING= TypeSelectionComponent.CARET_BEGINNING;
	public static final int FULL_SELECTION= TypeSelectionComponent.FULL_SELECTION;
	
	private static boolean fgFirstTime= true; 
	
	private class TitleLabel implements TypeSelectionComponent.ITitleLabel {
		public void setText(String text) {
			if (text == null || text.length() == 0) {
				getShell().setText(fTitle);
			} else {
				getShell().setText(Messages.format(
					JavaUIMessages.TypeSelectionDialog2_title_format,
					new String[] { fTitle, text}));
			}
		}
	}
	
	public TypeSelectionDialog2(Shell parent, boolean multi, IRunnableContext context, IJavaSearchScope scope, int elementKinds) {
		super(parent);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		fMultipleSelection= multi;
		fRunnableContext= context;
		fScope= scope;
		fElementKind= elementKinds;
		fSelectionMode= NONE;
	}
	
	public void setFilter(String filter) {
		setFilter(filter, FULL_SELECTION);
	}
	
	public void setFilter(String filter, int selectionMode) {
		fInitialFilter= filter;
		fSelectionMode= selectionMode;
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
		fContent.populate(fSelectionMode);
		getOkButton().setEnabled(fContent.getSelection().length > 0);
	}

	protected Control createDialogArea(Composite parent) {
		Composite area= (Composite)super.createDialogArea(parent);
		fContent= new TypeSelectionComponent(area, SWT.NONE, getMessage(), 
			fMultipleSelection, fScope, fElementKind, fInitialFilter,
			new TitleLabel());
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
		if (selection.length == 0)
			return;
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
	
	public void setTitle(String title) {
		super.setTitle(title);
		fTitle= title;
	}
	
	protected void computeResult() {
		TypeInfo[] selected= fContent.getSelection();
		if (selected == null || selected.length == 0) {
			setResult(null);
			return;
		}
		
		// If the scope is null then it got computed by the type selection component.
		if (fScope == null) {
			fScope= fContent.getScope();
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
		// we only have to ensure histroy consistency here since the search engine
		// takes care of working copies.
		IRunnableWithProgress runnable= new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				if (fgFirstTime) {
					// Join the initalize after load job.
					IJobManager manager= Platform.getJobManager();
					manager.join(JavaCore.PLUGIN_ID, monitor);
				}
				TypeInfoHistory history= TypeInfoHistory.getInstance();
				if (fgFirstTime || history.isEmpty()) {
					monitor.beginTask(JavaUIMessages.TypeSelectionDialog_progress_consistency, 100);
					refreshSearchIndices(new SubProgressMonitor(monitor, 90));
					history.checkConsistency(new SubProgressMonitor(monitor, 10));
					monitor.done();
					fgFirstTime= false;
				} else {
					history.checkConsistency(monitor);
				}
			}
			private void refreshSearchIndices(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					new SearchEngine().searchAllTypeNames(
						null, 
						// make sure we search a concrete name. This is faster according to Kent  
						"_______________".toCharArray(), //$NON-NLS-1$
						SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE, 
						IJavaSearchConstants.ENUM,
						SearchEngine.createWorkspaceScope(), 
						new TypeNameRequestor() {}, 
						IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, 
						monitor);
				} catch (JavaModelException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
		IRunnableContext context= fRunnableContext != null 
			? fRunnableContext 
			: PlatformUI.getWorkbench().getProgressService();
		context.run(true, true, runnable);
	}
}