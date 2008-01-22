/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ResourceAction;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;


/**
 * Action to hide and show the editor breadcrumb.

 * @since 3.4
 */
public class ToggleBreadcrumbAction extends ResourceAction implements IPropertyChangeListener {

	private IPreferenceStore fStore;

	/**
	 * Constructs and updates the action.
	 */
	public ToggleBreadcrumbAction() {
		super(JavaEditorMessages.getBundleForConstructedKeys(), "ToggleBreadcrumbAction.", IAction.AS_CHECK_BOX); //$NON-NLS-1$
		JavaPluginImages.setToolImageDescriptors(this, "toggle_breadcrumb.gif"); //$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.TOGGLE_BREADCRUMB_ACTION);
		update();
	}

	/*
	 * @see IAction#actionPerformed
	 */
	public void run() {
		fStore.setValue(PreferenceConstants.EDITOR_SHOW_BREADCRUMB, isChecked());
	}

	/*
	 * @see TextEditorAction#update
	 */
	public void update() {
		
		if (fStore == null) {
			fStore= JavaPlugin.getDefault().getPreferenceStore();
			fStore.addPropertyChangeListener(this);
		}

		setChecked(fStore.getBoolean(PreferenceConstants.EDITOR_SHOW_BREADCRUMB));
		setEnabled(true);
	}

	/*
	 * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(PreferenceConstants.EDITOR_SHOW_BREADCRUMB))
			setChecked(Boolean.valueOf(event.getNewValue().toString()).booleanValue());
	}

	/**
	 * Dispose this action
	 */
	public void dispose() {
		if (fStore != null) {
			fStore.removePropertyChangeListener(this);
			fStore= null;
		}
	}
}
