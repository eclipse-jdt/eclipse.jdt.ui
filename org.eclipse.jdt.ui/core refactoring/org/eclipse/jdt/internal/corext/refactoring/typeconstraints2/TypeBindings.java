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
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints2;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.TypeRules;

/**
 * Note: This class contains static helper methods to deal with
 * bindings from different clusters. They will be inlined as soon
 * as the compiler loop and subtyping-related queries on ITypeBindings
 * are implemented.  
 */
public class TypeBindings {
	
	private TypeBindings() {
		// no instances
	}
	
	//TODO: inline with b1 == b2 when compiler loop is used
	public static boolean equals(IBinding b1, IBinding b2) {
		return Bindings.equals(b1, b2);
	}
	
	public static boolean isSuperType(ITypeBinding supertype, ITypeBinding subtype) {
		return Bindings.isSuperType(supertype, subtype);
	}
	
	public static boolean canAssign(ITypeBinding expressionType, ITypeBinding variableType) {
		return TypeRules.canAssign(expressionType, variableType);
	}
	
//*** copied from org.eclipse.jdt.internal.compiler.lookup.Scope ***
//TODO: Ask JDT/Core for API for this?
	
	public static ITypeBinding mostSpecificCommonType(ITypeBinding[] types) {
		int length = types.length;
		int indexOfFirst = -1, actualLength = 0;
		for (int i = 0; i < length; i++) {
			ITypeBinding type = types[i];
			if (type == null) continue;
			if (type.isPrimitive()) return null;
			if (indexOfFirst < 0) indexOfFirst = i;
			actualLength ++;
		}
		switch (actualLength) {
			case 0: return null; //VoidBinding;
			case 1: return types[indexOfFirst];
		}

		// record all supertypes of type
		// intersect with all supertypes of otherType
		ITypeBinding firstType = types[indexOfFirst];
		ITypeBinding[] superTypes;
		int superLength;
		if (firstType.isPrimitive()) {
			return null; 
//TODO: arrays not done:
//		} else if (firstType.isArray()) {
//			superLength = 4;
//			superTypes = new ITypeBinding[] {
//					firstType, 
//					getJavaIoSerializable(),
//					getJavaLangCloneable(),
//					getJavaLangObject(),
//			};
		} else {
			ArrayList typesToVisit = new ArrayList(5);
			typesToVisit.add(firstType);
			ITypeBinding currentType = firstType;
			for (int i = 0, max = 1; i < max; i++) {
				currentType = (ITypeBinding) typesToVisit.get(i);
				ITypeBinding itsSuperclass = currentType.getSuperclass();
				if (itsSuperclass != null && !typesToVisit.contains(itsSuperclass)) {
					typesToVisit.add(itsSuperclass);
					max++;
				}
				ITypeBinding[] itsInterfaces = currentType.getInterfaces();
				for (int j = 0, count = itsInterfaces.length; j < count; j++)
					if (!typesToVisit.contains(itsInterfaces[j])) {
						typesToVisit.add(itsInterfaces[j]);
						max++;
					}
			}
			superLength = typesToVisit.size();
			superTypes = new ITypeBinding[superLength];
			typesToVisit.toArray(superTypes);
		}
		int remaining = superLength;
		nextOtherType: for (int i = indexOfFirst+1; i < length; i++) {
			ITypeBinding otherType = types[i];
			if (otherType == null)
				continue nextOtherType;
//TODO: arrays not done:
//			else if (otherType.isArray()) {
//				nextSuperType: for (int j = 0; j < superLength; j++) {
//					ITypeBinding superType = superTypes[j];
//					if (superType == null || superType == otherType) continue nextSuperType;
//					switch (superType.id) {
//						case T_JavaIoSerializable :
//						case T_JavaLangCloneable :
//						case T_JavaLangObject :
//							continue nextSuperType;
//					}
//					superTypes[j] = null;
//					if (--remaining == 0) return null;
//					
//				}
//				continue nextOtherType;
//			}
			ITypeBinding otherRefType = otherType;
			nextSuperType: for (int j = 0; j < superLength; j++) {
				ITypeBinding superType = superTypes[j];
				if (superType == null) continue nextSuperType;
				if (canAssign(otherRefType, superType)) {//otherRefType.isCompatibleWith(superType)) {
					break nextSuperType;
				} else {
					superTypes[j] = null;
					if (--remaining == 0) return null;
				}
			}				
		}
//		// per construction, first non-null supertype is most specific common supertype
//		for (int i = 0; i < superLength; i++) {
//			ITypeBinding superType = superTypes[i];
//			if (superType != null) return superType;
//		}
		//TODO: should often not be Object when Object and IInterface are available.
		// Need to count possible removable casts for each choice to decide for best choice.
		ITypeBinding object= null;
		for (int i = 0; i < superLength; i++) {
			ITypeBinding superType = superTypes[i];
			if (superType != null) {
				if (object == null && TypeRules.isJavaLangObject(superType))
					object= superType;
				else
					return superType;
			}
		}
		return object;
	}

//	public ITypeBinding lowerUpperBound(ITypeBinding[] types) {
//		ArrayList invocations = new ArrayList(1);
//		ITypeBinding mec = minimalErasedCandidate(types, invocations);
//		return leastContainingInvocation(mec, invocations);
//	}
//
//	/**
//	 * Returns the most specific type compatible with all given types.
//	 * (i.e. most specific common super type)
//	 * If no types is given, will return VoidBinding. If not compatible 
//	 * reference type is found, returns null.
//	 */
//	private ITypeBinding minimalErasedCandidate(ITypeBinding[] types, List invocations) {
//		Map allInvocations = new HashMap(2);
//		int length = types.length;
//		int indexOfFirst = -1, actualLength = 0;
//		for (int i = 0; i < length; i++) {
//			ITypeBinding type = types[i];
//			if (type == null) continue;
//			if (type.isBaseType()) return null;
//			if (indexOfFirst < 0) indexOfFirst = i;
//			actualLength ++;
//		}
//		switch (actualLength) {
//			case 0: return VoidBinding;
//			case 1: return types[indexOfFirst];
//		}
//
//		// record all supertypes of type
//		// intersect with all supertypes of otherType
//		ITypeBinding firstType = types[indexOfFirst];
//		ITypeBinding[] superTypes;
//		int superLength;
//		if (firstType.isBaseType()) {
//			return null; 
//		} else if (firstType.isArrayType()) {
//			superLength = 4;
//			if (firstType.erasure() != firstType) {
//				ArrayList someInvocations = new ArrayList(1);
//				someInvocations.add(firstType);
//				allInvocations.put(firstType.erasure(), someInvocations);
//			}
//			superTypes = new ITypeBinding[] {
//					firstType.erasure(), 
//					getJavaIoSerializable(),
//					getJavaLangCloneable(),
//					getJavaLangObject(),
//			};
//		} else {
//			ArrayList typesToVisit = new ArrayList(5);
//			if (firstType.erasure() != firstType) {
//				ArrayList someInvocations = new ArrayList(1);
//				someInvocations.add(firstType);
//				allInvocations.put(firstType.erasure(), someInvocations);
//			}			
//			typesToVisit.add(firstType.erasure());
//			ReferenceBinding currentType = (ReferenceBinding)firstType;
//			for (int i = 0, max = 1; i < max; i++) {
//				currentType = (ReferenceBinding) typesToVisit.get(i);
//				ITypeBinding itsSuperclass = currentType.superclass();
//				if (itsSuperclass != null) {
//					ITypeBinding itsSuperclassErasure = itsSuperclass.erasure();
//					if (!typesToVisit.contains(itsSuperclassErasure)) {
//						if (itsSuperclassErasure != itsSuperclass) {
//							ArrayList someInvocations = new ArrayList(1);
//							someInvocations.add(itsSuperclass);
//							allInvocations.put(itsSuperclassErasure, someInvocations);
//						}
//						typesToVisit.add(itsSuperclassErasure);
//						max++;
//					}
//				}
//				ReferenceBinding[] itsInterfaces = currentType.superInterfaces();
//				for (int j = 0, count = itsInterfaces.length; j < count; j++) {
//					ITypeBinding itsInterface = itsInterfaces[j];
//					ITypeBinding itsInterfaceErasure = itsInterface.erasure();
//					if (!typesToVisit.contains(itsInterfaceErasure)) {
//						if (itsInterfaceErasure != itsInterface) {
//							ArrayList someInvocations = new ArrayList(1);
//							someInvocations.add(itsInterface);
//							allInvocations.put(itsInterfaceErasure, someInvocations);
//						}						
//						typesToVisit.add(itsInterfaceErasure);
//						max++;
//					}
//				}
//			}
//			superLength = typesToVisit.size();
//			superTypes = new ITypeBinding[superLength];
//			typesToVisit.toArray(superTypes);
//		}
//		int remaining = superLength;
//		nextOtherType: for (int i = indexOfFirst+1; i < length; i++) {
//			ITypeBinding otherType = types[i];
//			if (otherType == null)
//				continue nextOtherType;
//			else if (otherType.isArrayType()) {
//				nextSuperType: for (int j = 0; j < superLength; j++) {
//					ITypeBinding superType = superTypes[j];
//					if (superType == null || superType == otherType) continue nextSuperType;
//					switch (superType.id) {
//						case T_JavaIoSerializable :
//						case T_JavaLangCloneable :
//						case T_JavaLangObject :
//							continue nextSuperType;
//					}
//					superTypes[j] = null;
//					if (--remaining == 0) return null;
//					
//				}
//				continue nextOtherType;
//			}
//			ReferenceBinding otherRefType = (ReferenceBinding) otherType;
//			nextSuperType: for (int j = 0; j < superLength; j++) {
//				ITypeBinding superType = superTypes[j];
//				if (superType == null) continue nextSuperType;
//				if (otherRefType.erasure().isCompatibleWith(superType)) {
//					ITypeBinding match = otherRefType.findSuperTypeErasingTo((ReferenceBinding)superType);
//						if (match != null && match.erasure() != match) { // match can be null: interface.findSuperTypeErasingTo(Object)
//							ArrayList someInvocations = (ArrayList) allInvocations.get(superType);
//							if (someInvocations == null) someInvocations = new ArrayList(1);
//							someInvocations.add(match);
//							allInvocations.put(superType, someInvocations);
//						}						
//					break nextSuperType;
//				} else {
//					superTypes[j] = null;
//					if (--remaining == 0) return null;
//				}
//			}				
//		}
//		// per construction, first non-null supertype is most specific common supertype
//		for (int i = 0; i < superLength; i++) {
//			ITypeBinding superType = superTypes[i];
//			if (superType != null) {
//				List matchingInvocations = (List)allInvocations.get(superType);
//				if (matchingInvocations != null) invocations.addAll(matchingInvocations);
//				return superType;
//			}
//		}
//		return null;
//	}	
//	
//	private ITypeBinding leastContainingInvocation(ITypeBinding mec, List invocations) {
//		int length = invocations.size();
//		if (length == 0) return mec;
//		if (length == 1) return (ITypeBinding) invocations.get(0);
//		int argLength = mec.typeVariables().length;
//		if (argLength == 0) return mec; // should be caught by no invocation check
//
//		// infer proper parameterized type from invocations
//		ITypeBinding[] bestArguments = new ITypeBinding[argLength];
//		for (int i = 0; i < length; i++) {
//			ITypeBinding invocation = (ITypeBinding)invocations.get(i);
//			if (invocation.isGenericType()) {
//				for (int j = 0; j < argLength; j++) {
//					ITypeBinding bestArgument = leastContainingTypeArgument(bestArguments[j], invocation.typeVariables()[j], (ReferenceBinding) mec, j);
//					if (bestArgument == null) return null;
//					bestArguments[j] = bestArgument;
//				}
//			} else if (invocation.isParameterizedType()) {
//				ParameterizedITypeBinding parameterizedType = (ParameterizedITypeBinding)invocation;
//				for (int j = 0; j < argLength; j++) {
//					ITypeBinding bestArgument = leastContainingTypeArgument(bestArguments[j], parameterizedType.arguments[j], (ReferenceBinding) mec, j);
//					if (bestArgument == null) return null;
//					bestArguments[j] = bestArgument;
//				}
//			} else if (invocation.isRawType()) {
//				return invocation; // raw type is taking precedence
//			}
//		}
//		return createParameterizedType((ReferenceBinding) mec, bestArguments, null);
//	}	
//	
//	private ITypeBinding leastContainingTypeArgument(ITypeBinding u, ITypeBinding v, ReferenceBinding genericType, int rank) {
//		if (u == null) return v;
//		if (u == v) return u;
//		if (v.isWildcard()) {
//			WildcardBinding wildV = (WildcardBinding) v;
//			if (u.isWildcard()) {
//				WildcardBinding wildU = (WildcardBinding) u;
//				switch (wildU.kind) {
//					// ? extends U
//					case Wildcard.EXTENDS :
//						switch(wildV.kind) {
//							// ? extends U, ? extends V
//							case Wildcard.EXTENDS :  
//								ITypeBinding lub = lowerUpperBound(new ITypeBinding[]{wildU.bound,wildV.bound});
//								if (lub == null) return null;
//								return environment().createWildcard(genericType, rank, lub, Wildcard.EXTENDS);	
//							// ? extends U, ? SUPER V
//							case Wildcard.SUPER : 
//								if (wildU.bound == wildV.bound) return wildU.bound;
//								return environment().createWildcard(genericType, rank, null, Wildcard.UNBOUND);
//						}
//						break;
//						// ? super U
//					case Wildcard.SUPER : 
//						// ? super U, ? super V
//						if (wildU.kind == Wildcard.SUPER) {
//							ITypeBinding[] glb = greaterLowerBound(new ITypeBinding[]{wildU.bound,wildV.bound});
//							if (glb == null) return null;
//							return environment().createWildcard(genericType, rank, glb[0], Wildcard.SUPER);	// TODO (philippe) need to capture entire bounds
//						}
//				}				
//			} else {
//				switch (wildV.kind) {
//					// U, ? extends V
//					case Wildcard.EXTENDS :
//						ITypeBinding lub = lowerUpperBound(new ITypeBinding[]{u,wildV.bound});
//						if (lub == null) return null;
//						return environment().createWildcard(genericType, rank, lub, Wildcard.EXTENDS);	
//					// U, ? super V
//					case Wildcard.SUPER :
//						ITypeBinding[] glb = greaterLowerBound(new ITypeBinding[]{u,wildV.bound});
//						if (glb == null) return null;
//						return environment().createWildcard(genericType, rank, glb[0], Wildcard.SUPER);	// TODO (philippe) need to capture entire bounds
//					case Wildcard.UNBOUND :
//				}
//			}
//		} else if (u.isWildcard()) {
//			WildcardBinding wildU = (WildcardBinding) u;
//			switch (wildU.kind) {
//				// U, ? extends V
//				case Wildcard.EXTENDS :
//					ITypeBinding lub = lowerUpperBound(new ITypeBinding[]{wildU.bound, v});
//					if (lub == null) return null;
//					return environment().createWildcard(genericType, rank, lub, Wildcard.EXTENDS);	
//				// U, ? super V
//				case Wildcard.SUPER :
//					ITypeBinding[] glb = greaterLowerBound(new ITypeBinding[]{wildU.bound, v});
//					if (glb == null) return null;
//					return environment().createWildcard(genericType, rank, glb[0], Wildcard.SUPER); // TODO (philippe) need to capture entire bounds		
//				case Wildcard.UNBOUND :
//			}
//		}
//		ITypeBinding lub = lowerUpperBound(new ITypeBinding[]{u,v});
//		if (lub == null) return null;
//		return environment().createWildcard(genericType, rank, lub, Wildcard.EXTENDS);
//	}
}
