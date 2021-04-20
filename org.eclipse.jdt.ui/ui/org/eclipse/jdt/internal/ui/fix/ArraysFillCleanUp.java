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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ForLoops;
import org.eclipse.jdt.internal.corext.dom.ForLoops.ContainerType;
import org.eclipse.jdt.internal.corext.dom.ForLoops.ForLoopContent;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that uses Arrays.fill() when possible:
 * <ul>
 * <li>Check that we iterate on the whole array.</li>
 * <li>Check that we always set the same value.</li>
 * </ul>
 */
public class ArraysFillCleanUp extends AbstractMultiFix {
	public ArraysFillCleanUp() {
		this(Collections.emptyMap());
	}

	public ArraysFillCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.ARRAYS_FILL);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.ARRAYS_FILL)) {
			return new String[] { MultiFixMessages.ArraysFillCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.ARRAYS_FILL)) {
			return "Arrays.fill(array, true);\n\n\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "for (int i = 0; i < array.length; i++) {\n" //$NON-NLS-1$
				+ "  array[i] = true;\n" //$NON-NLS-1$
				+ "}\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.ARRAYS_FILL)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final ForStatement node) {
				ForLoopContent loopContent= ForLoops.iterateOverContainer(node);
				Assignment assignment= ASTNodes.asExpression(node.getBody(), Assignment.class);

				if (assignment != null
						&& loopContent != null
						&& loopContent.getLoopVariable() != null
						&& loopContent.getContainerType() == ContainerType.ARRAY
						&& ASTNodes.hasOperator(assignment, Assignment.Operator.ASSIGN)
						&& isUnchangedValue(node, assignment.getRightHandSide())
						&& ASTNodes.isPassive(assignment.getRightHandSide())) {
					ArrayAccess arrayAccess= ASTNodes.as(assignment.getLeftHandSide(), ArrayAccess.class);

					if (arrayAccess != null
							&& isSameVariable(loopContent, arrayAccess)
							&& areTypesCompatible(loopContent, assignment)) {
						rewriteOperations.add(new ArraysFillOperation(node, assignment, arrayAccess));
						return false;
					}
				}

				return true;
			}

			private boolean areTypesCompatible(final ForLoopContent loopContent, final Assignment assignment) {
				return loopContent.getContainerVariable().resolveTypeBinding() != null
						&& loopContent.getContainerVariable().resolveTypeBinding().getElementType() != null
						&& (!ASTNodes.hasType(loopContent.getContainerVariable(), char[].class.getCanonicalName(), byte[].class.getCanonicalName(), short[].class.getCanonicalName())
								|| loopContent.getContainerVariable().resolveTypeBinding().getElementType().equals(assignment.getRightHandSide().resolveTypeBinding()));
			}

			private boolean isUnchangedValue(final ForStatement node, final Expression expression) {
				if (!ASTNodes.isPassive(expression)) {
					return false;
				}

				if (ASTNodes.isHardCoded(expression)) {
					return true;
				}

				SimpleName simpleName= null;

				if (expression instanceof QualifiedName) {
					QualifiedName name= (QualifiedName) expression;
					IBinding binding= name.getQualifier().resolveBinding();

					if (binding.getKind() == IBinding.TYPE) {
						simpleName= name.getName();
					}
				} else if (expression instanceof SimpleName) {
					simpleName= (SimpleName) expression;
				}

				if (simpleName != null) {
					IBinding binding= simpleName.resolveBinding();

					if (binding instanceof IVariableBinding) {
						IVariableBinding varBinding= (IVariableBinding) binding;

						if (varBinding.isField()) {
							return Modifier.isFinal(varBinding.getModifiers());
						}

						if (varBinding.isEffectivelyFinal()) {
							return true;
						}

						ASTNode declaration= ASTNodes.findDeclaration(binding, expression.getRoot());
						return declaration != null && !ASTNodes.isParent(declaration, node);
					}
				}

				return false;
			}

			private boolean isSameVariable(final ForLoopContent loopContent, final ArrayAccess arrayAccess) {
				return arrayAccess != null
						&& ASTNodes.isSameVariable(arrayAccess.getArray(), loopContent.getContainerVariable())
						&& ASTNodes.isSameLocalVariable(arrayAccess.getIndex(), loopContent.getLoopVariable());
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.ArraysFillCleanUp_description, unit,
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

	private static class ArraysFillOperation extends CompilationUnitRewriteOperation {
		private final ForStatement node;
		private final Assignment assignment;
		private final ArrayAccess arrayAccess;

		public ArraysFillOperation(final ForStatement node, final Assignment assignment, final ArrayAccess arrayAccess) {
			this.node= node;
			this.assignment= assignment;
			this.arrayAccess= arrayAccess;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.ArraysFillCleanUp_description, cuRewrite);
			ImportRewrite importRewrite= cuRewrite.getImportRewrite();

			String arraysNameText= importRewrite.addImport(Arrays.class.getCanonicalName());

			MethodInvocation methodInvocation= ast.newMethodInvocation();
			methodInvocation.setExpression(ASTNodeFactory.newName(ast, arraysNameText));
			methodInvocation.setName(ast.newSimpleName("fill")); //$NON-NLS-1$
			methodInvocation.arguments().add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression(arrayAccess.getArray())));
			methodInvocation.arguments().add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression(assignment.getRightHandSide())));

			ExpressionStatement expressionStatement= ast.newExpressionStatement(methodInvocation);

			ASTNodes.replaceButKeepComment(rewrite, node, expressionStatement, group);
		}
	}
}
