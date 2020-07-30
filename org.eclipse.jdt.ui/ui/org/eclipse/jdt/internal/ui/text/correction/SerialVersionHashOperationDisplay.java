/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - code extracted from SerialVersionOperation and placed here
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.corext.util.Messages;

public class SerialVersionHashOperationDisplay extends SerialVersionHashOperationDisplayCore {

	/**
	 * Displays an appropriate error message for a specific problem.
	 *
	 * @param message
	 *            The message to display
	 */
	@Override
	public void displayErrorMessage(final String message) {
		final Display display= PlatformUI.getWorkbench().getDisplay();
		if (display != null && !display.isDisposed()) {
			display.asyncExec(() -> {
				if (!display.isDisposed()) {
					final Shell shell= display.getActiveShell();
					if (shell != null && !shell.isDisposed())
						MessageDialog.openError(shell, CorrectionMessages.SerialVersionHashOperation_dialog_error_caption, Messages.format(CorrectionMessages.SerialVersionHashOperation_dialog_error_message, message));
				}
			});
		}
	}

	/**
	 * Displays a dialog with a question as message.
	 *
	 * @param title
	 *            The title to display
	 * @param message
	 *            The message to display
	 * @return returns the result of the dialog
	 */
	@Override
	public boolean displayYesNoMessage(final String title, final String message) {
		final boolean[] result= { true};
		final Display display= PlatformUI.getWorkbench().getDisplay();
		if (display != null && !display.isDisposed()) {
			display.syncExec(() -> {
				if (!display.isDisposed()) {
					final Shell shell= display.getActiveShell();
					if (shell != null && !shell.isDisposed())
						result[0]= MessageDialog.openQuestion(shell, title, message);
				}
			});
		}
		return result[0];
	}

}
