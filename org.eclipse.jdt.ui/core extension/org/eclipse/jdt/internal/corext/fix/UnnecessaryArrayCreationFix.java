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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class UnnecessaryArrayCreationFix extends CompilationUnitRewriteOperationsFix {

	public final static class UnnecessaryArrayCreationFinder extends GenericVisitor {
		private final List<CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation> fResult;
		private final boolean fRemoveUnnecessaryArrayCreation;
		private final Set<String> fInvalidTypes= new HashSet<>(Arrays.asList("byte", "char", "short")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		public UnnecessaryArrayCreationFinder(boolean removeUnnecessaryArrayCreation, List<CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation> resultingCollection) {
			fRemoveUnnecessaryArrayCreation= removeUnnecessaryArrayCreation;
			fResult= resultingCollection;
		}

		@Override
		public boolean visit(ArrayCreation node) {
			if (!fRemoveUnnecessaryArrayCreation
					|| node.getType().getDimensions() != 1) {
				return true;
			}

			ArrayInitializer initializer= node.getInitializer();

			if (initializer != null
					&& initializer.expressions() != null
					&& initializer.expressions().size() == 1) {
				Expression singleElement= (Expression) initializer.expressions().get(0);
				NullLiteral nullLiteral= ASTNodes.as(singleElement, NullLiteral.class);

				if (nullLiteral != null
						|| singleElement.resolveTypeBinding() == null || singleElement.resolveTypeBinding().isArray()) {
					return true;
				}
			}

			ASTNode parent= node.getParent();

			if (parent instanceof ClassInstanceCreation) {
				ClassInstanceCreation cic= (ClassInstanceCreation) parent;

				if (isUselessArrayCreation(node, cic.arguments(), cic.resolveConstructorBinding()) &&
						!hasEquivalentMethod(node, cic.arguments(), cic.resolveConstructorBinding())) {
					fResult.add(new UnwrapNewArrayOperation(node, cic));
				}
			} else if (parent instanceof MethodInvocation) {
				MethodInvocation m= (MethodInvocation) parent;

				if (isUselessArrayCreation(node, m.arguments(), m.resolveMethodBinding()) &&
						!hasEquivalentMethod(node, m.arguments(), m.resolveMethodBinding())) {
					fResult.add(new UnwrapNewArrayOperation(node, m));
				}
			} else if (parent instanceof SuperMethodInvocation) {
				SuperMethodInvocation sm= (SuperMethodInvocation) parent;

				if (isUselessArrayCreation(node, sm.arguments(), sm.resolveMethodBinding()) &&
						!hasEquivalentMethod(node, sm.arguments(), sm.resolveMethodBinding())) {
					fResult.add(new UnwrapNewArrayOperation(node, sm));
				}
			}

			return true;
		}

		private boolean isUselessArrayCreation(ArrayCreation node, List<Expression> arguments, IMethodBinding binding) {
			return (node.getInitializer() != null || (node.dimensions().size() == 1 && Long.valueOf(0).equals(ASTNodes.getIntegerLiteral((Expression) node.dimensions().get(0)))))
					&& arguments.size() > 0
					&& arguments.get(arguments.size() - 1) == node
					&& binding != null
					&& binding.isVarargs()
					&& binding.getParameterTypes().length == arguments.size()
					&& binding.getParameterTypes()[arguments.size() - 1].getDimensions() == 1
					&& !fInvalidTypes.contains(binding.getParameterTypes()[arguments.size() - 1].getElementType().getName());
		}

		private boolean hasEquivalentMethod(ArrayCreation node, List<Expression> arguments, IMethodBinding binding) {
			ITypeBinding type= binding.getDeclaringClass();
			TypeDeclaration typeDeclaration= (TypeDeclaration)ASTNodes.getFirstAncestorOrNull(node, TypeDeclaration.class);
			ITypeBinding callerBinding= null;
			boolean inSameClass= false;
			boolean inSamePackage= false;
			if (typeDeclaration != null) {
				callerBinding= typeDeclaration.resolveBinding();
			}
			if (callerBinding == null || type.isEqualTo(callerBinding)) {
				inSameClass= true;
			}

			while (type != null) {
				IPackageBinding packageBinding= type.getPackage();
				inSamePackage= callerBinding == null || packageBinding.isEqualTo(callerBinding.getPackage());
				if (hasEquivalentMethodForType(node, arguments, binding, type, inSameClass, inSamePackage)) {
					return true;
				}
				type= type.getSuperclass();
				inSameClass= false;
			}
			return false;
		}

		private boolean hasEquivalentMethodForType(ArrayCreation node, List<Expression> arguments, IMethodBinding binding,
				ITypeBinding type, boolean inSameClass, boolean inSamePackage) {
			for (IMethodBinding method : type.getDeclaredMethods()) {
				if (!binding.isEqualTo(method) && binding.getName().equals(method.getName()) &&
						((inSameClass || (method.getModifiers() & (Modifier.PUBLIC | Modifier.PROTECTED)) != 0)
						|| (inSamePackage && (method.getModifiers() & Modifier.PRIVATE) == 0))) {
					ITypeBinding[] typeArguments= method.getParameterTypes();
					ArrayInitializer initializer= node.getInitializer();

					if ((initializer == null ? 0 : initializer.expressions().size()) + arguments.size() - 1 == typeArguments.length) {
						boolean match= true;

						for (int i= 0; i < arguments.size() - 1; ++i) {
							ITypeBinding argBinding= arguments.get(i).resolveTypeBinding();

							if (argBinding == null) {
								return true;
							}

							if (!argBinding.isAssignmentCompatible(typeArguments[i])) {
								match= false;
								break;
							}
						}

						if (match) {
							for (int i= arguments.size() - 1, j= 0; i < typeArguments.length; ++i, ++j) {
								Expression exp= (Expression) initializer.expressions().get(j);
								ITypeBinding expBinding= exp.resolveTypeBinding();

								if (expBinding != null && !expBinding.isAssignmentCompatible(typeArguments[i])) {
									match= false;
									break;
								}
							}
						}

						if (match) {
							return true;
						}
					}
				}
			}

			return false;
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
		return new ConvertLoopFix(FixMessages.ControlStatementsFix_change_name, compilationUnit, ops, null);
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
