/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jdt.internal.ui.JavaPlugin;

class PotentialMatchDialog extends MessageDialog {
	
	// String constants for widgets
	private static String MESSAGE= SearchMessages.getString("Search.potentialMatchDialog.message"); //$NON-NLS-1$
	private static String CHECKBOX_TEXT= SearchMessages.getString("Search.potentialMatchDialog.dontShowAgain"); //$NON-NLS-1$

	public static final String EXTENSION_POINT_ID= "org.eclipse.jdt.ui.JavaSearchPage"; //$NON-NLS-1$

	// Dialog store id constants
	private final static String STORE_ID= "PotentialMatchDialog"; //$NON-NLS-1$
	private final static String STORE_HIDE_POTENTIAL_MATCH_DIALOG= STORE_ID + ".hide"; //$NON-NLS-1$
	
	private Button fHideDialogCheckBox;
		
	PotentialMatchDialog(Shell parentShell, int potentialMatchCount) {
		super(
			parentShell,
			createTitle(potentialMatchCount),
			null,
			MESSAGE,
			INFORMATION,
			new String[] { IDialogConstants.OK_LABEL },
			0);
	}

	static void open(Shell parentShell, int potentialMatchCount) {
		new PotentialMatchDialog(parentShell, potentialMatchCount).open();
	}

	protected Control createCustomArea(Composite parent) {
		fHideDialogCheckBox= new Button(parent, SWT.CHECK | SWT.LEFT);
		fHideDialogCheckBox.setText(CHECKBOX_TEXT);
		fHideDialogCheckBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				storeSetting(((Button)e.widget).getSelection());
			}
		}
		);
		return fHideDialogCheckBox;
	}

	//--------------- Configuration handling --------------
	
	/**
	 * Returns this dialog
	 * 
	 * @return the settings to be used
	 */
	private static IDialogSettings getDialogSettings() {
		IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings();
		settings= settings.getSection(STORE_ID);
		if (settings == null) {
			settings= JavaPlugin.getDefault().getDialogSettings().addNewSection(STORE_ID);
			settings.put(STORE_HIDE_POTENTIAL_MATCH_DIALOG, false);
		}
		return settings;
	}
	
	/**
	 * Answers whether a potential match should be explained to the user.
	 */
	static boolean shouldExplain() {
		IDialogSettings settings= getDialogSettings();
		return !settings.getBoolean(STORE_HIDE_POTENTIAL_MATCH_DIALOG);
	}
	
	/**
	 * Stores the current configuration in the dialog store.
	 */
	private void storeSetting(boolean hideDialog) {
		IDialogSettings settings= getDialogSettings();
		settings.put(STORE_HIDE_POTENTIAL_MATCH_DIALOG, hideDialog);
	}

	private static String createTitle(int potentialMatchCount) {
		if (potentialMatchCount == 1)
			return new String(SearchMessages.getString("Search.potentialMatchDialog.title.foundPotentialMatch")); //$NON-NLS-1$
		else
			return new String(SearchMessages.getFormattedString("Search.potentialMatchDialog.title.foundPotentialMatches", "" + potentialMatchCount)); //$NON-NLS-1$
	}
}
