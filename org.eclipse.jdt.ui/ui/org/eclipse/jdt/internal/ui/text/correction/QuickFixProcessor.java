/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Muskalla <b.muskalla@gmx.net> - [quick fix] Quick fix for missing synchronized modifier - https://bugs.eclipse.org/bugs/show_bug.cgi?id=245250
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;

import org.eclipse.jdt.internal.ui.text.correction.proposals.ReplaceCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.TaskMarkerProposal;

/**
  */
public class QuickFixProcessor implements IQuickFixProcessor {


	public boolean hasCorrections(ICompilationUnit cu, int problemId) {
		switch (problemId) {
			case IProblem.UnterminatedString:
			case IProblem.UnusedImport:
			case IProblem.DuplicateImport:
			case IProblem.CannotImportPackage:
			case IProblem.ConflictingImport:
			case IProblem.ImportNotFound:
			case IProblem.UndefinedMethod:
			case IProblem.UndefinedConstructor:
			case IProblem.ParameterMismatch:
			case IProblem.MethodButWithConstructorName:
			case IProblem.UndefinedField:
			case IProblem.UndefinedName:
			case IProblem.PublicClassMustMatchFileName:
			case IProblem.PackageIsNotExpectedPackage:
			case IProblem.UndefinedType:
			case IProblem.TypeMismatch:
			case IProblem.UnhandledException:
			case IProblem.UnreachableCatch:
			case IProblem.InvalidCatchBlockSequence:
			case IProblem.VoidMethodReturnsValue:
			case IProblem.ShouldReturnValue:
			case IProblem.MissingReturnType:
			case IProblem.NonExternalizedStringLiteral:
			case IProblem.NonStaticAccessToStaticField:
			case IProblem.NonStaticAccessToStaticMethod:
			case IProblem.StaticMethodRequested:
			case IProblem.NonStaticFieldFromStaticInvocation:
			case IProblem.InstanceMethodDuringConstructorInvocation:
			case IProblem.InstanceFieldDuringConstructorInvocation:
			case IProblem.NotVisibleMethod:
			case IProblem.NotVisibleConstructor:
			case IProblem.NotVisibleType:
			case IProblem.NotVisibleField:
			case IProblem.BodyForAbstractMethod:
			case IProblem.AbstractMethodInAbstractClass:
			case IProblem.AbstractMethodMustBeImplemented:
			case IProblem.EnumAbstractMethodMustBeImplemented:
			case IProblem.AbstractMethodsInConcreteClass:
			case IProblem.AbstractMethodInEnum:
			case IProblem.EnumConstantMustImplementAbstractMethod:
			case IProblem.ShouldImplementHashcode:
			case IProblem.BodyForNativeMethod:
			case IProblem.OuterLocalMustBeFinal:
			case IProblem.UninitializedLocalVariable:
			case IProblem.UndefinedConstructorInDefaultConstructor:
			case IProblem.UnhandledExceptionInDefaultConstructor:
			case IProblem.NotVisibleConstructorInDefaultConstructor:
			case IProblem.AmbiguousType:
			case IProblem.UnusedPrivateMethod:
			case IProblem.UnusedPrivateConstructor:
			case IProblem.UnusedPrivateField:
			case IProblem.UnusedPrivateType:
			case IProblem.LocalVariableIsNeverUsed:
			case IProblem.ArgumentIsNeverUsed:
			case IProblem.MethodRequiresBody:
			case IProblem.NeedToEmulateFieldReadAccess:
			case IProblem.NeedToEmulateFieldWriteAccess:
			case IProblem.NeedToEmulateMethodAccess:
			case IProblem.NeedToEmulateConstructorAccess:
			case IProblem.SuperfluousSemicolon:
			case IProblem.UnnecessaryCast:
			case IProblem.UnnecessaryInstanceof:
			case IProblem.IndirectAccessToStaticField:
			case IProblem.IndirectAccessToStaticMethod:
			case IProblem.Task:
			case IProblem.UnusedMethodDeclaredThrownException:
			case IProblem.UnusedConstructorDeclaredThrownException:
			case IProblem.UnqualifiedFieldAccess:
			case IProblem.JavadocMissing:
			case IProblem.JavadocMissingParamTag:
			case IProblem.JavadocMissingReturnTag:
			case IProblem.JavadocMissingThrowsTag:
			case IProblem.JavadocUndefinedType:
			case IProblem.JavadocAmbiguousType:
			case IProblem.JavadocNotVisibleType:
			case IProblem.JavadocInvalidThrowsClassName:
			case IProblem.JavadocDuplicateThrowsClassName:
			case IProblem.JavadocDuplicateReturnTag:
			case IProblem.JavadocDuplicateParamName:
			case IProblem.JavadocInvalidParamName:
			case IProblem.JavadocUnexpectedTag:
			case IProblem.JavadocInvalidTag:
			case IProblem.NonBlankFinalLocalAssignment:
			case IProblem.DuplicateFinalLocalInitialization:
			case IProblem.FinalFieldAssignment:
			case IProblem.DuplicateBlankFinalFieldInitialization:
			case IProblem.AnonymousClassCannotExtendFinalClass:
			case IProblem.ClassExtendFinalClass:
			case IProblem.FinalMethodCannotBeOverridden:
			case IProblem.InheritedMethodReducesVisibility:
			case IProblem.MethodReducesVisibility:
			case IProblem.OverridingNonVisibleMethod:
			case IProblem.CannotOverrideAStaticMethodWithAnInstanceMethod:
			case IProblem.CannotHideAnInstanceMethodWithAStaticMethod:
			case IProblem.UnexpectedStaticModifierForMethod:
			case IProblem.LocalVariableHidingLocalVariable:
			case IProblem.LocalVariableHidingField:
			case IProblem.FieldHidingLocalVariable:
			case IProblem.FieldHidingField:
			case IProblem.ArgumentHidingLocalVariable:
			case IProblem.ArgumentHidingField:
			case IProblem.DuplicateField:
			case IProblem.DuplicateMethod:
			case IProblem.DuplicateTypeVariable:
			case IProblem.DuplicateNestedType:
			case IProblem.IllegalModifierForInterfaceMethod:
			case IProblem.IllegalModifierForInterface:
			case IProblem.IllegalModifierForClass:
			case IProblem.IllegalModifierForInterfaceField:
			case IProblem.IllegalModifierForMemberInterface:
			case IProblem.IllegalModifierForMemberClass:
			case IProblem.IllegalModifierForLocalClass:
			case IProblem.IllegalModifierForArgument:
			case IProblem.IllegalModifierForField:
			case IProblem.IllegalModifierForMethod:
			case IProblem.IllegalModifierForConstructor:
			case IProblem.IllegalModifierForVariable:
			case IProblem.IllegalModifierForEnum:
			case IProblem.IllegalModifierForEnumConstant:
			case IProblem.IllegalModifierForEnumConstructor:
			case IProblem.IllegalModifierForMemberEnum:
			case IProblem.UnexpectedStaticModifierForField:
			case IProblem.IllegalModifierCombinationFinalVolatileForField:
			case IProblem.IllegalVisibilityModifierForInterfaceMemberType:
			case IProblem.IncompatibleReturnType:
			case IProblem.IncompatibleExceptionInThrowsClause:
			case IProblem.NoMessageSendOnArrayType:
			case IProblem.InvalidOperator:
			case IProblem.MissingSerialVersion:
			case IProblem.UnnecessaryElse:
			case IProblem.SuperclassMustBeAClass:
			case IProblem.UseAssertAsAnIdentifier:
			case IProblem.UseEnumAsAnIdentifier:
			case IProblem.RedefinedLocal:
			case IProblem.RedefinedArgument:
			case IProblem.CodeCannotBeReached:
			case IProblem.DeadCode:
			case IProblem.InvalidUsageOfTypeParameters:
			case IProblem.InvalidUsageOfStaticImports:
			case IProblem.InvalidUsageOfForeachStatements:
			case IProblem.InvalidUsageOfTypeArguments:
			case IProblem.InvalidUsageOfEnumDeclarations:
			case IProblem.InvalidUsageOfVarargs:
			case IProblem.InvalidUsageOfAnnotations:
			case IProblem.InvalidUsageOfAnnotationDeclarations:
			case IProblem.FieldMissingDeprecatedAnnotation:
			case IProblem.OverridingDeprecatedMethod:
			case IProblem.MethodMissingDeprecatedAnnotation:
			case IProblem.TypeMissingDeprecatedAnnotation:
			case IProblem.MissingOverrideAnnotation:
			case IProblem.MethodMustOverride:
			case IProblem.MethodMustOverrideOrImplement:
			case IProblem.IsClassPathCorrect:
			case IProblem.MethodReturnsVoid:
			case IProblem.ForbiddenReference:
			case IProblem.DiscouragedReference:
			case IProblem.UnnecessaryNLSTag:
			case IProblem.AssignmentHasNoEffect:
			case IProblem.UnsafeTypeConversion:
			case IProblem.UndefinedAnnotationMember:
			case IProblem.MissingValueForAnnotationMember:
			case IProblem.FallthroughCase:
			case IProblem.NonGenericType:
			case IProblem.UnhandledWarningToken:
			case IProblem.UnusedWarningToken:
			case IProblem.RedundantSuperinterface:
			case IProblem.JavadocInvalidMemberTypeQualification:
			case IProblem.IncompatibleTypesInForeach:
			case IProblem.MissingEnumConstantCase:
			case IProblem.MissingSynchronizedModifierInInheritedMethod:
				return true;
			default:
				if (JavaModelUtil.is50OrHigher(cu.getJavaProject())) {
					return SuppressWarningsSubProcessor.hasSuppressWarningsProposal(problemId);
				}
				return false;
		}
	}

	private static int moveBack(int offset, int start, String ignoreCharacters, ICompilationUnit cu) {
		try {
			IBuffer buf= cu.getBuffer();
			while (offset >= start) {
				if (ignoreCharacters.indexOf(buf.getChar(offset - 1)) == -1) {
					return offset;
				}
				offset--;
			}
		} catch(JavaModelException e) {
		}
		return start;
	}


	/* (non-Javadoc)
	 * @see IAssistProcessor#getCorrections(org.eclipse.jdt.internal.ui.text.correction.IAssistContext, org.eclipse.jdt.internal.ui.text.correction.IProblemLocation[])
	 */
	public IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations) throws CoreException {
		if (locations == null || locations.length == 0) {
			return null;
		}

		HashSet handledProblems= new HashSet(locations.length);
		ArrayList resultingCollections= new ArrayList();
		for (int i= 0; i < locations.length; i++) {
			IProblemLocation curr= locations[i];
			Integer id= new Integer(curr.getProblemId());
			if (handledProblems.add(id)) {
				process(context, curr, resultingCollections);
			}
		}
		return (IJavaCompletionProposal[]) resultingCollections.toArray(new IJavaCompletionProposal[resultingCollections.size()]);
	}

	private void process(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		int id= problem.getProblemId();
		if (id == 0) { // no proposals for none-problem locations
			return;
		}
		switch (id) {
			case IProblem.UnterminatedString:
				String quoteLabel= CorrectionMessages.JavaCorrectionProcessor_addquote_description;
				int pos= moveBack(problem.getOffset() + problem.getLength(), problem.getOffset(), "\n\r", context.getCompilationUnit()); //$NON-NLS-1$
				proposals.add(new ReplaceCorrectionProposal(quoteLabel, context.getCompilationUnit(), pos, 0, "\"", 0)); //$NON-NLS-1$
				break;
			case IProblem.UnusedImport:
			case IProblem.DuplicateImport:
			case IProblem.CannotImportPackage:
			case IProblem.ConflictingImport:
				ReorgCorrectionsSubProcessor.removeImportStatementProposals(context, problem, proposals);
				break;
			case IProblem.ImportNotFound:
				ReorgCorrectionsSubProcessor.importNotFoundProposals(context, problem, proposals);
				ReorgCorrectionsSubProcessor.removeImportStatementProposals(context, problem, proposals);
				break;
			case IProblem.UndefinedMethod:
				UnresolvedElementsSubProcessor.getMethodProposals(context, problem, false, proposals);
				break;
			case IProblem.UndefinedConstructor:
				UnresolvedElementsSubProcessor.getConstructorProposals(context, problem, proposals);
				break;
			case IProblem.UndefinedAnnotationMember:
				UnresolvedElementsSubProcessor.getAnnotationMemberProposals(context, problem, proposals);
				break;
			case IProblem.ParameterMismatch:
				UnresolvedElementsSubProcessor.getMethodProposals(context, problem, true, proposals);
				break;
			case IProblem.MethodButWithConstructorName:
				ReturnTypeSubProcessor.addMethodWithConstrNameProposals(context, problem, proposals);
				break;
			case IProblem.UndefinedField:
			case IProblem.UndefinedName:
				UnresolvedElementsSubProcessor.getVariableProposals(context, problem, null, proposals);
				break;
			case IProblem.AmbiguousType:
			case IProblem.JavadocAmbiguousType:
				UnresolvedElementsSubProcessor.getAmbiguosTypeReferenceProposals(context, problem, proposals);
				break;
			case IProblem.PublicClassMustMatchFileName:
				ReorgCorrectionsSubProcessor.getWrongTypeNameProposals(context, problem, proposals);
				break;
			case IProblem.PackageIsNotExpectedPackage:
				ReorgCorrectionsSubProcessor.getWrongPackageDeclNameProposals(context, problem, proposals);
				break;
			case IProblem.UndefinedType:
			case IProblem.JavadocUndefinedType:
				UnresolvedElementsSubProcessor.getTypeProposals(context, problem, proposals);
				break;
			case IProblem.TypeMismatch:
				TypeMismatchSubProcessor.addTypeMismatchProposals(context, problem, proposals);
				break;
			case IProblem.IncompatibleTypesInForeach:
				TypeMismatchSubProcessor.addTypeMismatchInForEachProposals(context, problem, proposals);
				break;
			case IProblem.IncompatibleReturnType:
				TypeMismatchSubProcessor.addIncompatibleReturnTypeProposals(context, problem, proposals);
				break;
			case IProblem.IncompatibleExceptionInThrowsClause:
				TypeMismatchSubProcessor.addIncompatibleThrowsProposals(context, problem, proposals);
				break;
			case IProblem.UnhandledException:
				LocalCorrectionsSubProcessor.addUncaughtExceptionProposals(context, problem, proposals);
				break;
			case IProblem.UnreachableCatch:
			case IProblem.InvalidCatchBlockSequence:
				LocalCorrectionsSubProcessor.addUnreachableCatchProposals(context, problem, proposals);
				break;
			case IProblem.RedundantSuperinterface:
				LocalCorrectionsSubProcessor.addRedundantSuperInterfaceProposal(context, problem, proposals);
				break;
			case IProblem.VoidMethodReturnsValue:
				ReturnTypeSubProcessor.addVoidMethodReturnsProposals(context, problem, proposals);
				break;
			case IProblem.MethodReturnsVoid:
				ReturnTypeSubProcessor.addMethodRetunsVoidProposals(context, problem, proposals);
				break;
			case IProblem.MissingReturnType:
				ReturnTypeSubProcessor.addMissingReturnTypeProposals(context, problem, proposals);
				break;
			case IProblem.ShouldReturnValue:
				ReturnTypeSubProcessor.addMissingReturnStatementProposals(context, problem, proposals);
				break;
			case IProblem.NonExternalizedStringLiteral:
				LocalCorrectionsSubProcessor.addNLSProposals(context, problem, proposals);
				break;
			case IProblem.UnnecessaryNLSTag:
				LocalCorrectionsSubProcessor.getUnnecessaryNLSTagProposals(context, problem, proposals);
				break;
			case IProblem.NonStaticAccessToStaticField:
			case IProblem.NonStaticAccessToStaticMethod:
			case IProblem.IndirectAccessToStaticField:
			case IProblem.IndirectAccessToStaticMethod:
				LocalCorrectionsSubProcessor.addCorrectAccessToStaticProposals(context, problem, proposals);
				break;
			case IProblem.StaticMethodRequested:
			case IProblem.NonStaticFieldFromStaticInvocation:
			case IProblem.InstanceMethodDuringConstructorInvocation:
			case IProblem.InstanceFieldDuringConstructorInvocation:
				ModifierCorrectionSubProcessor.addNonAccessibleReferenceProposal(context, problem, proposals, ModifierCorrectionSubProcessor.TO_STATIC, 5);
				break;
			case IProblem.NonBlankFinalLocalAssignment:
			case IProblem.DuplicateFinalLocalInitialization:
			case IProblem.FinalFieldAssignment:
			case IProblem.DuplicateBlankFinalFieldInitialization:
			case IProblem.AnonymousClassCannotExtendFinalClass:
			case IProblem.ClassExtendFinalClass:
				ModifierCorrectionSubProcessor.addNonAccessibleReferenceProposal(context, problem, proposals, ModifierCorrectionSubProcessor.TO_NON_FINAL, 9);
				break;
			case IProblem.InheritedMethodReducesVisibility:
			case IProblem.MethodReducesVisibility:
			case IProblem.OverridingNonVisibleMethod:
				ModifierCorrectionSubProcessor.addChangeOverriddenModfierProposal(context, problem, proposals, ModifierCorrectionSubProcessor.TO_VISIBLE);
				break;
			case IProblem.FinalMethodCannotBeOverridden:
				ModifierCorrectionSubProcessor.addChangeOverriddenModfierProposal(context, problem, proposals, ModifierCorrectionSubProcessor.TO_NON_FINAL);
				break;
			case IProblem.CannotOverrideAStaticMethodWithAnInstanceMethod:
				ModifierCorrectionSubProcessor.addChangeOverriddenModfierProposal(context, problem, proposals, ModifierCorrectionSubProcessor.TO_NON_STATIC);
				break;
			case IProblem.CannotHideAnInstanceMethodWithAStaticMethod:
			case IProblem.IllegalModifierForInterfaceMethod:
			case IProblem.IllegalModifierForInterface:
			case IProblem.IllegalModifierForClass:
			case IProblem.IllegalModifierForInterfaceField:
			case IProblem.UnexpectedStaticModifierForField:
			case IProblem.IllegalModifierCombinationFinalVolatileForField:
			case IProblem.IllegalModifierForMemberInterface:
			case IProblem.IllegalModifierForMemberClass:
			case IProblem.IllegalModifierForLocalClass:
			case IProblem.IllegalModifierForArgument:
			case IProblem.IllegalModifierForField:
			case IProblem.IllegalModifierForMethod:
			case IProblem.IllegalModifierForConstructor:
			case IProblem.IllegalModifierForVariable:
			case IProblem.IllegalModifierForEnum:
			case IProblem.IllegalModifierForEnumConstant:
			case IProblem.IllegalModifierForEnumConstructor:
			case IProblem.IllegalModifierForMemberEnum:
			case IProblem.IllegalVisibilityModifierForInterfaceMemberType:
			case IProblem.UnexpectedStaticModifierForMethod:
				ModifierCorrectionSubProcessor.addRemoveInvalidModfiersProposal(context, problem, proposals, 5);
				break;
			case IProblem.NotVisibleField:
				GetterSetterCorrectionSubProcessor.addGetterSetterProposal(context, problem, proposals, 9);
				ModifierCorrectionSubProcessor.addNonAccessibleReferenceProposal(context, problem, proposals, ModifierCorrectionSubProcessor.TO_VISIBLE, 10);
				break;
			case IProblem.NotVisibleMethod:
			case IProblem.NotVisibleConstructor:
			case IProblem.NotVisibleType:
			case IProblem.JavadocNotVisibleType:
				ModifierCorrectionSubProcessor.addNonAccessibleReferenceProposal(context, problem, proposals, ModifierCorrectionSubProcessor.TO_VISIBLE, 10);
				break;
			case IProblem.BodyForAbstractMethod:
			case IProblem.AbstractMethodInAbstractClass:
			case IProblem.AbstractMethodInEnum:
			case IProblem.EnumAbstractMethodMustBeImplemented:
				ModifierCorrectionSubProcessor.addAbstractMethodProposals(context, problem, proposals);
				break;
			case IProblem.AbstractMethodsInConcreteClass:
				ModifierCorrectionSubProcessor.addAbstractTypeProposals(context, problem, proposals);
				break;
			case IProblem.AbstractMethodMustBeImplemented:
			case IProblem.EnumConstantMustImplementAbstractMethod:
				LocalCorrectionsSubProcessor.addUnimplementedMethodsProposals(context, problem, proposals);
				break;
			case IProblem.ShouldImplementHashcode:
				LocalCorrectionsSubProcessor.addMissingHashCodeProposals(context, problem, proposals);
				break;
			case IProblem.MissingValueForAnnotationMember:
				LocalCorrectionsSubProcessor.addValueForAnnotationProposals(context, problem, proposals);
				break;
			case IProblem.BodyForNativeMethod:
				ModifierCorrectionSubProcessor.addNativeMethodProposals(context, problem, proposals);
				break;
			case IProblem.MethodRequiresBody:
				ModifierCorrectionSubProcessor.addMethodRequiresBodyProposals(context, problem, proposals);
				break;
			case IProblem.OuterLocalMustBeFinal:
				ModifierCorrectionSubProcessor.addNonFinalLocalProposal(context, problem, proposals);
				break;
			case IProblem.UninitializedLocalVariable:
				LocalCorrectionsSubProcessor.addUninitializedLocalVariableProposal(context, problem, proposals);
				break;
			case IProblem.UnhandledExceptionInDefaultConstructor:
			case IProblem.UndefinedConstructorInDefaultConstructor:
			case IProblem.NotVisibleConstructorInDefaultConstructor:
				LocalCorrectionsSubProcessor.addConstructorFromSuperclassProposal(context, problem, proposals);
				break;
			case IProblem.UnusedPrivateMethod:
			case IProblem.UnusedPrivateConstructor:
			case IProblem.UnusedPrivateField:
			case IProblem.UnusedPrivateType:
			case IProblem.LocalVariableIsNeverUsed:
			case IProblem.ArgumentIsNeverUsed:
				LocalCorrectionsSubProcessor.addUnusedMemberProposal(context, problem, proposals);
				break;
			case IProblem.NeedToEmulateFieldReadAccess:
			case IProblem.NeedToEmulateFieldWriteAccess:
			case IProblem.NeedToEmulateMethodAccess:
			case IProblem.NeedToEmulateConstructorAccess:
				ModifierCorrectionSubProcessor.addNonAccessibleReferenceProposal(context, problem, proposals, ModifierCorrectionSubProcessor.TO_NON_PRIVATE, 5);
				break;
			case IProblem.SuperfluousSemicolon:
				LocalCorrectionsSubProcessor.addSuperfluousSemicolonProposal(context, problem, proposals);
				break;
			case IProblem.UnnecessaryCast:
				LocalCorrectionsSubProcessor.addUnnecessaryCastProposal(context, problem, proposals);
				break;
			case IProblem.UnnecessaryInstanceof:
				LocalCorrectionsSubProcessor.addUnnecessaryInstanceofProposal(context, problem, proposals);
				break;
			case IProblem.UnusedMethodDeclaredThrownException:
			case IProblem.UnusedConstructorDeclaredThrownException:
				LocalCorrectionsSubProcessor.addUnnecessaryThrownExceptionProposal(context, problem, proposals);
				break;
			case IProblem.UnqualifiedFieldAccess:
				GetterSetterCorrectionSubProcessor.addGetterSetterProposal(context, problem, proposals, 5);
				LocalCorrectionsSubProcessor.addUnqualifiedFieldAccessProposal(context, problem, proposals);
				break;
			case IProblem.Task:
				proposals.add(new TaskMarkerProposal(context.getCompilationUnit(), problem, 10));
				break;
			case IProblem.JavadocMissing:
				JavadocTagsSubProcessor.getMissingJavadocCommentProposals(context, problem, proposals);
				break;
			case IProblem.JavadocMissingParamTag:
			case IProblem.JavadocMissingReturnTag:
			case IProblem.JavadocMissingThrowsTag:
				JavadocTagsSubProcessor.getMissingJavadocTagProposals(context, problem, proposals);
				break;
			case IProblem.JavadocInvalidThrowsClassName:
			case IProblem.JavadocDuplicateThrowsClassName:
			case IProblem.JavadocDuplicateReturnTag:
			case IProblem.JavadocDuplicateParamName:
			case IProblem.JavadocInvalidParamName:
			case IProblem.JavadocUnexpectedTag:
			case IProblem.JavadocInvalidTag:
				JavadocTagsSubProcessor.getRemoveJavadocTagProposals(context, problem, proposals);
				break;
			case IProblem.JavadocInvalidMemberTypeQualification:
				JavadocTagsSubProcessor.getInvalidQualificationProposals(context, problem, proposals);
				break;

			case IProblem.LocalVariableHidingLocalVariable:
			case IProblem.LocalVariableHidingField:
			case IProblem.FieldHidingLocalVariable:
			case IProblem.FieldHidingField:
			case IProblem.ArgumentHidingLocalVariable:
			case IProblem.ArgumentHidingField:
			case IProblem.UseAssertAsAnIdentifier:
			case IProblem.UseEnumAsAnIdentifier:
			case IProblem.RedefinedLocal:
			case IProblem.RedefinedArgument:
			case IProblem.DuplicateField:
			case IProblem.DuplicateMethod:
			case IProblem.DuplicateTypeVariable:
			case IProblem.DuplicateNestedType:
				LocalCorrectionsSubProcessor.addInvalidVariableNameProposals(context, problem, proposals);
				break;
			case IProblem.NoMessageSendOnArrayType:
				UnresolvedElementsSubProcessor.getArrayAccessProposals(context, problem, proposals);
				break;
			case IProblem.InvalidOperator:
				LocalCorrectionsSubProcessor.getInvalidOperatorProposals(context, problem, proposals);
				break;
			case IProblem.MissingSerialVersion:
			    SerialVersionSubProcessor.getSerialVersionProposals(context, problem, proposals);
				break;
			case IProblem.UnnecessaryElse:
				LocalCorrectionsSubProcessor.getUnnecessaryElseProposals(context, problem, proposals);
				break;
			case IProblem.SuperclassMustBeAClass:
				LocalCorrectionsSubProcessor.getInterfaceExtendsClassProposals(context, problem, proposals);
				break;
			case IProblem.CodeCannotBeReached:
			case IProblem.DeadCode:
				LocalCorrectionsSubProcessor.getUnreachableCodeProposals(context, problem, proposals);
				break;
			case IProblem.InvalidUsageOfTypeParameters:
			case IProblem.InvalidUsageOfStaticImports:
			case IProblem.InvalidUsageOfForeachStatements:
			case IProblem.InvalidUsageOfTypeArguments:
			case IProblem.InvalidUsageOfEnumDeclarations:
			case IProblem.InvalidUsageOfVarargs:
			case IProblem.InvalidUsageOfAnnotations:
			case IProblem.InvalidUsageOfAnnotationDeclarations:
				ReorgCorrectionsSubProcessor.getNeed50ComplianceProposals(context, problem, proposals);
				break;
			case IProblem.NonGenericType:
				TypeArgumentMismatchSubProcessor.removeMismatchedArguments(context, problem, proposals);
				break;
			case IProblem.MissingOverrideAnnotation:
				ModifierCorrectionSubProcessor.addOverrideAnnotationProposal(context, problem, proposals);
				break;
			case IProblem.MethodMustOverride:
			case IProblem.MethodMustOverrideOrImplement:
				ModifierCorrectionSubProcessor.removeOverrideAnnotationProposal(context, problem, proposals);
				break;
			case IProblem.FieldMissingDeprecatedAnnotation:
			case IProblem.MethodMissingDeprecatedAnnotation:
			case IProblem.TypeMissingDeprecatedAnnotation:
				ModifierCorrectionSubProcessor.addDeprecatedAnnotationProposal(context, problem, proposals);
				break;
			case IProblem.OverridingDeprecatedMethod:
				ModifierCorrectionSubProcessor.addOverridingDeprecatedMethodProposal(context, problem, proposals);
				break;
			case IProblem.IsClassPathCorrect:
				ReorgCorrectionsSubProcessor.getIncorrectBuildPathProposals(context, problem, proposals);
				break;
			case IProblem.ForbiddenReference:
			case IProblem.DiscouragedReference:
				ReorgCorrectionsSubProcessor.getAccessRulesProposals(context, problem, proposals);
				break;
			case IProblem.AssignmentHasNoEffect:
				LocalCorrectionsSubProcessor.getAssignmentHasNoEffectProposals(context, problem, proposals);
				break;
			case IProblem.UnsafeTypeConversion:
			case IProblem.RawTypeReference:
			case IProblem.UnsafeRawMethodInvocation:
				LocalCorrectionsSubProcessor.addDeprecatedFieldsToMethodsProposals(context, problem, proposals);
				LocalCorrectionsSubProcessor.addTypePrametersToRawTypeReference(context, problem, proposals);
				break;
			case IProblem.FallthroughCase:
				LocalCorrectionsSubProcessor.addFallThroughProposals(context, problem, proposals);
				break;
			case IProblem.UnhandledWarningToken:
				SuppressWarningsSubProcessor.addUnknownSuppressWarningProposals(context, problem, proposals);
				break;
			case IProblem.UnusedWarningToken:
				SuppressWarningsSubProcessor.addRemoveUnusedSuppressWarningProposals(context, problem, proposals);
				break;
			case IProblem.MissingEnumConstantCase:
				LocalCorrectionsSubProcessor.getMissingEnumConstantCaseProposals(context, problem, proposals);
				break;
			case IProblem.MissingSynchronizedModifierInInheritedMethod:
				ModifierCorrectionSubProcessor.addSynchronizedMethodProposal(context, problem, proposals);
				break;
			default:
		}
		if (JavaModelUtil.is50OrHigher(context.getCompilationUnit().getJavaProject())) {
			SuppressWarningsSubProcessor.addSuppressWarningsProposals(context, problem, proposals);
		}
	}
}
