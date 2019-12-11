/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core.rules;

import java.io.File;

import org.junit.rules.ExternalResource;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.search.SearchParticipantRecord;
import org.eclipse.jdt.internal.ui.search.SearchParticipantsExtensionPoint;


public class JUnitSourceSetup extends ExternalResource {
	public static final String PROJECT_NAME= "JUnitSource";
	public static final String SRC_CONTAINER= "src";

	private IJavaProject fProject;
	private SearchParticipantsExtensionPoint fExtensionPoint;

	static class NullExtensionPoint extends SearchParticipantsExtensionPoint {
		@Override
		public SearchParticipantRecord[] getSearchParticipants(IProject[] concernedProjects) {
			return new SearchParticipantRecord[0];
		}
	}

	public static IJavaProject getProject() {
		IProject project= ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
		return JavaCore.create(project);
	}

	public JUnitSourceSetup(SearchParticipantsExtensionPoint participants) {
		fExtensionPoint= participants;
	}

	public JUnitSourceSetup() {
		this( new NullExtensionPoint());
	}

	@Override
	public void before() throws Throwable {
		SearchParticipantsExtensionPoint.debugSetInstance(fExtensionPoint);
		fProject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		JavaProjectHelper.addRTJar(fProject);
		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
		JavaProjectHelper.addSourceContainerWithImport(fProject, SRC_CONTAINER, junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);
		JavaCore.setOptions(TestOptions.getDefaultOptions());
		TestOptions.initializeCodeGenerationOptions();
		JavaPlugin.getDefault().getCodeTemplateStore().load();
	}

	@Override
	public void after() {
		try {
			JavaProjectHelper.delete(fProject);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		SearchParticipantsExtensionPoint.debugSetInstance(null);
	}
}
