/*******************************************************************************
 * Copyright (c) 2026 Carsten Hammer and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.leaks;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.module.ModuleFinder;

import org.junit.jupiter.api.Test;

import org.eclipse.jdt.ui.leaktest.reftracker.ReferenceTracker;

public class LeakTestVmArgumentsTest {

	@Test
	public void allSystemModulePackagesAreOpenToLeakTracker() {
		Module targetModule= ReferenceTracker.class.getModule();
		ModuleFinder.ofSystem().findAll().stream()
				.sorted((left, right) -> left.descriptor().name().compareTo(right.descriptor().name()))
				.forEach(reference -> {
					String moduleName= reference.descriptor().name();
					Module module= ModuleLayer.boot().findModule(moduleName)
							.orElseThrow(() -> new AssertionError("System module is not resolved: " + moduleName));
					reference.descriptor().packages().stream().sorted().forEach(packageName ->
						assertTrue(module.isOpen(packageName, targetModule),
								() -> moduleName + "/" + packageName + " is not open to " + targetModule));
				});
	}
}
