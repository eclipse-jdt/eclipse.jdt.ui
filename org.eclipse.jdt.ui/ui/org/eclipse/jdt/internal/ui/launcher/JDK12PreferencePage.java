/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.launcher;

import java.io.File;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/*
 * The page for setting java concole preferences.
 */
public class JDK12PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {	
	
	public static final String PREF_LOCATION= "org.eclipse.jdt.ui.Launcher.SUNJDK.location";
	protected static final String PREFIX= "launcher.jdk12.preferences.";
	protected static final String DESCRIPTION= PREFIX+ "description";
	
	protected JDKRootFieldEditor fJDKRoot;
	
	public JDK12PreferencePage() {
		super(FLAT);
		
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
		fJDKRoot= new JDKRootFieldEditor(PREF_LOCATION, "Home:", new String[] { suffix1, suffix2 }, parent);
		addField(fJDKRoot);
	}
	
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible)
			fJDKRoot.setFocus();
	}
	
	/**
	 * @see IWorkbenchPreferencePage#init
	 */
	public void init(IWorkbench workbench) {
	}
}
