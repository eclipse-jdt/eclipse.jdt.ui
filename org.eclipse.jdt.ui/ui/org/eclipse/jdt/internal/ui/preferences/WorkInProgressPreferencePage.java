/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;

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

	public WorkInProgressPreferencePage() {
		super(GRID);

		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(JavaUIMessages.getString("WorkInProgressPreferencePage.description")); //$NON-NLS-1$
	}

	public static void initDefaults(IPreferenceStore store) {
		store.setDefault(ExperimentalPreference.CODE_ASSIST_EXPERIMENTAL, true);
	}
	
	/*
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);

		WorkbenchHelp.setHelp(getControl(), new DialogPageContextComputer(this, IJavaHelpContextIds.WORK_IN_PROGRESS_PREFERENCE_PAGE));
	}
	
	protected void createFieldEditors() {
		Composite parent= getFieldEditorParent();

		BooleanFieldEditor boolEditor= new BooleanFieldEditor(
			ExperimentalPreference.CODE_ASSIST_EXPERIMENTAL,
			JavaUIMessages.getString("WorkInProgressPreferencePage.codeassist.experimental"), //$NON-NLS-1$
			parent
        );
		addField(boolEditor);
	}
	
	/*
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
}


