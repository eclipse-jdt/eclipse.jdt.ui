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


public abstract class ConstraintVariable2 {
	
	protected static final String TO_STRING= "toString"; //$NON-NLS-1$
	
	private Object[] fDatas;

	public void setData(String name, Object data) {
		int index= 0;
		if (fDatas != null) {
			while (index < fDatas.length) {
				if (name.equals (fDatas[index]))
					break;
				index += 2;
			}
		}
		if (data != null) { //add
			if (fDatas != null) {
				if (index == fDatas.length) {
					Object[] newTable= new Object[fDatas.length + 2];
					System.arraycopy(fDatas, 0, newTable, 0, fDatas.length);
					fDatas= newTable;
				}
			} else {
				fDatas= new Object[2];
			}
			fDatas[index]= name;
			fDatas[index + 1]= data;
		} else { //remove
			if (fDatas != null) {
				if (index != fDatas.length) {
					int length= fDatas.length - 2;
					if (length == 0) {
						fDatas= null;
					} else {
						Object[] newTable= new Object[length];
						System.arraycopy(fDatas, 0, newTable, 0, index);
						System.arraycopy(fDatas, index + 2, newTable, index, length - index);
						fDatas= newTable;
					}
				}
			}
		}
		
//		int index= -1;
//		for (int i= 0; i < fDatas.length; i+= 2) {
//			String key= (String) fDatas[i];
//			if (key.equals(name))
//				index= i;
//		}
//		
//		if (fDatas == null) { // remove
//			if (fDatas != null) {
//				int len= fDatas.length;
//				if (len == 2) {
//					fDatas= null;
//				} else {
//					Object[] newData= new Object[len - 2];
//					System.arraycopy(fDatas, 0, newData, 0, len - 2);
//					fDatas= newData;
//				}				
//			}
//		} else { // add
//			if (fDatas == null) {
//				fDatas= new Object[2];
//			} else {
//				int len= fDatas.length;
//				Object[] newData= new Object[len + 2];
//				System.arraycopy(fDatas, 0, newData, 2, len);
//				fDatas= newData;
//			}
//			fDatas[0]= name;
//			fDatas[1]= fDatas;
//		}
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
		String toString= (String) getData(TO_STRING);
		if (toString != null)
			return toString;
		else
			return super.toString();
		
//		String name= getClass().getName();
//		int dot= name.lastIndexOf('.');
//		return name.substring(dot + 1) + ": " //$NON-NLS-1$
//				+ (fTypeHandle == null ? "<NULL TYPE HANDLE>" : fTypeHandle.getQualifiedName()); //$NON-NLS-1$
		//TODO:
//		if (fTypeHandle == null)
//			return "<NULL TYPE HANDLE>"; //$NON-NLS-1$
//		String toString= (String) getData(TO_STRING);
//		return toString == null ? fTypeHandle.getTypeKey() : toString;
	}
	
	protected abstract int getHash();
	
	protected abstract boolean isSameAs(ConstraintVariable2 other);

}
