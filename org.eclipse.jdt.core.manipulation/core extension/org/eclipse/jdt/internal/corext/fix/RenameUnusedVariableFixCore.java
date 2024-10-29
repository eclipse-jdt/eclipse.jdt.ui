/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
 *     Red Hat Inc. - copied and modified from UnusedCodeFixCore
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Pattern;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.fix.RenameUnusedVariableCleanUpCore;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

/**
 * Fix which removes unused code.
 */
public class RenameUnusedVariableFixCore extends CompilationUnitRewriteOperationsFixCore {

	public static class RenameToUnnamedVariableOperation extends CompilationUnitRewriteOperation {

		private final SimpleName fName;

		public RenameToUnnamedVariableOperation(SimpleName name) {
			fName= name;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore model) throws CoreException {
			AST ast= cuRewrite.getAST();
			SimpleName newName= ast.newSimpleName("_"); //$NON-NLS-1$
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			TextEditGroup group= createTextEditGroup(FixMessages.UnusedCodeFix_RenameToUnnamedVariable_description, cuRewrite);
			rewrite.replace(fName, newName, group);
		}
	}

	public static RenameUnusedVariableFixCore createRenameToUnnamedFix(CompilationUnit compilationUnit, IProblemLocation problem) {
		if (isUnusedMember(problem) || isUnusedLambdaParameter(problem)) {
			SimpleName name= getUnusedName(compilationUnit, problem);
			if (name != null) {
				IBinding binding= name.resolveBinding();
				if (binding != null) {
					if (JavaModelUtil.is22OrHigher(compilationUnit.getJavaElement().getJavaProject())) {
						if (name.getParent() instanceof SingleVariableDeclaration parent &&
								parent.getParent() instanceof Pattern ||
								name.getParent() instanceof VariableDeclarationFragment parent2 &&
								parent2.getParent() instanceof LambdaExpression) {
							String label= FixMessages.UnusedCodeFix_RenameToUnnamedVariable_description;
							RenameToUnnamedVariableOperation operation= new RenameToUnnamedVariableOperation(name);
							return new RenameUnusedVariableFixCore(label, compilationUnit, new CompilationUnitRewriteOperation[] { operation }, getCleanUpOptions());
						}
					}
				}
			}
		}
		return null;
	}

	public static boolean isUnusedMember(IProblemLocation problem) {
		int id= problem.getProblemId();
		return id == IProblem.LocalVariableIsNeverUsed;
	}

	public static boolean isUnusedLambdaParameter(IProblemLocation problem) {
		int id= problem.getProblemId();
		return id == IProblem.LambdaParameterIsNeverUsed;
	}

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit,
			boolean removeUnusedLocalVariables) {

		IProblem[] problems= compilationUnit.getProblems();
		IProblemLocation[] locations= new IProblemLocation[problems.length];
		for (int i= 0; i < problems.length; i++) {
			locations[i]= new ProblemLocation(problems[i]);
		}

		return createCleanUp(compilationUnit, locations,
				removeUnusedLocalVariables);
	}


	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit, IProblemLocation[] problems,
			boolean removeUnusedLocalVariables) {

		List<CompilationUnitRewriteOperation> result= new ArrayList<>();
		for (IProblemLocation problem : problems) {
			int id= problem.getProblemId();

			if ((removeUnusedLocalVariables && id == IProblem.LocalVariableIsNeverUsed)
					|| (removeUnusedLocalVariables && id == IProblem.LambdaParameterIsNeverUsed)) {
				SimpleName name= getUnusedName(compilationUnit, problem);
				if (name != null) {
					IBinding binding= name.resolveBinding();
					if (binding instanceof IVariableBinding) {
						VariableDeclarationFragment parent= ASTNodes.getParent(name, VariableDeclarationFragment.class);
						if (parent == null || id == IProblem.LambdaParameterIsNeverUsed) {
							if (JavaModelUtil.is22OrHigher(compilationUnit.getJavaElement().getJavaProject())) {
								if (name.getParent() instanceof SingleVariableDeclaration nameParent &&
										nameParent.getParent() instanceof Pattern ||
										name.getParent() instanceof VariableDeclarationFragment varFragment &&
										varFragment.getParent() instanceof LambdaExpression) {
									result.add(new RenameToUnnamedVariableOperation(name));
								}
							}
						}
					}
				}
			}
		}

		if (result.isEmpty())
			return null;

		return new RenameUnusedVariableFixCore(FixMessages.UnusedCodeFix_change_name, compilationUnit, result.toArray(new CompilationUnitRewriteOperation[result.size()]));
	}

	public static boolean isFormalParameterInEnhancedForStatement(SimpleName name) {
		return name.getParent() instanceof SingleVariableDeclaration && name.getParent().getLocationInParent() == EnhancedForStatement.PARAMETER_PROPERTY;
	}

	public static SimpleName getUnusedName(CompilationUnit compilationUnit, IProblemLocation problem) {
		ASTNode selectedNode= problem.getCoveringNode(compilationUnit);

		if (selectedNode instanceof MethodDeclaration) {
			return ((MethodDeclaration) selectedNode).getName();
		} else if (selectedNode instanceof SimpleName) {
			return (SimpleName) selectedNode;
		}

		return null;
	}

	public static Map<String, String> getCleanUpOptions() {
		Map<String, String> result= new Hashtable<>();
		result.put(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES, CleanUpOptions.TRUE);
		return result;
	}

	private final Map<String, String> fCleanUpOptions;

	private RenameUnusedVariableFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation[] fixRewriteOperations) {
		this(name, compilationUnit, fixRewriteOperations, null);
	}

	private RenameUnusedVariableFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation[] fixRewriteOperations, Map<String, String> options) {
		super(name, compilationUnit, fixRewriteOperations);
		fCleanUpOptions= options;
	}

	public RenameUnusedVariableCleanUpCore getCleanUp() {
		if (fCleanUpOptions == null)
			return null;

		return new RenameUnusedVariableCleanUpCore(fCleanUpOptions);
	}

}
