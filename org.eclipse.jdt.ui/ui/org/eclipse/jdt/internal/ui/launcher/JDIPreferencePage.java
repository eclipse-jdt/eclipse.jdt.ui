/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.launcher;

import java.io.File;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.preferences.MinMaxIntegerFieldEditor;import org.eclipse.jface.preference.FieldEditorPreferencePage;import org.eclipse.jface.preference.IPreferenceStore;import org.eclipse.swt.widgets.Composite;import org.eclipse.ui.IWorkbench;import org.eclipse.ui.IWorkbenchPreferencePage;

/*
 * The page for setting java concole preferences.
 */
public class JDIPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {	
	
	public static final String PREF_TIMEOUT= "org.eclipse.jdt.ui.launcher.jdi.timeout";
	private MinMaxIntegerFieldEditor fTimeout;
	
	public JDIPreferencePage() {
		super(GRID);
		
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		setPreferenceStore(store);
	}
	


	/**
	 * @see FieldEditorPreferencePage#createFieldEditors
	 */
	protected void createFieldEditors() {
		Composite parent= getFieldEditorParent();
		fTimeout= new MinMaxIntegerFieldEditor(PREF_TIMEOUT, "Request Timeout:", parent);
		fTimeout.setMinimumValue(500);
		addField(fTimeout);
	}
	
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible)
			fTimeout.setFocus();
	}
	
	public static void initDefaults(IPreferenceStore store) {
		store.setDefault(PREF_TIMEOUT, 3000);
	}
	
	/**
	 * @see IWorkbenchPreferencePage#init
	 */
	public void init(IWorkbench workbench) {
	}
}
