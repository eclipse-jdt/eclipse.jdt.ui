/*******************************************************************************
 * Copyright (c) 2019, 2022 Fabrice TIERCELIN and others.
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
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that uses Unboxing:
 * <ul>
 * <li>As of Java 5, intValue() call can be replaced the Integer wrapper expression directly.
 * And it is the case for all the primitive wrappers. The method call is automatically added at compile time.</li>
 * </ul>
 */
public class UnboxingCleanUp extends AbstractMultiFix {
	private static final String DOUBLE_VALUE= "doubleValue"; //$NON-NLS-1$
	private static final String FLOAT_VALUE= "floatValue"; //$NON-NLS-1$
	private static final String LONG_VALUE= "longValue"; //$NON-NLS-1$
	private static final String INT_VALUE= "intValue"; //$NON-NLS-1$
	private static final String SHORT_VALUE= "shortValue"; //$NON-NLS-1$
	private static final String CHAR_VALUE= "charValue"; //$NON-NLS-1$
	private static final String BYTE_VALUE= "byteValue"; //$NON-NLS-1$
	private static final String BOOLEAN_VALUE= "booleanValue"; //$NON-NLS-1$

	public UnboxingCleanUp() {
		this(Collections.emptyMap());
	}

	public UnboxingCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.USE_UNBOXING);
		Map<String, String> requiredOptions= null;
		return new CleanUpRequirements(requireAST, false, false, requiredOptions);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.USE_UNBOXING)) {
			return new String[] { MultiFixMessages.UnboxingCleanup_description };
		}
		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();
		bld.append("Integer integerObject = Integer.MAX_VALUE;\n"); //$NON-NLS-1$
		bld.append("Character cObject = Character.MAX_VALUE;\n"); //$NON-NLS-1$
		bld.append("\n"); //$NON-NLS-1$

		if (isEnabled(CleanUpConstants.USE_UNBOXING)) {
			bld.append("int i = integerObject;\n"); //$NON-NLS-1$
			bld.append("char c = cObject;\n"); //$NON-NLS-1$
		} else {
			bld.append("int i = integerObject.intValue();\n"); //$NON-NLS-1$
			bld.append("char c = cObject.charValue();\n"); //$NON-NLS-1$
		}

		return bld.toString();
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.USE_UNBOXING) || !JavaModelUtil.is50OrHigher(unit.getJavaElement().getJavaProject())) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodInvocation visited) {
				ASTNode parent= visited.getParent();
				while (parent != null && parent instanceof ParenthesizedExpression) {
					parent= parent.getParent();
				}
				if (parent instanceof CastExpression) {
					return true;
				}
				if (visited.getExpression() != null) {
					ITypeBinding nodeBinding= visited.getExpression().resolveTypeBinding();

					if (nodeBinding != null	&& nodeBinding.isClass()
							&& (ASTNodes.usesGivenSignature(visited, Boolean.class.getCanonicalName(), BOOLEAN_VALUE)
									|| ASTNodes.usesGivenSignature(visited, Byte.class.getCanonicalName(), BYTE_VALUE)
									|| ASTNodes.usesGivenSignature(visited, Character.class.getCanonicalName(), CHAR_VALUE)
									|| ASTNodes.usesGivenSignature(visited, Short.class.getCanonicalName(), SHORT_VALUE)
									|| ASTNodes.usesGivenSignature(visited, Integer.class.getCanonicalName(), INT_VALUE)
									|| ASTNodes.usesGivenSignature(visited, Long.class.getCanonicalName(), LONG_VALUE)
									|| ASTNodes.usesGivenSignature(visited, Float.class.getCanonicalName(), FLOAT_VALUE)
									|| ASTNodes.usesGivenSignature(visited, Double.class.getCanonicalName(), DOUBLE_VALUE))) {
						final ITypeBinding actualResultType= ASTNodes.getTargetType(visited);

						if (actualResultType != null && actualResultType.isAssignmentCompatible(visited.resolveTypeBinding())) {
							parent= visited.getParent();

							if (parent instanceof ClassInstanceCreation
									&& visited.getLocationInParent() == ClassInstanceCreation.ARGUMENTS_PROPERTY) {
								ClassInstanceCreation classInstanceCreation= (ClassInstanceCreation) parent;

								if (hasConflictingMethodOrConstructor(visited, classInstanceCreation.resolveConstructorBinding(), classInstanceCreation.arguments())) {
									return true;
								}
							} else if (parent instanceof MethodInvocation
									&& visited.getLocationInParent() == MethodInvocation.ARGUMENTS_PROPERTY) {
								MethodInvocation methodInvocation= (MethodInvocation) parent;

								if (hasConflictingMethodOrConstructor(visited, methodInvocation.resolveMethodBinding(), methodInvocation.arguments())) {
									return true;
								}
							} else if (parent instanceof SuperMethodInvocation
									&& visited.getLocationInParent() == SuperMethodInvocation.ARGUMENTS_PROPERTY) {
								SuperMethodInvocation superMethodInvocation= (SuperMethodInvocation) parent;

								if (hasConflictingMethodOrConstructor(visited, superMethodInvocation.resolveMethodBinding(), superMethodInvocation.arguments())) {
									return true;
								}
							} else if (parent instanceof SuperConstructorInvocation
									&& visited.getLocationInParent() == SuperConstructorInvocation.ARGUMENTS_PROPERTY) {
								SuperConstructorInvocation superConstructorInvocation= (SuperConstructorInvocation) parent;

								if (hasConflictingMethodOrConstructor(visited, superConstructorInvocation.resolveConstructorBinding(), superConstructorInvocation.arguments())) {
									return true;
								}
							}

							rewriteOperations.add(new UnboxingOperation(visited));
							return false;
						}
					}
				}

				return true;
			}

			private boolean hasConflictingMethodOrConstructor(final MethodInvocation visited, final IMethodBinding binding, final List<Expression> arguments) {
				int argumentIndex= arguments.indexOf(visited);

				if (argumentIndex < 0 || binding.getParameterTypes().length <= argumentIndex) {
					return true;
				}

				ITypeBinding[] argumentTypes= binding.getParameterTypes().clone();
				argumentTypes[argumentIndex]= visited.getExpression().resolveTypeBinding();

				return ASTNodes.hasConflictingMethodOrConstructor(visited.getParent(), binding, argumentTypes);
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.UnboxingCleanup_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[rewriteOperations.size()]));
	}

	@Override
	public boolean canFix(ICompilationUnit compilationUnit, IProblemLocation problem) {
		return false;
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit, IProblemLocation[] problems) throws CoreException {
		return null;
	}

	private static class UnboxingOperation extends CompilationUnitRewriteOperation {
		private final MethodInvocation node;

		public UnboxingOperation(MethodInvocation node) {
			this.node= node;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.UnboxingCleanup_description, cuRewrite);
			Expression copyOfWrapper= (Expression) rewrite.createCopyTarget(node.getExpression());
			ASTNodes.replaceButKeepComment(rewrite, node, copyOfWrapper, group);
		}
	}
}