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
 *     Red Hat Inc. - initial API and implementation
  *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.runtime.Platform;

public class SerialVersionHashOperationDisplayCore {

	/**
	 * Displays an appropriate error message for a specific problem.
	 *
	 * @param message
	 *            The message to display
	 */
	public void displayErrorMessage(final String message) {
		Platform.getLog(this.getClass()).error(message);
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
	public boolean displayYesNoMessage(final String title, final String message) {
		return true;
	}

}

