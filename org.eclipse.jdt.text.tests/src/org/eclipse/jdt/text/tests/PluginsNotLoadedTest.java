/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.text.tests;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.osgi.framework.Bundle;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.text.tests.performance.EditorTestHelper;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.search.SearchUtil;


/**
 * Tests whether the Java Editor forces the Search plug-in
 * to be loaded (which it should not).
 * 
 * @since 3.1
 */
public class PluginsNotLoadedTest extends TestCase {
	
	private JavaEditor fEditor;

	
	public static Test setUpTest(Test someTest) {
		return new JUnitProjectTestSetup(someTest);
	}
	
	public static Test suite() {
		return setUpTest(new TestSuite(PluginsNotLoadedTest.class));
	}
	
	
	/*
	 * @see junit.framework.TestCase#setUp()
	 * @since 3.1
	 */
	protected void setUp() throws Exception {
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.EDITOR_MARK_OCCURRENCES, true);
		fEditor= openJavaEditor(new Path("/" + JUnitProjectTestSetup.getProject().getElementName() + "/src/junit/framework/TestCase.java"));
		assertNotNull(fEditor);
	}
	
	/*
	 * @see junit.framework.TestCase#tearDown()
	 * @since 3.1
	 */
	protected void tearDown() throws Exception {
		EditorTestHelper.closeAllEditors();
		fEditor= null;
	}
	
	private JavaEditor openJavaEditor(IPath path) {
		IFile file= ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		assertTrue(file != null && file.exists());
		try {
			return (JavaEditor)EditorTestHelper.openInEditor(file, true);
		} catch (PartInitException e) {
			fail();
			return null;
		}
	}
	
	public void testSearchPluginNotLoaded() {
		assertFalse(SearchUtil.isSearchPlugInActivated());
	}
	
	public void testComparePluginNotLoaded() {
		assertFalse(Platform.getBundle("org.eclipse.compare").getState() == Bundle.ACTIVE);
	}

	public void testDebugPluginNotLoaded() {
		Bundle bundle= Platform.getBundle("org.eclipse.jdt.text.tests");
		Bundle[] bundles= bundle.getBundleContext().getBundles();
		for (int i= 0; i < bundles.length; i++) {
			if (bundles[i].getState() != Bundle.ACTIVE)
				System.out.println(bundles[i].getSymbolicName());
		}
	}
}
