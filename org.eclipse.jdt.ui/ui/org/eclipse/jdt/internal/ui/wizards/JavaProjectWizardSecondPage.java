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
package org.eclipse.jdt.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;

import org.eclipse.jdt.internal.ui.util.CoreUtility;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * As addition to the JavaCapabilityConfigurationPage, the wizard does an
 * early project creation (so that linked folders can be defined) and, if an
 * existing external location was specified, offers to do a classpath detection
 */
public class JavaProjectWizardSecondPage extends JavaCapabilityConfigurationPage {

	private final JavaProjectWizardFirstPage fFirstPage;

	protected IPath fCurrProjectLocation;
	protected IProject fCurrProject;
	
	protected boolean fCanRemoveContent;

	/**
	 * Constructor for NewProjectCreationWizardPage.
	 */
	public JavaProjectWizardSecondPage(JavaProjectWizardFirstPage mainPage) {
		super();
		fFirstPage= mainPage;
		fCurrProjectLocation= null;
		fCurrProject= null;
		fCanRemoveContent= false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		if (visible) {
			changeToNewProject();
		} else {
			removeProject();
		}
		super.setVisible(visible);
	}
	
	private void changeToNewProject() {
		final IProject newProjectHandle= fFirstPage.getProjectHandle();
		final IPath newProjectLocation= fFirstPage.getLocationPath();
		
		fCanRemoveContent= !fFirstPage.getDetect();
		
		final boolean initialize= !(newProjectHandle.equals(fCurrProject) && newProjectLocation.equals(fCurrProjectLocation));
		
		final IRunnableWithProgress op= new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					updateProject(initialize, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} 
			}
		};
	
		try {
			getContainer().run(false, true, new WorkspaceModifyDelegatingOperation(op, newProjectHandle));
		} catch (InvocationTargetException e) {
			final String title= NewWizardMessages.getString("JavaProjectWizardSecondPage.error.title"); //$NON-NLS-1$
			final String message= NewWizardMessages.getString("JavaProjectWizardSecondPage.error.message"); //$NON-NLS-1$
			ExceptionHandler.handle(e, getShell(), title, message);
		} catch  (InterruptedException e) {
			// cancel pressed
		}
	}
	
	protected void updateProject(boolean initialize, IProgressMonitor monitor) throws CoreException {
		
		fCurrProject= fFirstPage.getProjectHandle();
		fCurrProjectLocation= fFirstPage.getLocationPath();
		
		final boolean noProgressMonitor= !initialize && !fFirstPage.getDetect();
		
		if (monitor == null || noProgressMonitor ) {
			monitor= new NullProgressMonitor();
		}
		try {
			monitor.beginTask(NewWizardMessages.getString("JavaProjectWizardSecondPage.operation.initialize"), 2); //$NON-NLS-1$
			
			createProject(fCurrProject, fCurrProjectLocation, new SubProgressMonitor(monitor, 1));
			if (initialize) {
				
				IClasspathEntry[] entries= null;
				IPath outputLocation= null;
		
				if (fFirstPage.getDetect()) {
					if (!fCurrProject.getFile(".classpath").exists()) { //$NON-NLS-1$
						final ClassPathDetector detector= new ClassPathDetector(fCurrProject);
						entries= detector.getClasspath();
						outputLocation= detector.getOutputLocation();
					}
				} else if (fFirstPage.isSrcBin()) {
					IPreferenceStore store= PreferenceConstants.getPreferenceStore();
					IPath srcPath= new Path(store.getString(PreferenceConstants.SRCBIN_SRCNAME));
					IPath binPath= new Path(store.getString(PreferenceConstants.SRCBIN_BINNAME));
					
					if (srcPath.segmentCount() > 0) {
						IFolder folder= fCurrProject.getFolder(srcPath);
						CoreUtility.createFolder(folder, true, true, null);
					}
					
					if (binPath.segmentCount() > 0 && !binPath.equals(srcPath)) {
						IFolder folder= fCurrProject.getFolder(binPath);
						CoreUtility.createFolder(folder, true, true, null);
					}
					
					final IPath projectPath= fCurrProject.getFullPath();

					// configure the classpath entries, including the default jre library.
					final List cpEntries= new ArrayList();
					cpEntries.add(JavaCore.newSourceEntry(projectPath.append(srcPath)));
					cpEntries.addAll(Arrays.asList(PreferenceConstants.getDefaultJRELibrary()));
					entries= new IClasspathEntry[cpEntries.size()];
					cpEntries.toArray(entries);
					
					// configure the output location
					outputLocation= projectPath.append(binPath);
				}
				init(JavaCore.create(fCurrProject), outputLocation, entries, false);
			}
			monitor.worked(1);
		} finally {
			monitor.done();
		}
	}
	
	/**
	 * Called from the wizard on finish.
	 */
	public void performFinish(IProgressMonitor monitor) throws CoreException, InterruptedException {
		try {
			monitor.beginTask(NewWizardMessages.getString("JavaProjectWizardSecondPage.operation.create"), 3); //$NON-NLS-1$
			if (fCurrProject == null) {
				updateProject(true, new SubProgressMonitor(monitor, 1));
			}
			configureJavaProject(new SubProgressMonitor(monitor, 2));
		} finally {
			monitor.done();
			fCurrProject= null;
		}
	}

	private void removeProject() {
		if (fCurrProject == null || !fCurrProject.exists()) {
			return;
		}
		
		IRunnableWithProgress op= new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {

				final boolean noProgressMonitor= Platform.getInstanceLocation().equals(fCurrProjectLocation);

				if (monitor == null || noProgressMonitor) {
					monitor= new NullProgressMonitor();
				}

				monitor.beginTask(NewWizardMessages.getString("JavaProjectWizardSecondPage.operation.remove"), 3); //$NON-NLS-1$

				try {
					fCurrProject.delete(fCanRemoveContent, false, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
					fCurrProject= null;
					fCanRemoveContent= false;
				}
			}
		};
	
		try {
			getContainer().run(false, true, new WorkspaceModifyDelegatingOperation(op, fCurrProject));
		} catch (InvocationTargetException e) {
			final String title= NewWizardMessages.getString("JavaProjectWizardSecondPage.error.remove.title"); //$NON-NLS-1$
			final String message= NewWizardMessages.getString("JavaProjectWizardSecondPage.error.remove.message"); //$NON-NLS-1$
			ExceptionHandler.handle(e, getShell(), title, message);		
		} catch  (InterruptedException e) {
			// cancel pressed
		}
	}		
			
	/**
	 * Called from the wizard on cancel.
	 */
	public void performCancel() {
		removeProject();
	}
}
