/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.wizards;import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;

/**
 * Wizard page to be used for capability wizards that want to configure Java build page. 
 */
public class JavaCapatibilityConfigurationPage extends WizardPage {
	
	private static final String PAGE_NAME= "NewJavaProjectWizardPage"; //$NON-NLS-1$
	
	private IJavaProject fJavaProject;
	private BuildPathsBlock fBuildPathsBlock;

	private IStatus fCurrStatus;
	private boolean fPageVisible;
	
	/**
	 * Creates a Java project wizard creation page.
	 * <p>
	 * The Java project wizard reads project name and location from the main page.
	 * </p>
	 *
	 * @param project the project to configure
	 */	
	public JavaCapatibilityConfigurationPage(IJavaProject javaProject) {
		super(PAGE_NAME);
		fJavaProject= javaProject;
		
		setTitle(NewWizardMessages.getString("NewJavaProjectWizardPage.title")); //$NON-NLS-1$
		setDescription(NewWizardMessages.getString("NewJavaProjectWizardPage.description")); //$NON-NLS-1$
		
		IStatusChangeListener listener= new IStatusChangeListener() {
			public void statusChanged(IStatus status) {
				updateStatus(status);
			}
		};

		fBuildPathsBlock= new BuildPathsBlock(ResourcesPlugin.getWorkspace().getRoot(), listener, true);
		fBuildPathsBlock.init(javaProject, null, null);
		
		fCurrStatus= new StatusInfo();
		fPageVisible= false;
	}
	
	
	public boolean hasConfiguredClasspath() {
		return fJavaProject.getProject().getFile(".classpath").exists();
	}
	
	
	/**
	 * Sets the default classpaths to be used for the new Java project.
	 * <p>
	 * The caller of this method is responsible for creating the classpath entries 
	 * for the <code>IJavaProject</code> that corresponds to the created project.
	 * The caller is responsible for creating any new folders that might be mentioned
	 * on the classpath.
	 * </p>
	 * <p>
	 * The wizard will create the output folder if required.
	 * </p>
	 * 
	 *
	 * @param entries the default classpath entries
	 * @param path the folder to be taken as the default output path
	 * @param appendDefaultJRE <code>true</code> a variable entry for the
	 *  default JRE (specified in the preferences) will be added to the classpath.
	 */
	public void setDefaultPaths(IPath outputLocation, IClasspathEntry[] entries, boolean appendDefaultJRE) {
		if (entries != null && appendDefaultJRE) {
			IClasspathEntry[] newEntries= new IClasspathEntry[entries.length + 1];
			System.arraycopy(entries, 0, newEntries, 0, entries.length);
			newEntries[entries.length]= JavaRuntime.getJREVariableEntry();
			entries= newEntries;
		}
		fBuildPathsBlock.init(fJavaProject, outputLocation, entries);
	}
	

	/* (non-Javadoc)
	 * @see WizardPage#createControl
	 */	
	public void createControl(Composite parent) {
		Control control= fBuildPathsBlock.createControl(parent);
		setControl(control);
		
		WorkbenchHelp.setHelp(control, IJavaHelpContextIds.NEW_JAVAPROJECT_WIZARD_PAGE);
	}
	

	/* (non-Javadoc)
	 * @see IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		fPageVisible= visible;
		updateStatus(fCurrStatus);
	}
	
	
	/**
	 * Returns the currently configured output location. Note that the returned path must not be valid.
	 */
	public IPath getOutputLocation() {
		return fBuildPathsBlock.getOutputLocation();
	}

	/**
	 * Returns the currently configured class path. Note that the class path must not be valid.
	 */	
	public IClasspathEntry[] getRawClassPath() {
		return fBuildPathsBlock.getRawClassPath();
	}
	

	/**
	 * Returns the runnable that will create the Java project. 
	 * The runnable sets the project's classpath and output location to the values configured in the page.
	 *
	 * @return the runnable
	 */		
	public IRunnableWithProgress getRunnable() {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				if (monitor == null) {
					monitor= new NullProgressMonitor();
				}
				int nSteps= 10;			
				monitor.beginTask(NewWizardMessages.getString("NewJavaProjectWizardPage.op_desc"), nSteps); //$NON-NLS-1$
				
				try {
					IProject project= fJavaProject.getProject();
					if (!project.hasNature(JavaCore.NATURE_ID)) {
						addNatureToProject(project, JavaCore.NATURE_ID, new SubProgressMonitor(monitor, 1));
						nSteps--;
					}
													
					// confige the build paths
					IRunnableWithProgress jrunnable= fBuildPathsBlock.getRunnable();
					jrunnable.run(new SubProgressMonitor(monitor, nSteps));
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
					
				} finally {
					monitor.done();
				}
			}
		};	
	}
	
	/**
	 * Adds a nature to a project
	 */
	private static void addNatureToProject(IProject proj, String natureId, IProgressMonitor monitor) throws CoreException {
		IProjectDescription description = proj.getDescription();
		String[] prevNatures= description.getNatureIds();
		String[] newNatures= new String[prevNatures.length + 1];
		System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
		newNatures[prevNatures.length]= natureId;
		description.setNatureIds(newNatures);
		proj.setDescription(description, monitor);
	}		
	
	/* (non-Javadoc)
	 * Updates the status line
	 */	
	private void updateStatus(IStatus status) {
		fCurrStatus= status;
		setPageComplete(!status.matches(IStatus.ERROR));
		if (fPageVisible) {
			StatusUtil.applyToStatusLine(this, status);
		}
	}
		
}