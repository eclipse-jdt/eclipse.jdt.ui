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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;

/**
 * As addition to the JavaCapabilityConfigurationPage, the wizard checks if an existing external
 * location was specified and offers to do an early project creation so that classpath can be detected.
 */
public class NewProjectCreationWizardPage extends JavaCapabilityConfigurationPage {

	private WizardNewProjectCreationPage fMainPage;
	private IPath fCurrProjectLocation;
	private boolean fProjectCreated;

	/**
	 * Constructor for ProjectWizardPage.
	 */
	public NewProjectCreationWizardPage(WizardNewProjectCreationPage mainPage) {
		super();
		fMainPage= mainPage;
		fCurrProjectLocation= fMainPage.getLocationPath();
		fProjectCreated= false;
	}
	
	private boolean canDetectExistingClassPath(IPath projLocation) {
		return projLocation.toFile().exists() && !Platform.getLocation().equals(projLocation);
	}
	
	private void update() {
		IPath projLocation= fMainPage.getLocationPath();
		if (!projLocation.equals(fCurrProjectLocation) && canDetectExistingClassPath(projLocation)) {
			String title= NewWizardMessages.getString("NewProjectCreationWizardPage.EarlyCreationDialog.title"); //$NON-NLS-1$
			String description= NewWizardMessages.getString("NewProjectCreationWizardPage.EarlyCreationDialog.description"); //$NON-NLS-1$
			if (MessageDialog.openQuestion(getShell(), title, description)) {
				createAndDetect();
			}
		}
					
		fCurrProjectLocation= projLocation;
				
		IJavaProject prevProject= getJavaProject();
		IProject currProject= fMainPage.getProjectHandle();
		if ((prevProject == null) || !currProject.equals(prevProject.getProject())) {
			init(JavaCore.create(currProject), null, null, false);
		}		
	}

	private void createAndDetect() {
		IRunnableWithProgress op= new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				if (monitor == null)
					monitor= new NullProgressMonitor();		
		
				monitor.beginTask(NewWizardMessages.getString("NewProjectCreationWizardPage.EarlyCreationOperation.desc"), 3);				 //$NON-NLS-1$
				try {
					BuildPathsBlock.createProject(fMainPage.getProjectHandle(), fMainPage.getLocationPath(), new SubProgressMonitor(monitor, 1));
					fProjectCreated= true;					
					initFromExistingStructures(new SubProgressMonitor(monitor, 2));						
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
		
	/* (non-Javadoc)
	 * @see IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		if (visible) {
			update();
		}
		super.setVisible(visible);
	}
	
	/* (non-Javadoc)
	 * @see IWizardPage#getPreviousPage()
	 */
	public IWizardPage getPreviousPage() {
		if (fProjectCreated) {
			return null;
		}
		return super.getPreviousPage();
	}
		
	public void createProject(IProgressMonitor monitor) throws CoreException, InterruptedException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		try {		
			monitor.beginTask(NewWizardMessages.getString("NewProjectCreationWizardPage.NormalCreationOperation.desc"), 4); //$NON-NLS-1$
			BuildPathsBlock.createProject(fMainPage.getProjectHandle(), fMainPage.getLocationPath(), new SubProgressMonitor(monitor, 1));
			if (getJavaProject() == null) {
				initFromExistingStructures(new SubProgressMonitor(monitor, 1));
			} else {
				monitor.worked(1);
			}
			configureJavaProject(new SubProgressMonitor(monitor, 2));
		} finally {
			monitor.done();
		}
	}
	
	private void initFromExistingStructures(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(NewWizardMessages.getString("NewProjectCreationWizardPage.DetectingClasspathOperation.desc"), 2); //$NON-NLS-1$
		try {
			IProject project= fMainPage.getProjectHandle();
			
			if (project.getFile(".classpath").exists()) { //$NON-NLS-1$
				init(JavaCore.create(project), null, null, false);
				monitor.worked(2);
			} else{
				ClassPathDetector detector= new ClassPathDetector(project);
				IClasspathEntry[] entries= detector.getClasspath();
				IPath outputLocation= detector.getOutputLocation();

				init(JavaCore.create(project), outputLocation, entries, false);
				monitor.worked(2);
			}
		} finally {
			monitor.done();
		}
		
	}
		
	/**
	 * Called from the wizard on cancel.
	 */
	public void performCancel() {
		if (fProjectCreated) {
			try {
				fMainPage.getProjectHandle().delete(false, false, null);
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
	}
}
