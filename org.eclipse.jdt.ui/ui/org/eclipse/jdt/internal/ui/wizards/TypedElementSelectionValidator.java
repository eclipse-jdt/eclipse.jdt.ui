/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.util.Assert;

import org.eclipse.ui.dialogs.ISelectionStatusValidator;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;

/**
 * Implementation of a <code>ISelectionValidator</code> to validate the
 * type of an element.
 * Empty selections are not accepted.
 */
public class TypedElementSelectionValidator implements ISelectionStatusValidator {

	private IStatus fgErrorStatus= new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
	private IStatus fgOKStatus= new StatusInfo();

	private Class[] fAcceptedTypes;
	private boolean fAllowMultipleSelection;
	
	/**
	 * @param acceptedTypes The types accepted by the validator
	 * @param allowMultipleSelection If set to <code>true</code>, the validator
	 * allows multiple selection.
	 */
	public TypedElementSelectionValidator(Class[] acceptedTypes, boolean allowMultipleSelection) {
		Assert.isNotNull(acceptedTypes);
		fAcceptedTypes= acceptedTypes;
		fAllowMultipleSelection= allowMultipleSelection;
	}
	
	/*
	 * @see org.eclipse.ui.dialogs.ISelectionValidator#isValid(java.lang.Object)
	 */
	public IStatus validate(Object[] elements) {
		if (isValid(elements)) {
			return fgOKStatus;
		}
		return fgErrorStatus;
	}	

	private boolean isOfAcceptedType(Object o) {
		for (int i= 0; i < fAcceptedTypes.length; i++) {
			if (fAcceptedTypes[i].isInstance(o)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isValid(Object[] selection) {
		if (selection.length == 0) {
			return false;
		}
		
		if (!fAllowMultipleSelection && selection.length != 1) {
			return false;
		}
		
		for (int i= 0; i < selection.length; i++) {
			Object o= selection[i];	
			if (!isOfAcceptedType(o)) {
				return false;
			}
		}
		return true;
	}
}