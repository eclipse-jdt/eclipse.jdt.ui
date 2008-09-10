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
package org.eclipse.jdt.text.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.osgi.framework.Bundle;

import org.eclipse.jdt.text.tests.performance.EditorTestHelper;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;


/**
 * Tests whether the Java Editor forces the Search plug-in
 * to be loaded (which it should not).
 * 
 * @since 3.1
 */
public class PluginsNotLoadedTest extends TestCase {
	
	private static String[] NOT_LOADED_BUNDLES= new String[] {
			"org.apache.xerces",
			"org.eclipse.jdt.astview",
			"org.eclipse.jdt.jeview",
			"org.eclipse.reftracker",
			"org.eclipse.releng.tools",
			"org.eclipse.swt.sleak",
			"org.eclipse.swt.spy",
			"com.jcraft.jsch",
			"javax.servlet",
			"javax.servlet.jsp",
			"org.apache.ant",
			"org.apache.commons.el",
			"org.apache.commons.logging",
			"org.apache.jasper",
			"org.apache.lucene",
			"org.apache.lucene.analysis",
			"org.eclipse.ant.core",
			"org.eclipse.ant.ui",
			"org.eclipse.compare",
			"org.eclipse.core.commands",
			"org.eclipse.core.expressions.tests",
			"org.eclipse.core.filebuffers.tests",
			"org.eclipse.core.filesystem.win32.x86",
			"org.eclipse.core.resources.compatibility",
			"org.eclipse.core.resources.win32",
			"org.eclipse.core.runtime.compatibility.registry",
			"org.eclipse.core.variables",
			"org.eclipse.debug.ui",
			"org.eclipse.equinox.http.jetty",
			"org.eclipse.equinox.http.registry",
			"org.eclipse.equinox.http.servlet",
			"org.eclipse.equinox.jsp.jasper",
			"org.eclipse.equinox.jsp.jasper.registry",
			"org.eclipse.help.base",
			"org.eclipse.help.ui",
			"org.eclipse.help.webapp",
			"org.eclipse.jdt",
			"org.eclipse.jdt.apt.core",
			"org.eclipse.jdt.apt.ui",
			"org.eclipse.jdt.compiler.apt",
			"org.eclipse.jdt.compiler.tool",
			"org.eclipse.jdt.debug",
			"org.eclipse.jdt.debug.ui",
			"org.eclipse.jdt.doc.isv",
			"org.eclipse.jdt.doc.user",
			"org.eclipse.jdt.junit",
			"org.eclipse.jdt.junit.runtime",
			"org.eclipse.jdt.junit4.runtime",
			"org.eclipse.jdt.launching",
			"org.eclipse.jdt.ui.examples.javafamily",
			"org.eclipse.jdt.ui.examples.projects",
			"org.eclipse.jdt.ui.tests.refactoring",
			"org.eclipse.jface.databinding",
			"org.eclipse.jface.text",
			"org.eclipse.jface.text.tests",
			"org.eclipse.jsch.core",
			"org.eclipse.ltk.core.refactoring.tests",
			"org.eclipse.ltk.ui.refactoring.tests",
			"org.eclipse.osgi.services",
			"org.eclipse.pde",
			"org.eclipse.pde.build",
			"org.eclipse.pde.core",
			"org.eclipse.pde.doc.user",
			"org.eclipse.pde.runtime",
			"org.eclipse.platform.doc.isv",
			"org.eclipse.platform.doc.user",
			"org.eclipse.sdk",
			"org.eclipse.sdk.tests",
			"org.eclipse.search",
			"org.eclipse.search.tests",
			"org.eclipse.swt",
			"org.eclipse.swt.win32.win32.x86",
			"org.eclipse.team.cvs.core",
			"org.eclipse.team.cvs.ssh",
			"org.eclipse.team.cvs.ssh2",
			"org.eclipse.team.cvs.ui",
			"org.eclipse.test.performance",
			"org.eclipse.test.performance.ui",
			"org.eclipse.test.performance.win32",
			"org.eclipse.text",
			"org.eclipse.text.tests",
			"org.eclipse.ui.cheatsheets",
			"org.eclipse.ui.console",
			"org.eclipse.ui.editors.tests",
			"org.eclipse.ui.examples.javaeditor",
			"org.eclipse.ui.examples.rcp.texteditor",
			"org.eclipse.ui.examples.recipeeditor",
			"org.eclipse.ui.externaltools",
			"org.eclipse.ui.ide.application",
			"org.eclipse.ui.navigator",
			"org.eclipse.ui.navigator.resources",
			"org.eclipse.ui.views.properties.tabbed",
			"org.eclipse.ui.win32",
			"org.eclipse.ui.workbench.compatibility",
			"org.eclipse.ui.workbench.texteditor.tests",
			"org.eclipse.update.core.win32",
			"org.eclipse.update.core.linux",
			"org.eclipse.update.ui",
			"org.junit",
			"org.junit4",
			"org.mortbay.jetty",
			"om.ibm.icu.source",
			"javax.servlet.jsp.source",
			"javax.servlet.source",
			"org.apache.ant.source",
			"org.apache.commons.el.source",
			"org.apache.commons.logging.source",
			"org.apache.jasper.source",
			"org.apache.lucene.analysis.source",
			"org.apache.lucene.source",
			"org.eclipse.core.boot",
			"org.eclipse.core.databinding.beans",
			"org.eclipse.cvs",
			"org.eclipse.cvs.source",
			"org.eclipse.equinox.launcher",
			"org.eclipse.equinox.launcher.win32.win32.x86",
			"org.eclipse.help.appserver",
			"org.eclipse.jdt.apt.pluggable.core",
			"org.eclipse.jdt.source",
			"org.eclipse.jsch.ui",
			"org.eclipse.osgi.util",
			"org.eclipse.pde.junit.runtime",
			"org.eclipse.pde.source",
			"org.eclipse.pde.ui.templates",
			"org.eclipse.platform",
			"org.eclipse.platform.source",
			"org.eclipse.platform.source.win32.win32.x86",
			"org.eclipse.rcp",
			"org.eclipse.rcp.source",
			"org.eclipse.rcp.source.win32.win32.x86",
			"org.eclipse.ui.browser",
			"org.eclipse.ui.presentations.r21",
			"org.junit.source",
			"org.mortbay.jetty.source"
		};

	
	private JavaEditor fEditor;

	
	public static Test setUpTest(Test someTest) {
		return new JUnitProjectTestSetup(someTest);
	}
	
	public static Test suite() {
		return setUpTest(new TestSuite(PluginsNotLoadedTest.class));
	}

	/**
	 * If a test suite uses this test and has other tests that cause plug-ins to be loaded then
	 * those need to be indicated here.
	 * 
	 * @param loadedPlugins plug-ins that are additionally loaded by the caller
	 * @since 3.5
	 */
	public static void addLoadedPlugIns(String[] loadedPlugins) {
		Assert.isLegal(loadedPlugins != null);
		List l= new ArrayList(Arrays.asList(NOT_LOADED_BUNDLES));
		l.removeAll(Arrays.asList(loadedPlugins));
		NOT_LOADED_BUNDLES= (String[])l.toArray(new String[0]);
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

	public void _testPrintNotLoaded() {
		Bundle bundle= Platform.getBundle("org.eclipse.jdt.text.tests");
		Bundle[] bundles= bundle.getBundleContext().getBundles();
		for (int i= 0; i < bundles.length; i++) {
			if (bundles[i].getState() != Bundle.ACTIVE)
				System.out.println(bundles[i].getSymbolicName());
		}
	}

	public void testPluginsNotLoaded() {
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < NOT_LOADED_BUNDLES.length; i++) {
			Bundle bundle= Platform.getBundle(NOT_LOADED_BUNDLES[i]);
			if (bundle != null && bundle.getState() == Bundle.ACTIVE) {
				buf.append("- ");
				buf.append(NOT_LOADED_BUNDLES[i]);
				buf.append('\n');
			}
		}
		assertTrue("Wrong bundles loaded:\n" + buf, buf.length() == 0);
	}
}
