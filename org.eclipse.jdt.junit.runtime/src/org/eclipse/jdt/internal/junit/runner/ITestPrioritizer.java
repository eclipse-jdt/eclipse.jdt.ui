/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
 *     David Saff (saff@mit.edu) - bug 102632: [JUnit] Support for JUnit 4.
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.runner;

import junit.framework.Test;

/**
 * Strategy to prioritize a test suite
 */
public interface ITestPrioritizer {
	/**
	 * Prioritize a test
	 *
	 * @param input tests to be prioritized
	 * @return the prioritized test suite
	 */
	Test prioritize(Test input);
}
