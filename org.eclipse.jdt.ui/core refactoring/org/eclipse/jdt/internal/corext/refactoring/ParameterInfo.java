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

import org.eclipse.jdt.internal.corext.Assert;

public class ParameterInfo {
	
	private static final int INDEX_FOR_ADDED= -1;
	private final String fOldName;
	private final int fOldIndex;
	private String fType;
	private String fDefaultValue;
	private String fNewName;
	private Object fData;
	private boolean fIsDeleted;
	
	public ParameterInfo(String type, String name, int index){
		fType= type;
		fOldName= name;
		fNewName= name;
		fOldIndex= index;
		fDefaultValue= "";
		fIsDeleted= false;
	}

	public static ParameterInfo createInfoForAddedParameter(){
		return new ParameterInfo("", "", INDEX_FOR_ADDED);
	}
	
	public boolean isDeleted(){
		return fIsDeleted;
	}
	
	public void markAsDeleted(){
		Assert.isTrue(! isAdded());//added param infos should be simply removed from the list
		fIsDeleted= true;
	}
	
	public boolean isAdded(){
		return fOldIndex == INDEX_FOR_ADDED;
	}
	
	public String getDefaultValue(){
		return fDefaultValue;
	}
	
	public void setDefaultValue(String value){
		fDefaultValue= value;
	}
	
	public String getType() {
		return fType;
	}
	
	public void setType(String type){
		fType= type;
	}

	public String getOldName() {
		return fOldName;
	}

	public int getOldIndex() {
		return fOldIndex;
	}

	public void setNewName(String newName) {
		fNewName= newName;
	}

	public String getNewName() {
		return fNewName;
	}

	public Object getData() {
		return fData;
	}

	public void setData(Object data) {
		fData= data;
	}
	
	public boolean isRenamed() {
		return !fOldName.equals(fNewName);
	}
}