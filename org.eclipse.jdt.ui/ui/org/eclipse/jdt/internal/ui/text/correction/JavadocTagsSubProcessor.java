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

import java.util.Collection;

import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 *
 */
public class JavadocTagsSubProcessor {

	
	public static void getMissingJavadocProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		ASTNode node= problem.getCoveringNode(context.getASTRoot());
		if (node == null) {
			return;
		}
		BodyDeclaration declaration= ASTResolving.findParentBodyDeclaration(node);
		if (declaration == null) {
			return;
		}
		ICompilationUnit cu= context.getCompilationUnit();
		ITypeBinding binding= Bindings.getBindingOfParentType(declaration);
		if (binding == null) {
			return;
		}
		
		
		if (declaration instanceof MethodDeclaration) {
			MethodDeclaration methodDecl= (MethodDeclaration) declaration;
			IMethodBinding methodBinding= methodDecl.resolveBinding();
			if (methodBinding != null) {
				methodBinding= Bindings.findDeclarationInHierarchy(binding, methodBinding.getName(), methodBinding.getParameterTypes());
			}

			String string= CodeGeneration.getMethodComment(cu, binding.getName(), methodDecl, methodBinding, String.valueOf('\n'));
			if (string != null) {
				String label= CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.method.description"); //$NON-NLS-1$
				proposals.add(getNewJavadocTagProposal(cu, declaration.getStartPosition(), string, label));
			}
		} else if (declaration instanceof TypeDeclaration) {
			String typeQualifiedName= Bindings.getTypeQualifiedName(binding);
			
			String string= CodeGeneration.getTypeComment(cu, typeQualifiedName, String.valueOf('\n'));
			if (string != null) {
				String label= CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.type.description"); //$NON-NLS-1$
				proposals.add(getNewJavadocTagProposal(cu, declaration.getStartPosition(), string, label));
			}
		} else if (declaration instanceof FieldDeclaration) {
			 String comment= "/**\n *\n */\n"; //$NON-NLS-1$
			 String label= CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.field.description"); //$NON-NLS-1$
			 proposals.add(getNewJavadocTagProposal(cu, declaration.getStartPosition(), comment, label));
		}
	}

	private static CUCorrectionProposal getNewJavadocTagProposal(ICompilationUnit cu, int insertPosition, String comment, String label) throws CoreException {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG);
		
		CUCorrectionProposal proposal= new CUCorrectionProposal(label, cu, 1, image);
		TextBuffer buffer= TextBuffer.acquire((IFile) cu.getResource());
		try {
			int tabWidth= CodeFormatterUtil.getTabWidth();
			int line= buffer.getLineOfOffset(insertPosition);
			String lineContent= buffer.getLineContent(line);
			String indentString= Strings.getIndentString(lineContent, tabWidth);
			String str= Strings.changeIndent(comment, 0, tabWidth, indentString, buffer.getLineDelimiter());
			TextEdit rootEdit= proposal.getRootTextEdit();
			InsertEdit edit= new InsertEdit(insertPosition, str);
			rootEdit.addChild(edit); //$NON-NLS-1$
			if (comment.charAt(comment.length() - 1) != '\n') {
				rootEdit.addChild(new InsertEdit(insertPosition, buffer.getLineDelimiter())); 
				rootEdit.addChild(new InsertEdit(insertPosition, indentString));
			}
			
			return proposal;
		} finally {
			TextBuffer.release(buffer);
		}
	}

	
}
