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

public class AddVarLambdaParameterTypesFixCore extends CompilationUnitRewriteOperationsFixCore {

	public AddVarLambdaParameterTypesFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation operation) {
		super(name, compilationUnit, operation);
	}

	public static AddVarLambdaParameterTypesFixCore createAddVarLambdaParameterTypesFix(CompilationUnit compilationUnit, ASTNode node) {
		IJavaElement root= compilationUnit.getJavaElement();
		ASTNode parent= node.getParent();
		if (parent == null || root == null) {
			return null;
		}
		IJavaProject javaProject= root.getJavaProject();
		if (javaProject == null) {
			return null;
		}
		if (!JavaModelUtil.is11OrHigher(javaProject)) {
			return null;
		}

		LambdaExpression lambda= null;
		boolean isLambdaParamExplicitType= false;
		if (node instanceof LambdaExpression) {
			lambda= (LambdaExpression) node;
		} else if ((node.getLocationInParent() == VariableDeclarationFragment.NAME_PROPERTY
				|| node.getLocationInParent() == SingleVariableDeclaration.NAME_PROPERTY)
				&& parent.getLocationInParent() == LambdaExpression.PARAMETERS_PROPERTY) {
			lambda= (LambdaExpression) parent.getParent();
		}

		if (lambda == null) {
			return null;
		}

		List<VariableDeclaration> lambdaParameters= lambda.parameters();
		int noOfLambdaParams= lambdaParameters.size();
		if (noOfLambdaParams == 0)
			return null;

		VariableDeclaration firstLambdaParam= lambdaParameters.get(0);
		if (firstLambdaParam instanceof SingleVariableDeclaration) {
			if (!((SingleVariableDeclaration) firstLambdaParam).getType().isVar()) {
				isLambdaParamExplicitType= true;
			} else {
				return null;
			}
		}

		IMethodBinding methodBinding= lambda.resolveMethodBinding();
		if (methodBinding == null)
			return null;
		String label= null;
		if (isLambdaParamExplicitType) {
			label= CorrectionMessages.QuickAssistProcessor_replace_lambda_parameter_types_with_var;
		} else {
			label= CorrectionMessages.QuickAssistProcessor_add_var_lambda_parameter_types;
		}
		return new AddVarLambdaParameterTypesFixCore(label, compilationUnit, new AddVarLambdaParameterTypesProposalOperation(lambda, lambdaParameters, noOfLambdaParams));
	}

	private static class AddVarLambdaParameterTypesProposalOperation extends CompilationUnitRewriteOperation {
		private ASTNode lambda;

		private List<VariableDeclaration> lambdaParameters;

		private int noOfLambdaParams;

		public AddVarLambdaParameterTypesProposalOperation(ASTNode lambda, List<VariableDeclaration> lambdaParameters, int noOfLambdaParams) {
			this.lambda= lambda;
			this.lambdaParameters= lambdaParameters;
			this.noOfLambdaParams= noOfLambdaParams;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
			String VAR_TYPE= "var"; //$NON-NLS-1$
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
					if (oldType != null) {
						rewrite.replace(oldType, ast.newSimpleType(ast.newName(VAR_TYPE)), null);
						remover.registerRemovedNode(oldType);
					}
				}
				if (oldType == null) {
					SingleVariableDeclaration newParam= ast.newSingleVariableDeclaration();
					newParam.setName(ast.newSimpleName(param.getName().getIdentifier()));
					newParam.setType(ast.newSimpleType(ast.newName(VAR_TYPE)));
					rewrite.replace(param, newParam, null);
				}
			}
		}
	}

}
