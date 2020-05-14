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
 *     Red Hat Inc. - modified to use UnusedCodeFixCore
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.UnusedCodeFixCore.RemoveCastOperation;
import org.eclipse.jdt.internal.corext.fix.UnusedCodeFixCore.RemoveUnusedMemberOperation;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.fix.CleanUpFixWrapper;
import org.eclipse.jdt.internal.ui.fix.UnusedCodeCleanUp;
import org.eclipse.jdt.internal.ui.text.correction.IProblemLocationCore;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

/**
 * Fix which removes unused code.
 */
public class UnusedCodeFix extends CompilationUnitRewriteOperationsFix {


	public static UnusedCodeFix createRemoveUnusedImportFix(CompilationUnit compilationUnit, IProblemLocation problem) {
		if (isUnusedImport(problem)) {
			IProblemLocationCore problemCore= (ProblemLocation)problem;
			ImportDeclaration node= UnusedCodeFixCore.getImportDeclaration(problemCore, compilationUnit);
			if (node != null) {
				String label= FixMessages.UnusedCodeFix_RemoveImport_description;
				UnusedCodeFixCore.RemoveImportOperation operation= new UnusedCodeFixCore.RemoveImportOperation(node);
				Map<String, String> options= new Hashtable<>();
				options.put(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS, CleanUpOptions.TRUE);
				return new UnusedCodeFix(label, compilationUnit, new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] {operation}, options);
			}
		}
		return null;
	}

	public static boolean isUnusedImport(IProblemLocation problem) {
		IProblemLocationCore problemCore= (ProblemLocation)problem;
		return UnusedCodeFixCore.isUnusedImport(problemCore);
	}

	public static UnusedCodeFix createUnusedMemberFix(CompilationUnit compilationUnit, IProblemLocation problem, boolean removeAllAssignements) {
		if (isUnusedMember(problem)) {
			IProblemLocationCore problemCore= (ProblemLocation)problem;
			SimpleName name= UnusedCodeFixCore.getUnusedName(compilationUnit, problemCore);
			if (name != null) {
				IBinding binding= name.resolveBinding();
				if (binding != null) {
					if (UnusedCodeFixCore.isFormalParameterInEnhancedForStatement(name))
						return null;

					String label= UnusedCodeFixCore.getDisplayString(name, binding, removeAllAssignements);
					UnusedCodeFixCore.RemoveUnusedMemberOperation operation= new RemoveUnusedMemberOperation(new SimpleName[] { name }, removeAllAssignements);
					return new UnusedCodeFix(label, compilationUnit,
							new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] { operation }, UnusedCodeFixCore.getCleanUpOptions(binding, removeAllAssignements));
				}
			}
		}
		return null;
	}

	public static UnusedCodeFix createUnusedTypeParameterFix(CompilationUnit compilationUnit, IProblemLocation problemLoc) {
		if (problemLoc.getProblemId() == IProblem.UnusedTypeParameter) {
			IProblemLocationCore problemLocCore= (ProblemLocation)problemLoc;
			SimpleName name= UnusedCodeFixCore.getUnusedName(compilationUnit, problemLocCore);
			if (name != null) {
				IBinding binding= name.resolveBinding();
				if (binding != null) {
					String label= FixMessages.UnusedCodeFix_RemoveUnusedTypeParameter_description;
					UnusedCodeFixCore.RemoveUnusedTypeParameterOperation operation= new UnusedCodeFixCore.RemoveUnusedTypeParameterOperation(name);
					return new UnusedCodeFix(label, compilationUnit,
							new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] { operation }, UnusedCodeFixCore.getCleanUpOptions(binding, false));
				}
			}
		}
		return null;
	}

	public static boolean isUnusedMember(IProblemLocation problem) {
		IProblemLocationCore problemCore= (ProblemLocation)problem;
		return UnusedCodeFixCore.isUnusedMember(problemCore);
	}

	public static UnusedCodeFix createRemoveUnusedCastFix(CompilationUnit compilationUnit, IProblemLocation problem) {
		if (problem.getProblemId() != IProblem.UnnecessaryCast)
			return null;

		ASTNode selectedNode= ASTNodes.getUnparenthesedExpression(problem.getCoveringNode(compilationUnit));

		if (!(selectedNode instanceof CastExpression))
			return null;

		return new UnusedCodeFix(FixMessages.UnusedCodeFix_RemoveCast_description, compilationUnit,
				new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] {new RemoveCastOperation((CastExpression)selectedNode)});
	}

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit,
			boolean removeUnusedPrivateMethods,
			boolean removeUnusedPrivateConstructors,
			boolean removeUnusedPrivateFields,
			boolean removeUnusedPrivateTypes,
			boolean removeUnusedLocalVariables,
			boolean removeUnusedImports,
			boolean removeUnusedCast) {

		ICleanUpFixCore fix= UnusedCodeFixCore.createCleanUp(compilationUnit, removeUnusedPrivateMethods, removeUnusedPrivateConstructors,
				removeUnusedPrivateFields, removeUnusedPrivateTypes, removeUnusedLocalVariables, removeUnusedImports, removeUnusedCast);

		return fix == null ? null : new CleanUpFixWrapper(fix);
	}

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit, IProblemLocation[] problems,
			boolean removeUnusedPrivateMethods,
			boolean removeUnusedPrivateConstructors,
			boolean removeUnusedPrivateFields,
			boolean removeUnusedPrivateTypes,
			boolean removeUnusedLocalVariables,
			boolean removeUnusedImports,
			boolean removeUnusedCast) {

		IProblemLocationCore[] problemsCore= null;
		if (problems != null) {
			List<IProblemLocationCore> problemList= new ArrayList<>();
			for (IProblemLocation problem : problems) {
				problemList.add((ProblemLocation)problem);
			}
			problemsCore= problemList.toArray(new IProblemLocationCore[0]);
		}

		ICleanUpFixCore fix= UnusedCodeFixCore.createCleanUp(compilationUnit, problemsCore, removeUnusedPrivateMethods,
				removeUnusedPrivateConstructors, removeUnusedPrivateFields, removeUnusedPrivateTypes, removeUnusedLocalVariables, removeUnusedImports, removeUnusedCast);

		return fix == null ? null : new CleanUpFixWrapper(fix);
	}

	private final Map<String, String> fCleanUpOptions;

	private UnusedCodeFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		this(name, compilationUnit, fixRewriteOperations, null);
	}

	private UnusedCodeFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations, Map<String, String> options) {
		super(name, compilationUnit, fixRewriteOperations);
		fCleanUpOptions= options;
	}

	public UnusedCodeCleanUp getCleanUp() {
		if (fCleanUpOptions == null)
			return null;

		return new UnusedCodeCleanUp(fCleanUpOptions);
	}

}
