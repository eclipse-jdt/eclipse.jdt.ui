/*******************************************************************************
 * Copyright (c) 2026 Hélios GILLES and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hélios GILLES - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.junit.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationTab;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.util.DialogCheck;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;

import org.eclipse.debug.ui.ILaunchConfigurationDialog;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.core.JavaModelManager;

import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

/**
 * Tests the JUnit tab of a launch configuration: the first lookup of a type in a project reads
 * the packages of its libraries, which takes seconds in a large project, not in the UI thread.
 */
public class JUnitLaunchConfigurationTabTest {

	private IJavaProject fProject;
	private Shell fShell;
	private JUnitLaunchConfigurationTab fTab;

	@BeforeEach
	public void setUp() throws Exception {
		fProject= JavaProjectHelper.createJavaProject("TestProject", "bin");
		JavaProjectHelper.addRTJar(fProject);
		JavaProjectHelper.addToClasspath(fProject, JavaCore.newContainerEntry(JUnitCore.JUNIT4_CONTAINER_PATH));
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fProject, "src");
		IPackageFragment p= root.createPackageFragment("p", true, null);
		p.createCompilationUnit("FooTest.java", """
				package p;
				import org.junit.Test;
				public class FooTest {
					@Test
					public void first() {
					}
					@Test
					public void second() {
					}
				}
				""", true, null);
		fShell= new Shell(DialogCheck.getShell());
		fTab= new JUnitLaunchConfigurationTab();
		fTab.createControl(fShell);
	}

	@AfterEach
	public void tearDown() throws Exception {
		fTab.dispose();
		fShell.dispose();
		JavaProjectHelper.delete(fProject);
	}

	@Test
	public void testTypesAreReadOutOfTheUIThread() throws Exception {
		ILaunchConfigurationWorkingCopy config= newConfiguration("first");
		// the next lookup of a type in the project reads the packages of its libraries again
		fProject.close();
		fProject.open(null);
		assertFalse(hasTypesOfLibraries());

		List<String> runs= recordRuns();
		fTab.initializeFrom(config);

		assertEquals(List.of(
				"cancelable=false, types of the project known before: false", // the packages of the project
				"cancelable=true, types of the project known before: true"), // the test methods
				runs, "the UI thread does not look for the types of the project");
		assertTrue(fTab.isValid(config), fTab.getErrorMessage());

		runs.clear();
		fTab.initializeFrom(newConfiguration("second"));
		assertEquals(List.of(), runs, "the packages are read once, and the test methods are known");
	}

	@Test
	public void testTypesAreReadOutOfTheUIThreadInAProjectNotOpen() throws Exception {
		ILaunchConfigurationWorkingCopy config= newConfiguration("first");
		// as after a restart: the project is not open in the Java model
		fProject.close();
		assertFalse(fProject.isOpen());

		List<String> runs= recordRuns();
		fTab.initializeFrom(config);

		assertEquals("cancelable=false, types of the project known before: false", runs.get(0));
		assertTrue(fTab.isValid(config), fTab.getErrorMessage());
	}

	@Test
	public void testUnknownTestMethodIsAnError() throws Exception {
		ILaunchConfigurationWorkingCopy config= newConfiguration("third");
		recordRuns();

		fTab.initializeFrom(config);

		assertFalse(fTab.isValid(config));
		assertNotNull(fTab.getErrorMessage());
	}

	/**
	 * @return the runs of the launch configuration dialog of the tab, with whether the types of the
	 *         project were known before each one
	 */
	private List<String> recordRuns() {
		List<String> runs= new ArrayList<>();
		fTab.setLaunchConfigurationDialog(dialog((fork, cancelable, runnable) -> {
			assertTrue(fork, "out of the UI thread");
			runs.add("cancelable=" + cancelable + ", types of the project known before: " + hasTypesOfLibraries());
			runInAnotherThread(runnable);
		}));
		return runs;
	}

	private ILaunchConfigurationWorkingCopy newConfiguration(String methodName) throws Exception {
		ILaunchManager manager= DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationWorkingCopy config= manager.getLaunchConfigurationType(JUnitLaunchConfigurationConstants.ID_JUNIT_APPLICATION)
				.newInstance(null, manager.generateLaunchConfigurationName("JUnitLaunchConfigurationTabTest"));
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, fProject.getElementName());
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "p.FooTest");
		config.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_NAME, methodName);
		config.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND, TestKindRegistry.JUNIT4_TEST_KIND_ID);
		return config;
	}

	/**
	 * @return whether the project knows the packages of its libraries, which its first lookup of a
	 *         type reads
	 */
	private boolean hasTypesOfLibraries() {
		Object info= JavaModelManager.getJavaModelManager().getInfo(fProject);
		if (info == null) {
			return false;
		}
		try {
			for (String name : new String[] { "projectCache", "mainProjectCache" }) {
				Field projectCache= info.getClass().getDeclaredField(name);
				projectCache.setAccessible(true);
				if (projectCache.get(info) != null) {
					return true;
				}
			}
			return false;
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private interface Run {
		void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException;
	}

	/**
	 * @return a launch configuration dialog whose runnable context is the given one
	 */
	private static ILaunchConfigurationDialog dialog(Run run) {
		return (ILaunchConfigurationDialog) Proxy.newProxyInstance(JUnitLaunchConfigurationTabTest.class.getClassLoader(),
				new Class<?>[] { ILaunchConfigurationDialog.class }, (_, method, arguments) -> {
					if ("run".equals(method.getName())) {
						run.run((Boolean) arguments[0], (Boolean) arguments[1], (IRunnableWithProgress) arguments[2]);
					} else if ("getMode".equals(method.getName())) {
						return ILaunchManager.RUN_MODE;
					}
					return null;
				});
	}

	/**
	 * Runs the given runnable in another thread, as the launch configuration dialog does.
	 */
	private static void runInAnotherThread(IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException {
		assertNotNull(Display.getCurrent(), "called in the UI thread");
		AtomicReference<Throwable> failure= new AtomicReference<>();
		Thread thread= new Thread(() -> {
			try {
				runnable.run(new NullProgressMonitor());
			} catch (Throwable e) {
				failure.set(e);
			}
		});
		thread.start();
		thread.join();
		if (failure.get() instanceof InvocationTargetException e) {
			throw e;
		} else if (failure.get() instanceof InterruptedException e) {
			throw e;
		} else if (failure.get() != null) {
			throw new AssertionError("failed out of the UI thread", failure.get());
		}
	}
}
