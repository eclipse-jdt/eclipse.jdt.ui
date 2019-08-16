/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
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
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

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
 * A fix that removes unnecessary array creation for a varargs parameter of a method or super method invocation.
 */
public class UnnecessaryArrayCreationCleanUp extends AbstractMultiFix {

	public UnnecessaryArrayCreationCleanUp() {
		this(Collections.emptyMap());
	}

	public UnnecessaryArrayCreationCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);
		Map<String, String> requiredOptions= null;
		return new CleanUpRequirements(requireAST, false, false, requiredOptions);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION)) {
			return new String[] { MultiFixMessages.UnnecessaryArrayCreationCleanup_description };
		}
		return new String[0];
	}

	@SuppressWarnings("nls")
	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();
		bld.append("\n");
		bld.append("public class Foo {\n");
		bld.append("    public static void bar() {\n");
		if (isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION)) {
			bld.append("        List k= ArrayList.asList(\"a\", \"b\", \"c\");\n");
		} else {
			bld.append("        List k= ArrayList.asList(new String[] {\"a\", \"b\", \"c\"});\n");
		}
		bld.append("    }\n");
		bld.append("}\n");

		return bld.toString();
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION) || !JavaModelUtil.is50OrHigher(unit.getJavaElement().getJavaProject())) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@SuppressWarnings("rawtypes")
			@Override
			public boolean visit(ArrayCreation node) {
				ASTNode parent= node.getParent();
				if (parent instanceof MethodInvocation) {
					MethodInvocation m= (MethodInvocation)parent;
					List arguments= m.arguments();
					if (arguments.size() > 0 && arguments.get(arguments.size() - 1) == node) {
						IMethodBinding binding= m.resolveMethodBinding();
						if (binding != null && binding.isVarargs() && binding.getParameterTypes().length == arguments.size()) {
							rewriteOperations.add(new UnwrapNewArrayOperation(node, m));
						}
					}
				} else if (parent instanceof SuperMethodInvocation) {
					SuperMethodInvocation m= (SuperMethodInvocation)parent;
					List arguments= m.arguments();
					if (arguments.size() > 0 && arguments.get(arguments.size() - 1) == node) {
						IMethodBinding binding= m.resolveMethodBinding();
						if (binding != null && binding.isVarargs() && binding.getParameterTypes().length == arguments.size()) {
							rewriteOperations.add(new UnwrapNewArrayOperation(node, m));
						}
					}
				}
				return true;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.UnnecessaryArrayCreationCleanup_description, unit,
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

	/**
	 * Unwrap a new array with initializer used as input for varargs and replace with initializer elements
	 *
	 */
	private static class UnwrapNewArrayOperation extends CompilationUnitRewriteOperation {
		private final ArrayCreation node;

		private final ASTNode call;

		public UnwrapNewArrayOperation(ArrayCreation node, MethodInvocation method) {
			this.node= node;
			this.call= method;
		}

		public UnwrapNewArrayOperation(ArrayCreation node, SuperMethodInvocation superMethod) {
			this.node= node;
			this.call= superMethod;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			if (call instanceof MethodInvocation) {
				MethodInvocation method= (MethodInvocation)call;
				MethodInvocation newMethod= ast.newMethodInvocation();
				newMethod.setSourceRange(method.getStartPosition(), method.getLength());
				newMethod.setName(ast.newSimpleName(method.getName().getFullyQualifiedName()));
				newMethod.setExpression((Expression) ASTNode.copySubtree(ast, method.getExpression()));
				if (method.typeArguments() != null) {
					newMethod.typeArguments().addAll(ASTNode.copySubtrees(ast, method.typeArguments()));
				}
				for (int i= 0; i < method.arguments().size() - 1; ++i) {
					newMethod.arguments().add(ASTNode.copySubtree(ast, (Expression)method.arguments().get(i)));
				}
				ArrayInitializer initializer= node.getInitializer();
				if (initializer != null && initializer.expressions() != null) {
					for (Object exp : initializer.expressions()) {
						newMethod.arguments().add(ASTNode.copySubtree(ast, (Expression)exp));
					}
				}
				rewrite.replace(method, newMethod, null);
			} else if (call instanceof SuperMethodInvocation) {
				SuperMethodInvocation method= (SuperMethodInvocation)call;
				SuperMethodInvocation newSuperMethod= ast.newSuperMethodInvocation();
				newSuperMethod.setSourceRange(method.getStartPosition(), method.getLength());
				newSuperMethod.setName(ast.newSimpleName(method.getName().getFullyQualifiedName()));
				if (method.typeArguments() != null) {
					newSuperMethod.typeArguments().addAll(ASTNode.copySubtrees(ast, method.typeArguments()));
				}
				for (int i= 0; i < method.arguments().size() - 1; ++i) {
					newSuperMethod.arguments().add(ASTNode.copySubtree(ast, (Expression)method.arguments().get(i)));
				}
				ArrayInitializer initializer= node.getInitializer();
				if (initializer != null && initializer.expressions() != null) {
					for (Object exp : initializer.expressions()) {
						newSuperMethod.arguments().add(ASTNode.copySubtree(ast, (Expression)exp));
					}
				}
				rewrite.replace(method, newSuperMethod, null);

			}
		}
	}
}
