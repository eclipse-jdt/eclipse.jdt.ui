/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.launcher;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.launching.JavaRuntime;

/*
 * The page for setting the default java runtime preference.
 */
public class VMPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {	
	public static final String PREFIX= "org.eclipse.jdt.ui.launcher.";
	public static final String PREF_SHOW_ARGS= PREFIX+"show_args";
	public static final String DEFAULT_VM_LABEL= "launcher.default_vm.label";
	public static final String PREF_VM= PREFIX+"default_vm";

	public VMPreferencePage() {
		super(GRID);
		
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		setPreferenceStore(store);
	}
	
	/**
	 * @see FieldEditorPreferencePage#createFieldEditors
	 */
	protected void createFieldEditors() {
		Composite parent= getFieldEditorParent();
		StringChoiceFieldEditor fe= new StringChoiceFieldEditor(PREF_VM, JavaLaunchUtils.getResourceString(DEFAULT_VM_LABEL), parent);
		String[] vms= JavaRuntime.getJavaRuntimes();
		for (int i= 0; i < vms.length; i++) {
			String vm= vms[i];
			fe.addItem(vm, vm);
		}
		addField(fe);
	}
	
	/**
	 * @see IWorkbenchPreferencePage#init
	 */
	public void init(IWorkbench workbench) {
	}
}
