/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import java.util.ArrayList;
import java.util.Hashtable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.DialogPageContextComputer;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.util.TabFolderLayout;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

/*
 * The page for setting compiler options.
 */
public class CompilerPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	// Preference store keys
	private static final String PREF_LOCAL_VARIABLE_ATTR= JavaCore.COMPILER_LOCAL_VARIABLE_ATTR;
	private static final String PREF_LINE_NUMBER_ATTR= JavaCore.COMPILER_LINE_NUMBER_ATTR;
	private static final String PREF_SOURCE_FILE_ATTR= JavaCore.COMPILER_SOURCE_FILE_ATTR;
	private static final String PREF_CODEGEN_UNUSED_LOCAL= JavaCore.COMPILER_CODEGEN_UNUSED_LOCAL;
	private static final String PREF_CODEGEN_TARGET_PLATFORM= JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM;
	private static final String PREF_PB_UNREACHABLE_CODE= JavaCore.COMPILER_PB_UNREACHABLE_CODE;	
	private static final String PREF_PB_INVALID_IMPORT= JavaCore.COMPILER_PB_INVALID_IMPORT;	
	private static final String PREF_PB_OVERRIDING_PACKAGE_DEFAULT_METHOD= JavaCore.COMPILER_PB_OVERRIDING_PACKAGE_DEFAULT_METHOD;	
	private static final String PREF_PB_METHOD_WITH_CONSTRUCTOR_NAME= JavaCore.COMPILER_PB_METHOD_WITH_CONSTRUCTOR_NAME;
	private static final String PREF_PB_DEPRECATION= JavaCore.COMPILER_PB_DEPRECATION;
	private static final String PREF_PB_HIDDEN_CATCH_BLOCK= JavaCore.COMPILER_PB_HIDDEN_CATCH_BLOCK;
	private static final String PREF_PB_UNUSED_LOCAL= JavaCore.COMPILER_PB_UNUSED_LOCAL;	
	private static final String PREF_PB_UNUSED_PARAMETER= JavaCore.COMPILER_PB_UNUSED_PARAMETER;
	private static final String PREF_PB_SYNTHETIC_ACCESS_EMULATION= JavaCore.COMPILER_PB_SYNTHETIC_ACCESS_EMULATION;

	private static final String DATA_KEY= "#ControlData"; //$NON-NLS-1$

	public static void initDefaults(IPreferenceStore store) {
		Hashtable hashtable= JavaCore.getDefaultOptions();
		
		store.setDefault(PREF_LOCAL_VARIABLE_ATTR, (String)hashtable.get(PREF_LOCAL_VARIABLE_ATTR));
		store.setDefault(PREF_LINE_NUMBER_ATTR, (String)hashtable.get(PREF_LINE_NUMBER_ATTR));
		store.setDefault(PREF_SOURCE_FILE_ATTR, (String)hashtable.get(PREF_SOURCE_FILE_ATTR));
		store.setDefault(PREF_CODEGEN_UNUSED_LOCAL, (String)hashtable.get(PREF_CODEGEN_UNUSED_LOCAL));
		store.setDefault(PREF_CODEGEN_TARGET_PLATFORM, (String)hashtable.get(PREF_CODEGEN_TARGET_PLATFORM));
		store.setDefault(PREF_PB_UNREACHABLE_CODE, (String)hashtable.get(PREF_PB_UNREACHABLE_CODE));
		store.setDefault(PREF_PB_INVALID_IMPORT, (String)hashtable.get(PREF_PB_INVALID_IMPORT));
		store.setDefault(PREF_PB_OVERRIDING_PACKAGE_DEFAULT_METHOD, (String)hashtable.get(PREF_PB_OVERRIDING_PACKAGE_DEFAULT_METHOD));
		store.setDefault(PREF_PB_METHOD_WITH_CONSTRUCTOR_NAME, (String)hashtable.get(PREF_PB_METHOD_WITH_CONSTRUCTOR_NAME));
		store.setDefault(PREF_PB_DEPRECATION, (String)hashtable.get(PREF_PB_DEPRECATION));
		store.setDefault(PREF_PB_HIDDEN_CATCH_BLOCK, (String)hashtable.get(PREF_PB_HIDDEN_CATCH_BLOCK));
		store.setDefault(PREF_PB_UNUSED_LOCAL, (String)hashtable.get(PREF_PB_UNUSED_LOCAL));
		store.setDefault(PREF_PB_UNUSED_PARAMETER, (String)hashtable.get(PREF_PB_UNUSED_PARAMETER));
		store.setDefault(PREF_PB_SYNTHETIC_ACCESS_EMULATION, (String)hashtable.get(PREF_PB_SYNTHETIC_ACCESS_EMULATION));		
	}

	private static class ControlData {
		private String fKey;
		private String[] fValues;
		
		public ControlData(String key, String[] values) {
			fKey= key;
			fValues= values;
		}
		
		public String getKey() {
			return fKey;
		}
		
		public String getValue(boolean selection) {
			int index= selection ? 0 : 1;
			return fValues[index];
		}
		
		public boolean getSelection(String value) {
			return value.equals(fValues[0]);
		}
	}
	
	private Hashtable fWorkingValues;
	private ArrayList fCheckBoxes;
	
	private SelectionListener fSelectionListener;

	public CompilerPreferencePage() {
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(JavaUIMessages.getString("CompilerPreferencePage.description")); //$NON-NLS-1$
	
		fWorkingValues= JavaCore.getOptions();
		fCheckBoxes= new ArrayList();
		
		fSelectionListener= new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}

			public void widgetSelected(SelectionEvent e) {
				buttonControlChanged((Button)e.widget);
			}
		};
		
	}

	/**
	 * @see IWorkbenchPreferencePage#init()
	 */	
	public void init(IWorkbench workbench) {
	}

	/**
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		// added for 1GEUGE6: ITPJUI:WIN2000 - Help is the same on all preference pages
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), new DialogPageContextComputer(this, IJavaHelpContextIds.JAVA_BASE_PREFERENCE_PAGE));
	}	

	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
			
		TabFolder folder= new TabFolder(parent, SWT.NONE);
		folder.setLayout(new TabFolderLayout());	
		folder.setLayoutData(new GridData());
		
		String[] generateValues= new String[] { JavaCore.GENERATE, JavaCore.DO_NOT_GENERATE };
		String[] warningIngore= new String[] { JavaCore.WARNING, JavaCore.IGNORE };
		String[] errorWarning= new String[] { JavaCore.ERROR, JavaCore.WARNING };

		Composite warningsComposite= new Composite(folder, SWT.NULL);
		warningsComposite.setLayout(new GridLayout());

		String label= JavaUIMessages.getString("CompilerPreferencePage.pb_unreachable_code.label"); //$NON-NLS-1$
		addCheckBox(warningsComposite, label, PREF_PB_UNREACHABLE_CODE, errorWarning);	
		
		label= JavaUIMessages.getString("CompilerPreferencePage.pb_invalid_import.label"); //$NON-NLS-1$
		addCheckBox(warningsComposite, label, PREF_PB_INVALID_IMPORT, errorWarning);

		label= JavaUIMessages.getString("CompilerPreferencePage.pb_overriding_pkg_dflt.label"); //$NON-NLS-1$
		addCheckBox(warningsComposite, label, PREF_PB_OVERRIDING_PACKAGE_DEFAULT_METHOD, warningIngore);			

		label= JavaUIMessages.getString("CompilerPreferencePage.pb_method_naming.label"); //$NON-NLS-1$
		addCheckBox(warningsComposite, label, PREF_PB_METHOD_WITH_CONSTRUCTOR_NAME, warningIngore);			

		label= JavaUIMessages.getString("CompilerPreferencePage.pb_deprecation.label"); //$NON-NLS-1$
		addCheckBox(warningsComposite, label, PREF_PB_DEPRECATION, warningIngore);
		
		label= JavaUIMessages.getString("CompilerPreferencePage.pb_hidden_catchblock.label"); //$NON-NLS-1$
		addCheckBox(warningsComposite, label, PREF_PB_HIDDEN_CATCH_BLOCK, warningIngore);
		
		label= JavaUIMessages.getString("CompilerPreferencePage.pb_unused_local.label"); //$NON-NLS-1$
		addCheckBox(warningsComposite, label, PREF_PB_UNUSED_LOCAL, warningIngore);
		
		label= JavaUIMessages.getString("CompilerPreferencePage.pb_unused_parameter.label"); //$NON-NLS-1$
		addCheckBox(warningsComposite, label, PREF_PB_UNUSED_PARAMETER, warningIngore);
		
		label= JavaUIMessages.getString("CompilerPreferencePage.pb_synth_access_emul.label"); //$NON-NLS-1$
		addCheckBox(warningsComposite, label, PREF_PB_SYNTHETIC_ACCESS_EMULATION, warningIngore);

		Composite codeGenComposite= new Composite(folder, SWT.NULL);
		codeGenComposite.setLayout(new GridLayout());

		label= JavaUIMessages.getString("CompilerPreferencePage.variable_attr.label"); //$NON-NLS-1$
		addCheckBox(codeGenComposite, label, PREF_LOCAL_VARIABLE_ATTR, generateValues);
		
		label= JavaUIMessages.getString("CompilerPreferencePage.line_number_attr.label"); //$NON-NLS-1$
		addCheckBox(codeGenComposite, label, PREF_LINE_NUMBER_ATTR, generateValues);		

		label= JavaUIMessages.getString("CompilerPreferencePage.source_file_attr.label"); //$NON-NLS-1$
		addCheckBox(codeGenComposite, label, PREF_SOURCE_FILE_ATTR, generateValues);		

		label= JavaUIMessages.getString("CompilerPreferencePage.codegen_unused_local.label"); //$NON-NLS-1$
		addCheckBox(codeGenComposite, label, PREF_CODEGEN_UNUSED_LOCAL, new String[] { JavaCore.PRESERVE, JavaCore.OPTIMIZE_OUT });	

		label= JavaUIMessages.getString("CompilerPreferencePage.codegen_targetplatform.label"); //$NON-NLS-1$
		addCheckBox(codeGenComposite, label, PREF_CODEGEN_TARGET_PLATFORM, new String[] { JavaCore.VERSION_1_1, JavaCore.VERSION_1_2 });	


		TabItem item= new TabItem(folder, SWT.NONE);
		item.setText(JavaUIMessages.getString("CompilerPreferencePage.warnings.tabtitle")); //$NON-NLS-1$
		item.setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REFACTORING_WARNING));
		item.setControl(warningsComposite);

	
		item= new TabItem(folder, SWT.NONE);
		item.setText(JavaUIMessages.getString("CompilerPreferencePage.generation.tabtitle")); //$NON-NLS-1$
		item.setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_CFILE));
		item.setControl(codeGenComposite);
		
				
		return folder;
	}
	
	private void addCheckBox(Composite parent, String label, String key, String[] values) {
		ControlData data= new ControlData(key, values);
		
		Button checkBox= new Button(parent, SWT.CHECK);
		checkBox.setText(label);
		checkBox.setData(DATA_KEY, data);
		checkBox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		checkBox.addSelectionListener(fSelectionListener);
		
		String currValue= (String)fWorkingValues.get(key);	
		checkBox.setSelection(data.getSelection(currValue));
		
		fCheckBoxes.add(checkBox);
	}
	
	private void buttonControlChanged(Button button) {
		ControlData data= (ControlData)button.getData(DATA_KEY);
		String newValue= data.getValue(button.getSelection());
		fWorkingValues.put(data.getKey(), newValue);
	}
	
	/**
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		JavaCore.setOptions(fWorkingValues);
		return super.performOk();
	}	
	
	/**
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		fWorkingValues= JavaCore.getDefaultOptions();
		updateControls();
		super.performDefaults();
	}
	
	private void updateControls() {
		// update the UI
		for (int i= fCheckBoxes.size() - 1; i >= 0; i--) {
			Button curr= (Button) fCheckBoxes.get(i);
			ControlData data= (ControlData) curr.getData(DATA_KEY);
					
			String currValue= (String) fWorkingValues.get(data.getKey());	
			curr.setSelection(data.getSelection(currValue));			
		}
	}

}


