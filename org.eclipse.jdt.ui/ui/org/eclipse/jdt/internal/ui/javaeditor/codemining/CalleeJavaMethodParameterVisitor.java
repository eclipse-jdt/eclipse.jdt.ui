/*******************************************************************************
 * Copyright (c) 2017, 2024 Angelo Zerr and others.
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
 * - Red Hat Inc.: add default method parameter filtering (https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/457)
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor.codemining;

import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;

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

import org.eclipse.jdt.ui.PreferenceConstants;

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
				if (!skipParameterNamesCodeMinings(method, parameterNames)) {
					for (int i= 0; i < Math.min(arguments.size(), parameterNames.length); i++) {
						if (!skipParameterNameCodeMining(parameterNames, arguments, i)) {
							minings.add(new JavaMethodParameterCodeMining((Expression) arguments.get(i), i, parameterNames, isVarArgs, provider));
						}
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
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		boolean filter= store.getBoolean(PreferenceConstants.EDITOR_JAVA_CODEMINING_FILTER_IMPLIED_PARAMETER_NAMES);
		if (!filter) {
			return false;
		}
		String parameterName= parameterNames[parameterIndex].toLowerCase();
		String expression= arguments.get(parameterIndex).toString().toLowerCase();
		return parameterName.length() > 3 && expression.contains(parameterName) || parameterName.equals(expression);
	}

	private boolean skipParameterNamesCodeMinings(IMethod method) {
		return method.getNumberOfParameters() <= 1;
	}

	private boolean skipParameterNamesCodeMinings(IMethod method, String[] parameterNames) {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		boolean filter= store.getBoolean(PreferenceConstants.EDITOR_JAVA_CODEMINING_DEFAULT_FILTER_FOR_PARAMETER_NAMES);
		if (!filter) {
			return false;
		}
		// add default filtering to skip parameter names (based on original plug-in defaults)
		String typeName= method.getDeclaringType().getTypeQualifiedName();
		if (typeName.equals("Math")) { //$NON-NLS-1$
			String packageName= method.getDeclaringType().getPackageFragment().getElementName();
			if (packageName.equals("java.lang")) { //$NON-NLS-1$
				return true;
			}
		}
		if (typeName.equals("Logger")) { //$NON-NLS-1$
			String packageName= method.getDeclaringType().getPackageFragment().getElementName();
			if (packageName.equals("org.slf4j")) { //$NON-NLS-1$
				return true;
			}
		}
		String methodName= method.getElementName();
		if (methodName.equals("of")) { //$NON-NLS-1$
			if (typeName.startsWith("Set") || typeName.startsWith("ImmutableList")  //$NON-NLS-1$ //$NON-NLS-2$
					|| typeName.startsWith("ImmutableMultiset") || typeName.startsWith("ImmutableSortedMultiset") //$NON-NLS-1$ //$NON-NLS-2$
					|| typeName.startsWith("ImmutableSortedSet") || typeName.startsWith("List")) { //$NON-NLS-1$ //$NON-NLS-2$
				return true;
			}
		}
		if (methodName.equals("asList")) { //$NON-NLS-1$
			if (typeName.startsWith("Arrays")) { //$NON-NLS-1$
				return true;
			}

		}
		if (parameterNames.length != 2) {
			return false;
		}
		if (methodName.equals("set") || methodName.equals("setProperties")  //$NON-NLS-1$ //$NON-NLS-2$
				|| methodName.equals("compare")) { //$NON-NLS-1$
			return true;
		}
		if (parameterNames[0].startsWith("first")) { //$NON-NLS-1$
			return parameterNames[1].startsWith("second") || parameterNames[1].startsWith("last"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (parameterNames[1].startsWith("end")) { //$NON-NLS-1$
			return parameterNames[0].startsWith("begin") || parameterNames[0].startsWith("start"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (parameterNames[0].startsWith("from") && parameterNames[1].startsWith("to")) { //$NON-NLS-1$ //$NON-NLS-2$
			return true;
		} else if (parameterNames[0].startsWith("min") && parameterNames[1].startsWith("max")) { //$NON-NLS-1$ //$NON-NLS-2$
			return true;
		} else if (parameterNames[0].equals("format") && parameterNames[1].startsWith("arg")) { //$NON-NLS-1$ //$NON-NLS-2$
			return true;
		} else if (parameterNames[0].equals("key") && parameterNames[1].equals("value")) { //$NON-NLS-1$ //$NON-NLS-2$
			return true;
		} else if (parameterNames[0].equals("message") && parameterNames[1].equals("error")) { //$NON-NLS-1$ //$NON-NLS-2$
			return true;
		}
		return false;
	}

}
