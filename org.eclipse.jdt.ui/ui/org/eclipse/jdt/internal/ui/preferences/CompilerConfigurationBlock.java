/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
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
	private static final String PREF_CODEGEN_INLINE_JSR_BYTECODE= JavaCore.COMPILER_CODEGEN_INLINE_JSR_BYTECODE;
	
	//private static final String PREF_PB_UNREACHABLE_CODE= JavaCore.COMPILER_PB_UNREACHABLE_CODE;
	//private static final String PREF_PB_INVALID_IMPORT= JavaCore.COMPILER_PB_INVALID_IMPORT;
	private static final String PREF_PB_OVERRIDING_PACKAGE_DEFAULT_METHOD= JavaCore.COMPILER_PB_OVERRIDING_PACKAGE_DEFAULT_METHOD;
	private static final String PREF_PB_METHOD_WITH_CONSTRUCTOR_NAME= JavaCore.COMPILER_PB_METHOD_WITH_CONSTRUCTOR_NAME;
	private static final String PREF_PB_DEPRECATION= JavaCore.COMPILER_PB_DEPRECATION;
	private static final String PREF_PB_DEPRECATION_WHEN_OVERRIDING= JavaCore.COMPILER_PB_DEPRECATION_WHEN_OVERRIDING_DEPRECATED_METHOD;
	
	private static final String PREF_PB_HIDDEN_CATCH_BLOCK= JavaCore.COMPILER_PB_HIDDEN_CATCH_BLOCK;
	private static final String PREF_PB_UNUSED_LOCAL= JavaCore.COMPILER_PB_UNUSED_LOCAL;
	private static final String PREF_PB_UNUSED_PARAMETER= JavaCore.COMPILER_PB_UNUSED_PARAMETER;
	private static final String PREF_PB_SIGNAL_PARAMETER_IN_OVERRIDING= JavaCore.COMPILER_PB_UNUSED_PARAMETER_WHEN_OVERRIDING_CONCRETE;
	private static final String PREF_PB_SIGNAL_PARAMETER_IN_ABSTRACT= JavaCore.COMPILER_PB_UNUSED_PARAMETER_WHEN_IMPLEMENTING_ABSTRACT;
	private static final String PREF_PB_SYNTHETIC_ACCESS_EMULATION= JavaCore.COMPILER_PB_SYNTHETIC_ACCESS_EMULATION;
	private static final String PREF_PB_NON_EXTERNALIZED_STRINGS= JavaCore.COMPILER_PB_NON_NLS_STRING_LITERAL;
	private static final String PREF_PB_ASSERT_AS_IDENTIFIER= JavaCore.COMPILER_PB_ASSERT_IDENTIFIER;
	private static final String PREF_PB_MAX_PER_UNIT= JavaCore.COMPILER_PB_MAX_PER_UNIT;
	private static final String PREF_PB_UNUSED_IMPORT= JavaCore.COMPILER_PB_UNUSED_IMPORT;
	private static final String PREF_PB_UNUSED_PRIVATE= JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER;
	private static final String PREF_PB_STATIC_ACCESS_RECEIVER= JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER;
	private static final String PREF_PB_NO_EFFECT_ASSIGNMENT= JavaCore.COMPILER_PB_NO_EFFECT_ASSIGNMENT;
	private static final String PREF_PB_CHAR_ARRAY_IN_CONCAT= JavaCore.COMPILER_PB_CHAR_ARRAY_IN_STRING_CONCATENATION;
	private static final String PREF_PB_POSSIBLE_ACCIDENTAL_BOOLEAN_ASSIGNMENT= JavaCore.COMPILER_PB_POSSIBLE_ACCIDENTAL_BOOLEAN_ASSIGNMENT;
	private static final String PREF_PB_LOCAL_VARIABLE_HIDING= JavaCore.COMPILER_PB_LOCAL_VARIABLE_HIDING;
	private static final String PREF_PB_FIELD_HIDING= JavaCore.COMPILER_PB_FIELD_HIDING;
	private static final String PREF_PB_SPECIAL_PARAMETER_HIDING_FIELD= JavaCore.COMPILER_PB_SPECIAL_PARAMETER_HIDING_FIELD;
	private static final String PREF_PB_INDIRECT_STATIC_ACCESS= JavaCore.COMPILER_PB_INDIRECT_STATIC_ACCESS;
	private static final String PREF_PB_EMPTY_STATEMENT= JavaCore.COMPILER_PB_EMPTY_STATEMENT;
	private static final String PREF_PB_UNNECESSARY_ELSE= JavaCore.COMPILER_PB_UNNECESSARY_ELSE;
	private static final String PREF_PB_UNNECESSARY_TYPE_CHECK= JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK;
	
	private static final String PREF_JAVADOC_SUPPORT= JavaCore.COMPILER_DOC_COMMENT_SUPPORT;

	private static final String PREF_PB_INVALID_JAVADOC= JavaCore.COMPILER_PB_INVALID_JAVADOC;
	private static final String PREF_PB_INVALID_JAVADOC_TAGS= JavaCore.COMPILER_PB_INVALID_JAVADOC_TAGS;
	private static final String PREF_PB_INVALID_JAVADOC_TAGS_VISIBILITY= JavaCore.COMPILER_PB_INVALID_JAVADOC_TAGS_VISIBILITY;

	private static final String PREF_PB_MISSING_JAVADOC_TAGS= JavaCore.COMPILER_PB_MISSING_JAVADOC_TAGS;
	private static final String PREF_PB_MISSING_JAVADOC_TAGS_VISIBILITY= JavaCore.COMPILER_PB_MISSING_JAVADOC_TAGS_VISIBILITY;
	private static final String PREF_PB_MISSING_JAVADOC_TAGS_OVERRIDING= JavaCore.COMPILER_PB_MISSING_JAVADOC_TAGS_OVERRIDING;

	private static final String PREF_PB_MISSING_JAVADOC_COMMENTS= JavaCore.COMPILER_PB_MISSING_JAVADOC_COMMENTS;
	private static final String PREF_PB_MISSING_JAVADOC_COMMENTS_VISIBILITY= JavaCore.COMPILER_PB_MISSING_JAVADOC_COMMENTS_VISIBILITY;
	private static final String PREF_PB_MISSING_JAVADOC_COMMENTS_OVERRIDING= JavaCore.COMPILER_PB_MISSING_JAVADOC_COMMENTS_OVERRIDING;
	
	private static final String PREF_SOURCE_COMPATIBILITY= JavaCore.COMPILER_SOURCE;
	private static final String PREF_COMPLIANCE= JavaCore.COMPILER_COMPLIANCE;

	private static final String PREF_RESOURCE_FILTER= JavaCore.CORE_JAVA_BUILD_RESOURCE_COPY_FILTER;
	private static final String PREF_BUILD_INVALID_CLASSPATH= JavaCore.CORE_JAVA_BUILD_INVALID_CLASSPATH;
	private static final String PREF_BUILD_CLEAN_OUTPUT_FOLDER= JavaCore.CORE_JAVA_BUILD_CLEAN_OUTPUT_FOLDER;
	private static final String PREF_ENABLE_EXCLUSION_PATTERNS= JavaCore.CORE_ENABLE_CLASSPATH_EXCLUSION_PATTERNS;
	private static final String PREF_ENABLE_MULTIPLE_OUTPUT_LOCATIONS= JavaCore.CORE_ENABLE_CLASSPATH_MULTIPLE_OUTPUT_LOCATIONS;

	private static final String PREF_PB_INCOMPLETE_BUILDPATH= JavaCore.CORE_INCOMPLETE_CLASSPATH;
	private static final String PREF_PB_CIRCULAR_BUILDPATH= JavaCore.CORE_CIRCULAR_CLASSPATH;
	private static final String PREF_PB_INCOMPATIBLE_JDK_LEVEL= JavaCore.CORE_INCOMPATIBLE_JDK_LEVEL;
	private static final String PREF_PB_DUPLICATE_RESOURCE= JavaCore.CORE_JAVA_BUILD_DUPLICATE_RESOURCE;

	private static final String PREF_PB_DEPRECATION_IN_DEPRECATED_CODE= JavaCore.COMPILER_PB_DEPRECATION_IN_DEPRECATED_CODE;
	private static final String PREF_PB_INCOMPATIBLE_INTERFACE_METHOD= JavaCore.COMPILER_PB_INCOMPATIBLE_NON_INHERITED_INTERFACE_METHOD;

	private static final String PREF_PB_UNDOCUMENTED_EMPTY_BLOCK= JavaCore.COMPILER_PB_UNDOCUMENTED_EMPTY_BLOCK;
	private static final String PREF_PB_FINALLY_BLOCK_NOT_COMPLETING= JavaCore.COMPILER_PB_FINALLY_BLOCK_NOT_COMPLETING;
	private static final String PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION= JavaCore.COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION;
	private static final String PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING= JavaCore.COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING;
	private static final String PREF_PB_UNQUALIFIED_FIELD_ACCESS= JavaCore.COMPILER_PB_UNQUALIFIED_FIELD_ACCESS;

	private static final String PREF_15_PB_UNSAVE_TYPE_OPERATION= "org.eclipse.jdt.core.compiler.problem.unsafeTypeOperation"; //$NON-NLS-1$
	private static final String PREF_15_PB_FINAL_PARAM_BOUND= "org.eclipse.jdt.core.compiler.problem.finalParameterBound"; //$NON-NLS-1$
	
	
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
	private static final String VERSION_1_5= JavaCore.VERSION_1_5;
	
	private static final String ERROR= JavaCore.ERROR;
	private static final String WARNING= JavaCore.WARNING;
	private static final String IGNORE= JavaCore.IGNORE;

	private static final String ABORT= JavaCore.ABORT;
	private static final String CLEAN= JavaCore.CLEAN;

	private static final String ENABLED= JavaCore.ENABLED;
	private static final String DISABLED= JavaCore.DISABLED;
	
	private static final String PUBLIC= JavaCore.PUBLIC;
	private static final String PROTECTED= JavaCore.PROTECTED;
	private static final String DEFAULT= JavaCore.DEFAULT;
	private static final String PRIVATE= JavaCore.PRIVATE;
	
	private static final String DEFAULT_CONF= "default"; //$NON-NLS-1$
	private static final String USER_CONF= "user";	 //$NON-NLS-1$

	private ArrayList fComplianceControls;
	private PixelConverter fPixelConverter;
	private Composite fJavadocComposite;
	
	private String[] fRememberedUserCompliance;
	
	private static final int IDX_ASSERT_AS_IDENTIFIER= 0;
	private static final int IDX_SOURCE_COMPATIBILITY= 1;
	private static final int IDX_CODEGEN_TARGET_PLATFORM= 2;
	private static final int IDX_COMPLIANCE= 3;
	private static final int IDX_INLINE_JSR_BYTECODE= 4;

	private IStatus fComplianceStatus, fMaxNumberProblemsStatus, fResourceFilterStatus;

	public CompilerConfigurationBlock(IStatusChangeListener context, IJavaProject project) {
		super(context, project, getKeys());
		
		fComplianceControls= new ArrayList();
			
		fComplianceStatus= new StatusInfo();
		fMaxNumberProblemsStatus= new StatusInfo();
		fResourceFilterStatus= new StatusInfo();
		
		// compatibilty code for the merge of the two option PB_SIGNAL_PARAMETER: 
		if (ENABLED.equals(fWorkingValues.get(PREF_PB_SIGNAL_PARAMETER_IN_ABSTRACT))) {
			fWorkingValues.put(PREF_PB_SIGNAL_PARAMETER_IN_OVERRIDING, ENABLED);
		}
		fRememberedUserCompliance= new String[] {
			(String) fWorkingValues.get(PREF_PB_ASSERT_AS_IDENTIFIER),
			(String) fWorkingValues.get(PREF_SOURCE_COMPATIBILITY),
			(String) fWorkingValues.get(PREF_CODEGEN_TARGET_PLATFORM),
			(String) fWorkingValues.get(PREF_COMPLIANCE),
			(String) fWorkingValues.get(PREF_CODEGEN_INLINE_JSR_BYTECODE),
		};
	}
	
	private static String[] getKeys() {
		String[] keys= new String[] {
				PREF_LOCAL_VARIABLE_ATTR, PREF_LINE_NUMBER_ATTR, PREF_SOURCE_FILE_ATTR, PREF_CODEGEN_UNUSED_LOCAL,
				PREF_CODEGEN_INLINE_JSR_BYTECODE,
				PREF_CODEGEN_TARGET_PLATFORM, PREF_PB_OVERRIDING_PACKAGE_DEFAULT_METHOD,
				PREF_PB_METHOD_WITH_CONSTRUCTOR_NAME, PREF_PB_DEPRECATION, PREF_PB_HIDDEN_CATCH_BLOCK, PREF_PB_UNUSED_LOCAL,
				PREF_PB_UNUSED_PARAMETER, PREF_PB_SYNTHETIC_ACCESS_EMULATION, PREF_PB_NON_EXTERNALIZED_STRINGS,
				PREF_PB_ASSERT_AS_IDENTIFIER, PREF_PB_UNUSED_IMPORT, PREF_PB_MAX_PER_UNIT, PREF_SOURCE_COMPATIBILITY, PREF_COMPLIANCE, 
				PREF_PB_STATIC_ACCESS_RECEIVER, PREF_PB_DEPRECATION_IN_DEPRECATED_CODE, 
				PREF_PB_NO_EFFECT_ASSIGNMENT, PREF_PB_INCOMPATIBLE_INTERFACE_METHOD,
				PREF_PB_UNUSED_PRIVATE, PREF_PB_CHAR_ARRAY_IN_CONCAT, PREF_PB_UNNECESSARY_ELSE,
				PREF_PB_POSSIBLE_ACCIDENTAL_BOOLEAN_ASSIGNMENT, PREF_PB_LOCAL_VARIABLE_HIDING, PREF_PB_FIELD_HIDING,
				PREF_PB_SPECIAL_PARAMETER_HIDING_FIELD, PREF_PB_INDIRECT_STATIC_ACCESS,
				PREF_PB_EMPTY_STATEMENT, PREF_PB_SIGNAL_PARAMETER_IN_OVERRIDING, PREF_PB_SIGNAL_PARAMETER_IN_ABSTRACT,
				PREF_PB_UNNECESSARY_TYPE_CHECK, PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION, PREF_PB_UNQUALIFIED_FIELD_ACCESS,
				PREF_PB_UNDOCUMENTED_EMPTY_BLOCK, PREF_PB_FINALLY_BLOCK_NOT_COMPLETING, PREF_PB_DEPRECATION_WHEN_OVERRIDING,
				PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING,
				PREF_JAVADOC_SUPPORT,
				PREF_PB_INVALID_JAVADOC, PREF_PB_INVALID_JAVADOC_TAGS_VISIBILITY, PREF_PB_INVALID_JAVADOC_TAGS_VISIBILITY,
				PREF_PB_MISSING_JAVADOC_TAGS, PREF_PB_MISSING_JAVADOC_TAGS_VISIBILITY, PREF_PB_MISSING_JAVADOC_TAGS_OVERRIDING,
				PREF_PB_MISSING_JAVADOC_COMMENTS, PREF_PB_MISSING_JAVADOC_COMMENTS_VISIBILITY, PREF_PB_MISSING_JAVADOC_COMMENTS_OVERRIDING,
				
				PREF_RESOURCE_FILTER, PREF_BUILD_INVALID_CLASSPATH, PREF_PB_INCOMPLETE_BUILDPATH, PREF_PB_CIRCULAR_BUILDPATH,
				PREF_BUILD_CLEAN_OUTPUT_FOLDER, PREF_PB_DUPLICATE_RESOURCE,
				PREF_PB_INCOMPATIBLE_JDK_LEVEL, PREF_ENABLE_EXCLUSION_PATTERNS, PREF_ENABLE_MULTIPLE_OUTPUT_LOCATIONS
			};
		if (JavaModelUtil.isJDTCore_1_5()) {
			String[] newKeys= new String[] {
					PREF_15_PB_UNSAVE_TYPE_OPERATION, PREF_15_PB_FINAL_PARAM_BOUND
			};
			String[] merged= new String[keys.length + newKeys.length];
			System.arraycopy(keys, 0, merged, 0, keys.length);
			System.arraycopy(newKeys, 0, merged, keys.length, newKeys.length);
			keys= merged;
		}
		return keys;
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
	
	
	/*
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		fPixelConverter= new PixelConverter(parent);
		setShell(parent.getShell());
		
		TabFolder folder= new TabFolder(parent, SWT.NONE);
		folder.setLayout(new TabFolderLayout());	
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite commonComposite= createStyleTabContent(folder);
		Composite unusedComposite= createUnusedCodeTabContent(folder);
		Composite advancedComposite= createAdvancedTabContent(folder);
		
		Composite javadocComposite= createJavadocTabContent(folder);
		Composite complianceComposite= createComplianceTabContent(folder);
		Composite othersComposite= createBuildPathTabContent(folder);
		
		TabItem item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("CompilerConfigurationBlock.common.tabtitle")); //$NON-NLS-1$
		item.setControl(commonComposite);

		item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("CompilerConfigurationBlock.advanced.tabtitle")); //$NON-NLS-1$
		item.setControl(advancedComposite);

		item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("CompilerConfigurationBlock.unused.tabtitle")); //$NON-NLS-1$
		item.setControl(unusedComposite);
		
		item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("CompilerConfigurationBlock.javadoc.tabtitle")); //$NON-NLS-1$
		item.setControl(javadocComposite);
	
		item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("CompilerConfigurationBlock.compliance.tabtitle")); //$NON-NLS-1$
		item.setControl(complianceComposite);
		
		item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("CompilerConfigurationBlock.others.tabtitle")); //$NON-NLS-1$
		item.setControl(othersComposite);		
		
		if (JavaModelUtil.isJDTCore_1_5()) {
			Composite extraComposite= create15TabContent(folder);
			
			item= new TabItem(folder, SWT.NONE);
			item.setText(PreferencesMessages.getString("CompilerConfigurationBlock.15.tabtitle")); //$NON-NLS-1$
			item.setControl(extraComposite);		
		}
		
		validateSettings(null, null);
	
		return folder;
	}

	private Composite createStyleTabContent(Composite folder) {
		String[] errorWarningIgnore= new String[] { ERROR, WARNING, IGNORE };
		
		String[] errorWarningIgnoreLabels= new String[] {
			PreferencesMessages.getString("CompilerConfigurationBlock.error"),  //$NON-NLS-1$
			PreferencesMessages.getString("CompilerConfigurationBlock.warning"), //$NON-NLS-1$
			PreferencesMessages.getString("CompilerConfigurationBlock.ignore") //$NON-NLS-1$
		};
		
		int nColumns= 3;
		
		GridLayout layout= new GridLayout();
		layout.numColumns= nColumns;

		Composite composite= new Composite(folder, SWT.NULL);
		composite.setLayout(layout);
		
		Label description= new Label(composite, SWT.WRAP);
		description.setText(PreferencesMessages.getString("CompilerConfigurationBlock.common.description")); //$NON-NLS-1$
		GridData gd= new GridData();
		gd.horizontalSpan= nColumns;
		//gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(60);
		description.setLayoutData(gd);		

		String label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_overriding_pkg_dflt.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_OVERRIDING_PACKAGE_DEFAULT_METHOD, errorWarningIgnore, errorWarningIgnoreLabels, 0);			

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_method_naming.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_METHOD_WITH_CONSTRUCTOR_NAME, errorWarningIgnore, errorWarningIgnoreLabels, 0);			
		
		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_static_access_receiver.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_STATIC_ACCESS_RECEIVER, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_indirect_access_to_static.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_INDIRECT_STATIC_ACCESS, errorWarningIgnore, errorWarningIgnoreLabels, 0);	
		
		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_no_effect_assignment.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_NO_EFFECT_ASSIGNMENT, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_accidential_assignement.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_POSSIBLE_ACCIDENTAL_BOOLEAN_ASSIGNMENT, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_unqualified_field_access.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_UNQUALIFIED_FIELD_ACCESS, errorWarningIgnore, errorWarningIgnoreLabels, 0);
		
		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_finally_block_not_completing.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_FINALLY_BLOCK_NOT_COMPLETING, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_empty_statement.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_EMPTY_STATEMENT, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_undocumented_empty_block.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_UNDOCUMENTED_EMPTY_BLOCK, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		
		return composite;
	}

	private Composite createAdvancedTabContent(TabFolder folder) {
		String[] errorWarningIgnore= new String[] { ERROR, WARNING, IGNORE };
		
		String[] errorWarningIgnoreLabels= new String[] {
			PreferencesMessages.getString("CompilerConfigurationBlock.error"),  //$NON-NLS-1$
			PreferencesMessages.getString("CompilerConfigurationBlock.warning"), //$NON-NLS-1$
			PreferencesMessages.getString("CompilerConfigurationBlock.ignore") //$NON-NLS-1$
		};
		
		String[] enabledDisabled= new String[] { ENABLED, DISABLED };
		
		int nColumns= 3;
		
		GridLayout layout= new GridLayout();
		layout.numColumns= nColumns;
					
		Composite composite= new Composite(folder, SWT.NULL);
		composite.setLayout(layout);

		Label description= new Label(composite, SWT.WRAP);
		description.setText(PreferencesMessages.getString("CompilerConfigurationBlock.advanced.description")); //$NON-NLS-1$
		GridData gd= new GridData();
		gd.horizontalSpan= nColumns;
		//gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(60);
		description.setLayoutData(gd);
			
		String label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_synth_access_emul.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_SYNTHETIC_ACCESS_EMULATION, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_local_variable_hiding.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_LOCAL_VARIABLE_HIDING, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		int indent= fPixelConverter.convertWidthInCharsToPixels(2);
		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_special_param_hiding.label"); //$NON-NLS-1$
		addCheckBox(composite, label, PREF_PB_SPECIAL_PARAMETER_HIDING_FIELD, enabledDisabled, indent);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_field_hiding.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_FIELD_HIDING, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_incompatible_interface_method.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_INCOMPATIBLE_INTERFACE_METHOD, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_char_array_in_concat.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_CHAR_ARRAY_IN_CONCAT, errorWarningIgnore, errorWarningIgnoreLabels, 0);
		
		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_deprecation.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_DEPRECATION, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_deprecation_in_deprecation.label"); //$NON-NLS-1$
		addCheckBox(composite, label, PREF_PB_DEPRECATION_IN_DEPRECATED_CODE, enabledDisabled, indent);		
	
		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_deprecation_when_overriding.label"); //$NON-NLS-1$
		addCheckBox(composite, label, PREF_PB_DEPRECATION_WHEN_OVERRIDING, enabledDisabled, indent);
		
		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_non_externalized_strings.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_NON_EXTERNALIZED_STRINGS, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		
		gd= new GridData();
		gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(6);
		
		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_max_per_unit.label"); //$NON-NLS-1$
		Text text= addTextField(composite, label, PREF_PB_MAX_PER_UNIT, 0, 0);
		text.setTextLimit(6);
		text.setLayoutData(gd);

		return composite;
	}

	private Composite createUnusedCodeTabContent(TabFolder folder) {
		String[] errorWarningIgnore= new String[] { ERROR, WARNING, IGNORE };
		
		String[] errorWarningIgnoreLabels= new String[] {
			PreferencesMessages.getString("CompilerConfigurationBlock.error"),  //$NON-NLS-1$
			PreferencesMessages.getString("CompilerConfigurationBlock.warning"), //$NON-NLS-1$
			PreferencesMessages.getString("CompilerConfigurationBlock.ignore") //$NON-NLS-1$
		};
		
		String[] enabledDisabled= new String[] { ENABLED, DISABLED };
		
		int nColumns= 3;
		
		GridLayout layout= new GridLayout();
		layout.numColumns= nColumns;
					
		Composite composite= new Composite(folder, SWT.NULL);
		composite.setLayout(layout);

		Label description= new Label(composite, SWT.WRAP);
		description.setText(PreferencesMessages.getString("CompilerConfigurationBlock.unused.description")); //$NON-NLS-1$
		GridData gd= new GridData();
		gd.horizontalSpan= nColumns;
		//gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(60);
		description.setLayoutData(gd);
		
		String label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_unused_local.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_UNUSED_LOCAL, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_unused_parameter.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_UNUSED_PARAMETER, errorWarningIgnore, errorWarningIgnoreLabels, 0);
		
		int indent= fPixelConverter.convertWidthInCharsToPixels(2);
		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_signal_param_in_overriding.label"); //$NON-NLS-1$
		addCheckBox(composite, label, PREF_PB_SIGNAL_PARAMETER_IN_OVERRIDING, enabledDisabled, indent);
		
		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_unused_imports.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_UNUSED_IMPORT, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_unused_private.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_UNUSED_PRIVATE, errorWarningIgnore, errorWarningIgnoreLabels, 0);
		
		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_unnecessary_else.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_UNNECESSARY_ELSE, errorWarningIgnore, errorWarningIgnoreLabels, 0);
		
		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_unnecessary_type_check.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_UNNECESSARY_TYPE_CHECK, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_hidden_catchblock.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_HIDDEN_CATCH_BLOCK, errorWarningIgnore, errorWarningIgnoreLabels, 0);
		
		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_unused_throwing_exception.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION, errorWarningIgnore, errorWarningIgnoreLabels, 0);
		
		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_unused_throwing_exception_when_overriding.label"); //$NON-NLS-1$
		addCheckBox(composite, label, PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING, enabledDisabled, indent);

		return composite;
	}
	
	private Composite createJavadocTabContent(TabFolder folder) {
		String[] errorWarningIgnore= new String[] { ERROR, WARNING, IGNORE };
		
		String[] errorWarningIgnoreLabels= new String[] {
				PreferencesMessages.getString("CompilerConfigurationBlock.error"),  //$NON-NLS-1$
				PreferencesMessages.getString("CompilerConfigurationBlock.warning"), //$NON-NLS-1$
				PreferencesMessages.getString("CompilerConfigurationBlock.ignore") //$NON-NLS-1$
		};
		
		String[] enabledDisabled= new String[] { ENABLED, DISABLED };
		
		String[] visibilities= new String[] { PUBLIC, PROTECTED, DEFAULT, PRIVATE  };
		
		String[] visibilitiesLabels= new String[] {
				PreferencesMessages.getString("CompilerConfigurationBlock.public"), //$NON-NLS-1$
				PreferencesMessages.getString("CompilerConfigurationBlock.protected"), //$NON-NLS-1$
				PreferencesMessages.getString("CompilerConfigurationBlock.default"), //$NON-NLS-1$
				PreferencesMessages.getString("CompilerConfigurationBlock.private") //$NON-NLS-1$
		};
		int nColumns= 3;
				
		GridLayout layout = new GridLayout();
		layout.numColumns= nColumns;
		
		Composite outer= new Composite(folder, SWT.NULL);
		outer.setLayout(layout);
		
		String label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_javadoc_support.label"); //$NON-NLS-1$
		addCheckBox(outer, label, PREF_JAVADOC_SUPPORT, enabledDisabled, 0);
		
		layout = new GridLayout();
		layout.numColumns= nColumns;
		layout.marginHeight= 0;
		//layout.marginWidth= 0;
		
		Composite composite= new Composite(outer, SWT.NULL);
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		fJavadocComposite= composite;
		
		Label description= new Label(composite, SWT.WRAP);
		description.setText(PreferencesMessages.getString("CompilerConfigurationBlock.javadoc.description")); //$NON-NLS-1$
		GridData gd= new GridData();
		gd.horizontalSpan= nColumns;
		//gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(60);
		description.setLayoutData(gd);
			
		int indent= fPixelConverter.convertWidthInCharsToPixels(2);
		
		label = PreferencesMessages.getString("CompilerConfigurationBlock.pb_invalid_javadoc.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_INVALID_JAVADOC, errorWarningIgnore, errorWarningIgnoreLabels, 0);
		
		label = PreferencesMessages.getString("CompilerConfigurationBlock.pb_invalid_javadoc_tags_visibility.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_INVALID_JAVADOC_TAGS_VISIBILITY, visibilities, visibilitiesLabels, indent);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_invalid_javadoc_tags.label"); //$NON-NLS-1$
		addCheckBox(composite, label, PREF_PB_INVALID_JAVADOC_TAGS, enabledDisabled, indent);

		Label separator= new Label(composite, SWT.HORIZONTAL);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= nColumns;
		separator.setLayoutData(gd);
		
		label = PreferencesMessages.getString("CompilerConfigurationBlock.pb_missing_javadoc.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_MISSING_JAVADOC_TAGS, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label = PreferencesMessages.getString("CompilerConfigurationBlock.pb_missing_javadoc_tags_visibility.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_MISSING_JAVADOC_TAGS_VISIBILITY, visibilities, visibilitiesLabels, indent);
		
		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_missing_javadoc_tags_overriding.label"); //$NON-NLS-1$
		addCheckBox(composite, label, PREF_PB_MISSING_JAVADOC_TAGS_OVERRIDING, enabledDisabled, indent);

		separator= new Label(composite, SWT.HORIZONTAL);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= nColumns;
		separator.setLayoutData(gd);
		
		label = PreferencesMessages.getString("CompilerConfigurationBlock.pb_missing_comments.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_MISSING_JAVADOC_COMMENTS, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label = PreferencesMessages.getString("CompilerConfigurationBlock.pb_missing_comments_visibility.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_MISSING_JAVADOC_COMMENTS_VISIBILITY, visibilities, visibilitiesLabels, indent);
		
		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_missing_comments_overriding.label"); //$NON-NLS-1$
		addCheckBox(composite, label, PREF_PB_MISSING_JAVADOC_COMMENTS_OVERRIDING, enabledDisabled, indent);

		return outer;
	}
	


	private Composite createBuildPathTabContent(TabFolder folder) {
		String[] abortIgnoreValues= new String[] { ABORT, IGNORE };
		String[] cleanIgnoreValues= new String[] { CLEAN, IGNORE };
		String[] enableDisableValues= new String[] { ENABLED, DISABLED };
		
		String[] errorWarning= new String[] { ERROR, WARNING };
		
		String[] errorWarningLabels= new String[] {
			PreferencesMessages.getString("CompilerConfigurationBlock.error"),  //$NON-NLS-1$
			PreferencesMessages.getString("CompilerConfigurationBlock.warning") //$NON-NLS-1$
		};
		
		int nColumns= 3;
		
		GridLayout layout= new GridLayout();
		layout.numColumns= nColumns;

		Composite othersComposite= new Composite(folder, SWT.NULL);
		othersComposite.setLayout(layout);
		
		Label description= new Label(othersComposite, SWT.WRAP);
		description.setText(PreferencesMessages.getString("CompilerConfigurationBlock.build_warnings.description")); //$NON-NLS-1$
		GridData gd= new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		gd.horizontalSpan= nColumns;
		description.setLayoutData(gd);
				
		String label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_incomplete_build_path.label"); //$NON-NLS-1$
		addComboBox(othersComposite, label, PREF_PB_INCOMPLETE_BUILDPATH, errorWarning, errorWarningLabels, 0);	
		
		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_build_path_cycles.label"); //$NON-NLS-1$
		addComboBox(othersComposite, label, PREF_PB_CIRCULAR_BUILDPATH, errorWarning, errorWarningLabels, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_duplicate_resources.label"); //$NON-NLS-1$
		addComboBox(othersComposite, label, PREF_PB_DUPLICATE_RESOURCE, errorWarning, errorWarningLabels, 0);

		String[] errorWarningIgnore= new String[] { ERROR, WARNING, IGNORE };
		
		String[] errorWarningIgnoreLabels= new String[] {
			PreferencesMessages.getString("CompilerConfigurationBlock.error"),  //$NON-NLS-1$
			PreferencesMessages.getString("CompilerConfigurationBlock.warning"), //$NON-NLS-1$
			PreferencesMessages.getString("CompilerConfigurationBlock.ignore") //$NON-NLS-1$
		};
		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_check_prereq_binary_level.label");  //$NON-NLS-1$
		addComboBox(othersComposite, label, PREF_PB_INCOMPATIBLE_JDK_LEVEL, errorWarningIgnore, errorWarningIgnoreLabels, 0);	

		label= PreferencesMessages.getString("CompilerConfigurationBlock.build_invalid_classpath.label"); //$NON-NLS-1$
		addCheckBox(othersComposite, label, PREF_BUILD_INVALID_CLASSPATH, abortIgnoreValues, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.build_clean_outputfolder.label"); //$NON-NLS-1$
		addCheckBox(othersComposite, label, PREF_BUILD_CLEAN_OUTPUT_FOLDER, cleanIgnoreValues, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.enable_exclusion_patterns.label"); //$NON-NLS-1$
		addCheckBox(othersComposite, label, PREF_ENABLE_EXCLUSION_PATTERNS, enableDisableValues, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.enable_multiple_outputlocations.label"); //$NON-NLS-1$
		addCheckBox(othersComposite, label, PREF_ENABLE_MULTIPLE_OUTPUT_LOCATIONS, enableDisableValues, 0);
				
		label= PreferencesMessages.getString("CompilerConfigurationBlock.resource_filter.label"); //$NON-NLS-1$
		Text text= addTextField(othersComposite, label, PREF_RESOURCE_FILTER, 0, 0);
		gd= (GridData) text.getLayoutData();
		gd.grabExcessHorizontalSpace= true;
		gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(10);

		description= new Label(othersComposite, SWT.WRAP);
		description.setText(PreferencesMessages.getString("CompilerConfigurationBlock.resource_filter.description")); //$NON-NLS-1$
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= nColumns;
		gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(60);
		description.setLayoutData(gd);

		
		return othersComposite;
	}
	
	private Composite createComplianceTabContent(Composite folder) {
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;

		String[] values34, values34Labels;
		if (JavaModelUtil.isJDTCore_1_5()) {
			values34= new String[] { VERSION_1_3, VERSION_1_4, VERSION_1_5 };
			values34Labels= new String[] {
				PreferencesMessages.getString("CompilerConfigurationBlock.version13"),  //$NON-NLS-1$
				PreferencesMessages.getString("CompilerConfigurationBlock.version14"), //$NON-NLS-1$
				PreferencesMessages.getString("CompilerConfigurationBlock.version15") //$NON-NLS-1$
			};
		} else {
			values34= new String[] { VERSION_1_3, VERSION_1_4 };
			values34Labels= new String[] {
				PreferencesMessages.getString("CompilerConfigurationBlock.version13"),  //$NON-NLS-1$
				PreferencesMessages.getString("CompilerConfigurationBlock.version14") //$NON-NLS-1$
			};
		}

		Composite compComposite= new Composite(folder, SWT.NULL);
		compComposite.setLayout(layout);

		int nColumns= 3;

		layout= new GridLayout();
		layout.numColumns= nColumns;

		Group group= new Group(compComposite, SWT.NONE);
		group.setText(PreferencesMessages.getString("CompilerConfigurationBlock.compliance.group.label")); //$NON-NLS-1$
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setLayout(layout);
	
		String label= PreferencesMessages.getString("CompilerConfigurationBlock.compiler_compliance.label"); //$NON-NLS-1$
		addComboBox(group, label, PREF_COMPLIANCE, values34, values34Labels, 0);	

		label= PreferencesMessages.getString("CompilerConfigurationBlock.default_settings.label"); //$NON-NLS-1$
		addCheckBox(group, label, INTR_DEFAULT_COMPLIANCE, new String[] { DEFAULT_CONF, USER_CONF }, 0);	

		int indent= fPixelConverter.convertWidthInCharsToPixels(2);
		Control[] otherChildren= group.getChildren();	
				
		String[] versions, versionsLabels;
		if (JavaModelUtil.isJDTCore_1_5()) {
			versions= new String[] { VERSION_1_1, VERSION_1_2, VERSION_1_3, VERSION_1_4, VERSION_1_5 };
			versionsLabels= new String[] {
				PreferencesMessages.getString("CompilerConfigurationBlock.version11"),  //$NON-NLS-1$
				PreferencesMessages.getString("CompilerConfigurationBlock.version12"), //$NON-NLS-1$
				PreferencesMessages.getString("CompilerConfigurationBlock.version13"), //$NON-NLS-1$
				PreferencesMessages.getString("CompilerConfigurationBlock.version14"), //$NON-NLS-1$
				PreferencesMessages.getString("CompilerConfigurationBlock.version15") //$NON-NLS-1$
			};
		} else {
			versions= new String[] { VERSION_1_1, VERSION_1_2, VERSION_1_3, VERSION_1_4 };
			versionsLabels= new String[] {
				PreferencesMessages.getString("CompilerConfigurationBlock.version11"),  //$NON-NLS-1$
				PreferencesMessages.getString("CompilerConfigurationBlock.version12"), //$NON-NLS-1$
				PreferencesMessages.getString("CompilerConfigurationBlock.version13"), //$NON-NLS-1$
				PreferencesMessages.getString("CompilerConfigurationBlock.version14") //$NON-NLS-1$
			};
		}
		
		label= PreferencesMessages.getString("CompilerConfigurationBlock.codegen_targetplatform.label"); //$NON-NLS-1$
		addComboBox(group, label, PREF_CODEGEN_TARGET_PLATFORM, versions, versionsLabels, indent);	

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
		layout.numColumns= nColumns;

		group= new Group(compComposite, SWT.NONE);
		group.setText(PreferencesMessages.getString("CompilerConfigurationBlock.classfiles.group.label")); //$NON-NLS-1$
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setLayout(layout);

		String[] generateValues= new String[] { GENERATE, DO_NOT_GENERATE };
		String[] enableDisableValues= new String[] { ENABLED, DISABLED };

		label= PreferencesMessages.getString("CompilerConfigurationBlock.variable_attr.label"); //$NON-NLS-1$
		addCheckBox(group, label, PREF_LOCAL_VARIABLE_ATTR, generateValues, 0);
		
		label= PreferencesMessages.getString("CompilerConfigurationBlock.line_number_attr.label"); //$NON-NLS-1$
		addCheckBox(group, label, PREF_LINE_NUMBER_ATTR, generateValues, 0);		

		label= PreferencesMessages.getString("CompilerConfigurationBlock.source_file_attr.label"); //$NON-NLS-1$
		addCheckBox(group, label, PREF_SOURCE_FILE_ATTR, generateValues, 0);		

		label= PreferencesMessages.getString("CompilerConfigurationBlock.codegen_unused_local.label"); //$NON-NLS-1$
		addCheckBox(group, label, PREF_CODEGEN_UNUSED_LOCAL, new String[] { PRESERVE, OPTIMIZE_OUT }, 0);	

		label= PreferencesMessages.getString("CompilerConfigurationBlock.codegen_inline_jsr_bytecode.label"); //$NON-NLS-1$
		addCheckBox(group, label, PREF_CODEGEN_INLINE_JSR_BYTECODE, enableDisableValues, 0);	
		
		return compComposite;
	}
	
	
	private Composite create15TabContent(TabFolder folder) {
		String[] errorWarningIgnore= new String[] { ERROR, WARNING, IGNORE };
		
		String[] errorWarningIgnoreLabels= new String[] {
			PreferencesMessages.getString("CompilerConfigurationBlock.error"),  //$NON-NLS-1$
			PreferencesMessages.getString("CompilerConfigurationBlock.warning"), //$NON-NLS-1$
			PreferencesMessages.getString("CompilerConfigurationBlock.ignore") //$NON-NLS-1$
		};
			
		int nColumns= 3;
		
		GridLayout layout= new GridLayout();
		layout.numColumns= nColumns;
					
		Composite composite= new Composite(folder, SWT.NULL);
		composite.setLayout(layout);

		Label description= new Label(composite, SWT.WRAP);
		description.setText(PreferencesMessages.getString("CompilerConfigurationBlock.15.description")); //$NON-NLS-1$
		GridData gd= new GridData();
		gd.horizontalSpan= nColumns;
		description.setLayoutData(gd);
		
		String label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_unsafe_type_op.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_15_PB_UNSAVE_TYPE_OPERATION, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= PreferencesMessages.getString("CompilerConfigurationBlock.pb_final_param_bound.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_15_PB_FINAL_PARAM_BOUND, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		return composite;
	}
	
	
		
	
	/* (non-javadoc)
	 * Update fields and validate.
	 * @param changedKey Key that changed, or null, if all changed.
	 */	
	protected void validateSettings(String changedKey, String newValue) {
		
		if (changedKey != null) {
			if (INTR_DEFAULT_COMPLIANCE.equals(changedKey)) {
				updateComplianceEnableState();
				updateComplianceDefaultSettings(true);
				fComplianceStatus= validateCompliance();
			} else if (PREF_COMPLIANCE.equals(changedKey)) {
				if (checkValue(INTR_DEFAULT_COMPLIANCE, DEFAULT_CONF)) {
					updateComplianceDefaultSettings(false);
				}
				fComplianceStatus= validateCompliance();
			} else if (PREF_SOURCE_COMPATIBILITY.equals(changedKey)) {
				updateAssertAsIdentifierEnableState();
				fComplianceStatus= validateCompliance();
			} else if (PREF_CODEGEN_TARGET_PLATFORM.equals(changedKey) ||
					PREF_PB_ASSERT_AS_IDENTIFIER.equals(changedKey)) {
				fComplianceStatus= validateCompliance();
				updateInlineJSREnableState();
			} else if (PREF_PB_MAX_PER_UNIT.equals(changedKey)) {
				fMaxNumberProblemsStatus= validateMaxNumberProblems();
			} else if (PREF_RESOURCE_FILTER.equals(changedKey)) {
				fResourceFilterStatus= validateResourceFilters();
			} else if (PREF_PB_UNUSED_PARAMETER.equals(changedKey) ||
					PREF_PB_DEPRECATION.equals(changedKey) ||
					PREF_PB_INVALID_JAVADOC.equals(changedKey) ||
					PREF_PB_MISSING_JAVADOC_TAGS.equals(changedKey) ||
					PREF_PB_MISSING_JAVADOC_COMMENTS.equals(changedKey) ||
					PREF_PB_LOCAL_VARIABLE_HIDING.equals(changedKey) ||
					PREF_JAVADOC_SUPPORT.equals(changedKey) ||
					PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION.equals(changedKey)) {				
				updateEnableStates();
			} else if (PREF_PB_SIGNAL_PARAMETER_IN_OVERRIDING.equals(changedKey)) {
				// merging the two options
				fWorkingValues.put(PREF_PB_SIGNAL_PARAMETER_IN_ABSTRACT, newValue);
			} else {
				return;
			}
		} else {
			updateEnableStates();
			updateComplianceEnableState();
			updateAssertAsIdentifierEnableState();
			updateInlineJSREnableState();
			fComplianceStatus= validateCompliance();
			fMaxNumberProblemsStatus= validateMaxNumberProblems();
			fResourceFilterStatus= validateResourceFilters();
		}		
		IStatus status= StatusUtil.getMostSevere(new IStatus[] { fComplianceStatus, fMaxNumberProblemsStatus, fResourceFilterStatus });
		fContext.statusChanged(status);
	}
	
	private void updateEnableStates() {
		boolean enableUnusedParams= !checkValue(PREF_PB_UNUSED_PARAMETER, IGNORE);
		getCheckBox(PREF_PB_SIGNAL_PARAMETER_IN_OVERRIDING).setEnabled(enableUnusedParams);
		
		boolean enableDeprecation= !checkValue(PREF_PB_DEPRECATION, IGNORE);
		getCheckBox(PREF_PB_DEPRECATION_IN_DEPRECATED_CODE).setEnabled(enableDeprecation);
		getCheckBox(PREF_PB_DEPRECATION_WHEN_OVERRIDING).setEnabled(enableDeprecation);
		
		boolean enableThrownExceptions= !checkValue(PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION, IGNORE);
		getCheckBox(PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING).setEnabled(enableThrownExceptions);

		boolean enableHiding= !checkValue(PREF_PB_LOCAL_VARIABLE_HIDING, IGNORE);
		getCheckBox(PREF_PB_SPECIAL_PARAMETER_HIDING_FIELD).setEnabled(enableHiding);
		
		boolean enableJavadoc= checkValue(PREF_JAVADOC_SUPPORT, ENABLED);
		fJavadocComposite.setVisible(enableJavadoc);

		boolean enableInvalidTagsErrors= !checkValue(PREF_PB_INVALID_JAVADOC, IGNORE);
		getCheckBox(PREF_PB_INVALID_JAVADOC_TAGS).setEnabled(enableInvalidTagsErrors);
		setComboEnabled(PREF_PB_INVALID_JAVADOC_TAGS_VISIBILITY, enableInvalidTagsErrors);
		
		boolean enableMissingTagsErrors= !checkValue(PREF_PB_MISSING_JAVADOC_TAGS, IGNORE);
		getCheckBox(PREF_PB_MISSING_JAVADOC_TAGS_OVERRIDING).setEnabled(enableMissingTagsErrors);
		setComboEnabled(PREF_PB_MISSING_JAVADOC_TAGS_VISIBILITY, enableMissingTagsErrors);
		
		boolean enableMissingCommentsErrors= !checkValue(PREF_PB_MISSING_JAVADOC_COMMENTS, IGNORE);
		getCheckBox(PREF_PB_MISSING_JAVADOC_COMMENTS_OVERRIDING).setEnabled(enableMissingCommentsErrors);
		setComboEnabled(PREF_PB_MISSING_JAVADOC_COMMENTS_VISIBILITY, enableMissingCommentsErrors);
	}

	private IStatus validateCompliance() {
		StatusInfo status= new StatusInfo();
		String compliance= (String) fWorkingValues.get(PREF_COMPLIANCE);
		String source= (String) fWorkingValues.get(PREF_SOURCE_COMPATIBILITY);
		String target= (String) fWorkingValues.get(PREF_CODEGEN_TARGET_PLATFORM);
		
		if (VERSION_1_3.equals(compliance)) {
			if (VERSION_1_4.equals(source) || VERSION_1_5.equals(source)) {
				status.setError(PreferencesMessages.getString("CompilerConfigurationBlock.cpl13src145.error")); //$NON-NLS-1$
				return status;
			} 
			if (VERSION_1_4.equals(target) || VERSION_1_5.equals(target)) {
				status.setError(PreferencesMessages.getString("CompilerConfigurationBlock.cpl13trg145.error")); //$NON-NLS-1$
				return status;
			}
		} else if (VERSION_1_4.equals(compliance)) {
			if (VERSION_1_5.equals(source)) {
				status.setError(PreferencesMessages.getString("CompilerConfigurationBlock.cpl14src15.error")); //$NON-NLS-1$
				return status;
			} 
			if (VERSION_1_5.equals(target)) {
				status.setError(PreferencesMessages.getString("CompilerConfigurationBlock.cpl14trg15.error")); //$NON-NLS-1$
				return status;
			}			
		}
		if (VERSION_1_4.equals(source)) {
			if (!VERSION_1_4.equals(target)) {
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
		boolean enabled= checkValue(INTR_DEFAULT_COMPLIANCE, USER_CONF);
		for (int i= fComplianceControls.size() - 1; i >= 0; i--) {
			Control curr= (Control) fComplianceControls.get(i);
			curr.setEnabled(enabled);
		}
	}
	
	private void updateAssertAsIdentifierEnableState() {
		if (checkValue(INTR_DEFAULT_COMPLIANCE, USER_CONF)) {
			boolean enabled= checkValue(PREF_SOURCE_COMPATIBILITY, VERSION_1_3);
			Combo combo= getComboBox(PREF_PB_ASSERT_AS_IDENTIFIER);
			combo.setEnabled(enabled);
			
			if (!enabled) {
				String val= (String) fWorkingValues.get(PREF_PB_ASSERT_AS_IDENTIFIER);
				if (!ERROR.equals(val)) {
					fWorkingValues.put(PREF_PB_ASSERT_AS_IDENTIFIER, ERROR);
					updateCombo(combo);
					fRememberedUserCompliance[IDX_ASSERT_AS_IDENTIFIER]= val;
				}
			} else {
				String val= fRememberedUserCompliance[IDX_ASSERT_AS_IDENTIFIER];
				if (!ERROR.equals(val)) {
					fWorkingValues.put(PREF_PB_ASSERT_AS_IDENTIFIER, val);
					updateCombo(combo);
				}
			}
		}
	}
	
	private void updateInlineJSREnableState() {
		boolean enabled= !checkValue(PREF_CODEGEN_TARGET_PLATFORM, VERSION_1_5);
		Button checkBox= getCheckBox(PREF_CODEGEN_INLINE_JSR_BYTECODE);
		checkBox.setEnabled(enabled);
		
		if (!enabled) {
			String val= (String) fWorkingValues.get(PREF_CODEGEN_INLINE_JSR_BYTECODE);
			fRememberedUserCompliance[IDX_INLINE_JSR_BYTECODE]= val;
			
			if (!ENABLED.equals(val)) {
				fWorkingValues.put(PREF_CODEGEN_INLINE_JSR_BYTECODE, ENABLED);
				updateCheckBox(checkBox);
			}
		} else {
			String val= fRememberedUserCompliance[IDX_INLINE_JSR_BYTECODE];
			if (!ENABLED.equals(val)) {
				fWorkingValues.put(PREF_CODEGEN_INLINE_JSR_BYTECODE, val);
				updateCheckBox(checkBox);
			}
		}
	}

	/*
	 * Set the default compliance values derived from the chosen level
	 */	
	private void updateComplianceDefaultSettings(boolean rememberOld) {
		String assertAsId, source, target;
		String complianceLevel= (String) fWorkingValues.get(PREF_COMPLIANCE);
		boolean isDefault= checkValue(INTR_DEFAULT_COMPLIANCE, DEFAULT_CONF);
		
		if (isDefault) {
			if (rememberOld) {
				fRememberedUserCompliance[IDX_ASSERT_AS_IDENTIFIER]= (String) fWorkingValues.get(PREF_PB_ASSERT_AS_IDENTIFIER);
				fRememberedUserCompliance[IDX_SOURCE_COMPATIBILITY]= (String) fWorkingValues.get(PREF_SOURCE_COMPATIBILITY);
				fRememberedUserCompliance[IDX_CODEGEN_TARGET_PLATFORM]= (String) fWorkingValues.get(PREF_CODEGEN_TARGET_PLATFORM);
				fRememberedUserCompliance[IDX_COMPLIANCE]= complianceLevel;
			}

			if (VERSION_1_4.equals(complianceLevel)) {
				assertAsId= WARNING;
				source= VERSION_1_3;
				target= VERSION_1_2;
			} else if (VERSION_1_5.equals(complianceLevel)) {
				assertAsId= ERROR;
				source= VERSION_1_5;
				target= VERSION_1_5;
			} else {
				assertAsId= IGNORE;
				source= VERSION_1_3;
				target= VERSION_1_1;
			}
		} else {
			if (rememberOld && complianceLevel.equals(fRememberedUserCompliance[IDX_COMPLIANCE])) {
				assertAsId= fRememberedUserCompliance[IDX_ASSERT_AS_IDENTIFIER];
				source= fRememberedUserCompliance[IDX_SOURCE_COMPATIBILITY];
				target= fRememberedUserCompliance[IDX_CODEGEN_TARGET_PLATFORM];
			} else {
				return;
			}
		}
		fWorkingValues.put(PREF_PB_ASSERT_AS_IDENTIFIER, assertAsId);
		fWorkingValues.put(PREF_SOURCE_COMPATIBILITY, source);
		fWorkingValues.put(PREF_CODEGEN_TARGET_PLATFORM, target);
		updateControls();
		updateInlineJSREnableState();
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
				&& WARNING.equals(map.get(PREF_PB_ASSERT_AS_IDENTIFIER))
				&& VERSION_1_3.equals(map.get(PREF_SOURCE_COMPATIBILITY))
				&& VERSION_1_2.equals(map.get(PREF_CODEGEN_TARGET_PLATFORM)))
			|| (VERSION_1_5.equals(complianceLevel)
				&& ERROR.equals(map.get(PREF_PB_ASSERT_AS_IDENTIFIER))
				&& VERSION_1_5.equals(map.get(PREF_SOURCE_COMPATIBILITY))
				&& VERSION_1_5.equals(map.get(PREF_CODEGEN_TARGET_PLATFORM)))) {
			return DEFAULT_CONF;
		}
		return USER_CONF;
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
