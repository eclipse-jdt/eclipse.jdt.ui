/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.dom.ASTNodeConstants;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.dom.TypeRules;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class MissingReturnTypeCorrectionProposal extends LinkedCorrectionProposal {

	private static final String RETURN_EXPRESSION_KEY= "value"; //$NON-NLS-1$

	private MethodDeclaration fMethodDecl;
	private ReturnStatement fExistingReturn;

	public MissingReturnTypeCorrectionProposal(ICompilationUnit cu, MethodDeclaration decl, ReturnStatement existingReturn, int relevance) {
		super("", cu, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE)); //$NON-NLS-1$
		fMethodDecl= decl;
		fExistingReturn= existingReturn;
	}

	public String getDisplayString() {
		if (fExistingReturn != null) {
			return CorrectionMessages.getString("MissingReturnTypeCorrectionProposal.changereturnstatement.description"); //$NON-NLS-1$
		} else {
			return CorrectionMessages.getString("MissingReturnTypeCorrectionProposal.addreturnstatement.description"); //$NON-NLS-1$
		}
	}

	/*(non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal#getRewrite()
	 */
	protected ASTRewrite getRewrite() {
		AST ast= fMethodDecl.getAST();
		
		ITypeBinding returnBinding= getReturnTypeBinding();
		
		if (fExistingReturn != null) {
			ASTRewrite rewrite= new ASTRewrite(fExistingReturn.getParent());
			
			Expression expression= evaluateReturnExpressions(ast, returnBinding, fExistingReturn.getStartPosition());
			if (expression != null) {
				rewrite.markAsInsert(fExistingReturn, ASTNodeConstants.EXPRESSION, expression, null);
				
				markAsLinked(rewrite, expression, true, RETURN_EXPRESSION_KEY);
			}
			return rewrite;
		} else {
			ASTRewrite rewrite= new ASTRewrite(fMethodDecl);
			
			Block block= fMethodDecl.getBody();
				
			List statements= block.statements();
			int nStatements= statements.size();
			ASTNode lastStatement= null;
			if (nStatements > 0) {
				lastStatement= (ASTNode) statements.get(nStatements - 1);
			}
						
			if (returnBinding != null && lastStatement instanceof ExpressionStatement && lastStatement.getNodeType() != ASTNode.ASSIGNMENT) {
				Expression expression= ((ExpressionStatement) lastStatement).getExpression();
				ITypeBinding binding= expression.resolveTypeBinding();
				if (binding != null && TypeRules.canAssign(binding, returnBinding)) {
					Expression placeHolder= (Expression) rewrite.createMove(expression);
					
					ReturnStatement returnStatement= ast.newReturnStatement();
					returnStatement.setExpression(placeHolder);
					
					rewrite.markAsReplaced(lastStatement, returnStatement);
					return rewrite;
				}
			}
			
			int offset;
			if (lastStatement == null) {
				offset= block.getStartPosition() + 1;
			} else {
				offset= lastStatement.getStartPosition() + lastStatement.getLength();
			}
			ReturnStatement returnStatement= ast.newReturnStatement();
			Expression expression= evaluateReturnExpressions(ast, returnBinding, offset);
			
			returnStatement.setExpression(expression);

			rewrite.getListRewrite(block, ASTNodeConstants.STATEMENTS).insertLast(returnStatement, null);
			
			markAsLinked(rewrite, returnStatement.getExpression(), true, RETURN_EXPRESSION_KEY);
			return rewrite;
		}
	}
	
	private ITypeBinding getReturnTypeBinding() {
		IMethodBinding methodBinding= fMethodDecl.resolveBinding();
		if (methodBinding != null && methodBinding.getReturnType() != null) {
			return methodBinding.getReturnType();
		}
		return null;
	}
	
	
	/*
	 * Evaluates possible return expressions. The favourite expression is returned.
	 */
	private Expression evaluateReturnExpressions(AST ast, ITypeBinding returnBinding, int returnOffset) {
		CompilationUnit root= (CompilationUnit) fMethodDecl.getRoot();
	
		Expression result= null;
		if (returnBinding != null) {
			ScopeAnalyzer analyzer= new ScopeAnalyzer(root);
			IBinding[] bindings= analyzer.getDeclarationsInScope(returnOffset, ScopeAnalyzer.VARIABLES);
			for (int i= 0; i < bindings.length; i++) {
				IVariableBinding curr= (IVariableBinding) bindings[i];
				ITypeBinding type= curr.getType();
				if (type != null && TypeRules.canAssign(type, returnBinding) && testModifier(curr)) {
					if (result == null) {
						result= ast.newSimpleName(curr.getName());
					}
					addLinkedModeProposal(RETURN_EXPRESSION_KEY, curr.getName());
				}
			}
		}
		Expression defaultExpression= ASTNodeFactory.newDefaultExpression(ast, fMethodDecl.getReturnType(), fMethodDecl.getExtraDimensions());
		addLinkedModeProposal(RETURN_EXPRESSION_KEY, ASTNodes.asString(defaultExpression));
		if (result == null) {
			return defaultExpression;
		}
		return result;
	}

	private boolean testModifier(IVariableBinding curr) {
		int modifiers= curr.getModifiers();
		int staticFinal= Modifier.STATIC | Modifier.FINAL;
		if ((modifiers & staticFinal) == staticFinal) {
			return false;
		}
		if (Modifier.isStatic(modifiers) && !Modifier.isStatic(fMethodDecl.getModifiers())) {
			return false;
		}
		return true;
	}
	
}
