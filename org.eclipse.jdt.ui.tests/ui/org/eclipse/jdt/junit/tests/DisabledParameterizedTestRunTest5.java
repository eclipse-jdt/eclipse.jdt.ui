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

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;

public class DisabledParameterizedTestRunTest5 extends AbstractDisabledParameterizedTestRunSupport {

	@Override
	protected IPath getJUnitContainerPath() {
		return JUnitCore.JUNIT5_CONTAINER_PATH;
	}

	@Override
	protected String getTestKindId() {
		return TestKindRegistry.JUNIT5_TEST_KIND_ID;
	}

	@Override
	protected void addRuntimeLibrary() throws Exception {
		JavaProjectHelper.addRTJar18(fProject);
	}
}
