/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring;

public class ParameterInfo {
	
	private String type;
	private String oldName;
	private int oldIndex;
	private String newName;
	private Object data;
	
	public ParameterInfo(String t, String name, int index){
		type= t;
		oldName= name;
		newName= name;
		oldIndex= index;
	}
	
	public String getType() {
		return type;
	}

	public String getOldName() {
		return oldName;
	}

	public int getOldIndex() {
		return oldIndex;
	}

	public void setNewName(String newName) {
		this.newName= newName;
	}

	public String getNewName() {
		return newName;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data= data;
	}
}