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

	public static final String PREF_PKG_NAME_PATTERN_FOR_PKG_VIEW= "PackagesView.pkgNamePatternForPackagesView"; //$NON-NLS-1$
	public static final String PREF_SYNC_OUTLINE_ON_CURSOR_MOVE= "JavaEditor.SyncOutlineOnCursorMove";


	public WorkInProgressPreferencePage() {
		super(GRID);

		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(JavaUIMessages.getString("WorkInProgressPreferencePage.description")); //$NON-NLS-1$
	}

	public static void initDefaults(IPreferenceStore store) {
		store.setDefault(ExperimentalPreference.CODE_ASSIST_EXPERIMENTAL, false);
		store.setDefault(PREF_PKG_NAME_PATTERN_FOR_PKG_VIEW, "");
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

		BooleanFieldEditor boolEditor= new BooleanFieldEditor(
			ExperimentalPreference.CODE_ASSIST_EXPERIMENTAL,
			JavaUIMessages.getString("WorkInProgressPreferencePage.codeassist.experimental"), //$NON-NLS-1$
			parent
        );
		addField(boolEditor);
        StringFieldEditor stringEditor= new StringFieldEditor(
        	PREF_PKG_NAME_PATTERN_FOR_PKG_VIEW,
			JavaUIMessages.getString("WorkInProgressPreferencePage.packagesView.pkgNamePatternForPackagesView.text"), //$NON-NLS-1$
			parent
        );
        stringEditor.getLabelControl(parent).setToolTipText(JavaUIMessages.getString("WorkInProgressPreferencePage.packagesView.pkgNamePatternForPackagesView.tooltip")); //$NON-NLS-1$
		addField(stringEditor);
		 		
		boolEditor= new BooleanFieldEditor(
			PREF_SYNC_OUTLINE_ON_CURSOR_MOVE,
			"Synchronize &outliner selection on cursor move", //$NON-NLS-1$
			parent
        );
		addField(boolEditor);
	}
		
	static public boolean isCompressingPkgNameInPackagesView() {
		return getPkgNamePatternForPackagesView().length() > 0;
	}
	
	static public String getPkgNamePatternForPackagesView() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return store.getString(PREF_PKG_NAME_PATTERN_FOR_PKG_VIEW);
	}
	
	static public boolean synchronizeOutlineOnCursorMove() {
		return JavaPlugin.getDefault().getPreferenceStore().getBoolean(PREF_SYNC_OUTLINE_ON_CURSOR_MOVE);
	}
	
	/*
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
}


