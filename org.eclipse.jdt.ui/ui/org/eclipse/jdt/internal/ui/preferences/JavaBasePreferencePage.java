/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.swt.widgets.Composite;import org.eclipse.jface.preference.BooleanFieldEditor;import org.eclipse.jface.preference.FieldEditorPreferencePage;import org.eclipse.jface.preference.IPreferenceStore;import org.eclipse.jface.preference.RadioGroupFieldEditor;import org.eclipse.ui.IWorkbench;import org.eclipse.ui.IWorkbenchPreferencePage;import org.eclipse.ui.help.DialogPageContextComputer;import org.eclipse.ui.help.WorkbenchHelp;import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.internal.ui.IPreferencesConstants;import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;

/*
 * The page for setting java plugin preferences.
 */
public class JavaBasePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public JavaBasePreferencePage() {
		super(GRID);
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(JavaUIMessages.getString("JavaBasePreferencePage.description")); //$NON-NLS-1$
	}

	public static void initDefaults(IPreferenceStore store) {
		store.setDefault(IPreferencesConstants.LINK_PACKAGES_TO_EDITOR, true);
		store.setDefault(IPreferencesConstants.OPEN_TYPE_HIERARCHY, IPreferencesConstants.OPEN_TYPE_HIERARCHY_IN_VIEW_PART);
		store.setDefault(IPreferencesConstants.SRCBIN_FOLDERS_IN_NEWPROJ, false);		
		store.setDefault(IPreferencesConstants.DOUBLE_CLICK_GOES_INTO, false);		
	}
	
	/**
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		// added for 1GEUGE6: ITPJUI:WIN2000 - Help is the same on all preference pages
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), new DialogPageContextComputer(this, IJavaHelpContextIds.JAVA_BASE_PREFERENCE_PAGE));
	}	
	

	protected void createFieldEditors() {
		Composite parent= getFieldEditorParent();

		BooleanFieldEditor boolEditor= new BooleanFieldEditor(
			IPreferencesConstants.LINK_PACKAGES_TO_EDITOR,
			JavaUIMessages.getString("JavaBasePreferencePage.link"), //$NON-NLS-1$
			parent
        );
		addField(boolEditor);
		
		boolEditor= new BooleanFieldEditor(
			IPreferencesConstants.DOUBLE_CLICK_GOES_INTO,
			JavaUIMessages.getString("JavaBasePreferencePage.dblClick"), //$NON-NLS-1$
			parent
		);
		addField(boolEditor);
		
		boolEditor= new BooleanFieldEditor(
			IPreferencesConstants.SRCBIN_FOLDERS_IN_NEWPROJ,
			JavaUIMessages.getString("JavaBasePreferencePage.folders"), //$NON-NLS-1$
			parent
		);
		addField(boolEditor);
		
	 	RadioGroupFieldEditor editor= new RadioGroupFieldEditor(
 			IPreferencesConstants.OPEN_TYPE_HIERARCHY, 
 			JavaUIMessages.getString("JavaBasePreferencePage.openTypeHierarchy"),  //$NON-NLS-1$
 			1,
 			new String[][] {
 				{JavaUIMessages.getString("JavaBasePreferencePage.inPerspective"), IPreferencesConstants.OPEN_TYPE_HIERARCHY_IN_PERSPECTIVE}, //$NON-NLS-1$
 				{JavaUIMessages.getString("JavaBasePreferencePage.inView"), IPreferencesConstants.OPEN_TYPE_HIERARCHY_IN_VIEW_PART} //$NON-NLS-1$
 			},
           parent);	
		addField(editor);
	}

	public void init(IWorkbench workbench) {
	}	

	public static boolean useSrcAndBinFolders() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return store.getBoolean(IPreferencesConstants.SRCBIN_FOLDERS_IN_NEWPROJ);
	}
	
	public static boolean linkPackageSelectionToEditor() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return store.getBoolean(IPreferencesConstants.LINK_PACKAGES_TO_EDITOR);
	}
		
	public static boolean openTypeHierarchyInPerspective() {
		return IPreferencesConstants.OPEN_TYPE_HIERARCHY_IN_PERSPECTIVE.equals(
			JavaPlugin.getDefault().getPreferenceStore().getString(IPreferencesConstants.OPEN_TYPE_HIERARCHY));
	}
	
	public static boolean openTypeHierarchInViewPart() {
		return IPreferencesConstants.OPEN_TYPE_HIERARCHY_IN_VIEW_PART.equals(
			JavaPlugin.getDefault().getPreferenceStore().getString(IPreferencesConstants.OPEN_TYPE_HIERARCHY));
	}
	
	public static boolean doubleClockGoesInto() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return store.getBoolean(IPreferencesConstants.DOUBLE_CLICK_GOES_INTO);
	}

}


