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
public class JDK12PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {	
	
	public static final String PREF_LOCATION= "org.eclipse.jdt.ui.Launcher.SUNJDK.location";
	public static final String PREF_TIMEOUT= "org.eclipse.jdt.ui.Launcher.SUNJDK.timeout";
	protected static final String PREFIX= "launcher.jdk12.preferences.";
	protected static final String DESCRIPTION= PREFIX+ "description";
	protected static final String HOME= PREFIX+"home";
	protected static final String TIMEOUT= PREFIX+"timeout";
	
	
	protected JDKRootFieldEditor fJDKRoot;
	
	public JDK12PreferencePage() {
		super(GRID);
		
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		setPreferenceStore(store);
		
		setDescription(JavaLaunchUtils.getResourceString(DESCRIPTION));
	}
	
	/**
	 * @see FieldEditorPreferencePage#createFieldEditors
	 */
	protected void createFieldEditors() {
		Composite parent= getFieldEditorParent();
		String suffix1= "bin"+File.separator+"java.exe";
		String suffix2= "bin"+File.separator+"java";
		fJDKRoot= new JDKRootFieldEditor(PREF_LOCATION, JavaLaunchUtils.getResourceString(HOME), new String[] { suffix1, suffix2 }, parent);
		addField(fJDKRoot);
		MinMaxIntegerFieldEditor timeout= new MinMaxIntegerFieldEditor(PREF_TIMEOUT, JavaLaunchUtils.getResourceString(TIMEOUT), parent);
		timeout.setMinimumValue(500);
		addField(timeout);
	}
	
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible)
			fJDKRoot.setFocus();
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
