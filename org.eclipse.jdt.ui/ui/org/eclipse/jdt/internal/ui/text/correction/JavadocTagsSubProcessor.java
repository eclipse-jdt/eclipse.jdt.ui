/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     Red Hat Inc. - add module-info support
 *     Microsoft Corporation - read formatting options from the compilation unit
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IModuleBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ModuleDeclaration;
import org.eclipse.jdt.core.dom.ModuleDirective;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ProvidesDirective;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.UsesDirective;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.manipulation.CodeGeneration;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.util.Strings;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ReplaceCorrectionProposal;

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

		@Override
		protected void addEdits(IDocument document, TextEdit rootEdit) throws CoreException {
			try {
				String lineDelimiter= TextUtilities.getDefaultLineDelimiter(document);
				final ICompilationUnit unit= getCompilationUnit();
				IRegion region= document.getLineInformationOfOffset(fInsertPosition);

				String lineContent= document.get(region.getOffset(), region.getLength());
				String indentString= Strings.getIndentString(lineContent, unit);
				String str= Strings.changeIndent(fComment, 0, unit, indentString, lineDelimiter);
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

	private static final class AddMissingModuleJavadocTagProposal extends CUCorrectionProposal {

		private final ModuleDeclaration fDecl; // MethodDecl or TypeDecl or ModuleDecl
		private final ASTNode fMissingNode;

		public AddMissingModuleJavadocTagProposal(String label, ICompilationUnit cu, ModuleDeclaration decl, ASTNode missingNode, int relevance) {
			super(label, cu, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG));
			fDecl= decl;
			fMissingNode= missingNode;
		}

		@Override
		protected void addEdits(IDocument document, TextEdit rootEdit) throws CoreException {
			try {
				Javadoc javadoc= null;
				String lineDelimiter= TextUtilities.getDefaultLineDelimiter(document);
				final ICompilationUnit unit= getCompilationUnit();
				CompilationUnit cu= (CompilationUnit)fDecl.getParent();
				Name name= fDecl.getName();
				List<Comment> comments= cu.getCommentList();
				for (Comment comment : comments) {
					if (comment instanceof Javadoc
							&& comment.getStartPosition() + comment.getLength() < name.getStartPosition()) {
						javadoc= (Javadoc)comment;
					}
				}
				if (javadoc == null) {
					return;
				}
				StringBuilder comment= new StringBuilder();
				int insertPosition= findInsertPosition(javadoc, fMissingNode, document, lineDelimiter);

			 	if (fMissingNode instanceof UsesDirective) {
			 		UsesDirective directive= (UsesDirective)fMissingNode;
			 		comment.append(" * ").append(TagElement.TAG_USES).append(" ").append(directive.getName().getFullyQualifiedName().toString()).append(lineDelimiter); //$NON-NLS-1$ //$NON-NLS-2$
			 	} else if (fMissingNode instanceof ProvidesDirective) {
			 		ProvidesDirective directive= (ProvidesDirective)fMissingNode;
			 		comment.append(" * ").append(TagElement.TAG_PROVIDES).append(" ").append(directive.getName().getFullyQualifiedName().toString()).append(lineDelimiter); //$NON-NLS-1$ //$NON-NLS-2$
			 	}
				IRegion region= document.getLineInformationOfOffset(insertPosition);

				String lineContent= document.get(region.getOffset(), region.getLength());
				String indentString= Strings.getIndentString(lineContent, unit);
				String str= Strings.changeIndent(comment.toString(), 0, unit, indentString, lineDelimiter);
				InsertEdit edit= new InsertEdit(insertPosition, str);
				rootEdit.addChild(edit);
			} catch (BadLocationException e) {
				throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
			}
		}

		public static int findInsertPosition(Javadoc javadoc, ASTNode node, IDocument document, String lineDelimiter) throws BadLocationException {
			int position= -1;
			List<TagElement> tags= javadoc.tags();
			TagElement lastTag= null;
			// Put new tag after last @provides tag if found
			for (TagElement tag : tags) {
				String name= tag.getTagName();
				if (TagElement.TAG_PROVIDES.equals(name)) {
					lastTag= tag;
				}
			}
			if (lastTag == null) {
				// otherwise add before first @uses tag
				for (TagElement tag : tags) {
					String name= tag.getTagName();
					if (TagElement.TAG_USES.equals(name)) {
						IRegion region= document.getLineInformationOfOffset(tag.getStartPosition());
						return region.getOffset();
					}
				}
			}
			// otherwise put after last tag
			if (lastTag == null && !tags.isEmpty()) {
				lastTag= tags.get(tags.size() - 1);
			}
			if (lastTag != null) {
				IRegion region= document.getLineInformationOfOffset(lastTag.getStartPosition());
				position= region.getOffset() + region.getLength() + lineDelimiter.length();
			} else {
				// otherwise put after javadoc comment start
				IRegion region= document.getLineInformationOfOffset(javadoc.getStartPosition());
				position= region.getOffset() + region.getLength() + lineDelimiter.length();
			}
			return position;
		}

	}

	private static final class AddAllMissingModuleJavadocTagsProposal extends CUCorrectionProposal {

		private final ModuleDeclaration fDecl; // MethodDecl or TypeDecl or ModuleDecl
		private final ASTNode fMissingNode;

		public AddAllMissingModuleJavadocTagsProposal(String label, ICompilationUnit cu, ModuleDeclaration decl, ASTNode missingNode, int relevance) {
			super(label, cu, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG));
			fDecl= decl;
			fMissingNode= missingNode;
		}

		@Override
		protected void addEdits(IDocument document, TextEdit rootEdit) throws CoreException {
			try {
				Javadoc javadoc= null;
				int insertPosition;
				String lineDelimiter= TextUtilities.getDefaultLineDelimiter(document);
				final ICompilationUnit unit= getCompilationUnit();
				CompilationUnit cu= (CompilationUnit)fDecl.getParent();
				Name moduleName= fDecl.getName();
				List<Comment> comments= cu.getCommentList();
				for (Comment comment : comments) {
					if (comment instanceof Javadoc
							&& comment.getStartPosition() + comment.getLength() < moduleName.getStartPosition()) {
							javadoc= (Javadoc)comment;
					}
				}
				if (javadoc == null) {
					return;
				}
				StringBuilder comment= new StringBuilder();
				insertPosition= AddMissingModuleJavadocTagProposal.findInsertPosition(javadoc, fMissingNode, document, lineDelimiter);

			 	List<ModuleDirective> moduleStatements= fDecl.moduleStatements();
			 	for (int i= moduleStatements.size() - 1; i >= 0 ; i--) {
			 		ModuleDirective directive= moduleStatements.get(i);
			 		String name;
			 		if (directive instanceof ProvidesDirective) {
			 			name= ((ProvidesDirective)directive).getName().getFullyQualifiedName().toString();
			 			if (findTag(javadoc, TagElement.TAG_PROVIDES, name) == null) {
					 		comment.append(" * ").append(TagElement.TAG_PROVIDES).append(" ").append(name).append(lineDelimiter); //$NON-NLS-1$ //$NON-NLS-2$
			 			}
			 		}
			 	}

			 	for (int i= moduleStatements.size() - 1; i >= 0 ; i--) {
			 		ModuleDirective directive= moduleStatements.get(i);
			 		String name;
			 		if (directive instanceof UsesDirective) {
			 			name= ((UsesDirective)directive).getName().getFullyQualifiedName().toString();
			 			if (findTag(javadoc, TagElement.TAG_USES, name) == null) {
					 		comment.append(" * ").append(TagElement.TAG_USES).append(" ").append(name).append(lineDelimiter); //$NON-NLS-1$ //$NON-NLS-2$
			 			}
			 		}
			 	}

				IRegion region= document.getLineInformationOfOffset(insertPosition);

				String lineContent= document.get(region.getOffset(), region.getLength());
				String indentString= Strings.getIndentString(lineContent, unit);
				String str= Strings.changeIndent(comment.toString(), 0, unit, indentString, lineDelimiter);
				InsertEdit edit= new InsertEdit(insertPosition, str);
				rootEdit.addChild(edit);
			} catch (BadLocationException e) {
				throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
			}
		}
	}

	private static final class AddMissingJavadocTagProposal extends LinkedCorrectionProposal {

		private final ASTNode fDecl; // MethodDecl or TypeDecl
		private final ASTNode fMissingNode;

		public AddMissingJavadocTagProposal(String label, ICompilationUnit cu, ASTNode decl, ASTNode missingNode, int relevance) {
			super(label, cu, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG));
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

					Set<String> sameKindLeadingNames= getPreviousParamNames(params, decl);

					List<TypeParameter> typeParams= methodDeclaration.typeParameters();
					for (TypeParameter typeParam : typeParams) {
						String curr= '<' + typeParam.getName().getIdentifier() + '>';
						sameKindLeadingNames.add(curr);
					}
					insertTag(tagsRewriter, newTag, sameKindLeadingNames);
				} else if (bodyDecl instanceof RecordDeclaration) {
					RecordDeclaration recordDeclaration= (RecordDeclaration) bodyDecl;
					List<SingleVariableDeclaration> params= recordDeclaration.recordComponents();

					Set<String> sameKindLeadingNames= getPreviousParamNames(params, decl);

					List<TypeParameter> typeParams= recordDeclaration.typeParameters();
					for (TypeParameter typeParam : typeParams) {
						String curr= '<' + typeParam.getName().getIdentifier() + '>';
						sameKindLeadingNames.add(curr);
					}
					insertTag(tagsRewriter, newTag, sameKindLeadingNames);
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
				insertTag(tagsRewriter, newTag, getPreviousTypeParamNames(params, typeParam));
		 	} else if (location == MethodDeclaration.RETURN_TYPE2_PROPERTY) {
				newTag= ast.newTagElement();
				newTag.setTagName(TagElement.TAG_RETURN);
				insertTag(tagsRewriter, newTag, null);
			} else if (location == MethodDeclaration.THROWN_EXCEPTION_TYPES_PROPERTY) {
				newTag= ast.newTagElement();
				newTag.setTagName(TagElement.TAG_THROWS);
				TextElement excNode= ast.newTextElement();
				excNode.setText(ASTNodes.getQualifiedTypeName((Type) missingNode));
				newTag.fragments().add(excNode);
				List<Type> exceptions= ((MethodDeclaration) bodyDecl).thrownExceptionTypes();
				insertTag(tagsRewriter, newTag, getPreviousExceptionNames(exceptions, missingNode));
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

	private static final class AddAllMissingJavadocTagsProposal extends LinkedCorrectionProposal {

		private final ASTNode fDecl;

		public AddAllMissingJavadocTagsProposal(String label, ICompilationUnit cu, ASTNode decl, int relevance) {
			super(label, cu, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG));
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
		 		if (findTag(javadoc, TagElement.TAG_PARAM, name) == null) {
		 			TagElement newTag= ast.newTagElement();
		 			newTag.setTagName(TagElement.TAG_PARAM);
		 			TextElement text= ast.newTextElement();
		 			text.setText(name);
		 			newTag.fragments().add(text);
					insertTabStop(rewriter, newTag.fragments(), "typeParam" + i); //$NON-NLS-1$
		 			insertTag(tagsRewriter, newTag, getPreviousTypeParamNames(typeParams, decl));
		 		}
				typeParamNames.add(name);
		 	}
		 	List<SingleVariableDeclaration> params= methodDecl.parameters();
		 	for (int i= params.size() - 1; i >= 0 ; i--) {
		 		SingleVariableDeclaration decl= params.get(i);
		 		String name= decl.getName().getIdentifier();
		 		if (findTag(javadoc, TagElement.TAG_PARAM, name) == null) {
		 			TagElement newTag= ast.newTagElement();
		 			newTag.setTagName(TagElement.TAG_PARAM);
		 			newTag.fragments().add(ast.newSimpleName(name));
					insertTabStop(rewriter, newTag.fragments(), "methParam" + i); //$NON-NLS-1$
		 			Set<String> sameKindLeadingNames= getPreviousParamNames(params, decl);
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
			List<Type> thrownExceptions= methodDecl.thrownExceptionTypes();
		 	for (int i= thrownExceptions.size() - 1; i >= 0 ; i--) {
				Type exception= thrownExceptions.get(i);
				ITypeBinding binding= exception.resolveBinding();
		 		if (binding != null) {
		 			String name= binding.getName();
		 			if (findThrowsTag(javadoc, name) == null) {
		 				TagElement newTag= ast.newTagElement();
		 				newTag.setTagName(TagElement.TAG_THROWS);
						TextElement excNode= ast.newTextElement();
						excNode.setText(ASTNodes.getQualifiedTypeName(exception));
		 				newTag.fragments().add(excNode);
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

			List<TypeParameter> typeParams= typeDecl.typeParameters();
			for (int i= typeParams.size() - 1; i >= 0; i--) {
				TypeParameter decl= typeParams.get(i);
				String name= '<' + decl.getName().getIdentifier() + '>';
				if (findTag(javadoc, TagElement.TAG_PARAM, name) == null) {
					TagElement newTag= ast.newTagElement();
					newTag.setTagName(TagElement.TAG_PARAM);
					TextElement text= ast.newTextElement();
					text.setText(name);
					newTag.fragments().add(text);
					insertTabStop(rewriter, newTag.fragments(), "typeParam" + i); //$NON-NLS-1$
					insertTag(tagsRewriter, newTag, getPreviousTypeParamNames(typeParams, decl));
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
				if (findTag(javadoc, TagElement.TAG_PARAM, name) == null) {
					TagElement newTag= ast.newTagElement();
					newTag.setTagName(TagElement.TAG_PARAM);
					TextElement text= ast.newTextElement();
					text.setText(name);
					newTag.fragments().add(text);
					insertTabStop(rewriter, newTag.fragments(), "recComps" + i); //$NON-NLS-1$
					insertTag(tagsRewriter, newTag, getPreviousParamNames(recComps, decl));
				}
			}

			List<TypeParameter> typeParams= recDecl.typeParameters();
			for (int i= typeParams.size() - 1; i >= 0; i--) {
				TypeParameter decl= typeParams.get(i);
				String name= '<' + decl.getName().getIdentifier() + '>';
				if (findTag(javadoc, TagElement.TAG_PARAM, name) == null) {
					TagElement newTag= ast.newTagElement();
					newTag.setTagName(TagElement.TAG_PARAM);
					TextElement text= ast.newTextElement();
					text.setText(name);
					newTag.fragments().add(text);
					insertTabStop(rewriter, newTag.fragments(), "typeParam" + i); //$NON-NLS-1$
					insertTag(tagsRewriter, newTag, getPreviousTypeParamNames(typeParams, decl));
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

	public static void getMissingJavadocTagProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
	 	ASTNode node= problem.getCoveringNode(context.getASTRoot());
	 	ASTNode parentDeclaration= null;
	 	if (node == null) {
	 		return;
	 	}
	 	node= ASTNodes.getNormalizedNode(node);
	 	String label;

	 	StructuralPropertyDescriptor location= node.getLocationInParent();
	 	if (location == ModuleDeclaration.MODULE_DIRECTIVES_PROPERTY) {
	 		if (node instanceof UsesDirective) {
	 			label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_usestag_description;
	 		} else if (node instanceof ProvidesDirective) {
	 			label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_providestag_description;
	 		} else {
	 			return;
	 		}
	 		ModuleDeclaration moduleDecl= (ModuleDeclaration)node.getParent();
	 		CUCorrectionProposal proposal= new AddMissingModuleJavadocTagProposal(label, context.getCompilationUnit(), moduleDecl, node, IProposalRelevance.ADD_MISSING_TAG);
	 		proposals.add(proposal);

	 		String label2= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_allmissing_description;
	 		CUCorrectionProposal addAllMissing= new AddAllMissingModuleJavadocTagsProposal(label2, context.getCompilationUnit(), moduleDecl, node, IProposalRelevance.ADD_ALL_MISSING_TAGS);
	 		proposals.add(addAllMissing);
	 	} else {
	 		parentDeclaration= ASTResolving.findParentBodyDeclaration(node);
	 		if (parentDeclaration == null) {
	 			return;
	 		}
	 		Javadoc javadoc= ((BodyDeclaration)parentDeclaration).getJavadoc();
	 		if (javadoc == null) {
	 			return;
	 		}

	 		if (location == SingleVariableDeclaration.NAME_PROPERTY) {
	 			label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_paramtag_description;
	 			StructuralPropertyDescriptor propDesc= node.getParent().getLocationInParent();
	 			if (propDesc != MethodDeclaration.PARAMETERS_PROPERTY
	 					&& propDesc != RecordDeclaration.RECORD_COMPONENTS_PROPERTY) {
	 				return; // paranoia checks
	 			}
	 		} else if (location == TypeParameter.NAME_PROPERTY) {
	 			label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_paramtag_description;
	 			StructuralPropertyDescriptor parentLocation= node.getParent().getLocationInParent();
	 			if (parentLocation != MethodDeclaration.TYPE_PARAMETERS_PROPERTY
	 					&& parentLocation != TypeDeclaration.TYPE_PARAMETERS_PROPERTY
	 					&& parentLocation != RecordDeclaration.TYPE_PARAMETERS_PROPERTY) {
	 				return; // paranoia checks
	 			}
	 		} else if (location == MethodDeclaration.RETURN_TYPE2_PROPERTY) {
	 			label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_returntag_description;
	 		} else if (location == MethodDeclaration.THROWN_EXCEPTION_TYPES_PROPERTY) {
	 			label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_throwstag_description;
	 		} else {
	 			return;
	 		}
	 		ASTRewriteCorrectionProposal proposal= new AddMissingJavadocTagProposal(label, context.getCompilationUnit(), parentDeclaration, node, IProposalRelevance.ADD_MISSING_TAG);
	 		proposals.add(proposal);

	 		String label2= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_allmissing_description;
	 		ASTRewriteCorrectionProposal addAllMissing= new AddAllMissingJavadocTagsProposal(label2, context.getCompilationUnit(), parentDeclaration, IProposalRelevance.ADD_ALL_MISSING_TAGS);
	 		proposals.add(addAllMissing);
	 	}
	}

	public static void getUnusedAndUndocumentedParameterOrExceptionProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		ICompilationUnit cu= context.getCompilationUnit();
		IJavaProject project= cu.getJavaProject();

		if (!JavaCore.ENABLED.equals(project.getOption(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, true))) {
			return;
		}

		int problemId= problem.getProblemId();
		boolean isUnusedTypeParam= problemId == IProblem.UnusedTypeParameter;
		boolean isUnusedParam= problemId == IProblem.ArgumentIsNeverUsed || isUnusedTypeParam;
		String key= isUnusedParam ? JavaCore.COMPILER_PB_UNUSED_PARAMETER_INCLUDE_DOC_COMMENT_REFERENCE : JavaCore.COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_INCLUDE_DOC_COMMENT_REFERENCE;

		if (!JavaCore.ENABLED.equals(project.getOption(key, true))) {
			return;
		}

	 	ASTNode node= problem.getCoveringNode(context.getASTRoot());
	 	if (node == null) {
	 		return;
	 	}

		BodyDeclaration bodyDecl= ASTResolving.findParentBodyDeclaration(node);
		if (bodyDecl == null || ASTResolving.getParentMethodOrTypeBinding(bodyDecl) == null) {
			return;
		}

		String label;
		if (isUnusedTypeParam) {
			label= CorrectionMessages.JavadocTagsSubProcessor_document_type_parameter_description;
		} else if (isUnusedParam) {
			label= CorrectionMessages.JavadocTagsSubProcessor_document_parameter_description;
		} else {
			node= ASTNodes.getNormalizedNode(node);
			label= CorrectionMessages.JavadocTagsSubProcessor_document_exception_description;
		}
		ASTRewriteCorrectionProposal proposal= new AddMissingJavadocTagProposal(label, context.getCompilationUnit(), bodyDecl, node, IProposalRelevance.DOCUMENT_UNUSED_ITEM);
		proposals.add(proposal);
	}

	public static void getMissingJavadocCommentProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
		ASTNode node= problem.getCoveringNode(context.getASTRoot());
		if (node == null) {
			return;
		}
		ICompilationUnit cu= context.getCompilationUnit();
	 	if (node instanceof ModuleDeclaration) {
	 		ModuleDeclaration declaration= (ModuleDeclaration)node;
	 		IModuleBinding binding= declaration.resolveBinding();
	 		if (binding == null) {
	 			return;
	 		}
	 		List<String> usesNames= new ArrayList<>();
	 		for (ITypeBinding use : binding.getUses()) {
	 			usesNames.add(use.getName());
	 		}
	 		List<String> providesNames= new ArrayList<>();
	 		for (ITypeBinding provide : binding.getServices()) {
	 			providesNames.add(provide.getName());
	 		}
	 		String comment= CodeGeneration.getModuleComment(cu, declaration.getName().getFullyQualifiedName(), providesNames.toArray(new String[0]), usesNames.toArray(new String[0]), String.valueOf('\n'));
	 		if (comment != null) {
	 			String label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_method_description;
	 			proposals.add(new AddJavadocCommentProposal(label, cu, IProposalRelevance.ADD_JAVADOC_MODULE, declaration.getStartPosition(), comment));
	 		}
	 	} else {
	 		BodyDeclaration declaration= ASTResolving.findParentBodyDeclaration(node);
	 		if (declaration == null) {
	 			return;
	 		}
	 		ITypeBinding binding= Bindings.getBindingOfParentType(declaration);
	 		if (binding == null) {
	 			return;
	 		}

	 		if (declaration instanceof MethodDeclaration) {
	 			MethodDeclaration methodDecl= (MethodDeclaration) declaration;
	 			IMethodBinding methodBinding= methodDecl.resolveBinding();
	 			IMethodBinding overridden= null;
	 			if (methodBinding != null) {
	 				overridden= Bindings.findOverriddenMethod(methodBinding, true);
	 			}

	 			String string= CodeGeneration.getMethodComment(cu, binding.getName(), methodDecl, overridden, String.valueOf('\n'));
	 			if (string != null) {
	 				String label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_method_description;
	 				proposals.add(new AddJavadocCommentProposal(label, cu, IProposalRelevance.ADD_JAVADOC_METHOD, declaration.getStartPosition(), string));
	 			}
	 		} else if (declaration instanceof AbstractTypeDeclaration) {
	 			String typeQualifiedName= Bindings.getTypeQualifiedName(binding);
	 			String[] typeParamNames, params;
	 			if (declaration instanceof TypeDeclaration) {
	 				List<TypeParameter> typeParams= ((TypeDeclaration) declaration).typeParameters();
	 				typeParamNames= new String[typeParams.size()];
	 				for (int i= 0; i < typeParamNames.length; i++) {
	 					typeParamNames[i]= (typeParams.get(i)).getName().getIdentifier();
	 				}
	 				params= new String[0];
	 			} else if (declaration instanceof RecordDeclaration) {
	 				List<SingleVariableDeclaration> recComps= ((RecordDeclaration)declaration).recordComponents();
	 				params= new String[recComps.size()];
	 				for (int i= 0; i < params.length; i++) {
	 					params[i]= (recComps.get(i)).getName().getIdentifier();
	 				}
	 				List<TypeParameter> typeParams= ((RecordDeclaration) declaration).typeParameters();
	 				typeParamNames= new String[typeParams.size()];
	 				for (int i= 0; i < typeParamNames.length; i++) {
	 					typeParamNames[i]= (typeParams.get(i)).getName().getIdentifier();
	 				}
	 			} else {
	 				typeParamNames= new String[0];
	 				params= new String[0];
	 			}
	 			String string= CodeGeneration.getTypeComment(cu, typeQualifiedName, typeParamNames, params, String.valueOf('\n'));
	 			if (string != null) {
	 				String label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_type_description;
	 				proposals.add(new AddJavadocCommentProposal(label, cu, IProposalRelevance.ADD_JAVADOC_TYPE, declaration.getStartPosition(), string));
	 			}
	 		} else if (declaration instanceof FieldDeclaration) {
	 			String comment= "/**\n *\n */\n"; //$NON-NLS-1$
	 			List<VariableDeclarationFragment> fragments= ((FieldDeclaration)declaration).fragments();
	 			if (fragments != null && fragments.size() > 0) {
	 				VariableDeclaration decl= fragments.get(0);
	 				String fieldName= decl.getName().getIdentifier();
	 				String typeName= binding.getName();
	 				comment= CodeGeneration.getFieldComment(cu, typeName, fieldName, String.valueOf('\n'));
	 			}
	 			if (comment != null) {
	 				String label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_field_description;
	 				proposals.add(new AddJavadocCommentProposal(label, cu, IProposalRelevance.ADD_JAVADOC_FIELD, declaration.getStartPosition(), comment));
	 			}
	 		} else if (declaration instanceof EnumConstantDeclaration) {
	 			EnumConstantDeclaration enumDecl= (EnumConstantDeclaration) declaration;
	 			String id= enumDecl.getName().getIdentifier();
	 			String comment= CodeGeneration.getFieldComment(cu, binding.getName(), id, String.valueOf('\n'));
	 			String label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_enumconst_description;
	 			proposals.add(new AddJavadocCommentProposal(label, cu, IProposalRelevance.ADD_JAVADOC_ENUM, declaration.getStartPosition(), comment));
	 		}
	 	}
	}

	public static Set<String> getPreviousTypeParamNames(List<TypeParameter> typeParams, ASTNode missingNode) {
		Set<String> previousNames=  new HashSet<>();
		for (TypeParameter curr : typeParams) {
			if (curr == missingNode) {
				return previousNames;
			}
			previousNames.add('<' + curr.getName().getIdentifier() + '>');
		}
		return previousNames;
	}

	public static Set<String> getPreviousProvidesNames(List<ModuleDirective> directives, ASTNode missingNode) {
		Set<String> previousNames=  new HashSet<>();
		for (int i = 0; i < directives.size() && missingNode != directives.get(i); i++) {
			ModuleDirective directive= directives.get(i);
			if (directive instanceof ProvidesDirective) {
				ProvidesDirective providesDirective= (ProvidesDirective)directive;
				previousNames.add(providesDirective.getName().getFullyQualifiedName().toString());
			}
		}
		return previousNames;
	}

	public static Set<String> getPreviousUsesNames(List<ModuleDirective> directives, ASTNode missingNode) {
		Set<String> previousNames=  new HashSet<>();
		for (int i = 0; i < directives.size() && missingNode != directives.get(i); i++) {
			ModuleDirective directive= directives.get(i);
			if (directive instanceof UsesDirective) {
				UsesDirective usesDirective= (UsesDirective)directive;
				previousNames.add(usesDirective.getName().getFullyQualifiedName().toString());
			}
		}
		return previousNames;
	}

	private static Set<String> getPreviousParamNames(List<SingleVariableDeclaration> params, ASTNode missingNode) {
		Set<String> previousNames=  new HashSet<>();
		for (SingleVariableDeclaration curr : params) {
			if (curr == missingNode) {
				return previousNames;
			}
			previousNames.add(curr.getName().getIdentifier());
		}
		return previousNames;
	}

	private static Set<String> getPreviousExceptionNames(List<Type> list, ASTNode missingNode) {
		Set<String> previousNames=  new HashSet<>();
		for (int i= 0; i < list.size() && missingNode != list.get(i); i++) {
			Type curr= list.get(i);
			previousNames.add(ASTNodes.getTypeName(curr));
		}
		return previousNames;
	}

	public static TagElement findTag(Javadoc javadoc, String name, String arg) {
		List<TagElement> tags= javadoc.tags();
		int nTags= tags.size();
		for (int i= 0; i < nTags; i++) {
			TagElement curr= tags.get(i);
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
		List<TagElement> tags= javadoc.tags();
		int nTags= tags.size();
		for (int i= 0; i < nTags; i++) {
			TagElement curr= tags.get(i);
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
		List<TagElement> tags= javadoc.tags();
		int nTags= tags.size();
		for (int i= 0; i < nTags; i++) {
			TagElement curr= tags.get(i);
			String currName= curr.getTagName();
			if (TagElement.TAG_THROWS.equals(currName) || TagElement.TAG_EXCEPTION.equals(currName)) {
				String argument= getArgument(curr);
				if (arg.equals(argument)) {
					return curr;
				}
			}
		}
		return null;
	}

	public static void insertTag(ListRewrite rewriter, TagElement newElement, Set<String> sameKindLeadingNames) {
		insertTag(rewriter, newElement, sameKindLeadingNames, null);
	}

	public static void insertTag(ListRewrite rewriter, TagElement newElement, Set<String> sameKindLeadingNames, TextEditGroup groupDescription) {
		List<? extends ASTNode> tags= rewriter.getRewrittenList();

		String insertedTagName= newElement.getTagName();

		ASTNode after= null;
		int tagRanking= getTagRanking(insertedTagName);
		for (int i= tags.size() - 1; i >= 0; i--) {
			TagElement curr= (TagElement) tags.get(i);
			String tagName= curr.getTagName();
			if (tagName == null || tagRanking > getTagRanking(tagName)) {
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

	private static String[] TAG_ORDER= { // see http://www.oracle.com/technetwork/java/javase/documentation/index-137868.html#orderoftags
		TagElement.TAG_AUTHOR,
		TagElement.TAG_VERSION,
		TagElement.TAG_PARAM,
		TagElement.TAG_RETURN,
		TagElement.TAG_THROWS, // synonym to TAG_EXCEPTION
		TagElement.TAG_SEE,
		TagElement.TAG_SINCE,
		TagElement.TAG_SERIAL,
		TagElement.TAG_DEPRECATED
	};

	private static int getTagRanking(String tagName) {
		if (TagElement.TAG_EXCEPTION.equals(tagName)) {
			tagName= TagElement.TAG_THROWS;
		}
		for (int i= 0; i < TAG_ORDER.length; i++) {
			if (tagName.equals(TAG_ORDER[i])) {
				return i;
			}
		}
		return TAG_ORDER.length;
	}

	private static String getArgument(TagElement curr) {
		List<? extends ASTNode> fragments= curr.fragments();
		if (!fragments.isEmpty()) {
			Object first= fragments.get(0);
			if (first instanceof Name) {
				return ASTNodes.getSimpleNameIdentifier((Name) first);
			} else if (first instanceof TextElement && TagElement.TAG_PARAM.equals(curr.getTagName())) {
				String text= ((TextElement) first).getText();
				if ("<".equals(text) && fragments.size() >= 3) { //$NON-NLS-1$
					Object second= fragments.get(1);
					Object third= fragments.get(2);
					if (second instanceof Name && third instanceof TextElement && ">".equals(((TextElement) third).getText())) { //$NON-NLS-1$
						return '<' + ASTNodes.getSimpleNameIdentifier((Name) second) + '>';
					}
				} else if (text.startsWith(String.valueOf('<')) && text.endsWith(String.valueOf('>')) && text.length() > 2) {
					return text.substring(1, text.length() - 1);
				}
			} else if (first instanceof TextElement && (TagElement.TAG_USES.equals(curr.getTagName())
					|| TagElement.TAG_PROVIDES.equals(curr.getTagName()))) {
				String text= ((TextElement) first).getText();
				return text.trim();
			}
		}
		return null;
	}

	public static void getRemoveJavadocTagProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		ASTNode node= problem.getCoveringNode(context.getASTRoot());
		while (node != null && !(node instanceof TagElement)) {
			node= node.getParent();
		}
		if (node == null) {
			return;
		}
		ASTRewrite rewrite= ASTRewrite.create(node.getAST());
		rewrite.remove(node, null);

		String label= CorrectionMessages.JavadocTagsSubProcessor_removetag_description;
		Image image= PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
		proposals.add(new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.REMOVE_TAG, image));
	}

	public static void getRemoveDuplicateModuleJavadocTagProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		ASTNode node= problem.getCoveringNode(context.getASTRoot());
		if (node instanceof ModuleDeclaration) {
			node= findModuleJavadocTag((ModuleDeclaration)node, problem);
			if (node == null) {
				return;
			}
			CompilationUnit cu= (CompilationUnit)((Javadoc)node.getParent()).getAlternateRoot();
			if (cu == null) {
				return;
			}
			int line= cu.getLineNumber(problem.getOffset());
			IJavaElement javaElement= cu.getJavaElement();
			if (javaElement == null) {
				return;
			}
			String lineDelimiter= StubUtility.getLineDelimiterUsed(javaElement);
			int start= cu.getPosition(line, 0) - lineDelimiter.length();
			int column= cu.getColumnNumber(node.getStartPosition());
			int length= node.getLength() + column + lineDelimiter.length();
			String label= Messages.format(CorrectionMessages.JavadocTagsSubProcessor_removeduplicatetag_description, ((TextElement)((TagElement)node).fragments().get(0)).getText().trim());
			CUCorrectionProposal proposal= new ReplaceCorrectionProposal(label, context.getCompilationUnit(), start, length,
					"", IProposalRelevance.REMOVE_TAG); //$NON-NLS-1$
			proposal.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE));
			proposals.add(proposal);
		}
	}

	private static ASTNode findModuleJavadocTag(ModuleDeclaration decl, IProblemLocation problem) {
		ASTNode result= null;
		CompilationUnit cu= (CompilationUnit)decl.getParent();
		int problemLocationStart= problem.getOffset();
		Name moduleName= decl.getName();
		List<Comment> comments= cu.getCommentList();

		for (Comment comment : comments) {
			if (comment instanceof Javadoc
					&& comment.getStartPosition() + comment.getLength() < moduleName.getStartPosition()) {
				Javadoc javadoc= (Javadoc)comment;
				List<TagElement> tags= javadoc.tags();

				for (TagElement tag : tags) {
					if (problemLocationStart  > tag.getStartPosition()
							&& problemLocationStart < tag.getStartPosition() + tag.getLength()) {
						result= tag;
						break;
					}
				}
			}
		}
		return result;
	}

	public static void getInvalidQualificationProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		ASTNode node= problem.getCoveringNode(context.getASTRoot());
		if (!(node instanceof Name)) {
			return;
		}
		Name name= (Name) node;
		IBinding binding= name.resolveBinding();
		if (!(binding instanceof ITypeBinding)) {
			return;
		}
		ITypeBinding typeBinding= (ITypeBinding)binding;

		AST ast= node.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		rewrite.replace(name, ast.newName(typeBinding.getQualifiedName()), null);

		String label= CorrectionMessages.JavadocTagsSubProcessor_qualifylinktoinner_description;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.QUALIFY_INNER_TYPE_NAME, image);

		proposals.add(proposal);
	}

	private JavadocTagsSubProcessor() {
	}
}
