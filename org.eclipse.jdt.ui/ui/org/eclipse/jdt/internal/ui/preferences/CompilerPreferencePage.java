/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.TabFolderLayout;

/*
 * The page for setting the compiler options.
 */
public class CompilerPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	// Preference store keys, see JavaCore.getOptions
	private static final String PREF_LOCAL_VARIABLE_ATTR= "org.eclipse.jdt.core.compiler.debug.localVariable";
	private static final String PREF_LINE_NUMBER_ATTR= "org.eclipse.jdt.core.compiler.debug.lineNumber";
	private static final String PREF_SOURCE_FILE_ATTR= "org.eclipse.jdt.core.compiler.debug.sourceFile";
	private static final String PREF_CODEGEN_UNUSED_LOCAL= "org.eclipse.jdt.core.compiler.codegen.unusedLocal";
	private static final String PREF_CODEGEN_TARGET_PLATFORM= "org.eclipse.jdt.core.compiler.codegen.targetPlatform";
	private static final String PREF_PB_UNREACHABLE_CODE= "org.eclipse.jdt.core.compiler.problem.unreachableCode";	
	private static final String PREF_PB_INVALID_IMPORT= "org.eclipse.jdt.core.compiler.problem.invalidImport";	
	private static final String PREF_PB_OVERRIDING_PACKAGE_DEFAULT_METHOD= "org.eclipse.jdt.core.compiler.problem.overridingPackageDefaultMethod";	
	private static final String PREF_PB_METHOD_WITH_CONSTRUCTOR_NAME= "org.eclipse.jdt.core.compiler.problem.methodWithConstructorName";
	private static final String PREF_PB_DEPRECATION= "org.eclipse.jdt.core.compiler.problem.deprecation";
	private static final String PREF_PB_HIDDEN_CATCH_BLOCK= "org.eclipse.jdt.core.compiler.problem.hiddenCatchBlock";
	private static final String PREF_PB_UNUSED_LOCAL= "org.eclipse.jdt.core.compiler.problem.unusedLocal";	
	private static final String PREF_PB_UNUSED_PARAMETER= "org.eclipse.jdt.core.compiler.problem.unusedParameter";
	private static final String PREF_PB_SYNTHETIC_ACCESS_EMULATION= "org.eclipse.jdt.core.compiler.problem.syntheticAccessEmulation";
	private static final String PREF_PB_NON_EXTERNALIZED_STRINGS= "org.eclipse.jdt.core.compiler.problem.nonExternalizedStringLiteral";
	private static final String PREF_PB_ASSERT_AS_IDENTIFIER= "org.eclipse.jdt.core.compiler.problem.assertIdentifier";
	private static final String PREF_SOURCE_COMPATIBILITY= "org.eclipse.jdt.core.compiler.source";
	private static final String PREF_COMPLIANCE= "org.eclipse.jdt.core.compiler.compliance";

	private static final String INTR_DEFAULT_COMPLIANCE= "internal.default.compliance";

	// values
	private static final String GENERATE= "generate";
	private static final String DO_NOT_GENERATE= "do not generate";
	
	private static final String PRESERVE= "preserve";
	private static final String OPTIMIZE_OUT= "optimize out";
	
	private static final String VERSION_1_1= "1.1";
	private static final String VERSION_1_2= "1.2";
	private static final String VERSION_1_3= "1.3";
	private static final String VERSION_1_4= "1.4";
	
	private static final String ERROR= "error";
	private static final String WARNING= "warning";
	private static final String IGNORE= "ignore";
	
	private static final String DEFAULT= "default";
	private static final String USER= "user";	

	private static String[] getAllKeys() {
		return new String[] {
			PREF_LOCAL_VARIABLE_ATTR, PREF_LINE_NUMBER_ATTR, PREF_SOURCE_FILE_ATTR, PREF_CODEGEN_UNUSED_LOCAL,
			PREF_CODEGEN_TARGET_PLATFORM, PREF_PB_UNREACHABLE_CODE, PREF_PB_INVALID_IMPORT, PREF_PB_OVERRIDING_PACKAGE_DEFAULT_METHOD,
			PREF_PB_METHOD_WITH_CONSTRUCTOR_NAME, PREF_PB_DEPRECATION, PREF_PB_HIDDEN_CATCH_BLOCK, PREF_PB_UNUSED_LOCAL,
			PREF_PB_UNUSED_PARAMETER, PREF_PB_SYNTHETIC_ACCESS_EMULATION, PREF_PB_NON_EXTERNALIZED_STRINGS,
			PREF_PB_ASSERT_AS_IDENTIFIER, PREF_SOURCE_COMPATIBILITY, PREF_COMPLIANCE
		};	
	}

	/**
	 * Initializes the current options (read from preference store)
	 */
	public static void initDefaults(IPreferenceStore store) {
		Hashtable hashtable= JavaCore.getDefaultOptions();
		Hashtable currOptions= JavaCore.getOptions();
		String[] allKeys= getAllKeys();
		for (int i= 0; i < allKeys.length; i++) {
			String key= allKeys[i];
			String defValue= (String) hashtable.get(key);
			if (defValue != null) {
				store.setDefault(key, defValue);
			} else {
				JavaPlugin.logErrorMessage("CompilerPreferencePage: value is null: " + key);
			}
			// update the JavaCore options from the pref store
			String val= store.getString(key);
			if (val != null) {
				currOptions.put(key, val);
			}			
		}
		JavaCore.setOptions(currOptions);
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
		
		public String getValue(int index) {
			return fValues[index];
		}		
		
		public int getSelection(String value) {
			for (int i= 0; i < fValues.length; i++) {
				if (value.equals(fValues[i])) {
					return i;
				}
			}
			throw new IllegalArgumentException();
		}
	}
	
	private Hashtable fWorkingValues;

	private ArrayList fCheckBoxes;
	private ArrayList fComboBoxes;
	
	private SelectionListener fSelectionListener;
	
	private ArrayList fComplianceControls;

	public CompilerPreferencePage() {
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(JavaUIMessages.getString("CompilerPreferencePage.description")); //$NON-NLS-1$
	
		fWorkingValues= JavaCore.getOptions();
		fWorkingValues.put(INTR_DEFAULT_COMPLIANCE, getCurrentCompliance());
		
		fCheckBoxes= new ArrayList();
		fComboBoxes= new ArrayList();
		
		fComplianceControls= new ArrayList();
		
		fSelectionListener= new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}

			public void widgetSelected(SelectionEvent e) {
				controlChanged(e.widget);
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
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.COMPILER_PREFERENCE_PAGE);
	}	

	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		TabFolder folder= new TabFolder(parent, SWT.NONE);
		folder.setLayout(new TabFolderLayout());	
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite warningsComposite= createWarningsTabContent(folder);
		Composite codeGenComposite= createCodeGenTabContent(folder);
		Composite complianceComposite= createComplianceTabContent(folder);

		TabItem item= new TabItem(folder, SWT.NONE);
		item.setText(JavaUIMessages.getString("CompilerPreferencePage.warnings.tabtitle")); //$NON-NLS-1$
		item.setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REFACTORING_WARNING));
		item.setControl(warningsComposite);
	
		item= new TabItem(folder, SWT.NONE);
		item.setText(JavaUIMessages.getString("CompilerPreferencePage.generation.tabtitle")); //$NON-NLS-1$
		item.setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_CFILE));
		item.setControl(codeGenComposite);

		item= new TabItem(folder, SWT.NONE);
		item.setText(JavaUIMessages.getString("CompilerPreferencePage.compliance.tabtitle")); //$NON-NLS-1$
		item.setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_LIBRARY));
		item.setControl(complianceComposite);
		
		validateSettings(null, null);
				
		return folder;
	}

	private Composite createWarningsTabContent(Composite folder) {
		String[] errorWarningIgnore= new String[] { ERROR, WARNING, IGNORE };
		
		String[] errorWarningIgnoreLabels= new String[] {
			JavaUIMessages.getString("CompilerPreferencePage.error"), 
			JavaUIMessages.getString("CompilerPreferencePage.warning"),
			JavaUIMessages.getString("CompilerPreferencePage.ignore")
		};
			
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;

		Composite warningsComposite= new Composite(folder, SWT.NULL);
		warningsComposite.setLayout(layout);

		Label description= new Label(warningsComposite, SWT.WRAP);
		description.setText(JavaUIMessages.getString("CompilerPreferencePage.warnings.description")); //$NON-NLS-1$
		GridData gd= new GridData();
		gd.horizontalSpan= 2;
		gd.widthHint= convertWidthInCharsToPixels(60);
		description.setLayoutData(gd);

		String label= JavaUIMessages.getString("CompilerPreferencePage.pb_unreachable_code.label"); //$NON-NLS-1$
		addComboBox(warningsComposite, label, PREF_PB_UNREACHABLE_CODE, errorWarningIgnore, errorWarningIgnoreLabels, 0);	
		
		label= JavaUIMessages.getString("CompilerPreferencePage.pb_invalid_import.label"); //$NON-NLS-1$
		addComboBox(warningsComposite, label, PREF_PB_INVALID_IMPORT, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= JavaUIMessages.getString("CompilerPreferencePage.pb_overriding_pkg_dflt.label"); //$NON-NLS-1$
		addComboBox(warningsComposite, label, PREF_PB_OVERRIDING_PACKAGE_DEFAULT_METHOD, errorWarningIgnore, errorWarningIgnoreLabels, 0);			

		label= JavaUIMessages.getString("CompilerPreferencePage.pb_method_naming.label"); //$NON-NLS-1$
		addComboBox(warningsComposite, label, PREF_PB_METHOD_WITH_CONSTRUCTOR_NAME, errorWarningIgnore, errorWarningIgnoreLabels, 0);			

		label= JavaUIMessages.getString("CompilerPreferencePage.pb_deprecation.label"); //$NON-NLS-1$
		addComboBox(warningsComposite, label, PREF_PB_DEPRECATION, errorWarningIgnore, errorWarningIgnoreLabels, 0);
		
		label= JavaUIMessages.getString("CompilerPreferencePage.pb_hidden_catchblock.label"); //$NON-NLS-1$
		addComboBox(warningsComposite, label, PREF_PB_HIDDEN_CATCH_BLOCK, errorWarningIgnore, errorWarningIgnoreLabels, 0);
		
		label= JavaUIMessages.getString("CompilerPreferencePage.pb_unused_local.label"); //$NON-NLS-1$
		addComboBox(warningsComposite, label, PREF_PB_UNUSED_LOCAL, errorWarningIgnore, errorWarningIgnoreLabels, 0);
		
		label= JavaUIMessages.getString("CompilerPreferencePage.pb_unused_parameter.label"); //$NON-NLS-1$
		addComboBox(warningsComposite, label, PREF_PB_UNUSED_PARAMETER, errorWarningIgnore, errorWarningIgnoreLabels, 0);
		
		label= JavaUIMessages.getString("CompilerPreferencePage.pb_synth_access_emul.label"); //$NON-NLS-1$
		addComboBox(warningsComposite, label, PREF_PB_SYNTHETIC_ACCESS_EMULATION, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= JavaUIMessages.getString("CompilerPreferencePage.pb_non_externalized_strings.label"); //$NON-NLS-1$
		addComboBox(warningsComposite, label, PREF_PB_NON_EXTERNALIZED_STRINGS, errorWarningIgnore, errorWarningIgnoreLabels, 0);
		
		return warningsComposite;
	}
	
	private Composite createCodeGenTabContent(Composite folder) {
		String[] generateValues= new String[] { GENERATE, DO_NOT_GENERATE };

		GridLayout layout= new GridLayout();
		layout.numColumns= 2;

		Composite codeGenComposite= new Composite(folder, SWT.NULL);
		codeGenComposite.setLayout(layout);

		String label= JavaUIMessages.getString("CompilerPreferencePage.variable_attr.label"); //$NON-NLS-1$
		addCheckBox(codeGenComposite, label, PREF_LOCAL_VARIABLE_ATTR, generateValues, 0);
		
		label= JavaUIMessages.getString("CompilerPreferencePage.line_number_attr.label"); //$NON-NLS-1$
		addCheckBox(codeGenComposite, label, PREF_LINE_NUMBER_ATTR, generateValues, 0);		

		label= JavaUIMessages.getString("CompilerPreferencePage.source_file_attr.label"); //$NON-NLS-1$
		addCheckBox(codeGenComposite, label, PREF_SOURCE_FILE_ATTR, generateValues, 0);		

		label= JavaUIMessages.getString("CompilerPreferencePage.codegen_unused_local.label"); //$NON-NLS-1$
		addCheckBox(codeGenComposite, label, PREF_CODEGEN_UNUSED_LOCAL, new String[] { PRESERVE, OPTIMIZE_OUT }, 0);	
		
		return codeGenComposite;
	}
	
	private Composite createComplianceTabContent(Composite folder) {
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;

		Composite complianceComposite= new Composite(folder, SWT.NULL);
		complianceComposite.setLayout(layout);

		String[] values34= new String[] { VERSION_1_3, VERSION_1_4 };
		String[] values34Labels= new String[] {
			JavaUIMessages.getString("CompilerPreferencePage.version13"), 
			JavaUIMessages.getString("CompilerPreferencePage.version14")
		};
		
		String label= JavaUIMessages.getString("CompilerPreferencePage.compiler_compliance.label"); //$NON-NLS-1$
		addComboBox(complianceComposite, label, PREF_COMPLIANCE, values34, values34Labels, 0);	

		label= JavaUIMessages.getString("CompilerPreferencePage.default_settings.label"); //$NON-NLS-1$
		addCheckBox(complianceComposite, label, INTR_DEFAULT_COMPLIANCE, new String[] { DEFAULT, USER }, 0);	

		int indent= convertWidthInCharsToPixels(2);
		Control[] otherChildren= complianceComposite.getChildren();	
				
		String[] values14= new String[] { VERSION_1_1, VERSION_1_2, VERSION_1_3, VERSION_1_4 };
		String[] values14Labels= new String[] {
			JavaUIMessages.getString("CompilerPreferencePage.version11"), 
			JavaUIMessages.getString("CompilerPreferencePage.version12"),
			JavaUIMessages.getString("CompilerPreferencePage.version13"),
			JavaUIMessages.getString("CompilerPreferencePage.version14")
		};
		
		label= JavaUIMessages.getString("CompilerPreferencePage.codegen_targetplatform.label"); //$NON-NLS-1$
		addComboBox(complianceComposite, label, PREF_CODEGEN_TARGET_PLATFORM, values14, values14Labels, indent);	

		label= JavaUIMessages.getString("CompilerPreferencePage.source_compatibility.label"); //$NON-NLS-1$
		addComboBox(complianceComposite, label, PREF_SOURCE_COMPATIBILITY, values34, values34Labels, indent);	

		String[] errorWarningIgnore= new String[] { ERROR, WARNING, IGNORE };
		
		String[] errorWarningIgnoreLabels= new String[] {
			JavaUIMessages.getString("CompilerPreferencePage.error"), 
			JavaUIMessages.getString("CompilerPreferencePage.warning"),
			JavaUIMessages.getString("CompilerPreferencePage.ignore")
		};

		label= JavaUIMessages.getString("CompilerPreferencePage.pb_assert_as_identifier.label"); //$NON-NLS-1$
		addComboBox(complianceComposite, label, PREF_PB_ASSERT_AS_IDENTIFIER, errorWarningIgnore, errorWarningIgnoreLabels, indent);		

		Control[] allChildren= complianceComposite.getChildren();
		fComplianceControls.addAll(Arrays.asList(allChildren));
		fComplianceControls.removeAll(Arrays.asList(otherChildren));
		
		return complianceComposite;
	}
		
	private void addCheckBox(Composite parent, String label, String key, String[] values, int indent) {
		ControlData data= new ControlData(key, values);
		
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		gd.horizontalIndent= indent;
		
		Button checkBox= new Button(parent, SWT.CHECK);
		checkBox.setText(label);
		checkBox.setData(data);
		checkBox.setLayoutData(gd);
		checkBox.addSelectionListener(fSelectionListener);
		
		String currValue= (String)fWorkingValues.get(key);	
		checkBox.setSelection(data.getSelection(currValue) == 0);
		
		fCheckBoxes.add(checkBox);
	}
	
	private void addComboBox(Composite parent, String label, String key, String[] values, String[] valueLabels, int indent) {
		ControlData data= new ControlData(key, values);
		
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent= indent;		
		
		Label labelControl= new Label(parent, SWT.NONE);
		labelControl.setText(label);
		labelControl.setLayoutData(gd);
		
		Combo comboBox= new Combo(parent, SWT.READ_ONLY);
		comboBox.setItems(valueLabels);
		comboBox.setData(data);
		comboBox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		comboBox.addSelectionListener(fSelectionListener);
		
		String currValue= (String)fWorkingValues.get(key);	
		comboBox.select(data.getSelection(currValue));
		
		fComboBoxes.add(comboBox);
	}	
	
	private void controlChanged(Widget widget) {
		ControlData data= (ControlData) widget.getData();
		String newValue= null;
		if (widget instanceof Button) {
			newValue= data.getValue(((Button)widget).getSelection());			
		} else if (widget instanceof Combo) {
			newValue= data.getValue(((Combo)widget).getSelectionIndex());
		} else {
			return;
		}
		fWorkingValues.put(data.getKey(), newValue);
		
		validateSettings(data.getKey(), newValue);
	}

	private boolean checkValue(String key, String value) {
		return value.equals(fWorkingValues.get(key));
	}

	/* (non-javadoc)
	 * Update fields and validate.
	 * @param changedKey Key that changed, or null, if all changed.
	 */	
	private void validateSettings(String changedKey, String newValue) {
		if (changedKey != null) {
			if (INTR_DEFAULT_COMPLIANCE.equals(changedKey)) {
				updateComplianceEnableState();
				if (DEFAULT.equals(newValue)) {
					updateComplianceDefaultSettings();
				}
			} else if (PREF_COMPLIANCE.equals(changedKey)) {
				if (checkValue(INTR_DEFAULT_COMPLIANCE, DEFAULT)) {
					updateComplianceDefaultSettings();
				}
			} else if (!PREF_SOURCE_COMPATIBILITY.equals(changedKey) &&
					!PREF_CODEGEN_TARGET_PLATFORM.equals(changedKey) &&
					!PREF_PB_ASSERT_AS_IDENTIFIER.equals(changedKey)) {
				return;
			}
		} else {
			updateComplianceEnableState();
		}
		updateStatus(getValidation());
	}
	
	private IStatus getValidation() {
		StatusInfo status= new StatusInfo();
		if (checkValue(PREF_COMPLIANCE, VERSION_1_3)) {
			if (checkValue(PREF_SOURCE_COMPATIBILITY, VERSION_1_4)) {
				status.setError(JavaUIMessages.getString("CompilerPreferencePage.cpl13src14.error"));
				return status;
			} else if (checkValue(PREF_CODEGEN_TARGET_PLATFORM, VERSION_1_4)) {
				status.setError(JavaUIMessages.getString("CompilerPreferencePage.cpl13trg14.error"));
				return status;
			}
		}
		if (checkValue(PREF_SOURCE_COMPATIBILITY, VERSION_1_4)) {
			if (!checkValue(PREF_PB_ASSERT_AS_IDENTIFIER, ERROR)) {
				status.setError(JavaUIMessages.getString("CompilerPreferencePage.src14asrterr.error"));
				return status;
			}
		}
		if (checkValue(PREF_SOURCE_COMPATIBILITY, VERSION_1_4)) {
			if (!checkValue(PREF_CODEGEN_TARGET_PLATFORM, VERSION_1_4)) {
				status.setError(JavaUIMessages.getString("CompilerPreferencePage.src14tgt14.error"));
				return status;
			}
		}		
		return status;
	}		

	/*
	 * Update the compliance controls' enable state
	 */		
	private void updateComplianceEnableState() {
		boolean enabled= checkValue(INTR_DEFAULT_COMPLIANCE, USER);
		for (int i= fComplianceControls.size() - 1; i >= 0; i--) {
			Control curr= (Control) fComplianceControls.get(i);
			curr.setEnabled(enabled);
		}
	}

	/*
	 * Set the default compliance values derived from the chosen level
	 */	
	private void updateComplianceDefaultSettings() {
		Object complianceLevel= fWorkingValues.get(PREF_COMPLIANCE);
		if (VERSION_1_3.equals(complianceLevel)) {
			fWorkingValues.put(PREF_PB_ASSERT_AS_IDENTIFIER, IGNORE);
			fWorkingValues.put(PREF_SOURCE_COMPATIBILITY, VERSION_1_3);
			fWorkingValues.put(PREF_CODEGEN_TARGET_PLATFORM, VERSION_1_1);
		} else if (VERSION_1_4.equals(complianceLevel)) {
			fWorkingValues.put(PREF_PB_ASSERT_AS_IDENTIFIER, ERROR);
			fWorkingValues.put(PREF_SOURCE_COMPATIBILITY, VERSION_1_4);
			fWorkingValues.put(PREF_CODEGEN_TARGET_PLATFORM, VERSION_1_4);
		}
		updateControls();
	}
	
	/*
	 * Evaluate if the current compliance setting correspond to a default setting
	 */
	private String getCurrentCompliance() {
		Object complianceLevel= fWorkingValues.get(PREF_COMPLIANCE);
		if ((VERSION_1_3.equals(complianceLevel)
				&& checkValue(PREF_PB_ASSERT_AS_IDENTIFIER, IGNORE)
				&& checkValue(PREF_SOURCE_COMPATIBILITY, VERSION_1_3)
				&& checkValue(PREF_CODEGEN_TARGET_PLATFORM, VERSION_1_1))
			|| (VERSION_1_4.equals(complianceLevel)
				&& checkValue(PREF_PB_ASSERT_AS_IDENTIFIER, ERROR)
				&& checkValue(PREF_SOURCE_COMPATIBILITY, VERSION_1_4)
				&& checkValue(PREF_CODEGEN_TARGET_PLATFORM, VERSION_1_4))) {
			return DEFAULT;
		}
		return USER;
	}
	
	/*
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		String[] allKeys= getAllKeys();
		// preserve other options
		// store in JCore and the preferences
		Hashtable actualOptions= JavaCore.getOptions();
		IPreferenceStore store= getPreferenceStore();
		boolean hasChanges= false;
		for (int i= 0; i < allKeys.length; i++) {
			String key= allKeys[i];
			String val=  (String) fWorkingValues.get(key);
			String oldVal= (String) actualOptions.get(key);
			hasChanges= hasChanges | !val.equals(oldVal);
			
			actualOptions.put(key, val);
			store.setValue(key, val);
		}
		JavaCore.setOptions(actualOptions);
		
		if (hasChanges) {
			String title= JavaUIMessages.getString("CompilerPreferencePage.needsbuild.title");
			String message= JavaUIMessages.getString("CompilerPreferencePage.needsbuild.message");
			if (MessageDialog.openQuestion(getShell(), title, message)) {
				doFullBuild();
			}
		}
		return super.performOk();
	}
	
	private void doFullBuild() {
		ProgressMonitorDialog dialog= new ProgressMonitorDialog(getShell());
		try {
			dialog.run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					try {
						JavaPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InterruptedException e) {
			// cancelled by user
		} catch (InvocationTargetException e) {
			String title= JavaUIMessages.getString("CompilerPreferencePage.builderror.title");
			String message= JavaUIMessages.getString("CompilerPreferencePage.builderror.message");
			ExceptionHandler.handle(e, getShell(), title, message);
		}
	}		
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		fWorkingValues= JavaCore.getDefaultOptions();
		fWorkingValues.put(INTR_DEFAULT_COMPLIANCE, getCurrentCompliance());
		updateControls();
		validateSettings(null, null);
		super.performDefaults();
	}
	
	private void updateControls() {
		// update the UI
		for (int i= fCheckBoxes.size() - 1; i >= 0; i--) {
			Button curr= (Button) fCheckBoxes.get(i);
			ControlData data= (ControlData) curr.getData();
					
			String currValue= (String) fWorkingValues.get(data.getKey());	
			curr.setSelection(data.getSelection(currValue) == 0);			
		}
		for (int i= fComboBoxes.size() - 1; i >= 0; i--) {
			Combo curr= (Combo) fComboBoxes.get(i);
			ControlData data= (ControlData) curr.getData();
					
			String currValue= (String) fWorkingValues.get(data.getKey());	
			curr.select(data.getSelection(currValue));			
		}		
		
	}
	
	private void updateStatus(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}

}


