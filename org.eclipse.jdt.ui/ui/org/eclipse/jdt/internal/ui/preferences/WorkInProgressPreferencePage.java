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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * Preference page for work in progress.
 */
public class WorkInProgressPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	/** prefix for resources */
	private static final String PREFIX= "WorkInProgress."; //$NON-NLS-1$

	/** 
	 * All FieldEditors except <code>smartTyping</code>, whose enable state
	 * is controlled by the smartTyping preference.
	 */
	private Set fSmartTypingItems= new HashSet();
	
	/** The controlling smartTyping field */
	private BooleanFieldEditor fSmartTyping;

	/**
	 * creates a new preference page.
	 */
	public WorkInProgressPreferencePage() {
		super(GRID);
		setPreferenceStore(getPreferenceStore());
	}

	/*
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), "WORK_IN_PROGRESS_PREFERENCE_PAGE"); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
	 */
	protected void createFieldEditors() {
		fSmartTyping= new BooleanFieldEditor(PreferenceConstants.EDITOR_SMART_TYPING, PreferencesMessages.getString(PREFIX + "smartTyping"), getFieldEditorParent()); //$NON-NLS-1$
		addField(fSmartTyping);
		fSmartTyping.setPropertyChangeListener(this);
		createSpacer(getFieldEditorParent(), 1);

		Label label= new Label(getFieldEditorParent(), SWT.NONE);
		label.setText(PreferencesMessages.getString(PREFIX + "smartTyping.label")); //$NON-NLS-1$

		FieldEditor	editor= new BooleanFieldEditor(PreferenceConstants.EDITOR_SMART_SEMICOLON, PreferencesMessages.getString(PREFIX + "smartTyping.smartSemicolon"), getFieldEditorParent()); //$NON-NLS-1$
		addField(editor);
		fSmartTypingItems.add(editor);
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	protected void createSpacer(Composite composite, int columnSpan) {
		Label label= new Label(composite, SWT.NONE);
		GridData gd= new GridData();
		gd.horizontalSpan= columnSpan;
		label.setLayoutData(gd);
	}

	/**
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		boolean ok= super.performOk();
		JavaPlugin.getDefault().savePluginPreferences();
		return ok;
	}

	/*
	 * @see org.eclipse.jface.preference.PreferencePage#doGetPreferenceStore()
	 */
	protected IPreferenceStore doGetPreferenceStore() {
		return JavaPlugin.getDefault().getPreferenceStore();
	}
	
	/**
	 * Sets all field editors in <code>fSmartTypingItems</code> to <code>enabled</code>.
	 * @param enabled the new state for the smart typing field editors.
	 */
	private void doSetEnableState(boolean enabled) {
		for (Iterator it= fSmartTypingItems.iterator(); it.hasNext();) {
			FieldEditor editor= (FieldEditor) it.next();
			editor.setEnabled(enabled, getFieldEditorParent());
		}
	}

	/*
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		// load defaults
		super.performDefaults();
		// enable depending controls
		doSetEnableState(fSmartTyping.getBooleanValue());
	}

	/*
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getSource() == fSmartTyping) {
			boolean enabled= ((Boolean) event.getNewValue()).booleanValue();
			doSetEnableState(enabled);
		}
		// always feed the chain
		super.propertyChange(event);
	}
}
