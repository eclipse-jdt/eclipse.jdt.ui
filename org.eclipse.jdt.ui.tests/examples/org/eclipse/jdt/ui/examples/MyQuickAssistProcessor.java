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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;

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
		return (IJavaCompletionProposal[]) resultingCollections.toArray(new IJavaCompletionProposal[resultingCollections.size()]);
	}

}
