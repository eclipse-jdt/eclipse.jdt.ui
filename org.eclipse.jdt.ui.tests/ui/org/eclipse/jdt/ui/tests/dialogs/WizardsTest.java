/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     Harald Albers <eclipse@albersweb.de> - [type wizards] New Annotation dialog could allow generating @Documented, @Retention and @Target - https://bugs.eclipse.org/339292
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.dialogs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.util.DialogCheck;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.jarpackager.JarPackageData;

import org.eclipse.jdt.internal.ui.jarpackager.JarPackageReader;
import org.eclipse.jdt.internal.ui.jarpackager.JarPackageWizard;
import org.eclipse.jdt.internal.ui.wizards.JavaProjectWizard;
import org.eclipse.jdt.internal.ui.wizards.NewAnnotationCreationWizard;
import org.eclipse.jdt.internal.ui.wizards.NewClassCreationWizard;
import org.eclipse.jdt.internal.ui.wizards.NewInterfaceCreationWizard;
import org.eclipse.jdt.internal.ui.wizards.NewPackageCreationWizard;
import org.eclipse.jdt.internal.ui.wizards.NewSourceFolderCreationWizard;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class WizardsTest extends TestCase {

	private static final String PROJECT_NAME = "DummyProject";

	public static Test suite() {
		TestSuite suite= new TestSuite(WizardsTest.class.getName());
		suite.addTest(new WizardsTest("testClassWizard"));
		suite.addTest(new WizardsTest("testInterfaceWizard"));
		suite.addTest(new WizardsTest("testAnnotationWizard"));
		suite.addTest(new WizardsTest("testJarPackageWizard"));
		suite.addTest(new WizardsTest("testNewProjectWizard"));
		suite.addTest(new WizardsTest("testPackageWizard"));
		suite.addTest(new WizardsTest("testSourceFolderWizard"));
		suite.addTest(new WizardsTest("testJarPackageWizard_sameElementOrderInJarDescriptionExports"));
		return suite;
	}

	public WizardsTest(String name) {
		super(name);
	}
	private Shell getShell() {
		return DialogCheck.getShell();
	}
	private IWorkbench getWorkbench() {
		return PlatformUI.getWorkbench();
	}

	public void testNewProjectWizard() throws Exception {
		JavaProjectWizard wizard = new JavaProjectWizard();
		wizard.init(getWorkbench(),  null);
		wizard.setForcePreviousAndNextButtons(true);
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		dialog.create();
		DialogCheck.assertDialog(dialog);
	}

	public void testSourceFolderWizard() throws Exception {
		IJavaProject jproject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		JavaProjectHelper.addSourceContainer(jproject, "src1");
		JavaProjectHelper.addRTJar(jproject);

		NewSourceFolderCreationWizard wizard = new NewSourceFolderCreationWizard();
		wizard.init(getWorkbench(), new StructuredSelection(jproject));
		wizard.setForcePreviousAndNextButtons(true);
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		dialog.create();
		DialogCheck.assertDialog(dialog);

		JavaProjectHelper.delete(jproject);
	}

	public void testPackageWizard() throws Exception {
		IJavaProject jproject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(jproject, "src1");
		JavaProjectHelper.addRTJar(jproject);

		NewPackageCreationWizard wizard = new NewPackageCreationWizard();
		wizard.init(getWorkbench(), new StructuredSelection(root));
		wizard.setForcePreviousAndNextButtons(true);
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		dialog.create();
		DialogCheck.assertDialog(dialog);

		JavaProjectHelper.delete(jproject);
	}

	public void testClassWizard() throws Exception {
		IJavaProject jproject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(jproject, "src1");
		IPackageFragment pack= root.createPackageFragment("org.eclipse.jdt.internal.ui.hello", true, null);
		JavaProjectHelper.addRTJar(jproject);

		NewClassCreationWizard wizard = new NewClassCreationWizard();
		wizard.init(getWorkbench(), new StructuredSelection(pack));
		wizard.setForcePreviousAndNextButtons(true);
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		dialog.create();
		DialogCheck.assertDialog(dialog);

		JavaProjectHelper.delete(jproject);
	}

	public void testInterfaceWizard() throws Exception {
		IJavaProject jproject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(jproject, "src1");
		IPackageFragment pack= root.createPackageFragment("org.eclipse.jdt.internal.ui.hello", true, null);
		JavaProjectHelper.addRTJar(jproject);

		NewInterfaceCreationWizard wizard = new NewInterfaceCreationWizard();
		wizard.init(getWorkbench(), new StructuredSelection(pack));
		wizard.setForcePreviousAndNextButtons(true);
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		dialog.create();
		DialogCheck.assertDialog(dialog);

		JavaProjectHelper.delete(jproject);
	}

	public void testAnnotationWizard() throws Exception {
		IJavaProject jproject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(jproject, "src1");
		IPackageFragment pack= root.createPackageFragment("org.eclipse.jdt.internal.ui.hello", true, null);
		JavaProjectHelper.addRTJar(jproject);

		NewAnnotationCreationWizard wizard= new NewAnnotationCreationWizard();
		wizard.init(getWorkbench(), new StructuredSelection(pack));
		wizard.setForcePreviousAndNextButtons(true);
		WizardDialog dialog= new WizardDialog(getShell(), wizard);
		dialog.create();
		DialogCheck.assertDialog(dialog);

		JavaProjectHelper.delete(jproject);
	}

	public void testJarPackageWizard() throws Exception {
		IJavaProject jproject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(jproject, "src1");
		JavaProjectHelper.addRTJar(jproject);
		IPackageFragment pack= root.createPackageFragment("org.eclipse.jdt.internal.ui.hello", true, null);
		ICompilationUnit cu= pack.getCompilationUnit("HelloWorld.java");
		cu.createType("public class HelloWorld {\npublic static void main(String[] args) {}\n}\n", null, true, null);

		JarPackageWizard wizard = new JarPackageWizard();
		wizard.init(getWorkbench(), new StructuredSelection(root));
		wizard.setForcePreviousAndNextButtons(true);
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		dialog.create();
		DialogCheck.assertDialog(dialog);

		JavaProjectHelper.delete(jproject);
	}

	public void testJarPackageWizard_sameElementOrderInJarDescriptionExports() throws Exception {
		IResource[] roots = createProjectsWithSourceFolders(20);
		IProject outputProject = roots[0].getProject();

		IFile jardescProperOrder = outputProject.getFile("proper.jardesc");
		exportJardesc(outputProject, roots).move(jardescProperOrder.getFullPath(), true, null);

		IFile jardescReverseOrder = outputProject.getFile("reverse.jardesc");
		Collections.reverse(Arrays.asList(roots));
		exportJardesc(outputProject, roots).move(jardescReverseOrder.getFullPath(), true, null);

		IFile jardescRewritten= outputProject.getFile("rewritten.jardesc");
		JarPackageData packageData = new JarPackageData();
		try (InputStream packageDataFileStream = Files.newInputStream(jardescReverseOrder.getLocation().toPath())) {
			new JarPackageReader(packageDataFileStream).read(packageData);
		}
		exportJardesc(outputProject, packageData).move(jardescRewritten.getFullPath(), true, null);

		assertThat("JAR descriptions written for elements in original and reverse order differ", Files.readString(jardescProperOrder.getLocation().toPath()),
				is(Files.readString(jardescReverseOrder.getLocation().toPath())));
		assertThat("original and rewritten JAR descriptions differ", Files.readString(jardescProperOrder.getLocation().toPath()), is(Files.readString(jardescRewritten.getLocation().toPath())));

		for (IResource root : roots) {
			JavaProjectHelper.delete(root.getProject());
		}
	}

	private IResource[] createProjectsWithSourceFolders(int numberOfProjects) throws CoreException {
		String binFolderName = "bin";
		String sourceFolderName = "src";
		IResource[] roots = new IResource[numberOfProjects];
		for (int projectNumber = 0 ; projectNumber < numberOfProjects; projectNumber++) {
			IJavaProject project= JavaProjectHelper.createJavaProject(UUID.randomUUID().toString(), binFolderName);
			roots[projectNumber] = JavaProjectHelper.addSourceContainer(project, sourceFolderName).getResource();
		}
		return roots;
	}

	private IFile exportJardesc(IProject projectForExport, IResource[] exportElements) throws CoreException {
		JarPackageData data = new JarPackageData();
		data.setElements(exportElements);
		return exportJardesc(projectForExport, data);
	}

	private IFile exportJardesc(IProject projectForExport, JarPackageData data) throws CoreException {
		IFile exportFile = projectForExport.getFile("export.jar");
		data.setJarLocation(exportFile.getLocation());
		IFile jardescFile = projectForExport.getFile("save.jardesc");
		data.setDescriptionLocation(jardescFile.getFullPath());
		data.setSaveDescription(true);

		JarPackageWizard wizard = new JarPackageWizard();
		wizard.init(getWorkbench(), data);
		wizard.setForcePreviousAndNextButtons(true);
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		dialog.create();
		boolean exportSuccessful = wizard.performFinish();

		exportFile.delete(true, null);
		assertTrue("wizard did not finish successfully: " + wizard, exportSuccessful);
		assertTrue("JAR description file was not created", jardescFile.exists());
		return jardescFile;
	}

}

