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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.ListRewrite;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIStatus;

/**
 *
 */
public class JavadocTagsSubProcessor {

	
	public static void getMissingJavadocCommentProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
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
			List fragments= ((FieldDeclaration)declaration).fragments();
			if (fragments != null && fragments.size() > 0) {
				VariableDeclaration decl= (VariableDeclaration)fragments.get(0);
				String fieldName= decl.getName().getIdentifier();
				String typeName= binding.getName();
				comment= CodeGeneration.getFieldComment(cu, typeName, fieldName, String.valueOf('\n'));
			}
			String label= CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.field.description"); //$NON-NLS-1$
			proposals.add(getNewJavadocTagProposal(cu, declaration.getStartPosition(), comment, label));
		}
	}

	private static CUCorrectionProposal getNewJavadocTagProposal(ICompilationUnit cu, final int insertPosition, final String comment, String label) {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG);
		
		return new CUCorrectionProposal(label, cu, 1, image) {
			protected void addEdits(IDocument document, TextEdit rootEdit) throws CoreException {
				try {
					String lineDelimiter= TextUtilities.getDefaultLineDelimiter(document);
					int tabWidth= CodeFormatterUtil.getTabWidth();
					IRegion region= document.getLineInformationOfOffset(insertPosition);
					
					String lineContent= document.get(region.getOffset(), region.getLength());
					String indentString= Strings.getIndentString(lineContent, tabWidth);
					String str= Strings.changeIndent(comment, 0, tabWidth, indentString, lineDelimiter);
					InsertEdit edit= new InsertEdit(insertPosition, str);
					rootEdit.addChild(edit); //$NON-NLS-1$
					if (comment.charAt(comment.length() - 1) != '\n') {
						rootEdit.addChild(new InsertEdit(insertPosition, lineDelimiter)); 
						rootEdit.addChild(new InsertEdit(insertPosition, indentString));
					}
				} catch (BadLocationException e) {
					throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
				}
			}
		};
	}
	
	public static void getMissingJavadocTagProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		ASTNode node= problem.getCoveringNode(context.getASTRoot());
		if (node == null) {
			return;
		}
		
		BodyDeclaration declaration= ASTResolving.findParentBodyDeclaration(node);
		if (!(declaration instanceof MethodDeclaration)) {
			return;
		}
		
		Javadoc javadoc= declaration.getJavadoc();
		if (javadoc == null) {
			return;
		}
		
		MethodDeclaration methodDecl= (MethodDeclaration) declaration;
		
		AST ast= javadoc.getAST();
		ASTRewrite rewrite= new ASTRewrite(ast);
		ListRewrite tagsRewriter= rewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);
		
		
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", context.getCompilationUnit(), rewrite, 1, image); //$NON-NLS-1$
		
		switch (problem.getProblemId()) {
			case IProblem.JavadocMissingParamTag: {
				String name= ASTNodes.asString(node);
				proposal.setDisplayName(CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.paramtag.description")); //$NON-NLS-1$
				TagElement newTag= ast.newTagElement();
				newTag.setTagName("@param"); //$NON-NLS-1$
				newTag.fragments().add(ast.newSimpleName(name));
				insertParamTag(javadoc, tagsRewriter, newTag, node.getParent());
				break;
			}
			case IProblem.JavadocMissingReturnTag: {
				proposal.setDisplayName(CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.returntag.description")); //$NON-NLS-1$
				TagElement newTag= ast.newTagElement();
				newTag.setTagName("@return"); //$NON-NLS-1$
				insertReturnTag(javadoc, tagsRewriter, newTag);
				break;
			}
			case IProblem.JavadocMissingThrowsTag: {
				proposal.setDisplayName(CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.throwstag.description")); //$NON-NLS-1$
				TagElement newTag= ast.newTagElement();
				newTag.setTagName("@throws"); //$NON-NLS-1$
				insertThrowsTag(javadoc, tagsRewriter, newTag, node);
				break;
			}
			default:
				return;
		}		
		proposals.add(proposal);
		
		rewrite= new ASTRewrite(ast);
		tagsRewriter= rewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);
		
		String label= CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.allmissing.description"); //$NON-NLS-1$
		ASTRewriteCorrectionProposal addAllMissing= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 5, image); //$NON-NLS-1$
		List list= methodDecl.parameters();
		for (int i= list.size() - 1; i >= 0 ; i--) {
			SingleVariableDeclaration decl= (SingleVariableDeclaration) list.get(i);
			String name= decl.getName().getIdentifier();
			if (findTag(javadoc, "@param", name) == null) { //$NON-NLS-1$
				TagElement newTag= ast.newTagElement();
				newTag.setTagName("@param"); //$NON-NLS-1$
				newTag.fragments().add(ast.newSimpleName(name));
				insertParamTag(javadoc, tagsRewriter, newTag, decl);
			}
		}
		if (!methodDecl.isConstructor()) {
			Type type= methodDecl.getReturnType();
			if (!type.isPrimitiveType() || (((PrimitiveType) type).getPrimitiveTypeCode() != PrimitiveType.VOID)) {
				if (findTag(javadoc, "@return", null) == null) { //$NON-NLS-1$
					TagElement newTag= ast.newTagElement();
					newTag.setTagName("@return"); //$NON-NLS-1$
					insertReturnTag(javadoc, tagsRewriter, newTag);
				}
			}
		}
		List throwsExceptions= methodDecl.thrownExceptions();
		for (int i= throwsExceptions.size() - 1; i >= 0 ; i--) {
			Name exception= (Name) throwsExceptions.get(i);
			ITypeBinding binding= exception.resolveTypeBinding();
			if (binding != null) {
				String name= binding.getName();
				if (findThrowsTag(javadoc, name) == null) {
					TagElement newTag= ast.newTagElement();
					newTag.setTagName("@throws"); //$NON-NLS-1$
					newTag.fragments().add(ast.newSimpleName(name));
					insertThrowsTag(javadoc, tagsRewriter, newTag, exception);
				}
			}
		}
		proposals.add(addAllMissing);
	}
	
	public static TagElement findTag(Javadoc javadoc, String name, String arg) {
		List tags= javadoc.tags();
		int nTags= tags.size();
		for (int i= 0; i < nTags; i++) {
			TagElement curr= (TagElement) tags.get(i);
			if (name.equals(curr.getTagName())) {
				if (arg != null) {
					String argument= getArgument(curr);
					if (arg.equals(argument)) {
						return curr;
					}
				} else {
					return curr;
				}
			}
		}
		return null;
	}
	
	private static TagElement findThrowsTag(Javadoc javadoc, String arg) {
		List tags= javadoc.tags();
		int nTags= tags.size();
		for (int i= 0; i < nTags; i++) {
			TagElement curr= (TagElement) tags.get(i);
			String currName= curr.getTagName();
			if ("@throws".equals(currName) || "@exception".equals(currName)) {  //$NON-NLS-1$//$NON-NLS-2$
				String argument= getArgument(curr);
				if (arg.equals(argument)) {
					return curr;
				}
			}
		}
		return null;
	}
	
	
	private static void insertThrowsTag(Javadoc javadoc, ListRewrite rewriter, TagElement newElement, ASTNode node) {
		Set previousArgs= new HashSet();
		List list= ((MethodDeclaration) javadoc.getParent()).thrownExceptions();
		for (int i= 0; i < list.size() && node != list.get(i); i++) {
			Name curr= (Name) list.get(i);
			previousArgs.add(ASTResolving.getSimpleName(curr));
		}
		List tags= rewriter.getRewrittenList();
		
		ASTNode before= null;
		for (int i= tags.size() - 1; i >= 0; i--) {
			TagElement curr= (TagElement) tags.get(i);
			String tagName= curr.getTagName();
			if ("@throws".equals(tagName) || "@exception".equals(tagName)) {  //$NON-NLS-1$//$NON-NLS-2$
				String arg= getArgument(curr);
				if (arg != null && previousArgs.contains(arg)) {
					rewriter.insertAfter(newElement, curr, null);
					return;
				}
				before= curr;
			}
		}
		if (before != null) {
			rewriter.insertBefore(newElement, before, null);
		} else {
			rewriter.insertLast(newElement, null);
		}
	}

	private static void insertReturnTag(Javadoc javadoc, ListRewrite rewriter, TagElement newElement) {
		List tags= rewriter.getRewrittenList();

		ASTNode before= null;
		
		for (int i= tags.size() - 1; i >= 0; i--) {
			TagElement curr= (TagElement) tags.get(i);
			String tagName=curr.getTagName();
			if ("@throws".equals(tagName) || "@exception".equals(tagName)) { //$NON-NLS-1$ //$NON-NLS-2$
				before= curr;
			} else if ("@param".equals(tagName)) { //$NON-NLS-1$
				break;
			}
		}
		if (before != null) {
			rewriter.insertBefore(newElement, before, null);
		} else {
			rewriter.insertLast(newElement, null);
		}
	}

	private static void insertParamTag(Javadoc javadoc, ListRewrite rewriter, TagElement newElement, ASTNode node) {
		Set previousArgs= new HashSet();

		List list= ((MethodDeclaration) javadoc.getParent()).parameters();
		for (int i= 0; i < list.size() && (list.get(i) != node); i++) {
			SingleVariableDeclaration curr= (SingleVariableDeclaration) list.get(i);
			previousArgs.add(curr.getName().getIdentifier());
		}
		List tags= rewriter.getRewrittenList();
		for (int i= tags.size() - 1; i >= 0; i--) {
			TagElement curr= (TagElement) tags.get(i);
			if ("@param".equals(curr.getTagName())) { //$NON-NLS-1$
				String arg= getArgument(curr);
				if (arg != null && previousArgs.contains(arg)) {
					rewriter.insertAfter(newElement, curr, null);
					return;
				}
			}
		}
		if (!tags.isEmpty()) {
			TagElement first= (TagElement) tags.get(0);
			if (first.getTagName() == null) {
				rewriter.insertAfter(newElement, first, null);
				return;
			}
		}
		rewriter.insertFirst(newElement, null);
	}
	
	private static String getArgument(TagElement curr) {
		List fragments= curr.fragments();
		if (!fragments.isEmpty()) {
			Object first= fragments.get(0);
			if (first instanceof Name) {
				return ASTResolving.getSimpleName((Name) first);
			}
		}
		return null;
	}
}
