/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.resource.JFaceColors;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * A message line displaying a status.
 */
public class MessageLine extends CLabel {
	
	private Color fNormalMsgAreaBackground;

	/**
	 * Creates a new message line as a child of the given parent.
	 */
	public MessageLine(Composite parent) {
		this(parent, SWT.LEFT);
	}

	/**
	 * Creates a new message line as a child of the parent and with the given SWT stylebits.
	 */
	public MessageLine(Composite parent, int style) {
		super(parent, style);
		fNormalMsgAreaBackground= getBackground();
	}

	
	private Image findImage(IStatus status) {
		if (status.isOK()) {
			return null;
		} else if (status.matches(IStatus.ERROR)) {
			return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REFACTORING_ERROR);
		} else if (status.matches(IStatus.WARNING)) {
			return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REFACTORING_WARNING);
		} else if (status.matches(IStatus.INFO)) {
			return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REFACTORING_INFO);
		}
		return null;
	}

	/**
	 * Sets the message and image to the given status.
	 * <code>null</code> is a valid argument and will set the empty text and no image
	 */
	public void setErrorStatus(IStatus status) {
		if (status != null && !status.isOK()) {
			String message= status.getMessage();
			if (message != null && message.length() > 0) {
				setText(message);
				setImage(findImage(status));
				setBackground(JFaceColors.getErrorBackground(getDisplay()));
				return;
			}
		}		
		setText(""); //$NON-NLS-1$	
		setImage(null);
		setBackground(fNormalMsgAreaBackground);	
	}	
}

