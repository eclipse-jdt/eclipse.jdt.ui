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
package org.eclipse.jdt.ui.examples;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.dom.OldASTRewrite;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.ReplaceCorrectionProposal;

/**
 *
 */
public class MyQuickAssistProcessor implements IQuickAssistProcessor {
	

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.ICorrectionProcessor#hasCorrections(org.eclipse.jdt.core.ICompilationUnit, int)
	 */
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		return (problemId == IProblem.NumericValueOutOfRange);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.ICorrectionProcessor#getCorrections(org.eclipse.jdt.internal.ui.text.correction.IAssistContext, org.eclipse.jdt.internal.ui.text.correction.IProblemLocation[])
	 */
	public IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations) {
		for (int i= 0; i < locations.length; i++) {
			if (locations[i].getProblemId() == IProblem.NumericValueOutOfRange) {
				return getNumericValueOutOfRangeCorrection(context, locations[i]);
			}
		}
		return null;
	}

	private IJavaCompletionProposal[] getNumericValueOutOfRangeCorrection(IInvocationContext context, IProblemLocation location) {
		ICompilationUnit cu= context.getCompilationUnit();
		
		ReplaceCorrectionProposal proposal= new ReplaceCorrectionProposal("Change to 0", cu, location.getOffset(), location.getLength(), "0", 5);
		return new IJavaCompletionProposal[] { proposal };
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IQuickAssistProcessor#hasAssists(org.eclipse.jdt.ui.text.java.IInvocationContext)
	 */
	public boolean hasAssists(IInvocationContext context) {
		return getConvertProposal(context, null);
	}
	
	private boolean getConvertProposal(IInvocationContext context, Collection resultingCollections) {
		ASTNode node= getCoveringNode(context);	
		if (!(node instanceof SimpleName && node.getParent() instanceof MethodInvocation)) {
			return false;
			
		}
		MethodInvocation inv= (MethodInvocation) node.getParent();
		IMethodBinding binding= inv.resolveMethodBinding();
		if (binding == null || Bindings.findTypeInHierarchy(binding.getDeclaringClass(), "org.eclipse.jdt.core.dom.ASTNode") == null) {
			return false;
		}
		if (resultingCollections == null) {
			return true;
		}

		String constName= toConstantName(getPropertyName(binding.getName()));
			
		String str= ASTNodes.asString(inv.getExpression()) + ", ASTNodeConstants." + constName;
		if ("List".equals(binding.getReturnType().getName())) {
			str= "getChildList(" + str + ")";
		} else if ("int".equals(binding.getReturnType().getName())) {
			str= "getIntAttribute(" + str + ")";
		} else if ("boolean".equals(binding.getReturnType().getName())) {
			str= "getBooleanAttribute(" + str + ")";
		} else {
			str= "getChildNode(" + str + ")";
		}
		
		OldASTRewrite rewrite= new OldASTRewrite(inv.getParent());
		rewrite.replace(inv, rewrite.createStringPlaceholder(str, ASTNode.METHOD_INVOCATION), null);
		
		String label= "Use AST Constants";
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 10, image);
		resultingCollections.add(proposal);
		
		
		return false;
	}
	
	
	private static String getPropertyName(String name) {
		if (name.startsWith("get")) {
			return name.substring(3);
		} else if (name.startsWith("is")) {
			return name;
		}
		return name;
	}
	
	private static String toConstantName(String string) {
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < string.length(); i++) {
			char ch= string.charAt(i);
			if (i != 0 && Character.isUpperCase(ch)) {
				buf.append('_');
			}
			buf.append(Character.toUpperCase(ch));
		}
		return buf.toString();
	}
	
		
	
	
	private ASTNode getCoveringNode(IInvocationContext context) {
		NodeFinder finder= new NodeFinder(context.getSelectionOffset(), context.getSelectionLength());
		context.getASTRoot().accept(finder);
		return finder.getCoveringNode();	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IQuickAssistProcessor#getAssists(org.eclipse.jdt.ui.text.java.IInvocationContext, org.eclipse.jdt.ui.text.java.IProblemLocation[])
	 */
	public IJavaCompletionProposal[] getAssists(IInvocationContext context, IProblemLocation[] locations) {
		ArrayList resultingCollections= new ArrayList();
		getConvertProposal(context, resultingCollections);
		return (IJavaCompletionProposal[]) resultingCollections.toArray(new IJavaCompletionProposal[resultingCollections.size()]);
	}

}
