/*******************************************************************************
 * Copyright (c) 2025 Carsten Hammer and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer using github cooilot - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.ui.CodeStyleConfiguration;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

public class RemoveAnnotationProposal implements IJavaCompletionProposal {

	private final IInvocationContext fContext;
	private final MethodDeclaration fMethodDecl;
	private final String fAnnotationQualifiedName;

	public RemoveAnnotationProposal(IInvocationContext context, MethodDeclaration methodDecl, String annotationQualifiedName) {
		fContext = context;
		fMethodDecl = methodDecl;
		fAnnotationQualifiedName = annotationQualifiedName;
	}

	@Override
	public void apply(IDocument document) {
		try {
			CompilationUnit astRoot = fContext.getASTRoot();
			ICompilationUnit cu = fContext.getCompilationUnit();

			AST ast = astRoot.getAST();
			ASTRewrite rewrite = ASTRewrite.create(ast);

			// Find and remove the annotation
			IMethodBinding methodBinding = fMethodDecl.resolveBinding();
			if (methodBinding != null) {
				IAnnotationBinding[] annotations = methodBinding.getAnnotations();
				for (IAnnotationBinding annotationBinding : annotations) {
					ITypeBinding annotationType = annotationBinding.getAnnotationType();
					if (annotationType != null && fAnnotationQualifiedName.equals(annotationType.getQualifiedName())) {
						// Find the matching annotation node in the modifiers
						List<?> modifiers = fMethodDecl.modifiers();

						for (Object modifier : modifiers) {
							if (modifier instanceof Annotation) {
								Annotation annotation = (Annotation) modifier;
								if (annotation.resolveAnnotationBinding() == annotationBinding) {
									ListRewrite listRewrite = rewrite.getListRewrite(fMethodDecl, MethodDeclaration.MODIFIERS2_PROPERTY);
									listRewrite.remove(annotation, null);
									break;
								}
							}
						}
						break;
					}
				}
			}

			// Remove unused import
			ImportRewrite importRewrite = CodeStyleConfiguration.createImportRewrite(astRoot, true);
			importRewrite.removeImport(fAnnotationQualifiedName);

			// Combine both edits using MultiTextEdit to avoid conflicts
			MultiTextEdit multiEdit = new MultiTextEdit();
			
			TextEdit importEdit = importRewrite.rewriteImports(null);
			if (importEdit.hasChildren() || importEdit.getLength() != 0) {
				multiEdit.addChild(importEdit);
			}
			
			TextEdit rewriteEdit = rewrite.rewriteAST(document, cu.getOptions(true));
			if (rewriteEdit.hasChildren() || rewriteEdit.getLength() != 0) {
				multiEdit.addChild(rewriteEdit);
			}

			// Apply the combined edit
			if (multiEdit.hasChildren()) {
				multiEdit.apply(document);
			}

		} catch (CoreException | BadLocationException e) {
			JUnitPlugin.log(e);
		}
	}

	@Override
	public String getAdditionalProposalInfo() {
		String simpleName = fAnnotationQualifiedName.substring(fAnnotationQualifiedName.lastIndexOf('.') + 1);
		return java.text.MessageFormat.format(JUnitMessages.JUnitQuickAssistProcessor_remove_annotation_info, simpleName);
	}

	@Override
	public IContextInformation getContextInformation() {
		return null;
	}

	@Override
	public String getDisplayString() {
		String simpleName = fAnnotationQualifiedName.substring(fAnnotationQualifiedName.lastIndexOf('.') + 1);
		return java.text.MessageFormat.format(JUnitMessages.JUnitQuickAssistProcessor_remove_annotation_description, simpleName);
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
