/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// AW
package org.eclipse.jdt.internal.ui.preferences;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringSavePreferences;

import org.eclipse.jdt.ui.PreferenceConstants;
	
/*
 * The page for setting general java plugin preferences.
 * See PreferenceConstants to access or change these values through public API.
 */
public class JavaBasePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String OPEN_TYPE_HIERARCHY= PreferenceConstants.OPEN_TYPE_HIERARCHY;
	private static final String OPEN_TYPE_HIERARCHY_IN_PERSPECTIVE= PreferenceConstants.OPEN_TYPE_HIERARCHY_IN_PERSPECTIVE;
	private static final String OPEN_TYPE_HIERARCHY_IN_VIEW_PART= PreferenceConstants.OPEN_TYPE_HIERARCHY_IN_VIEW_PART;

	private static final String DOUBLE_CLICK= PreferenceConstants.DOUBLE_CLICK;
	private static final String DOUBLE_CLICK_GOES_INTO= PreferenceConstants.DOUBLE_CLICK_GOES_INTO;
	private static final String DOUBLE_CLICK_EXPANDS= PreferenceConstants.DOUBLE_CLICK_EXPANDS;

	private ArrayList fCheckBoxes;
	private ArrayList fRadioButtons;
	private ArrayList fTextControls;
	
	public JavaBasePreferencePage() {
		super();
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(PreferencesMessages.getString("JavaBasePreferencePage.description")); //$NON-NLS-1$
	
		fRadioButtons= new ArrayList();
		fCheckBoxes= new ArrayList();
		fTextControls= new ArrayList();
	}

	/*
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}		
	
	/*
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.JAVA_BASE_PREFERENCE_PAGE);
	}	

	private Button addRadioButton(Composite parent, String label, String key, String value) { 
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		
		Button button= new Button(parent, SWT.RADIO);
		button.setText(label);
		button.setData(new String[] { key, value });
		button.setLayoutData(gd);

		button.setSelection(value.equals(getPreferenceStore().getString(key)));
		
		fRadioButtons.add(button);
		return button;
	}
	
	private Button addCheckBox(Composite parent, String label, String key) { 
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		
		Button button= new Button(parent, SWT.CHECK);
		button.setText(label);
		button.setData(key);
		button.setLayoutData(gd);

		button.setSelection(getPreferenceStore().getBoolean(key));
		
		fCheckBoxes.add(button);
		return button;
	}
	
	protected Control createContents(Composite parent) {
		initializeDialogUnits(parent);
		
		Composite result= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth= 0;
		layout.verticalSpacing= convertVerticalDLUsToPixels(10);
		layout.horizontalSpacing= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		result.setLayout(layout);
		
		// new Label(composite, SWT.NONE); // spacer
		// Group linkSettings= new Group(result, SWT.NONE);
		// linkSettings.setLayout(new GridLayout());
		// linkSettings.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		// linkSettings.setText(PreferencesMessages.getString("JavaBasePreferencePage.linkSettings.text")); //$NON-NLS-1$
		//addCheckBox(linkSettings, PreferencesMessages.getString("JavaBasePreferencePage.linkJavaBrowsingViewsCheckbox.text"), LINK_BROWSING_VIEW_TO_EDITOR); //$NON-NLS-1$
		//addCheckBox(linkSettings, PreferencesMessages.getString("JavaBasePreferencePage.linkPackageView"), LINK_PACKAGES_TO_EDITOR); //$NON-NLS-1$
		//addCheckBox(linkSettings, PreferencesMessages.getString("JavaBasePreferencePage.linkTypeHierarchy"), LINK_TYPEHIERARCHY_TO_EDITOR); //$NON-NLS-1$

		// new Label(result, SWT.NONE); // spacer

		Group doubleClickGroup= new Group(result, SWT.NONE);
		doubleClickGroup.setLayout(new GridLayout());		
		doubleClickGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		doubleClickGroup.setText(PreferencesMessages.getString("JavaBasePreferencePage.doubleclick.action"));  //$NON-NLS-1$
		addRadioButton(doubleClickGroup, PreferencesMessages.getString("JavaBasePreferencePage.doubleclick.gointo"), DOUBLE_CLICK, DOUBLE_CLICK_GOES_INTO); //$NON-NLS-1$
		addRadioButton(doubleClickGroup, PreferencesMessages.getString("JavaBasePreferencePage.doubleclick.expand"), DOUBLE_CLICK, DOUBLE_CLICK_EXPANDS); //$NON-NLS-1$

		// new Label(result, SWT.NONE); // spacer
		
		Group typeHierarchyGroup= new Group(result, SWT.NONE);
		typeHierarchyGroup.setLayout(new GridLayout());		
		typeHierarchyGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		typeHierarchyGroup.setText(PreferencesMessages.getString("JavaBasePreferencePage.openTypeHierarchy")); //$NON-NLS-1$
		addRadioButton(typeHierarchyGroup, PreferencesMessages.getString("JavaBasePreferencePage.inPerspective"), OPEN_TYPE_HIERARCHY, OPEN_TYPE_HIERARCHY_IN_PERSPECTIVE);  //$NON-NLS-1$
		addRadioButton(typeHierarchyGroup, PreferencesMessages.getString("JavaBasePreferencePage.inView"), OPEN_TYPE_HIERARCHY, OPEN_TYPE_HIERARCHY_IN_VIEW_PART); //$NON-NLS-1$

		Group refactoringGroup= new Group(result, SWT.NONE);
		refactoringGroup.setLayout(new GridLayout());		
		refactoringGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		refactoringGroup.setText(PreferencesMessages.getString("JavaBasePreferencePage.refactoring.title")); //$NON-NLS-1$
		addCheckBox(refactoringGroup, 
			PreferencesMessages.getString("JavaBasePreferencePage.refactoring.auto_save"), //$NON-NLS-1$
			RefactoringSavePreferences.PREF_SAVE_ALL_EDITORS);

		Group group= new Group(result, SWT.NONE);
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(PreferencesMessages.getString("JavaBasePreferencePage.search")); //$NON-NLS-1$
		
		addCheckBox(group, PreferencesMessages.getString("JavaBasePreferencePage.search.small_menu"), PreferenceConstants.SEARCH_USE_REDUCED_MENU); //$NON-NLS-1$

		Dialog.applyDialogFont(result);
		return result;
	}
		
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		IPreferenceStore store= getPreferenceStore();
		for (int i= 0; i < fCheckBoxes.size(); i++) {
			Button button= (Button) fCheckBoxes.get(i);
			String key= (String) button.getData();
			button.setSelection(store.getDefaultBoolean(key));
		}
		for (int i= 0; i < fRadioButtons.size(); i++) {
			Button button= (Button) fRadioButtons.get(i);
			String[] info= (String[]) button.getData();
			button.setSelection(info[1].equals(store.getDefaultString(info[0])));
		}
		for (int i= 0; i < fTextControls.size(); i++) {
			Text text= (Text) fTextControls.get(i);
			String key= (String) text.getData();
			text.setText(store.getDefaultString(key));
		}
		super.performDefaults();
	}

	/*
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		IPreferenceStore store= getPreferenceStore();
		for (int i= 0; i < fCheckBoxes.size(); i++) {
			Button button= (Button) fCheckBoxes.get(i);
			String key= (String) button.getData();
			store.setValue(key, button.getSelection());
		}
		for (int i= 0; i < fRadioButtons.size(); i++) {
			Button button= (Button) fRadioButtons.get(i);
			if (button.getSelection()) {
				String[] info= (String[]) button.getData();
				store.setValue(info[0], info[1]);
			}
		}
		for (int i= 0; i < fTextControls.size(); i++) {
			Text text= (Text) fTextControls.get(i);
			String key= (String) text.getData();
			store.setValue(key, text.getText());
		}
		
		JavaPlugin.getDefault().savePluginPreferences();
		return super.performOk();
	}


}


