/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs.tests;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IInputValidator;

import org.eclipse.jdt.internal.ui.dialogs.StringInputDialog;

public class StringInputDialogTest {

	public static void main(String[] args) {
		Display display= new Display();
		StringInputDialog dialog= new StringInputDialog(new Shell(display), "Title", null, "Message",
			"Input", new IInputValidator() {
				public String isValid(String s) {
					return null;
				}
			}
			);
		dialog.open();
	}
}