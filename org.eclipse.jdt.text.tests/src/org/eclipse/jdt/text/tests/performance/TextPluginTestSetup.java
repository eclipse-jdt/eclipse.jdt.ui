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

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.text.tests.JdtTextTestPlugin;

public class TextPluginTestSetup extends TestSetup {

	public static final String PROJECT= "org.eclipse.text";
	
	private static final String PROJECT_ZIP= "/testResources/org.eclipse.text-R3_0.zip";

	private String fPreviousPerspective;

	private String fPerspective;

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
	protected void setUp() throws Exception {
		IWorkbench workbench= PlatformUI.getWorkbench();
		IWorkbenchWindow activeWindow= workbench.getActiveWorkbenchWindow();
		IWorkbenchPage activePage= activeWindow.getActivePage();
		
		IViewReference viewReference= activePage.findViewReference(EditorTestHelper.INTRO_VIEW_ID);
		if (viewReference != null)
			activePage.hideView(viewReference);
		
		if (fPerspective != null)
			fPreviousPerspective= EditorTestHelper.showPerspective(fPerspective);
		
		boolean wasAutobuilding= ResourceTestHelper.disableAutoBuilding();
		if (ResourceTestHelper.projectExists(PROJECT))
			ResourceTestHelper.getProject(PROJECT).delete(true, true, null);
		ResourceTestHelper.createProjectFromZip(JdtTextTestPlugin.getDefault(), PROJECT_ZIP, PROJECT);
		if (wasAutobuilding) {
			ResourceTestHelper.fullBuild();
			ResourceTestHelper.enableAutoBuilding();
		}
		
		EditorTestHelper.joinBackgroundActivities();
	}
	
	/*
	 * @see junit.extensions.TestSetup#tearDown()
	 */
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
