/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.TabFolderLayout;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;

/*
 * The page for setting the compiler options.
 */
public class CompilerPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	// Preference store keys, see JavaCore.getOptions
	private static final String PREF_LOCAL_VARIABLE_ATTR=  JavaCore.COMPILER_LOCAL_VARIABLE_ATTR;
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
	private static final String PREF_PB_NON_EXTERNALIZED_STRINGS= JavaCore.COMPILER_PB_NON_NLS_STRING_LITERAL;
	private static final String PREF_PB_ASSERT_AS_IDENTIFIER= JavaCore.COMPILER_PB_ASSERT_IDENTIFIER;
	private static final String PREF_PB_MAX_PER_UNIT= JavaCore.COMPILER_PB_MAX_PER_UNIT;
	private static final String PREF_PB_UNUSED_IMPORT= JavaCore.COMPILER_PB_UNUSED_IMPORT;
	private static final String PREF_PB_STATIC_ACCESS_RECEIVER= JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER;

	private static final String PREF_SOURCE_COMPATIBILITY= JavaCore.COMPILER_SOURCE;
	private static final String PREF_COMPLIANCE= JavaCore.COMPILER_COMPLIANCE;

	private static final String PREF_RESOURCE_FILTER= JavaCore.CORE_JAVA_BUILD_RESOURCE_COPY_FILTER;
	private static final String PREF_BUILD_INVALID_CLASSPATH= JavaCore.CORE_JAVA_BUILD_INVALID_CLASSPATH;
	private static final String PREF_PB_INCOMPLETE_BUILDPATH= JavaCore.CORE_INCOMPLETE_CLASSPATH;
	private static final String PREF_PB_CIRCULAR_BUILDPATH= JavaCore.CORE_CIRCULAR_CLASSPATH;
	private static final String PREF_COMPILER_PB_DEPRECATION_IN_DEPRECATED_CODE= JavaCore.COMPILER_PB_DEPRECATION_IN_DEPRECATED_CODE;
	private static final String PREF_COMPILER_TASK_TAGS= JavaCore.COMPILER_TASK_TAGS;



	private static final String INTR_DEFAULT_COMPLIANCE= "internal.default.compliance"; //$NON-NLS-1$

	// values
	private static final String GENERATE= JavaCore.GENERATE;
	private static final String DO_NOT_GENERATE= JavaCore.DO_NOT_GENERATE;
	
	private static final String PRESERVE= JavaCore.PRESERVE;
	private static final String OPTIMIZE_OUT= JavaCore.OPTIMIZE_OUT;
	
	private static final String VERSION_1_1= JavaCore.VERSION_1_1;
	private static final String VERSION_1_2= JavaCore.VERSION_1_2;
	private static final String VERSION_1_3= JavaCore.VERSION_1_3;
	private static final String VERSION_1_4= JavaCore.VERSION_1_4;
	
	private static final String ERROR= JavaCore.ERROR;
	private static final String WARNING= JavaCore.WARNING;
	private static final String IGNORE= JavaCore.IGNORE;
	private static final String ABORT= JavaCore.ABORT;

	private static final String ENABLED= JavaCore.ENABLED;
	private static final String DISABLED= JavaCore.DISABLED;
	
	private static final String DEFAULT= "default"; //$NON-NLS-1$
	private static final String USER= "user";	 //$NON-NLS-1$

	private static String[] getAllKeys() {
		return new String[] {
			PREF_LOCAL_VARIABLE_ATTR, PREF_LINE_NUMBER_ATTR, PREF_SOURCE_FILE_ATTR, PREF_CODEGEN_UNUSED_LOCAL,
			PREF_CODEGEN_TARGET_PLATFORM, PREF_PB_UNREACHABLE_CODE, PREF_PB_INVALID_IMPORT, PREF_PB_OVERRIDING_PACKAGE_DEFAULT_METHOD,
			PREF_PB_METHOD_WITH_CONSTRUCTOR_NAME, PREF_PB_DEPRECATION, PREF_PB_HIDDEN_CATCH_BLOCK, PREF_PB_UNUSED_LOCAL,
			PREF_PB_UNUSED_PARAMETER, PREF_PB_SYNTHETIC_ACCESS_EMULATION, PREF_PB_NON_EXTERNALIZED_STRINGS,
			PREF_PB_ASSERT_AS_IDENTIFIER, PREF_PB_UNUSED_IMPORT, PREF_PB_MAX_PER_UNIT, PREF_SOURCE_COMPATIBILITY, PREF_COMPLIANCE, 
			PREF_RESOURCE_FILTER, PREF_BUILD_INVALID_CLASSPATH, PREF_PB_STATIC_ACCESS_RECEIVER, PREF_PB_INCOMPLETE_BUILDPATH,
			PREF_PB_CIRCULAR_BUILDPATH, PREF_COMPILER_PB_DEPRECATION_IN_DEPRECATED_CODE, PREF_COMPILER_TASK_TAGS
		};	
	}

	/**
	 * Initializes the current options (read from preference store)
	 */
	public static void initDefaults(IPreferenceStore store) {
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
	private ArrayList fTextBoxes;
	
	private SelectionListener fSelectionListener;
	private ModifyListener fTextModifyListener;
	
	private ArrayList fComplianceControls;

	private IStatus fComplianceStatus, fMaxNumberProblemsStatus, fResourceFilterStatus, fTaskTagsStatus;

	public CompilerPreferencePage() {
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(JavaUIMessages.getString("CompilerPreferencePage.description")); //$NON-NLS-1$
	
		fWorkingValues= JavaCore.getOptions();
		fWorkingValues.put(INTR_DEFAULT_COMPLIANCE, getCurrentCompliance());
		
		fCheckBoxes= new ArrayList();
		fComboBoxes= new ArrayList();
		fTextBoxes= new ArrayList(2); 
		
		fComplianceControls= new ArrayList();
		
		fSelectionListener= new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}

			public void widgetSelected(SelectionEvent e) {
				controlChanged(e.widget);
			}
		};
		
		fTextModifyListener= new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				textChanged((Text) e.widget);
			}
		};
		
		fComplianceStatus= new StatusInfo();
		fMaxNumberProblemsStatus= new StatusInfo();
		fResourceFilterStatus= new StatusInfo();
		fTaskTagsStatus= new StatusInfo();		
	}
	
	/**
	 * @see IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
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
		initializeDialogUnits(parent);
		
		TabFolder folder= new TabFolder(parent, SWT.NONE);
		folder.setLayout(new TabFolderLayout());	
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite warningsComposite= createWarningsTabContent(folder);
		Composite markersComposite= createMarkersTabContent(folder);
		Composite codeGenComposite= createCodeGenTabContent(folder);
		Composite complianceComposite= createComplianceTabContent(folder);
		Composite othersComposite= createOthersTabContent(folder);

		TabItem item= new TabItem(folder, SWT.NONE);
		item.setText(JavaUIMessages.getString("CompilerPreferencePage.warnings.tabtitle")); //$NON-NLS-1$
		item.setControl(warningsComposite);

		item= new TabItem(folder, SWT.NONE);
		item.setText(JavaUIMessages.getString("CompilerPreferencePage.markers.tabtitle")); //$NON-NLS-1$
		item.setControl(markersComposite);
	
		item= new TabItem(folder, SWT.NONE);
		item.setText(JavaUIMessages.getString("CompilerPreferencePage.generation.tabtitle")); //$NON-NLS-1$
		item.setControl(codeGenComposite);

		item= new TabItem(folder, SWT.NONE);
		item.setText(JavaUIMessages.getString("CompilerPreferencePage.compliance.tabtitle")); //$NON-NLS-1$
		item.setControl(complianceComposite);
		
		item= new TabItem(folder, SWT.NONE);
		item.setText(JavaUIMessages.getString("CompilerPreferencePage.others.tabtitle")); //$NON-NLS-1$
		item.setControl(othersComposite);		
		
		validateSettings(null, null);
	
		return folder;
	}

	private Composite createMarkersTabContent(TabFolder folder) {
		String[] errorWarningIgnore= new String[] { ERROR, WARNING, IGNORE };
		
		String[] errorWarningIgnoreLabels= new String[] {
			JavaUIMessages.getString("CompilerPreferencePage.error"),  //$NON-NLS-1$
			JavaUIMessages.getString("CompilerPreferencePage.warning"), //$NON-NLS-1$
			JavaUIMessages.getString("CompilerPreferencePage.ignore") //$NON-NLS-1$
		};
		
		String[] enabledDisabled= new String[] { ENABLED, DISABLED };
					
		Composite markersComposite= new Composite(folder, SWT.NULL);
		markersComposite.setLayout(new GridLayout());

		GridLayout layout= new GridLayout();
		layout.numColumns= 2;

		Group group= new Group(markersComposite, SWT.NONE);
		group.setText(JavaUIMessages.getString("CompilerPreferencePage.markers.deprecated.label"));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setLayout(layout);
		
		int indent= convertWidthInCharsToPixels(2);

		String label= JavaUIMessages.getString("CompilerPreferencePage.pb_deprecation.label"); //$NON-NLS-1$
		addComboBox(group, label, PREF_PB_DEPRECATION, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= JavaUIMessages.getString("CompilerPreferencePage.pb_deprecation_in_deprecation.label"); //$NON-NLS-1$
		addCheckBox(group, label, PREF_COMPILER_PB_DEPRECATION_IN_DEPRECATED_CODE, enabledDisabled, indent);

		layout= new GridLayout();
		layout.numColumns= 1;



		group= new Group(markersComposite, SWT.NONE);
		group.setText(JavaUIMessages.getString("CompilerPreferencePage.markers.taskmarkers.label"));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setLayout(layout);

		label= JavaUIMessages.getString("CompilerPreferencePage.pb_todo_comments.label"); //$NON-NLS-1$
		Text text= addTextField(group, label, PREF_COMPILER_TASK_TAGS);
		GridData gd= (GridData) text.getLayoutData();
		gd.grabExcessHorizontalSpace= true;
		gd.widthHint= convertWidthInCharsToPixels(10);		
		

		layout= new GridLayout();
		layout.numColumns= 2;

		group= new Group(markersComposite, SWT.NONE);
		group.setText(JavaUIMessages.getString("CompilerPreferencePage.markers.nls.label"));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setLayout(layout);

		label= JavaUIMessages.getString("CompilerPreferencePage.pb_non_externalized_strings.label"); //$NON-NLS-1$
		addComboBox(group, label, PREF_PB_NON_EXTERNALIZED_STRINGS, errorWarningIgnore, errorWarningIgnoreLabels, 0);

	
		return markersComposite;
	}


	private Composite createOthersTabContent(TabFolder folder) {
		String[] abortIgnoreValues= new String[] { ABORT, IGNORE };
		
		String[] errorWarning= new String[] { ERROR, WARNING };
		
		String[] errorWarningLabels= new String[] {
			JavaUIMessages.getString("CompilerPreferencePage.error"),  //$NON-NLS-1$
			JavaUIMessages.getString("CompilerPreferencePage.warning") //$NON-NLS-1$
		};
			
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;

		Composite othersComposite= new Composite(folder, SWT.NULL);
		othersComposite.setLayout(layout);
		
		Label description= new Label(othersComposite, SWT.WRAP);
		description.setText(JavaUIMessages.getString("CompilerPreferencePage.build_warnings.description")); //$NON-NLS-1$
		GridData gd= new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		gd.horizontalSpan= 2;
		// gd.widthHint= convertWidthInCharsToPixels(50);
		description.setLayoutData(gd);
		
		Composite combos= new Composite(othersComposite, SWT.NULL);
		gd= new GridData(GridData.FILL | GridData.GRAB_HORIZONTAL);
		gd.horizontalSpan= 2;
		combos.setLayoutData(gd);
		GridLayout cl= new GridLayout();
		cl.numColumns=2; cl.marginWidth= 0; cl.marginHeight= 0;
		combos.setLayout(cl);
		
		String label= JavaUIMessages.getString("CompilerPreferencePage.pb_incomplete_build_path.label"); //$NON-NLS-1$
		addComboBox(combos, label, PREF_PB_INCOMPLETE_BUILDPATH, errorWarning, errorWarningLabels, 0);	
		
		label= JavaUIMessages.getString("CompilerPreferencePage.pb_build_path_cycles.label"); //$NON-NLS-1$
		addComboBox(combos, label, PREF_PB_CIRCULAR_BUILDPATH, errorWarning, errorWarningLabels, 0);
		
		label= JavaUIMessages.getString("CompilerPreferencePage.build_invalid_classpath.label"); //$NON-NLS-1$
		addCheckBox(othersComposite, label, PREF_BUILD_INVALID_CLASSPATH, abortIgnoreValues, 0);
		
		description= new Label(othersComposite, SWT.WRAP);
		description.setText(JavaUIMessages.getString("CompilerPreferencePage.resource_filter.description")); //$NON-NLS-1$
		gd= new GridData(GridData.FILL);
		gd.horizontalSpan= 2;
		gd.widthHint= convertWidthInCharsToPixels(60);
		description.setLayoutData(gd);
		
		label= JavaUIMessages.getString("CompilerPreferencePage.resource_filter.label"); //$NON-NLS-1$
		Text text= addTextField(othersComposite, label, PREF_RESOURCE_FILTER);
		gd= (GridData) text.getLayoutData();
		gd.grabExcessHorizontalSpace= true;
		gd.widthHint= convertWidthInCharsToPixels(10);
		
		return othersComposite;

	}


	private Composite createWarningsTabContent(Composite folder) {
		String[] errorWarningIgnore= new String[] { ERROR, WARNING, IGNORE };
		
		String[] errorWarningIgnoreLabels= new String[] {
			JavaUIMessages.getString("CompilerPreferencePage.error"),  //$NON-NLS-1$
			JavaUIMessages.getString("CompilerPreferencePage.warning"), //$NON-NLS-1$
			JavaUIMessages.getString("CompilerPreferencePage.ignore") //$NON-NLS-1$
		};
			
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.verticalSpacing= 2;

		Composite warningsComposite= new Composite(folder, SWT.NULL);
		warningsComposite.setLayout(layout);

		Label description= new Label(warningsComposite, SWT.WRAP);
		description.setText(JavaUIMessages.getString("CompilerPreferencePage.warnings.description")); //$NON-NLS-1$
		GridData gd= new GridData();
		gd.horizontalSpan= 2;
		gd.widthHint= convertWidthInCharsToPixels(50);
		description.setLayoutData(gd);

		String label= JavaUIMessages.getString("CompilerPreferencePage.pb_unreachable_code.label"); //$NON-NLS-1$
		addComboBox(warningsComposite, label, PREF_PB_UNREACHABLE_CODE, errorWarningIgnore, errorWarningIgnoreLabels, 0);	
		
		label= JavaUIMessages.getString("CompilerPreferencePage.pb_invalid_import.label"); //$NON-NLS-1$
		addComboBox(warningsComposite, label, PREF_PB_INVALID_IMPORT, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= JavaUIMessages.getString("CompilerPreferencePage.pb_unused_local.label"); //$NON-NLS-1$
		addComboBox(warningsComposite, label, PREF_PB_UNUSED_LOCAL, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= JavaUIMessages.getString("CompilerPreferencePage.pb_overriding_pkg_dflt.label"); //$NON-NLS-1$
		addComboBox(warningsComposite, label, PREF_PB_OVERRIDING_PACKAGE_DEFAULT_METHOD, errorWarningIgnore, errorWarningIgnoreLabels, 0);			

		label= JavaUIMessages.getString("CompilerPreferencePage.pb_method_naming.label"); //$NON-NLS-1$
		addComboBox(warningsComposite, label, PREF_PB_METHOD_WITH_CONSTRUCTOR_NAME, errorWarningIgnore, errorWarningIgnoreLabels, 0);			

		label= JavaUIMessages.getString("CompilerPreferencePage.pb_hidden_catchblock.label"); //$NON-NLS-1$
		addComboBox(warningsComposite, label, PREF_PB_HIDDEN_CATCH_BLOCK, errorWarningIgnore, errorWarningIgnoreLabels, 0);
		
		label= JavaUIMessages.getString("CompilerPreferencePage.pb_unused_imports.label"); //$NON-NLS-1$
		addComboBox(warningsComposite, label, PREF_PB_UNUSED_IMPORT, errorWarningIgnore, errorWarningIgnoreLabels, 0);
		
		label= JavaUIMessages.getString("CompilerPreferencePage.pb_unused_parameter.label"); //$NON-NLS-1$
		addComboBox(warningsComposite, label, PREF_PB_UNUSED_PARAMETER, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= JavaUIMessages.getString("CompilerPreferencePage.pb_static_access_receiver.label"); //$NON-NLS-1$
		addComboBox(warningsComposite, label, PREF_PB_STATIC_ACCESS_RECEIVER, errorWarningIgnore, errorWarningIgnoreLabels, 0);
		
		label= JavaUIMessages.getString("CompilerPreferencePage.pb_synth_access_emul.label"); //$NON-NLS-1$
		addComboBox(warningsComposite, label, PREF_PB_SYNTHETIC_ACCESS_EMULATION, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= JavaUIMessages.getString("CompilerPreferencePage.pb_max_per_unit.label"); //$NON-NLS-1$
		Text text= addTextField(warningsComposite, label, PREF_PB_MAX_PER_UNIT);
		text.setTextLimit(6);
		
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
			JavaUIMessages.getString("CompilerPreferencePage.version13"),  //$NON-NLS-1$
			JavaUIMessages.getString("CompilerPreferencePage.version14") //$NON-NLS-1$
		};
		
		String label= JavaUIMessages.getString("CompilerPreferencePage.compiler_compliance.label"); //$NON-NLS-1$
		addComboBox(complianceComposite, label, PREF_COMPLIANCE, values34, values34Labels, 0);	

		label= JavaUIMessages.getString("CompilerPreferencePage.default_settings.label"); //$NON-NLS-1$
		addCheckBox(complianceComposite, label, INTR_DEFAULT_COMPLIANCE, new String[] { DEFAULT, USER }, 0);	

		int indent= convertWidthInCharsToPixels(2);
		Control[] otherChildren= complianceComposite.getChildren();	
				
		String[] values14= new String[] { VERSION_1_1, VERSION_1_2, VERSION_1_3, VERSION_1_4 };
		String[] values14Labels= new String[] {
			JavaUIMessages.getString("CompilerPreferencePage.version11"),  //$NON-NLS-1$
			JavaUIMessages.getString("CompilerPreferencePage.version12"), //$NON-NLS-1$
			JavaUIMessages.getString("CompilerPreferencePage.version13"), //$NON-NLS-1$
			JavaUIMessages.getString("CompilerPreferencePage.version14") //$NON-NLS-1$
		};
		
		label= JavaUIMessages.getString("CompilerPreferencePage.codegen_targetplatform.label"); //$NON-NLS-1$
		addComboBox(complianceComposite, label, PREF_CODEGEN_TARGET_PLATFORM, values14, values14Labels, indent);	

		label= JavaUIMessages.getString("CompilerPreferencePage.source_compatibility.label"); //$NON-NLS-1$
		addComboBox(complianceComposite, label, PREF_SOURCE_COMPATIBILITY, values34, values34Labels, indent);	

		String[] errorWarningIgnore= new String[] { ERROR, WARNING, IGNORE };
		
		String[] errorWarningIgnoreLabels= new String[] {
			JavaUIMessages.getString("CompilerPreferencePage.error"),  //$NON-NLS-1$
			JavaUIMessages.getString("CompilerPreferencePage.warning"), //$NON-NLS-1$
			JavaUIMessages.getString("CompilerPreferencePage.ignore") //$NON-NLS-1$
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
		
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= indent;
				
		Label labelControl= new Label(parent, SWT.LEFT | SWT.WRAP);
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
	
	private Text addTextField(Composite parent, String label, String key) {	
		Label labelControl= new Label(parent, SWT.NONE);
		labelControl.setText(label);
		labelControl.setLayoutData(new GridData());
				
		Text textBox= new Text(parent, SWT.BORDER | SWT.SINGLE);
		textBox.setData(key);
		textBox.setLayoutData(new GridData());
		
		String currValue= (String) fWorkingValues.get(key);	
		textBox.setText(currValue);
		textBox.addModifyListener(fTextModifyListener);

		textBox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

		fTextBoxes.add(textBox);
		return textBox;
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
	
	private void textChanged(Text textControl) {
		String key= (String) textControl.getData();
		String number= textControl.getText();
		fWorkingValues.put(key, number);
		validateSettings(key, number);
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
				fComplianceStatus= validateCompliance();
			} else if (PREF_SOURCE_COMPATIBILITY.equals(changedKey) ||
					PREF_CODEGEN_TARGET_PLATFORM.equals(changedKey) ||
					PREF_PB_ASSERT_AS_IDENTIFIER.equals(changedKey)) {
				fComplianceStatus= validateCompliance();
			} else if (PREF_PB_MAX_PER_UNIT.equals(changedKey)) {
				fMaxNumberProblemsStatus= validateMaxNumberProblems();
			} else if (PREF_RESOURCE_FILTER.equals(changedKey)) {
				fResourceFilterStatus= validateResourceFilters();
			} else if (PREF_COMPILER_TASK_TAGS.equals(changedKey)) {
				fTaskTagsStatus= validateTaskTags();
			} else {
				return;
			}
		} else {
			updateComplianceEnableState();
			fComplianceStatus= validateCompliance();
			fMaxNumberProblemsStatus= validateMaxNumberProblems();
			fResourceFilterStatus= validateResourceFilters();
			fTaskTagsStatus= validateTaskTags();
		}		
		IStatus status= StatusUtil.getMostSevere(new IStatus[] { fComplianceStatus, fMaxNumberProblemsStatus, fResourceFilterStatus, fTaskTagsStatus });
		updateStatus(status);
	}
	
	private IStatus validateCompliance() {
		StatusInfo status= new StatusInfo();
		if (checkValue(PREF_COMPLIANCE, VERSION_1_3)) {
			if (checkValue(PREF_SOURCE_COMPATIBILITY, VERSION_1_4)) {
				status.setError(JavaUIMessages.getString("CompilerPreferencePage.cpl13src14.error")); //$NON-NLS-1$
				return status;
			} else if (checkValue(PREF_CODEGEN_TARGET_PLATFORM, VERSION_1_4)) {
				status.setError(JavaUIMessages.getString("CompilerPreferencePage.cpl13trg14.error")); //$NON-NLS-1$
				return status;
			}
		}
		if (checkValue(PREF_SOURCE_COMPATIBILITY, VERSION_1_4)) {
			if (!checkValue(PREF_PB_ASSERT_AS_IDENTIFIER, ERROR)) {
				status.setError(JavaUIMessages.getString("CompilerPreferencePage.src14asrterr.error")); //$NON-NLS-1$
				return status;
			}
		}
		if (checkValue(PREF_SOURCE_COMPATIBILITY, VERSION_1_4)) {
			if (!checkValue(PREF_CODEGEN_TARGET_PLATFORM, VERSION_1_4)) {
				status.setError(JavaUIMessages.getString("CompilerPreferencePage.src14tgt14.error")); //$NON-NLS-1$
				return status;
			}
		}
		return status;
	}
	
	private IStatus validateMaxNumberProblems() {
		String number= (String) fWorkingValues.get(PREF_PB_MAX_PER_UNIT);
		StatusInfo status= new StatusInfo();
		if (number.length() == 0) {
			status.setError(JavaUIMessages.getString("CompilerPreferencePage.empty_input")); //$NON-NLS-1$
		} else {
			try {
				int value= Integer.parseInt(number);
				if (value <= 0) {
					status.setError(JavaUIMessages.getFormattedString("CompilerPreferencePage.invalid_input", number)); //$NON-NLS-1$
				}
			} catch (NumberFormatException e) {
				status.setError(JavaUIMessages.getFormattedString("CompilerPreferencePage.invalid_input", number)); //$NON-NLS-1$
			}
		}
		return status;
	}
	
	private IStatus validateResourceFilters() {
		String text= (String) fWorkingValues.get(PREF_RESOURCE_FILTER);
		
		IWorkspace workspace= ResourcesPlugin.getWorkspace();

		String[] filters= getFilters(text);
		for (int i= 0; i < filters.length; i++) {
			String fileName= filters[i].replace('*', 'x');
			int resourceType= IResource.FILE;
			int lastCharacter= fileName.length() - 1;
			if (lastCharacter >= 0 && fileName.charAt(lastCharacter) == '/') {
				fileName= fileName.substring(0, lastCharacter);
				resourceType= IResource.FOLDER;
			}
			IStatus status= workspace.validateName(fileName, resourceType);
			if (status.matches(IStatus.ERROR)) {
				String message= JavaUIMessages.getFormattedString("CompilerPreferencePage.filter.invalidsegment.error", status.getMessage()); //$NON-NLS-1$
				return new StatusInfo(IStatus.ERROR, message);
			}
		}
		return new StatusInfo();
	}
	
	private IStatus validateTaskTags() {
		String text= (String) fWorkingValues.get(PREF_COMPILER_TASK_TAGS);
		
		IWorkspace workspace= ResourcesPlugin.getWorkspace();

		String[] tags= getFilters(text);
		for (int i= 0; i < tags.length; i++) {
			if (tags[i].length() == 0) {
				String message= JavaUIMessages.getString("CompilerPreferencePage.task.invalidsegment.error"); //$NON-NLS-1$
				return new StatusInfo(IStatus.ERROR, message);			
			}	
		}
		return new StatusInfo();
	}	
	
	private String[] getFilters(String text) {
		StringTokenizer tok= new StringTokenizer(text, ","); //$NON-NLS-1$
		int nTokens= tok.countTokens();
		String[] res= new String[nTokens];
		for (int i= 0; i < res.length; i++) {
			res[i]= tok.nextToken();
		}
		return res;
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
		
		boolean hasChanges= false;
		for (int i= 0; i < allKeys.length; i++) {
			String key= allKeys[i];
			String val=  (String) fWorkingValues.get(key);
			String oldVal= (String) actualOptions.get(key);
			hasChanges= hasChanges | !val.equals(oldVal);
			
			actualOptions.put(key, val);
		}
		
		
		if (hasChanges) {
			String title= JavaUIMessages.getString("CompilerPreferencePage.needsbuild.title"); //$NON-NLS-1$
			String message= JavaUIMessages.getString("CompilerPreferencePage.needsbuild.message"); //$NON-NLS-1$
			
			MessageDialog dialog= new MessageDialog(getShell(), title, null, message, MessageDialog.QUESTION, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL }, 2);
			int res= dialog.open();
			if (res != 0 && res != 1) {
				JavaPlugin.getDefault().savePluginPreferences();
				return false;
			}
			
			JavaCore.setOptions(actualOptions);
			if (res == 0) {
				doFullBuild();
			}
		}
		JavaPlugin.getDefault().savePluginPreferences();
		return super.performOk();
	}
	
	public static boolean openQuestion(Shell parent, String title, String message) {
		MessageDialog dialog= new MessageDialog(parent, title, null, message, MessageDialog.QUESTION, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL }, 2);
		return dialog.open() == 0;
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
			String title= JavaUIMessages.getString("CompilerPreferencePage.builderror.title"); //$NON-NLS-1$
			String message= JavaUIMessages.getString("CompilerPreferencePage.builderror.message"); //$NON-NLS-1$
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
		for (int i= fTextBoxes.size() - 1; i >= 0; i--) {
			Text curr= (Text) fTextBoxes.get(i);
			String key= (String) curr.getData();
			
			String currValue= (String) fWorkingValues.get(key);
			curr.setText(currValue);
		}		
	}
	
	private void updateStatus(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}

}