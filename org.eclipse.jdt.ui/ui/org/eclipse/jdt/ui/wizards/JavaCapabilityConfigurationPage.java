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
package org.eclipse.jdt.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;

/**
 * Standard wizard page for creating new Java projects. This page can be used in 
 * project creation wizards. The page shows UI to configure the project with a Java 
 * build path and output location. On finish the page will also configure the Java nature.
 * <p>
 * This is a replacement for <code>NewJavaProjectWizardPage</code> with a cleaner API.
 * </p>
 * 
 * @since 2.0
 */
public class JavaCapabilityConfigurationPage extends NewElementWizardPage {

	private static final String PAGE_NAME= "JavaCapabilityConfigurationPage"; //$NON-NLS-1$
	
	private IJavaProject fJavaProject;
	private BuildPathsBlock fBuildPathsBlock;
	
	/**
	 * Creates a wizard page that can be used in a Java project creation wizard.
	 * It contains UI to configure a the classpath and the output folder.
	 * 
	 * <p>
	 * After constructing, a call to <code>init</code> is required
	 * </p>
	 */	
	public JavaCapabilityConfigurationPage() {
		super(PAGE_NAME);
		fJavaProject= null;
		
		setTitle(NewWizardMessages.getString("JavaCapabilityConfigurationPage.title")); //$NON-NLS-1$
		setDescription(NewWizardMessages.getString("JavaCapabilityConfigurationPage.description")); //$NON-NLS-1$
		
		IStatusChangeListener listener= new IStatusChangeListener() {
			public void statusChanged(IStatus status) {
				updateStatus(status);
			}
		};

		fBuildPathsBlock= new BuildPathsBlock(listener, 0);
	}
	
	/**
	 * Initializes the page with the project and default classpaths.
	 * <p>
	 * The default classpath entries must correspond the the given project.
	 * </p>
	 * <p>
	 * The caller of this method is responsible for creating the underlying project. The page will create the output,
	 * source and library folders if required.
	 * </p>
	 * <p>
	 * The project does not have to exist at the time of initialization, but must exist when executing the runnable
	 * obtained by <code>getRunnable()</code>.
	 * </p>
	 * @param jproject The Java project.
	 * @param defaultOutputLocation The default classpath entries or <code>null</code> to let the page choose the default
	 * @param defaultEntries The folder to be taken as the default output path or <code>null</code> to let the page choose the default
	 * @param defaultsOverrideExistingClasspath If set to <code>true</code>, an existing '.classpath' file is ignored. If set to <code>false</code>
	 * the given default classpath and output location is only used if no '.classpath' exists.
	 */
	public void init(IJavaProject jproject, IPath defaultOutputLocation, IClasspathEntry[] defaultEntries, boolean defaultsOverrideExistingClasspath) {
		if (!defaultsOverrideExistingClasspath && jproject.exists() && jproject.getProject().getFile(".classpath").exists()) { //$NON-NLS-1$
			defaultOutputLocation= null;
			defaultEntries= null;
		}
		fBuildPathsBlock.init(jproject, defaultOutputLocation, defaultEntries);
		fJavaProject= jproject;
	}
	

	/* (non-Javadoc)
	 * @see WizardPage#createControl
	 */	
	public void createControl(Composite parent) {
		Control control= fBuildPathsBlock.createControl(parent);
		setControl(control);
		Dialog.applyDialogFont(control);
		WorkbenchHelp.setHelp(control, IJavaHelpContextIds.NEW_JAVAPROJECT_WIZARD_PAGE);
	}
		
	/**
	 * Returns the currently configured output location. Note that the returned path 
	 * might not be a valid path.
	 * 
	 * @return the currently configured output location
	 */
	public IPath getOutputLocation() {
		return fBuildPathsBlock.getOutputLocation();
	}

	/**
	 * Returns the currently configured classpath. Note that the classpath might 
	 * not be valid.
	 * 
	 * @return the currently configured classpath
	 */	
	public IClasspathEntry[] getRawClassPath() {
		return fBuildPathsBlock.getRawClassPath();
	}
	
	/**
	 * Returns the Java project that was passed in <code>init</code> or <code>null</code> if the 
	 * page has not been initialized yet.
	 * 
	 * @return the managed Java project or <code>null</code>
	 */	
	public IJavaProject getJavaProject() {
		return fJavaProject;
	}	
	

	/**
	 * Returns the runnable that will create the Java project or <code>null</code> if the page has 
	 * not been initialized. The runnable sets the project's classpath and output location to the values 
	 * configured in the page and adds the Java nature if not set yet. The method requires that the 
	 * project is created and opened.
	 *
	 * @return the runnable that creates the new Java project
	 */		
	public IRunnableWithProgress getRunnable() {
		if (getJavaProject() != null) {
			return new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						configureJavaProject(monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			};
		}
		return null;	
	}

	/**
	 * Helper method to create and open a IProject . The project location
	 * is configured. No natures are added.
	 * 
	 * @param monitor a progress monitor to report progress or <code>null</code> if
	 * progress reporting is not desired
	 * @since 2.1
	 */
	public static void createProject(IProject project, IPath locationPath, IProgressMonitor monitor) throws CoreException {
		BuildPathsBlock.createProject(project, locationPath, monitor);
	}
	
	/**
	 * Adds the Java nature to the project (if not set yet) and configures the build classpath.
	 * 
	 * @param monitor a progress monitor to report progress or <code>null</code> if
	 * progress reporting is not desired
	 */
	public void configureJavaProject(IProgressMonitor monitor) throws CoreException, InterruptedException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		
		int nSteps= 6;			
		monitor.beginTask(NewWizardMessages.getString("JavaCapabilityConfigurationPage.op_desc_java"), nSteps); //$NON-NLS-1$
		
		try {
			IProject project= getJavaProject().getProject();
			BuildPathsBlock.addJavaNature(project, new SubProgressMonitor(monitor, 1));
			fBuildPathsBlock.configureJavaProject(new SubProgressMonitor(monitor, 5));
		} catch (OperationCanceledException e) {
			throw new InterruptedException();
		} finally {
			monitor.done();
		}			
	}
	
}
