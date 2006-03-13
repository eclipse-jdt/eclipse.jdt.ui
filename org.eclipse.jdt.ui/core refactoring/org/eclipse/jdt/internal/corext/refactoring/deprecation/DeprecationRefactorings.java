/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.deprecation;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import org.eclipse.jdt.internal.corext.Assert;

/**
 * Helper class for refactorings used to resolve deprecations.
 * 
 * @since 3.2
 */
public final class DeprecationRefactorings {

	/** The script prefix */
	private static final String SCRIPT_PREFIX= "DEPRECATE_"; //$NON-NLS-1$

	/**
	 * Returns the refactoring script name associated with the method binding.
	 * 
	 * @param binding
	 *            the method binding
	 * @return the refactoring script name, or <code>null</code>
	 */
	public static String getRefactoringScriptName(final IMethodBinding binding) {
		Assert.isNotNull(binding);
		final IJavaElement element= binding.getDeclaringClass().getJavaElement();
		if (element instanceof IType) {
			final IType type= (IType) element;
			final StringBuffer buffer= new StringBuffer();
			buffer.append(SCRIPT_PREFIX);
			buffer.append(type.getFullyQualifiedName());
			buffer.append('.');
			buffer.append(binding.getName());
			buffer.append('(');
			final ITypeBinding[] parameters= binding.getParameterTypes();
			for (int index= 0; index < parameters.length; index++) {
				if (index != 0)
					buffer.append(',');
				final IJavaElement javaElem= parameters[index].getJavaElement();
				if (javaElem instanceof IType)
					buffer.append(((IType) javaElem).getFullyQualifiedName());
				else if (javaElem instanceof ITypeParameter)
					buffer.append(((ITypeParameter) javaElem).getElementName());
				else
					buffer.append(parameters[index].getQualifiedName());
			}
			buffer.append(')');
			buffer.append(".xml"); //$NON-NLS-1$
			return buffer.toString();
		}
		return null;
	}

	/**
	 * Returns the refactoring script name associated with the variable binding.
	 * 
	 * @param binding
	 *            the variable binding
	 * @return the refactoring script name, or <code>null</code>
	 */
	public static String getRefactoringScriptName(final IVariableBinding binding) {
		final IJavaElement element= binding.getDeclaringClass().getJavaElement();
		if (element instanceof IType) {
			final IType type= (IType) element;
			final StringBuffer buffer= new StringBuffer();
			buffer.append(SCRIPT_PREFIX);
			buffer.append(type.getFullyQualifiedName());
			buffer.append('.');
			buffer.append(binding.getName());
			buffer.append(".xml"); //$NON-NLS-1$
			return buffer.toString();
		}
		return null;
	}

	/**
	 * Creates a new deprecation refactorings.
	 */
	private DeprecationRefactorings() {
		// Not for instantiation
	}
}
