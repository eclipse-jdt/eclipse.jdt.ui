/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.cleanup;

import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.fix.CleanUpOptions;
import org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialog;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileStore;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;

public class CleanUpModifyDialog extends ModifyDialog {
	
	/**
	 * Constant array for boolean selection 
	 */
	static String[] FALSE_TRUE = {
		CleanUpOptions.FALSE,
		CleanUpOptions.TRUE
	};
	
	private Label fCountLabel;
	private CleanUpTabPage[] fPages;

	public CleanUpModifyDialog(Shell parentShell, Profile profile, ProfileManager profileManager, ProfileStore profileStore, boolean newProfile, String dialogPreferencesKey, String lastSavePathKey) {
	    super(parentShell, profile, profileManager, profileStore, newProfile, dialogPreferencesKey, lastSavePathKey);
    }

	/**
	 * {@inheritDoc}
	 */
	protected void addPages(final Map values) {
		fPages= JavaPlugin.getDefault().getCleanUpRegistry().getCleanUpTabPages();
		
		for (int i= 0; i < fPages.length; i++) {
			fPages[i].setIsSaveAction(false);
			fPages[i].setWorkingValues(values);
			fPages[i].setModifyListener(this);
		}
		
		for (int i= 0; i < fPages.length; i++) {
			addTabPage(fPages[i].getTitle(), fPages[i]);
		}
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite control= (Composite)super.createDialogArea(parent);
		
		fCountLabel= new Label(control, SWT.NONE);
		fCountLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		fCountLabel.setFont(parent.getFont());
		updateCountLabel();
		
		return control;
	}
	
	public void updateStatus(IStatus status) {
		int count= 0;
		for (int i= 0; i < fPages.length; i++) { 
			count+= fPages[i].getSelectedCleanUpCount();
		}
		if (count == 0) {
			super.updateStatus(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, CleanUpMessages.CleanUpModifyDialog_SelectOne_Error));
		} else {
			super.updateStatus(status);
		}
	}
	
	public void valuesModified() {
		super.valuesModified();
		updateCountLabel();
	}
	
	private void updateCountLabel() {
		int size= 0, count= 0;
		for (int i= 0; i < fPages.length; i++) {
			size+= fPages[i].getCleanUpCount();
			count+= fPages[i].getSelectedCleanUpCount();
		}
		
		fCountLabel.setText(Messages.format(CleanUpMessages.CleanUpModifyDialog_XofYSelected_Label, new Object[] {new Integer(count), new Integer(size)}));
	}
}
