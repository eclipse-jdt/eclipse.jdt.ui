/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.DialogPageContextComputer;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.browsing.JavaBrowsingMessages;

/*
 * The page for setting the Java Browsing preferences.
 */
public class JavaBrowsingPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String LINK_VIEW_TO_EDITOR= "org.eclipse.jdt.ui.browsing.linktoeditor"; //$NON-NLS-1$
	public static final String STACK_VERTICALLY= "org.eclipse.jdt.ui.browsing.stackVertically"; //$NON-NLS-1$


	public JavaBrowsingPreferencePage() {
		super(GRID);

		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(JavaBrowsingMessages.getString("JavaBrowsingPreferencePage.description")); //$NON-NLS-1$
	}

	public static void initDefaults(IPreferenceStore store) {
		store.setDefault(LINK_VIEW_TO_EDITOR, true);
		store.setDefault(STACK_VERTICALLY, false);
	}

	public static boolean linkViewSelectionToEditor() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return store.getBoolean(LINK_VIEW_TO_EDITOR);
	}

	public static boolean stackHorizontal() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return !store.getBoolean(STACK_VERTICALLY);
	}

	/*
	 * Method declared on PreferencePage
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.JAVA_BROWSING_PREFERENCE_PAGE);
	}
	
	protected void createFieldEditors() {
		Composite parent= getFieldEditorParent();

		BooleanFieldEditor boolEditor= new BooleanFieldEditor(
			LINK_VIEW_TO_EDITOR,
			JavaBrowsingMessages.getString("JavaBrowsingPreferencePage.linkViewToEditor"), //$NON-NLS-1$
			parent
        );
		addField(boolEditor);

		boolEditor= new BooleanFieldEditor(
			STACK_VERTICALLY,
			JavaBrowsingMessages.getString("JavaBrowsingPreferencePage.stackVertically"), //$NON-NLS-1$
			parent
        );
		addField(boolEditor);
	}

	/*
	 * Method declared on IWorkbenchPreferencePage
	 */
	public void init(IWorkbench workbench) {
	}
}
