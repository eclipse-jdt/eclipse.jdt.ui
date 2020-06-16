/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.JFaceResources;


public class LayoutUtil {

	/*
	 * Sets the span of a control. Assumes that MGridData is used.
	 */
	public static void setHorizontalSpan(Control control, int span) {
		Object ld= control.getLayoutData();
		if (ld instanceof GridData) {
			((GridData)ld).horizontalSpan= span;
		} else if (span != 1) {
			GridData gd= new GridData();
			gd.horizontalSpan= span;
			control.setLayoutData(gd);
		}
	}

	/*
	 * Creates a spacer control with the given span.
	 * The composite is assumed to have <code>MGridLayout</code> as
	 * layout.
	 * @param parent The parent composite
	 */
	public static Control createEmptySpace(Composite parent, int span) {
		Label label= new Label(parent, SWT.LEFT);
		GridData gd= new GridData();
		gd.horizontalAlignment= GridData.BEGINNING;
		gd.grabExcessHorizontalSpace= false;
		gd.horizontalSpan= span;
		gd.horizontalIndent= 0;
		gd.widthHint= 0;
		gd.heightHint= 0;
		label.setLayoutData(gd);
		return label;
	}


	/**
	 * Returns a width hint for a button control.
	 * @param button	the button for which to set the dimension hint
	 * @return the width hint
	 */
	public static int getButtonWidthHint(Button button) {
		button.setFont(JFaceResources.getDialogFont());
		PixelConverter converter= new PixelConverter(button);
		int widthHint= converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		return Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
	}

	/**
	 * Sets width and height hint for the button control.
	 * <b>Note:</b> This is a NOP if the button's layout data is not
	 * an instance of <code>GridData</code>.
	 *
	 * @param button	the button for which to set the dimension hint
	 */
	public static void setButtonDimensionHint(Button button) {
		Assert.isNotNull(button);
		Object gd= button.getLayoutData();
		if (gd instanceof GridData) {
			((GridData)gd).widthHint= getButtonWidthHint(button);
			((GridData)gd).horizontalAlignment = GridData.FILL;
		}
	}

	/**
	 * Sets the horizontal indent of a dependent control. Assumes that GridData is used.
	 *
	 * @param control the control
	 */
	public static void setHorizontalIndent(Control control) {
		Object ld= control.getLayoutData();
		if (ld instanceof GridData) {
			((GridData) ld).horizontalIndent= LayoutUtil.getIndent();
		}
	}

	/**
	 * Returns the indent of dependent controls, in pixels.
	 * <p>
	 * <strong>Note:</strong> Use this method instead of {@link LayoutConstants#getIndent()} for
	 * compatibility reasons.
	 * </p>
	 *
	 * @return the indent of dependent controls, in pixels.
	 */
	public static final int getIndent() {
		return LayoutConstants.getIndent();
	}

	/**
	 * Sets the horizontal grabbing of a control to true. Assumes that GridData is used.
	 *
	 * @param control the control
	 */
	public static void setHorizontalGrabbing(Control control) {
		Object ld= control.getLayoutData();
		if (ld instanceof GridData) {
			((GridData) ld).grabExcessHorizontalSpace= true;
		}
	}

	/**
	 * Sets the vertical grabbing of a control to true. Assumes that GridData is used.
	 *
	 * @param control the control
	 *
	 */
	public static void setVerticalGrabbing(Control control) {
		Object ld= control.getLayoutData();
		if (ld instanceof GridData) {
			GridData gd= ((GridData) ld);
			gd.grabExcessVerticalSpace= true;
			gd.verticalAlignment= SWT.FILL;
		}
	}

	/**
	 * Sets the width hint of a control. Assumes that GridData is used.
	 *
	 * @param control the control
	 * @param widthHint the preferred width in pixels
	 */
	public static void setWidthHint(Control control, int widthHint) {
		Object ld= control.getLayoutData();
		if (ld instanceof GridData) {
			((GridData) ld).widthHint= widthHint;
		}
	}

	/**
	 * Sets the heightHint hint of a control. Assumes that GridData is used.
	 *
	 * @param control the control
	 * @param heightHint the preferred height in pixels
	 */
	public static void setHeightHint(Control control, int heightHint) {
		Object ld= control.getLayoutData();
		if (ld instanceof GridData) {
			((GridData) ld).heightHint= heightHint;
		}
	}

	private LayoutUtil() {
	}
}
