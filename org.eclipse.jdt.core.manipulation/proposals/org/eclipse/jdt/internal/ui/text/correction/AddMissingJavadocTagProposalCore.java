package org.eclipse.jdt.internal.ui.text.correction;

import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;

import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposalCore;

public class AddMissingJavadocTagProposalCore extends LinkedCorrectionProposalCore {


	private final ASTNode fDecl; // MethodDecl or TypeDecl
	private final ASTNode fMissingNode;
	public AddMissingJavadocTagProposalCore(String label, ICompilationUnit cu, ASTNode decl, ASTNode missingNode, int relevance) {
		super(label, cu, null, relevance);
		fDecl= decl;
		fMissingNode= missingNode;
	}

	@Override
	protected ASTRewrite getRewrite() throws CoreException {
		AST ast= fDecl.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		insertMissingJavadocTag(rewrite, fMissingNode, (BodyDeclaration)fDecl);
		return rewrite;
	}

	private void insertMissingJavadocTag(ASTRewrite rewrite, ASTNode missingNode, BodyDeclaration bodyDecl) {
		AST ast= bodyDecl.getAST();
		Javadoc javadoc= bodyDecl.getJavadoc();
		if (javadoc == null) {
			javadoc= ast.newJavadoc();
			rewrite.set(bodyDecl, bodyDecl.getJavadocProperty(), javadoc, null);
		}

	 	ListRewrite tagsRewriter= rewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);

	 	StructuralPropertyDescriptor location= missingNode.getLocationInParent();
	 	TagElement newTag;
	 	if (location == SingleVariableDeclaration.NAME_PROPERTY) {
	 		// normal parameter
	 		SingleVariableDeclaration decl= (SingleVariableDeclaration) missingNode.getParent();

			String name= ((SimpleName) missingNode).getIdentifier();
			newTag= ast.newTagElement();
			newTag.setTagName(TagElement.TAG_PARAM);
			newTag.fragments().add(ast.newSimpleName(name));

			if (bodyDecl instanceof MethodDeclaration) {
				MethodDeclaration methodDeclaration= (MethodDeclaration) bodyDecl;
				List<SingleVariableDeclaration> params= methodDeclaration.parameters();

				Set<String> sameKindLeadingNames= JavadocTagsSubProcessorCore.getPreviousParamNames(params, decl);

				List<TypeParameter> typeParams= methodDeclaration.typeParameters();
				for (TypeParameter typeParam : typeParams) {
					String curr= '<' + typeParam.getName().getIdentifier() + '>';
					sameKindLeadingNames.add(curr);
				}
				JavadocTagsSubProcessorCore.insertTag(tagsRewriter, newTag, sameKindLeadingNames);
			} else if (bodyDecl instanceof RecordDeclaration) {
				RecordDeclaration recordDeclaration= (RecordDeclaration) bodyDecl;
				List<SingleVariableDeclaration> params= recordDeclaration.recordComponents();

				Set<String> sameKindLeadingNames= JavadocTagsSubProcessorCore.getPreviousParamNames(params, decl);

				List<TypeParameter> typeParams= recordDeclaration.typeParameters();
				for (TypeParameter typeParam : typeParams) {
					String curr= '<' + typeParam.getName().getIdentifier() + '>';
					sameKindLeadingNames.add(curr);
				}
				JavadocTagsSubProcessorCore.insertTag(tagsRewriter, newTag, sameKindLeadingNames);
			}
	 	} else if (location == TypeParameter.NAME_PROPERTY) {
	 		// type parameter
	 		TypeParameter typeParam= (TypeParameter) missingNode.getParent();

			String name= '<' + ((SimpleName) missingNode).getIdentifier() + '>';
			newTag= ast.newTagElement();
			newTag.setTagName(TagElement.TAG_PARAM);
			TextElement text= ast.newTextElement();
			text.setText(name);
			newTag.fragments().add(text);
			List<TypeParameter> params;
			if (bodyDecl instanceof TypeDeclaration) {
				params= ((TypeDeclaration) bodyDecl).typeParameters();
			} else if (bodyDecl instanceof RecordDeclaration) {
				params= ((RecordDeclaration) bodyDecl).typeParameters();
			} else {
				params= ((MethodDeclaration) bodyDecl).typeParameters();
			}
			JavadocTagsSubProcessorCore.insertTag(tagsRewriter, newTag, JavadocTagsSubProcessorCore.getPreviousTypeParamNames(params, typeParam));
	 	} else if (location == MethodDeclaration.RETURN_TYPE2_PROPERTY) {
			newTag= ast.newTagElement();
			newTag.setTagName(TagElement.TAG_RETURN);
			JavadocTagsSubProcessorCore.insertTag(tagsRewriter, newTag, null);
		} else if (location == MethodDeclaration.THROWN_EXCEPTION_TYPES_PROPERTY) {
			newTag= ast.newTagElement();
			newTag.setTagName(TagElement.TAG_THROWS);
			TextElement excNode= ast.newTextElement();
			excNode.setText(ASTNodes.getQualifiedTypeName((Type) missingNode));
			newTag.fragments().add(excNode);
			List<Type> exceptions= ((MethodDeclaration) bodyDecl).thrownExceptionTypes();
			JavadocTagsSubProcessorCore.insertTag(tagsRewriter, newTag, JavadocTagsSubProcessorCore.getPreviousExceptionNames(exceptions, missingNode));
	 	} else {
	 		Assert.isTrue(false, "AddMissingJavadocTagProposal: unexpected node location"); //$NON-NLS-1$
	 		return;
	 	}

		TextElement textElement= ast.newTextElement();
		textElement.setText(""); //$NON-NLS-1$
		newTag.fragments().add(textElement);

		addLinkedPosition(rewrite.track(textElement), false, "comment_start"); //$NON-NLS-1$

		if (bodyDecl.getJavadoc() == null) {
			// otherwise the linked position spans over a line delimiter
			newTag.fragments().add(ast.newTextElement());
		}
	}
}