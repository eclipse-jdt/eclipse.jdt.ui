/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.examples;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;

import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal;

/**
 *
 */
public class MyQuickAssistProcessor implements IQuickAssistProcessor {
	
	private boolean getConvertProposal(IInvocationContext context, List result) {
		ASTNode node= context.getCoveringNode();
		if (!(node instanceof StringLiteral)) {
			return false;
		}
		if (result == null) {
			return true;
		}
		
		StringLiteral oldLiteral= (StringLiteral) node;
		
		AST ast= node.getAST();
		StringLiteral newLiteral= ast.newStringLiteral();
		newLiteral.setEscapedValue(oldLiteral.getEscapedValue().toUpperCase());
		
		ASTRewrite rewrite= ASTRewrite.create(ast);
		rewrite.replace(oldLiteral, newLiteral, null);
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		result.add(new ASTRewriteCorrectionProposal("To uppercase", context.getCompilationUnit(), rewrite, 10, image));
		return true;
	}
	
	private boolean getASTProposal(IInvocationContext context, List result) {
		ASTNode node= context.getASTRoot();
		
		AST ast= node.getAST();
		final ASTRewrite rewrite= ASTRewrite.create(ast);
		
		ASTVisitor visitor= new ASTVisitor() {
			public boolean visit(MethodInvocation inv) {
				if (!inv.arguments().isEmpty()) {
					return true;
				}
				Expression expression= inv.getExpression();
				if (expression == null || expression.resolveTypeBinding() == null) {
					return true;
				}
				ITypeBinding expressionBinding= expression.resolveTypeBinding();
				if (Bindings.findTypeInHierarchy(expressionBinding, ASTNode.class.getName()) == null) {
					return true;
				}
				IMethodBinding methodBinding= inv.resolveMethodBinding();
				if (methodBinding == null) {
					return true;
				}
				ITypeBinding returnType= methodBinding.getReturnType();
				String name= returnType.getName();
				if ("void".equals(name)) { //$NON-NLS-1$
					return true;
				}
				if ("getAST".equals(methodBinding.getName())) { //$NON-NLS-1$
					return true;
				}
				
				String invName;
				if ("int".equals(name)) { //$NON-NLS-1$
					invName= "getIntAttribute"; //$NON-NLS-1$
				} else if ("boolean".equals(name)) { //$NON-NLS-1$
					invName= "getBooleanAttribute"; //$NON-NLS-1$
				} else if ("List".equals(name)) { //$NON-NLS-1$
					invName= "getChildList"; //$NON-NLS-1$
				} else if (Bindings.findTypeInHierarchy(returnType, ASTNode.class.getName()) != null) {
					invName= "getChildNode"; //$NON-NLS-1$
				} else {
					invName= "getAttribute"; //$NON-NLS-1$
				}
				AST ast2= inv.getAST();
				MethodInvocation newInv= ast2.newMethodInvocation();
				newInv.arguments().add(rewrite.createCopyTarget(expression));
				
				Name arg= ast2.newName(new String[] { expressionBinding.getName(), toConstantName(methodBinding.getName()) });
				newInv.arguments().add(arg);
				newInv.setName(ast2.newSimpleName(invName));
				
				rewrite.replace(inv, newInv, null);
				return true;
			}
		};
		node.accept(visitor);
		
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		result.add(new ASTRewriteCorrectionProposal("Use AST Properties", context.getCompilationUnit(), rewrite, 10, image));
		return true;
	}
	
	private static String getPropertyName(String name) {
		if (name.startsWith("get")) {
			return name.substring(3);
		} else if (name.startsWith("is")) {
			return name;
		}
		return name;
	}
	
	private static String toConstantName(String name) {
		String string= getPropertyName(name);
		
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < string.length(); i++) {
			char ch= string.charAt(i);
			if (i != 0 && Character.isUpperCase(ch)) {
				buf.append('_');
			}
			buf.append(Character.toUpperCase(ch));
		}
		buf.append("_PROPERTY"); //$NON-NLS-1$
		return buf.toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IQuickAssistProcessor#hasAssists(org.eclipse.jdt.ui.text.java.IInvocationContext)
	 */
	public boolean hasAssists(IInvocationContext context) {
		return getConvertProposal(context, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IQuickAssistProcessor#getAssists(org.eclipse.jdt.ui.text.java.IInvocationContext, org.eclipse.jdt.ui.text.java.IProblemLocation[])
	 */
	public IJavaCompletionProposal[] getAssists(IInvocationContext context, IProblemLocation[] locations) {
		ArrayList resultingCollections= new ArrayList();
		getConvertProposal(context, resultingCollections);
		//getASTProposal(context, resultingCollections);
		return (IJavaCompletionProposal[]) resultingCollections.toArray(new IJavaCompletionProposal[resultingCollections.size()]);
	}

}
