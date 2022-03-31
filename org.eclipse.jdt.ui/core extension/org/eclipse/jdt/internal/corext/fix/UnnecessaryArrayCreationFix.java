/*******************************************************************************
 * Copyright (c) 2019, 2021 Red Hat Inc. and others.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

import org.eclipse.jdt.internal.ui.fix.CleanUpFixWrapper;

public class UnnecessaryArrayCreationFix extends CompilationUnitRewriteOperationsFix {

	public final static class UnnecessaryArrayCreationFinder extends GenericVisitor {
		private static final Set<String> fInvalidTypes= new HashSet<>(Arrays.asList("byte", "char", "short")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		private final List<CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation> fResult;
		private final boolean fRemoveUnnecessaryArrayCreation;

		public UnnecessaryArrayCreationFinder(boolean removeUnnecessaryArrayCreation, List<CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation> resultingCollection) {
			fRemoveUnnecessaryArrayCreation= removeUnnecessaryArrayCreation;
			fResult= resultingCollection;
		}

		@Override
		public boolean visit(ArrayCreation visited) {
			if (!fRemoveUnnecessaryArrayCreation
					|| visited.getType().getDimensions() != 1) {
				return true;
			}

			ArrayInitializer initializer= visited.getInitializer();

			if (initializer != null
					&& initializer.expressions() != null
					&& initializer.expressions().size() == 1) {
				List<Expression> expressions= initializer.expressions();
				ITypeBinding singleElement= expressions.get(0).resolveTypeBinding();

				if (ASTNodes.is(expressions.get(0), NullLiteral.class)
						|| singleElement == null
						|| singleElement.isArray()) {
					return true;
				}
			}

			ASTNode parent= visited.getParent();

			if (parent instanceof ClassInstanceCreation
					&& visited.getLocationInParent() == ClassInstanceCreation.ARGUMENTS_PROPERTY) {
				ClassInstanceCreation cic= (ClassInstanceCreation) parent;

				if (canArrayBeRemoved(visited, cic.arguments(), cic.resolveConstructorBinding())) {
					fResult.add(new UnwrapNewArrayOperation(visited, cic));
				}
			} else if (parent instanceof MethodInvocation
					&& visited.getLocationInParent() == MethodInvocation.ARGUMENTS_PROPERTY) {
				MethodInvocation m= (MethodInvocation) parent;

				if (canArrayBeRemoved(visited, m.arguments(), m.resolveMethodBinding())) {
					fResult.add(new UnwrapNewArrayOperation(visited, m));
				}
			} else if (parent instanceof SuperMethodInvocation
					&& visited.getLocationInParent() == SuperMethodInvocation.ARGUMENTS_PROPERTY) {
				SuperMethodInvocation sm= (SuperMethodInvocation) parent;

				if (canArrayBeRemoved(visited, sm.arguments(), sm.resolveMethodBinding())) {
					fResult.add(new UnwrapNewArrayOperation(visited, sm));
				}
			}

			return true;
		}

		private boolean canArrayBeRemoved(ArrayCreation visited, List<Expression> arguments, IMethodBinding binding) {
			return isUselessArrayCreation(visited, arguments, binding)
					&& !ASTNodes.hasConflictingMethodOrConstructor(visited.getParent(), binding, getParameterTypesForConflictingMethod(arguments, visited));
		}

		private boolean isUselessArrayCreation(ArrayCreation visited, List<Expression> arguments, IMethodBinding binding) {
			return (visited.getInitializer() != null || (visited.dimensions().size() == 1 && Long.valueOf(0L).equals(ASTNodes.getIntegerLiteral((Expression) visited.dimensions().get(0)))))
					&& !arguments.isEmpty()
					&& arguments.get(arguments.size() - 1) == visited
					&& binding != null
					&& binding.isVarargs()
					&& binding.getParameterTypes().length == arguments.size()
					&& binding.getParameterTypes()[arguments.size() - 1].getDimensions() == 1
					&& !fInvalidTypes.contains(binding.getParameterTypes()[arguments.size() - 1].getElementType().getName());
		}

		private ITypeBinding[] getParameterTypesForConflictingMethod(List<Expression> arguments, ArrayCreation visited) {
			ArrayInitializer initializer= visited.getInitializer();

			List<Expression> initializerExpressions;
			if (initializer != null) {
				initializerExpressions= initializer.expressions();
			} else {
				initializerExpressions= Collections.EMPTY_LIST;
			}

			return Stream.concat(arguments.stream().limit(arguments.size() - 1), initializerExpressions.stream()).map(Expression::resolveTypeBinding).toArray(ITypeBinding[]::new);
		}
	}

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit, boolean removeUnnecessaryArrayCreation) {
		if (!JavaModelUtil.is50OrHigher(compilationUnit.getJavaElement().getJavaProject()))
			return null;

		if (!removeUnnecessaryArrayCreation)
			return null;

		List<CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation> operations= new ArrayList<>();
		UnnecessaryArrayCreationFix.UnnecessaryArrayCreationFinder finder= new UnnecessaryArrayCreationFix.UnnecessaryArrayCreationFinder(removeUnnecessaryArrayCreation, operations);
		compilationUnit.accept(finder);

		if (operations.isEmpty())
			return null;

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[operations.size()]);
		return new CleanUpFixWrapper(new ConvertLoopFixCore(FixMessages.ControlStatementsFix_change_name, compilationUnit, ops, null));
	}

	public static UnnecessaryArrayCreationFix createUnnecessaryArrayCreationFix(CompilationUnit compilationUnit, Expression methodInvocation) {
		if (!JavaModelUtil.is50OrHigher(compilationUnit.getJavaElement().getJavaProject()))
			return null;

		List<CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation> operations= new ArrayList<>();
		UnnecessaryArrayCreationFix.UnnecessaryArrayCreationFinder finder= new UnnecessaryArrayCreationFix.UnnecessaryArrayCreationFinder(true, operations);
		methodInvocation.accept(finder);

		if (operations.isEmpty())
			return null;

		return new UnnecessaryArrayCreationFix(FixMessages.Java50Fix_RemoveUnnecessaryArrayCreation_description, compilationUnit, new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] {operations.get(0)}, null);
	}

	private final IStatus fStatus;

	protected UnnecessaryArrayCreationFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations, IStatus status) {
		super(name, compilationUnit, fixRewriteOperations);
		fStatus= status;
	}

	@Override
	public IStatus getStatus() {
		return fStatus;
	}

}
