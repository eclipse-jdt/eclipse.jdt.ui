/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs;


import java.util.List;

public class TypedElementSelectionValidator implements ISelectionValidator {
	
	private Class[] fAcceptedTypes;
	private boolean fMultiple;
	private String fErrorMessage; 
	
	public TypedElementSelectionValidator(Class type) {
		this(type, false);
	}
	
	public TypedElementSelectionValidator(Class[] types) {
		this(types, false);
	}		
	
	
	public TypedElementSelectionValidator(Class type, boolean multiple) {
		this(new Class[] { type }, multiple);
	}
	
	public TypedElementSelectionValidator(Class[] types, boolean multiple) {
		fAcceptedTypes= types;
		fMultiple= multiple;
		fErrorMessage= ""; //$NON-NLS-1$
	}
	
	public void setErrorString(String errorMessage) {
		fErrorMessage= errorMessage;
	}
			
	
	private boolean isOfAcceptedType(Object o) {
		for (int i= 0; i < fAcceptedTypes.length; i++) {
			if (fAcceptedTypes[i].isInstance(o)) {
				return true;
			}
		}
		return false;
	}

	public void isValid(Object[] elements, StatusInfo res) {
		if (isValid(elements)) {
			res.setOK();
		} else {
			res.setError(fErrorMessage);
		}
	}	
	
	private boolean isValid(Object[] selection) {
		int length= selection.length;
		if (length == 0) {
			return false;
		}
		
		if (!fMultiple && length != 1) {
			return false;
		}
		
		for (int i= 0; i < length; i++) {
			Object o= selection[i];	
			if (!isOfAcceptedType(o) || !isSelectedValid(o)) {
				return false;
			}
		}
		return true;
	}
	
	public boolean isSelectedValid(Object element) {
		return true;
	}
}