/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards;

import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;

/**
 * 
 */
public class ChangeComplianceDialog extends MessageDialog {

	private SelectionButtonDialogField fChangeProject;
	private SelectionButtonDialogField fChangeWorkspace;
	private final IJavaProject fProject;

	public ChangeComplianceDialog(Shell parentShell, IJavaProject project) {
		super(parentShell, getTitle(), null, getMessage(), QUESTION, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL }, 0);
		fProject= project;
		fChangeProject= new SelectionButtonDialogField(SWT.RADIO);
		fChangeProject.setLabelText(NewWizardMessages.getString("ChangeComplianceDialog.project.selection"));  //$NON-NLS-1$
		
		fChangeWorkspace= new SelectionButtonDialogField(SWT.RADIO);
		fChangeWorkspace.setLabelText(NewWizardMessages.getString("ChangeComplianceDialog.workspace.selection"));  //$NON-NLS-1$
	}
	
	private static String getTitle() {
		return NewWizardMessages.getString("ChangeComplianceDialog.title");  //$NON-NLS-1$
	}
	
	private static String getMessage() {
		return NewWizardMessages.getString("ChangeComplianceDialog.message");  //$NON-NLS-1$
	}
	

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.MessageDialog#createCustomArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createCustomArea(Composite parent) {
		GridLayout layout= new GridLayout();
		
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(layout);
		
		fChangeProject.doFillIntoGrid(composite, 1);
		fChangeWorkspace.doFillIntoGrid(composite, 1);
		return composite;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#open()
	 */
	public int open() {
		int res= super.open();
		if (res == OK) {
			if (fChangeProject.isSelected()) {
				Map map= fProject.getOptions(false);
				set50Compiliance(map);
				fProject.setOptions(map);
			} else {
				Hashtable map= JavaCore.getOptions();
				set50Compiliance(map);
				JavaCore.setOptions(map);
			}
			doFullBuild();
			
		}
		return res;
	}

	private void set50Compiliance(Map map) {
		map.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_5);
		map.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_5);
		map.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_5);
		map.put(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
	}
	
	private boolean doFullBuild() {
		
		Job buildJob = new Job(NewWizardMessages.getString("ChangeComplianceDialog.job.title")){  //$NON-NLS-1$
			/* (non-Javadoc)
			 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
			 */
			protected IStatus run(IProgressMonitor monitor) {
				try {
					if (fChangeProject.isSelected()) {
						monitor.beginTask(NewWizardMessages.getFormattedString("ChangeComplianceDialog.buildproject.taskname", fProject.getElementName()), 2); //$NON-NLS-1$
						fProject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, new SubProgressMonitor(monitor,1));
						JavaPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new SubProgressMonitor(monitor,1));
					} else {
						monitor.beginTask(NewWizardMessages.getString("ChangeComplianceDialog.buildall.taskname"), 2); //$NON-NLS-1$
						JavaPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, new SubProgressMonitor(monitor, 2));
					}
				} catch (CoreException e) {
					return e.getStatus();
				} catch (OperationCanceledException e) {
					return Status.CANCEL_STATUS;
				}
				finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
			public boolean belongsTo(Object family) {
				return ResourcesPlugin.FAMILY_MANUAL_BUILD == family;
			}
		};
		
		buildJob.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
		buildJob.setUser(true); 
		buildJob.schedule();
		return true;
	}	
	

}
