/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.DialogPageContextComputer;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;

import org.eclipse.jdt.internal.ui.text.java.ExperimentalPreference;
	
/*
 * The page for setting 'work in progress' features preferences.
 */
public class WorkInProgressPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {


	public static final String PREF_SYNC_OUTLINE_ON_CURSOR_MOVE= "JavaEditor.SyncOutlineOnCursorMove"; //$NON-NLS-1$
	public static final String PREF_SHOW_TEMP_PROBLEMS= "JavaEditor.ShowTemporaryProblem"; //$NON-NLS-1$

	public WorkInProgressPreferencePage() {
		super(GRID);

		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(JavaUIMessages.getString("WorkInProgressPreferencePage.description")); //$NON-NLS-1$
	}

	public static void initDefaults(IPreferenceStore store) {
		store.setDefault(PREF_SYNC_OUTLINE_ON_CURSOR_MOVE, false);
	}
	
	/*
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);

		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.WORK_IN_PROGRESS_PREFERENCE_PAGE);
	}
	
	protected void createFieldEditors() {
		Composite parent= getFieldEditorParent();

		BooleanFieldEditor boolEditor;
		boolEditor= new BooleanFieldEditor(
			PREF_SYNC_OUTLINE_ON_CURSOR_MOVE,
			"Synchronize &outline selection on cursor move", //$NON-NLS-1$
			parent
        );
		addField(boolEditor);
	}
	
	/*
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
	public static boolean synchronizeOutlineOnCursorMove() {
		return JavaPlugin.getDefault().getPreferenceStore().getBoolean(PREF_SYNC_OUTLINE_ON_CURSOR_MOVE);
	}
	
	public static boolean showTempProblems() {
		return JavaPlugin.getDefault().getPreferenceStore().getBoolean(PREF_SHOW_TEMP_PROBLEMS);
	}
	
	/*
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		JavaPlugin.getDefault().savePluginPreferences();
		return super.performOk();
	}

}


