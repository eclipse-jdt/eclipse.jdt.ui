/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	RenameTests18.class,
	InlineTempTests18.class,
	InlineConstantTests18.class,

	//--code
	ExtractMethodTests.class,
	ExtractMethodTests1d7.class,
	ExtractMethodTests18.class,
	InlineMethodTests.class,
	InlineMethodTests18.class,
	ReplaceInvocationsTests.class,
	SefTests.class,
	InlineTempTests.class,
	InlineTempTests1d7.class,
	ExtractTempTests.class,
	ExtractTempTests1d7.class,
	ExtractTempTests18.class,
	RenameTempTests.class,
	ExtractConstantTests.class,
	PromoteTempToFieldTests.class,
	PromoteTempToFieldTests18.class,
	ConvertAnonymousToNestedTests.class,
	ConvertAnonymousToNestedTests18.class,
	InlineConstantTests.class,
	InlineConstantTests1d7.class,
	IntroduceParameterTests.class,
	IntroduceParameterTests1d7.class,
	IntroduceFactoryTests.class,

	//-- structure
	ChangeSignatureTests.class,
	ChangeSignatureTests18.class,
	IntroduceParameterObjectTests.class,
	PullUpTests.class,
	PullUpTests18.class,
	PushDownTests.class,
	MoveMembersTests.class,
	MoveMembersTests18.class,
	ExtractInterfaceTests.class,
	ExtractInterfaceTests18.class,
	ExtractSupertypeTests.class,
	MoveInnerToTopLevelTests.class,
	UseSupertypeWherePossibleTests.class,
	ExtractClassTests.class,

	//-- generics
	InferTypeArgumentsTests.class,

	//--methods
	RenameVirtualMethodInClassTests.class,
	RenameMethodInInterfaceTests.class,
	RenamePrivateMethodTests.class,
	RenameStaticMethodTests.class,
	RenameParametersTests.class,
	MoveInstanceMethodTests.class,
	MoveInstanceMethodTests18.class,
	IntroduceIndirectionTests.class,
	IntroduceIndirectionTests1d7.class,
	IntroduceIndirectionTests18.class,

	//--types
	RenameTypeTests.class,
	RenameTypeParameterTests.class,
	ChangeTypeRefactoringTests.class,
	ChangeTypeRefactoringTests1d7.class,

	//--packages
	RenamePackageTests.class,

	//--fields
	RenamePrivateFieldTests.class,
	RenameNonPrivateFieldTests.class,
	RenameRecordElementsTests.class,

	//--initializers
	MoveInitializerTests.class,

	//--projects
	RenameJavaProjectTests.class,

	//--binaries
	BinaryReferencesTests.class,

	// validate edit
//	ValidateEditTests.class,

	//--helpers
	RenamingNameSuggestorTests.class,
	DelegateCreatorTests.class
})
public class AllRefactoringTests {
}

