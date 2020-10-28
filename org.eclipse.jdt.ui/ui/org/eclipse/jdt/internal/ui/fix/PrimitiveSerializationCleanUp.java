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
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that replaces a primitive boxing to serialize by a call to the static <code>toString()</code> method.
 *
 * The conditions of triggering are:
 * <ul>
 *  <li>The structure of the code (a <code>toString()</code> call on a boxing)</li>
 *  <li>The boxing can be a <code>valueOf()</code>, an instantiation or a cast,</li>
 *  <li>The boxing should not be on a wrapper.</li>
 * </ul>
 */
public class PrimitiveSerializationCleanUp extends AbstractMultiFix {
	private static final Class<?>[] WRAPPER_CLASSES= { Integer.class, Boolean.class, Long.class, Double.class, Character.class, Float.class, Short.class, Byte.class };

	public PrimitiveSerializationCleanUp() {
		this(Collections.emptyMap());
	}

	public PrimitiveSerializationCleanUp(final Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.PRIMITIVE_SERIALIZATION);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.PRIMITIVE_SERIALIZATION)) {
			return new String[] { MultiFixMessages.PrimitiveSerializationCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.PRIMITIVE_SERIALIZATION)) {
			return "" //$NON-NLS-1$
					+ "String text1 = Integer.toString(number);\n" //$NON-NLS-1$
					+ "String text2 = Character.toString(letter);\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "String text1 = Integer.valueOf(number).toString();\n" //$NON-NLS-1$
				+ "String text2 = Character.valueOf(letter).toString();\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.PRIMITIVE_SERIALIZATION)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final MethodInvocation visited) {
				if (visited.getExpression() != null
						&& visited.arguments().isEmpty()) {
					for (Class<?> wrapperClass : WRAPPER_CLASSES) {
						String canonicalName= wrapperClass.getCanonicalName();

						if (ASTNodes.usesGivenSignature(visited, canonicalName, "toString")) { //$NON-NLS-1$
							String primitiveType= Bindings.getUnboxedTypeName(canonicalName);
							MethodInvocation methodInvocation = ASTNodes.as(visited.getExpression(), MethodInvocation.class);

							if (methodInvocation != null
									&& ASTNodes.usesGivenSignature(methodInvocation, canonicalName, "valueOf", primitiveType)) { //$NON-NLS-1$
								return maybeRefactor(visited, wrapperClass, primitiveType, (Expression) methodInvocation.arguments().get(0));
							}

							ClassInstanceCreation classInstanceCreation = ASTNodes.as(visited.getExpression(), ClassInstanceCreation.class);

							if (classInstanceCreation != null
									&& ASTNodes.hasType(classInstanceCreation.getType().resolveBinding(), canonicalName)) {
								return maybeRefactor(visited, wrapperClass, primitiveType, (Expression) classInstanceCreation.arguments().get(0));
							}

							CastExpression castExpression = ASTNodes.as(visited.getExpression(), CastExpression.class);

							if (castExpression != null
									&& ASTNodes.hasType(castExpression.getType().resolveBinding(), canonicalName)) {
								return maybeRefactor(visited, wrapperClass, primitiveType, castExpression.getExpression());
							}

							return true;
						}
					}
				}

				return true;
			}

			private boolean maybeRefactor(final MethodInvocation visited, final Class<?> wrapperClass, final String primitiveType, final Expression primitiveValue) {
				if (ASTNodes.isPrimitive(primitiveValue, primitiveType)) {
					rewriteOperations.add(new PrimitiveSerializationOperation(visited, primitiveValue, wrapperClass));
					return false;
				}

				return true;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.PrimitiveSerializationCleanUp_description, unit,
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

	private static class PrimitiveSerializationOperation extends CompilationUnitRewriteOperation {
		private final MethodInvocation visited;
		private final Expression primitiveValue;
		private final Class<?> wrapperClass;

		public PrimitiveSerializationOperation(final MethodInvocation visited, final Expression primitiveValue, final Class<?> wrapperClass) {
			this.visited= visited;
			this.primitiveValue= primitiveValue;
			this.wrapperClass= wrapperClass;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.PrimitiveSerializationCleanUp_description, cuRewrite);

			MethodInvocation newMethodInvocation= ast.newMethodInvocation();
			newMethodInvocation.setExpression(ast.newSimpleName(wrapperClass.getSimpleName()));
			newMethodInvocation.setName(ast.newSimpleName("toString")); //$NON-NLS-1$
			newMethodInvocation.arguments().add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression(primitiveValue)));
			ASTNodes.replaceButKeepComment(rewrite, visited, newMethodInvocation, group);
		}
	}
}
