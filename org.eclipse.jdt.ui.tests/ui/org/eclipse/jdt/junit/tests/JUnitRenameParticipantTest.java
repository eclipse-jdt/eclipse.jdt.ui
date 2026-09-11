/*******************************************************************************
 * Copyright (c) 2026 Carsten Hammer and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer - initial tests
 *******************************************************************************/
package org.eclipse.jdt.junit.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.CreateChangeOperation;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameJavaProjectProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTypeProcessor;
import org.eclipse.jdt.internal.junit.buildpath.BuildPathSupport;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

public class JUnitRenameParticipantTest {

	private IJavaProject fProject;
	private IJavaProject fRenamedProject;
	private IPackageFragmentRoot fSourceFolder;
	private IPackageFragment fPackage;
	private final List<ILaunchConfiguration> fLaunchConfigurations= new ArrayList<>();
	private Change fUndoChange;

	@AfterEach
	public void tearDown() throws Exception {
		if (fUndoChange != null) {
			fUndoChange.dispose();
		}
		for (ILaunchConfiguration configuration : fLaunchConfigurations) {
			configuration.delete();
		}
		if (fProject != null && fProject.exists()) {
			JavaProjectHelper.delete(fProject);
		}
		if (fRenamedProject != null && fRenamedProject.exists()) {
			JavaProjectHelper.delete(fRenamedProject);
		}
	}

	@ParameterizedTest
	@ValueSource(ints= { 4, 5, 6 })
	public void testProjectRenameUpdatesTestContainers(int junitVersion) throws Exception {
		createProject(junitVersion);
		List<IJavaElement> originalContainers= List.of(fProject, fSourceFolder, fPackage);
		for (IJavaElement container : originalContainers) {
			createLaunchConfiguration(container, junitVersion);
		}
		String newName= fProject.getElementName() + "Renamed";
		fRenamedProject= fProject.getJavaModel().getJavaProject(newName);
		assertFalse(fRenamedProject.exists());
		IPackageFragmentRoot renamedSourceFolder= fRenamedProject.getPackageFragmentRoot("src");
		List<IJavaElement> renamedContainers= List.of(fRenamedProject, renamedSourceFolder,
				renamedSourceFolder.getPackageFragment(fPackage.getElementName()));

		RenameJavaProjectProcessor processor= new RenameJavaProjectProcessor(fProject);
		processor.setNewElementName(newName);
		processor.setUpdateReferences(true);
		CreateChangeOperation create= new CreateChangeOperation(
				new CheckConditionsOperation(new RenameRefactoring(processor), CheckConditionsOperation.ALL_CONDITIONS), RefactoringStatus.FATAL);
		PerformChangeOperation perform= new PerformChangeOperation(create);
		Change undo= perform(perform);
		assertTrue(create.getConditionCheckingStatus().isOK(), () -> create.getConditionCheckingStatus().toString());
		assertFalse(fProject.exists());
		assertTestContainers(renamedContainers);

		Change redo= perform(new PerformChangeOperation(undo));
		assertFalse(fRenamedProject.exists());
		assertTestContainers(originalContainers);

		perform(new PerformChangeOperation(redo));
		assertFalse(fProject.exists());
		assertTestContainers(renamedContainers);
	}

	@ParameterizedTest
	@ValueSource(ints= { 4, 5, 6 })
	public void testTypeRenameParticipantIsLoaded(int junitVersion) throws Exception {
		IType type= createProject(junitVersion);
		RenameRefactoring refactoring= new RenameRefactoring(new RenameTypeProcessor(type));
		RefactoringStatus status= new RefactoringStatus();
		RenameParticipant[] participants= ParticipantManager.loadRenameParticipants(status, refactoring.getProcessor(), type,
				new RenameArguments("RenamedTest", true),
				(configuration, participantStatus) -> "org.eclipse.jdt.junit.renameTypeParticipant".equals(configuration.getAttribute("id")),
				new String[] { JavaCore.NATURE_ID }, new SharableParticipants());
		assertTrue(status.isOK(), status::toString);
		// The general debug participant also updates type names, so a successful rename alone
		// would not prove that the JUnit participant's enablement works.
		assertEquals(1, participants.length, "The JUnit type rename participant must be loaded");
	}

	private IType createProject(int junitVersion) throws Exception {
		fProject= JavaProjectHelper.createJavaProject("JUnitRenameParticipantTest" + junitVersion, "bin");
		JavaProjectHelper.addRTJar_17(fProject, false);
		JavaProjectHelper.set17CompilerOptions(fProject, false);
		if (junitVersion == 5) {
			// Use only Jupiter's API dependencies: the JUnit 5 container can also supply
			// JUnit 4, which would hide bug 570024.
			JavaProjectHelper.addToClasspath(fProject, BuildPathSupport.getJUnitJupiterApiLibraryEntry());
			JavaProjectHelper.addToClasspath(fProject, BuildPathSupport.getJUnitPlatformCommonsLibraryEntry());
			JavaProjectHelper.addToClasspath(fProject, BuildPathSupport.getJUnitOpentest4jLibraryEntry());
			JavaProjectHelper.addToClasspath(fProject, BuildPathSupport.getJUnitApiGuardianLibraryEntry());
		} else {
			JavaProjectHelper.addToClasspath(fProject, JavaCore.newContainerEntry(new Path(JUnitCore.JUNIT_CONTAINER_ID).append(Integer.toString(junitVersion))));
		}
		if (junitVersion == 4) {
			assertNotNull(fProject.findType("junit.framework.Test"));
		} else {
			assertNull(fProject.findType("junit.framework.Test"), "This must be a pure JUnit Jupiter project");
			assertNotNull(fProject.findType("org.junit.platform.commons.annotation.Testable"));
		}
		fSourceFolder= JavaProjectHelper.addSourceContainer(fProject, "src");
		fPackage= fSourceFolder.createPackageFragment("tests", true, null);
		String annotation= junitVersion == 4 ? "org.junit.Test" : "org.junit.jupiter.api.Test";
		return fPackage.createCompilationUnit("SampleTest.java", """
				package tests;
				public class SampleTest {
				    @%s
				    public void test() {}
				}
				""".formatted(annotation), true, null).getType("SampleTest");
	}

	private void createLaunchConfiguration(IJavaElement container, int junitVersion) throws CoreException {
		ILaunchManager manager= DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationWorkingCopy configuration= manager.getLaunchConfigurationType(JUnitLaunchConfigurationConstants.ID_JUNIT_APPLICATION)
				.newInstance(null, manager.generateLaunchConfigurationName("JUnit rename " + container.getElementName()));
		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, fProject.getElementName());
		configuration.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER, container.getHandleIdentifier());
		String testKindId= switch (junitVersion) {
			case 4 -> TestKindRegistry.JUNIT4_TEST_KIND_ID;
			case 5 -> TestKindRegistry.JUNIT5_TEST_KIND_ID;
			case 6 -> TestKindRegistry.JUNIT6_TEST_KIND_ID;
			default -> throw new IllegalArgumentException("Unsupported JUnit version: " + junitVersion);
		};
		configuration.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND, testKindId);
		fLaunchConfigurations.add(configuration.doSave());
	}

	private void assertTestContainers(List<IJavaElement> containers) throws CoreException {
		for (int i= 0; i < containers.size(); i++) {
			IJavaElement expected= containers.get(i);
			ILaunchConfiguration configuration= fLaunchConfigurations.get(i);
			assertEquals(expected.getJavaProject().getElementName(), configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""));
			String handle= configuration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER, "");
			assertEquals(expected.getHandleIdentifier(), handle, configuration.getName());
			IJavaElement actual= JavaCore.create(handle);
			assertNotNull(actual);
			assertTrue(actual.exists(), "The launch target must still exist");
		}
	}

	private Change perform(PerformChangeOperation operation) throws CoreException {
		try {
			ResourcesPlugin.getWorkspace().run(operation, new NullProgressMonitor());
		} finally {
			// PerformChangeOperation disposes executed changes, including intermediate undo/redo
			// changes. Keep the unperformed inverse for tearDown(), even if workspace.run fails.
			fUndoChange= operation.getUndoChange();
			if (!operation.changeExecuted() && operation.getChange() != null) {
				operation.getChange().dispose();
			}
		}
		assertTrue(operation.changeExecuted(), () -> String.valueOf(operation.getConditionCheckingStatus()));
		assertFalse(operation.getValidationStatus().hasFatalError(), () -> operation.getValidationStatus().toString());
		assertNotNull(fUndoChange);
		return fUndoChange;
	}
}
