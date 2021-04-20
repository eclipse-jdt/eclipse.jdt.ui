/*******************************************************************************
 * Copyright (c) 2021 Fabrice TIERCELIN and others.
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
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that uses an <code>instanceof</code> expression to check an object against a hardcoded class:
 * <ul>
 * <li>The type should be a class,</li>
 * <li>The class should not be a variable.</li>
 * </ul>
 */
public class InstanceofCleanUp extends AbstractMultiFix {
	public InstanceofCleanUp() {
		this(Collections.emptyMap());
	}

	public InstanceofCleanUp(final Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.INSTANCEOF);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.INSTANCEOF)) {
			return new String[] { MultiFixMessages.CodeStyleCleanUp_Instanceof_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.INSTANCEOF)) {
			return "boolean isRightType = (o instanceof String);\n"; //$NON-NLS-1$
		}

		return "boolean isRightType = String.class.isInstance(o);\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.INSTANCEOF)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final MethodInvocation visited) {
				TypeLiteral clazz= ASTNodes.as(visited.getExpression(), TypeLiteral.class);

				if (clazz != null
						&& clazz.getType().resolveBinding() != null
						&& !clazz.getType().resolveBinding().isPrimitive()
						&& ASTNodes.usesGivenSignature(visited, Class.class.getCanonicalName(), "isInstance", Object.class.getCanonicalName()) //$NON-NLS-1$
						&& ((Expression) visited.arguments().get(0)).resolveTypeBinding() != null
						&& clazz.getType().resolveBinding().isSubTypeCompatible(((Expression) visited.arguments().get(0)).resolveTypeBinding())) {
					rewriteOperations.add(new InstanceofOperation(visited, clazz));
					return false;
				}

				return true;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.CodeStyleCleanUp_Instanceof_description, unit,
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

	private static class InstanceofOperation extends CompilationUnitRewriteOperation {
		private final MethodInvocation visited;
		private final TypeLiteral clazz;

		public InstanceofOperation(final MethodInvocation visited, final TypeLiteral clazz) {
			this.visited= visited;
			this.clazz= clazz;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.CodeStyleCleanUp_Instanceof_description, cuRewrite);

			InstanceofExpression newInstanceofExpression= ast.newInstanceofExpression();
			newInstanceofExpression.setLeftOperand(ASTNodeFactory.parenthesizeIfNeeded(ast, ASTNodes.createMoveTarget(rewrite, (Expression) visited.arguments().get(0))));
			newInstanceofExpression.setRightOperand(ASTNodes.createMoveTarget(rewrite, clazz.getType()));

			ASTNodes.replaceButKeepComment(rewrite, visited, ASTNodeFactory.parenthesizeIfNeeded(ast, newInstanceofExpression), group);
		}
	}
}
