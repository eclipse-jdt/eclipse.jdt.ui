/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringPreferences;

public class RefactoringPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private static final String FATAL_SEVERITY= PreferenceConstants.REFACTOR_FATAL_SEVERITY;
	private static final String ERROR_SEVERITY= PreferenceConstants.REFACTOR_ERROR_SEVERITY;
	private static final String WARNING_SEVERITY= PreferenceConstants.REFACTOR_WARNING_SEVERITY;
	private static final String INFO_SEVERITY= PreferenceConstants.REFACTOR_INFO_SEVERITY;
		
	public RefactoringPreferencePage() {
		super(GRID);
		setDescription(RefactoringMessages.getString("RefactoringPreferencePage.description")); //$NON-NLS-1$
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
	}
			
	public void createControl(Composite parent) {
		// added for 1GEUGE6: ITPJUI:WIN2000 - Help is the same on all preference pages
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.REFACTORING_PREFERENCE_PAGE);
	}		

	public void createFieldEditors() {
    	addField(createSeverityLevelField(getFieldEditorParent()));
    	addField(createSaveAllField(getFieldEditorParent()));
    }
	
	private FieldEditor createSeverityLevelField(Composite parent) {
		RadioGroupFieldEditor editor= new RadioGroupFieldEditor(
			RefactoringPreferences.PREF_ERROR_PAGE_SEVERITY_THRESHOLD,
			RefactoringMessages.getString("RefactoringPreferencePage.show_error_page"), //$NON-NLS-1$
			1,
			new String[] [] {
				{ RefactoringMessages.getString("RefactoringPreferencePage.fatal_error"), FATAL_SEVERITY }, //$NON-NLS-1$
				{ RefactoringMessages.getString("RefactoringPreferencePage.error"), ERROR_SEVERITY }, //$NON-NLS-1$
				{ RefactoringMessages.getString("RefactoringPreferencePage.warning"), WARNING_SEVERITY }, //$NON-NLS-1$
				{ RefactoringMessages.getString("RefactoringPreferencePage.info"), INFO_SEVERITY } //$NON-NLS-1$
			},
			parent, true
			);
		return editor;	
	}
	
	private FieldEditor createSaveAllField(Composite parent) {
		BooleanFieldEditor editor= new BooleanFieldEditor(
		RefactoringPreferences.PREF_SAVE_ALL_EDITORS,
			RefactoringMessages.getString("RefactoringPreferencePage.auto_save"), //$NON-NLS-1$
			BooleanFieldEditor.DEFAULT,
			parent);
		return editor;
	}
	
	public void init(IWorkbench workbench) {
	}
	
	public boolean performOk() {
		JavaPlugin.getDefault().savePluginPreferences();
		return super.performOk();
	}
}
