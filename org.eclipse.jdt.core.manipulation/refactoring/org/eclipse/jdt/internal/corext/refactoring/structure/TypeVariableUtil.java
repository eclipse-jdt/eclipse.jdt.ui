/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;

/**
 * Utilities to create mappings between type variables of different types in a type hierarchy.
 */
public final class TypeVariableUtil {

	/**
	 * Returns the composition of two type variable mappings. The type variables signatures can have an arbitrary format.
	 *
	 * @param first
	 *        the first type variable mapping
	 * @param second
	 *        the second type variable mapping
	 * @return the possibly empty composed type variable mapping
	 */
	public static TypeVariableMaplet[] composeMappings(final TypeVariableMaplet[] first, final TypeVariableMaplet[] second) {
		Assert.isNotNull(first);
		Assert.isNotNull(second);

		if (first.length == 0)
			return first;
		else if (second.length == 0)
			return second;
		else {
			TypeVariableMaplet source= null;
			TypeVariableMaplet target= null;
			final Set<TypeVariableMaplet> set= new HashSet<>(first.length * second.length);
			for (int index= 0; index < first.length; index++) {
				for (int offset= 0; offset < second.length; offset++) {
					source= first[index];
					target= second[offset];
					if (source.getTargetIndex() == target.getSourceIndex() && source.getTargetName().equals(target.getSourceName()))
						set.add(new TypeVariableMaplet(source.getSourceName(), index, target.getTargetName(), offset));
				}
			}
			final TypeVariableMaplet[] mapping= new TypeVariableMaplet[set.size()];
			set.toArray(mapping);
			return mapping;
		}
	}

	/**
	 * Extracts the type variables from a signature
	 *
	 * @param signature
	 *        the signature to extract the type variables from
	 * @param variables
	 *        the set of variables to fill in
	 */
	private static void extractTypeVariables(final String signature, final Set<String> variables) {
		Assert.isNotNull(signature);
		Assert.isNotNull(variables);

		final String[] arguments= Signature.getTypeArguments(signature);
		if (arguments.length == 0) {
			variables.add(Signature.toString(signature));
		} else {
			for (String argument : arguments) {
				variables.add(Signature.toString(argument));
			}
		}
	}

	/**
	 * Returns the type variables referenced in the signature of the specified member.
	 *
	 * @param declaring
	 *        The declaring type of the specified member
	 * @param member
	 *        the member to get its type variables. Can be a type, field or a method.
	 * @return a possibly empty array of type variable candidates
	 * @throws JavaModelException
	 *         if the signature of the specified member could not be resolved
	 */
	private static String[] getReferencedVariables(final IType declaring, final IMember member) throws JavaModelException {

		Assert.isNotNull(declaring);
		Assert.isNotNull(member);

		final String[] variables= parametersToVariables(declaring.getTypeParameters());
		String[] result= new String[0];
		if (member instanceof IField) {
			final String signature= ((IField) member).getTypeSignature();
			final String[] signatures= getVariableSignatures(signature);
			if (signatures.length == 0) {
				final String variable= Signature.toString(signature);
				for (String v : variables) {
					if (variable.equals(v)) {
						result= new String[] { variable};
						break;
					}
				}
			} else {
				result= new String[signatures.length];
				for (int index= 0; index < result.length; index++)
					result[index]= Signature.toString(signatures[index]);
			}
		} else if (member instanceof IMethod) {
			final IMethod method= (IMethod) member;
			final HashSet<String> set= new HashSet<>();
			for (String type : method.getParameterTypes()) {
				extractTypeVariables(type, set);
			}
			extractTypeVariables(method.getReturnType(), set);
			final String[] arguments= parametersToVariables(((IMethod) member).getTypeParameters());
			Collections.addAll(set, arguments);
			result= new String[set.size()];
			set.toArray(result);
		} else if (member instanceof IType)
			result= parametersToVariables(((IType) member).getTypeParameters());
		else {
			JavaManipulationPlugin.logErrorMessage("Unexpected sub-type of IMember: " + member.getClass().getName()); //$NON-NLS-1$
			Assert.isTrue(false);
		}

		final List<String> list= new ArrayList<>(variables.length);
		for (String variable : variables) {
			for (String r : result) {
				if (variable.equals(r)) {
					list.add(r);
				}
			}
		}
		result= new String[list.size()];
		list.toArray(result);
		return result;
	}

	/**
	 * Returns all type variable names of the indicated member not mapped by the specified type variable mapping.
	 *
	 * @param mapping
	 *        the type variable mapping. The entries of this mapping must be simple type variable names.
	 * @param declaring
	 *        the declaring type of the indicated member
	 * @param member
	 *        the member to determine its unmapped type variable names
	 * @return a possibly empty array of unmapped type variable names
	 * @throws JavaModelException
	 *         if the type parameters of the member could not be determined
	 */
	public static String[] getUnmappedVariables(final TypeVariableMaplet[] mapping, final IType declaring, final IMember member) throws JavaModelException {

		Assert.isNotNull(mapping);
		Assert.isNotNull(declaring);
		Assert.isNotNull(member);

		List<String> list= null;
		final String[] types= getReferencedVariables(declaring, member);
		if (mapping.length == 0) {
			list= new ArrayList<>(types.length);
			list.addAll(Arrays.asList(types));
		} else {
			final Set<String> mapped= new HashSet<>(types.length);
			for (String type : types) {
				for (TypeVariableMaplet m : mapping) {
					if (m.getSourceName().equals(type)) {
						mapped.add(type);
					}
				}
			}
			list= new ArrayList<>(types.length - mapped.size());
			for (String type : types) {
				if (!mapped.contains(type))
					list.add(type);
			}
		}
		final String[] result= new String[list.size()];
		list.toArray(result);
		return result;
	}

	/**
	 * Returns the type variable signatures of the specified parameterized type signature, or an empty array if none.
	 *
	 * @param signature
	 *        the signature to get its type variable signatures from. The signature must be a parameterized type signature.
	 * @return a possibly empty array of type variable signatures
	 * @see Signature#getTypeArguments(String)
	 */
	private static String[] getVariableSignatures(final String signature) {
		Assert.isNotNull(signature);

		String[] result= null;
		try {
			result= Signature.getTypeArguments(signature);
		} catch (IllegalArgumentException exception) {
			result= new String[0];
		}
		return result;
	}

	/**
	 * Returns the reversed type variable mapping of the specified mapping.
	 *
	 * @param mapping
	 *        the mapping to inverse
	 * @return the possibly empty inverse mapping
	 */
	public static TypeVariableMaplet[] inverseMapping(final TypeVariableMaplet[] mapping) {
		Assert.isNotNull(mapping);

		final TypeVariableMaplet[] result= new TypeVariableMaplet[mapping.length];
		TypeVariableMaplet maplet= null;
		for (int index= 0; index < mapping.length; index++) {
			maplet= mapping[index];
			result[index]= new TypeVariableMaplet(maplet.getTargetName(), maplet.getTargetIndex(), maplet.getSourceName(), maplet.getSourceIndex());
		}
		return result;
	}

	/**
	 * Creates a type variable mapping from a domain to a range.
	 *
	 * @param domain
	 *        the domain of the mapping
	 * @param range
	 *        the range of the mapping
	 * @param indexes
	 *        <code>true</code> if the indexes should be compared, <code>false</code> if the names should be compared
	 * @return a possibly empty type variable mapping
	 */
	private static TypeVariableMaplet[] parametersToSignatures(final ITypeParameter[] domain, final String[] range, final boolean indexes) {
		Assert.isNotNull(domain);
		Assert.isNotNull(range);

		final Set<TypeVariableMaplet> set= new HashSet<>();
		ITypeParameter source= null;
		String target= null;
		String element= null;
		String signature= null;
		for (int index= 0; index < domain.length; index++) {
			source= domain[index];
			for (int offset= 0; offset < range.length; offset++) {
				target= range[offset];
				element= source.getElementName();
				signature= Signature.toString(target);
				if (indexes) {
					if (offset == index)
						set.add(new TypeVariableMaplet(element, index, signature, offset));
				} else {
					if (element.equals(signature))
						set.add(new TypeVariableMaplet(element, index, signature, offset));
				}
			}
		}
		final TypeVariableMaplet[] result= new TypeVariableMaplet[set.size()];
		set.toArray(result);
		return result;
	}

	/**
	 * Converts the specified type parameters to type variable names.
	 *
	 * @param parameters
	 *        the type parameters to convert.
	 * @return the converted type variable names
	 * @see ITypeParameter#getElementName()
	 */
	private static String[] parametersToVariables(final ITypeParameter[] parameters) {
		Assert.isNotNull(parameters);

		String[] result= new String[parameters.length];
		for (int index= 0; index < parameters.length; index++)
			result[index]= parameters[index].getElementName();

		return result;
	}

	/**
	 * Creates a type variable mapping from a domain to a range.
	 *
	 * @param domain
	 *        the domain of the mapping
	 * @param range
	 *        the range of the mapping
	 * @return a possibly empty type variable mapping
	 */
	private static TypeVariableMaplet[] signaturesToParameters(final String[] domain, final ITypeParameter[] range) {
		Assert.isNotNull(domain);
		Assert.isNotNull(range);
		Assert.isTrue(domain.length == 0 || domain.length == range.length);

		final List<TypeVariableMaplet> list= new ArrayList<>();
		String source= null;
		String target= null;
		for (int index= 0; index < domain.length; index++) {
			source= Signature.toString(domain[index]);
			target= range[index].getElementName();
			list.add(new TypeVariableMaplet(source, index, target, index));
		}
		final TypeVariableMaplet[] result= new TypeVariableMaplet[list.size()];
		list.toArray(result);
		return result;
	}

	/**
	 * Returns a type variable mapping from a subclass to a superclass.
	 *
	 * @param type
	 *        the type representing the subclass class
	 * @return a type variable mapping. The mapping entries consist of simple type variable names.
	 * @throws JavaModelException
	 *         if the signature of one of the types involved could not be retrieved
	 */
	public static TypeVariableMaplet[] subTypeToInheritedType(final IType type) throws JavaModelException {
		Assert.isNotNull(type);

		final ITypeParameter[] domain= type.getTypeParameters();
		if (domain.length > 0) {
			final String signature= type.getSuperclassTypeSignature();
			if (signature != null) {
				final String[] range= getVariableSignatures(signature);
				if (range.length > 0)
					return parametersToSignatures(domain, range, false);
			}
		}
		return new TypeVariableMaplet[0];
	}

	/**
	 * Returns a type variable mapping from a subclass to an implemented interface.
	 *
	 * @param subtype
	 *        the type representing the subclass class
	 * @param implemented
	 *        the interface being implemented by the subclass
	 * @return a type variable mapping. The mapping entries consist of simple type variable names.
	 * @throws JavaModelException
	 *         if the signature of one of the types involved could not be retrieved
	 */
	public static TypeVariableMaplet[] subTypeToImplementedType(final IType subtype, final IType implemented) throws JavaModelException {
		Assert.isNotNull(subtype);
		Assert.isNotNull(implemented);

		Index index= new Index();
		final TypeVariableMaplet[] mapping= subTypeToImplementedTypeMapping(subtype, implemented, index);
		if (mapping.length > 0) {
			final ITypeParameter[] range= implemented.getTypeParameters();
			if (range.length > 0) {
				final String signature= subtype.getSuperInterfaceTypeSignatures()[index.index];
				if (signature != null) {
					final String[] domain= getVariableSignatures(signature);
					if (domain.length > 0)
						return composeMappings(mapping, signaturesToParameters(domain, range));
				}
			}
		}
		return mapping;
	}

	private static class Index {
		public int index;
	}

	private static TypeVariableMaplet[] subTypeToImplementedTypeMapping(final IType type, final IType implemented, Index index) throws JavaModelException {
		Assert.isNotNull(type);
		Assert.isNotNull(implemented);

		final ITypeParameter[] domain= type.getTypeParameters();
		if (domain.length > 0) {
			final String[] signatures= type.getSuperInterfaceTypeSignatures();
			for (int i= 0; i < signatures.length; ++i) {
				String signature= signatures[i];
				if (Signature.getSignatureSimpleName(Signature.getTypeErasure(signature)).equals(implemented.getElementName())) {
					index.index= i;
					final String[] range= getVariableSignatures(signature);
					if (range.length > 0)
						return parametersToSignatures(domain, range, false);
				}
			}
		}
		return new TypeVariableMaplet[0];
	}

	/**
	 * Returns a type variable mapping from a subclass to a superclass.
	 *
	 * @param subtype
	 *        the type representing the subclass
	 * @param supertype
	 *        the type representing the superclass
	 * @return a type variable mapping. The mapping entries consist of simple type variable names.
	 * @throws JavaModelException
	 *         if the signature of one of the types involved could not be retrieved
	 */
	public static TypeVariableMaplet[] subTypeToSuperType(final IType subtype, final IType supertype) throws JavaModelException {
		Assert.isNotNull(subtype);
		Assert.isNotNull(supertype);

		final TypeVariableMaplet[] mapping= subTypeToInheritedType(subtype);
		if (mapping.length > 0) {
			final ITypeParameter[] range= supertype.getTypeParameters();
			if (range.length > 0) {
				final String signature= subtype.getSuperclassTypeSignature();
				if (signature != null) {
					final String[] domain= getVariableSignatures(signature);
					if (domain.length > 0)
						return composeMappings(mapping, signaturesToParameters(domain, range));
				}
			}
		}
		return mapping;
	}

	/**
	 * Returns a type variable mapping from a superclass to a subclass.
	 *
	 * @param supertype
	 *        the type representing the superclass
	 * @param subtype
	 *        the type representing the subclass
	 * @return a type variable mapping. The mapping entries consist of simple type variable names.
	 * @throws JavaModelException
	 *         if the signature of one of the types involved could not be retrieved
	 */
	public static TypeVariableMaplet[] superTypeToInheritedType(final IType supertype, final IType subtype) throws JavaModelException {
		Assert.isNotNull(subtype);
		Assert.isNotNull(supertype);

		final ITypeParameter[] domain= supertype.getTypeParameters();
		if (domain.length > 0) {
			final String signature= subtype.getSuperclassTypeSignature();
			if (signature != null) {
				final String[] range= getVariableSignatures(signature);
				if (range.length > 0)
					return parametersToSignatures(domain, range, true);
			}
		}
		return new TypeVariableMaplet[0];
	}

	/**
	 * Returns a type variable mapping from a superclass to a subclass.
	 *
	 * @param supertype
	 *        the type representing the superclass
	 * @param subtype
	 *        the type representing the subclass
	 * @return a type variable mapping. The mapping entries consist of simple type variable names.
	 * @throws JavaModelException
	 *         if the signature of one of the types involved could not be retrieved
	 */
	public static TypeVariableMaplet[] superTypeToSubType(final IType supertype, final IType subtype) throws JavaModelException {
		Assert.isNotNull(supertype);
		Assert.isNotNull(subtype);

		return inverseMapping(subTypeToSuperType(subtype, supertype));
	}

	private TypeVariableUtil() {
		// Not to be instantiated
	}
}
