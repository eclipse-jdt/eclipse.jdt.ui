/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;

import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * As addition to the JavaCapabilityConfigurationPage, the wizard does an
 * early project creation (so that linked folders can be defined) and, if an
 * existing external location was specified, offers to do a classpath detection
 */
public class NewProjectCreationWizardPage extends JavaCapabilityConfigurationPage {

	private WizardNewProjectCreationPage fMainPage;

	private IPath fCurrProjectLocation;
	private IProject fCurrProject;	

	/**
	 * Constructor for NewProjectCreationWizardPage.
	 */
	public NewProjectCreationWizardPage(WizardNewProjectCreationPage mainPage) {
		super();
		fMainPage= mainPage;
		fCurrProjectLocation= null;
		fCurrProject= null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		if (visible) {
			changeToNewProject();
		}
		super.setVisible(visible);
	}
	
	private void changeToNewProject() {
		IProject newProjectHandle= fMainPage.getProjectHandle();
		IPath newProjectLocation= fMainPage.getLocationPath();
		
		if (newProjectHandle.equals(fCurrProject) && newProjectLocation.equals(fCurrProjectLocation)) {
			return; // no changes
		}		
		
		IRunnableWithProgress op= new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					updateProject(monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} 
			}
		};
	
		try {
			getContainer().run(false, true, op);
		} catch (InvocationTargetException e) {
			String title= NewWizardMessages.getString("NewProjectCreationWizardPage.EarlyCreationOperation.error.title"); //$NON-NLS-1$
			String message= NewWizardMessages.getString("NewProjectCreationWizardPage.EarlyCreationOperation.error.desc");			 //$NON-NLS-1$
			ExceptionHandler.handle(e, getShell(), title, message);
		} catch  (InterruptedException e) {
			// cancel pressed
		}
	}
	
	protected void updateProject(IProgressMonitor monitor) throws CoreException, InterruptedException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		try {
			monitor.beginTask(NewWizardMessages.getString("NewProjectCreationWizardPage.EarlyCreationOperation.desc"), 3);				 //$NON-NLS-1$
				
			if (fCurrProject != null) { // remove existing project
				fCurrProject.delete(false, false, new SubProgressMonitor(monitor, 1));
			} else {
				monitor.worked(1);
			}
			fCurrProject= fMainPage.getProjectHandle();
			fCurrProjectLocation= fMainPage.getLocationPath();			
				
			createProject(fCurrProject, fCurrProjectLocation, new SubProgressMonitor(monitor, 1));
			
			IClasspathEntry[] entries= null;
			IPath outputLocation= null;

			if (fCurrProjectLocation.toFile().exists() && !Platform.getLocation().equals(fCurrProjectLocation)) {
				// detect classpath
				if (!fCurrProject.getFile(".classpath").exists()) { //$NON-NLS-1$
					// if .classpath exists noneed to look for files
					ClassPathDetector detector= new ClassPathDetector(fCurrProject);
					entries= detector.getClasspath();
					outputLocation= detector.getOutputLocation();
				}
			}				
			init(JavaCore.create(fCurrProject), outputLocation, entries, false);
			monitor.worked(1);
	
		} finally {
			monitor.done();
		}
	}
		
	/**
	 * Called from the wizard on cancel.
	 */
	public void performCancel() {
		if (fCurrProject != null) {
			try {
				fCurrProject.delete(false, false, null);
			} catch (CoreException e) {
				String title= NewWizardMessages.getString("NewProjectCreationWizardPage.op_error.title"); //$NON-NLS-1$
				String message= NewWizardMessages.getString("NewProjectCreationWizardPage.op_error_remove.message");			 //$NON-NLS-1$
				ExceptionHandler.handle(e, getShell(), title, message);				
			}
		}
	}
}
