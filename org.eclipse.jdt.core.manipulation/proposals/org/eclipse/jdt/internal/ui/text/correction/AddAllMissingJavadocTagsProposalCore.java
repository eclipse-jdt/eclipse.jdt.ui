package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;

import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposalCore;

public final class AddAllMissingJavadocTagsProposalCore extends LinkedCorrectionProposalCore {


	private final ASTNode fDecl;
	public AddAllMissingJavadocTagsProposalCore(String label, ICompilationUnit cu, ASTNode decl, int relevance) {
		super(label, cu, null, relevance);
		fDecl= decl;
	}
	@Override
	protected ASTRewrite getRewrite() throws CoreException {
		ASTRewrite rewrite= ASTRewrite.create(fDecl.getAST());
		if (fDecl instanceof MethodDeclaration) {
			insertAllMissingMethodTags(rewrite, (MethodDeclaration) fDecl);
		} else if (fDecl instanceof RecordDeclaration) {
			insertAllMissingRecordTypeTags(rewrite, (RecordDeclaration) fDecl);
		} else {
			insertAllMissingTypeTags(rewrite, (TypeDeclaration) fDecl);
		}
		return rewrite;
	}

	private void insertAllMissingMethodTags(ASTRewrite rewriter, MethodDeclaration methodDecl) {
	 	AST ast= methodDecl.getAST();
	 	Javadoc javadoc= methodDecl.getJavadoc();
	 	ListRewrite tagsRewriter= rewriter.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);

	 	List<TypeParameter> typeParams= methodDecl.typeParameters();
	 	List<String> typeParamNames= new ArrayList<>();
	 	for (int i= typeParams.size() - 1; i >= 0 ; i--) {
	 		TypeParameter decl= typeParams.get(i);
	 		String name= '<' + decl.getName().getIdentifier() + '>';
	 		if (JavadocTagsSubProcessorCore.findTag(javadoc, TagElement.TAG_PARAM, name) == null) {
	 			TagElement newTag= ast.newTagElement();
	 			newTag.setTagName(TagElement.TAG_PARAM);
	 			TextElement text= ast.newTextElement();
	 			text.setText(name);
	 			newTag.fragments().add(text);
				insertTabStop(rewriter, newTag.fragments(), "typeParam" + i); //$NON-NLS-1$
				JavadocTagsSubProcessorCore.insertTag(tagsRewriter, newTag, JavadocTagsSubProcessorCore.getPreviousTypeParamNames(typeParams, decl));
	 		}
			typeParamNames.add(name);
	 	}
	 	List<SingleVariableDeclaration> params= methodDecl.parameters();
	 	for (int i= params.size() - 1; i >= 0 ; i--) {
	 		SingleVariableDeclaration decl= params.get(i);
	 		String name= decl.getName().getIdentifier();
	 		if (JavadocTagsSubProcessorCore.findTag(javadoc, TagElement.TAG_PARAM, name) == null) {
	 			TagElement newTag= ast.newTagElement();
	 			newTag.setTagName(TagElement.TAG_PARAM);
	 			newTag.fragments().add(ast.newSimpleName(name));
				insertTabStop(rewriter, newTag.fragments(), "methParam" + i); //$NON-NLS-1$
	 			Set<String> sameKindLeadingNames= JavadocTagsSubProcessorCore.getPreviousParamNames(params, decl);
	 			sameKindLeadingNames.addAll(typeParamNames);
	 			JavadocTagsSubProcessorCore.insertTag(tagsRewriter, newTag, sameKindLeadingNames);
	 		}
	 	}
	 	if (!methodDecl.isConstructor()) {
	 		Type type= methodDecl.getReturnType2();
	 		if (!type.isPrimitiveType() || (((PrimitiveType) type).getPrimitiveTypeCode() != PrimitiveType.VOID)) {
	 			if (JavadocTagsSubProcessorCore.findTag(javadoc, TagElement.TAG_RETURN, null) == null) {
	 				TagElement newTag= ast.newTagElement();
	 				newTag.setTagName(TagElement.TAG_RETURN);
					insertTabStop(rewriter, newTag.fragments(), "return"); //$NON-NLS-1$
					JavadocTagsSubProcessorCore.insertTag(tagsRewriter, newTag, null);
	 			}
	 		}
	 	}
		List<Type> thrownExceptions= methodDecl.thrownExceptionTypes();
	 	for (int i= thrownExceptions.size() - 1; i >= 0 ; i--) {
			Type exception= thrownExceptions.get(i);
			ITypeBinding binding= exception.resolveBinding();
	 		if (binding != null) {
	 			String name= binding.getName();
	 			if (JavadocTagsSubProcessorCore.findThrowsTag(javadoc, name) == null) {
	 				TagElement newTag= ast.newTagElement();
	 				newTag.setTagName(TagElement.TAG_THROWS);
					TextElement excNode= ast.newTextElement();
					excNode.setText(ASTNodes.getQualifiedTypeName(exception));
	 				newTag.fragments().add(excNode);
					insertTabStop(rewriter, newTag.fragments(), "exception" + i); //$NON-NLS-1$
					JavadocTagsSubProcessorCore.insertTag(tagsRewriter, newTag, JavadocTagsSubProcessorCore.getPreviousExceptionNames(thrownExceptions, exception));
	 			}
	 		}
	 	}
	}

	private void insertAllMissingTypeTags(ASTRewrite rewriter, TypeDeclaration typeDecl) {
		AST ast= typeDecl.getAST();
		Javadoc javadoc= typeDecl.getJavadoc();
		ListRewrite tagsRewriter= rewriter.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);

		List<TypeParameter> typeParams= typeDecl.typeParameters();
		for (int i= typeParams.size() - 1; i >= 0; i--) {
			TypeParameter decl= typeParams.get(i);
			String name= '<' + decl.getName().getIdentifier() + '>';
			if (JavadocTagsSubProcessorCore.findTag(javadoc, TagElement.TAG_PARAM, name) == null) {
				TagElement newTag= ast.newTagElement();
				newTag.setTagName(TagElement.TAG_PARAM);
				TextElement text= ast.newTextElement();
				text.setText(name);
				newTag.fragments().add(text);
				insertTabStop(rewriter, newTag.fragments(), "typeParam" + i); //$NON-NLS-1$
				JavadocTagsSubProcessorCore.insertTag(tagsRewriter, newTag, JavadocTagsSubProcessorCore.getPreviousTypeParamNames(typeParams, decl));
			}
		}
	}

	private void insertAllMissingRecordTypeTags(ASTRewrite rewriter, RecordDeclaration recDecl) {
		AST ast= recDecl.getAST();
		Javadoc javadoc= recDecl.getJavadoc();
		ListRewrite tagsRewriter= rewriter.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);

		List<SingleVariableDeclaration> recComps= recDecl.recordComponents();
		for (int i= recComps.size() - 1; i >= 0; i--) {
			SingleVariableDeclaration decl= recComps.get(i);
			String name= decl.getName().getIdentifier();
			if (JavadocTagsSubProcessorCore.findTag(javadoc, TagElement.TAG_PARAM, name) == null) {
				TagElement newTag= ast.newTagElement();
				newTag.setTagName(TagElement.TAG_PARAM);
				TextElement text= ast.newTextElement();
				text.setText(name);
				newTag.fragments().add(text);
				insertTabStop(rewriter, newTag.fragments(), "recComps" + i); //$NON-NLS-1$
				JavadocTagsSubProcessorCore.insertTag(tagsRewriter, newTag, JavadocTagsSubProcessorCore.getPreviousParamNames(recComps, decl));
			}
		}

		List<TypeParameter> typeParams= recDecl.typeParameters();
		for (int i= typeParams.size() - 1; i >= 0; i--) {
			TypeParameter decl= typeParams.get(i);
			String name= '<' + decl.getName().getIdentifier() + '>';
			if (JavadocTagsSubProcessorCore.findTag(javadoc, TagElement.TAG_PARAM, name) == null) {
				TagElement newTag= ast.newTagElement();
				newTag.setTagName(TagElement.TAG_PARAM);
				TextElement text= ast.newTextElement();
				text.setText(name);
				newTag.fragments().add(text);
				insertTabStop(rewriter, newTag.fragments(), "typeParam" + i); //$NON-NLS-1$
				JavadocTagsSubProcessorCore.insertTag(tagsRewriter, newTag, JavadocTagsSubProcessorCore.getPreviousTypeParamNames(typeParams, decl));
			}
		}
	}

	private void insertTabStop(ASTRewrite rewriter, List<ASTNode> fragments, String linkedName) {
		TextElement textElement= rewriter.getAST().newTextElement();
		textElement.setText(""); //$NON-NLS-1$
		fragments.add(textElement);
		addLinkedPosition(rewriter.track(textElement), false, linkedName);
	}

}