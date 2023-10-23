/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.jdt.text.tests.performance;

import java.io.IOException;
import java.util.zip.ZipException;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.jdt.text.tests.JdtTextTestPlugin;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.internal.ui.util.CoreUtility;


public class TextPluginTestSetup extends TestSetup {

	public static final String PROJECT= "org.eclipse.text";

	private static final String PROJECT_ZIP= "/testResources/org.eclipse.text-R3_0.zip";

	private String fPreviousPerspective;

	private final String fPerspective;

	public TextPluginTestSetup(Test test) {
		this(test, null);
	}

	public TextPluginTestSetup(Test test, String perspective) {
		super(test);
		fPerspective= perspective;
	}

	/*
	 * @see junit.extensions.TestSetup#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		EditorTestHelper.showView(EditorTestHelper.INTRO_VIEW_ID, false);

		if (fPerspective != null)
			fPreviousPerspective= EditorTestHelper.showPerspective(fPerspective);

		boolean wasAutobuilding= CoreUtility.setAutoBuilding(false);
		createProjectFromZip();
		if (wasAutobuilding) {
			ResourceTestHelper.fullBuild();
			CoreUtility.setAutoBuilding(true);
		}

		EditorTestHelper.joinBackgroundActivities();
	}

	public static void createProjectFromZip() throws CoreException, IOException, ZipException {
		if (ResourceTestHelper.projectExists(PROJECT))
			ResourceTestHelper.getProject(PROJECT).delete(true, true, null);
		ResourceTestHelper.createProjectFromZip(JdtTextTestPlugin.getDefault(), PROJECT_ZIP, PROJECT);
	}

	/*
	 * @see junit.extensions.TestSetup#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
//		/*
//		 * Work around bug 72633: Problem deleting resources
//		 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=72633
//		 */
//		int tries= 3;
//		int delay= 2000;
//		for (int i= 0; i < tries && ResourceTestHelper.projectExists(PROJECT); i++) {
//			try {
//				ResourceTestHelper.getProject(PROJECT).delete(true, true, null);
//			} catch (CoreException x) {
//				x.printStackTrace();
//				Thread.sleep(delay);
//			}
//		}
		if (ResourceTestHelper.projectExists(PROJECT))
			ResourceTestHelper.getProject(PROJECT).delete(true, true, null);

		if (fPreviousPerspective != null)
			EditorTestHelper.showPerspective(fPreviousPerspective);
	}
}
