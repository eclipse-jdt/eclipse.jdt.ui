/*******************************************************************************
 * Copyright (c) 2025 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.CorrectionEngine;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
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
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

/**
 * Remove unneeded SuppressWarnings annotations.
 */
public class SuppressWarningsFixCore extends CompilationUnitRewriteOperationsFixCore {

	public final String fWarningToken;

	public SuppressWarningsFixCore(String name, CompilationUnit compilationUnit,
			CompilationUnitRewriteOperation operation, String warningToken) {
		super(name, compilationUnit, operation);
		this.fWarningToken= warningToken;
	}

	public static IProposableFix createFix(CompilationUnit compilationUnit, ASTNode node, ChildListPropertyDescriptor property,
			String warningToken, String name) {
		String label= Messages.format(CorrectionMessages.SuppressWarningsSubProcessor_suppress_warnings_label, new String[] { warningToken, name });
		Map<ASTNode, ChildListPropertyDescriptor> nodeMap= new HashMap<>();
		nodeMap.put(node, property);
		return new SuppressWarningsFixCore(label, compilationUnit, new AddNeededSuppressWarningsOperation(nodeMap, warningToken), warningToken);
	}

	public static IProposableFix createAllFix(CompilationUnit compilationUnit, Map<ASTNode, ChildListPropertyDescriptor> nodeMap, String warningToken) {
		String label= Messages.format(CorrectionMessages.SuppressWarningsSubProcessor_suppress_all_warnings_label, warningToken);
		return new SuppressWarningsFixCore(label, compilationUnit, new AddNeededSuppressWarningsOperation(nodeMap, warningToken), null);
	}

	public static IProposableFix createAllFix(CompilationUnit compilationUnit, String warningToken) {
		IProblem[] problems= compilationUnit.getProblems();
		IProblemLocation[] problemLocations= new IProblemLocation[problems.length];
		for (int i= 0; i < problems.length; ++i) {
			IProblemLocation location= new ProblemLocation(problems[i]);
			problemLocations[i]= location;
		}
		return createAllFix(compilationUnit, problemLocations, warningToken);
	}

	public static IProposableFix createAllFix(CompilationUnit compilationUnit, IProblemLocation[] problems, String warningToken) {
		Map<ASTNode, ChildListPropertyDescriptor> nodeMap= new HashMap<>();
		for (int i= 0; i < problems.length; i++) {
			IProblemLocation location= problems[i];
			if (CorrectionEngine.getWarningToken(location.getProblemId()).equals(warningToken)) {
				ASTNode node= location.getCoveringNode(compilationUnit);
				ChildListPropertyDescriptor property= getChildListPropertyDescriptor(node, warningToken);
				nodeMap.put(node, property);
			}
		}
		String label= Messages.format(CorrectionMessages.SuppressWarningsSubProcessor_suppress_all_warnings_label, warningToken);
		return new SuppressWarningsFixCore(label, compilationUnit, new AddNeededSuppressWarningsOperation(nodeMap, warningToken), null);
	}

	public static ChildListPropertyDescriptor getChildListPropertyDescriptor(ASTNode node, String warningToken) {
		switch (node.getNodeType()) {
			case ASTNode.SINGLE_VARIABLE_DECLARATION:
				if (node.getParent() instanceof PatternInstanceofExpression && warningToken.equals("preview")) //$NON-NLS-1$
					return null;
				return SingleVariableDeclaration.MODIFIERS2_PROPERTY;
			case ASTNode.VARIABLE_DECLARATION_STATEMENT:
				return VariableDeclarationStatement.MODIFIERS2_PROPERTY;
			case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
				return VariableDeclarationExpression.MODIFIERS2_PROPERTY;
			case ASTNode.TYPE_DECLARATION:
				return TypeDeclaration.MODIFIERS2_PROPERTY;
			case ASTNode.RECORD_DECLARATION:
				return RecordDeclaration.MODIFIERS2_PROPERTY;
			case ASTNode.ANNOTATION_TYPE_DECLARATION:
				return AnnotationTypeDeclaration.MODIFIERS2_PROPERTY;
			case ASTNode.ENUM_DECLARATION:
				return EnumDeclaration.MODIFIERS2_PROPERTY;
			case ASTNode.FIELD_DECLARATION:
				return FieldDeclaration.MODIFIERS2_PROPERTY;
			// case ASTNode.INITIALIZER: not used, because Initializer cannot have annotations
			case ASTNode.METHOD_DECLARATION:
				return MethodDeclaration.MODIFIERS2_PROPERTY;
			case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION:
				return AnnotationTypeMemberDeclaration.MODIFIERS2_PROPERTY;
			case ASTNode.ENUM_CONSTANT_DECLARATION:
				return EnumConstantDeclaration.MODIFIERS2_PROPERTY;
			default:
				return null;
		}
	}

	public String getWarningToken() {
		return fWarningToken;
	}

	private static class AddNeededSuppressWarningsOperation extends CompilationUnitRewriteOperation {

		private final Map<ASTNode, ChildListPropertyDescriptor> fNodeMap;
		private final String fWarningToken;
		public AddNeededSuppressWarningsOperation(Map<ASTNode, ChildListPropertyDescriptor> nodeMap, String warningToken) {
			this.fNodeMap= nodeMap;
			this.fWarningToken= warningToken;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {

			for (Entry<ASTNode, ChildListPropertyDescriptor> location : fNodeMap.entrySet()) {
				ASTNode coveringNode= location.getKey();
				ChildListPropertyDescriptor property= location.getValue();

				ASTRewrite rewrite= cuRewrite.getASTRewrite();
				AST ast= rewrite.getAST();

				StringLiteral newStringLiteral= ast.newStringLiteral();
				newStringLiteral.setLiteralValue(fWarningToken);

				Annotation existing= findExistingAnnotation(ASTNodes.getChildListProperty(coveringNode, property));
				if (existing == null) {
					ListRewrite listRewrite= rewrite.getListRewrite(coveringNode, property);

					SingleMemberAnnotation newAnnot= ast.newSingleMemberAnnotation();
					ImportRewrite importRewrite= StubUtility.createImportRewrite((CompilationUnit) coveringNode.getRoot(), true);

					String importString= importRewrite.addImport("java.lang.SuppressWarnings"); //$NON-NLS-1$
					newAnnot.setTypeName(ast.newName(importString));

					newAnnot.setValue(newStringLiteral);

					listRewrite.insertFirst(newAnnot, null);
				} else if (existing instanceof SingleMemberAnnotation) {
					SingleMemberAnnotation annotation= (SingleMemberAnnotation) existing;
					Expression value= annotation.getValue();
					if (!addSuppressArgument(rewrite, value, newStringLiteral)) {
						rewrite.set(existing, SingleMemberAnnotation.VALUE_PROPERTY, newStringLiteral, null);
					}
				} else if (existing instanceof NormalAnnotation) {
					NormalAnnotation annotation= (NormalAnnotation) existing;
					Expression value= findValue(annotation.values());
					if (!addSuppressArgument(rewrite, value, newStringLiteral)) {
						ListRewrite listRewrite= rewrite.getListRewrite(annotation, NormalAnnotation.VALUES_PROPERTY);
						MemberValuePair pair= ast.newMemberValuePair();
						pair.setName(ast.newSimpleName("value")); //$NON-NLS-1$
						pair.setValue(newStringLiteral);
						listRewrite.insertFirst(pair, null);
					}
				}
			}
		}

		private static boolean addSuppressArgument(ASTRewrite rewrite, Expression value, StringLiteral newStringLiteral) {
			if (value instanceof ArrayInitializer) {
				ListRewrite listRewrite= rewrite.getListRewrite(value, ArrayInitializer.EXPRESSIONS_PROPERTY);
				listRewrite.insertLast(newStringLiteral, null);
			} else if (value instanceof StringLiteral) {
				ArrayInitializer newArr= rewrite.getAST().newArrayInitializer();
				newArr.expressions().add(rewrite.createMoveTarget(value));
				newArr.expressions().add(newStringLiteral);
				rewrite.replace(value, newArr, null);
			} else {
				return false;
			}
			return true;
		}

		private static Expression findValue(List<MemberValuePair> keyValues) {
			for (MemberValuePair curr : keyValues) {
				if ("value".equals(curr.getName().getIdentifier())) { //$NON-NLS-1$
					return curr.getValue();
				}
			}
			return null;
		}

		private static Annotation findExistingAnnotation(List<? extends ASTNode> modifiers) {
			for (ASTNode curr : modifiers) {
				if (curr instanceof NormalAnnotation || curr instanceof SingleMemberAnnotation) {
					Annotation annotation= (Annotation) curr;
					ITypeBinding typeBinding= annotation.resolveTypeBinding();
					if (typeBinding != null) {
						if ("java.lang.SuppressWarnings".equals(typeBinding.getQualifiedName())) { //$NON-NLS-1$
							return annotation;
						}
					} else {
						String fullyQualifiedName= annotation.getTypeName().getFullyQualifiedName();
						if ("SuppressWarnings".equals(fullyQualifiedName) || "java.lang.SuppressWarnings".equals(fullyQualifiedName)) { //$NON-NLS-1$ //$NON-NLS-2$
							return annotation;
						}
					}
				}
			}
			return null;
		}
	}

}

