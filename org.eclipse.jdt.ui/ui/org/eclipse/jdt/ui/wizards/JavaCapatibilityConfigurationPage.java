/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.wizards;import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;

/**
 * @deprecated Use JavaCapabilityConfigurationPage. Will be removed before M6
 */
public class JavaCapatibilityConfigurationPage extends JavaCapabilityConfigurationPage {
	
	private static final String PAGE_NAME= "NewJavaProjectWizardPage"; //$NON-NLS-1$
	
	private IJavaProject fJavaProject;
	private BuildPathsBlock fBuildPathsBlock;
	
	/**
	 */	
	public JavaCapatibilityConfigurationPage(IProject project) {
		super();
		fJavaProject= JavaCore.create(project);
		init(fJavaProject, null, null, false);
	}
	
	/**
	 * @see JavaCapabilityConfigurationPage#init
	 */
	public void setDefaultPaths(IPath outputLocation, IClasspathEntry[] entries, boolean overrideExistingClasspath) {
		init(fJavaProject, outputLocation, entries, overrideExistingClasspath);
	}
	
}