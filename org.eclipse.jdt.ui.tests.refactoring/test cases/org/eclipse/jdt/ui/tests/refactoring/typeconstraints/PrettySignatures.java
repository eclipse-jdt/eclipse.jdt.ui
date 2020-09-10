/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.typeconstraints;

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
		} else if (binding.isCapture()) {
			return getPlainCaptureType(binding);
		}
		return getPlainStandardType(binding);
	}

	private static String getPlainCaptureType(ITypeBinding binding) {
		StringBuilder result= new StringBuilder("capture-of ");
		result.append(PrettySignatures.getPlain(binding.getWildcard()));
		return result.toString();
	}

	private static String getPlainSuperWildCardType(ITypeBinding binding) {
		StringBuilder result= new StringBuilder("?");
		ITypeBinding bound= binding.getBound();
		if (bound != null) {
			result.append(" super ");
			result.append(PrettySignatures.getPlain(bound));
		}
		return result.toString();
	}

	private static String getPlainExtendsWildCardType(ITypeBinding binding) {
		StringBuilder result= new StringBuilder("?");
		ITypeBinding bound= binding.getBound();
		if (bound != null) {
			result.append(" extends ");
			result.append(PrettySignatures.getPlain(bound));
		}
		return result.toString();
	}

	private static String getPlainParameterizedType(ITypeBinding binding) {
		StringBuilder result= new StringBuilder(getQualifiedName(binding));
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
		StringBuilder result= new StringBuilder(getQualifiedName(binding));
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
		ITypeBinding[] bounds= binding.getTypeBounds();
		if (bounds.length == 1 && "java.lang.Object".equals(bounds[0].getQualifiedName()))
			return binding.getName();

		StringBuilder result= new StringBuilder(binding.getName());
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
		StringBuilder result= new StringBuilder(PrettySignatures.getPlain(binding.getElementType()));
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
