/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Gayan Perera - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.ImportRemover;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public class RemoveVarOrInferredLambdaParameterTypesFixCore extends CompilationUnitRewriteOperationsFixCore {

	public RemoveVarOrInferredLambdaParameterTypesFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation operation) {
		super(name, compilationUnit, operation);
	}

	public static RemoveVarOrInferredLambdaParameterTypesFixCore createRemoveVarOrInferredLambdaParameterTypesFix(CompilationUnit compilationUnit, ASTNode node) {
		IJavaElement root= compilationUnit.getJavaElement();
		ASTNode parent= node.getParent();
		if (parent == null || root == null) {
			return null;
		}
		IJavaProject javaProject= root.getJavaProject();
		if (javaProject == null) {
			return null;
		}
		boolean checkForVarTypes= false;
		if (JavaModelUtil.is11OrHigher(javaProject)) {
			checkForVarTypes= true;
		}

		LambdaExpression lambda;
		if (node instanceof LambdaExpression) {
			lambda= (LambdaExpression) node;
		} else if (node.getLocationInParent() == SingleVariableDeclaration.NAME_PROPERTY &&
				parent.getLocationInParent() == LambdaExpression.PARAMETERS_PROPERTY) {
			lambda= (LambdaExpression) node.getParent().getParent();
		} else {
			return null;
		}

		List<VariableDeclaration> lambdaParameters= lambda.parameters();
		int noOfLambdaParams= lambdaParameters.size();
		if (noOfLambdaParams == 0)
			return null;

		if (!(lambdaParameters.get(0) instanceof SingleVariableDeclaration))
			return null;

		IMethodBinding methodBinding= lambda.resolveMethodBinding();
		if (methodBinding == null)
			return null;

		String label= CorrectionMessages.QuickAssistProcessor_remove_lambda_parameter_types;
		return new RemoveVarOrInferredLambdaParameterTypesFixCore(label, compilationUnit,
				new RemoveVarOrInferredLambdaParameterTypesProposalOperation(lambda, noOfLambdaParams, lambdaParameters, checkForVarTypes));
	}

	private static class RemoveVarOrInferredLambdaParameterTypesProposalOperation extends CompilationUnitRewriteOperation {

		private LambdaExpression lambda;

		private int noOfLambdaParams;

		private List<VariableDeclaration> lambdaParameters;

		private boolean checkForVarTypes;

		public RemoveVarOrInferredLambdaParameterTypesProposalOperation(LambdaExpression lambda, int noOfLambdaParams, List<VariableDeclaration> lambdaParameters, boolean checkForVarTypes) {
			this.lambda= lambda;
			this.noOfLambdaParams= noOfLambdaParams;
			this.lambdaParameters= lambdaParameters;
			this.checkForVarTypes= checkForVarTypes;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
			AST ast= cuRewrite.getAST();
			ASTRewrite rewrite= cuRewrite.getASTRewrite();

			ImportRemover remover= cuRewrite.getImportRemover();

			rewrite.set(lambda, LambdaExpression.PARENTHESES_PROPERTY, Boolean.TRUE, null);
			for (int i= 0; i < noOfLambdaParams; i++) {
				VariableDeclaration param= lambdaParameters.get(i);
				Type oldType= null;
				if (param instanceof SingleVariableDeclaration) {
					SingleVariableDeclaration curParent= (SingleVariableDeclaration) param;
					oldType= curParent.getType();
					if (oldType != null && (!checkForVarTypes || (checkForVarTypes && !oldType.isVar()))) {
						remover.registerRemovedNode(oldType);
					}
					VariableDeclarationFragment newParam= ast.newVariableDeclarationFragment();
					newParam.setName(ast.newSimpleName(param.getName().getIdentifier()));
					rewrite.replace(param, newParam, null);
				}
			}
		}
	}
}
