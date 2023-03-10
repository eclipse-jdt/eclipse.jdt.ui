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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;

/**
 * Represents a transition from Type A to Type B by some chain element ( {@link IField} access,
 * {@link IMethod} call, or {@link ILocalVariable} (as entrypoints only)).
 *
 * @see ChainFinder
 */
public class ChainElement {

	public enum ElementType {
		METHOD, FIELD, LOCAL_VARIABLE, TYPE
	}

	private final IJavaElement element;

	private ChainType returnType;

	private int dimension;

	private ElementType elementType;

	private final boolean requireThis;

	public ChainElement(final IJavaElement element, final boolean requireThis) {
		if (element == null) {
			throw new IllegalArgumentException("???"); //$NON-NLS-1$
		}
		this.element= element;
		this.requireThis= requireThis;
		initializeReturnType();
	}

	private void initializeReturnType() {
		String signature= null;
		IJavaProject proj= element.getJavaProject();
		IType declType;
		switch (element.getElementType()) {
			case IJavaElement.FIELD:
				elementType= ElementType.FIELD;
				try {
					signature= ((IField)element).getTypeSignature();
				} catch (JavaModelException e) {
					// ignore
				}
				declType= ((IField)element).getDeclaringType();
				setReturnType(proj, signature, declType);
				break;
			case IJavaElement.LOCAL_VARIABLE:
				elementType= ElementType.LOCAL_VARIABLE;
				signature= ((ILocalVariable)element).getTypeSignature();
				declType= ((ILocalVariable)element).getDeclaringMember().getDeclaringType();
				setReturnType(proj, signature, declType);
				break;
			case IJavaElement.METHOD:
				elementType= ElementType.METHOD;
				try {
					signature= ((IMethod)element).getReturnType();
				} catch (JavaModelException e) {
					// ignore
				}
				declType= ((IMethod)element).getDeclaringType();
				setReturnType(proj, signature, declType);
				break;
			case IJavaElement.TYPE:
				elementType= ElementType.TYPE;
				returnType= new ChainType((IType) element);
				break;
			default:
				/*
				 * Other IJavaElement types may end up here that
				 * are not relevant for chain completion. Ignore
				 * these using fact that getElementType() == null
				 */
		}
		dimension= signature == null ? 0 : Signature.getArrayCount(signature);
	}

	private void setReturnType(IJavaProject proj, String signature, IType declType) {
		if (ChainElementAnalyzer.isPrimitive(signature)) {
			returnType= new ChainType(signature);
		} else {
			IType res= ChainElementAnalyzer.getTypeFromSignature(proj, signature, declType);
			returnType= (res != null) ? new ChainType(res) : new ChainType(signature);
		}
	}

	public IJavaElement getElement() {
		return element;
	}

	/**
	 * Returns the type of this chain element as an ElementType
	 * @return the ElementType or null, if this chain element
	 * does not support the IJavaElement that is associated.
	 */
	public ElementType getElementType() {
		return elementType;
	}

	/**
	 * Returns the return type of this chain element as a ChainType
	 * @return the ChainType or null, if this chain element
	 * does not support the IJavaElement that is associated.
	 */
	public ChainType getReturnType() {
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
			final IMethod m= (IMethod) element;
			StringBuilder ret= new StringBuilder(m.getElementName());
			try {
				return ret.append(m.getSignature()).toString();
			} catch (JavaModelException e1) {
				return ret.toString();
			}
		}
		return element.toString();
	}

	public static String createChainCode(final Chain chain, final boolean createAsTitle, final int expectedDimension) {
		final Map<String, Integer> varNames= new HashMap<>();
		StringBuilder sb= new StringBuilder(64);
		for (final ChainElement edge : chain.getElements()) {
			switch (edge.getElementType()) {
				case FIELD:
				case TYPE:
				case LOCAL_VARIABLE:
					appendVariableString(edge, sb);
					break;
				case METHOD:
					final IMethod method= (IMethod) edge.getElement();
					if (createAsTitle) {
						StringBuffer tmp= new StringBuffer(sb.toString());
						JavaElementLabelsCore.getMethodLabel(method, JavaElementLabelsCore.ALL_DEFAULT, tmp);
						sb= new StringBuilder(tmp.toString());
					} else {
						sb.append(method.getElementName());
						appendParameters(sb, method, varNames);
					}
					break;
				default:
			}
			final boolean appendVariables= !createAsTitle;
			appendArrayDimensions(sb, edge.getReturnTypeDimension(), expectedDimension, appendVariables, varNames);
			sb.append("."); //$NON-NLS-1$
		}
		deleteLastChar(sb);
		return sb.toString();
	}

	private static void appendVariableString(final ChainElement edge, final StringBuilder sb) {
		if (edge.requiresThisForQualification() && sb.length() == 0) {
			sb.append("this."); //$NON-NLS-1$
		}
		sb.append((edge.getElement()).getElementName());
	}

	private static void appendParameters(final StringBuilder sb, final IMethod method, final Map<String, Integer> varNames) {
		sb.append("("); //$NON-NLS-1$
		for (final String typeSig : method.getParameterTypes()) {
			String parameterName= Signature.getSignatureSimpleName(Signature.getElementType(typeSig));
			parameterName= parameterName.substring(0, 1).toLowerCase() + parameterName.substring(1);
			int index= parameterName.indexOf('<');
			if (index != -1) {
				parameterName= parameterName.substring(0, index);
			}
			appendTemplateVariable(sb, parameterName, varNames);
			sb.append(", "); //$NON-NLS-1$
		}
		if (method.getParameterTypes().length > 0) {
			deleteLastChar(sb);
			deleteLastChar(sb);
		}
		sb.append(")"); //$NON-NLS-1$
	}

	private static void appendTemplateVariable(final StringBuilder sb, final String varname,
			final Map<String, Integer> varNames) {
		int val= varNames.containsKey(varname) ? varNames.get(varname) : 0;
		varNames.put(varname, val + 1);
		sb.append("${").append(varname); //$NON-NLS-1$
		final int count= varNames.get(varname);
		if (count > 1) {
			sb.append(count);
		}
		sb.append("}"); //$NON-NLS-1$
	}

	private static void appendArrayDimensions(final StringBuilder sb, final int dimension, final int expectedDimension,
			final boolean appendVariables, final Map<String, Integer> varNames) {
		for (int i= dimension; i-- > expectedDimension;) {
			sb.append("["); //$NON-NLS-1$
			if (appendVariables) {
				appendTemplateVariable(sb, "i", varNames); //$NON-NLS-1$
			}
			sb.append("]"); //$NON-NLS-1$
		}
	}

	private static StringBuilder deleteLastChar(final StringBuilder sb) {
		return sb.deleteCharAt(sb.length() - 1);
	}
}
