/*******************************************************************************
 * Copyright (c) 2019 Fabrice TIERCELIN and others.
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
import java.util.Objects;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PrimitiveType;
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
 * A fix that uses Autoboxing:
 * <ul>
 * <li>As of Java 5, Integer.valueOf(int) can be replaced the int expression directly.
 * And it is the case for all the primitive types. The method call is automatically added at compile time.</li>
 * </ul>
 */
public class AutoboxingCleanUp extends AbstractMultiFix {
	private static final String VALUE_OF_METHOD= "valueOf"; //$NON-NLS-1$

	public AutoboxingCleanUp() {
		this(Collections.emptyMap());
	}

	public AutoboxingCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.USE_AUTOBOXING);
		Map<String, String> requiredOptions= null;
		return new CleanUpRequirements(requireAST, false, false, requiredOptions);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.USE_AUTOBOXING)) {
			return new String[] { MultiFixMessages.AutoboxingCleanup_description };
		}
		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.USE_AUTOBOXING)) {
			return "" //$NON-NLS-1$
					+ "Integer i = 0;\n" //$NON-NLS-1$
					+ "Character c = '*';\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "Integer i = Integer.valueOf(0);\n" //$NON-NLS-1$
				+ "Character c = Character.valueOf('*');\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.USE_AUTOBOXING) || !JavaModelUtil.is50OrHigher(unit.getJavaElement().getJavaProject())) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodInvocation visited) {
				if ((ASTNodes.usesGivenSignature(visited, Boolean.class.getCanonicalName(), VALUE_OF_METHOD, boolean.class.getSimpleName())
						|| ASTNodes.usesGivenSignature(visited, Byte.class.getCanonicalName(), VALUE_OF_METHOD, byte.class.getSimpleName())
						|| ASTNodes.usesGivenSignature(visited, Character.class.getCanonicalName(), VALUE_OF_METHOD, char.class.getSimpleName())
						|| ASTNodes.usesGivenSignature(visited, Short.class.getCanonicalName(), VALUE_OF_METHOD, short.class.getSimpleName())
						|| ASTNodes.usesGivenSignature(visited, Integer.class.getCanonicalName(), VALUE_OF_METHOD, int.class.getSimpleName())
						|| ASTNodes.usesGivenSignature(visited, Long.class.getCanonicalName(), VALUE_OF_METHOD, long.class.getSimpleName())
						|| ASTNodes.usesGivenSignature(visited, Float.class.getCanonicalName(), VALUE_OF_METHOD, float.class.getSimpleName())
						|| ASTNodes.usesGivenSignature(visited, Double.class.getCanonicalName(), VALUE_OF_METHOD, double.class.getSimpleName())
						)) {
					final ITypeBinding primitiveType= visited.resolveMethodBinding().getParameterTypes()[0];
					final ITypeBinding wrapperClass= visited.resolveMethodBinding().getDeclaringClass();

					final ITypeBinding actualResultType= ASTNodes.getTargetType(visited);
					final ITypeBinding actualParameterType= ((Expression) visited.arguments().get(0)).resolveTypeBinding();

					if (actualParameterType != null
							&& (actualResultType != null
							&& (actualResultType.equals(primitiveType) || actualResultType.equals(wrapperClass)))
							|| Objects.equals(actualParameterType, wrapperClass)) {
						ASTNode parent= visited.getParent();

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

						rewriteOperations.add(new AutoboxingOperation(visited, primitiveType, wrapperClass, actualParameterType, actualResultType));
						return false;
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

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.AutoboxingCleanup_description, unit,
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

	private static class AutoboxingOperation extends CompilationUnitRewriteOperation {
		private final ASTNode node;

		private final ITypeBinding primitiveType;

		private final ITypeBinding wrapperClass;

		private final ITypeBinding actualParameterType;

		private final ITypeBinding actualResultType;

		public AutoboxingOperation(ASTNode node, final ITypeBinding primitiveType,
				final ITypeBinding wrapperClass, final ITypeBinding actualParameterType,
				final ITypeBinding actualResultType) {
			this.node= node;
			this.primitiveType= primitiveType;
			this.wrapperClass= wrapperClass;
			this.actualParameterType= actualParameterType;
			this.actualResultType= actualResultType;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.AutoboxingCleanup_description, cuRewrite);
			Expression arg0= (Expression) rewrite.createCopyTarget((Expression) ((MethodInvocation) node).arguments().get(0));

			if (primitiveType != null && !primitiveType.equals(actualParameterType)
					&& !primitiveType.equals(actualResultType)
					&& (wrapperClass == null || !wrapperClass.equals(actualParameterType))) {
				CastExpression newCastExpression= ast.newCastExpression();
				newCastExpression.setType(ast.newPrimitiveType(PrimitiveType.toCode(primitiveType.getName())));
				newCastExpression.setExpression(arg0);

				ASTNodes.replaceButKeepComment(rewrite, node, newCastExpression, group);
			} else {
				ASTNodes.replaceButKeepComment(rewrite, node, arg0, group);
			}
		}
	}
}
