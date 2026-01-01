/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.ui.CodeStyleConfiguration;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

public class AddAnnotationProposal implements IJavaCompletionProposal {

	private final IInvocationContext fContext;
	private final MethodDeclaration fMethodDecl;
	private final String fAnnotationQualifiedName;
	private final String fAnnotationSimpleName;

	public AddAnnotationProposal(IInvocationContext context, MethodDeclaration methodDecl, String annotationQualifiedName, String annotationSimpleName) {
		fContext = context;
		fMethodDecl = methodDecl;
		fAnnotationQualifiedName = annotationQualifiedName;
		fAnnotationSimpleName = annotationSimpleName;
	}

	@Override
	public void apply(IDocument document) {
		try {
			CompilationUnit astRoot = fContext.getASTRoot();
			ICompilationUnit cu = fContext.getCompilationUnit();

			AST ast = astRoot.getAST();
			ASTRewrite rewrite = ASTRewrite.create(ast);

			// Add the annotation
			org.eclipse.jdt.core.dom.MarkerAnnotation annotation = ast.newMarkerAnnotation();
			annotation.setTypeName(ast.newName(fAnnotationSimpleName));

			ListRewrite listRewrite = rewrite.getListRewrite(fMethodDecl, MethodDeclaration.MODIFIERS2_PROPERTY);
			listRewrite.insertFirst(annotation, null);

			// Add import
			ImportRewrite importRewrite = CodeStyleConfiguration.createImportRewrite(astRoot, true);
			importRewrite.addImport(fAnnotationQualifiedName);

			// Apply changes - rewrite AST first, then imports
			TextEdit rewriteEdit = rewrite.rewriteAST(document, cu.getOptions(true));
			TextEdit importEdit = importRewrite.rewriteImports(null);

			rewriteEdit.apply(document);
			importEdit.apply(document);

		} catch (CoreException | BadLocationException e) {
			JUnitPlugin.log(e);
		}
	}

	@Override
	public String getAdditionalProposalInfo() {
		return java.text.MessageFormat.format(JUnitMessages.JUnitQuickAssistProcessor_add_annotation_info, fAnnotationSimpleName);
	}

	@Override
	public IContextInformation getContextInformation() {
		return null;
	}

	@Override
	public String getDisplayString() {
		return java.text.MessageFormat.format(JUnitMessages.JUnitQuickAssistProcessor_add_annotation_description, fAnnotationSimpleName);
	}

	@Override
	public Image getImage() {
		return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_ANNOTATION);
	}

	@Override
	public Point getSelection(IDocument document) {
		return null;
	}

	@Override
	public int getRelevance() {
		return 10;
	}
}
