/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.typeconstraints;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ITypeBinding;

public class PrettySignatures {
	
	public static String get(ITypeBinding binding) {
		if (binding.isTypeVariable()) {
			return getTypeVariable(binding);
		}
		return getPlain(binding);	
	}

	private static String getPlain(ITypeBinding binding) {
		if (binding.isPrimitive()) {
			return binding.getName();
		} else if (binding.isArray()) {
			return getPlainArrayType(binding);
		} else if (binding.isRawType()) {
			return getPlainRawType(binding);
		} else if (binding.isGenericType()) {
			return getPlainGenericType(binding);
		} else if (binding.isParameterizedType()) {
			return getPlainParameterizedType(binding);
		} else if (binding.isTypeVariable()) {
			return getPlainTypeVariable(binding);
		} else if (binding.isWildcardType()) {
			if (binding.isUpperbound()) {
				return getPlainExtendsWildCardType(binding);
			} else {
				return getPlainSuperWildCardType(binding);
			}
		}
		return getPlainStandardType(binding);
	}

	private static String getPlainSuperWildCardType(ITypeBinding binding) {
		StringBuffer result= new StringBuffer("?");
		ITypeBinding bound= binding.getBound();
		if (bound != null) {
			result.append(" super ");
			result.append(PrettySignatures.getPlain(bound));
		}
		return result.toString();
	}

	private static String getPlainExtendsWildCardType(ITypeBinding binding) {
		StringBuffer result= new StringBuffer("?");
		ITypeBinding bound= binding.getBound();
		if (bound != null) {
			result.append(" extends ");
			result.append(PrettySignatures.getPlain(bound));
		}
		return result.toString();
	}

	private static String getPlainParameterizedType(ITypeBinding binding) {
		StringBuffer result= new StringBuffer(getQualifiedName(binding));
		ITypeBinding[] typeArguments= binding.getTypeArguments();
		result.append("<"); //$NON-NLS-1$
		result.append(PrettySignatures.getPlain(typeArguments[0]));
		for (int i= 1; i < typeArguments.length; i++) {
			result.append(", "); //$NON-NLS-1$
			result.append(PrettySignatures.getPlain(typeArguments[i]));
		}
		result.append(">"); //$NON-NLS-1$
		return result.toString();
	}

	private static String getPlainGenericType(ITypeBinding binding) {
		StringBuffer result= new StringBuffer(getQualifiedName(binding));
		ITypeBinding[] typeParameters= binding.getTypeParameters();
		result.append("<"); //$NON-NLS-1$
		result.append(PrettySignatures.get(typeParameters[0]));
		for (int i= 1; i < typeParameters.length; i++) {
			result.append(", "); //$NON-NLS-1$
			result.append(PrettySignatures.get(typeParameters[i]));
		}
		result.append(">"); //$NON-NLS-1$
		return result.toString();
	}

	private static String getTypeVariable(ITypeBinding binding) {
		StringBuffer result= new StringBuffer(binding.getName());
		ITypeBinding[] bounds= binding.getTypeBounds();
		if (bounds.length > 0) {
			result.append(" extends "); //$NON-NLS-1$
			result.append(PrettySignatures.getPlain(bounds[0]));
			for (int i= 1; i < bounds.length; i++) {
				result.append(" & "); //$NON-NLS-1$
				result.append(PrettySignatures.getPlain(bounds[i]));
			}
		}
		return result.toString();
	}

	private static String getPlainTypeVariable(ITypeBinding binding) {
		return binding.getName();
	}

	private static String getPlainRawType(ITypeBinding binding) {
		return getQualifiedName(binding);
	}

	private static String getPlainArrayType(ITypeBinding binding) {
		StringBuffer result= new StringBuffer(PrettySignatures.getPlain(binding.getElementType()));
		for (int i= 0; i < binding.getDimensions(); i++) {
			result.append("[]");
		}
		return result.toString();
	}

	private static String getPlainStandardType(ITypeBinding binding) {
		return getQualifiedName(binding);
	}

	private static String getQualifiedName(ITypeBinding binding) {
		if (binding.isLocal())
			return ((IType)binding.getJavaElement()).getFullyQualifiedName('.');
		String result= binding.getQualifiedName();
		if (binding.isParameterizedType()) {
			int index= result.indexOf('<');
			if (index != -1)
				return result.substring(0, index);
		}
		return result;
	}
}
