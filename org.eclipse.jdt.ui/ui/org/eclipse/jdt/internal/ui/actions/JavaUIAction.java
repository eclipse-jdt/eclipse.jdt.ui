/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
// AW
package org.eclipse.jdt.internal.ui.actions;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * @deprecated Use Action instead
 */
public abstract class JavaUIAction extends Action {
	
	private static final String LABEL_KEY= "label"; //$NON-NLS-1$
	private static final String DESCRIPTION_KEY= "description"; //$NON-NLS-1$
	private static final String TOOLTIP_KEY= "tooltip";	 //$NON-NLS-1$

	/**
	 * Creates a new action with the given label.
	 * @deprecated Use Action(String) instead.
	 */
	public JavaUIAction(String label) {
		super(label);
	}

	/**
	 * Creates a new action with the given label and image.
	 * @deprecated Use Action(String, ImageDescriptor) instead.
	 */
	public JavaUIAction(String label, ImageDescriptor image) {
		super(label, image);
	}

	/**
	 * Creates a new action. The action's label, description and
	 * tooltip text is retrieved from the given resource bundle.
	 * Use keys are:
	 * <ul>
	 *  <li> label: for the action's label.
	 *  <li> description: for the action's description.
	 *  <li> tooltip: for the action's tooltip text.
	 * </ul>
	 * 
	 * @deprecated Use Action(String, ImageDescriptor) or Action(String) instead.
	 */
	public JavaUIAction(ResourceBundle bundle, String prefix) {
		this(bundle.getString(prefix + LABEL_KEY));
		try {
			setDescription(bundle.getString(prefix + DESCRIPTION_KEY));
		} catch (MissingResourceException e) {
		}

		try {
			setToolTipText(bundle.getString(prefix + TOOLTIP_KEY));
		} catch (MissingResourceException e) {
		}
	}
	
	public final void actionPerformed(Window window) {
	}
	
	/**
	 * @deprecated Use JavaPluginImages.setImageDescriptors(...) instead
	 */
	public void setImageDescriptors(String type, String name) {
		JavaPluginImages.setImageDescriptors(this, type, name);
	}
}
