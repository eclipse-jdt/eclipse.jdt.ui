/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.sef.SelfEncapsulateFieldRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.sef.SelfEncapsulateFieldWizard;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;

public class SelfEncapsulateFieldAction extends RefactoringAction {

	public SelfEncapsulateFieldAction(StructuredSelectionProvider provider) {
		super("Self Encapsulate...", provider);
	}
	
	public void run() {
		IField selectedField= (IField)SelectionUtil.getSingleElement(getStructuredSelection());
		IField field= null;
		try {
			field= (IField)WorkingCopyUtil.getWorkingCopyIfExists(selectedField);
		} catch (JavaModelException e) {
		}
		if (field == null) {
			MessageDialog.openInformation(JavaPlugin.getActiveWorkbenchShell(), "Self Encapsulate Field", "Field '" + selectedField.getElementName() + "' doesn't exist in editor buffer anymore.");
			return;
		}
			
		SelfEncapsulateFieldRefactoring refactoring= new SelfEncapsulateFieldRefactoring(field, JavaPreferencesSettings.getCodeGenerationSettings());
		try  {	
			new RefactoringStarter().activate(
				refactoring, 
				new SelfEncapsulateFieldWizard(refactoring),
				"Self Encapsulate Field", true);
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, "Self Encapsulate Field", "Cannot perform refactoring. See log for more details.");
		}
	}
	
	public boolean canOperateOn(IStructuredSelection selection) {
		Object o= SelectionUtil.getSingleElement(selection);
		if (o instanceof IField) {
			IField field= (IField)o;
			if (!field.isBinary())
				return true;
		}
		return false;
	}	
}

