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
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.ui.ISharedImages;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.ui.JavaPlugin;
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
					rootEdit.addChild(edit);
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
		ASTRewrite rewrite= ASTRewrite.create(ast);
		ListRewrite tagsRewriter= rewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);
		
		
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", context.getCompilationUnit(), rewrite, 1, image); //$NON-NLS-1$
		
		switch (problem.getProblemId()) {
			case IProblem.JavadocMissingParamTag: {
				String name= ASTNodes.asString(node);
				proposal.setDisplayName(CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.paramtag.description")); //$NON-NLS-1$
				TagElement newTag= ast.newTagElement();
				newTag.setTagName(TagElement.TAG_PARAM);
				newTag.fragments().add(ast.newSimpleName(name));
				
				List params= methodDecl.parameters();
				insertTag(tagsRewriter, newTag, getPreviousParamNames(params, node.getParent()));
				break;
			}
			case IProblem.JavadocMissingReturnTag: {
				proposal.setDisplayName(CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.returntag.description")); //$NON-NLS-1$
				TagElement newTag= ast.newTagElement();
				newTag.setTagName(TagElement.TAG_RETURN);
				insertTag(tagsRewriter, newTag, null);
				break;
			}
			case IProblem.JavadocMissingThrowsTag: {
				proposal.setDisplayName(CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.throwstag.description")); //$NON-NLS-1$
				TagElement newTag= ast.newTagElement();
				newTag.setTagName(TagElement.TAG_THROWS);
				List exceptions= methodDecl.thrownExceptions();
				insertTag(tagsRewriter, newTag, getPreviousExceptionNames(exceptions, node));
				break;
			}
			default:
				return;
		}		
		proposals.add(proposal);
		
		rewrite= ASTRewrite.create(ast);
		tagsRewriter= rewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);
		
		String label= CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.allmissing.description"); //$NON-NLS-1$
		ASTRewriteCorrectionProposal addAllMissing= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 5, image); //$NON-NLS-1$
		List list= methodDecl.parameters();
		for (int i= list.size() - 1; i >= 0 ; i--) {
			SingleVariableDeclaration decl= (SingleVariableDeclaration) list.get(i);
			String name= decl.getName().getIdentifier();
			if (findTag(javadoc, TagElement.TAG_PARAM, name) == null) {
				TagElement newTag= ast.newTagElement();
				newTag.setTagName(TagElement.TAG_PARAM);
				newTag.fragments().add(ast.newSimpleName(name));
				insertTag(tagsRewriter, newTag, getPreviousParamNames(list, decl));
			}
		}
		if (!methodDecl.isConstructor()) {
			Type type= methodDecl.getReturnType2();
			if (!type.isPrimitiveType() || (((PrimitiveType) type).getPrimitiveTypeCode() != PrimitiveType.VOID)) {
				if (findTag(javadoc, TagElement.TAG_RETURN, null) == null) {
					TagElement newTag= ast.newTagElement();
					newTag.setTagName(TagElement.TAG_RETURN);
					insertTag(tagsRewriter, newTag, null);
				}
			}
		}
		List thrownExceptions= methodDecl.thrownExceptions();
		for (int i= thrownExceptions.size() - 1; i >= 0 ; i--) {
			Name exception= (Name) thrownExceptions.get(i);
			ITypeBinding binding= exception.resolveTypeBinding();
			if (binding != null) {
				String name= binding.getName();
				if (findThrowsTag(javadoc, name) == null) {
					TagElement newTag= ast.newTagElement();
					newTag.setTagName(TagElement.TAG_THROWS);
					newTag.fragments().add(ast.newSimpleName(name));
					insertTag(tagsRewriter, newTag, getPreviousExceptionNames(thrownExceptions, exception));
				}
			}
		}
		proposals.add(addAllMissing);
	}
	
	private static Set getPreviousParamNames(List params, ASTNode missingNode) {
		Set previousNames=  new HashSet();
		for (int i = 0; i < params.size(); i++) {
			SingleVariableDeclaration curr= (SingleVariableDeclaration) params.get(i);
			if (curr == missingNode) {
				return previousNames;
			}
			previousNames.add(curr.getName().getIdentifier());
		}
		return previousNames;
	}
	
	private static Set getPreviousExceptionNames(List list, ASTNode missingNode) {
		Set previousNames=  new HashSet();
		for (int i= 0; i < list.size() && missingNode != list.get(i); i++) {
			Name curr= (Name) list.get(i);
			previousNames.add(ASTNodes.getSimpleNameIdentifier(curr));
		}
		return previousNames;
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
	
	public static TagElement findParamTag(Javadoc javadoc, String arg) {
		List tags= javadoc.tags();
		int nTags= tags.size();
		for (int i= 0; i < nTags; i++) {
			TagElement curr= (TagElement) tags.get(i);
			String currName= curr.getTagName();
			if (TagElement.TAG_PARAM.equals(currName)) {
				String argument= getArgument(curr);
				if (arg.equals(argument)) {
					return curr;
				}
			}
		}
		return null;
	}
	
	
	public static TagElement findThrowsTag(Javadoc javadoc, String arg) {
		List tags= javadoc.tags();
		int nTags= tags.size();
		for (int i= 0; i < nTags; i++) {
			TagElement curr= (TagElement) tags.get(i);
			String currName= curr.getTagName();
			if (TagElement.TAG_THROWS.equals(currName) || TagElement.TAG_EXCEPTION.equals(currName)) {  //$NON-NLS-1$//$NON-NLS-2$
				String argument= getArgument(curr);
				if (arg.equals(argument)) {
					return curr;
				}
			}
		}
		return null;
	}
	
	public static void insertTag(ListRewrite rewriter, TagElement newElement, Set sameKindLeadingNames) {
		insertTag(rewriter, newElement, sameKindLeadingNames, null);
	}
	
	public static void insertTag(ListRewrite rewriter, TagElement newElement, Set sameKindLeadingNames, TextEditGroup groupDescription) {
		List tags= rewriter.getRewrittenList();
		
		String insertedTagName= newElement.getTagName();
		
		ASTNode after= null;
		for (int i= tags.size() - 1; i >= 0; i--) {
			TagElement curr= (TagElement) tags.get(i);
			String tagName= curr.getTagName();
			if (tagName == null || isTagLeading(insertedTagName, tagName)) {
				after= curr;
				break;
			}
			if (sameKindLeadingNames != null && isSameTag(insertedTagName, tagName)) {
				String arg= getArgument(curr);
				if (arg != null && sameKindLeadingNames.contains(arg)) {
					after= curr;
					break;
				}
			}
		}
		if (after != null) {
			rewriter.insertAfter(newElement, after, groupDescription);
		} else {
			rewriter.insertFirst(newElement, groupDescription);
		}
	}
		
	private static boolean isSameTag(String insertedTagName, String tagName) {
		if (insertedTagName.equals(tagName)) {
			return true;
		}
		if (TagElement.TAG_EXCEPTION.equals(tagName)) {
			return TagElement.TAG_THROWS.equals(insertedTagName);
		}
		return false;
	}

	private static boolean isTagLeading(String insertedTagName, String tagName) {
		if (TagElement.TAG_EXCEPTION.equals(insertedTagName) || TagElement.TAG_THROWS.equals(insertedTagName)) {
			return TagElement.TAG_PARAM.equals(tagName) || TagElement.TAG_RETURN.equals(tagName);
		} else if (TagElement.TAG_RETURN.equals(insertedTagName)) {
			return TagElement.TAG_PARAM.equals(tagName);
		}
		return false;
	}

	
	private static String getArgument(TagElement curr) {
		List fragments= curr.fragments();
		if (!fragments.isEmpty()) {
			Object first= fragments.get(0);
			if (first instanceof Name) {
				return ASTNodes.getSimpleNameIdentifier((Name) first);
			}
		}
		return null;
	}

	public static void getRemoveJavadocTagProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		ASTNode node= problem.getCoveringNode(context.getASTRoot());
		while (node != null && !(node instanceof TagElement)) {
			node= node.getParent();
		}
		if (node == null) {
			return;
		}
		ASTRewrite rewrite= ASTRewrite.create(node.getAST());
		rewrite.remove(node, null);
		
		String label= CorrectionMessages.getString("JavadocTagsSubProcessor.removetag.description"); //$NON-NLS-1$
		Image image= JavaPlugin.getDefault().getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
		proposals.add(new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 5, image)); //$NON-NLS-1$
	}
}
