/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllChangeTests {
	private static final Class<AllChangeTests> clazz= AllChangeTests.class;

	public static Test suite() {
		TestSuite suite= new TestSuite(clazz.getName());
		suite.addTest(new TestSuite(RenameResourceChangeTests.class));
		suite.addTest(new TestSuite(MoveRenameResourceChangeTests.class));
		suite.addTest(new TestSuite(RenameSourceFolderChangeTests.class));
		suite.addTest(new TestSuite(CopyPackageChangeTest.class));
		suite.addTest(new TestSuite(CopyPackageChangeTest.class));
		suite.addTest(DocumentChangeTest.suiteWithoutRefactoringTestSetup());
		return new RefactoringTestSetup(suite);
	}
}

