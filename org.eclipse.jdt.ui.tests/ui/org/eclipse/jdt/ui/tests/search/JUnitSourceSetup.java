/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.search;

import java.io.File;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.search.SearchParticipantRecord;
import org.eclipse.jdt.internal.ui.search.SearchParticipantsExtensionPoint;

/**
 */
public class JUnitSourceSetup extends TestSetup {
	public static final String PROJECT_NAME= "JUnitSource";
	public static final String SRC_CONTAINER= "src";

	private IJavaProject fProject;
	private SearchParticipantsExtensionPoint fExtensionPoint;

	static class NullExtensionPoint extends SearchParticipantsExtensionPoint {
		public SearchParticipantRecord[] getSearchParticipants(IProject[] concernedProjects) {
			return new SearchParticipantRecord[0];
		}
	}

	public static IJavaProject getProject() {
		IProject project= ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
		return JavaCore.create(project);
	}

	public JUnitSourceSetup(Test test, SearchParticipantsExtensionPoint participants) {
		super(test);
		fExtensionPoint= participants;
	}

	public JUnitSourceSetup(Test test) {
		this(test, new NullExtensionPoint());
	}

	protected void setUp() throws Exception {
		SearchParticipantsExtensionPoint.debugSetInstance(fExtensionPoint);
		fProject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		IClasspathEntry jreLib= JavaCore.newContainerEntry(new Path("org.eclipse.jdt.launching.JRE_CONTAINER"));  //$NON-NLS-1$
		JavaProjectHelper.addToClasspath(fProject, jreLib);
		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
		JavaProjectHelper.addSourceContainerWithImport(fProject, SRC_CONTAINER, junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);
		JavaCore.setOptions(TestOptions.getDefaultOptions());
		TestOptions.initializeCodeGenerationOptions();
		JavaPlugin.getDefault().getCodeTemplateStore().load();
	}

	/* (non-Javadoc)
	 * @see junit.extensions.TestSetup#tearDown()
	 */
	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fProject);
		SearchParticipantsExtensionPoint.debugSetInstance(null);
	}
}
