/*******************************************************************************
 * Copyright (c) 2017 Angelo Zerr and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Angelo Zerr: initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor.codemining;

import java.util.List;

import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;

import org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class CalleeJavaMethodParameterVisitor extends HierarchicalASTVisitor {

	private final List<ICodeMining> minings;

	private final ICodeMiningProvider provider;

	public CalleeJavaMethodParameterVisitor(List<ICodeMining> minings, ICodeMiningProvider provider) {
		this.minings= minings;
		this.provider= provider;
	}

	@Override
	public boolean visit(ConstructorInvocation constructorInvocation) {
		List<?> arguments= constructorInvocation.arguments();
		if (!arguments.isEmpty()) {
			IMethodBinding constructorBinding= constructorInvocation.resolveConstructorBinding();
			collectParameterNameCodeMinings(constructorBinding, arguments);
		}
		return super.visit(constructorInvocation);
	}

	@Override
	public boolean visit(ClassInstanceCreation classInstanceCreation) {
		List<?> arguments= classInstanceCreation.arguments();
		if (!arguments.isEmpty()) {
			IMethodBinding constructorBinding= classInstanceCreation.resolveConstructorBinding();
			collectParameterNameCodeMinings(constructorBinding, arguments);
		}
		return super.visit(classInstanceCreation);
	}

	@Override
	public boolean visit(SuperConstructorInvocation superConstructorInvocation) {
		List<?> arguments= superConstructorInvocation.arguments();
		if (!arguments.isEmpty()) {
			IMethodBinding constructorBinding= superConstructorInvocation.resolveConstructorBinding();
			collectParameterNameCodeMinings(constructorBinding, arguments);
		}
		return super.visit(superConstructorInvocation);
	}

	@Override
	public boolean visit(EnumConstantDeclaration enumConstantDeclaration) {
		List<?> arguments= enumConstantDeclaration.arguments();
		if (!arguments.isEmpty()) {
			IMethodBinding constructorBinding= enumConstantDeclaration.resolveConstructorBinding();
			collectParameterNameCodeMinings(constructorBinding, arguments);
		}
		return super.visit(enumConstantDeclaration);
	}

	@Override
	public boolean visit(MethodInvocation methodInvocation) {
		List<?> arguments= methodInvocation.arguments();
		if (!arguments.isEmpty()) {
			IMethodBinding methodBinding= methodInvocation.resolveMethodBinding();
			collectParameterNameCodeMinings(methodBinding, arguments);
		}
		return super.visit(methodInvocation);
	}

	protected void collectParameterNameCodeMinings(IMethodBinding mBinding, List<?> arguments) {
		if (mBinding != null) {
			boolean isVarArgs = mBinding.isVarargs();
			IMethod method= resolveMethodBinding(mBinding);
			if (method != null) {
				collectParameterNamesCodeMinings(method, arguments, isVarArgs);
			} else {
				collectParameterNamesCodeMinings(mBinding, arguments, isVarArgs);
			}
		}
	}

	protected void collectParameterNamesCodeMinings(IMethod method, List<?> arguments, boolean isVarArgs) {
		if (!skipParameterNamesCodeMinings(method)) {
			try {
				String[] parameterNames= method.getParameterNames();
				for (int i= 0; i < Math.min(arguments.size(), parameterNames.length); i++) {
					if (!skipParameterNameCodeMining(parameterNames, arguments, i)) {
						minings.add(new JavaMethodParameterCodeMining((Expression) arguments.get(i), i, parameterNames, isVarArgs, provider));
					}
				}
			} catch (Exception e) {
				JavaPlugin.log(e);
			}
		}
	}

	protected void collectParameterNamesCodeMinings(IMethodBinding mbinding, List<?> arguments, boolean isVarArgs) {
		// synthetic method of a record
		if (mbinding.getDeclaringClass().isRecord()) {
			String[] parameterNames= mbinding.getParameterNames();
			for (int i= 0; i < Math.min(arguments.size(), parameterNames.length); i++) {
				if (!skipParameterNameCodeMining(parameterNames, arguments, i)) {
					minings.add(new JavaMethodParameterCodeMining((Expression) arguments.get(i), i, parameterNames, isVarArgs, provider));
				}
			}
		}
	}

	protected static IMethod resolveMethodBinding(IMethodBinding binding) {
		if (binding == null) {
			return null;
		}
		IJavaElement javaElement= binding.getJavaElement();
		if (javaElement == null || !(javaElement instanceof IMethod)) {
			return null;
		}
		return (IMethod) javaElement;
	}

	private boolean skipParameterNameCodeMining(String[] parameterNames, List<?> arguments, int parameterIndex) {
		if (parameterNames.length < parameterIndex) {
			return true;
		}
		String parameterName= parameterNames[parameterIndex].toLowerCase();
		String expression= arguments.get(parameterIndex).toString().toLowerCase();
		return expression.contains(parameterName);
	}

	private boolean skipParameterNamesCodeMinings(IMethod method) {
		return method.getNumberOfParameters() <= 1;
	}

}
