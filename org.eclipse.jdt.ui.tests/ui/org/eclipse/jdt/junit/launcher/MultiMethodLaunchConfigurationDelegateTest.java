/*******************************************************************************
 * Copyright (c) 2026 Microsoft Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.jdt.junit.launcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;

/**
 * Tests for the multi-method extension to {@code -testNameFile} introduced to allow
 * an arbitrary set of methods (potentially across multiple classes) to be executed
 * inside a single launch.
 *
 * <p>The change lives in the private {@code createTestNamesFile(IJavaElement[])}
 * helper of {@link JUnitLaunchConfigurationDelegate}, reached through
 * {@code collectExecutionArguments(...)} when {@code fTestElements.length > 1}.
 * The default {@code evaluateTests(...)} in the delegate never produces multiple
 * {@code IMethod} entries, so each test below subclasses the delegate and feeds a
 * synthetic {@code IMember[]} via an override, then asserts on the file emitted
 * after {@code showCommandLine(...)}.</p>
 */
public class MultiMethodLaunchConfigurationDelegateTest {

	private IJavaProject fJavaProject;

	@AfterEach
	public void deleteProject() throws CoreException {
		if (fJavaProject != null) {
			JavaProjectHelper.delete(fJavaProject);
		}
	}

	@Test
	public void multipleMethodsInSameClassEmitClassColonMethodLines() throws Exception {
		String projectName= "JUnitLaunchConfigurationDelegate-MultiMethod-SameClass";
		fJavaProject= createJUnit5Project(projectName);
		IPackageFragmentRoot src= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		IPackageFragment p1= src.createPackageFragment("p1", true, null);
		ICompilationUnit cu= p1.createCompilationUnit("FooTest.java", """
				package p1;
				public class FooTest {
					@org.junit.jupiter.api.Test public void a() { }
					@org.junit.jupiter.api.Test public void b() { }
					@org.junit.jupiter.api.Test public void c() { }
				}
				""", true, null);
		IType fooType= cu.getType("FooTest");
		IMember[] members= {
				fooType.getMethod("a", new String[0]),
				fooType.getMethod("b", new String[0]),
				fooType.getMethod("c", new String[0])
		};

		List<String> lines= showCommandLineAndReadTestNamesFile(fJavaProject, fooType, members);

		assertThat(lines).containsExactlyInAnyOrder("p1.FooTest:a", "p1.FooTest:b", "p1.FooTest:c");
	}

	@Test
	public void methodsAcrossMultipleClassesEmitClassColonMethodLines() throws Exception {
		String projectName= "JUnitLaunchConfigurationDelegate-MultiMethod-CrossClass";
		fJavaProject= createJUnit5Project(projectName);
		IPackageFragmentRoot src= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		IPackageFragment p1= src.createPackageFragment("p1", true, null);
		ICompilationUnit fooCu= p1.createCompilationUnit("FooTest.java", """
				package p1;
				public class FooTest {
					@org.junit.jupiter.api.Test public void a() { }
				}
				""", true, null);
		ICompilationUnit barCu= p1.createCompilationUnit("BarTest.java", """
				package p1;
				public class BarTest {
					@org.junit.jupiter.api.Test public void b() { }
				}
				""", true, null);
		IType fooType= fooCu.getType("FooTest");
		IType barType= barCu.getType("BarTest");
		IMember[] members= {
				fooType.getMethod("a", new String[0]),
				barType.getMethod("b", new String[0])
		};

		List<String> lines= showCommandLineAndReadTestNamesFile(fJavaProject, fooType, members);

		assertThat(lines).containsExactlyInAnyOrder("p1.FooTest:a", "p1.BarTest:b");
	}

	@Test
	public void mixedTypeAndMethodEmitLegacyAndExtendedLines() throws Exception {
		String projectName= "JUnitLaunchConfigurationDelegate-MultiMethod-Mixed";
		fJavaProject= createJUnit5Project(projectName);
		IPackageFragmentRoot src= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		IPackageFragment p1= src.createPackageFragment("p1", true, null);
		ICompilationUnit fooCu= p1.createCompilationUnit("FooTest.java", """
				package p1;
				public class FooTest {
					@org.junit.jupiter.api.Test public void a() { }
				}
				""", true, null);
		ICompilationUnit barCu= p1.createCompilationUnit("BarTest.java", """
				package p1;
				public class BarTest {
					@org.junit.jupiter.api.Test public void onlyOne() { }
				}
				""", true, null);
		IType fooType= fooCu.getType("FooTest");
		IType barType= barCu.getType("BarTest");
		IMember[] members= {
				fooType.getMethod("a", new String[0]),
				barType
		};

		List<String> lines= showCommandLineAndReadTestNamesFile(fJavaProject, fooType, members);

		assertThat(lines).containsExactlyInAnyOrder("p1.FooTest:a", "p1.BarTest");
	}

	@Test
	public void multipleTypesStillEmitLegacyClassOnlyLines() throws Exception {
		String projectName= "JUnitLaunchConfigurationDelegate-MultiMethod-LegacyClassOnly";
		fJavaProject= createJUnit5Project(projectName);
		IPackageFragmentRoot src= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		IPackageFragment p1= src.createPackageFragment("p1", true, null);
		ICompilationUnit fooCu= p1.createCompilationUnit("FooTest.java", """
				package p1;
				public class FooTest {
					@org.junit.jupiter.api.Test public void a() { }
				}
				""", true, null);
		ICompilationUnit barCu= p1.createCompilationUnit("BarTest.java", """
				package p1;
				public class BarTest {
					@org.junit.jupiter.api.Test public void b() { }
				}
				""", true, null);
		IType fooType= fooCu.getType("FooTest");
		IType barType= barCu.getType("BarTest");
		IMember[] members= { fooType, barType };

		List<String> lines= showCommandLineAndReadTestNamesFile(fJavaProject, fooType, members);

		assertThat(lines).containsExactlyInAnyOrder("p1.FooTest", "p1.BarTest");
	}

	private IJavaProject createJUnit5Project(String projectName) throws CoreException {
		IJavaProject javaProject= JavaProjectHelper.createJavaProject(projectName, "bin");
		JavaProjectHelper.addRTJar18(javaProject);
		IClasspathEntry cpe= JavaCore.newContainerEntry(JUnitCore.JUNIT5_CONTAINER_PATH);
		JavaProjectHelper.addToClasspath(javaProject, cpe);
		JavaProjectHelper.set18CompilerOptions(javaProject);
		return javaProject;
	}

	private List<String> showCommandLineAndReadTestNamesFile(IJavaProject project, IType anchorType, IMember[] testMembers)
			throws CoreException, IOException {
		AdvancedJUnitLaunchConfigurationDelegate delegate= new AdvancedJUnitLaunchConfigurationDelegate() {
			@Override
			protected IMember[] evaluateTests(ILaunchConfiguration configuration, IProgressMonitor monitor) {
				return testMembers;
			}
		};
		MockLaunchConfig configuration= new MockLaunchConfig();
		configuration.setProjectName(project.getElementName());
		configuration.setTestRunnerKind(TestKindRegistry.JUNIT5_TEST_KIND_ID);
		configuration.setContainerHandle(anchorType.getHandleIdentifier());
		String mode= ILaunchManager.DEBUG_MODE;
		ILaunch launch= new Launch(configuration, mode, null);
		String showCommandLine= delegate.showCommandLine(configuration, mode, launch, new NullProgressMonitor());
		int idx= showCommandLine.indexOf("-testNameFile");
		assertThat(idx).overridingErrorMessage("-testNameFile argument not found in: %s", showCommandLine).isGreaterThan(-1);
		String filePath= showCommandLine.substring(idx + "-testNameFile".length() + 1);
		if (Platform.OS.isWindows()) {
			filePath= filePath.substring(1, filePath.length() - 1);
		}
		return Files.readAllLines(Paths.get(filePath));
	}

}
