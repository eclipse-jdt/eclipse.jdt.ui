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
package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.core.util.Util;

/**
 * Utilities for Signature operations.
 * 
 * @see Signature
 * @since 3.1
 */
public final class SignatureUtil {
	private static final String OBJECT_SIGNATURE= "Ljava.lang.Object;"; //$NON-NLS-1$

	private SignatureUtil() {
		// do not instantiate
	}
	
	/**
	 * Returns <code>true</code> if <code>signature</code> is the
	 * signature of the <code>java.lang.Object</code> type.
	 * 
	 * @param signature the signature
	 * @return <code>true</code> if <code>signature</code> is the
	 *         signature of the <code>java.lang.Object</code> type,
	 *         <code>false</code> otherwise
	 */
	public static boolean isJavaLangObject(String signature) {
		return OBJECT_SIGNATURE.equals(signature);
	}
	
	/**
	 * Returns the upper bound of a type signature. Returns the signature of <code>java.lang.Object</code> if
	 * <code>signature</code> is a lower bound (<code>(? super T)</code>); returns
	 * the signature of the type <code>T</code> of an upper bound (<code>(? extends T)</code>)
	 * or <code>signature</code> itself if it is not a bound signature.
	 * 
	 * @param signature the signature
	 * @return the upper bound signature of <code>signature</code>
	 */
	public static String getUpperBound(String signature) {
		if (signature.equals(String.valueOf(Signature.C_STAR))) //$NON-NLS-1$
			return OBJECT_SIGNATURE;
		
		if (signature.startsWith(String.valueOf(Signature.C_SUPER))) //$NON-NLS-1$
			return OBJECT_SIGNATURE;
		
		if (signature.startsWith(String.valueOf(Signature.C_EXTENDS))) //$NON-NLS-1$
			return signature.substring(1);
		
		return signature;
	}

	/**
	 * Returns the fully qualified type name of the given signature, with any
	 * type parameters and arrays erased.
	 * 
	 * @param signature the signature
	 * @return the fully qualified type name of the signature
	 */
	public static String stripSignatureToFQN(String signature) throws IllegalArgumentException {
		signature= Signature.getTypeErasure(signature);
		signature= Signature.getElementType(signature);
		String simpleName= Signature.getSignatureSimpleName(signature);
		String qualifier= Signature.getSignatureQualifier(signature);
		if (qualifier.length() > 0)
			return Signature.toQualifiedName(new String[] {qualifier, simpleName});
		else
			return simpleName;
	}
	
	/**
	 * Returns the qualified signature corresponding to
	 * <code>signature</code>.
	 * 
	 * @param signature the signature to qualify
	 * @param context the type inside which an unqualified type will be
	 *        resolved to find the qualifier, or <code>null</code> if no
	 *        context is available
	 * @return the qualified signature
	 */
	public static String qualifySignature(final String signature, final IType context) {
		if (context == null)
			return signature;
		
		String qualifier= Signature.getSignatureQualifier(signature);
		if (qualifier.length() > 0)
			return signature;

		String elementType= Signature.getElementType(signature);
		String erasure= Signature.getTypeErasure(elementType);
		String simpleName= Signature.getSignatureSimpleName(erasure);
		String genericSimpleName= Signature.getSignatureSimpleName(elementType);
		
		int dim= Signature.getArrayCount(signature);
		
		try {
			String[][] strings= context.resolveType(simpleName);
			if (strings != null && strings.length > 0)
				qualifier= strings[0][0];
		} catch (JavaModelException e) {
			// ignore - not found
		}
		
		if (qualifier.length() == 0)
			return signature;
		
		String qualifiedType= Signature.toQualifiedName(new String[] {qualifier, genericSimpleName});
		String qualifiedSignature= Signature.createTypeSignature(qualifiedType, true);
		String newSignature= Signature.createArraySignature(qualifiedSignature, dim);
		
		return newSignature;
	}
	
	/**
	 * Takes a method signature <code>(paramTypeSig1;paramTypeSig2)</code> and
	 * returns it with any type signature that is a bounded type converted using
	 * <code>getUpperBound</code>.
	 * <p>
	 * TODO this is a temporary workaround for
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=83383
	 * </p>
	 * 
	 * @param signature the method signature to convert
	 * @return the signature with no bounded types
	 */
	public static char[] unboundedSignature(char[] signature) {
		if (signature == null || signature.length < 2)
			return signature;
		
		// XXX the signature somehow contains double '+'
		StringBuffer sig= new StringBuffer();
		sig.append(signature);
		do {
			int pos= sig.indexOf("++"); //$NON-NLS-1$
			if (pos == -1)
				break;
			sig.deleteCharAt(pos);
		} while (true);
		signature= sig.toString().toCharArray();
		
		int pos= 0;
		// skip type declaration
		if (signature[pos] == Signature.C_GENERIC_START) {
			pos= Util.scanIdentifier(signature, pos + 1);
			if (signature[pos + 1] != Signature.C_COLON)
				throw new IllegalArgumentException(String.valueOf(signature));
			pos= Util.scanTypeArgumentSignature(signature, pos + 2) + 1;
			if (signature[pos] != Signature.C_GENERIC_END)
				throw new IllegalArgumentException(String.valueOf(signature));
			pos++;
		}
		
		if (signature[pos] != Signature.C_PARAM_START)
			throw new IllegalArgumentException(String.valueOf(signature));
		pos++;
		
		StringBuffer res= new StringBuffer("("); //$NON-NLS-1$
		boolean isReturnType= false;
		while (pos < signature.length) {
			char ch= signature[pos];
			switch (ch) {
				case Signature.C_STAR:
					pos++;
					res.append(OBJECT_SIGNATURE);
					break;
				case Signature.C_SUPER:
					int end= Util.scanTypeSignature(signature, pos + 1);
					if (isReturnType)
						res.append(OBJECT_SIGNATURE); // return type is at least Object
					else
						res.append(signature, pos + 1, end - pos);
					pos= end + 1;
					break;
				case Signature.C_EXTENDS:
					end= Util.scanTypeSignature(signature, pos + 1);
					if (isReturnType)
						res.append(signature, pos + 1, end - pos);
					else
//						res.append(OBJECT_SIGNATURE); // XXX wrong - should be the null type!
						res.append("V"); // no lower bound! return void type for now //$NON-NLS-1$
					pos= end + 1;
					break;
				case Signature.C_PARAM_END:
					pos++;
					res.append(Signature.C_PARAM_END);
					isReturnType= true;
					break;
				default:
					end= Util.scanTypeSignature(signature, pos);
					res.append(signature, pos, end - pos + 1);
					pos= end + 1;
					break;
			}
		}
		
		return res.toString().toCharArray();
	}

}