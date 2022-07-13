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
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.InterruptibleVisitor;
import org.eclipse.jdt.internal.corext.dom.VarDefinitionsUsesVisitor;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that removes passive assignment when the variable is reassigned before being read.
 */
public class OverriddenAssignmentCleanUp extends AbstractMultiFix {
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
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.OVERRIDDEN_ASSIGNMENT)) {
			return null;
		}

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

						if (overridingAssignment != null) {
							rewriteOperations.add(new OverriddenAssignmentOperation(node, fragment, overridingAssignment, firstSibling == stmtToInspect,
									shouldMoveDown && isEnabled(CleanUpConstants.OVERRIDDEN_ASSIGNMENT_MOVE_DECL)));
							return false;
						}
					}
				}

				return true;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.OverriddenAssignmentCleanUp_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[0]));
	}

	@Override
	public boolean canFix(final ICompilationUnit compilationUnit, final IProblemLocation problem) {
		return false;
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit, final IProblemLocation[] problems) throws CoreException {
		return null;
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
			TextEditGroup group= createTextEditGroup(MultiFixMessages.OverriddenAssignmentCleanUp_description, cuRewrite);
			boolean canMoveDown = overridingAssignment.getParent() instanceof ExpressionStatement;
			boolean canMoveUp= followsImmediately || canMoveUp();

			if (canMoveUp) {
				// only move initialization up if there are no side effects and the assignment is a statement
				var copy= rewrite.createCopyTarget(this.overridingAssignment.getRightHandSide());
				rewrite.replace(fragment.getInitializer(), copy , group);
				rewrite.remove(overridingAssignment.getParent(), group);
			} else if (canMoveDown && moveDown) {
				var copy= rewrite.createCopyTarget(this.overridingAssignment.getRightHandSide());
				rewrite.replace(fragment.getInitializer(), copy , group);
				copy= rewrite.createCopyTarget(declaration);
				rewrite.replace(overridingAssignment.getParent(), copy, group);
				rewrite.remove(declaration, group);
			} else {
				rewrite.remove(fragment.getInitializer(), group);
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
	}
}
