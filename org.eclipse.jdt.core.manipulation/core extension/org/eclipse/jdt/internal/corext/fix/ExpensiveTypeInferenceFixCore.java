/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IModuleBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.code.Invocations;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

public class ExpensiveTypeInferenceFixCore extends CompilationUnitRewriteOperationsFixCore {

	public static ExpensiveTypeInferenceFixCore createFix(CompilationUnit root, IProblemLocation[] problems) {
		Set<MethodInvocation> calls= findCalls(root, problems);
		return calls.isEmpty() ? null : new ExpensiveTypeInferenceFixCore(root, calls);
	}

	public static int countFixes(CompilationUnit root, IProblemLocation[] problems) {
		return findCalls(root, problems).size();
	}

	private static Set<MethodInvocation> findCalls(CompilationUnit root, IProblemLocation[] problems) {
		Set<MethodInvocation> calls= new LinkedHashSet<>();
		for (IProblemLocation problem : problems) {
			if (problem.getProblemId() != IProblem.PotentiallyExpensiveTypeInference) {
				continue;
			}
			ASTNode node= problem.getCoveredNode(root);
			if (node instanceof SimpleName name && name.getLocationInParent() == MethodInvocation.NAME_PROPERTY
					&& name.getStartPosition() == problem.getOffset() && name.getLength() == problem.getLength()) {
				MethodInvocation call= (MethodInvocation) name.getParent();
				if (canInsertTypeArguments(call)) {
					calls.add(call);
				}
			}
		}
		return calls;
	}

	private static boolean canInsertTypeArguments(MethodInvocation call) {
		if (!call.typeArguments().isEmpty() || call.getExpression() == null) {
			return false;
		}
		ITypeBinding receiver= call.getExpression().resolveTypeBinding();
		if (receiver == null || receiver.isRecovered() || receiver.isTypeVariable()
				|| receiver.isIntersectionType() || receiver.isCapture()) {
			return false;
		}
		IMethodBinding method= call.resolveMethodBinding();
		int arity= call.arguments().size();
		if (method == null || method.isRecovered() || method.isRawMethod() || !method.isVarargs()
				|| (receiver.isRawType() && !Modifier.isStatic(method.getModifiers()))
				|| arity <= method.getParameterTypes().length) {
			return false;
		}

		// Changing a nested poly expression can select a different overload at an enclosing call.
		ASTNode expression= call;
		while (expression.getParent() instanceof ParenthesizedExpression) {
			expression= expression.getParent();
		}
		ASTNode parent= expression.getParent();
		Type target= null;
		if (parent instanceof VariableDeclarationFragment fragment) {
			target= ASTNodes.getType(fragment);
		} else if (parent instanceof ReturnStatement) {
			MethodDeclaration declaration= ASTResolving.findParentMethodDeclaration(call);
			if (declaration == null) { // includes returns from a lambda
				return false;
			}
			target= declaration.getReturnType2();
		} else if (parent instanceof Assignment assignment) {
			if (assignment.getOperator() != Assignment.Operator.ASSIGN || expression != assignment.getRightHandSide()) {
				return false;
			}
		} else if (!(parent instanceof ExpressionStatement)) {
			return false;
		}
		ITypeBinding[] inferred= method.getTypeArguments();
		ITypeBinding enclosing= ASTNodes.getEnclosingType(call);
		if (inferred.length == 0 || enclosing == null) {
			return false;
		}
		ITypeBinding[] written= target instanceof ParameterizedType && target.resolveBinding() != null
				? target.resolveBinding().getTypeArguments() : new ITypeBinding[0];
		for (int i= 0; i < inferred.length; i++) {
			boolean alreadyInScope= written.length == inferred.length && inferred[i].isEqualTo(written[i]);
			if (!isConcreteType(inferred[i], enclosing, alreadyInScope)) {
				return false;
			}
		}
		IMethodBinding declaration= method.getMethodDeclaration();
		return !hasCompetingOverload(receiver, declaration, arity)
				&& Bindings.visitHierarchy(receiver, type -> !hasCompetingOverload(type, declaration, arity));
	}

	private static boolean isConcreteType(ITypeBinding type, ITypeBinding enclosing, boolean alreadyInScope) {
		if ((!type.isClass() && !type.isInterface() && !type.isEnum() && !type.isRecord())
				|| type.isRecovered() || type.isLocal() || type.isAnonymous() || type.isRawType()
				|| type.isIntersectionType() || type.getTypeAnnotations().length != 0
				|| (!alreadyInScope && (!Modifier.isPublic(type.getModifiers()) || !isPackageAccessible(type, enclosing)))) {
			return false;
		}
		ITypeBinding owner= type.getDeclaringClass();
		if (owner != null) {
			// ImportRewrite uses the declaring type, which can differ from a visible source alias.
			ITypeBinding qualifier= Modifier.isStatic(type.getModifiers()) ? owner.getTypeDeclaration() : owner;
			if (!isConcreteType(qualifier, enclosing, false)) {
				return false;
			}
		}
		for (ITypeBinding argument : type.getTypeArguments()) {
			if (!isConcreteType(argument, enclosing, alreadyInScope)) {
				return false;
			}
		}
		return true;
	}

	private static boolean isPackageAccessible(ITypeBinding type, ITypeBinding enclosing) {
		IModuleBinding from= enclosing.getModule();
		IModuleBinding module= type.getModule();
		if (module == null || module.isEqualTo(from)) {
			return true;
		}
		boolean unnamed= from == null || from.getName().isEmpty();
		if (module.getName().isEmpty()) {
			return unnamed;
		}
		// A direct requires or java.base proves readability. Leave indirect reads to a future fix.
		if (!unnamed && !"java.base".equals(module.getName()) //$NON-NLS-1$
				&& Arrays.stream(from.getRequiredModules()).noneMatch(module::isEqualTo)) {
			return false;
		}
		for (IPackageBinding exported : module.getExportedPackages()) {
			if (exported.getName().equals(type.getPackage().getName())) {
				String[] targets= module.getExportedTo(exported);
				return targets.length == 0 || (!unnamed && Arrays.asList(targets).contains(from.getName()));
			}
		}
		return false;
	}

	private static boolean hasCompetingOverload(ITypeBinding type, IMethodBinding method, int arity) {
		if (type.isRecovered()) {
			return true;
		}
		for (IMethodBinding other : type.getDeclaredMethods()) {
			if (other.getName().equals(method.getName()) && !method.isEqualTo(other.getMethodDeclaration())) {
				int parameters= other.getParameterTypes().length;
				if (parameters == arity || (other.isVarargs() && arity >= parameters - 1)) {
					return true;
				}
			}
		}
		return false;
	}

	private ExpensiveTypeInferenceFixCore(CompilationUnit root, Set<MethodInvocation> calls) {
		super(FixMessages.ExpensiveTypeInferenceFix_description, root, new CompilationUnitRewriteOperation() {
			@Override
			public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore model) {
				TextEditGroup group= createTextEditGroup(FixMessages.ExpensiveTypeInferenceFix_description, cuRewrite);
				for (MethodInvocation call : calls) {
					ListRewrite arguments= Invocations.getInferredTypeArgumentsRewrite(cuRewrite.getASTRewrite(), call);
					for (ITypeBinding type : Invocations.getInferredTypeArguments(call)) {
						arguments.insertLast(importType(type, call, cuRewrite.getImportRewrite(), root, TypeLocation.TYPE_ARGUMENT), group);
					}
				}
			}
		});
	}
}
