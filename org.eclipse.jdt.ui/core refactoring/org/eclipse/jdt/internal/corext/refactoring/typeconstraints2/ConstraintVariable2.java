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

import org.eclipse.jdt.internal.corext.Assert;


/**
 * A ConstraintVariable stands for an AST entity which has a type. 
 */
public abstract class ConstraintVariable2 {
	
	protected static final String TO_STRING= "toString"; //$NON-NLS-1$
	
	private TypeHandle fTypeHandle;
	
	private Object[] fDatas;

	private ConstraintVariable2 fUpdatableConstraintVariable;

	
	/**
	 * @param typeHandle the type binding TODO: allow null?
	 */
	protected ConstraintVariable2(TypeHandle typeHandle) {
		Assert.isNotNull(typeHandle);
		fTypeHandle= typeHandle;
//		if (DEBUG)
//			setData(TO_STRING, Bindings.asString(typeBinding));
	}
	
	public TypeHandle getTypeHandle() {
		return fTypeHandle;
	}
	
	public void setData(String name, Object data) {
		if (data == null) { // remove
			if (fDatas != null) {
				int len= fDatas.length;
				if (len == 2) {
					fDatas= null;
				} else {
					Object[] newData= new Object[len - 2];
					System.arraycopy(fDatas, 0, newData, 0, len - 2);
					fDatas= newData;
				}				
			}
		} else { // add
			if (fDatas == null) {
				fDatas= new Object[2];
			} else {
				int len= fDatas.length;
				Object[] newData= new Object[len + 2];
				System.arraycopy(fDatas, 0, newData, 2, len);
				fDatas= newData;
			}
			fDatas[0]= name;
			fDatas[1]= data;
		}
	}

	public Object getData(String name) {
		if (fDatas == null) {
			return null;
		} else {
			for (int i= 0; i < fDatas.length; i+= 2) {
				String key= (String) fDatas[i];
				if (key.equals(name))
					return fDatas[i + 1];
			}
			return null;
		}
	}
	
	public String toString() {
		if (fTypeHandle == null)
			return "<NULL TYPE HANDLE>"; //$NON-NLS-1$
		String toString= (String) getData(TO_STRING);
		return toString == null ? fTypeHandle.getTypeKey() : toString;
	}
	
	protected abstract int getHash();
	
	protected abstract boolean isSameAs(ConstraintVariable2 other);

	/**
	 * @param updatableConstraintVariable
	 * 		the updatable ConstraintVariable for <code>this</code>'s equivalence group
	 */
	public void setRepresentative(ConstraintVariable2 updatableConstraintVariable) {
		Assert.isTrue(fUpdatableConstraintVariable == null);
		fUpdatableConstraintVariable= updatableConstraintVariable;
	}
}
