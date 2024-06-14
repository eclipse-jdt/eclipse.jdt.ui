/*******************************************************************************
 * Copyright (c) 2024 Fabrice TIERCELIN and others.
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
 *     Red Hat Inc. - code extracted from OverriddenAssignmentCleanUp
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
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
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.InterruptibleVisitor;
import org.eclipse.jdt.internal.corext.dom.VarDefinitionsUsesVisitor;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class OverriddenAssignmentFixCore extends CompilationUnitRewriteOperationsFixCore {

	private static final String IGNORE_LEADING_COMMENT= "ignoreLeadingComment"; //$NON-NLS-1$

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
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
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


			int start= fragment.getStartPosition()+ fragment.getLength();
			int end= overridingAssignment.getStartPosition();

			final class DeclarationChecker extends ASTVisitor{
				private boolean preventsMoveUp= false;

				private boolean preventsMoveUp(Name name) {
					IBinding binding= name.resolveBinding();
					if (binding instanceof IVariableBinding) {
						ASTNode var= ASTNodes.findDeclaration(binding, fragment.getRoot());
						if (var != null && (var.getStartPosition() >= start || var.getStartPosition() <= end-var.getLength())) {
							return true;
						}
					}
					return false;
				}

				@Override
				public boolean visit(QualifiedName node) {
					preventsMoveUp |= preventsMoveUp(node);
					return false;
				}

				@Override
				public boolean visit(SimpleName node) {
					preventsMoveUp |= preventsMoveUp(node);
					return false;
				}
			}

			DeclarationChecker declarationChecker= new DeclarationChecker();
			overridingAssignment.getRightHandSide().accept(declarationChecker);
			canMoveUp &= !declarationChecker.preventsMoveUp;

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
			String rhsText= cu.getBuffer().getText(extendedStart(cuRewrite.getRoot(), rhs), extendedEnd(cuRewrite.getRoot(), overridingAssignment.getParent()) - extendedStart(cuRewrite.getRoot(), rhs));

			String targetText= null;
			if (fragment.getInitializer() == null) {
				String declarationText= cu.getBuffer().getText(declaration.getStartPosition(), declaration.getLength() - 1);
				Hashtable<String, String> options= JavaCore.getOptions();
				String spaceBeforeAssignment= options.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR) == JavaCore.INSERT ? " " : ""; //$NON-NLS-1$ //$NON-NLS-2$
				String spaceAfterAssignment= options.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ASSIGNMENT_OPERATOR) == JavaCore.INSERT ? " " : ""; //$NON-NLS-1$ //$NON-NLS-2$
				targetText= declarationText + spaceBeforeAssignment + "=" + spaceAfterAssignment + rhsText; //$NON-NLS-1$
			} else {
				String declarationText= cu.getBuffer().getText(declaration.getStartPosition(), fragment.getInitializer().getStartPosition() - declaration.getStartPosition());
				targetText= declarationText + rhsText;
			}
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

			String declarationText= ""; //$NON-NLS-1$
			if (fragment.getInitializer() != null) {
				declarationText= cu.getBuffer().getText(declaration.getStartPosition(), fragment.getInitializer().getStartPosition() - declaration.getStartPosition());
			} else {
				declarationText= cu.getBuffer().getText(declaration.getStartPosition(), fragment.getStartPosition() + fragment.getLength() - declaration.getStartPosition());
				Hashtable<String, String> options= JavaCore.getOptions();
				String spaceBeforeAssignment= options.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR) == JavaCore.INSERT ? " " : ""; //$NON-NLS-1$ //$NON-NLS-2$
				String spaceAfterAssignment= options.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ASSIGNMENT_OPERATOR) == JavaCore.INSERT ? " " : ""; //$NON-NLS-1$ //$NON-NLS-2$
				declarationText= declarationText + spaceBeforeAssignment + "=" + spaceAfterAssignment; //$NON-NLS-1$
			}
			String targetText= declarationText + rhsText;
			ASTRewrite astRewrite= cuRewrite.getASTRewrite();
			ASTNode replacementNode= astRewrite.createStringPlaceholder(targetText, ASTNode.VARIABLE_DECLARATION_STATEMENT);
			overridingAssignment.getParent().setProperty(IGNORE_LEADING_COMMENT, Boolean.TRUE);
			astRewrite.remove(declaration, group);
			ASTNodes.replaceButKeepComment(astRewrite, overridingAssignment.getParent(), replacementNode, group);
		}

		int extendedEnd(CompilationUnit cu, ASTNode node) {
			return cu.getExtendedStartPosition(node) + cu.getExtendedLength(node);
		}

		int extendedStart(CompilationUnit cu, ASTNode node) {
			return cu.getExtendedStartPosition(node);
		}
	}

	public static ICleanUpFix createCleanUp(final CompilationUnit unit, boolean canMoveDecl) {

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final VariableDeclarationStatement node) {
				VariableDeclarationFragment fragment= ASTNodes.getUniqueFragment(node);

				if (fragment != null
						&& (fragment.getInitializer() == null
						|| ASTNodes.isPassiveWithoutFallingThrough(fragment.getInitializer()))) {
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

							if (fragment.getInitializer() == null) {
								stmtToInspect= null;
							} else {
								stmtToInspect= ASTNodes.getNextSibling(stmtToInspect);
							}
						}

						if (overridingAssignment != null && doesNotShareLines(node) && doesNotShareLines(overridingAssignment)) {
							rewriteOperations.add(new OverriddenAssignmentOperation(node, fragment, overridingAssignment, firstSibling == stmtToInspect,
									shouldMoveDown && canMoveDecl));
							return false;
						}
					}
				}

				return true;
			}

			/**
			 * Check that statement containing ASTNode does not share lines with other statements
			 *
			 * @param node ASTNode
			 * @return true if node's statement does not share lines with other statements otherwise false
			 */
			private boolean doesNotShareLines(ASTNode node) {
				Statement stmt= null;
				if (node instanceof Statement) {
					stmt= (Statement)node;
				} else {
					stmt= ASTNodes.getFirstAncestorOrNull(node, Statement.class);
				}
				if (stmt == null) {
					return false;
				}
				int stmtStartLine= unit.getLineNumber(stmt.getStartPosition());
				int stmtEndLine= unit.getLineNumber(stmt.getStartPosition() + stmt.getLength());
				Statement prevStmt= ASTNodes.getPreviousSibling(stmt);
				if (prevStmt == null) {
					ASTNode parent= stmt.getParent();
					while (parent != null && parent instanceof Block) {
						parent= parent.getParent();
					}
					if (parent instanceof Statement) {
						prevStmt= (Statement)parent;
					}
				}
				Statement nextStmt= ASTNodes.getNextStatement(stmt);
				int prevEndLine= prevStmt == null ? -1 : unit.getLineNumber(prevStmt.getStartPosition() + prevStmt.getLength());
				int nextStartLine= nextStmt == null ? Integer.MAX_VALUE : unit.getLineNumber(nextStmt.getStartPosition());

				return (stmtStartLine > prevEndLine) && (stmtEndLine < nextStartLine);
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFixCore(MultiFixMessages.OverriddenAssignmentCleanUp_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[0]));
	}

	protected OverriddenAssignmentFixCore(final String name, final CompilationUnit compilationUnit, final CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}

}
