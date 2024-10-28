/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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
 *     Red Hat Inc. - body copied from SuppressWarningsSubProcessor
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.CorrectionEngine;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
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
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.IProposableFix;
import org.eclipse.jdt.internal.corext.fix.UnusedSuppressWarningsFixCore;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.fix.UnusedSuppressWarningsCleanUp;

public abstract class SuppressWarningsBaseSubProcessor<T> {

	static final String ADD_SUPPRESSWARNINGS_ID= "org.eclipse.jdt.ui.correction.addSuppressWarnings"; //$NON-NLS-1$

	public static final boolean hasSuppressWarningsProposal(IJavaProject javaProject, int problemId) {
		if (CorrectionEngine.getWarningToken(problemId) != null && JavaModelUtil.is50OrHigher(javaProject)) {
			String optionId= JavaCore.getOptionForConfigurableSeverity(problemId);
			if (optionId != null) {
				String optionValue= javaProject.getOption(optionId, true);
				return JavaCore.INFO.equals(optionValue)
						|| JavaCore.WARNING.equals(optionValue)
						|| (JavaCore.ERROR.equals(optionValue) && JavaCore.ENABLED.equals(javaProject.getOption(JavaCore.COMPILER_PB_SUPPRESS_OPTIONAL_ERRORS, true)));
			}
		}
		return false;
	}

	public void getSuppressWarningsProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
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
		for (T element : proposals) {
			if (element instanceof SuppressWarningsProposalCore && warningToken.equals(((SuppressWarningsProposalCore) element).getWarningToken())) {
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
		return;
	}

	private int addSuppressWarningsProposalIfPossible(ICompilationUnit cu, ASTNode node, String warningToken, int relevance, Collection<T> proposals) {

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
		T proposal = createSuppressWarningsProposal(warningToken, label, cu, node, property, relevance);

		proposals.add(proposal);
		return isLocalVariable ? relevance - 1 : 0;
	}

	private static String getFirstFragmentName(List<VariableDeclarationFragment> fragments) {
		if (fragments.size() > 0) {
			return fragments.get(0).getName().getIdentifier();
		}
		return ""; //$NON-NLS-1$
	}

	public void getUnknownSuppressWarningProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
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
				T proposal= createASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.FIX_SUPPRESS_TOKEN);
				proposals.add(proposal);
			}
		}

		getRemoveUnusedSuppressWarningProposals(context, problem, proposals);
	}


	public void getRemoveUnusedSuppressWarningProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		ASTNode coveringNode= problem.getCoveringNode(context.getASTRoot());
		if (!(coveringNode instanceof StringLiteral))
			return;

		StringLiteral literal= (StringLiteral) coveringNode;

		if (coveringNode.getParent() instanceof MemberValuePair) {
			coveringNode= coveringNode.getParent();
		}

		ASTNode parent= coveringNode.getParent();
		if (!(parent instanceof SingleMemberAnnotation) && !(parent instanceof NormalAnnotation) && !(parent instanceof ArrayInitializer)) {
			return;
		}
		IProposableFix fix= UnusedSuppressWarningsFixCore.createFix(context.getASTRoot(), problem);
		if (fix != null) {
			Map<String, String> options= new Hashtable<>();
			options.put(CleanUpConstants.REMOVE_UNNECESSARY_SUPPRESS_WARNINGS, CleanUpOptions.TRUE);
			UnusedSuppressWarningsCleanUp cleanUp= new UnusedSuppressWarningsCleanUp(options);
			cleanUp.setLiteral(literal);
			T proposal= createFixCorrectionProposal(fix, cleanUp, IProposalRelevance.REMOVE_ANNOTATION, context);
			proposals.add(proposal);
		}
		fix= UnusedSuppressWarningsFixCore.createAllFix(context.getASTRoot(), null);
		if (fix != null) {
			Map<String, String> options= new Hashtable<>();
			options.put(CleanUpConstants.REMOVE_UNNECESSARY_SUPPRESS_WARNINGS, CleanUpOptions.TRUE);
			UnusedSuppressWarningsCleanUp cleanUp= new UnusedSuppressWarningsCleanUp(options);
			T proposal= createFixCorrectionProposal(fix, cleanUp, IProposalRelevance.REMOVE_ANNOTATION, context);
			proposals.add(proposal);
		}
	}

	protected abstract T createSuppressWarningsProposal(String warningToken, String label, ICompilationUnit cu, ASTNode node, ChildListPropertyDescriptor property, int relevance);

	protected abstract T createASTRewriteCorrectionProposal(String name, ICompilationUnit cu, ASTRewrite rewrite, int relevance);

	protected abstract T createFixCorrectionProposal(IProposableFix fix, ICleanUp cleanUp, int relevance, IInvocationContext context);
}
