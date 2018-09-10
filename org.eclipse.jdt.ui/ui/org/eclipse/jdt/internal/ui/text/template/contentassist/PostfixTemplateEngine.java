/*******************************************************************************
 * Copyright (c) 2019 Nicolaj Hoess.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Nicolaj Hoess - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.template.contentassist;

import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContextType;

import org.eclipse.jdt.core.CompletionContext;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;

import org.eclipse.jdt.internal.corext.template.java.JavaContext;
import org.eclipse.jdt.internal.corext.template.java.JavaPostfixContext;
import org.eclipse.jdt.internal.corext.template.java.JavaPostfixContextType;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * An extension to the {@linkplain TemplateEngine} to override the creation of the
 * {@linkplain JavaContext}. This implementation creates a {@linkplain JavaPostfixContext} instead
 * of a {@linkplain JavaContext}.
 *
 * @since 3.10
 */
public class PostfixTemplateEngine extends TemplateEngine {

	private ASTNode currentNode;

	private ASTNode parentNode;

	private CompletionContext completionCtx;

	public PostfixTemplateEngine(TemplateContextType contextType) {
		super(contextType);
	}

	public void setASTNodes(ASTNode currentNode, ASTNode parentNode) {
		this.currentNode= currentNode;
		this.parentNode= parentNode;
	}

	@Override
	@Deprecated
	public void complete(ITextViewer viewer, int completionPosition, ICompilationUnit compilationUnit) {
		if (!(getContextType() instanceof JavaPostfixContextType)) {
			return;
		}
		complete(viewer, viewer.getSelectedRange(), completionPosition, compilationUnit);
	}

	@Override
	public void complete(ITextViewer viewer, Point selectedRange, int completionPosition, ICompilationUnit compilationUnit) {
		IDocument document= viewer.getDocument();

		if (!(getContextType() instanceof JavaPostfixContextType)) {
			return;
		}

		Point selection= selectedRange;

		String selectedText= null;
		if (selection.y != 0) {
			return;
		}

		JavaPostfixContext context= ((JavaPostfixContextType) getContextType()).createContext(document, completionPosition, selection.y, compilationUnit, currentNode, parentNode, completionCtx);
		context.setVariable("selection", selectedText); //$NON-NLS-1$
		int start= context.getStart();
		int end= context.getEnd();
		IRegion region= new Region(start, end - start);

		Template[] templates= JavaPlugin.getDefault().getTemplateStore().getTemplates(getContextType().getId());

		for (Template template : templates) {
			if (context.canEvaluate(template)) {
				getProposals().add(new PostfixTemplateProposal(template, context, region, getImage()));
			}
		}
	}

	public void setContext(CompletionContext context) {
		this.completionCtx= context;
	}
}
