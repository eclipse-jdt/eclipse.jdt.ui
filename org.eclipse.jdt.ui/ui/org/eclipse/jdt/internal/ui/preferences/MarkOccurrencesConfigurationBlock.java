/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.Assert;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;

/**
 * Configures Java Editor hover preferences.
 * 
 * @since 2.1
 */
class MarkOccurrencesConfigurationBlock {

	private IPreferenceStore fStore;
	
	
	private Map fCheckBoxes= new HashMap();
	private SelectionListener fCheckBoxListener= new SelectionListener() {
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		public void widgetSelected(SelectionEvent e) {
			Button button= (Button) e.widget;
			fStore.setValue((String) fCheckBoxes.get(button), button.getSelection());
		}
	};
	
	/**
	 * List of master/slave listeners when there's a dependency.
	 * 
	 * @see #createDependency(Button, String, Control)
	 * @since 3.0
	 */
	private ArrayList fMasterSlaveListeners= new ArrayList();
	
	private JavaEditorPreferencePage fMainPreferencePage;

	private StatusInfo fStatus;

	public MarkOccurrencesConfigurationBlock(JavaEditorPreferencePage mainPreferencePage, IPreferenceStore store) {
		Assert.isNotNull(mainPreferencePage);
		Assert.isNotNull(store);
		fMainPreferencePage= mainPreferencePage;
		fStore= store;
	}

	/**
	 * Creates page for mark occurrences preferences.
	 * 
	 * @param parent the parent composite
	 * @return the control for the preference page
	 */
	public Control createControl(Composite parent) {
	
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout(); layout.numColumns= 1;
		composite.setLayout(layout);

		String label= PreferencesMessages.getString("MarkOccurrencesConfigurationBlock.markOccurrences"); //$NON-NLS-1$
		Button master= addCheckBox(composite, label, PreferenceConstants.EDITOR_MARK_OCCURRENCES, 0); //$NON-NLS-1$
		
		label= PreferencesMessages.getString("MarkOccurrencesConfigurationBlock.stickyOccurrences"); //$NON-NLS-1$
		Button slave= addCheckBox(composite, label, PreferenceConstants.EDITOR_STICKY_OCCURRENCES, 0); //$NON-NLS-1$
		createDependency(master, PreferenceConstants.EDITOR_STICKY_OCCURRENCES, slave);

		label= PreferencesMessages.getString("MarkOccurrencesConfigurationBlock.markTypeOccurrences"); //$NON-NLS-1$
		slave= addCheckBox(composite, label, PreferenceConstants.EDITOR_MARK_TYPE_OCCURRENCES, 0); //$NON-NLS-1$
		createDependency(master, PreferenceConstants.EDITOR_STICKY_OCCURRENCES, slave);
		
		label= PreferencesMessages.getString("MarkOccurrencesConfigurationBlock.markMethodOccurrences"); //$NON-NLS-1$
		slave= addCheckBox(composite, label, PreferenceConstants.EDITOR_MARK_METHOD_OCCURRENCES, 0); //$NON-NLS-1$
		createDependency(master, PreferenceConstants.EDITOR_MARK_METHOD_OCCURRENCES, slave);
		
		label= PreferencesMessages.getString("MarkOccurrencesConfigurationBlock.markFieldOccurrences"); //$NON-NLS-1$
		slave= addCheckBox(composite, label, PreferenceConstants.EDITOR_MARK_FIELD_OCCURRENCES, 0); //$NON-NLS-1$
		createDependency(master, PreferenceConstants.EDITOR_MARK_FIELD_OCCURRENCES, slave);

		label= PreferencesMessages.getString("MarkOccurrencesConfigurationBlock.markLocalVariableOccurrences"); //$NON-NLS-1$
		slave= addCheckBox(composite, label, PreferenceConstants.EDITOR_MARK_LOCAL_VARIABLE_OCCURRENCES, 0); //$NON-NLS-1$
		createDependency(master, PreferenceConstants.EDITOR_MARK_LOCAL_VARIABLE_OCCURRENCES, slave);

		return composite;
	}
	
	private Button addCheckBox(Composite parent, String label, String key, int indentation) {		
		Button checkBox= new Button(parent, SWT.CHECK);
		checkBox.setText(label);
		
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= indentation;
		gd.horizontalSpan= 2;
		checkBox.setLayoutData(gd);
		checkBox.addSelectionListener(fCheckBoxListener);
		
		fCheckBoxes.put(checkBox, key);
		
		return checkBox;
	}

	private void createDependency(final Button master, String masterKey, final Control slave) {
		indent(slave);
		boolean masterState= fStore.getBoolean(masterKey);
		slave.setEnabled(masterState);
		SelectionListener listener= new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				slave.setEnabled(master.getSelection());
			}

			public void widgetDefaultSelected(SelectionEvent e) {}
		};
		master.addSelectionListener(listener);
		fMasterSlaveListeners.add(listener);
	}

	private static void indent(Control control) {
		GridData gridData= new GridData();
		gridData.horizontalIndent= 20;
		control.setLayoutData(gridData);		
	}

	void initialize() {
		initializeFields();
	}

	void initializeFields() {
		
		Iterator iter= fCheckBoxes.keySet().iterator();
		while (iter.hasNext()) {
			Button b= (Button) iter.next();
			String key= (String) fCheckBoxes.get(b);
			b.setSelection(fStore.getBoolean(key));
		}
		
        // Update slaves
        iter= fMasterSlaveListeners.iterator();
        while (iter.hasNext()) {
            SelectionListener listener= (SelectionListener)iter.next();
            listener.widgetSelected(null);
        }
        
	}

	void performOk() {
	}

	void performDefaults() {
		restoreFromPreferences();
		initializeFields();
	}

	private void restoreFromPreferences() {

	}

	IStatus getStatus() {
		if (fStatus == null)
			fStatus= new StatusInfo();
		return fStatus;
	}

	// don't delete might be used later
	private void updateStatus() {
		if (fStatus.isOK())
			fMainPreferencePage.updateStatus(fStatus);
		else {
			fMainPreferencePage.setValid(false);
			StatusUtil.applyToStatusLine(fMainPreferencePage, fStatus);
		}
	}
}
