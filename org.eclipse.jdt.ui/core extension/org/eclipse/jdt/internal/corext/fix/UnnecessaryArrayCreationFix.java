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
import java.util.Objects;
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
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
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
				List<Expression> expressions= initializer.expressions();
				ITypeBinding singleElement= expressions.get(0).resolveTypeBinding();
				NullLiteral nullLiteral= ASTNodes.as(expressions.get(0), NullLiteral.class);

				if (nullLiteral != null
						|| singleElement == null
						|| singleElement.isArray()) {
					return true;
				}
			}

			ASTNode parent= node.getParent();

			if (parent instanceof ClassInstanceCreation) {
				ClassInstanceCreation cic= (ClassInstanceCreation) parent;

				if (canArrayBeRemoved(node, cic.arguments(), cic.resolveConstructorBinding())) {
					fResult.add(new UnwrapNewArrayOperation(node, cic));
				}
			} else if (parent instanceof MethodInvocation) {
				MethodInvocation m= (MethodInvocation) parent;

				if (canArrayBeRemoved(node, m.arguments(), m.resolveMethodBinding())) {
					fResult.add(new UnwrapNewArrayOperation(node, m));
				}
			} else if (parent instanceof SuperMethodInvocation) {
				SuperMethodInvocation sm= (SuperMethodInvocation) parent;

				if (canArrayBeRemoved(node, sm.arguments(), sm.resolveMethodBinding())) {
					fResult.add(new UnwrapNewArrayOperation(node, sm));
				}
			}

			return true;
		}

		private boolean canArrayBeRemoved(ArrayCreation node, List<Expression> arguments, IMethodBinding binding) {
			return isUselessArrayCreation(node, arguments, binding)
					&& !hasEquivalentMethod(node, arguments, binding);
		}

		private boolean isUselessArrayCreation(ArrayCreation node, List<Expression> arguments, IMethodBinding binding) {
			return (node.getInitializer() != null || (node.dimensions().size() == 1 && Long.valueOf(0L).equals(ASTNodes.getIntegerLiteral((Expression) node.dimensions().get(0)))))
					&& !arguments.isEmpty()
					&& arguments.get(arguments.size() - 1) == node
					&& binding != null
					&& binding.isVarargs()
					&& binding.getParameterTypes().length == arguments.size()
					&& binding.getParameterTypes()[arguments.size() - 1].getDimensions() == 1
					&& !fInvalidTypes.contains(binding.getParameterTypes()[arguments.size() - 1].getElementType().getName());
		}

		private boolean hasEquivalentMethod(ArrayCreation node, List<Expression> arguments, IMethodBinding binding) {
			TypeDeclaration typeDeclaration= ASTNodes.getTypedAncestor(node, TypeDeclaration.class);

			if (typeDeclaration == null) {
				return true;
			}

			ITypeBinding type= typeDeclaration.resolveBinding();

			if (type == null) {
				return true;
			}

			boolean inSameClass= true;
			ASTNode parent= node.getParent();
			ITypeBinding[] parameterTypesForConflictingMethod= getParameterTypesForConflictingMethod(arguments, node);

			// Figure out the type where we need to start looking at methods in the hierarchy.
			// If we have a new class instance or super method call or this expression or
			// we have a static call that is qualified, we use the referenced class as the starting point.
			// If we have a non-qualified method call, we use the class containing the call.
			// Otherwise, we bail on the clean-up.
			if (parent instanceof ClassInstanceCreation) {
				type= ((ClassInstanceCreation) parent).resolveTypeBinding();
				inSameClass= type.isNested();
			} else if (parent instanceof MethodInvocation) {
				MethodInvocation methodInvocation= (MethodInvocation) parent;
				Expression expression= methodInvocation.getExpression();

				if (expression != null) {
					if (!(expression instanceof ThisExpression)) {
						inSameClass= binding.getDeclaringClass().isEqualTo(type);
						type= expression.resolveTypeBinding();
					}
				} else {
					ASTNode root= node.getRoot();

					if (root instanceof CompilationUnit) {
						CompilationUnit compilationUnit= (CompilationUnit) root;
						List<ImportDeclaration> imports= compilationUnit.imports();
						String localPackage= null;

						if (compilationUnit.getPackage() != null && compilationUnit.getPackage().getName() != null) {
							localPackage= compilationUnit.getPackage().getName().getFullyQualifiedName();
						}

						for (ImportDeclaration oneImport : imports) {
							if (oneImport.isStatic()
									&& !oneImport.isOnDemand()
									&& oneImport.getName() instanceof QualifiedName) {
								QualifiedName methodName= (QualifiedName) oneImport.getName();
								String methodIdentifier= methodName.getName().getIdentifier();
								ITypeBinding conflictingType= methodName.getQualifier().resolveTypeBinding();

								if (conflictingType == null) {
									return true; // Error on side of caution
								}

								String importPackage= null;

								if (conflictingType.getPackage() != null) {
									importPackage= conflictingType.getPackage().getName();
								}

								boolean inSamePackage= Objects.equals(localPackage, importPackage);

								for (IMethodBinding declaredMethod : conflictingType.getDeclaredMethods()) {
									if (methodIdentifier.equals(declaredMethod.getName())
											&& isMethodMatching(parameterTypesForConflictingMethod, binding, false, inSamePackage, declaredMethod)) {
										return true;
									}
								}
							}
						}
					}

					if (Modifier.isStatic(binding.getModifiers())) {
						inSameClass= binding.getDeclaringClass().isEqualTo(type);
						type= binding.getDeclaringClass();
					}
				}
			} else if (parent instanceof SuperMethodInvocation) {
				inSameClass= type.isNested();
				type= type.getSuperclass();
			} else {
				return true; // Error on side of caution
			}

			if (type == null) {
				return true;
			}

			return hasEquivalentMethodForInheritedTypes(parameterTypesForConflictingMethod, binding, type, type, inSameClass);
		}

		private ITypeBinding[] getParameterTypesForConflictingMethod(List<Expression> arguments, ArrayCreation node) {
			ArrayInitializer initializer= node.getInitializer();

			List<Expression> initializerExpressions;
			if (initializer != null) {
				initializerExpressions= initializer.expressions();
			} else {
				initializerExpressions= Collections.EMPTY_LIST;
			}

			ITypeBinding[] parameterTypesForConflictingMethod= new ITypeBinding[arguments.size() - 1 + initializerExpressions.size()];

			for (int i= 0; i < arguments.size() - 1; i++) {
				parameterTypesForConflictingMethod[i]= arguments.get(i).resolveTypeBinding();
			}

			for (int i= 0; i < initializerExpressions.size(); i++) {
				parameterTypesForConflictingMethod[arguments.size() - 1 + i]= initializerExpressions.get(i).resolveTypeBinding();
			}

			return parameterTypesForConflictingMethod;
		}

		private boolean hasEquivalentMethodForInheritedTypes(ITypeBinding[] parameterTypesForConflictingMethod, IMethodBinding binding, ITypeBinding type,
				ITypeBinding origType, boolean inSameClass) {
			while (type != null) {
				IPackageBinding packageBinding= type.getPackage();
				boolean inSamePackage= packageBinding.isEqualTo(origType.getPackage());

				if (hasEquivalentMethodForOneType(parameterTypesForConflictingMethod, binding, type, inSameClass, inSamePackage)) {
					return true;
				}

				if (type.isNested()) {
					if (hasEquivalentMethodForInheritedTypes(parameterTypesForConflictingMethod, binding, type.getDeclaringClass(), origType, inSameClass)) {
						return true;
					}

					type= type.getSuperclass();
					inSameClass&= type.isNested();
				} else {
					type= type.getSuperclass();
					inSameClass= false;
				}
			}

			return false;
		}

		private boolean hasEquivalentMethodForOneType(ITypeBinding[] parameterTypesForConflictingMethod, IMethodBinding binding,
				ITypeBinding type, boolean inSameClass, boolean inSamePackage) {
			for (IMethodBinding method : type.getDeclaredMethods()) {
				if (isMethodMatching(parameterTypesForConflictingMethod, binding, inSameClass, inSamePackage, method)) {
					return true;
				}
			}

			return false;
		}

		private boolean isMethodMatching(ITypeBinding[] parameterTypesForConflictingMethod, IMethodBinding binding, boolean inSameClass, boolean inSamePackage, IMethodBinding testedMethod) {
			int methodModifiers= testedMethod.getModifiers();
			ITypeBinding[] parameterTypes= testedMethod.getParameterTypes();

			if (!binding.isEqualTo(testedMethod)
					&& parameterTypesForConflictingMethod.length == parameterTypes.length
					&& binding.getName().equals(testedMethod.getName())
					&& (inSameClass || Modifier.isPublic(methodModifiers) || Modifier.isProtected(methodModifiers)
							|| (inSamePackage && !Modifier.isPrivate(methodModifiers)))) {
				for (int i= 0; i < parameterTypesForConflictingMethod.length; i++) {
					if (parameterTypesForConflictingMethod[i] == null || parameterTypes[i] == null) {
						return true;
					}

					if (!parameterTypesForConflictingMethod[i].isAssignmentCompatible(parameterTypes[i])) {
						return false;
					}
				}

				return true;
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
