/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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
 *     Red Hat Inc - separate core logic from UI images
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.ModuleDeclaration;
import org.eclipse.jdt.core.dom.ModuleDirective;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ReplaceCorrectionProposal;

public class JavadocTagsSubProcessor extends JavadocTagsBaseSubProcessor<ICommandAccess>{

	public static void getMissingJavadocTagProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new JavadocTagsSubProcessor().addMissingJavadocTagProposals(context, problem, proposals);
	}

	public static void getUnusedAndUndocumentedParameterOrExceptionProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new JavadocTagsSubProcessor().addUnusedAndUndocumentedParameterOrExceptionProposals(context, problem, proposals);
	}

	public static void getMissingJavadocCommentProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
		new JavadocTagsSubProcessor().addMissingJavadocCommentProposals(context, problem, proposals);
	}

	public static Set<String> getPreviousTypeParamNames(List<TypeParameter> typeParams, ASTNode missingNode) {
		return JavadocTagsSubProcessorCore.getPreviousTypeParamNames(typeParams, missingNode);
	}

	public static Set<String> getPreviousProvidesNames(List<ModuleDirective> directives, ASTNode missingNode) {
		return JavadocTagsSubProcessorCore.getPreviousProvidesNames(directives, missingNode);
	}

	public static Set<String> getPreviousUsesNames(List<ModuleDirective> directives, ASTNode missingNode) {
		return JavadocTagsSubProcessorCore.getPreviousUsesNames(directives, missingNode);
	}

	static Set<String> getPreviousParamNames(List<SingleVariableDeclaration> params, ASTNode missingNode) {
		return JavadocTagsSubProcessorCore.getPreviousParamNames(params, missingNode);
	}

	static Set<String> getPreviousExceptionNames(List<Type> list, ASTNode missingNode) {
		return JavadocTagsSubProcessorCore.getPreviousExceptionNames(list, missingNode);
	}

	public static TagElement findTag(Javadoc javadoc, String name, String arg) {
		return JavadocTagsSubProcessorCore.findTag(javadoc, name, arg);
	}

	public static TagElement findParamTag(Javadoc javadoc, String arg) {
		return JavadocTagsSubProcessorCore.findParamTag(javadoc, arg);
	}


	public static TagElement findThrowsTag(Javadoc javadoc, String arg) {
		return JavadocTagsSubProcessorCore.findThrowsTag(javadoc, arg);
	}

	public static void insertTag(ListRewrite rewriter, TagElement newElement, Set<String> sameKindLeadingNames) {
		JavadocTagsSubProcessorCore.insertTag(rewriter, newElement, sameKindLeadingNames, null);
	}

	public static void getRemoveJavadocTagProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new JavadocTagsSubProcessor().addRemoveJavadocTagProposals(context, problem, proposals);
	}

	public static void getRemoveDuplicateModuleJavadocTagProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new JavadocTagsSubProcessor().addRemoveDuplicateModuleJavadocTagProposals(context, problem, proposals);
	}

	public static void getInvalidQualificationProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new JavadocTagsSubProcessor().addInvalidQualificationProposals(context, problem, proposals);
	}


	private static final class AddJavadocCommentProposal extends CUCorrectionProposal {
		private AddJavadocCommentProposal(String name, ICompilationUnit cu, int relevance, int insertPosition, String comment) {
			super(name, cu, relevance, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG), new AddJavadocCommentProposalCore(name, cu, relevance, insertPosition, comment));
		}
	}

	private static final class AddMissingModuleJavadocTagProposal extends CUCorrectionProposal {
		public AddMissingModuleJavadocTagProposal(String label, ICompilationUnit cu, ModuleDeclaration decl, ASTNode missingNode, int relevance) {
			super(label, cu, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG), new AddMissingModuleJavadocTagProposalCore(label, cu, decl, missingNode, relevance));
		}
	}

	private static final class AddAllMissingModuleJavadocTagsProposal extends CUCorrectionProposal {
		public AddAllMissingModuleJavadocTagsProposal(String label, ICompilationUnit cu, ModuleDeclaration decl, @SuppressWarnings("unused") ASTNode missingNode, int relevance) {
			super(label, cu, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG), new AddAllMissingModuleJavadocTagsProposalCore(label, cu, decl, relevance));
		}
	}

	private static final class AddMissingJavadocTagProposal extends LinkedCorrectionProposal {
		public AddMissingJavadocTagProposal(String label, ICompilationUnit cu, ASTNode decl, ASTNode missingNode, int relevance) {
			super(label, cu, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG), new AddMissingJavadocTagProposalCore(label, cu, decl, missingNode, relevance));
		}
	}

	private static final class AddAllMissingJavadocTagsProposal extends LinkedCorrectionProposal {
		public AddAllMissingJavadocTagsProposal(String label, ICompilationUnit cu, ASTNode decl, int relevance) {
			super(label, cu, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG), new AddAllMissingJavadocTagsProposalCore(label, cu, decl, relevance));
		}
	}

	private JavadocTagsSubProcessor() {
		super();
	}

	@Override
	protected ICommandAccess addAllMissingJavadocTagsProposal(String label2, ICompilationUnit compilationUnit, ASTNode parentDeclaration, int addAllMissingTags) {
		return new AddAllMissingJavadocTagsProposal(label2, compilationUnit, parentDeclaration, IProposalRelevance.ADD_ALL_MISSING_TAGS);
	}

	@Override
	protected ICommandAccess addMissingJavadocTagProposal(String label, ICompilationUnit compilationUnit, ASTNode parentDeclaration, ASTNode node, int addMissingTag) {
		return new AddMissingJavadocTagProposal(label, compilationUnit, parentDeclaration, node, IProposalRelevance.ADD_MISSING_TAG);
	}

	@Override
	protected ICommandAccess addAllMissingModuleJavadocTagsProposal(String label2, ICompilationUnit compilationUnit, ModuleDeclaration moduleDecl, ASTNode node, int addAllMissingTags) {
		return new AddAllMissingModuleJavadocTagsProposal(label2, compilationUnit, moduleDecl, node, IProposalRelevance.ADD_ALL_MISSING_TAGS);
	}

	@Override
	protected ICommandAccess addMissingModuleJavadocTagProposal(String label, ICompilationUnit compilationUnit, ModuleDeclaration moduleDecl, ASTNode node, int addMissingTag) {
		return new AddMissingModuleJavadocTagProposal(label, compilationUnit, moduleDecl, node, IProposalRelevance.ADD_MISSING_TAG);
	}

	@Override
	protected ICommandAccess addJavadocCommentProposal(String label, ICompilationUnit cu, int addJavadocModule, int startPosition, String comment) {
		return new AddJavadocCommentProposal(label, cu, IProposalRelevance.ADD_JAVADOC_MODULE, startPosition, comment);
	}

	@Override
	protected ICommandAccess createRemoveJavadocTagProposals(String label, ICompilationUnit compilationUnit, ASTRewrite rewrite, int removeTag) {
		Image image= PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
		return new ASTRewriteCorrectionProposal(label, compilationUnit, rewrite, IProposalRelevance.REMOVE_TAG, image);
	}

	@Override
	protected ICommandAccess createRemoveDuplicateModuleJavadocTagProposal(String label, ICompilationUnit compilationUnit, int start, int length, String string, int removeTag) {
		ReplaceCorrectionProposal proposal= new ReplaceCorrectionProposal(label, compilationUnit, start, length,
				"", IProposalRelevance.REMOVE_TAG); //$NON-NLS-1$
		proposal.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE));
		return proposal;
	}

	@Override
	protected ICommandAccess createInvalidQualificationProposal(String label, ICompilationUnit compilationUnit, ASTRewrite rewrite, int qualifyInnerTypeName) {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, compilationUnit, rewrite, IProposalRelevance.QUALIFY_INNER_TYPE_NAME, image);
		return proposal;
	}
}
