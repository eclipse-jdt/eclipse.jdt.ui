/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;

/**
 * A dialog to enter a filter pattern. 
 */
public class FilterPatternsDialog extends InputDialog {	
    
    static class NonEmptyValidator implements IInputValidator {
		public String isValid(String input) {
		    if (input.length() > 0)
		    	return null;
		    return JUnitMessages.getString("FilterPatternsDialog.message.notempty"); //$NON-NLS-1$
		}
	};
	
	/*
	 * @see InputDialog#InputDialog
	 */
	public FilterPatternsDialog(Shell parent, String title, String message) {
		super(parent, title, message, "", new NonEmptyValidator()); //$NON-NLS-1$
	}
	
	/*
	 * @see InputDialog#createDialogArea(Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Control result= super.createDialogArea(parent);
		getText().setFocus();
		return result;
	}
}