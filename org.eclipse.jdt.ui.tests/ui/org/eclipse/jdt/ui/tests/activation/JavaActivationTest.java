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
package org.eclipse.jdt.ui.tests.activation;

import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.Platform;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

public class JavaActivationTest {

	private IJavaProject project;


	private static final Set<String> inactiveTestBundles= Set.of(
			"org.apache.xerces",
			"org.eclipse.jdt.astview",
			"org.eclipse.jdt.jeview",
			"org.eclipse.reftracker",
			"org.eclipse.swt.sleak",
			"org.eclipse.swt.spy",
			"com.jcraft.jsch",
			"javax.servlet",
			"javax.servlet.jsp-api",
			"org.apache.ant",
			"org.apache.commons.el",
			"org.apache.commons.logging",
			"org.apache.jasper",
			"org.apache.lucene",
			"org.apache.lucene.analysis",
			"org.eclipse.ant.core",
			"org.eclipse.ant.ui",
			"org.eclipse.compare", // caveat, see workaround for EGit in setUpTest below!
			"org.eclipse.core.commands",
			"org.eclipse.core.expressions.tests",
			"org.eclipse.core.filebuffers.tests",
			"org.eclipse.core.filesystem.win32.x86",
			"org.eclipse.core.resources.compatibility",
			"org.eclipse.core.resources.win32",
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
			"org.eclipse.jdt.doc.isv",
			"org.eclipse.jdt.doc.user",
			"org.eclipse.jdt.junit",
			"org.eclipse.jdt.junit.runtime",
			"org.eclipse.jdt.junit4.runtime",
			"org.eclipse.jdt.ui.examples.javafamily",
			"org.eclipse.jdt.ui.examples.projects",
			"org.eclipse.jdt.ui.tests.refactoring",
			"org.eclipse.jface.databinding",
			"org.eclipse.jface.text",
			"org.eclipse.jface.text.tests",
			"org.eclipse.ltk.core.refactoring.tests",
			"org.eclipse.ltk.ui.refactoring.tests",
			"org.eclipse.osgi.services",
			"org.eclipse.pde",
			"org.eclipse.pde.build",
			"org.eclipse.pde.doc.user",
			"org.eclipse.pde.runtime",
			"org.eclipse.platform.doc.isv",
			"org.eclipse.platform.doc.user",
			"org.eclipse.sdk",
			"org.eclipse.sdk.tests",
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
			"org.eclipse.ui.editors.tests",
			"org.eclipse.ui.examples.javaeditor",
			"org.eclipse.ui.examples.rcp.texteditor",
			"org.eclipse.ui.examples.recipeeditor",
			"org.eclipse.ui.externaltools",
// Bug 416915: Allow to run tests with tycho-surefire-plugin
//			"org.eclipse.ui.navigator",
//			"org.eclipse.ui.navigator.resources",
			"org.eclipse.ui.views.properties.tabbed",
			"org.eclipse.ui.win32",
			"org.eclipse.ui.workbench.compatibility",
			"org.eclipse.ui.workbench.texteditor.tests",
			"org.eclipse.update.ui",
			"org.junit",
			"org.junit4",
			"org.mortbay.jetty",
			"com.ibm.icu.source",
			"javax.servlet.jsp-api.source",
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
			"org.eclipse.pde.source",
			"org.eclipse.pde.ui.templates",
			"org.eclipse.platform",
			"org.eclipse.platform.source",
			"org.eclipse.platform.source.win32.win32.x86",
			"org.eclipse.rcp",
			"org.eclipse.rcp.source",
			"org.eclipse.rcp.source.win32.win32.x86",
			"org.eclipse.ui.browser",
			"org.junit.source",
			"org.mortbay.jetty.source"
		);

	@Before
	public void setUp() throws Exception {
		project= JavaProjectHelper.createJavaProject("TestProject1", "bin");
	}

	@After
	public void tearDown() throws Exception {
	    getPage().closeAllEditors(false);
		JavaProjectHelper.delete(project);
	}

	private IWorkbenchPage getPage() {
	    IWorkbench workbench= PlatformUI.getWorkbench();
	    return workbench.getActiveWorkbenchWindow().getActivePage();
	}

	private ICompilationUnit createTestCU() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(project, "src");
		IPackageFragment pack= sourceFolder.createPackageFragment("pack0", false, null);
		String str= """
			package pack0;
			public class List1 {
			}
			""";
		return pack.createCompilationUnit("List1.java", str, false, null);
	}

	@Test
	public void testOpenJavaEditor() throws Exception {
		ICompilationUnit unit= createTestCU();
		EditorUtility.openInEditor(unit);
		checkNotLoaded(inactiveTestBundles);
	}

	public void checkNotLoaded(Set<String> inactiveBundles) {
		Bundle bundle= Platform.getBundle("org.eclipse.jdt.ui.tests");
		for (Bundle b : bundle.getBundleContext().getBundles()) {
			if (b.getState() == Bundle.ACTIVE && inactiveBundles.contains(b.getSymbolicName())) {
				Assert.fail("plugin should not be activated: " + b.getSymbolicName());
			}
		}
	}
}
