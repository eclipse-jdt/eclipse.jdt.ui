/*******************************************************************************
 * Copyright (c) 2024 Erik Brangs and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Erik Brangs - initial implementation
 *******************************************************************************/
package org.eclipse.jdt.junit.launcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.After;
import org.junit.Test;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;

public class AdvancedJUnitLaunchConfigurationDelegateTest {

	private IJavaProject fJavaProject;

	@After
	public void deleteProject() throws CoreException {
		if (fJavaProject != null) {
			JavaProjectHelper.delete(fJavaProject);
		}
	}

	@Test
	public void runTestsInSourceFolderHandlesMultipleSourceFoldersCorrectly() throws Exception {
		String projectName= "JUnitLaunchConfigurationDelegate-TestProject-MultipleSourceFolders";
		fJavaProject= JavaProjectHelper.createJavaProject(projectName, "bin");
		String firstTestSrcFolder= "test-src1";
		IPackageFragmentRoot testSrc1= JavaProjectHelper.addSourceContainer(fJavaProject, firstTestSrcFolder);
		IPackageFragmentRoot testSrc2= JavaProjectHelper.addSourceContainer(fJavaProject, "test-src2");
		String testPackage= "p1";
		IPackageFragment packageTestSrc1= testSrc1.createPackageFragment(testPackage, true, null);
		String contentsTestSrc1Test= """
				public class FirstTest {
				@org.junit.jupiter.api.Test
				public void myTest() { }
				}
				""";
		packageTestSrc1.createCompilationUnit("FirstTest.java", contentsTestSrc1Test, true, null);
		IPackageFragment packageTestSrc2= testSrc2.createPackageFragment(testPackage, true, null);
		String contentsTestSrc2Test= """
				public class ShouldNotBeRunTest {
				@org.junit.jupiter.api.Test
				public void shouldNotRunTest() { }
				}
				""";

		packageTestSrc2.createCompilationUnit("ShouldNotBeRunTest.java", contentsTestSrc2Test, true, null);

		List<String> fileLines= showCommandLineAndExtractContentOfTestNameFile(projectName, fJavaProject, testSrc1);
		String lineForFirstTest= "p1.FirstTest";
		assertThat(fileLines).contains(lineForFirstTest).size().isEqualTo(1);
	}

	@Test
	public void runTestsInSourceFolderHandlesMultiplePackagesInASourceFolderCorrectly() throws Exception {
		String projectName= "JUnitLaunchConfigurationDelegate-TestProject-MultiplePackagesInOneFolder";
		fJavaProject= JavaProjectHelper.createJavaProject(projectName, "bin");
		String firstTestSrcFolder= "test-src1";
		IPackageFragmentRoot testSrc1= JavaProjectHelper.addSourceContainer(fJavaProject, firstTestSrcFolder);
		String testPackage= "p1";
		IPackageFragment packageTestSrc1= testSrc1.createPackageFragment(testPackage, true, null);
		String contentsTestSrc1Test= """
				public class FirstTest {
				@org.junit.jupiter.api.Test
				public void myTest() { }
				}
				""";
		String secondTestPackage= "p2";
		packageTestSrc1.createCompilationUnit("FirstTest.java", contentsTestSrc1Test, true, null);
		IPackageFragment packageTestSrc2= testSrc1.createPackageFragment(secondTestPackage, true, null);
		String contentsTestSrc2Test= """
				public class SecondTest {
				@org.junit.jupiter.api.Test
				public void mySecondTest() { }
				}
				""";
		packageTestSrc2.createCompilationUnit("SecondTest.java", contentsTestSrc2Test, true, null);

		List<String> fileLines= showCommandLineAndExtractContentOfTestNameFile(projectName, fJavaProject, testSrc1);
		String lineForFirstTest= "p1.FirstTest";
		String lineForSecondTest= "p2.SecondTest";
		assertThat(fileLines).contains(lineForFirstTest, lineForSecondTest).size().isEqualTo(2);
	}

	@Test
	public void runTestsInSourceFolderHandlesMultipleSourceFilesInPackagesInASourceFolderCorrectly() throws Exception {
		String projectName= "JUnitLaunchConfigurationDelegate-TestProject-MultipleSourceFilesInOnePackage";
		fJavaProject= JavaProjectHelper.createJavaProject(projectName, "bin");
		String firstTestSrcFolder= "test-src1";
		IPackageFragmentRoot testSrc1= JavaProjectHelper.addSourceContainer(fJavaProject, firstTestSrcFolder);
		String testPackage= "p1";
		IPackageFragment packageTestSrc1= testSrc1.createPackageFragment(testPackage, true, null);
		String contentsTestSrc1Test= """
				public class FirstTest {
				@org.junit.jupiter.api.Test
				public void myTest() { }
				}
				""";
		packageTestSrc1.createCompilationUnit("FirstTest.java", contentsTestSrc1Test, true, null);
		String contentsTestSrc2Test= """
				public class SecondTest {
				@org.junit.jupiter.api.Test
				public void mySecondTest() { }
				}
				""";
		packageTestSrc1.createCompilationUnit("SecondTest.java", contentsTestSrc2Test, true, null);

		List<String> fileLines= showCommandLineAndExtractContentOfTestNameFile(projectName, fJavaProject, testSrc1);
		String lineForFirstTest= "p1.FirstTest";
		String lineForSecondTest= "p1.SecondTest";
		assertThat(fileLines).contains(lineForFirstTest, lineForSecondTest).size().isEqualTo(2);
	}

	@Test
	public void runTestsInSourceFolderHandlesPackagesWithCompilationUnitsWithoutTopLevelClassCorrectly() throws Exception {
		String projectName= "JUnitLaunchConfigurationDelegate-TestProject-NoTopLevelClassInPackage";
		fJavaProject= JavaProjectHelper.createJavaProject(projectName, "bin");
		String firstTestSrcFolder= "test-src1";
		IPackageFragmentRoot testSrc1= JavaProjectHelper.addSourceContainer(fJavaProject, firstTestSrcFolder);
		String testPackage= "p1";
		IPackageFragment packageTestSrc1= testSrc1.createPackageFragment(testPackage, true, null);
		String contentsTestSrc1Test= """
				public class FirstTest {
				@org.junit.jupiter.api.Test
				public void myTest() { }
				}
				""";
		String secondTestPackage= "p2";
		packageTestSrc1.createCompilationUnit("package-info.java", contentsTestSrc1Test, true, null);
		IPackageFragment packageTestSrc2= testSrc1.createPackageFragment(secondTestPackage, true, null);
		String contentsTestSrc2Test= """
				/** This is a package-info.java and thus does not have a top-level type */
				package p2;
				""";
		packageTestSrc2.createCompilationUnit("SecondTest.java", contentsTestSrc2Test, true, null);

		List<String> fileLines= showCommandLineAndExtractContentOfTestNameFile(projectName, fJavaProject, testSrc1);
		String lineForFirstTest= "p1.FirstTest";
		assertThat(fileLines).contains(lineForFirstTest).size().isEqualTo(1);
	}

	@Test
	public void runTestsInSourceFolderOnlyUsesTopLevelClasses() throws Exception {
		String projectName= "JUnitLaunchConfigurationDelegate-TestProject-OnlyTopLevelClasses";
		fJavaProject= JavaProjectHelper.createJavaProject(projectName, "bin");
		String firstTestSrcFolder= "test-src1";
		IPackageFragmentRoot testSrc1= JavaProjectHelper.addSourceContainer(fJavaProject, firstTestSrcFolder);
		String testPackage= "p1";
		IPackageFragment packageTestSrc1= testSrc1.createPackageFragment(testPackage, true, null);
		String contentsTestSrc1Test= """
				public class FirstTest {
				@org.junit.jupiter.api.Test
				public void myTest() { }
				}

				@org.junit.jupiter.api.Nest
				class NestedTestClass {
					@org.junit.jupiter.api.Test
					public void myNestedTest() { }
					}
				}
				""";
		packageTestSrc1.createCompilationUnit("FirstTest.java", contentsTestSrc1Test, true, null);

		List<String> fileLines= showCommandLineAndExtractContentOfTestNameFile(projectName, fJavaProject, testSrc1);
		String lineForFirstTest= "p1.FirstTest";
		assertThat(fileLines).contains(lineForFirstTest).size().isEqualTo(1);
	}

	private List<String> showCommandLineAndExtractContentOfTestNameFile(String projectName, IJavaProject javaProject, IPackageFragmentRoot testSrc1)
			throws CoreException, JavaModelException, IOException {
		JavaProjectHelper.addRTJar18(javaProject);
		IClasspathEntry cpe= JavaCore.newContainerEntry(JUnitCore.JUNIT5_CONTAINER_PATH);
		JavaProjectHelper.addToClasspath(javaProject, cpe);
		JavaProjectHelper.set18CompilerOptions(javaProject);
		AdvancedJUnitLaunchConfigurationDelegate delegate= new AdvancedJUnitLaunchConfigurationDelegate();
		MockLaunchConfig configuration= new MockLaunchConfig();
		configuration.setProjectName(projectName);
		String testRunnerKind= TestKindRegistry.JUNIT5_TEST_KIND_ID;
		configuration.setTestRunnerKind(testRunnerKind);
		String containerHandle= testSrc1.getHandleIdentifier();
		configuration.setContainerHandle(containerHandle);
		String mode= ILaunchManager.DEBUG_MODE;
		ILaunch launch= new Launch(configuration, mode, null);
		IProgressMonitor progressMonitor= null;
		String showCommandLine= delegate.showCommandLine(configuration, mode, launch, progressMonitor);
		String firstSearchStr= "-testNameFile";
		int indexTestNameFile= showCommandLine.indexOf(firstSearchStr);
		assertThat(indexTestNameFile).overridingErrorMessage("-testNameFile argument not found").isGreaterThan(-1);
		String filePath= extractPathForArgumentFile(showCommandLine, firstSearchStr, indexTestNameFile);
		List<String> fileLines= Files.readAllLines(Paths.get(filePath));
		return fileLines;
	}

	private String extractPathForArgumentFile(String showCommandLine, String firstSearchStr, int indexTestNameFile) {
		String filePath= showCommandLine.substring(indexTestNameFile + firstSearchStr.length() + 1);
		if (Platform.OS_WIN32.equals(Platform.getOS())) {
			filePath = removeQuotationMarks(filePath);
		}
		return filePath;
	}

	private String removeQuotationMarks(String filePath) {
		return filePath.substring(1, filePath.length()-1);
	}

}
