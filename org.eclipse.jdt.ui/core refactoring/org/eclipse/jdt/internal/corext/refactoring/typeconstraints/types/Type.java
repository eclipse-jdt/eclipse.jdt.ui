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

public abstract class Type {
	
	public static final int NULL_TYPE= 1;
	public static final int PRIMITIVE_TYPE= 2;
	
	public static final int ARRAY_TYPE= 3;
	
	public static final int STANDARD_TYPE= 4;
	public static final int GENERIC_TYPE= 5;
	public static final int PARAMETERIZED_TYPE= 6;
	public static final int RAW_TYPE= 7;
	
	public static final int UNBOUND_WILDCARD_TYPE= 8;
	public static final int SUPER_WILDCARD_TYPE= 9;
	public static final int EXTENDS_WILDCARD_TYPE= 10;
	
	public static final int TYPE_VARIABLE= 11;
	
	protected static final int WILDCARD_TYPE_SHIFT= 3;
	protected static final int ARRAY_TYPE_SHIFT= 5;
	
	private TypeEnvironment fEnvironment;
	
	/**
	 * Creates a new type with the given environment as 
	 * an owner
	 */
	protected Type(TypeEnvironment environment) {
		fEnvironment= environment;
	}
	
	/**
	 * Initialized the type from the given binding
	 * 
	 * @param binding the binding to initialize from
	 */
	protected void initialize(ITypeBinding binding) {
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
	 * Returns the element type
	 * 
	 * @return the element type.
	 */
	public abstract int getElementType();
	
	/**
	 * Returns whether this type represents <code>java.lang.Object</code>
	 * or not.
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
	 * Returns <code>true</code> if the given type represents a wildcard type.
	 * Otherwise <code>false</code> is returned.
	 * 
	 * @return whether this type is a wildcard type or not
	 */
	protected boolean isWildcardType() {
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public final boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof Type))
			return false;
		Type otherType= (Type)other;
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
	protected abstract boolean doEquals(Type type);
	
	/**
	 * Returns the erasure of this type as defined by ITypeBinding#getErasure().
	 * 
	 * @return the erasure of this type
	 */
	public Type getErasure() {
		return this;
	}
	
	/**
	 * Answer <code>true</code> if the receiver of this method can be assigned to the 
	 * argument lhs (e.g lhs= this is a valid assignment).
	 * 
	 * @param lhs the left hand side of the assignment
	 * @return whether or not this type can be assigned to lhs
	 */
	public final boolean canAssignTo(Type lhs) {
		if (this.isTypeEquivalentTo(lhs))
			return true;
		return doCanAssignTo(lhs);
	}
	
	/**
	 * Returns whether the receiver type is type equivalent
	 * to the other type. This method considers the erasure
	 * for generic, raw and parameterized types.
	 * 
	 * @return whether the receiver is type equivalent to other
	 */
	protected boolean isTypeEquivalentTo(Type other) {
		return this.equals(other);
	}
	
	/**
	 * Hook method to perform the actual can assign test
	 * 
	 * @param lhs the left hand side of the assignment
	 * "return whether or not this type can be assigned to lhs
	 */
	protected abstract boolean doCanAssignTo(Type lhs);

	/**
	 * Returns a signature of this type which can be presented
	 * to the user.
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
