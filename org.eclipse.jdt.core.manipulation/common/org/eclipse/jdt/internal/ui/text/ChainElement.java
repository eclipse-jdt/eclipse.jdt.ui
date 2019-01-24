/**
 * Copyright (c) 2010, 2019 Darmstadt University of Technology and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.jdt.internal.ui.text;

import org.eclipse.osgi.util.NLS;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;

/**
 * Represents a transition from Type A to Type B by some chain element ( {@link IField} access,
 * {@link IMethod} call, or {@link ILocalVariable} (as entrypoints only)).
 *
 * @see ChainFinder
 */
public class ChainElement {

	public enum ElementType {
		METHOD, FIELD, LOCAL_VARIABLE
	}

	private final IBinding element;

	private ITypeBinding returnType;

	private int dimension;

	private ElementType elementType;

	private final boolean requireThis;

	public ChainElement(final IBinding binding, final boolean requireThis) {
		if (binding == null) {
			throw new IllegalArgumentException("???"); //$NON-NLS-1$
		}
		element= binding;
		this.requireThis= requireThis;
		initializeReturnType();
	}

	private void initializeReturnType() {
		switch (element.getKind()) {
			case IBinding.VARIABLE:
				IVariableBinding tmp= ((IVariableBinding) element);
				returnType= tmp.getType();
				if (tmp.isField()) {
					elementType= ElementType.FIELD;
				} else {
					elementType= ElementType.LOCAL_VARIABLE;
				}
				break;
			case IBinding.METHOD:
				returnType= ((IMethodBinding) element).getReturnType();
				elementType= ElementType.METHOD;
				break;
			case IBinding.TYPE:
				returnType= ((ITypeBinding) element);
				elementType= ElementType.FIELD;
				break;
			default:
				JavaManipulationPlugin.logErrorMessage(NLS.bind("Cannot handle {0} as return type.", element));
		}
		dimension= returnType.getDimensions();
	}

	@SuppressWarnings("unchecked")
	public <T extends IBinding> T getElementBinding() {
		return (T) element;
	}

	public ElementType getElementType() {
		return elementType;
	}

	public ITypeBinding getReturnType() {
		return returnType;
	}

	public int getReturnTypeDimension() {
		return dimension;
	}

	public boolean requiresThisForQualification() {
		return requireThis;
	}

	@Override
	public int hashCode() {
		return element.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof ChainElement) {
			final ChainElement other= (ChainElement) obj;
			return element.equals(other.element);
		}
		return false;
	}

	@Override
	public String toString() {
		if (elementType == ElementType.METHOD) {
			final IMethodBinding m= (IMethodBinding) element;
			IJavaElement e= m.getJavaElement();
			StringBuilder ret= new StringBuilder(m.getName());
			if (e instanceof IMethod) {
				try {
					return ret.append(((IMethod) e).getSignature()).toString();
				} catch (JavaModelException e1) {
					return ret.toString();
				}
			}
		}
		return element.toString();
	}
}
