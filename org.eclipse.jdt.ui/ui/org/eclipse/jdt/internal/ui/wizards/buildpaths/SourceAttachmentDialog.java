/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

/**
 * A dialog to configure the source attachment of a library (class folder, JAR
 * and zip archive).
 *
 */
public class SourceAttachmentDialog extends StatusDialog {
	
	private SourceAttachmentBlock fSourceAttachmentBlock;
	private boolean fApplyChanges;

	/**
	 * Creates an instance of the SourceAttachmentDialog. After
	 * <code>open</code>, the edited paths can be access with
	 * <code>getSourceAttachmentPath</code> and
	 * <code>getSourceAttachmentRootPath</code>. 
	 * @param parent Parent shell for the dialog
	 * @param entry The entry to edit
	 * @param containerPath Path of the container that contains the given entry or
	 * <code>null</code> if the entry does not belong to a container.
	 * @param project Project to which the entry belongs. Can be
	 * <code>null</code> if <code>applyChanges</code> is false and the entry
	 * does not belong to a container.
	 * @param applyChanges If set to <code>true</code>, changes are applied on
	 * OK. If set to false, no changes are commited. When changes are applied,
	 * classpath entries which are not found on the classpath will be added as
	 * new libraries.
	 */
	public SourceAttachmentDialog(Shell parent, IClasspathEntry entry, IPath containerPath, IJavaProject project, boolean applyChanges) {
		super(parent);
		fApplyChanges= applyChanges;

		IStatusChangeListener listener= new IStatusChangeListener() {
			public void statusChanged(IStatus status) {
				updateStatus(status);
			}
		};		
		fSourceAttachmentBlock= new SourceAttachmentBlock(listener, entry, containerPath, project);			
	
		setTitle(NewWizardMessages.getString("SourceAttachmentDialog.title")); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		WorkbenchHelp.setHelp(newShell, IJavaHelpContextIds.SOURCE_ATTACHMENT_DIALOG);
	}		
			
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite) super.createDialogArea(parent);
			
		Control inner= createSourceAttachmentControls(composite);
		inner.setLayoutData(new GridData(GridData.FILL_BOTH));			
		applyDialogFont(composite);		
		return composite;
	}

	/**
	 * Creates the controls for the source attachment configuration.
	 */	
	protected Control createSourceAttachmentControls(Composite composite) {
		return fSourceAttachmentBlock.createControl(composite);
	}
	
	
	/**
	 * Returns the configured source attachment path.
	 */
	public IPath getSourceAttachmentPath() {
		return fSourceAttachmentBlock.getSourceAttachmentPath();
	}

	/**
	 * Returns the configured source attachment path root. Sonce 2.1 source
	 * attachment roots are autodetected. The value returned is therefore always
	 * null.
	 */	
	public IPath getSourceAttachmentRootPath() {
		return fSourceAttachmentBlock.getSourceAttachmentRootPath();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		super.okPressed();
		if (fApplyChanges) {
			try {
				IRunnableWithProgress runnable= getRunnable();
				new ProgressMonitorDialog(getShell()).run(true, true, runnable);						
	
			} catch (InvocationTargetException e) {
				String title= NewWizardMessages.getString("SourceAttachmentDialog.error.title"); //$NON-NLS-1$
				String message= NewWizardMessages.getString("SourceAttachmentDialog.error.message"); //$NON-NLS-1$
				ExceptionHandler.handle(e, getShell(), title, message);
	
			} catch (InterruptedException e) {
				// cancelled
			}
		}
	}
	
	/**
	 * Creates the runnable that configures the project with the new source
	 * attachements.
     */
	protected IRunnableWithProgress getRunnable() {
		return fSourceAttachmentBlock.getRunnable(getShell());
	}

	/**
	 * Helper method that tests if an classpath entry can be found in a
	 * container. <code>null</code> is returned if the entry can not be found
	 * or if the container does not allows the configuration of source
	 * attachments
	 * @param jproject The container's parent project
	 * @param containerPath The path of the container
	 * @param libPath The path of the bibrary to be found
	 * @return IClasspathEntry A classpath entry from the container of
	 * <code>null</code> if the container can not be modified.
     */
	public static IClasspathEntry getClasspathEntryToEdit(IJavaProject jproject, IPath containerPath, IPath libPath) throws JavaModelException {
		IClasspathContainer container= JavaCore.getClasspathContainer(containerPath, jproject);
		ClasspathContainerInitializer initializer= JavaCore.getClasspathContainerInitializer(containerPath.segment(0));
		if (container != null && initializer != null && initializer.canUpdateClasspathContainer(containerPath, jproject)) {
			IClasspathEntry[] entries= container.getClasspathEntries();
			for (int i= 0; i < entries.length; i++) {
				IClasspathEntry curr= entries[i];
				IClasspathEntry resolved= JavaCore.getResolvedClasspathEntry(curr);
				if (resolved != null && libPath.equals(resolved.getPath())) {
					return curr; // return the real entry
				}
			}
		}
		return null; // attachment not possible
	}	
	
	
}	