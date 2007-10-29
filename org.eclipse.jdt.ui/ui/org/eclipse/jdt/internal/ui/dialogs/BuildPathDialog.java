/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.core.resources.IWorkspaceRunnable;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.preferences.PreferencesMessages;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;

public class BuildPathDialog extends StatusDialog {

	private IJavaProject fProject;
	private BuildPathsBlock fBlock;

	public BuildPathDialog(Shell parent, IJavaProject project) {
		super(parent);
		Assert.isNotNull(project);
		fProject= project;
	}

	/*
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 * @since 3.4
	 */
	protected boolean isResizable() {
		return true;
	}

	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(Messages.format(JavaUIMessages.BuildPathDialog_title, fProject.getElementName())); 
	}

	protected Control createDialogArea(Composite parent) {
		IStatusChangeListener listener = new IStatusChangeListener() {
			public void statusChanged(IStatus status) {
				updateStatus(status);
			}
		};
		Composite result= (Composite)super.createDialogArea(parent);
		fBlock= new BuildPathsBlock(new BusyIndicatorRunnableContext(), listener, 0, false, null);
		fBlock.init(fProject, null, null);
		fBlock.createControl(result).setLayoutData(new GridData(GridData.FILL_BOTH));
		applyDialogFont(result);		
		return result;
	}

	protected void buttonPressed(int buttonId) {
		try {
			if (buttonId == IDialogConstants.OK_ID) {
				configureBuildPath();
			}
			super.buttonPressed(buttonId);
		} finally {
			fBlock.dispose();
		}
	}

	private void configureBuildPath() {
		Shell shell= getShell();
		IWorkspaceRunnable runnable= new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor)	throws CoreException, OperationCanceledException {
				fBlock.configureJavaProject(monitor); 
			}
		};
		IRunnableWithProgress op= new WorkbenchRunnableAdapter(runnable); // lock on root
		try {
			PlatformUI.getWorkbench().getProgressService().run(true, true, op);
		} catch (InvocationTargetException e) {
			String title= PreferencesMessages.BuildPathDialog_error_title; 
			String message= PreferencesMessages.BuildPathDialog_error_message; 
			ExceptionHandler.handle(e, shell, title, message);
		} catch (InterruptedException e) {
			// cancelled
		}
	}
}
