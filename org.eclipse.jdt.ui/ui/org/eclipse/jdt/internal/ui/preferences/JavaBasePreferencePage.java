/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IPreferencesConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/*
 * The page for setting java plugin preferences.
 */
public class JavaBasePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private static final String KEY_DESCRIPTION= "JavaBasePreferencePage.description";
	private static final String KEY_LINKING= "JavaBasePreferencePage.linkSelection";
	private static final String KEY_OPEN_TYPE_DIALOG= "JavaBasePreferencePage.openTypeDialog";
	private static final String KEY_USE_SRCBIN_FOLDERS= "JavaBasePreferencePage.useSrcBinFoldersInNewProj";

	public JavaBasePreferencePage() {
		super(GRID);
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(JavaPlugin.getResourceString(KEY_DESCRIPTION));
	}

	public static void initDefaults(IPreferenceStore store) {
		store.setDefault(IPreferencesConstants.LINK_PACKAGES_TO_EDITOR, true);
		store.setDefault(IPreferencesConstants.OPEN_TYPE_DIALOG_OPEN_TYPE_HIERARCHY_PERSPECTIVE, true);
		store.setDefault(IPreferencesConstants.SRCBIN_FOLDERS_IN_NEWPROJ, true);
	}

	protected void createFieldEditors() {
		Composite parent= getFieldEditorParent();

		BooleanFieldEditor boolEditor= new BooleanFieldEditor(
			IPreferencesConstants.LINK_PACKAGES_TO_EDITOR,
			JavaPlugin.getResourceString(KEY_LINKING),
			parent
        );
		addField(boolEditor);
		
		boolEditor= new BooleanFieldEditor(
			IPreferencesConstants.OPEN_TYPE_DIALOG_OPEN_TYPE_HIERARCHY_PERSPECTIVE,
			JavaPlugin.getResourceString(KEY_OPEN_TYPE_DIALOG),
			parent
		);
		addField(boolEditor);
		
		boolEditor= new BooleanFieldEditor(
			IPreferencesConstants.SRCBIN_FOLDERS_IN_NEWPROJ,
			JavaPlugin.getResourceString(KEY_USE_SRCBIN_FOLDERS),
			parent
		);
		addField(boolEditor);	
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
	
	
}


