/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.sef;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardDialog;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringAction;
import org.eclipse.jdt.internal.ui.refactoring.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;

public class SelfEncapsulateFieldAction extends RefactoringAction {

	public SelfEncapsulateFieldAction(StructuredSelectionProvider provider) {
		super("Self Encapsulate", provider);
	}
	
	public void run() {
		IField field= (IField)SelectionUtil.getSingleElement(getStructuredSelection());
		SelfEncapsulateFieldWizard wizard= new SelfEncapsulateFieldWizard(field,
			JavaPlugin.getDefault().getCompilationUnitDocumentProvider());
		WizardDialog dialog= new RefactoringWizardDialog(JavaPlugin.getActiveWorkbenchShell(), wizard);
		dialog.open();		
	}
	
	public boolean canOperateOn(IStructuredSelection selection) {
		Object o= SelectionUtil.getSingleElement(selection);
		if (o instanceof IField)
			return true;
		return false;
	}	
}

