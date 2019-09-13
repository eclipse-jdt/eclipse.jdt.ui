/*******************************************************************************
 * Copyright (c) 2011, 2018 GK Software AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - [quick fix] Add quick fixes for null annotations - https://bugs.eclipse.org/337977
 *     IBM Corporation - bug fixes
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import org.eclipse.jdt.internal.corext.codemanipulation.RedundantNullnessTypeAnnotationsFilter;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.NullAnnotationsRewriteOperations.AddMissingDefaultNullnessRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.NullAnnotationsRewriteOperations.Builder;
import org.eclipse.jdt.internal.corext.fix.NullAnnotationsRewriteOperations.ChangeKind;
import org.eclipse.jdt.internal.corext.fix.NullAnnotationsRewriteOperations.RemoveRedundantAnnotationRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.NullAnnotationsRewriteOperations.SignatureAnnotationRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.TypeAnnotationRewriteOperations.MoveTypeAnnotationRewriteOperation;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;
import org.eclipse.jdt.internal.ui.text.correction.TypeAnnotationSubProcessor;

public class NullAnnotationsFix extends CompilationUnitRewriteOperationsFix {

	private CompilationUnit cu;

	public NullAnnotationsFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation[] operations) {
		super(name, compilationUnit, operations);
		cu= compilationUnit;
	}

	public CompilationUnit getCu() {
		return cu;
	}

	/* recognizes any simple name referring to a parameter binding */
	public static boolean isComplainingAboutArgument(ASTNode selectedNode) {
		if (!(selectedNode instanceof SimpleName))
			return false;
		SimpleName nameNode= (SimpleName) selectedNode;
		IBinding binding= nameNode.resolveBinding();
		if (binding.getKind() == IBinding.VARIABLE && ((IVariableBinding) binding).isParameter())
			return true;
		VariableDeclaration argDecl= ASTNodes.getParent(selectedNode, VariableDeclaration.class);
		if (argDecl != null)
			binding= argDecl.resolveBinding();
		if (binding.getKind() == IBinding.VARIABLE && ((IVariableBinding) binding).isParameter())
			return true;
		return false;
	}

	/* recognizes the expression of a return statement and the return type of a method declaration. */
	public static boolean isComplainingAboutReturn(ASTNode selectedNode) {
		if (selectedNode.getParent().getNodeType() == ASTNode.RETURN_STATEMENT)
			return true;
		while (!(selectedNode instanceof Type)) {
			if (selectedNode == null) return false;
			selectedNode= selectedNode.getParent();
		}
		return selectedNode.getLocationInParent() == MethodDeclaration.RETURN_TYPE2_PROPERTY;
	}

	public static NullAnnotationsFix createNullAnnotationInSignatureFix(CompilationUnit compilationUnit, IProblemLocation problem,
			ChangeKind changeKind, boolean isArgumentProblem) {
		IJavaElement javaElement= compilationUnit.getJavaElement();
		String nullableAnnotationName= getNullableAnnotationName(javaElement, false);
		String nonNullAnnotationName= getNonNullAnnotationName(javaElement, false);
		Builder builder= new Builder(problem, compilationUnit, nullableAnnotationName, nonNullAnnotationName,
										/*allowRemove*/true, isArgumentProblem, changeKind);
		boolean addNonNull= false;

		switch (problem.getProblemId()) {
			case IProblem.IllegalDefinitionToNonNullParameter:
			case IProblem.IllegalRedefinitionToNonNullParameter:
				// case ParameterLackingNullableAnnotation: // never proposed with modifyOverridden
				if (changeKind == ChangeKind.OVERRIDDEN) {
					addNonNull= true;
					builder.swapAnnotations();
				}
				break;
			case IProblem.ParameterLackingNonNullAnnotation:
			case IProblem.IllegalReturnNullityRedefinition:
				if (changeKind != ChangeKind.OVERRIDDEN) {
					addNonNull= true;
					builder.swapAnnotations();
				}
				break;
			case IProblem.RequiredNonNullButProvidedNull:
			case IProblem.RequiredNonNullButProvidedPotentialNull:
			case IProblem.RequiredNonNullButProvidedUnknown:
			case IProblem.RequiredNonNullButProvidedSpecdNullable:
				if (isArgumentProblem == (changeKind != ChangeKind.TARGET)) {
					addNonNull= true;
					builder.swapAnnotations();
				}
				break;
			case IProblem.ConflictingNullAnnotations:
				if (isArgumentProblem && changeKind == ChangeKind.INVERSE) {
					return null; // cannot redefine @Nullable param to @NonNull
				}
				//$FALL-THROUGH$
			case IProblem.ConflictingInheritedNullAnnotations:
				if (changeKind == ChangeKind.INVERSE || changeKind == ChangeKind.OVERRIDDEN) {
					addNonNull= true;
					builder.swapAnnotations();
				}
				break;
			default:
				// all others propose to add @Nullable
		}

		// when performing one change at a time we can actually modify another CU than the current one:
		NullAnnotationsRewriteOperations.SignatureAnnotationRewriteOperation operation= builder.createAddAnnotationOperation(null, false/*thisUnitOnly*/, changeKind);
		if (operation == null)
			return null;

		if (addNonNull) {
			operation.fRemoveIfNonNullByDefault= true;
			operation.fNonNullByDefaultNames= RedundantNullnessTypeAnnotationsFilter.determineNonNullByDefaultNames(javaElement.getJavaProject());
		}
		return new NullAnnotationsFix(operation.getMessage(), operation.getCompilationUnit(), // note that this uses the findings from createAddAnnotationOperation(..)
				new NullAnnotationsRewriteOperations.SignatureAnnotationRewriteOperation[] { operation });
	}

	public static NullAnnotationsFix createRemoveRedundantNullAnnotationsFix(CompilationUnit compilationUnit, IProblemLocation problem) {
		RemoveRedundantAnnotationRewriteOperation operation= new RemoveRedundantAnnotationRewriteOperation(compilationUnit, problem);
		return new NullAnnotationsFix(FixMessages.NullAnnotationsRewriteOperations_remove_redundant_nullness_annotation, compilationUnit, new RemoveRedundantAnnotationRewriteOperation[] { operation });
	}

	public static NullAnnotationsFix createAddMissingDefaultNullnessAnnotationsFix(CompilationUnit compilationUnit, IProblemLocation problem) {
		AddMissingDefaultNullnessRewriteOperation operation= new AddMissingDefaultNullnessRewriteOperation(compilationUnit, problem);
		String nonNullByDefaultAnnotationname= NullAnnotationsFix.getNonNullByDefaultAnnotationName(compilationUnit.getJavaElement(), true);
		String label= Messages.format(FixMessages.NullAnnotationsRewriteOperations_add_missing_default_nullness_annotation, new String[] { nonNullByDefaultAnnotationname });
		return new NullAnnotationsFix(label, compilationUnit, new AddMissingDefaultNullnessRewriteOperation[] { operation });
	}

	// Entry for NullAnnotationsCleanup:
	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit, IProblemLocation[] locations, int problemID) {
		ICompilationUnit cu= (ICompilationUnit) compilationUnit.getJavaElement();
		if (!JavaModelUtil.is50OrHigher(cu.getJavaProject()))
			return null;

		List<CompilationUnitRewriteOperation> operations= new ArrayList<>();
		if (locations == null) {
			org.eclipse.jdt.core.compiler.IProblem[] problems= compilationUnit.getProblems();
			locations= new IProblemLocation[problems.length];
			for (int i= 0; i < problems.length; i++) {
				if (problems[i].getID() == problemID)
					locations[i]= new ProblemLocation(problems[i]);
			}
		}
		String message;
		if (TypeAnnotationSubProcessor.hasFixFor(problemID)) {
			boolean isMove= createMoveTypeAnnotationOperations(compilationUnit, locations, operations);
			message= isMove ? FixMessages.TypeAnnotationFix_move : FixMessages.TypeAnnotationFix_remove;
		} else {
			createAddNullAnnotationOperations(compilationUnit, locations, operations);
			createRemoveRedundantNullAnnotationsOperations(compilationUnit, locations, operations);
			message= FixMessages.NullAnnotationsFix_add_annotation_change_name;
		}
		if (operations.isEmpty())
			return null;
		CompilationUnitRewriteOperation[] operationsArray= operations.toArray(new CompilationUnitRewriteOperation[operations.size()]);
		return new NullAnnotationsFix(message, compilationUnit, operationsArray);
	}

	private static boolean createMoveTypeAnnotationOperations(CompilationUnit compilationUnit, IProblemLocation[] locations, List<CompilationUnitRewriteOperation> operations) {
		boolean isMove= false;
		for (IProblemLocation location: locations) {
			if (location == null)
				continue;
			MoveTypeAnnotationRewriteOperation operation= new MoveTypeAnnotationRewriteOperation(compilationUnit, location);
			operations.add(operation);
			isMove|= operation.isMove();
		}
		return isMove;
	}

	private static void createAddNullAnnotationOperations(CompilationUnit compilationUnit, IProblemLocation[] locations, List<CompilationUnitRewriteOperation> result) {
		String nullableAnnotationName= getNullableAnnotationName(compilationUnit.getJavaElement(), false);
		String nonNullAnnotationName= getNonNullAnnotationName(compilationUnit.getJavaElement(), false);

		Set<String> handledPositions= new HashSet<>();
		for (IProblemLocation problem : locations) {
			if (problem == null)
				continue; // problem was filtered out by createCleanUp()

			if (problem.getProblemId() == IProblem.MissingNonNullByDefaultAnnotationOnPackage) {
				result.add(new AddMissingDefaultNullnessRewriteOperation(compilationUnit, problem));
				continue;
			}

			ASTNode coveredNode= problem.getCoveredNode(compilationUnit);
			boolean isArgumentProblem= isComplainingAboutArgument(coveredNode);
			if(!isArgumentProblem && !isComplainingAboutReturn(coveredNode)) {
				continue;
			}
			Builder builder= new Builder(problem, compilationUnit, nullableAnnotationName, nonNullAnnotationName,
					/*allowRemove*/false, isArgumentProblem, ChangeKind.LOCAL);
			boolean addNonNull= false;
			// cf. createNullAnnotationInSignatureFix() but changeKind is constantly LOCAL
			switch (problem.getProblemId()) {
				case IProblem.IllegalDefinitionToNonNullParameter:
				case IProblem.IllegalRedefinitionToNonNullParameter:
					break;
				case IProblem.ParameterLackingNonNullAnnotation:
				case IProblem.IllegalReturnNullityRedefinition:
					addNonNull= true;
					builder.swapAnnotations();
					break;
				case IProblem.RequiredNonNullButProvidedNull:
				case IProblem.RequiredNonNullButProvidedPotentialNull:
				case IProblem.RequiredNonNullButProvidedUnknown:
				case IProblem.RequiredNonNullButProvidedSpecdNullable:
					if (isArgumentProblem) {
						addNonNull= true;
						builder.swapAnnotations();
					}
					break;
				default:
					// all others propose to add @Nullable
			}
			// when performing multiple changes we can only modify the one CU that the CleanUp infrastructure provides to the operation.
			SignatureAnnotationRewriteOperation fix= builder.createAddAnnotationOperation(handledPositions, true/*thisUnitOnly*/, ChangeKind.LOCAL);
			if (fix != null) {
				if (addNonNull) {
					fix.fRemoveIfNonNullByDefault= true;
					fix.fNonNullByDefaultNames= RedundantNullnessTypeAnnotationsFilter.determineNonNullByDefaultNames(compilationUnit.getJavaElement().getJavaProject());
				}
				result.add(fix);
			}
		}
	}

	private static void createRemoveRedundantNullAnnotationsOperations(CompilationUnit compilationUnit, IProblemLocation[] locations, List<CompilationUnitRewriteOperation> result) {
		for (IProblemLocation problem : locations) {
			if (problem == null)
				continue; // problem was filtered out by createCleanUp()

			int problemId= problem.getProblemId();
			if (problemId == IProblem.RedundantNullAnnotation || problemId == IProblem.RedundantNullDefaultAnnotationPackage || problemId == IProblem.RedundantNullDefaultAnnotationType
				|| problemId == IProblem.RedundantNullDefaultAnnotationMethod || problemId == IProblem.RedundantNullDefaultAnnotationLocal
				|| problemId == IProblem.RedundantNullDefaultAnnotationField) {
				RemoveRedundantAnnotationRewriteOperation operation= new RemoveRedundantAnnotationRewriteOperation(compilationUnit, problem);
				result.add(operation);
			}
		}
	}

//	private static boolean isMissingNullAnnotationProblem(int id) {
//		return id == IProblem.RequiredNonNullButProvidedNull || id == IProblem.RequiredNonNullButProvidedPotentialNull || id == IProblem.IllegalReturnNullityRedefinition
//				|| mayIndicateParameterNullcheck(id);
//	}
//
//	private static boolean mayIndicateParameterNullcheck(int problemId) {
//		return problemId == IProblem.NonNullLocalVariableComparisonYieldsFalse || problemId == IProblem.RedundantNullCheckOnNonNullLocalVariable;
//	}

	/**
	 * Tells whether an explicit null annotation exists on the given compilation unit.
	 *
	 * @param compilationUnit the compilation unit
	 * @param offset the offset
	 * @return <code>true</code> if the compilation unit has an explicit null annotation
	 */
	public static boolean hasExplicitNullAnnotation(ICompilationUnit compilationUnit, int offset) {
// FIXME(SH): check for existing annotations disabled due to lack of precision:
//		      should distinguish what is actually annotated (return? param? which?)
//		try {
//			IJavaElement problemElement= compilationUnit.getElementAt(offset);
//			if (problemElement.getElementType() == IJavaElement.METHOD) {
//				IMethod method= (IMethod) problemElement;
//				String nullable= getNullableAnnotationName(compilationUnit, true);
//				String nonnull= getNonNullAnnotationName(compilationUnit, true);
//				for (IAnnotation annotation : method.getAnnotations()) {
//					if (   annotation.getElementName().equals(nonnull)
//						|| annotation.getElementName().equals(nullable))
//						return true;
//				}
//			}
//		} catch (JavaModelException jme) {
//			/* nop */
//		}
		return false;
	}

	public static String getNullableAnnotationName(IJavaElement javaElement, boolean makeSimple) {
		return getAnnotationName(javaElement, makeSimple, JavaCore.COMPILER_NULLABLE_ANNOTATION_NAME);
	}

	public static String getNonNullAnnotationName(IJavaElement javaElement, boolean makeSimple) {
		return getAnnotationName(javaElement, makeSimple, JavaCore.COMPILER_NONNULL_ANNOTATION_NAME);
	}

	public static String getNonNullByDefaultAnnotationName(IJavaElement javaElement, boolean makeSimple) {
		return getAnnotationName(javaElement, makeSimple, JavaCore.COMPILER_NONNULL_BY_DEFAULT_ANNOTATION_NAME);
	}

	private static String getAnnotationName(IJavaElement javaElement, boolean makeSimple, String annotation) {
		String qualifiedName= javaElement.getJavaProject().getOption(annotation, true);
		int lastDot;
		if (makeSimple && qualifiedName != null && (lastDot= qualifiedName.lastIndexOf('.')) != -1)
			return qualifiedName.substring(lastDot + 1);
		return qualifiedName;
	}
}
