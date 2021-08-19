/*******************************************************************************
 *  Copyright (c) 2000, 2022 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 * Note:
 *     The code extracted from org.eclipse.jdt.internal.debug.ui package from the
 *     eclipse.jdt.debug project.
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.filtertable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.PixelConverter;

/**
 * Factory class to create some SWT resources.
 */
class SWTFactory {

	/**
	 * Returns a width hint for a button control.
	 */
	private static int getButtonWidthHint(Button button) {
		/*button.setFont(JFaceResources.getDialogFont());*/
		PixelConverter converter= new PixelConverter(button);
		int widthHint= converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		return Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
	}

	/**
	 * Sets width and height hint for the button control.
	 * <b>Note:</b> This is a NOP if the button's layout data is not
	 * an instance of <code>GridData</code>.
	 *
	 * @param button the button for which to set the dimension hint
	 */
	private static void setButtonDimensionHint(Button button) {
		Assert.isNotNull(button);
		Object gd= button.getLayoutData();
		if (gd instanceof GridData) {
			((GridData)gd).widthHint= getButtonWidthHint(button);
			((GridData)gd).horizontalAlignment = GridData.FILL;
		}
	}

	/**
	 * Creates and returns a new push button with the given
	 * label and/or image.
	 *
	 * @param parent parent control
	 * @param label button label or <code>null</code>
	 * @param image image of <code>null</code>
	 *
	 * @return a new push button
	 */
	private static Button createPushButton(Composite parent, String label, Image image) {
		Button button = new Button(parent, SWT.PUSH);
		button.setFont(parent.getFont());
		if (image != null) {
			button.setImage(image);
		}
		if (label != null) {
			button.setText(label);
		}
		GridData gd = new GridData();
		button.setLayoutData(gd);
		setButtonDimensionHint(button);
		return button;
	}

	/**
	 * Creates and returns a new push button with the given
	 * label, tooltip and/or image.
	 *
	 * @param parent parent control
	 * @param label button label or <code>null</code>
	 * @param tooltip the tooltip text for the button or <code>null</code>
	 * @param image image of <code>null</code>
	 *
	 * @return a new push button
	 * @since 3.25
	 */
	static Button createPushButton(Composite parent, String label, String tooltip, Image image) {
		Button button = createPushButton(parent, label, image);
		button.setToolTipText(tooltip);
		return button;
	}

	/**
	 * Creates a new label widget
	 * @param parent the parent composite to add this label widget to
	 * @param text the text for the label
	 * @param hspan the horizontal span to take up in the parent composite
	 * @return the new label
	 * @since 3.25
	 *
	 */
	static Label createLabel(Composite parent, String text, int hspan) {
		Label l = new Label(parent, SWT.NONE);
		l.setFont(parent.getFont());
		l.setText(text);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = hspan;
		gd.grabExcessHorizontalSpace = false;
		l.setLayoutData(gd);
		return l;
	}

}
