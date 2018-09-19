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
 *******************************************************************************/

package org.eclipse.jdt.text.tests.performance;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * Measures the time to replaceAll in a large file with Quick Diff disabled.
 *
 * @since 3.1
 */
public class JavaReplaceAllTest extends AbstractJavaReplaceAllTest {

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(JavaReplaceAllTest.class));
	}

	@Override
	protected boolean isQuickDiffEnabled() {
		return false;
	}

}
