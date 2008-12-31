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
package org.eclipse.jdt.ui.tests.dialogs;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.util.DialogCheck;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferenceDialog;

import org.eclipse.ui.dialogs.PreferencesUtil;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

public class PreferencesTest extends TestCase {

	public static Test suite() {
		TestSuite suite= new TestSuite(PreferencesTest.class.getName());
		suite.addTest(new PreferencesTest("testJavaBasePrefPage"));
		suite.addTest(new PreferencesTest("testClasspathVarPrefPage"));
		suite.addTest(new PreferencesTest("testCodeFormatterPrefPage"));
		suite.addTest(new PreferencesTest("testExecArgPropPage"));
		suite.addTest(new PreferencesTest("testImportOrganizePrefPage"));
		suite.addTest(new PreferencesTest("testInfoPropPage"));
		suite.addTest(new PreferencesTest("testJARSourceAttachmentPropPage"));
		suite.addTest(new PreferencesTest("testJavaBasePrefPage"));
		suite.addTest(new PreferencesTest("testJavaCompilerPrefPage"));
		suite.addTest(new PreferencesTest("testJavaDebugPrefPage"));
		suite.addTest(new PreferencesTest("testJavaEditorPrefPage"));
		suite.addTest(new PreferencesTest("testNoExecArgPropPage"));
		suite.addTest(new PreferencesTest("testRefactoringPrefPage"));
		suite.addTest(new PreferencesTest("testVMPrefPage"));
		suite.addTest(new PreferencesTest("testVMPropPage"));
		suite.addTest(new PreferencesTest("testVSourceLookupPage"));
		suite.addTest(new PreferencesTest("testVariableSourceAttachmentPropPage"));
		return suite;
	}

	private boolean fIsInteractive= true;

	private static final String PROJECT_NAME = "DummyProject";

	public PreferencesTest(String name) {
		super(name);
	}

	private Shell getShell() {
		return DialogCheck.getShell();
	}

	public void assertDialog(Dialog dialog) {
		if (fIsInteractive) {
			DialogCheck.assertDialog(dialog);
		} else {
			DialogCheck.assertDialogTexts(dialog);
		}
	}


	private PreferenceDialog getPreferenceDialog(String id) {
		return PreferencesUtil.createPreferenceDialogOn(getShell(), id, null, null);
	}

	private PreferenceDialog getPropertyDialog(String id, IAdaptable element) {
		return PreferencesUtil.createPropertyDialogOn(getShell(), element, id, null, null);
	}

	public void testJavaBasePrefPage() {
		Dialog dialog = getPreferenceDialog("org.eclipse.jdt.ui.preferences.JavaBasePreferencePage");
		assertDialog(dialog);
	}

	public void testImportOrganizePrefPage() {
		Dialog dialog = getPreferenceDialog("org.eclipse.jdt.ui.preferences.ImportOrganizePreferencePage");
		assertDialog(dialog);
	}

	public void testCodeFormatterPrefPage() {
		Dialog dialog = getPreferenceDialog("org.eclipse.jdt.ui.preferences.CodeFormatterPreferencePage");
		assertDialog(dialog);
	}

	public void testClasspathVarPrefPage() throws Exception {
		IPath path= ResourcesPlugin.getWorkspace().getRoot().getLocation();
		JavaCore.setClasspathVariable("DUMMY_VAR", path, null);
		Dialog dialog = getPreferenceDialog("org.eclipse.jdt.ui.preferences.ClasspathVariablesPreferencePage");
		assertDialog(dialog);
		JavaCore.removeClasspathVariable("DUMMY_VAR", null);
	}

	public void testJavaEditorPrefPage() {
		Dialog dialog = getPreferenceDialog("org.eclipse.jdt.ui.preferences.JavaEditorPreferencePage");
		assertDialog(dialog);
	}

	public void testJavaCompilerPrefPage() {
		Dialog dialog = getPreferenceDialog("org.eclipse.jdt.ui.preferences.CompilerPreferencePage");
		assertDialog(dialog);
	}

	public void testJavaDebugPrefPage() {
		Dialog dialog = getPreferenceDialog("org.eclipse.jdt.ui.JavaDebugPreferencePage");
		assertDialog(dialog);
	}

	public void testRefactoringPrefPage() {
		Dialog dialog = getPreferenceDialog("org.eclipse.jdt.ui.preferences.RefactoringPreferencePage");
		assertDialog(dialog);
	}

	public void testVMPrefPage() {
		Dialog dialog = getPreferenceDialog("org.eclipse.jdt.ui.preferences.VMPreferencePage");
		assertDialog(dialog);
	}

	public void testBuildPathPropPage() throws Exception {
		IJavaProject jproject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		JavaProjectHelper.addSourceContainer(jproject, "src");
		JavaProjectHelper.addRTJar(jproject);
		IJavaProject dep1= JavaProjectHelper.createJavaProject("OtherProject", "bin");
		JavaProjectHelper.addRequiredProject(jproject, dep1);

		Dialog dialog = getPropertyDialog("org.eclipse.jdt.ui.propertyPages.BuildPathsPropertyPage", jproject);
		assertDialog(dialog);
		JavaProjectHelper.delete(jproject);
		JavaProjectHelper.delete(dep1);
	}

	public void testJARSourceAttachmentPropPage() throws Exception {
		IJavaProject jproject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		IPackageFragmentRoot root= JavaProjectHelper.addRTJar(jproject);
		assertTrue(root != null);

		Dialog dialog = getPropertyDialog("org.eclipse.jdt.ui.propertyPages.SourceAttachmentPage1", root);
		assertDialog(dialog);

		JavaProjectHelper.delete(jproject);
	}

	public void testVariableSourceAttachmentPropPage() throws Exception {
		IJavaProject jproject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		IPackageFragmentRoot root= JavaProjectHelper.addVariableRTJar(jproject, "VAR_LIB", "VAR_SRC", "VAR_SRCROOT");
		assertTrue(root != null);

		Dialog dialog = getPropertyDialog("org.eclipse.jdt.ui.propertyPages.SourceAttachmentPage1", root);
		assertDialog(dialog);

		JavaProjectHelper.delete(jproject);
		JavaCore.removeClasspathVariable("VAR_LIB", null);
		JavaCore.removeClasspathVariable("VAR_SRC", null);
		JavaCore.removeClasspathVariable("VAR_SRCROOT", null);
	}

	public void testInfoPropPage() throws Exception {
		IJavaProject jproject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(jproject, "src");
		IPackageFragment pack= root.createPackageFragment("org.eclipse.jdt.internal.ui.wizards.dummy", true, null);
		ICompilationUnit cu= pack.getCompilationUnit("DummyCompilationUnitWizard.java");
		cu.createType("public class DummyCompilationUnitWizard {\n\n}\n", null, true, null);

		Dialog dialog = getPropertyDialog("org.eclipse.jdt.ui.propertyPages.InfoPage", cu);
		assertDialog(dialog);

		JavaProjectHelper.delete(jproject);
	}

	public void testNoExecArgPropPage() throws Exception {
		IJavaProject jproject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(jproject, "src");
		IPackageFragment pack= root.createPackageFragment("org.eclipse.jdt.internal.ui.hello", true, null);
		ICompilationUnit cu= pack.getCompilationUnit("HelloWorld.java");
		cu.createType("public class HelloWorld {\n\n}\n", null, true, null);

		Dialog dialog = getPropertyDialog("org.eclipse.jdt.ui.propertyPages.ExecutionArgsPage", cu);
		assertDialog(dialog);

		JavaProjectHelper.delete(jproject);
	}

	public void testExecArgPropPage() throws Exception {
		IJavaProject jproject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(jproject, "src");
		IPackageFragment pack= root.createPackageFragment("org.eclipse.jdt.internal.ui.hello", true, null);
		ICompilationUnit cu= pack.getCompilationUnit("HelloWorld.java");
		cu.createType("public class HelloWorld {\npublic static void main(String[] args) {}\n}\n", null, true, null);

		Dialog dialog = getPropertyDialog("org.eclipse.jdt.ui.propertyPages.ExecutionArgsPage", cu);
		assertDialog(dialog);

		JavaProjectHelper.delete(jproject);
	}


	public void testVMPropPage() throws Exception {
		IJavaProject jproject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");

		Dialog dialog = getPropertyDialog("org.eclipse.jdt.ui.propertyPages.VMPropertyPage", jproject);
		assertDialog(dialog);

		JavaProjectHelper.delete(jproject);
	}

	public void testVSourceLookupPage() throws Exception {
		IJavaProject jproject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		IJavaProject dep1= JavaProjectHelper.createJavaProject("OtherProject", "bin");
		JavaProjectHelper.addRequiredProject(jproject, dep1);

		Dialog dialog = getPropertyDialog("org.eclipse.jdt.ui.launching.SourceLookupPage", jproject);
		assertDialog(dialog);

		JavaProjectHelper.delete(jproject);
		JavaProjectHelper.delete(dep1);
	}

}

