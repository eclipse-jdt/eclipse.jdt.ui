/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
	InlineTempTests1d8.class,
	InlineConstantTests1d8.class,

	//--code
	ExtractMethodTests.class,
	ExtractMethodTests1d7.class,
	ExtractMethodTests1d8.class,
	InlineMethodTests.class,
	InlineMethodTests1d8.class,
	InlineMethodTests16.class,
	ReplaceInvocationsTests.class,
	SefTests.class,
	InlineTempTests.class,
	InlineTempTests1d7.class,
	ExtractTempTests.class,
	ExtractTempTests1d7.class,
	ExtractTempTests1d8.class,
	RenameTempTests.class,
	ExtractConstantTests.class,
	PromoteTempToFieldTests.class,
	PromoteTempToFieldTests1d8.class,
	ConvertAnonymousToNestedTests.class,
	ConvertAnonymousToNestedTests1d8.class,
	ConvertAnonymousToNestedTests9.class,
	InlineConstantTests.class,
	InlineConstantTests1d7.class,
	IntroduceParameterTests.class,
	IntroduceParameterTests1d7.class,
	IntroduceFactoryTests.class,
	IntroduceFactoryTests16.class,

	//-- structure
	ChangeSignatureTests.class,
	ChangeSignatureTests1d8.class,
	ChangeSignatureTests16.class,
	IntroduceParameterObjectTests.class,
	PullUpTests.class,
	PullUpTests1d8.class,
	PushDownTests.class,
	MoveMembersTests.class,
	MoveMembersTests1d8.class,
	ExtractInterfaceTests.class,
	ExtractInterfaceTests1d8.class,
	ExtractSupertypeTests.class,
	MoveInnerToTopLevelTests.class,
	MoveInnerToTopLevelTests16.class,
	MoveInnerToNewTests16.class,
	UseSupertypeWherePossibleTests.class,
	UseSupertypeWherePossibleTests16.class,
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
	MoveInstanceMethodTests1d8.class,
	IntroduceIndirectionTests.class,
	IntroduceIndirectionTests1d7.class,
	IntroduceIndirectionTests1d8.class,

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

