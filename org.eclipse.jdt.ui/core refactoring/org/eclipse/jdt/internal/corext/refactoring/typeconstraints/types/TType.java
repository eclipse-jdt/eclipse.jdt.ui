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
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types;

import org.eclipse.jdt.core.dom.ITypeBinding;

public abstract class TType {
	
	public static final int NULL_TYPE= 1;
	public static final int VOID_TYPE= 2;
	public static final int PRIMITIVE_TYPE= 3;
	
	public static final int ARRAY_TYPE= 4;
	
	public static final int STANDARD_TYPE= 5;
	public static final int GENERIC_TYPE= 6;
	public static final int PARAMETERIZED_TYPE= 7;
	public static final int RAW_TYPE= 8;
	
	public static final int UNBOUND_WILDCARD_TYPE= 9;
	public static final int SUPER_WILDCARD_TYPE= 10;
	public static final int EXTENDS_WILDCARD_TYPE= 11;
	
	public static final int TYPE_VARIABLE= 12;
	
	protected static final int WILDCARD_TYPE_SHIFT= 3;
	protected static final int ARRAY_TYPE_SHIFT= 5;
	
	private static final int F_IS_CLASS= 1 << 0;
	private static final int F_IS_INTERFACE= 1 << 1;
	private static final int F_IS_ENUM= 1 << 2;
	private static final int F_IS_ANNOTATION= 1 << 3;
	
	private static final int F_IS_TOP_LEVEL= 1 << 4;
	private static final int F_IS_NESTED= 1 << 5;
	private static final int F_IS_MEMBER= 1 << 6;
	private static final int F_IS_LOCAL= 1 << 7;
	private static final int F_IS_ANONYMOUS= 1 << 8;
	
	private static final TType[] EMPTY_TYPE_ARRAY= new TType[0];
	
	private TypeEnvironment fEnvironment;
	private String fBindingKey;
	private int fModifiers;
	private int fFlags;
	
	/**
	 * Creates a new type with the given environment as an owner
	 */
	protected TType(TypeEnvironment environment) {
		fEnvironment= environment;
	}
	
	/**
	 * Creates a new type with the given environment as an owner
	 */
	protected TType(TypeEnvironment environment, String key) {
		this(environment);
		fBindingKey= key;
	}
	
	/**
	 * Initialized the type from the given binding
	 * 
	 * @param binding the binding to initialize from
	 */
	protected void initialize(ITypeBinding binding) {
		if (binding != null) {
			fBindingKey= binding.getKey();
			fModifiers= binding.getModifiers();
			if (binding.isClass()) {
				fFlags= F_IS_CLASS;
			} else if (binding.isInterface()) {
				fFlags= F_IS_INTERFACE;
			} else if (binding.isAnnotation()) {
				fFlags= F_IS_ANNOTATION;
			} else if (binding.isEnum()) {
				fFlags= F_IS_ENUM;
			}

			if (binding.isTopLevel()) {
				fFlags|= F_IS_TOP_LEVEL;
			} else if (binding.isNested()) {
				fFlags|= F_IS_NESTED;
				if (binding.isMember()) {
					fFlags|= F_IS_MEMBER;
				} else if (binding.isLocal()) {
					fFlags|= F_IS_LOCAL;
				} else if (binding.isAnonymous()) {
					fFlags|= F_IS_ANONYMOUS;
				}
			}
		}
	}
	
	/**
	 * Returns the type's environment
	 * 
	 * @return the types's environment
	 */
	public TypeEnvironment getEnvironment() {
		return fEnvironment;
	}
	
	/**
	 * Returns the key of the binding from which this type
	 * got constructed.
	 * 
	 * @return the binding key
	 */
	protected String getBindingKey() {
		return fBindingKey;
	}
	
	/**
	 * Returns the modifiers for this type.
	 * 
	 * @return the bit-wise or of <code>Modifier</code> constants
	 * @see org.eclipse.jdt.core.dom.IBinding#getModifiers()
	 * @see org.eclipse.jdt.core.dom.Modifier
	 */ 
	public int getModifiers() {
		return fModifiers;
	}
	
	/**
	 * Returns the element type
	 * 
	 * @return the element type.
	 */
	public abstract int getElementType();
	
	/**
	 * Returns whether this type represents <code>java.lang.Object</code> or
	 * not.
	 * 
	 * @return whether this type is <code>java.lang.Object</code> or not
	 */
	public boolean isJavaLangObject() {
		return false;
	}
	
	/**
	 * Returns whether this type represents <code>java.lang.Cloneable</code>
	 * or not.
	 * 
	 * @return whether this type is <code>java.lang.Cloneable</code> or not
	 */
	public boolean isJavaLangCloneable() {
		return false;
	}
	
	/**
	 * Returns whether this type represents <code>java.io.Serializable</code>
	 * or not.
	 * 
	 * @return whether this type is <code>java.io.Serializable</code> or not
	 */
	public boolean isJavaIoSerializable() {
		return false;
	}
	
	/**
	 * Returns <code>true</code> if the given type represents the null type.
	 * Otherwise <code>false</code> is returned.
	 * 
	 * @return whether this type is the null type or not
	 */
	public final boolean isNullType() {
		return getElementType() == NULL_TYPE;
	}
	
	/**
	 * Returns <code>true</code> if the given type represents the void type.
	 * Otherwise <code>false</code> is returned.
	 * 
	 * @return whether this type is the void type or not
	 */
	public final boolean isVoidType() {
		return getElementType() == VOID_TYPE;
	}
	
	/**
	 * Returns <code>true</code> if the given type represents a primitive type.
	 * Otherwise <code>false</code> is returned.
	 * 
	 * @return whether this type is a primitive type or not
	 */
	public final boolean isPrimitiveType() {
		return getElementType() == PRIMITIVE_TYPE;
	}
	
	/**
	 * Returns <code>true</code> if the given type represents an array type.
	 * Otherwise <code>false</code> is returned.
	 * 
	 * @return whether this type is an array type or not
	 */
	public final boolean isArrayType() {
		return getElementType() == ARRAY_TYPE;
	}
	
	/**
	 * Returns <code>true</code> if the given type represents a hierarchy type.
	 * Otherwise <code>false</code> is returned.
	 * 
	 * @return whether this type is a hierarchy type or not
	 */
	public final boolean isHierarchyType() {
		int elementType= getElementType();
		return elementType == RAW_TYPE || elementType == PARAMETERIZED_TYPE
			|| elementType == GENERIC_TYPE || elementType == STANDARD_TYPE; 
	}
	
	/**
	 * Returns <code>true</code> if the given type represents a standard type.
	 * Otherwise <code>false</code> is returned.
	 * 
	 * @return whether this type is a standard type or not
	 */
	public final boolean isStandardType() {
		return getElementType() == STANDARD_TYPE;
	}
	
	/**
	 * Returns <code>true</code> if the given type represents a raw type.
	 * Otherwise <code>false</code> is returned.
	 * 
	 * @return whether this type is a raw type or not
	 */
	public final boolean isRawType() {
		return getElementType() == RAW_TYPE;
	}
	
	/**
	 * Returns <code>true</code> if the given type represents a parameterized type.
	 * Otherwise <code>false</code> is returned.
	 * 
	 * @return whether this type is a parameterized type or not
	 */
	public final boolean isParameterizedType() {
		return getElementType() == PARAMETERIZED_TYPE;
	}
	
	/**
	 * Returns <code>true</code> if the given type represents a generic type.
	 * Otherwise <code>false</code> is returned.
	 * 
	 * @return whether this type is a generic type or not
	 */
	public final boolean isGenericType() {
		return getElementType() == GENERIC_TYPE;
	}
	
	/**
	 * Returns <code>true</code> if the given type represents a type variable.
	 * Otherwise <code>false</code> is returned.
	 * 
	 * @return whether this type is a type variable or not
	 */
	public final boolean isTypeVariable() {
		return getElementType() == TYPE_VARIABLE;
	}
	
	/**
	 * Returns <code>true</code> if the given type represents a wildcard type.
	 * Otherwise <code>false</code> is returned.
	 * 
	 * @return whether this type is a wildcard type or not
	 */
	public final boolean isWildcardType() {
		int elementType= getElementType();
		return elementType == EXTENDS_WILDCARD_TYPE || elementType == UNBOUND_WILDCARD_TYPE
			|| elementType == SUPER_WILDCARD_TYPE; 
	}
	
	/**
	 * Returns <code>true</code> if the given type represents a unbound wildcard type.
	 * Otherwise <code>false</code> is returned.
	 * 
	 * @return whether this type is a unbound wildcard type or not
	 */
	public final boolean isUnboundWildcardType() {
		return getElementType() == UNBOUND_WILDCARD_TYPE;
	}
	
	/**
	 * Returns <code>true</code> if the given type represents an extends wildcard type.
	 * Otherwise <code>false</code> is returned.
	 * 
	 * @return whether this type is an extends wildcard type or not
	 */
	public final boolean isExtendsWildcardType() {
		return getElementType() == EXTENDS_WILDCARD_TYPE;
	}
	
	/**
	 * Returns <code>true</code> if the given type represents a super wildcard type.
	 * Otherwise <code>false</code> is returned.
	 * 
	 * @return whether this type is a super wildcard type or not
	 */
	public final boolean isSuperWildcardType() {
		return getElementType() == SUPER_WILDCARD_TYPE;
	}
	
	/**
	 * Returns whether this type represents a class. 
	 * 
	 * @return whether this type represents a class
	 * @see ITypeBinding#isClass()
	 */
	public final boolean isClass() {
		return (fFlags & F_IS_CLASS) != 0;
	}
	
	/**
	 * Returns whether this type represents a interface. 
	 * 
	 * @return whether this type represents a interface
	 * @see ITypeBinding#isInterface()
	 */
	public final boolean isInterface() {
		return (fFlags & F_IS_INTERFACE) != 0;
	}
	
	/**
	 * Returns whether this type represents a enumeration. 
	 * 
	 * @return whether this type represents a enumeration
	 * @see ITypeBinding#isEnum()
	 */
	public final boolean isEnum() {
		return (fFlags & F_IS_ENUM) != 0;
	}
	
	/**
	 * Returns whether this type represents an annotation. 
	 * 
	 * @return whether this type represents an annotation
	 * @see ITypeBinding#isAnnotation()
	 */
	public final boolean isAnnotation() {
		return (fFlags & F_IS_ANNOTATION) != 0;
	}
	
	/**
	 * Returns whether this type represents a top level type. 
	 * 
	 * @return whether this type represents a top level type
	 * @see ITypeBinding#isTopLevel()
	 */
	public final boolean isTopLevel() {
		return (fFlags & F_IS_TOP_LEVEL) != 0;
	}
	
	/**
	 * Returns whether this type represents a nested type. 
	 * 
	 * @return whether this type represents a nested type
	 * @see ITypeBinding#isNested()
	 */
	public final boolean isNested() {
		return (fFlags & F_IS_NESTED) != 0;
	}
	
	/**
	 * Returns whether this type represents a member type. 
	 * 
	 * @return whether this type represents a member type
	 * @see ITypeBinding#isMember()
	 */
	public final boolean isMember() {
		return (fFlags & F_IS_MEMBER) != 0;
	}
	
	/**
	 * Returns whether this type represents a local type. 
	 * 
	 * @return whether this type represents a local type
	 * @see ITypeBinding#isLocal()
	 */
	public final boolean isLocal() {
		return (fFlags & F_IS_LOCAL) != 0;
	}
	
	/**
	 * Returns whether this type represents an anonymous type. 
	 * 
	 * @return whether this type represents an anonymous type
	 * @see ITypeBinding#isAnonymous()
	 */
	public final boolean isAnonymous() {
		return (fFlags & F_IS_ANONYMOUS) != 0;
	}
	
	/**
	 * Returns the super classes of this type or <code>null</code>.
	 * 
	 * @return the super class of this type
	 */
	public TType getSuperclass() {
		return null;
	}
	
	/**
	 * Returns the interfaces this type implements or extends.
	 * 
	 * @return the "super" interfaces or an empty array
	 */
	public TType[] getInterfaces() {
		return EMPTY_TYPE_ARRAY;
	}
	
	public boolean isEqualTo(ITypeBinding binding) {
		if (binding == null)
			return false;
		return binding.getKey().equals(fBindingKey);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public final boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof TType))
			return false;
		TType otherType= (TType)other;
		TypeEnvironment otherEnvironment= otherType.fEnvironment;
		if (fEnvironment.isIdentityTest() && otherEnvironment.isIdentityTest() && fEnvironment == otherEnvironment)
			return false;
		if (getElementType() != otherType.getElementType())
			return false;
		return doEquals(otherType);
	}
	
	/**
	 * Performs the actual equals check.
	 * 
	 * @param type the right hand side of the equals operation
	 */
	protected abstract boolean doEquals(TType type);
	
	/**
	 * Returns the erasure of this type as defined by ITypeBinding#getErasure().
	 * 
	 * @return the erasure of this type
	 */
	public TType getErasure() {
		return this;
	}
	
	/**
	 * Returns the type for the type declaration corresponding to this type.
	 *
	 * @return the type representing the declaration of this type
	 * @see ITypeBinding#getTypeDeclaration()
	 */
	public TType getTypeDeclaration() {
		return this;
	}
	
	/**
	 * Answer <code>true</code> if the receiver of this method can be assigned
	 * to the argument lhs (e.g lhs= this is a valid assignment).
	 * 
	 * @param lhs the left hand side of the assignment
	 * @return whether or not this type can be assigned to lhs
	 */
	public final boolean canAssignTo(TType lhs) {
		if (this.isTypeEquivalentTo(lhs))
			return true;
		return doCanAssignTo(lhs);
	}
	
	/**
	 * Returns whether the receiver type is type equivalent to the other type.
	 * This method considers the erasure for generic, raw and parameterized
	 * types.
	 * 
	 * @return whether the receiver is type equivalent to other
	 */
	protected boolean isTypeEquivalentTo(TType other) {
		return this.equals(other);
	}
	
	/**
	 * Checks whether the this left hand side type interpreted as a type
	 * argument of a parameterized type is compatible with the given 
	 * type <code>rhs</code>. For example if List<this>= List<rhs> is 
	 * a valid assignment.
	 * 
	 * @retrun whether this type is type argument compatible with rhs
	 */
	protected boolean checkTypeArgument(TType rhs) {
		return this.equals(rhs);
	}
	
	/**
	 * Hook method to perform the actual can assign test
	 * 
	 * @param lhs the left hand side of the assignment "return whether or not
	 *        this type can be assigned to lhs
	 */
	protected abstract boolean doCanAssignTo(TType lhs);

	/**
	 * Returns the name of this type as defined by {@link ITypeBinding#getName()}.
	 * 
	 * @return the name of this type
	 * @see ITypeBinding#getName()
	 */
	public abstract String getName();
	
	/**
	 * Returns a signature of this type which can be presented to the user.
	 * 
	 * @return a pretty signature for this type
	 */
	public abstract String getPrettySignature();
	
	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		return getPrettySignature();
	}
}
