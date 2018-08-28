/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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


public class AllRefactoringTests {

	private static final Class<AllRefactoringTests> clazz= AllRefactoringTests.class;

	public static Test suite() {
		TestSuite suite= new TestSuite(clazz.getName());

		suite.addTest(RenameTests18.suite());
		suite.addTest(InlineTempTests18.suite());
		suite.addTest(InlineConstantTests18.suite());

		//--code
		suite.addTest(ExtractMethodTests.suite());
		suite.addTest(ExtractMethodTests17.suite());
		suite.addTest(ExtractMethodTests18.suite());
		suite.addTest(InlineMethodTests.suite());
		suite.addTest(InlineMethodTests18.suite());
		suite.addTest(ReplaceInvocationsTests.suite());
		suite.addTest(SefTests.suite());
		suite.addTest(InlineTempTests.suite());
		suite.addTest(InlineTempTests17.suite());
		suite.addTest(ExtractTempTests.suite());
		suite.addTest(ExtractTempTests17.suite());
		suite.addTest(ExtractTempTests18.suite());
		suite.addTest(RenameTempTests.suite());
		suite.addTest(ExtractConstantTests.suite());
		suite.addTest(PromoteTempToFieldTests.suite());
		suite.addTest(PromoteTempToFieldTests18.suite());
		suite.addTest(ConvertAnonymousToNestedTests.suite());
		suite.addTest(ConvertAnonymousToNestedTests18.suite());
		suite.addTest(InlineConstantTests.suite());
		suite.addTest(InlineConstantTests17.suite());
		suite.addTest(IntroduceParameterTests.suite());
		suite.addTest(IntroduceParameterTests17.suite());
		suite.addTest(IntroduceFactoryTests.suite());

		//-- structure
		suite.addTest(ChangeSignatureTests.suite());
		suite.addTest(ChangeSignatureTests18.suite());
		suite.addTest(IntroduceParameterObjectTests.suite());
		suite.addTest(PullUpTests.suite());
		suite.addTest(PullUpTests18.suite());
		suite.addTest(PushDownTests.suite());
		suite.addTest(MoveMembersTests.suite());
		suite.addTest(MoveMembersTests18.suite());
		suite.addTest(ExtractInterfaceTests.suite());
		suite.addTest(ExtractInterfaceTests18.suite());
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
		suite.addTest(MoveInstanceMethodTests18.suite());
		suite.addTest(IntroduceIndirectionTests.suite());
		suite.addTest(IntroduceIndirectionTests17.suite());
		suite.addTest(IntroduceIndirectionTests18.suite());

		//--types
		suite.addTest(RenameTypeTests.suite());
		suite.addTest(RenameTypeParameterTests.suite());
		suite.addTest(ChangeTypeRefactoringTests.suite());
		suite.addTest(ChangeTypeRefactoringTests17.suite());

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

