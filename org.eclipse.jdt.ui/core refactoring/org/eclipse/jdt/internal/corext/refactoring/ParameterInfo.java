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
package org.eclipse.jdt.internal.corext.refactoring;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import org.eclipse.jdt.internal.corext.Assert;

public class ParameterInfo {
	
	public static final int INDEX_FOR_ADDED= -1;
	private final IVariableBinding fOldBinding;
	private final boolean fIsVarargs;
	private final String fOldName;
	private final String fOldTypeName;
	private final int fOldIndex;

	private String fNewTypeName;
	private ITypeBinding fNewTypeBinding;
	private String fDefaultValue;
	private String fNewName;
	private boolean fIsDeleted;
	
	public ParameterInfo(String type, String name, int index) {
		this(null, type, name, index);
	}

	public ParameterInfo(IVariableBinding binding, String type, String name, int index) {
		this(binding, false, type, name, index);
	}

	public ParameterInfo(IVariableBinding binding, boolean isVarargs, String type, String name, int index) {
		fOldBinding= binding;
		fOldTypeName= type;
		fNewTypeName= type;
		fOldName= name;
		fNewName= name;
		fOldIndex= index;
		fDefaultValue= ""; //$NON-NLS-1$
		fIsDeleted= false;
		fIsVarargs= isVarargs;
	}

	public static ParameterInfo createInfoForAddedParameter(){
		ParameterInfo info= new ParameterInfo("Object", "newParam", INDEX_FOR_ADDED); //$NON-NLS-1$ //$NON-NLS-2$
		info.setDefaultValue("null"); //$NON-NLS-1$
		return info;
	}
	
	public boolean isVarargs() {
		return fIsVarargs;
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
		Assert.isNotNull(value);
		fDefaultValue= value;
	}

	public IVariableBinding getOldBinding() {
		return fOldBinding;
	}

	public String getOldTypeName() {
		return fOldTypeName;
	}
	
	public String getNewTypeName() {
		return fNewTypeName;
	}
	
	public void setNewTypeName(String type){
		Assert.isNotNull(type);
		fNewTypeName= type;
	}

	public ITypeBinding getNewTypeBinding() {
		return fNewTypeBinding;
	}
	
	public void setNewTypeBinding(ITypeBinding typeBinding){
		fNewTypeBinding= typeBinding;
	}

	public String getOldName() {
		return fOldName;
	}

	public int getOldIndex() {
		return fOldIndex;
	}

	public void setNewName(String newName) {
		Assert.isNotNull(newName);
		fNewName= newName;
	}

	public String getNewName() {
		return fNewName;
	}

	public boolean isRenamed() {
		return !fOldName.equals(fNewName);
	}
	
	public boolean isTypeNameChanged() {
		return !fOldTypeName.equals(fNewTypeName);
	}
	
	public String toString() {
		return fOldTypeName + " " + fOldName + " @" + fOldIndex + " -> " //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		+ fNewTypeName + " " + fNewName + ": " + fDefaultValue  //$NON-NLS-1$//$NON-NLS-2$
		+ (fIsDeleted ? " (deleted)" : " (stays)");  //$NON-NLS-1$//$NON-NLS-2$
	}
}
