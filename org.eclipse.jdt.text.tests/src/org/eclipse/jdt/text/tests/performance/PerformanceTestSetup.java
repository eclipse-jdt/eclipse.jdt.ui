/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.text.tests.performance;

import java.io.IOException;
import java.util.zip.ZipException;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.text.tests.JdtTextTestPlugin;

public class PerformanceTestSetup extends TestSetup {

	public static final String PROJECT= "org.eclipse.swt";
	
	public static final String TEXT_LAYOUT= "/" + PerformanceTestSetup.PROJECT + "/Eclipse SWT/win32/org/eclipse/swt/graphics/TextLayout.java";

	public static final String STYLED_TEXT= "/" + PerformanceTestSetup.PROJECT + "/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/StyledText.java";

	private static final String PROJECT_ZIP= "/testResources/org.eclipse.swt-R3_0.zip";

	private static final String INTRO_VIEW= "org.eclipse.ui.internal.introview";

	private boolean fSetPerspective;
	
	public PerformanceTestSetup(Test test) {
		this(test, true);
	}
	
	public PerformanceTestSetup(Test test, boolean setPerspective) {
		super(test);
		fSetPerspective= setPerspective;
	}

	/*
	 * @see junit.extensions.TestSetup#setUp()
	 */
	protected void setUp() throws Exception {
		IWorkbench workbench= PlatformUI.getWorkbench();
		IWorkbenchWindow activeWindow= workbench.getActiveWorkbenchWindow();
		IWorkbenchPage activePage= activeWindow.getActivePage();
		
		IViewReference viewReference= activePage.findViewReference(INTRO_VIEW);
		if (viewReference != null)
			activePage.hideView(viewReference);
		
		if (fSetPerspective)
			EditorTestHelper.showPerspective(EditorTestHelper.JAVA_PERSPECTIVE);
		
		if (!EditorTestHelper.projectExists(PROJECT)) {
			boolean wasAutobuilding= ResourceTestHelper.disableAutoBuilding();
			setUpProject();
			ResourceTestHelper.fullBuild();
			if (wasAutobuilding) {
				ResourceTestHelper.enableAutoBuilding();
				EditorTestHelper.joinBackgroundActivities();
			}
		}
	}
	
	/*
	 * @see junit.extensions.TestSetup#tearDown()
	 */
	protected void tearDown() throws Exception {
		// do nothing, the set up workspace will be used by other tests (see test.xml)
	}
	
	private static void setUpProject() throws IOException, ZipException, CoreException {
		IProject project= ResourceTestHelper.createProjectFromZip(JdtTextTestPlugin.getDefault(), PROJECT_ZIP, PROJECT);
		ResourceTestHelper.copy("/" + PROJECT + "/.classpath_win32", "/" + PROJECT + "/.classpath");
		assertTrue(JavaCore.create(project).exists());
	}
}
