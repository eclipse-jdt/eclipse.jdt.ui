/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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
 *     David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.runner;

/**
 * Note that tests may be executed in parallel, so be aware of concurrency issues when implementing
 * this interface.
 */
public interface IListensToTestExecutions {
	void notifyTestFailed(TestReferenceFailure failure);

	void notifyTestStarted(ITestIdentifier test);

	void notifyTestEnded(ITestIdentifier test);
}
