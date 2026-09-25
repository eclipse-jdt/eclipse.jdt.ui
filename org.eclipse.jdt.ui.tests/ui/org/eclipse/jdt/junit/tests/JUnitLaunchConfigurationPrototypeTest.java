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
 *     Carsten Hammer - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.junit.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;

import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.PrototypeTab;

import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.internal.junit.launcher.JUnitTabGroup;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

public class JUnitLaunchConfigurationPrototypeTest {

	private static final String VM_ARGUMENTS= "-ea -Dprototype=true";
	private static final Map<String, String> ENVIRONMENT= Map.of("SWT_GTK3", "1", "GDK_BACKEND", "x11");

	private final List<ILaunchConfiguration> fConfigurations= new ArrayList<>();
	private ILaunchManager fLaunchManager;
	private ILaunchConfigurationType fType;

	@BeforeEach
	public void setUp() {
		fLaunchManager= DebugPlugin.getDefault().getLaunchManager();
		fType= fLaunchManager.getLaunchConfigurationType(JUnitLaunchConfigurationConstants.ID_JUNIT_APPLICATION);
		assertNotNull(fType);
	}

	@AfterEach
	public void tearDown() throws CoreException {
		// Delete linked configurations before their prototypes.
		for (int i= fConfigurations.size() - 1; i >= 0; i--) {
			ILaunchConfiguration configuration= fConfigurations.get(i);
			if (configuration.exists()) {
				configuration.delete();
			}
		}
	}

	@Test
	public void testSupportsPrototypes() throws CoreException {
		assertTrue(fType.supportsPrototypes());
		ILaunchConfiguration prototype= createPrototype();
		assertTrue(prototype.isPrototype());
		assertTrue(Arrays.asList(fType.getPrototypes()).contains(prototype));
	}

	@Test
	public void testPrototypeAttributeVisibility() throws CoreException {
		ILaunchConfiguration prototype= createPrototype();
		Set<String> visibleAttributes= prototype.getPrototypeVisibleAttributes();
		assertTrue(visibleAttributes.contains(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS));
		assertTrue(visibleAttributes.contains(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES));
		assertFalse(visibleAttributes.contains(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME));
		assertFalse(visibleAttributes.contains(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME));
		assertFalse(visibleAttributes.contains(JUnitLaunchConfigurationConstants.ATTR_TEST_NAME));
		assertFalse(visibleAttributes.contains(JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND));
	}

	@Test
	public void testPrototypeTabForRunAndDebug() {
		for (String mode : new String[] { ILaunchManager.RUN_MODE, ILaunchManager.DEBUG_MODE }) {
			JUnitTabGroup tabGroup= new JUnitTabGroup();
			try {
				tabGroup.createTabs(null, mode);
				ILaunchConfigurationTab[] tabs= tabGroup.getTabs();
				assertEquals(1L, Arrays.stream(tabs).filter(PrototypeTab.class::isInstance).count());
				assertInstanceOf(PrototypeTab.class, tabs[tabs.length - 1]);
			} finally {
				tabGroup.dispose();
			}
		}
	}

	@Test
	public void testLinkCopiesOnlySharedAttributes() throws CoreException {
		ILaunchConfiguration prototype= createPrototype();
		ILaunchConfigurationWorkingCopy workingCopy= newTestConfiguration();
		workingCopy.setPrototype(prototype, true);
		ILaunchConfiguration configuration= save(workingCopy);

		assertFalse(configuration.isPrototype());
		assertEquals(prototype, configuration.getPrototype());
		assertSharedValues(configuration, VM_ARGUMENTS, ENVIRONMENT);
		assertTestSelection(configuration);
	}

	@Test
	public void testLocalOverridesAndReset() throws CoreException {
		ILaunchConfiguration prototype= createPrototype();
		ILaunchConfigurationWorkingCopy workingCopy= newTestConfiguration();
		workingCopy.setPrototype(prototype, true);
		workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, "-Dlocal=true");
		workingCopy.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, Map.of("LOCAL_ONLY", "true"));
		ILaunchConfiguration configuration= save(workingCopy);
		assertSharedValues(configuration, "-Dlocal=true", Map.of("LOCAL_ONLY", "true"));
		assertTrue(configuration.isAttributeModified(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS));
		assertTrue(configuration.isAttributeModified(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES));

		workingCopy= configuration.getWorkingCopy();
		// Use the same operation as the Prototype tab's reset action.
		workingCopy.setPrototype(configuration.getPrototype(), true);
		configuration= save(workingCopy);
		assertSharedValues(configuration, VM_ARGUMENTS, ENVIRONMENT);
		assertFalse(configuration.isAttributeModified(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS));
		assertFalse(configuration.isAttributeModified(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES));
		assertTestSelection(configuration);
	}

	@Test
	public void testLinkWithoutCopyPreservesLocalValues() throws CoreException {
		ILaunchConfiguration prototype= createPrototype();
		ILaunchConfigurationWorkingCopy workingCopy= newTestConfiguration();
		workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, "-Dlocal=true");
		workingCopy.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, Map.of("LOCAL_ONLY", "true"));
		workingCopy.setPrototype(prototype, false);
		ILaunchConfiguration configuration= save(workingCopy);

		assertEquals(prototype, configuration.getPrototype());
		assertSharedValues(configuration, "-Dlocal=true", Map.of("LOCAL_ONLY", "true"));
		assertTestSelection(configuration);
	}

	@Test
	public void testUnlinkPreservesValues() throws CoreException {
		ILaunchConfiguration prototype= createPrototype();
		ILaunchConfigurationWorkingCopy workingCopy= newTestConfiguration();
		workingCopy.setPrototype(prototype, true);
		ILaunchConfiguration configuration= save(workingCopy);

		workingCopy= configuration.getWorkingCopy();
		workingCopy.setPrototype(null, false);
		configuration= save(workingCopy);
		assertNull(configuration.getPrototype());
		assertSharedValues(configuration, VM_ARGUMENTS, ENVIRONMENT);
		assertTestSelection(configuration);
	}

	@Test
	public void testResetUsesUpdatedPrototypeValues() throws CoreException {
		ILaunchConfiguration prototype= createPrototype();
		ILaunchConfigurationWorkingCopy workingCopy= newTestConfiguration();
		workingCopy.setPrototype(prototype, true);
		ILaunchConfiguration configuration= save(workingCopy);

		ILaunchConfigurationWorkingCopy prototypeCopy= prototype.getWorkingCopy();
		prototypeCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, "-Dupdated=true");
		Map<String, String> updatedEnvironment= Map.of("GDK_BACKEND", "wayland");
		prototypeCopy.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, updatedEnvironment);
		prototype= save(prototypeCopy);

		workingCopy= configuration.getWorkingCopy();
		workingCopy.setPrototype(prototype, true);
		configuration= save(workingCopy);
		assertSharedValues(configuration, "-Dupdated=true", updatedEnvironment);
		assertTestSelection(configuration);
	}

	@Test
	public void testJUnitAttributesCanBeShared() throws CoreException {
		ILaunchConfigurationWorkingCopy prototypeCopy= createPrototype().getWorkingCopy();
		prototypeCopy.setAttribute(JUnitLaunchConfigurationConstants.ATTR_KEEPRUNNING, true);
		prototypeCopy.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_HAS_INCLUDE_TAGS, true);
		prototypeCopy.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_INCLUDE_TAGS, "fast");
		prototypeCopy.setPrototypeAttributeVisibility(JUnitLaunchConfigurationConstants.ATTR_KEEPRUNNING, true);
		prototypeCopy.setPrototypeAttributeVisibility(JUnitLaunchConfigurationConstants.ATTR_TEST_HAS_INCLUDE_TAGS, true);
		prototypeCopy.setPrototypeAttributeVisibility(JUnitLaunchConfigurationConstants.ATTR_TEST_INCLUDE_TAGS, true);
		ILaunchConfiguration prototype= save(prototypeCopy);

		ILaunchConfigurationWorkingCopy workingCopy= newTestConfiguration();
		workingCopy.setPrototype(prototype, true);
		ILaunchConfiguration configuration= save(workingCopy);
		assertTrue(configuration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_KEEPRUNNING, false));
		assertTrue(configuration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_HAS_INCLUDE_TAGS, false));
		assertEquals("fast", configuration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_INCLUDE_TAGS, ""));
		assertTestSelection(configuration);
	}

	private ILaunchConfiguration createPrototype() throws CoreException {
		String name= fLaunchManager.generateLaunchConfigurationName("JUnit prototype test");
		ILaunchConfigurationWorkingCopy prototype= fType.newPrototypeInstance(null, name);
		prototype.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, VM_ARGUMENTS);
		prototype.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, ENVIRONMENT);
		prototype.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "PrototypeProject");
		prototype.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "example.PrototypeTest");
		prototype.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_NAME, "prototypeMethod");
		prototype.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND, TestKindRegistry.JUNIT3_TEST_KIND_ID);
		// Initialize the lazily populated visibility set before changing individual entries.
		prototype.getPrototypeVisibleAttributes();
		prototype.setPrototypeAttributeVisibility(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, true);
		prototype.setPrototypeAttributeVisibility(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, true);
		prototype.setPrototypeAttributeVisibility(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, false);
		prototype.setPrototypeAttributeVisibility(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, false);
		prototype.setPrototypeAttributeVisibility(JUnitLaunchConfigurationConstants.ATTR_TEST_NAME, false);
		prototype.setPrototypeAttributeVisibility(JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND, false);
		return save(prototype);
	}

	private ILaunchConfigurationWorkingCopy newTestConfiguration() throws CoreException {
		String name= fLaunchManager.generateLaunchConfigurationName("JUnit linked configuration test");
		ILaunchConfigurationWorkingCopy configuration= fType.newInstance(null, name);
		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "TestProject");
		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "example.MyTest");
		configuration.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_NAME, "testMethod");
		configuration.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND, TestKindRegistry.JUNIT4_TEST_KIND_ID);
		return configuration;
	}

	private ILaunchConfiguration save(ILaunchConfigurationWorkingCopy workingCopy) throws CoreException {
		ILaunchConfiguration configuration= workingCopy.doSave();
		if (!fConfigurations.contains(configuration)) {
			fConfigurations.add(configuration);
		}
		return configuration;
	}

	private void assertSharedValues(ILaunchConfiguration configuration, String vmArguments, Map<String, String> environment) throws CoreException {
		assertEquals(vmArguments, configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, ""));
		assertEquals(environment, configuration.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, Map.<String, String>of()));
	}

	private void assertTestSelection(ILaunchConfiguration configuration) throws CoreException {
		assertEquals("TestProject", configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""));
		assertEquals("example.MyTest", configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, ""));
		assertEquals("testMethod", configuration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_NAME, ""));
		assertEquals(TestKindRegistry.JUNIT4_TEST_KIND_ID, configuration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND, ""));
	}
}
