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
package org.eclipse.jdt.internal.ui.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.util.TabFolderLayout;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;

/**
  */
public class CompilerConfigurationBlock extends OptionsConfigurationBlock {

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
	private static final String PREF_PB_UNUSED_PRIVATE= JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER;
	private static final String PREF_PB_STATIC_ACCESS_RECEIVER= JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER;
	private static final String PREF_PB_NO_EFFECT_ASSIGNMENT= JavaCore.COMPILER_PB_NO_EFFECT_ASSIGNMENT;
	private static final String PREF_PB_CHAR_ARRAY_IN_CONCAT= JavaCore.COMPILER_PB_CHAR_ARRAY_IN_STRING_CONCATENATION;
	

	private static final String PREF_SOURCE_COMPATIBILITY= JavaCore.COMPILER_SOURCE;
	private static final String PREF_COMPLIANCE= JavaCore.COMPILER_COMPLIANCE;

	private static final String PREF_RESOURCE_FILTER= JavaCore.CORE_JAVA_BUILD_RESOURCE_COPY_FILTER;
	private static final String PREF_BUILD_INVALID_CLASSPATH= JavaCore.CORE_JAVA_BUILD_INVALID_CLASSPATH;
	private static final String PREF_BUILD_CLEAN_OUTPUT_FOLDER= JavaCore.CORE_JAVA_BUILD_CLEAN_OUTPUT_FOLDER;
	private static final String PREF_ENABLE_EXCLUSION_PATTERNS= JavaCore.CORE_ENABLE_CLASSPATH_EXCLUSION_PATTERNS;
	private static final String PREF_ENABLE_MULTIPLE_OUTPUT_LOCATIONS= JavaCore.CORE_ENABLE_CLASSPATH_MULTIPLE_OUTPUT_LOCATIONS;

	private static final String PREF_PB_INCOMPLETE_BUILDPATH= JavaCore.CORE_INCOMPLETE_CLASSPATH;
	private static final String PREF_PB_CIRCULAR_BUILDPATH= JavaCore.CORE_CIRCULAR_CLASSPATH;
	private static final String PREF_COMPILER_PB_DEPRECATION_IN_DEPRECATED_CODE= JavaCore.COMPILER_PB_DEPRECATION_IN_DEPRECATED_CODE;
	private static final String PREF_PB_DUPLICATE_RESOURCE= JavaCore.CORE_JAVA_BUILD_DUPLICATE_RESOURCE;
	private static final String PREF_PB_INCOMPATIBLE_INTERFACE_METHOD= JavaCore.COMPILER_PB_INCOMPATIBLE_NON_INHERITED_INTERFACE_METHOD;

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
	
	private static final String CLEAN= JavaCore.CLEAN;

	private static final String ENABLED= JavaCore.ENABLED;
	private static final String DISABLED= JavaCore.DISABLED;
		
	private static final String DEFAULT= "default"; //$NON-NLS-1$
	private static final String USER= "user";	 //$NON-NLS-1$

	private ArrayList fComplianceControls;
	private PixelConverter fPixelConverter;

	private IStatus fComplianceStatus, fMaxNumberProblemsStatus, fResourceFilterStatus;

	public CompilerConfigurationBlock(IStatusChangeListener context, IJavaProject project) {
		super(context, project);
		
		fComplianceControls= new ArrayList();
			
		fComplianceStatus= new StatusInfo();
		fMaxNumberProblemsStatus= new StatusInfo();
		fResourceFilterStatus= new StatusInfo();
	}
	
	private final String[] KEYS= new String[] {
		PREF_LOCAL_VARIABLE_ATTR, PREF_LINE_NUMBER_ATTR, PREF_SOURCE_FILE_ATTR, PREF_CODEGEN_UNUSED_LOCAL,
		PREF_CODEGEN_TARGET_PLATFORM, PREF_PB_UNREACHABLE_CODE, PREF_PB_INVALID_IMPORT, PREF_PB_OVERRIDING_PACKAGE_DEFAULT_METHOD,
		PREF_PB_METHOD_WITH_CONSTRUCTOR_NAME, PREF_PB_DEPRECATION, PREF_PB_HIDDEN_CATCH_BLOCK, PREF_PB_UNUSED_LOCAL,
		PREF_PB_UNUSED_PARAMETER, PREF_PB_SYNTHETIC_ACCESS_EMULATION, PREF_PB_NON_EXTERNALIZED_STRINGS,
		PREF_PB_ASSERT_AS_IDENTIFIER, PREF_PB_UNUSED_IMPORT, PREF_PB_MAX_PER_UNIT, PREF_SOURCE_COMPATIBILITY, PREF_COMPLIANCE, 
		PREF_RESOURCE_FILTER, PREF_BUILD_INVALID_CLASSPATH, PREF_PB_STATIC_ACCESS_RECEIVER, PREF_PB_INCOMPLETE_BUILDPATH,
		PREF_PB_CIRCULAR_BUILDPATH, PREF_COMPILER_PB_DEPRECATION_IN_DEPRECATED_CODE, PREF_BUILD_CLEAN_OUTPUT_FOLDER,
		PREF_PB_DUPLICATE_RESOURCE, PREF_PB_NO_EFFECT_ASSIGNMENT, PREF_PB_INCOMPATIBLE_INTERFACE_METHOD,
		PREF_PB_UNUSED_PRIVATE, PREF_PB_CHAR_ARRAY_IN_CONCAT, PREF_ENABLE_EXCLUSION_PATTERNS, PREF_ENABLE_MULTIPLE_OUTPUT_LOCATIONS
	};	
	
	protected String[] getAllKeys() {
		return KEYS;	
	}
	
	protected final Map getOptions(boolean inheritJavaCoreOptions) {
		Map map= super.getOptions(inheritJavaCoreOptions);
		map.put(INTR_DEFAULT_COMPLIANCE, getCurrentCompliance(map));
		return map;
	}
	
	protected final Map getDefaultOptions() {
		Map map= super.getDefaultOptions();
		map.put(INTR_DEFAULT_COMPLIANCE, getCurrentCompliance(map));
		return map;
	}	
	
	
	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		fPixelConverter= new PixelConverter(parent);
		setShell(parent.getShell());
		
		TabFolder folder= new TabFolder(parent, SWT.NONE);
		folder.setLayout(new TabFolderLayout());	
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite problemsComposite= createProblemsTabContent(folder);
		Composite styleComposite= createStyleTabContent(folder);
		Composite complianceComposite= createComplianceTabContent(folder);
		Composite othersComposite= createOthersTabContent(folder);

		TabItem item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("CompilerConfigurationBlock.problems.tabtitle")); //$NON-NLS-1$
		item.setControl(problemsComposite);

		item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("CompilerConfigurationBlock.style.tabtitle")); //$NON-NLS-1$
		item.setControl(styleComposite);
	
		item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("CompilerConfigurationBlock.compliance.tabtitle")); //$NON-NLS-1$
		item.setControl(complianceComposite);
		
		item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("CompilerConfigurationBlock.others.tabtitle")); //$NON-NLS-1$
		item.setControl(othersComposite);		
		
		validateSettings(null, null);
	
		return folder;
	}

	private Composite createStyleTabContent(TabFolder folder) {
		String[] errorWarningIgnore= new String[] { ERROR, WARNING, IGNORE };
		
		String[] errorWarningIgnoreLabels= new String[] {
			PreferencesMessages.getString("CompilerConfigurationBlock.error"),  //$NON-NLS-1$
			PreferencesMessages.getString("CompilerConfigurationBlock.warning"), //$NON-NLS-1$
			PreferencesMessages.getString("CompilerConfigurationBlock.ignore") //$NON-NLS-1$
		};
		
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		//layout.verticalSpacing= 2;		
					
		Composite styleComposite= new Composite(folder, SWT.NULL);
		styleComposite.setLayout(layout);

		Label description= new Label(styleComposite, SWT.WRAP);
		description.setText(PreferencesMessages.getString("CompilerConfigurationBlock.style.description")); //$NON-NLS-1$
		GridData gd= new GridData();
		gd.horizontalSpan= 2;
		gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(50);
		description.setLayoutData(gd);

		String label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_overriding_pkg_dflt.label"); //$NON-NLS-1$
		addComboBox(styleComposite, label, PREF_PB_OVERRIDING_PACKAGE_DEFAULT_METHOD, errorWarningIgnore, errorWarningIgnoreLabels, 0);			

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_method_naming.label"); //$NON-NLS-1$
		addComboBox(styleComposite, label, PREF_PB_METHOD_WITH_CONSTRUCTOR_NAME, errorWarningIgnore, errorWarningIgnoreLabels, 0);			

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_incompatible_interface_method.label"); //$NON-NLS-1$
		addComboBox(styleComposite, label, PREF_PB_INCOMPATIBLE_INTERFACE_METHOD, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_hidden_catchblock.label"); //$NON-NLS-1$
		addComboBox(styleComposite, label, PREF_PB_HIDDEN_CATCH_BLOCK, errorWarningIgnore, errorWarningIgnoreLabels, 0);
		
		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_static_access_receiver.label"); //$NON-NLS-1$
		addComboBox(styleComposite, label, PREF_PB_STATIC_ACCESS_RECEIVER, errorWarningIgnore, errorWarningIgnoreLabels, 0);
		
		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_synth_access_emul.label"); //$NON-NLS-1$
		addComboBox(styleComposite, label, PREF_PB_SYNTHETIC_ACCESS_EMULATION, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_no_effect_assignment.label"); //$NON-NLS-1$
		addComboBox(styleComposite, label, PREF_PB_NO_EFFECT_ASSIGNMENT, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_char_array_in_concat.label"); //$NON-NLS-1$
		addComboBox(styleComposite, label, PREF_PB_CHAR_ARRAY_IN_CONCAT, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		return styleComposite;
	}


	private Composite createOthersTabContent(TabFolder folder) {
		String[] abortIgnoreValues= new String[] { ABORT, IGNORE };
		String[] cleanIgnoreValues= new String[] { CLEAN, IGNORE };
		String[] enableDisableValues= new String[] { ENABLED, DISABLED };
		
		String[] errorWarning= new String[] { ERROR, WARNING };
		
		String[] errorWarningLabels= new String[] {
			PreferencesMessages.getString("CompilerConfigurationBlock.error"),  //$NON-NLS-1$
			PreferencesMessages.getString("CompilerConfigurationBlock.warning") //$NON-NLS-1$
		};
			
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;

		Composite othersComposite= new Composite(folder, SWT.NULL);
		othersComposite.setLayout(layout);
		
		Label description= new Label(othersComposite, SWT.WRAP);
		description.setText(PreferencesMessages.getString("CompilerConfigurationBlock.build_warnings.description")); //$NON-NLS-1$
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
		
		String label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_incomplete_build_path.label"); //$NON-NLS-1$
		addComboBox(combos, label, PREF_PB_INCOMPLETE_BUILDPATH, errorWarning, errorWarningLabels, 0);	
		
		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_build_path_cycles.label"); //$NON-NLS-1$
		addComboBox(combos, label, PREF_PB_CIRCULAR_BUILDPATH, errorWarning, errorWarningLabels, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_duplicate_resources.label"); //$NON-NLS-1$
		addComboBox(combos, label, PREF_PB_DUPLICATE_RESOURCE, errorWarning, errorWarningLabels, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.build_invalid_classpath.label"); //$NON-NLS-1$
		addCheckBox(othersComposite, label, PREF_BUILD_INVALID_CLASSPATH, abortIgnoreValues, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.build_clean_outputfolder.label"); //$NON-NLS-1$
		addCheckBox(othersComposite, label, PREF_BUILD_CLEAN_OUTPUT_FOLDER, cleanIgnoreValues, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.enable_exclusion_patterns.label"); //$NON-NLS-1$
		addCheckBox(othersComposite, label, PREF_ENABLE_EXCLUSION_PATTERNS, enableDisableValues, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.enable_multiple_outputlocations.label"); //$NON-NLS-1$
		addCheckBox(othersComposite, label, PREF_ENABLE_MULTIPLE_OUTPUT_LOCATIONS, enableDisableValues, 0);
		
		
		description= new Label(othersComposite, SWT.WRAP);
		description.setText(PreferencesMessages.getString("CompilerConfigurationBlock.resource_filter.description")); //$NON-NLS-1$
		gd= new GridData(GridData.FILL);
		gd.horizontalSpan= 2;
		gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(60);
		description.setLayoutData(gd);
		
		label= PreferencesMessages.getString("CompilerConfigurationBlock.resource_filter.label"); //$NON-NLS-1$
		Text text= addTextField(othersComposite, label, PREF_RESOURCE_FILTER, 0, 0);
		gd= (GridData) text.getLayoutData();
		gd.grabExcessHorizontalSpace= true;
		gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(10);
		
		return othersComposite;

	}


	private Composite createProblemsTabContent(Composite folder) {
		String[] errorWarningIgnore= new String[] { ERROR, WARNING, IGNORE };
		
		String[] errorWarningIgnoreLabels= new String[] {
			PreferencesMessages.getString("CompilerConfigurationBlock.error"),  //$NON-NLS-1$
			PreferencesMessages.getString("CompilerConfigurationBlock.warning"), //$NON-NLS-1$
			PreferencesMessages.getString("CompilerConfigurationBlock.ignore") //$NON-NLS-1$
		};
		
		String[] enabledDisabled= new String[] { ENABLED, DISABLED };
			
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		//layout.verticalSpacing= 2;

		Composite problemsComposite= new Composite(folder, SWT.NULL);
		problemsComposite.setLayout(layout);

		Label description= new Label(problemsComposite, SWT.WRAP);
		description.setText(PreferencesMessages.getString("CompilerConfigurationBlock.problems.description")); //$NON-NLS-1$
		GridData gd= new GridData();
		gd.horizontalSpan= 2;
		gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(50);
		description.setLayoutData(gd);

		Composite combos= new Composite(problemsComposite, SWT.NULL);
		gd= new GridData();
		gd.horizontalSpan= 2;
		combos.setLayoutData(gd);
		GridLayout cl= new GridLayout();
		cl.numColumns=2; cl.marginWidth= 0; cl.marginHeight= 0;
		combos.setLayout(cl);

		String label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_unreachable_code.label"); //$NON-NLS-1$
		addComboBox(combos, label, PREF_PB_UNREACHABLE_CODE, errorWarningIgnore, errorWarningIgnoreLabels, 0);	
		
		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_invalid_import.label"); //$NON-NLS-1$
		addComboBox(combos, label, PREF_PB_INVALID_IMPORT, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_unused_local.label"); //$NON-NLS-1$
		addComboBox(combos, label, PREF_PB_UNUSED_LOCAL, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_unused_parameter.label"); //$NON-NLS-1$
		addComboBox(combos, label, PREF_PB_UNUSED_PARAMETER, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_unused_imports.label"); //$NON-NLS-1$
		addComboBox(combos, label, PREF_PB_UNUSED_IMPORT, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_unused_private.label"); //$NON-NLS-1$
		addComboBox(combos, label, PREF_PB_UNUSED_PRIVATE, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_non_externalized_strings.label"); //$NON-NLS-1$
		addComboBox(combos, label, PREF_PB_NON_EXTERNALIZED_STRINGS, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_deprecation.label"); //$NON-NLS-1$
		addComboBox(combos, label, PREF_PB_DEPRECATION, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_deprecation_in_deprecation.label"); //$NON-NLS-1$
		addCheckBox(problemsComposite, label, PREF_COMPILER_PB_DEPRECATION_IN_DEPRECATED_CODE, enabledDisabled, 0);

		Composite textField= new Composite(problemsComposite, SWT.NULL);
		gd= new GridData(GridData.FILL | GridData.GRAB_HORIZONTAL);
		gd.horizontalSpan= 2;
		textField.setLayoutData(gd);
		cl= new GridLayout();
		cl.numColumns=2; cl.marginWidth= 0; cl.marginHeight= 0;
		textField.setLayout(cl);
		
		gd= new GridData();
		gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(6);
		
		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_max_per_unit.label"); //$NON-NLS-1$
		Text text= addTextField(textField, label, PREF_PB_MAX_PER_UNIT, 0, 0);
		text.setTextLimit(6);
		text.setLayoutData(gd);


		return problemsComposite;
	}
	
	
	private Composite createComplianceTabContent(Composite folder) {
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;

		String[] values34= new String[] { VERSION_1_3, VERSION_1_4 };
		String[] values34Labels= new String[] {
			PreferencesMessages.getString("CompilerConfigurationBlock.version13"),  //$NON-NLS-1$
			PreferencesMessages.getString("CompilerConfigurationBlock.version14") //$NON-NLS-1$
		};

		Composite compComposite= new Composite(folder, SWT.NULL);
		compComposite.setLayout(layout);

		layout= new GridLayout();
		layout.numColumns= 2;

		Group group= new Group(compComposite, SWT.NONE);
		group.setText(PreferencesMessages.getString("CompilerConfigurationBlock.compliance.group.label")); //$NON-NLS-1$
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setLayout(layout);
	
		String label= PreferencesMessages.getString("CompilerConfigurationBlock.compiler_compliance.label"); //$NON-NLS-1$
		addComboBox(group, label, PREF_COMPLIANCE, values34, values34Labels, 0);	

		label= PreferencesMessages.getString("CompilerConfigurationBlock.default_settings.label"); //$NON-NLS-1$
		addCheckBox(group, label, INTR_DEFAULT_COMPLIANCE, new String[] { DEFAULT, USER }, 0);	

		int indent= fPixelConverter.convertWidthInCharsToPixels(2);
		Control[] otherChildren= group.getChildren();	
				
		String[] values14= new String[] { VERSION_1_1, VERSION_1_2, VERSION_1_3, VERSION_1_4 };
		String[] values14Labels= new String[] {
			PreferencesMessages.getString("CompilerConfigurationBlock.version11"),  //$NON-NLS-1$
			PreferencesMessages.getString("CompilerConfigurationBlock.version12"), //$NON-NLS-1$
			PreferencesMessages.getString("CompilerConfigurationBlock.version13"), //$NON-NLS-1$
			PreferencesMessages.getString("CompilerConfigurationBlock.version14") //$NON-NLS-1$
		};
		
		label= PreferencesMessages.getString("CompilerConfigurationBlock.codegen_targetplatform.label"); //$NON-NLS-1$
		addComboBox(group, label, PREF_CODEGEN_TARGET_PLATFORM, values14, values14Labels, indent);	

		label= PreferencesMessages.getString("CompilerConfigurationBlock.source_compatibility.label"); //$NON-NLS-1$
		addComboBox(group, label, PREF_SOURCE_COMPATIBILITY, values34, values34Labels, indent);	

		String[] errorWarningIgnore= new String[] { ERROR, WARNING, IGNORE };
		
		String[] errorWarningIgnoreLabels= new String[] {
			PreferencesMessages.getString("CompilerConfigurationBlock.error"),  //$NON-NLS-1$
			PreferencesMessages.getString("CompilerConfigurationBlock.warning"), //$NON-NLS-1$
			PreferencesMessages.getString("CompilerConfigurationBlock.ignore") //$NON-NLS-1$
		};

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_assert_as_identifier.label"); //$NON-NLS-1$
		addComboBox(group, label, PREF_PB_ASSERT_AS_IDENTIFIER, errorWarningIgnore, errorWarningIgnoreLabels, indent);		

		Control[] allChildren= group.getChildren();
		fComplianceControls.addAll(Arrays.asList(allChildren));
		fComplianceControls.removeAll(Arrays.asList(otherChildren));

		layout= new GridLayout();
		layout.numColumns= 2;

		group= new Group(compComposite, SWT.NONE);
		group.setText(PreferencesMessages.getString("CompilerConfigurationBlock.classfiles.group.label")); //$NON-NLS-1$
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setLayout(layout);

		String[] generateValues= new String[] { GENERATE, DO_NOT_GENERATE };

		label= PreferencesMessages.getString("CompilerConfigurationBlock.variable_attr.label"); //$NON-NLS-1$
		addCheckBox(group, label, PREF_LOCAL_VARIABLE_ATTR, generateValues, 0);
		
		label= PreferencesMessages.getString("CompilerConfigurationBlock.line_number_attr.label"); //$NON-NLS-1$
		addCheckBox(group, label, PREF_LINE_NUMBER_ATTR, generateValues, 0);		

		label= PreferencesMessages.getString("CompilerConfigurationBlock.source_file_attr.label"); //$NON-NLS-1$
		addCheckBox(group, label, PREF_SOURCE_FILE_ATTR, generateValues, 0);		

		label= PreferencesMessages.getString("CompilerConfigurationBlock.codegen_unused_local.label"); //$NON-NLS-1$
		addCheckBox(group, label, PREF_CODEGEN_UNUSED_LOCAL, new String[] { PRESERVE, OPTIMIZE_OUT }, 0);	

		
		return compComposite;
	}
		
	
	/* (non-javadoc)
	 * Update fields and validate.
	 * @param changedKey Key that changed, or null, if all changed.
	 */	
	protected void validateSettings(String changedKey, String newValue) {
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
			} else {
				return;
			}
		} else {
			updateComplianceEnableState();
			fComplianceStatus= validateCompliance();
			fMaxNumberProblemsStatus= validateMaxNumberProblems();
			fResourceFilterStatus= validateResourceFilters();
		}		
		IStatus status= StatusUtil.getMostSevere(new IStatus[] { fComplianceStatus, fMaxNumberProblemsStatus, fResourceFilterStatus });
		fContext.statusChanged(status);
	}
	
	private IStatus validateCompliance() {
		StatusInfo status= new StatusInfo();
		if (checkValue(PREF_COMPLIANCE, VERSION_1_3)) {
			if (checkValue(PREF_SOURCE_COMPATIBILITY, VERSION_1_4)) {
				status.setError(PreferencesMessages.getString("CompilerConfigurationBlock.cpl13src14.error")); //$NON-NLS-1$
				return status;
			} else if (checkValue(PREF_CODEGEN_TARGET_PLATFORM, VERSION_1_4)) {
				status.setError(PreferencesMessages.getString("CompilerConfigurationBlock.cpl13trg14.error")); //$NON-NLS-1$
				return status;
			}
		}
		if (checkValue(PREF_SOURCE_COMPATIBILITY, VERSION_1_4)) {
			if (!checkValue(PREF_PB_ASSERT_AS_IDENTIFIER, ERROR)) {
				status.setError(PreferencesMessages.getString("CompilerConfigurationBlock.src14asrterr.error")); //$NON-NLS-1$
				return status;
			}
		}
		if (checkValue(PREF_SOURCE_COMPATIBILITY, VERSION_1_4)) {
			if (!checkValue(PREF_CODEGEN_TARGET_PLATFORM, VERSION_1_4)) {
				status.setError(PreferencesMessages.getString("CompilerConfigurationBlock.src14tgt14.error")); //$NON-NLS-1$
				return status;
			}
		}
		return status;
	}
	
	private IStatus validateMaxNumberProblems() {
		String number= (String) fWorkingValues.get(PREF_PB_MAX_PER_UNIT);
		StatusInfo status= new StatusInfo();
		if (number.length() == 0) {
			status.setError(PreferencesMessages.getString("CompilerConfigurationBlock.empty_input")); //$NON-NLS-1$
		} else {
			try {
				int value= Integer.parseInt(number);
				if (value <= 0) {
					status.setError(PreferencesMessages.getFormattedString("CompilerConfigurationBlock.invalid_input", number)); //$NON-NLS-1$
				}
			} catch (NumberFormatException e) {
				status.setError(PreferencesMessages.getFormattedString("CompilerConfigurationBlock.invalid_input", number)); //$NON-NLS-1$
			}
		}
		return status;
	}
	
	private IStatus validateResourceFilters() {
		String text= (String) fWorkingValues.get(PREF_RESOURCE_FILTER);
		
		IWorkspace workspace= ResourcesPlugin.getWorkspace();

		String[] filters= getTokens(text, ","); //$NON-NLS-1$
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
				String message= PreferencesMessages.getFormattedString("CompilerConfigurationBlock.filter.invalidsegment.error", status.getMessage()); //$NON-NLS-1$
				return new StatusInfo(IStatus.ERROR, message);
			}
		}
		return new StatusInfo();
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
	private static String getCurrentCompliance(Map map) {
		Object complianceLevel= map.get(PREF_COMPLIANCE);
		if ((VERSION_1_3.equals(complianceLevel)
				&& IGNORE.equals(map.get(PREF_PB_ASSERT_AS_IDENTIFIER))
				&& VERSION_1_3.equals(map.get(PREF_SOURCE_COMPATIBILITY))
				&& VERSION_1_1.equals(map.get(PREF_CODEGEN_TARGET_PLATFORM)))
			|| (VERSION_1_4.equals(complianceLevel)
				&& ERROR.equals(map.get(PREF_PB_ASSERT_AS_IDENTIFIER))
				&& VERSION_1_4.equals(map.get(PREF_SOURCE_COMPATIBILITY))
				&& VERSION_1_4.equals(map.get(PREF_CODEGEN_TARGET_PLATFORM)))) {
			return DEFAULT;
		}
		return USER;
	}
	
	
	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		String title= PreferencesMessages.getString("CompilerConfigurationBlock.needsbuild.title"); //$NON-NLS-1$
		String message;
		if (workspaceSettings) {
			message= PreferencesMessages.getString("CompilerConfigurationBlock.needsfullbuild.message"); //$NON-NLS-1$
		} else {
			message= PreferencesMessages.getString("CompilerConfigurationBlock.needsprojectbuild.message"); //$NON-NLS-1$
		}
		return new String[] { title, message };
	}	
	
}
