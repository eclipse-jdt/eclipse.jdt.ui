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

import java.lang.reflect.Modifier;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import org.eclipse.jdt.internal.corext.dom.ASTNodeConstants;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.dom.TypeRules;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class AddArgumentCorrectionProposal extends LinkedCorrectionProposal {

	private int[] fInsertIndexes;
	private ITypeBinding[] fParamTypes;
	private ASTNode fNameNode;

	public AddArgumentCorrectionProposal(String label, ICompilationUnit cu, ASTNode nameNode, int[] insertIdx, ITypeBinding[] expectedTypes, int relevance) {
		super(label, cu, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE)); //$NON-NLS-1$
		fNameNode= nameNode;
		fInsertIndexes= insertIdx;
		fParamTypes= expectedTypes;
	}

	/*(non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal#getRewrite()
	 */
	protected ASTRewrite getRewrite() {
		AST ast= fNameNode.getAST();
		ASTRewrite rewrite= new ASTRewrite(fNameNode.getParent());

		ASTNode callerNode= fNameNode.getParent();
		for (int i= 0; i < fInsertIndexes.length; i++) {
			int idx= fInsertIndexes[i];
			String key= "newarg_" + i; //$NON-NLS-1$
			Expression newArg= evaluateArgumentExpressions(ast, fParamTypes[idx], key);
			rewrite.markAsInsertInNew(callerNode, ASTNodeConstants.ARGUMENTS, newArg, idx, null);
			
			markAsLinked(rewrite, newArg, i == 0, key); 
		}
		return rewrite;
	}
	
	private Expression evaluateArgumentExpressions(AST ast, ITypeBinding requiredType, String key) {
		CompilationUnit root= (CompilationUnit) fNameNode.getRoot();

		int offset= fNameNode.getStartPosition();
		Expression best= null;
		
		ScopeAnalyzer analyzer= new ScopeAnalyzer(root);
		IBinding[] bindings= analyzer.getDeclarationsInScope(offset, ScopeAnalyzer.VARIABLES);
		for (int i= 0; i < bindings.length; i++) {
			IVariableBinding curr= (IVariableBinding) bindings[i];
			ITypeBinding type= curr.getType();
			if (type != null && TypeRules.canAssign(type, requiredType) && testModifier(curr)) {
				if (best == null) {
					best= ast.newSimpleName(curr.getName());
				}
				addLinkedModeProposal(key, curr.getName());
			}
		}
		Expression defaultExpression= ASTNodeFactory.newDefaultExpression(ast, requiredType);
		if (best == null) {
			best= defaultExpression;
		}
		addLinkedModeProposal(key, ASTNodes.asString(defaultExpression));
		return best;
	}
	
	private boolean testModifier(IVariableBinding curr) {
		int modifiers= curr.getModifiers();
		int staticFinal= Modifier.STATIC | Modifier.FINAL;
		if ((modifiers & staticFinal) == staticFinal) {
			return false;
		}
		if (Modifier.isStatic(modifiers) && !ASTResolving.isInStaticContext(fNameNode)) {
			return false;
		}
		return true;
	}	
}
