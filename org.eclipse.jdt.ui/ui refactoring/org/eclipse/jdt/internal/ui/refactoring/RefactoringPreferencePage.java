/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.widgets.Composite;import org.eclipse.jface.preference.BooleanFieldEditor;import org.eclipse.jface.preference.FieldEditor;import org.eclipse.jface.preference.FieldEditorPreferencePage;import org.eclipse.jface.preference.IPreferenceStore;import org.eclipse.jface.preference.RadioGroupFieldEditor;import org.eclipse.ui.IWorkbench;import org.eclipse.ui.IWorkbenchPreferencePage;import org.eclipse.ui.help.DialogPageContextComputer;import org.eclipse.ui.help.WorkbenchHelp;import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

public class RefactoringPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public RefactoringPreferencePage() {
		super(GRID);
	}
	
	public static void initDefaults(IPreferenceStore store) {
		store.setDefault(RefactoringPreferences.PREF_ERROR_PAGE_SEVERITY_THRESHOLD, RefactoringPreferences.ERROR_SEVERITY);
		store.setDefault(RefactoringPreferences.PREF_JAVA_STYLE_GUIDE_CONFORM, true);
		store.setDefault(RefactoringPreferences.PREF_SAVE_ALL_EDITORS, false);
	}
	
	protected IPreferenceStore doGetPreferenceStore() {
		return JavaPlugin.getDefault().getPreferenceStore();
	}
	
	/**
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		// added for 1GEUGE6: ITPJUI:WIN2000 - Help is the same on all preference pages
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), new DialogPageContextComputer(this, IJavaHelpContextIds.REFACTORING_PREFERENCE_PAGE));
	}		

	public void createFieldEditors() {
		Composite parent= getFieldEditorParent();
		addField(createSeverityLevelField(parent));
		addField(createSaveAllField(parent));
	}
	
	private FieldEditor createSeverityLevelField(Composite parent){
			RadioGroupFieldEditor editor= new RadioGroupFieldEditor(
			RefactoringPreferences.PREF_ERROR_PAGE_SEVERITY_THRESHOLD,
			RefactoringMessages.getString("RefactoringPreferencePage.show_error_page"), //$NON-NLS-1$
			1,
			new String[] [] {
				{ RefactoringMessages.getString("RefactoringPreferencePage.fatal_error"), 			RefactoringPreferences.FATAL_SEVERITY }, //$NON-NLS-1$
				{ RefactoringMessages.getString("RefactoringPreferencePage.error"), 			RefactoringPreferences.ERROR_SEVERITY }, //$NON-NLS-1$
				{ RefactoringMessages.getString("RefactoringPreferencePage.warning"), 		RefactoringPreferences.WARNING_SEVERITY }, //$NON-NLS-1$
				{ RefactoringMessages.getString("RefactoringPreferencePage.info"),	RefactoringPreferences.INFO_SEVERITY } //$NON-NLS-1$
			},
			parent
			);
		return editor;	
	}
	
	private FieldEditor createSaveAllField(Composite parent){
		BooleanFieldEditor editor= new BooleanFieldEditor(
		RefactoringPreferences.PREF_SAVE_ALL_EDITORS,
			RefactoringMessages.getString("RefactoringPreferencePage.auto_save"), //$NON-NLS-1$
			BooleanFieldEditor.DEFAULT,
			parent);
		return editor;
	}
	
	public void init(IWorkbench workbench) {
	}	
}
