/*******************************************************************************
 * Copyright (c) 2026 Simeon Andreev and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simeon Andreev - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class LogTestListener implements TestExecutionListener {

    @Override
    public void executionStarted(TestIdentifier id) {
    	String name = id.getDisplayName();
		JavaPlugin.log(Status.info(name + " STARTING"));
    }

    @Override
    public void executionFinished(TestIdentifier id, TestExecutionResult result) {
    	String name = id.getDisplayName();
		JavaPlugin.log(Status.info(name + " DONE: " + result.getStatus()));
    }
}