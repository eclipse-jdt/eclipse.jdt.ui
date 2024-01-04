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
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;

import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2Core;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public class AddInferredLambdaParameterTypesFixCore extends CompilationUnitRewriteOperationsFixCore {

	public AddInferredLambdaParameterTypesFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation operation) {
		super(name, compilationUnit, operation);
	}

	public static AddInferredLambdaParameterTypesFixCore createAddInferredLambdaParameterTypesFix(CompilationUnit compilationUnit, ASTNode node) {
		LambdaExpression lambda= null;
		boolean isLambdaParamVarType= false;
		ASTNode parent= node.getParent();
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
			IJavaElement root= compilationUnit.getJavaElement();
			if (root != null) {
				IJavaProject javaProject= root.getJavaProject();
				if (javaProject != null && JavaModelUtil.is11OrHigher(javaProject)) {
					if (((SingleVariableDeclaration) firstLambdaParam).getType().isVar()) {
						isLambdaParamVarType= true;
					}
				}
			}
			if (!isLambdaParamVarType) {
				return null;
			}
		}

		IMethodBinding methodBinding= lambda.resolveMethodBinding();
		if (methodBinding == null)
			return null;

		String label= CorrectionMessages.QuickAssistProcessor_add_inferred_lambda_parameter_types;
		if (isLambdaParamVarType) {
			label= CorrectionMessages.QuickAssistProcessor_replace_var_with_inferred_lambda_parameter_types;
		}

		return new AddInferredLambdaParameterTypesFixCore(label, compilationUnit, new AddInferredLambdaParameterTypesProposalOperation(lambda, methodBinding, noOfLambdaParams, lambdaParameters));
	}

	private static class AddInferredLambdaParameterTypesProposalOperation extends CompilationUnitRewriteOperation {

		private LambdaExpression lambda;

		private IMethodBinding methodBinding;

		private int noOfLambdaParams;

		private List<VariableDeclaration> lambdaParameters;

		public AddInferredLambdaParameterTypesProposalOperation(LambdaExpression lambda, IMethodBinding methodBinding, int noOfLambdaParams, List<VariableDeclaration> lambdaParameters) {
			this.lambda= lambda;
			this.methodBinding= methodBinding;
			this.noOfLambdaParams= noOfLambdaParams;
			this.lambdaParameters= lambdaParameters;
		}


		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
			AST ast= cuRewrite.getAST();
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			ImportRewrite importRewrite= cuRewrite.getImportRewrite();

			rewrite.set(lambda, LambdaExpression.PARENTHESES_PROPERTY, Boolean.TRUE, null);
			ContextSensitiveImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(lambda, importRewrite);
			ITypeBinding[] parameterTypes= methodBinding.getParameterTypes();
			for (int i= 0; i < noOfLambdaParams; i++) {
				VariableDeclaration param= lambdaParameters.get(i);
				SingleVariableDeclaration newParam= ast.newSingleVariableDeclaration();
				newParam.setName(ast.newSimpleName(param.getName().getIdentifier()));
				ITypeBinding type= StubUtility2Core.replaceWildcardsAndCaptures(parameterTypes[i]);
				newParam.setType(importRewrite.addImport(type, ast, importRewriteContext, TypeLocation.PARAMETER));
				rewrite.replace(param, newParam, null);
			}

		}
	}
}
