/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestRunSession;

/**
 * This test run listener is the entry point that makes sure the org.eclipse.jdt.junit
 * plug-in gets loaded when a JUnit launch configuration is launched.
 *
 * @since 3.6
 */
public class UITestRunListener extends TestRunListener {
	/*
	 * @see org.eclipse.jdt.junit.TestRunListener#sessionStarted(org.eclipse.jdt.junit.model.ITestRunSession)
	 */
	public void sessionStarted(ITestRunSession session) {
		JUnitPlugin.asyncShowTestRunnerViewPart();
	}
}
