/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.text.tests.performance;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * Measures the time to replaceAll in a large file with Quick Diff enabled.
 *
 * @since 3.1
 */
public class JavaReplaceAllWithQuickDiffTest extends AbstractJavaReplaceAllTest {

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(JavaReplaceAllWithQuickDiffTest.class));
	}

	protected boolean isQuickDiffEnabled() {
		return true;
	}

	public void test() throws Exception {
		// XXX: Removing from fingerprint as it is unstable
//		setShortName("Replace All in Java editor (with quick diff)");
		super.test();
	}

}
