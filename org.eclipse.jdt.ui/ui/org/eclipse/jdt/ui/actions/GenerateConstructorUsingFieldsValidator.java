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
package org.eclipse.jdt.ui.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.ui.dialogs.ISelectionStatusValidator;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;


class GenerateConstructorUsingFieldsValidator implements ISelectionStatusValidator {
	static int fEntries;
	IType fType;
	GenerateConstructorUsingFieldsSelectionDialog fDialog;
	List fExistingSigs;

	GenerateConstructorUsingFieldsValidator(int entries) {
		super();
		fEntries= entries;
		fType= null;
	}

	GenerateConstructorUsingFieldsValidator(int entries, GenerateConstructorUsingFieldsSelectionDialog dialog, IType type) {
		super();
		fEntries= entries;
		fDialog= dialog;
		fType= type;
		// Create the potential signature and compare it to the existing ones	
		fExistingSigs= getExistingConstructorSignatures();
	}

	public IStatus validate(Object[] selection) {
		StringBuffer buffer= new StringBuffer();
		buffer.append('(');
		// first form the part of the signature corresponding to the super constructor combo choice
		IMethod chosenSuper= fDialog.getSuperConstructorChoice();
		try {
			String superParamTypes[]= chosenSuper.getParameterTypes();
			for (int i= 0; i < superParamTypes.length; i++) {
				buffer.append(superParamTypes[i]);
			}

			// second form the part of the signature corresponding to the fields selected
			for (int i= 0; i < selection.length; i++) {
				if (selection[i] instanceof IField) {
					buffer.append(((IField) selection[i]).getTypeSignature());
				}
			}
		} catch (JavaModelException e) {
		}

		buffer.append(")V"); //$NON-NLS-1$
		if (fExistingSigs.contains(buffer.toString())) {
			return new StatusInfo(IStatus.WARNING, ActionMessages.getString("GenerateConstructorUsingFieldsAction.error.duplicate_constructor")); //$NON-NLS-1$							
		}

		int fieldCount= countSelectedFields(selection);
		String message= ActionMessages.getFormattedString("GenerateConstructorUsingFieldsAction.fields_selected", new Object[] { String.valueOf(fieldCount), String.valueOf(fEntries)}); //$NON-NLS-1$
		return new StatusInfo(IStatus.INFO, message);
	}

	int countSelectedFields(Object[] selection) {
		int count= 0;
		for (int i= 0; i < selection.length; i++) {
			if (selection[i] instanceof IField)
				count++;
		}
		return count;
	}

	List getExistingConstructorSignatures() {
		List constructorMethods= new ArrayList();
		try {
			IMethod[] methods= fType.getMethods();
			for (int i= 0; i < methods.length; i++) {
				IMethod curr= methods[i];
				if (curr.isConstructor()) {
					constructorMethods.add(curr.getSignature());
				}
			}
		} catch (JavaModelException e) {
		}
		return constructorMethods;
	}
}