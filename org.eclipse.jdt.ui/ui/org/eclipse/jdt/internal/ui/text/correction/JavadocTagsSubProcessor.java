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

import java.util.ArrayList;
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

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.corext.Assert;
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

	private static final class AddJavadocCommentProposal extends CUCorrectionProposal {
		
	 	private final int fInsertPosition;
		private final String fComment;
		
		private AddJavadocCommentProposal(String name, ICompilationUnit cu, int relevance, int insertPosition, String comment) {
			super(name, cu, relevance, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG));
			fInsertPosition= insertPosition;
			fComment= comment;
		}
		
		protected void addEdits(IDocument document, TextEdit rootEdit) throws CoreException {
			try {
				String lineDelimiter= TextUtilities.getDefaultLineDelimiter(document);
				int tabWidth= CodeFormatterUtil.getTabWidth();
				IRegion region= document.getLineInformationOfOffset(fInsertPosition);
				
				String lineContent= document.get(region.getOffset(), region.getLength());
				String indentString= Strings.getIndentString(lineContent, tabWidth);
				String str= Strings.changeIndent(fComment, 0, tabWidth, indentString, lineDelimiter);
				InsertEdit edit= new InsertEdit(fInsertPosition, str);
				rootEdit.addChild(edit);
				if (fComment.charAt(fComment.length() - 1) != '\n') {
					rootEdit.addChild(new InsertEdit(fInsertPosition, lineDelimiter)); 
					rootEdit.addChild(new InsertEdit(fInsertPosition, indentString));
				}
			} catch (BadLocationException e) {
				throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
			}
		}
	}

	private static final class AddMissingJavadocTagProposal extends LinkedCorrectionProposal {
		
		private final BodyDeclaration fBodyDecl; // MethodDecl or TypeDecl
		private final ASTNode fMissingNode;

		public AddMissingJavadocTagProposal(String label, ICompilationUnit cu, BodyDeclaration methodDecl, ASTNode missingNode, int relevance) {
			super(label, cu, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG));
			fBodyDecl= methodDecl;
			fMissingNode= missingNode;
		}
		
		protected ASTRewrite getRewrite() throws CoreException {
			AST ast= fBodyDecl.getAST();
			ASTRewrite rewrite= ASTRewrite.create(ast);
		 	insertMissingJavadocTag(rewrite, fMissingNode, fBodyDecl);
			return rewrite;
		}

		private void insertMissingJavadocTag(ASTRewrite rewrite, ASTNode missingNode, BodyDeclaration bodyDecl) {
			AST ast= bodyDecl.getAST();
			Javadoc javadoc= bodyDecl.getJavadoc();
		 	ListRewrite tagsRewriter= rewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);
			
		 	StructuralPropertyDescriptor location= missingNode.getLocationInParent();
		 	TagElement newTag;
		 	if (location == SingleVariableDeclaration.NAME_PROPERTY) {
		 		// normal parameter
		 		SingleVariableDeclaration decl= (SingleVariableDeclaration) missingNode.getParent();
		 		
				String name= ((SimpleName) missingNode).getIdentifier();
				newTag= ast.newTagElement();
				newTag.setTagName(TagElement.TAG_PARAM);
				List fragments= newTag.fragments();
				fragments.add(ast.newSimpleName(name));
				
				MethodDeclaration methodDeclaration= (MethodDeclaration) bodyDecl;
				List params= methodDeclaration.parameters();
				
				Set sameKindLeadingNames= getPreviousParamNames(params, decl);
				
				List typeParams= methodDeclaration.typeParameters();
				for (int i= 0; i < typeParams.size(); i++) {
					String curr= ((TypeParameter) typeParams.get(i)).getName().getIdentifier();
					sameKindLeadingNames.add(curr);
				}
				insertTag(tagsRewriter, newTag, sameKindLeadingNames);
		 	} else if (location == TypeParameter.NAME_PROPERTY) {
		 		// type parameter
		 		TypeParameter typeParam= (TypeParameter) missingNode.getParent();
		 		
				String name= ((SimpleName) missingNode).getIdentifier();
				newTag= ast.newTagElement();
				newTag.setTagName(TagElement.TAG_PARAM);
				TextElement text= ast.newTextElement();
				text.setText('<' + name + '>');
				newTag.fragments().add(text);
				List params;
				if (bodyDecl instanceof TypeDeclaration) {
					params= ((TypeDeclaration) bodyDecl).typeParameters();
				} else {
					params= ((MethodDeclaration) bodyDecl).typeParameters();
				}
				insertTag(tagsRewriter, newTag, getPreviousTypeParamNames(params, typeParam));
		 	} else if (location == MethodDeclaration.RETURN_TYPE2_PROPERTY) {
				newTag= ast.newTagElement();
				newTag.setTagName(TagElement.TAG_RETURN);
				insertTag(tagsRewriter, newTag, null);
		 	} else if (location == MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY) {
				newTag= ast.newTagElement();
				newTag.setTagName(TagElement.TAG_THROWS);
				List exceptions= ((MethodDeclaration) bodyDecl).thrownExceptions();
				insertTag(tagsRewriter, newTag, getPreviousExceptionNames(exceptions, missingNode));
		 	} else {
		 		Assert.isTrue(false, "AddMissingJavadocTagProposal: unexpected node location"); //$NON-NLS-1$
		 		return;
		 	}
		 	
			TextElement textElement= ast.newTextElement();
			textElement.setText(""); //$NON-NLS-1$
			newTag.fragments().add(textElement);
			addLinkedPosition(rewrite.track(textElement), false, "comment_start"); //$NON-NLS-1$
		}
	}
	
	private static final class AddAllMissingJavadocTagsProposal extends LinkedCorrectionProposal {
		
		private final BodyDeclaration fBodyDecl;

		public AddAllMissingJavadocTagsProposal(String label, ICompilationUnit cu, BodyDeclaration bodyDecl, int relevance) {
			super(label, cu, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG));
			fBodyDecl= bodyDecl;
		}
		
		protected ASTRewrite getRewrite() throws CoreException {
			ASTRewrite rewrite= ASTRewrite.create(fBodyDecl.getAST());
			if (fBodyDecl instanceof MethodDeclaration) {
				insertAllMissingMethodTags(rewrite, (MethodDeclaration) fBodyDecl);
			} else {
				insertAllMissingTypeTags(rewrite, (TypeDeclaration) fBodyDecl);
			}
			return rewrite;
		}

		private void insertAllMissingMethodTags(ASTRewrite rewriter, MethodDeclaration methodDecl) {
		 	AST ast= methodDecl.getAST();
		 	Javadoc javadoc= methodDecl.getJavadoc();
		 	ListRewrite tagsRewriter= rewriter.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);
		 	
		 	List typeParams= methodDecl.typeParameters();
		 	List typeParamNames= new ArrayList();
		 	for (int i= typeParams.size() - 1; i >= 0 ; i--) {
		 		TypeParameter decl= (TypeParameter) typeParams.get(i);
		 		String name= decl.getName().getIdentifier();
		 		if (findTag(javadoc, TagElement.TAG_PARAM, name) == null) {
		 			TagElement newTag= ast.newTagElement();
		 			newTag.setTagName(TagElement.TAG_PARAM);
		 			TextElement text= ast.newTextElement();
		 			text.setText('<' + name + '>');
		 			newTag.fragments().add(text);
					insertTabStop(rewriter, newTag.fragments(), "typeParam" + i); //$NON-NLS-1$
		 			insertTag(tagsRewriter, newTag, getPreviousTypeParamNames(typeParams, decl));
		 		}
				typeParamNames.add(name);
		 	}
		 	List params= methodDecl.parameters();
		 	for (int i= params.size() - 1; i >= 0 ; i--) {
		 		SingleVariableDeclaration decl= (SingleVariableDeclaration) params.get(i);
		 		String name= decl.getName().getIdentifier();
		 		if (findTag(javadoc, TagElement.TAG_PARAM, name) == null) {
		 			TagElement newTag= ast.newTagElement();
		 			newTag.setTagName(TagElement.TAG_PARAM);
		 			newTag.fragments().add(ast.newSimpleName(name));
					insertTabStop(rewriter, newTag.fragments(), "methParam" + i); //$NON-NLS-1$
		 			Set sameKindLeadingNames= getPreviousParamNames(params, decl);
		 			sameKindLeadingNames.addAll(typeParamNames);
		 			insertTag(tagsRewriter, newTag, sameKindLeadingNames);
		 		}
		 	}
		 	if (!methodDecl.isConstructor()) {
		 		Type type= methodDecl.getReturnType2();
		 		if (!type.isPrimitiveType() || (((PrimitiveType) type).getPrimitiveTypeCode() != PrimitiveType.VOID)) {
		 			if (findTag(javadoc, TagElement.TAG_RETURN, null) == null) {
		 				TagElement newTag= ast.newTagElement();
		 				newTag.setTagName(TagElement.TAG_RETURN);
						insertTabStop(rewriter, newTag.fragments(), "return"); //$NON-NLS-1$
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
						insertTabStop(rewriter, newTag.fragments(), "exception" + i); //$NON-NLS-1$
		 				insertTag(tagsRewriter, newTag, getPreviousExceptionNames(thrownExceptions, exception));
		 			}
		 		}
		 	}
		 }

		private void insertAllMissingTypeTags(ASTRewrite rewriter, TypeDeclaration typeDecl) {
			AST ast= typeDecl.getAST();
			Javadoc javadoc= typeDecl.getJavadoc();
			ListRewrite tagsRewriter= rewriter.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);

			List typeParams= typeDecl.typeParameters();
			for (int i= typeParams.size() - 1; i >= 0; i--) {
				TypeParameter decl= (TypeParameter) typeParams.get(i);
				String name= decl.getName().getIdentifier();
				if (findTag(javadoc, TagElement.TAG_PARAM, name) == null) {
					TagElement newTag= ast.newTagElement();
					newTag.setTagName(TagElement.TAG_PARAM);
					TextElement text= ast.newTextElement();
					text.setText('<' + name + '>');
					newTag.fragments().add(text);
					insertTabStop(rewriter, newTag.fragments(), "typeParam" + i); //$NON-NLS-1$
					insertTag(tagsRewriter, newTag, getPreviousTypeParamNames(typeParams, decl));
				}
			}
		}
		
		private void insertTabStop(ASTRewrite rewriter, List fragments, String linkedName) {
			TextElement textElement= rewriter.getAST().newTextElement();
			textElement.setText(""); //$NON-NLS-1$
			fragments.add(textElement);
			addLinkedPosition(rewriter.track(textElement), false, linkedName);
		}
		
	}
	 
	public static void getMissingJavadocTagProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
	 	ASTNode node= problem.getCoveringNode(context.getASTRoot());
	 	if (node == null) {
	 		return;
	 	}
	 	node= ASTNodes.getNormalizedNode(node);
	 	
	 	BodyDeclaration bodyDeclaration= ASTResolving.findParentBodyDeclaration(node);
	 	if (bodyDeclaration == null) {
	 		return;
	 	}
	 	Javadoc javadoc= bodyDeclaration.getJavadoc();
	 	if (javadoc == null) {
	 		return;
	 	}
	 	
	 	String label;
	 	StructuralPropertyDescriptor location= node.getLocationInParent();
	 	if (location == SingleVariableDeclaration.NAME_PROPERTY) {
	 		label= CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.paramtag.description"); //$NON-NLS-1$
	 		if (node.getParent().getLocationInParent() != MethodDeclaration.PARAMETERS_PROPERTY) {
	 			return; // paranoia checks
	 		}
	 	} else if (location == TypeParameter.NAME_PROPERTY) {
	 		label= CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.paramtag.description"); //$NON-NLS-1$
	 		StructuralPropertyDescriptor parentLocation= node.getParent().getLocationInParent();
	 		if (parentLocation != MethodDeclaration.TYPE_PARAMETERS_PROPERTY && parentLocation != TypeDeclaration.TYPE_PARAMETERS_PROPERTY) {
	 			return; // paranoia checks
	 		}
	 	} else if (location == MethodDeclaration.RETURN_TYPE2_PROPERTY) {
	 		label= CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.returntag.description"); //$NON-NLS-1$
	 	} else if (location == MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY) {
	 		label= CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.throwstag.description"); //$NON-NLS-1$
	 	} else {
	 		return;
	 	}
	 	ASTRewriteCorrectionProposal proposal= new AddMissingJavadocTagProposal(label, context.getCompilationUnit(), bodyDeclaration, node, 1); //$NON-NLS-1$
	 	proposals.add(proposal);
	 	
	 	String label2= CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.allmissing.description"); //$NON-NLS-1$
	 	ASTRewriteCorrectionProposal addAllMissing= new AddAllMissingJavadocTagsProposal(label2, context.getCompilationUnit(), bodyDeclaration, 5); //$NON-NLS-1$
	 	proposals.add(addAllMissing);
	}

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
				proposals.add(new AddJavadocCommentProposal(label, cu, 1, declaration.getStartPosition(), string));
			}
		} else if (declaration instanceof AbstractTypeDeclaration) {
			String typeQualifiedName= Bindings.getTypeQualifiedName(binding);
			String[] typeParamNames;
			if (declaration instanceof TypeDeclaration) {
				List typeParams= ((TypeDeclaration) declaration).typeParameters();
				typeParamNames= new String[typeParams.size()];
				for (int i= 0; i < typeParamNames.length; i++) {
					typeParamNames[i]= ((TypeParameter) typeParams.get(i)).getName().getIdentifier();
				}
			} else {
				typeParamNames= new String[0];
			}
			String string= CodeGeneration.getTypeComment(cu, typeQualifiedName, typeParamNames, String.valueOf('\n'));
			if (string != null) {
				String label= CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.type.description"); //$NON-NLS-1$
				proposals.add(new AddJavadocCommentProposal(label, cu, 1, declaration.getStartPosition(), string));
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
			if (comment != null) {
				String label= CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.field.description"); //$NON-NLS-1$
				proposals.add(new AddJavadocCommentProposal(label, cu, 1, declaration.getStartPosition(), comment));
			}
		} else if (declaration instanceof EnumConstantDeclaration) {
			EnumConstantDeclaration enumDecl= (EnumConstantDeclaration) declaration;
			String id= enumDecl.getName().getIdentifier();
			String comment= CodeGeneration.getFieldComment(cu, binding.getName(), id, String.valueOf('\n'));
			String label= CorrectionMessages.getString("JavadocTagsSubProcessor.addjavadoc.enumconst.description"); //$NON-NLS-1$
			proposals.add(new AddJavadocCommentProposal(label, cu, 1, declaration.getStartPosition(), comment));
		}
	}
	 
	private static Set getPreviousTypeParamNames(List typeParams, ASTNode missingNode) {
		Set previousNames=  new HashSet();
		for (int i = 0; i < typeParams.size(); i++) {
			TypeParameter curr= (TypeParameter) typeParams.get(i);
			if (curr == missingNode) {
				return previousNames;
			}
			previousNames.add(curr.getName().getIdentifier());
		}
		return previousNames;
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
			} else if (first instanceof TextElement && TagElement.TAG_PARAM.equals(curr.getTagName())) {
				String text= ((TextElement) first).getText();
				if (text.startsWith(String.valueOf('<')) && text.endsWith(String.valueOf('>'))) {
					return text.substring(1, text.length() - 1).trim();
				}
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
