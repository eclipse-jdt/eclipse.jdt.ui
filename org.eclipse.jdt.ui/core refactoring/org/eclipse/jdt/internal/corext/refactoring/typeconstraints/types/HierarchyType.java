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

import java.util.Map;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.internal.corext.Assert;

public abstract class HierarchyType extends TType {
	private HierarchyType fSuperclass;
	private HierarchyType[] fInterfaces;
	private IType fJavaElementType;
	
	protected HierarchyType(TypeEnvironment environment) {
		super(environment);
	}

	protected void initialize(ITypeBinding binding, IType javaElementType) {
		super.initialize(binding);
		Assert.isNotNull(javaElementType);
		fJavaElementType= javaElementType;
		TypeEnvironment environment= getEnvironment();
		ITypeBinding superclass= binding.getSuperclass();
		if (superclass != null) {
			fSuperclass= (HierarchyType)environment.create(superclass);
		}
		ITypeBinding[] interfaces= binding.getInterfaces();
		fInterfaces= new HierarchyType[interfaces.length];
		for (int i= 0; i < interfaces.length; i++) {
			fInterfaces[i]= (HierarchyType)environment.create(interfaces[i]);
		}
	}
	
	public TType getSuperclass() {
		return fSuperclass;
	}
	
	public TType[] getInterfaces() {
		return fInterfaces;
	}
	
	public IType getJavaElementType() {
		return fJavaElementType;
	}
	
	public boolean isSubType(HierarchyType other) {
		if (getEnvironment() == other.getEnvironment()) {
			Map cache= getEnvironment().getSubTypeCache();
			TypeTuple key= new TypeTuple(this, other);
			Boolean value= (Boolean)cache.get(key);
			if (value != null)
				return value.booleanValue();
			boolean isSub= doIsSubType(other);
			value= new Boolean(isSub);
			cache.put(key, value);
			return isSub;
		}
		return doIsSubType(other);
	}

	private boolean doIsSubType(HierarchyType other) {
		if (fSuperclass != null && (other.isTypeEquivalentTo(fSuperclass) || fSuperclass.doIsSubType(other)))
			return true;
		for (int i= 0; i < fInterfaces.length; i++) {
			if (other.isTypeEquivalentTo(fInterfaces[i]) || fInterfaces[i].doIsSubType(other))
				return true;
		}
		return false;
	}
	
	protected boolean canAssignToStandardType(StandardType target) {
		if (target.isJavaLangObject())
			return true;
		return isSubType(target);
	}	
}
