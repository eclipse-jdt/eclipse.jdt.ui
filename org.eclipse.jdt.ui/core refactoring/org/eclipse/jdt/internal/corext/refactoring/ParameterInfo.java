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
	
	private static final int INDEX_FOR_ADDED= -1;
	private final String oldName;
	private final int oldIndex;
	private String type;
	private String defaultValue;
	private String newName;
	private Object data;
	
	public ParameterInfo(String type, String name, int index){
		this.type= type;
		this.oldName= name;
		this.newName= name;
		this.oldIndex= index;
		this.defaultValue= "";
	}

	public static ParameterInfo createInfoForAddedParameter(){
		return new ParameterInfo("", "", INDEX_FOR_ADDED);
	}
	
	public boolean isAdded(){
		return oldIndex == INDEX_FOR_ADDED;
	}
	
	public String getDefaultValue(){
		return defaultValue;
	}
	
	public void setDefaultValue(String value){
		this.defaultValue= value;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type){
		this.type= type;
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