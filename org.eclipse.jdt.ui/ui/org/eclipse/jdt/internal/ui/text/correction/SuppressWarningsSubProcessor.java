/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
 *     Red Hat Inc - separate core logic from UI images
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.CorrectionEngine;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.PatternInstanceofExpression;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class SuppressWarningsSubProcessor extends SuppressWarningsSubProcessorCore {

	static final String ADD_SUPPRESSWARNINGS_ID= SuppressWarningsSubProcessorCore.ADD_SUPPRESSWARNINGS_ID;

	public static void addSuppressWarningsProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) {
		if (problem.isError() && !JavaCore.ENABLED.equals(context.getCompilationUnit().getJavaProject().getOption(JavaCore.COMPILER_PB_SUPPRESS_OPTIONAL_ERRORS, true))) {
			return;
		}
		if (JavaCore.DISABLED.equals(context.getCompilationUnit().getJavaProject().getOption(JavaCore.COMPILER_PB_SUPPRESS_WARNINGS, true))) {
			return;
		}

		String warningToken= CorrectionEngine.getWarningToken(problem.getProblemId());
		if (warningToken == null) {
			return;
		}
		for (ICommandAccess element : proposals) {
			if (element instanceof SuppressWarningsProposal && warningToken.equals(((SuppressWarningsProposal) element).getWarningToken())) {
				return; // only one at a time
			}
		}

		ASTNode node= problem.getCoveringNode(context.getASTRoot());
		if (node == null) {
			return;
		}

		ASTNode target= node;
		int relevance= IProposalRelevance.ADD_SUPPRESSWARNINGS;
		do {
			relevance= addSuppressWarningsProposalIfPossible(context.getCompilationUnit(), target, warningToken, relevance, proposals);
			if (relevance == 0)
				return;
			target= target.getParent();
		} while (target != null);

		ASTNode importStatement= ASTNodes.getParent(node, ImportDeclaration.class);
		if (importStatement != null && !context.getASTRoot().types().isEmpty()) {
			target= (ASTNode) context.getASTRoot().types().get(0);
			if (target != null) {
				addSuppressWarningsProposalIfPossible(context.getCompilationUnit(), target, warningToken, IProposalRelevance.ADD_SUPPRESSWARNINGS, proposals);
			}
		}
	}

	private static String getFirstFragmentName(List<VariableDeclarationFragment> fragments) {
		if (fragments.size() > 0) {
			return fragments.get(0).getName().getIdentifier();
		}
		return ""; //$NON-NLS-1$
	}


	private static class SuppressWarningsProposal extends ASTRewriteCorrectionProposal {
		public SuppressWarningsProposal(String warningToken, String label, ICompilationUnit cu, ASTNode node, ChildListPropertyDescriptor property, int relevance) {
			super(label, cu, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG), new SuppressWarningsProposalCore(warningToken, label, cu, node, property, relevance));
		}

		public String getWarningToken() {
			return ((SuppressWarningsProposalCore) getDelegate()).getWarningToken();
		}
	}

	/**
	 * Adds a SuppressWarnings proposal if possible and returns whether parent nodes should be
	 * processed or not (and with what relevance).
	 *
	 * @param cu the compilation unit
	 * @param node the node on which to add a SuppressWarning token
	 * @param warningToken the warning token to add
	 * @param relevance the proposal's relevance
	 * @param proposals collector to which the proposal should be added
	 * @return <code>0</code> if no further proposals should be added to parent nodes, or the
	 *         relevance of the next proposal
	 *
	 * @since 3.6
	 */
	private static int addSuppressWarningsProposalIfPossible(ICompilationUnit cu, ASTNode node, String warningToken, int relevance, Collection<ICommandAccess> proposals) {

		ChildListPropertyDescriptor property;
		String name;
		boolean isLocalVariable= false;
		switch (node.getNodeType()) {
			case ASTNode.SINGLE_VARIABLE_DECLARATION:
				if (node.getParent() instanceof PatternInstanceofExpression && warningToken.equals("preview")) //$NON-NLS-1$
					return relevance;
				property= SingleVariableDeclaration.MODIFIERS2_PROPERTY;
				name= ((SingleVariableDeclaration) node).getName().getIdentifier();
				isLocalVariable= true;
				break;
			case ASTNode.VARIABLE_DECLARATION_STATEMENT:
				property= VariableDeclarationStatement.MODIFIERS2_PROPERTY;
				name= getFirstFragmentName(((VariableDeclarationStatement) node).fragments());
				isLocalVariable= true;
				break;
			case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
				property= VariableDeclarationExpression.MODIFIERS2_PROPERTY;
				name= getFirstFragmentName(((VariableDeclarationExpression) node).fragments());
				isLocalVariable= true;
				break;
			case ASTNode.TYPE_DECLARATION:
				property= TypeDeclaration.MODIFIERS2_PROPERTY;
				name= ((TypeDeclaration) node).getName().getIdentifier();
				break;
			case ASTNode.RECORD_DECLARATION:
				property= RecordDeclaration.MODIFIERS2_PROPERTY;
				name= ((RecordDeclaration) node).getName().getIdentifier();
				break;
			case ASTNode.ANNOTATION_TYPE_DECLARATION:
				property= AnnotationTypeDeclaration.MODIFIERS2_PROPERTY;
				name= ((AnnotationTypeDeclaration) node).getName().getIdentifier();
				break;
			case ASTNode.ENUM_DECLARATION:
				property= EnumDeclaration.MODIFIERS2_PROPERTY;
				name= ((EnumDeclaration) node).getName().getIdentifier();
				break;
			case ASTNode.FIELD_DECLARATION:
				property= FieldDeclaration.MODIFIERS2_PROPERTY;
				name= getFirstFragmentName(((FieldDeclaration) node).fragments());
				break;
			// case ASTNode.INITIALIZER: not used, because Initializer cannot have annotations
			case ASTNode.METHOD_DECLARATION:
				property= MethodDeclaration.MODIFIERS2_PROPERTY;
				name= ((MethodDeclaration) node).getName().getIdentifier() + "()"; //$NON-NLS-1$
				break;
			case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION:
				property= AnnotationTypeMemberDeclaration.MODIFIERS2_PROPERTY;
				name= ((AnnotationTypeMemberDeclaration) node).getName().getIdentifier() + "()"; //$NON-NLS-1$
				break;
			case ASTNode.ENUM_CONSTANT_DECLARATION:
				property= EnumConstantDeclaration.MODIFIERS2_PROPERTY;
				name= ((EnumConstantDeclaration) node).getName().getIdentifier();
				break;
			default:
				return relevance;
		}

		String label= Messages.format(CorrectionMessages.SuppressWarningsSubProcessor_suppress_warnings_label, new String[] { warningToken, BasicElementLabels.getJavaElementName(name) });
		ASTRewriteCorrectionProposal proposal= new SuppressWarningsProposal(warningToken, label, cu, node, property, relevance);

		proposals.add(proposal);
		return isLocalVariable ? relevance - 1 : 0;
	}

	/**
	 * Adds a proposal to correct the name of the SuppressWarning annotation
	 *
	 * @param context the context
	 * @param problem the problem
	 * @param proposals the resulting proposals
	 */
	public static void addUnknownSuppressWarningProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) {

		ASTNode coveringNode= context.getCoveringNode();
		if (!(coveringNode instanceof StringLiteral))
			return;

		AST ast= coveringNode.getAST();
		StringLiteral literal= (StringLiteral) coveringNode;

		String literalValue= literal.getLiteralValue();
		for (String curr : CorrectionEngine.getAllWarningTokens()) {
			if (NameMatcher.isSimilarName(literalValue, curr)) {
				StringLiteral newLiteral= ast.newStringLiteral();
				newLiteral.setLiteralValue(curr);
				ASTRewrite rewrite= ASTRewrite.create(ast);
				rewrite.replace(literal, newLiteral, null);
				String label= Messages.format(CorrectionMessages.SuppressWarningsSubProcessor_fix_suppress_token_label, new String[] { curr });
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.FIX_SUPPRESS_TOKEN, image);
				proposals.add(proposal);
			}
		}
		addRemoveUnusedSuppressWarningProposals(context, problem, proposals);
	}


	public static void addRemoveUnusedSuppressWarningProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) {
		ASTNode coveringNode= problem.getCoveringNode(context.getASTRoot());
		if (!(coveringNode instanceof StringLiteral))
			return;

		StringLiteral literal= (StringLiteral) coveringNode;

		if (coveringNode.getParent() instanceof MemberValuePair) {
			coveringNode= coveringNode.getParent();
		}

		ASTNode parent= coveringNode.getParent();

		ASTRewrite rewrite= ASTRewrite.create(coveringNode.getAST());
		if (parent instanceof SingleMemberAnnotation) {
			rewrite.remove(parent, null);
		} else if (parent instanceof NormalAnnotation) {
			NormalAnnotation annot= (NormalAnnotation) parent;
			if (annot.values().size() == 1) {
				rewrite.remove(annot, null);
			} else {
				rewrite.remove(coveringNode, null);
			}
		} else if (parent instanceof ArrayInitializer) {
			rewrite.remove(coveringNode, null);
		} else {
			return;
		}
		String label= Messages.format(CorrectionMessages.SuppressWarningsSubProcessor_remove_annotation_label, literal.getLiteralValue());
		Image image= PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.REMOVE_ANNOTATION, image);
		proposals.add(proposal);
	}

	private SuppressWarningsSubProcessor() {
	}

}
