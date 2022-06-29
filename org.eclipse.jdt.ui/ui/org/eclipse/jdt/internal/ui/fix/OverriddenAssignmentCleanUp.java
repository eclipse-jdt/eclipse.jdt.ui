/*******************************************************************************
 * Copyright (c) 2020 Fabrice TIERCELIN and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fabrice TIERCELIN - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.InterruptibleVisitor;
import org.eclipse.jdt.internal.corext.dom.VarDefinitionsUsesVisitor;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

/**
 * A fix that removes passive assignment when the variable is reassigned before being read.
 */
public class OverriddenAssignmentCleanUp extends AbstractCleanUp {
	private static final String IGNORE_LEADING_COMMENT= "ignoreLeadingComment"; //$NON-NLS-1$

	public OverriddenAssignmentCleanUp() {
		this(Collections.emptyMap());
	}

	public OverriddenAssignmentCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.OVERRIDDEN_ASSIGNMENT);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.OVERRIDDEN_ASSIGNMENT)) {
			return new String[] { MultiFixMessages.OverriddenAssignmentCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();

		if (isEnabled(CleanUpConstants.OVERRIDDEN_ASSIGNMENT)) {
			if (isEnabled(CleanUpConstants.OVERRIDDEN_ASSIGNMENT_MOVE_DECL)) {
				bld.append("String separator = System.lineSeparator();\n"); //$NON-NLS-1$
				bld.append("long time = System.currentTimeMillis();\n"); //$NON-NLS-1$
			} else {
				bld.append("long time;\n"); //$NON-NLS-1$
				bld.append("String separator = System.lineSeparator();\n"); //$NON-NLS-1$
				bld.append("time = System.currentTimeMillis();\n"); //$NON-NLS-1$
			}
		} else {
			bld.append("long time = 0;\n"); //$NON-NLS-1$
			bld.append("String separator = \"\";\n"); //$NON-NLS-1$
			bld.append("separator = System.lineSeparator();\n"); //$NON-NLS-1$
			bld.append("time = System.currentTimeMillis();\n"); //$NON-NLS-1$
		}


		return bld.toString();
	}

	@Override
	public ICleanUpFix createFix(CleanUpContext context) throws CoreException {
		if (!isEnabled(CleanUpConstants.OVERRIDDEN_ASSIGNMENT)) {
			return null;
		}

		ICompilationUnit cu= context.getCompilationUnit();
		CompilationUnit unit= context.getAST();

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final VariableDeclarationStatement node) {
				VariableDeclarationFragment fragment= ASTNodes.getUniqueFragment(node);

				if (fragment != null
						&& fragment.getInitializer() != null
						&& ASTNodes.isPassiveWithoutFallingThrough(fragment.getInitializer())) {
					SimpleName varName= fragment.getName();
					IVariableBinding variable= fragment.resolveBinding();
					if (variable != null) {
						Statement stmtToInspect= ASTNodes.getNextSibling(node);
						Statement firstSibling= stmtToInspect;
						Assignment overridingAssignment= null;

						boolean shouldMoveDown= true;
						while (stmtToInspect != null) {
							VarDefinitionsUsesVisitor varDefinitionsUsesVisitor= new VarDefinitionsUsesVisitor(variable, stmtToInspect, true);
							if (!varDefinitionsUsesVisitor.getReads().isEmpty()) {
								return true;
							}


							Assignment assignment= ASTNodes.asExpression(stmtToInspect, Assignment.class);

							if (assignment != null && ASTNodes.isSameVariable(varName, assignment.getLeftHandSide())) {
								if (!ASTNodes.hasOperator(assignment, Assignment.Operator.ASSIGN)) {
									return true;
								}

								overridingAssignment= assignment;
								break;
							} else {
								// it's not an assignment, but it writes to the variable: for example assignments
								// inside an if statement.
								shouldMoveDown &= varDefinitionsUsesVisitor.getWrites().isEmpty();
							}

							stmtToInspect= ASTNodes.getNextSibling(stmtToInspect);
						}

						if (overridingAssignment != null && canMoveNls(node, overridingAssignment)) {
							rewriteOperations.add(new OverriddenAssignmentOperation(node, fragment, overridingAssignment, firstSibling == stmtToInspect,
									shouldMoveDown && isEnabled(CleanUpConstants.OVERRIDDEN_ASSIGNMENT_MOVE_DECL)));
							return false;
						}
					}
				}

				return true;
			}

			private boolean canMoveNls(VariableDeclarationStatement node, Assignment overridingAssignment) {
				try {
					return isOnOwnLine(node) && isOnOwnLine(overridingAssignment);
				} catch (JavaModelException e) {
					// can't guarantee correctness, so bail
					return false;
				}
			}

			private boolean isOnOwnLine(ASTNode node) throws JavaModelException {
				// make sure there is only whitespace before and after the node (or a line comment)
				int startLine= unit.getLineNumber(node.getStartPosition());
				int endLine= unit.getLineNumber(node.getStartPosition() + node.getLength());
				if (startLine != endLine) {
					return false;
				}
				int lineStart= unit.getPosition(startLine, 0);
				int lineEnd= unit.getPosition(startLine + 1, 0);
				if (lineEnd < 0) {
					lineEnd= unit.getLength();
				}
				String textBefore= cu.getBuffer().getText(lineStart, node.getStartPosition()-lineStart);
				if (!textBefore.isBlank()) {
					return false;
				}

				int nodeEnd= node.getStartPosition() + node.getLength();
				String textAfter= cu.getBuffer().getText(nodeEnd, lineEnd - nodeEnd);
				int i= 0;
				while (i < textAfter.length() && (Character.isWhitespace(textAfter.charAt(i)) || textAfter.charAt(i) == ';')) {
					i++;
				}
				if (i > textAfter.length() - 1) {
					return true;
				}
				return textAfter.substring(i).startsWith("//"); //$NON-NLS-1$
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.OverriddenAssignmentCleanUp_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[0]));
	}

	private static class OverriddenAssignmentOperation extends CompilationUnitRewriteOperation {
		private final VariableDeclarationStatement declaration;
		private final Assignment overridingAssignment;
		private final boolean followsImmediately;
		private final boolean moveDown;
		private final VariableDeclarationFragment fragment;

		public OverriddenAssignmentOperation(final VariableDeclarationStatement declaration, VariableDeclarationFragment fragment, Assignment overridingAssignment, boolean followsImmediately, boolean moveDown) {
			this.declaration= declaration;
			this.fragment = fragment;
			this.overridingAssignment= overridingAssignment;
			this.followsImmediately= followsImmediately;
			this.moveDown= moveDown;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
					if (Boolean.TRUE.equals(nodeWithComment.getProperty(IGNORE_LEADING_COMMENT))) {
						ASTNode root= nodeWithComment.getRoot();
						if (root instanceof CompilationUnit) {
							CompilationUnit cu= (CompilationUnit) root;
							int extendedEnd= cu.getExtendedStartPosition(nodeWithComment) + cu.getExtendedLength(nodeWithComment);

							return new SourceRange(nodeWithComment.getStartPosition(), extendedEnd - nodeWithComment.getStartPosition());
						}
					}

					return super.computeSourceRange(nodeWithComment);
				}
			});
			TextEditGroup group= createTextEditGroup(MultiFixMessages.OverriddenAssignmentCleanUp_description, cuRewrite);
			boolean canMoveDown = overridingAssignment.getParent() instanceof ExpressionStatement;
			boolean canMoveUp= followsImmediately || canMoveUp();


			if (canMoveUp) {
				moveUp(cuRewrite, group);
			} else if (canMoveDown && moveDown) {
				moveDown(cuRewrite, group);
			} else {
				removeInitializer(cuRewrite, group);
			}
		}

		private boolean canMoveUp() {
			if (!ASTNodes.isPassiveWithoutFallingThrough(overridingAssignment.getRightHandSide())) {
				return false;
			}

			Block containingBlock= ASTNodes.getTypedAncestor(declaration, Block.class);

			class UndefinedVarsFinder extends InterruptibleVisitor {
				boolean preventsMoveUp= false;

				@Override
				public boolean visit(SimpleName node) {
					IBinding usedBinding= node.resolveBinding();
					if (usedBinding == null) {
						this.preventsMoveUp= true;
						return interruptVisit();
					}
					ASTNode usedName= ASTNodes.findDeclaration(usedBinding, containingBlock);
					if (usedName != null && usedName.getStartPosition() > declaration.getStartPosition()) {
						this.preventsMoveUp= true;
						return interruptVisit();
					}
					return super.visit(node);
				}
			}
			UndefinedVarsFinder visitor= new UndefinedVarsFinder();

			visitor.traverseNodeInterruptibly(overridingAssignment.getRightHandSide());

			return !visitor.preventsMoveUp;
		}

		private void removeInitializer(CompilationUnitRewrite cuRewrite, TextEditGroup group) throws JavaModelException {
			ICompilationUnit cu= cuRewrite.getCu();
			int nameEnd= fragment.getName().getStartPosition() + fragment.getName().getLength();
			String declarationText= cu.getBuffer().getText(declaration.getStartPosition(), nameEnd-declaration.getStartPosition());
			ASTRewrite astRewrite= cuRewrite.getASTRewrite();
			ASTNode replacementNode= astRewrite.createStringPlaceholder(declarationText+";", ASTNode.VARIABLE_DECLARATION_STATEMENT); //$NON-NLS-1$
			declaration.setProperty(IGNORE_LEADING_COMMENT, Boolean.TRUE);
			astRewrite.replace(declaration, replacementNode, group);
		}

		private void moveUp(final CompilationUnitRewrite cuRewrite, TextEditGroup group) throws JavaModelException {
			ICompilationUnit cu= cuRewrite.getCu();

			Expression rhs= overridingAssignment.getRightHandSide();
			String rhsText= cu.getBuffer().getText(rhs.getStartPosition(), extendedEnd(cuRewrite.getRoot(), overridingAssignment.getParent()) - rhs.getStartPosition());

			String declarationText= cu.getBuffer().getText(declaration.getStartPosition(), fragment.getInitializer().getStartPosition() - declaration.getStartPosition());
			String targetText= declarationText + rhsText;

			ASTRewrite astRewrite= cuRewrite.getASTRewrite();
			ASTNode replacementNode= astRewrite.createStringPlaceholder(targetText, ASTNode.VARIABLE_DECLARATION_STATEMENT);
			declaration.setProperty(IGNORE_LEADING_COMMENT, Boolean.TRUE);
			astRewrite.replace(declaration, replacementNode, group);
			astRewrite.remove(overridingAssignment.getParent(), group);
		}

		private void moveDown(final CompilationUnitRewrite cuRewrite, TextEditGroup group) throws JavaModelException {
			ICompilationUnit cu= cuRewrite.getCu();

			Expression rhs= overridingAssignment.getRightHandSide();
			String rhsText= cu.getBuffer().getText(rhs.getStartPosition(), extendedEnd(cuRewrite.getRoot(), overridingAssignment.getParent()) - rhs.getStartPosition());

			String declarationText= cu.getBuffer().getText(declaration.getStartPosition(), fragment.getInitializer().getStartPosition() - declaration.getStartPosition());

			String targetText= declarationText + rhsText;

			ASTRewrite astRewrite= cuRewrite.getASTRewrite();
			ASTNode replacementNode= astRewrite.createStringPlaceholder(targetText, ASTNode.VARIABLE_DECLARATION_STATEMENT);
			astRewrite.replace(overridingAssignment.getParent(), replacementNode, group);
			astRewrite.remove(declaration, group);
		}

		int extendedEnd(CompilationUnit cu, ASTNode node) {
			return cu.getExtendedStartPosition(node) + cu.getExtendedLength(node);
		}
	}
}
