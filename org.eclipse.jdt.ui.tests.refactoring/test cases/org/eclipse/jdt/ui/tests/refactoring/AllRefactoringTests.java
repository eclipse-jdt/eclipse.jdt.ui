/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;


public class AllRefactoringTests {

	private static final Class clazz= AllRefactoringTests.class;

	public static Test suite() {
		TestSuite suite= new TestSuite(clazz.getName());

		//--code
		suite.addTest(ExtractMethodTests.suite());
		suite.addTest(InlineMethodTests.suite());
		suite.addTest(ReplaceInvocationsTests.suite());
		suite.addTest(SefTests.suite());
		suite.addTest(InlineTempTests.suite());
		suite.addTest(ExtractTempTests.suite());
		suite.addTest(RenameTempTests.suite());
		suite.addTest(ExtractConstantTests.suite());
		suite.addTest(PromoteTempToFieldTests.suite());
		suite.addTest(ConvertAnonymousToNestedTests.suite());
		suite.addTest(InlineConstantTests.suite());
		suite.addTest(IntroduceParameterTests.suite());
		suite.addTest(IntroduceFactoryTests.suite());

		//-- structure
		suite.addTest(ChangeSignatureTests.suite());
		suite.addTest(IntroduceParameterObjectTests.suite());
		suite.addTest(PullUpTests.suite());
		suite.addTest(PushDownTests.suite());
		suite.addTest(MoveMembersTests.suite());
		suite.addTest(ExtractInterfaceTests.suite());
		suite.addTest(ExtractSupertypeTests.suite());
		suite.addTest(MoveInnerToTopLevelTests.suite());
		suite.addTest(UseSupertypeWherePossibleTests.suite());
		suite.addTest(ExtractClassTests.suite());

		//-- generics
		suite.addTest(InferTypeArgumentsTests.suite());

		//--methods
		suite.addTest(RenameVirtualMethodInClassTests.suite());
		suite.addTest(RenameMethodInInterfaceTests.suite());
		suite.addTest(RenamePrivateMethodTests.suite());
		suite.addTest(RenameStaticMethodTests.suite());
		suite.addTest(RenameParametersTests.suite());
		suite.addTest(MoveInstanceMethodTests.suite());
		suite.addTest(IntroduceIndirectionTests.suite());

		//--types
		suite.addTest(RenameTypeTests.suite());
		suite.addTest(RenameTypeParameterTests.suite());
		suite.addTest(ChangeTypeRefactoringTests.suite());

		//--packages
		suite.addTest(RenamePackageTests.suite());

		//--fields
		suite.addTest(RenamePrivateFieldTests.suite());
		suite.addTest(RenameNonPrivateFieldTests.suite());

		//--projects
		suite.addTest(RenameJavaProjectTests.suite());

		//--binaries
		suite.addTest(BinaryReferencesTests.suite());

		// validate edit
//		suite.addTest(ValidateEditTests.suite());

		//--helpers
		suite.addTest(RenamingNameSuggestorTests.suite());
		suite.addTest(DelegateCreatorTests.suite());
		return suite;
	}
}

