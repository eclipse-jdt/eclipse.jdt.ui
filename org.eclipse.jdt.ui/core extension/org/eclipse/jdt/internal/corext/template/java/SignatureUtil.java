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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;

import org.eclipse.jdt.internal.core.util.Util;

/**
 * Utilities for Signature operations.
 * 
 * @see Signature
 * @since 3.1
 */
public final class SignatureUtil {
	
	private static final String NULL_TYPE_SIGNATURE= "TNULL;"; //$NON-NLS-1$
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
	 * Takes a method signature <code>[&lt; typeVariableName : formalTypeDecl &gt;] ( paramTypeSig1* ) retTypeSig</code> and
	 * returns it with any type signature that is a bounded type converted using
	 * <code>getUpperBound</code>.
	 * <p>
	 * TODO this is a temporary workaround for
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=83600
	 * </p>
	 * 
	 * @param signature the method signature to convert
	 * @return the signature with no bounded types
	 */
	public static char[] unboundedSignature(char[] signature) {
		if (signature == null || signature.length < 2)
			return signature;
		
		// XXX the signatures from CompletionRequestor contain a superfluous '+'
		// before type parameters to parameter types
		StringBuffer sig= new StringBuffer();
		sig.append(signature);
		do {
			int pos= sig.indexOf("++"); //$NON-NLS-1$
			if (pos == -1)
				pos= sig.indexOf("+*"); //$NON-NLS-1$
			if (pos == -1)
				pos= sig.indexOf("+-"); //$NON-NLS-1$
			if (pos == -1)
				break;
			sig.deleteCharAt(pos);
		} while (true);
		signature= sig.toString().toCharArray();
		
		int pos= 0;
		Map methodTypeDecls= new HashMap();
		if (signature[pos] == Signature.C_GENERIC_START) {
			// read type declarations
			int nextColon= CharOperation.indexOf(Signature.C_COLON, signature, pos + 1);
			do {
				int id_end= Util.scanIdentifier(signature, pos + 1);
				String typeVar= String.valueOf(signature, pos + 1, id_end - pos);
				methodTypeDecls.put(typeVar, new ArrayList());
				pos= id_end;
				while (nextColon != -1) {
					while (CharOperation.indexOf(Signature.C_COLON, signature, nextColon + 1) == nextColon + 1)
						nextColon++; // skip empty class bound
					
					int sig_end= Util.scanTypeArgumentSignature(signature, nextColon + 1);
					String typeBound= String.valueOf(signature, nextColon + 1, sig_end - nextColon);
					((List) methodTypeDecls.get(typeVar)).add(typeBound);
					pos= sig_end;
					if (CharOperation.indexOf(Signature.C_COLON, signature, pos + 1) == pos + 1)
						nextColon= pos + 1;
					else
						nextColon= -1;
				}
				nextColon= CharOperation.indexOf(Signature.C_COLON, signature, pos + 1);
			} while (nextColon != -1);
			pos++;
			if (signature[pos] != Signature.C_GENERIC_END)
				throw new IllegalArgumentException(String.valueOf(signature));
			pos++;
		}
		
		if (signature[pos] != Signature.C_PARAM_START)
			throw new IllegalArgumentException(String.valueOf(signature));
		pos++;
		
		// read arguments and return type
		StringBuffer res= new StringBuffer("("); //$NON-NLS-1$
		boolean isReturnType= false;
		while (pos < signature.length) {
			char ch= signature[pos];
			switch (ch) {
				case Signature.C_STAR:
					pos++;
					if (isReturnType)
						res.append(OBJECT_SIGNATURE); // return type is at least Object
					else
						res.append(NULL_TYPE_SIGNATURE); // no lower bound! return void type for now //$NON-NLS-1$
					break;
				case Signature.C_SUPER:
					int end= Util.scanTypeSignature(signature, pos + 1);
					if (isReturnType)
						res.append(OBJECT_SIGNATURE); // return type is at least Object
					else
						res.append(replaceTypeVariableBySingleBound(signature, pos + 1, end - pos, methodTypeDecls));
					pos= end + 1;
					break;
				case Signature.C_EXTENDS:
					end= Util.scanTypeSignature(signature, pos + 1);
					if (isReturnType)
						res.append(replaceTypeVariableBySingleBound(signature, pos + 1, end - pos, methodTypeDecls));
					else
						res.append(NULL_TYPE_SIGNATURE);
					pos= end + 1;
					break;
				case Signature.C_PARAM_END:
					pos++;
					res.append(Signature.C_PARAM_END);
					isReturnType= true;
					break;
				default:
					end= Util.scanTypeSignature(signature, pos);
					String typeSig= replaceTypeVariableBySingleBound(signature, pos, end, methodTypeDecls);
					res.append(typeSig);
					pos= end + 1;
					break;
			}
		}
		
		return res.toString().toCharArray();
	}

	private static String replaceTypeVariableBySingleBound(char[] signature, int pos, int end, Map methodTypeDecls) {
		String typeSig= String.valueOf(signature, pos, end - pos + 1);
		// don't do early resolvation for now
		if (false && Signature.getTypeSignatureKind(typeSig) == Signature.TYPE_VARIABLE_SIGNATURE) {
			String typeVar= Signature.getSignatureSimpleName(typeSig);
			if (methodTypeDecls.containsKey(typeVar)) {
				List types= (List) methodTypeDecls.get(typeVar);
				if (types.size() == 1)
					typeSig= (String) types.get(0); // only replace single matches - multi-matches have to be resolved later
			}
		}
		return typeSig;
	}

}