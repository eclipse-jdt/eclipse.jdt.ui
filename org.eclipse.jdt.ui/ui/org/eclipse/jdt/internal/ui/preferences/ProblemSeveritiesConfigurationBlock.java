/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Muskalla <b.muskalla@gmx.net> - [preferences] Add preference for new compiler warning: MissingSynchronizedModifierInInheritedMethod - https://bugs.eclipse.org/bugs/show_bug.cgi?id=245240
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.core.resources.IProject;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.PixelConverter;

import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;


public class ProblemSeveritiesConfigurationBlock extends OptionsConfigurationBlock {

	private static final String SETTINGS_SECTION_NAME= "ProblemSeveritiesConfigurationBlock";  //$NON-NLS-1$

	// Preference store keys, see JavaCore.getOptions
	private static final Key PREF_PB_OVERRIDING_PACKAGE_DEFAULT_METHOD= getJDTCoreKey(JavaCore.COMPILER_PB_OVERRIDING_PACKAGE_DEFAULT_METHOD);
	private static final Key PREF_PB_METHOD_WITH_CONSTRUCTOR_NAME= getJDTCoreKey(JavaCore.COMPILER_PB_METHOD_WITH_CONSTRUCTOR_NAME);
	private static final Key PREF_PB_DEPRECATION= getJDTCoreKey(JavaCore.COMPILER_PB_DEPRECATION);
	private static final Key PREF_PB_DEPRECATION_IN_DEPRECATED_CODE=getJDTCoreKey(JavaCore.COMPILER_PB_DEPRECATION_IN_DEPRECATED_CODE);
	private static final Key PREF_PB_DEPRECATION_WHEN_OVERRIDING= getJDTCoreKey(JavaCore.COMPILER_PB_DEPRECATION_WHEN_OVERRIDING_DEPRECATED_METHOD);

	private static final Key PREF_PB_HIDDEN_CATCH_BLOCK= getJDTCoreKey(JavaCore.COMPILER_PB_HIDDEN_CATCH_BLOCK);
	private static final Key PREF_PB_UNUSED_LOCAL= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_LOCAL);
	private static final Key PREF_PB_UNUSED_PARAMETER= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_PARAMETER);
	private static final Key PREF_PB_SIGNAL_PARAMETER_IN_OVERRIDING= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_PARAMETER_WHEN_OVERRIDING_CONCRETE);
	private static final Key PREF_PB_UNUSED_PARAMETER_INCLUDE_DOC_COMMENT_REFERENCE= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_PARAMETER_INCLUDE_DOC_COMMENT_REFERENCE);
	private static final Key PREF_PB_SIGNAL_PARAMETER_IN_ABSTRACT= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_PARAMETER_WHEN_IMPLEMENTING_ABSTRACT);
	private static final Key PREF_PB_SYNTHETIC_ACCESS_EMULATION= getJDTCoreKey(JavaCore.COMPILER_PB_SYNTHETIC_ACCESS_EMULATION);
	private static final Key PREF_PB_NON_EXTERNALIZED_STRINGS= getJDTCoreKey(JavaCore.COMPILER_PB_NON_NLS_STRING_LITERAL);
	private static final Key PREF_PB_UNUSED_IMPORT= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_IMPORT);
	private static final Key PREF_PB_UNUSED_PRIVATE= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER);
	private static final Key PREF_PB_STATIC_ACCESS_RECEIVER= getJDTCoreKey(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER);
	private static final Key PREF_PB_NO_EFFECT_ASSIGNMENT= getJDTCoreKey(JavaCore.COMPILER_PB_NO_EFFECT_ASSIGNMENT);
	private static final Key PREF_PB_CHAR_ARRAY_IN_CONCAT= getJDTCoreKey(JavaCore.COMPILER_PB_CHAR_ARRAY_IN_STRING_CONCATENATION);
	private static final Key PREF_PB_POSSIBLE_ACCIDENTAL_BOOLEAN_ASSIGNMENT= getJDTCoreKey(JavaCore.COMPILER_PB_POSSIBLE_ACCIDENTAL_BOOLEAN_ASSIGNMENT);
	private static final Key PREF_PB_LOCAL_VARIABLE_HIDING= getJDTCoreKey(JavaCore.COMPILER_PB_LOCAL_VARIABLE_HIDING);
	private static final Key PREF_PB_FIELD_HIDING= getJDTCoreKey(JavaCore.COMPILER_PB_FIELD_HIDING);
	private static final Key PREF_PB_SPECIAL_PARAMETER_HIDING_FIELD= getJDTCoreKey(JavaCore.COMPILER_PB_SPECIAL_PARAMETER_HIDING_FIELD);
	private static final Key PREF_PB_INDIRECT_STATIC_ACCESS= getJDTCoreKey(JavaCore.COMPILER_PB_INDIRECT_STATIC_ACCESS);
	private static final Key PREF_PB_EMPTY_STATEMENT= getJDTCoreKey(JavaCore.COMPILER_PB_EMPTY_STATEMENT);
	private static final Key PREF_PB_UNNECESSARY_ELSE= getJDTCoreKey(JavaCore.COMPILER_PB_UNNECESSARY_ELSE);
	private static final Key PREF_PB_UNNECESSARY_TYPE_CHECK= getJDTCoreKey(JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK);
	private static final Key PREF_PB_INCOMPATIBLE_INTERFACE_METHOD= getJDTCoreKey(JavaCore.COMPILER_PB_INCOMPATIBLE_NON_INHERITED_INTERFACE_METHOD);
	private static final Key PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION);
	private static final Key PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING);
	private static final Key PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_INCLUDE_DOC_COMMENT_REFERENCE= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_INCLUDE_DOC_COMMENT_REFERENCE);
	private static final Key PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_EXEMPT_EXCEPTION_AND_THROWABLE= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_EXEMPT_EXCEPTION_AND_THROWABLE);
	private static final Key PREF_PB_MISSING_SERIAL_VERSION= getJDTCoreKey(JavaCore.COMPILER_PB_MISSING_SERIAL_VERSION);
	private static final Key PREF_PB_UNDOCUMENTED_EMPTY_BLOCK= getJDTCoreKey(JavaCore.COMPILER_PB_UNDOCUMENTED_EMPTY_BLOCK);
	private static final Key PREF_PB_FINALLY_BLOCK_NOT_COMPLETING= getJDTCoreKey(JavaCore.COMPILER_PB_FINALLY_BLOCK_NOT_COMPLETING);
	private static final Key PREF_PB_UNQUALIFIED_FIELD_ACCESS= getJDTCoreKey(JavaCore.COMPILER_PB_UNQUALIFIED_FIELD_ACCESS);
	private static final Key PREF_PB_MISSING_DEPRECATED_ANNOTATION= getJDTCoreKey(JavaCore.COMPILER_PB_MISSING_DEPRECATED_ANNOTATION);
	private static final Key PREF_PB_FORBIDDEN_REFERENCE= getJDTCoreKey(JavaCore.COMPILER_PB_FORBIDDEN_REFERENCE);
	private static final Key PREF_PB_DISCOURRAGED_REFERENCE= getJDTCoreKey(JavaCore.COMPILER_PB_DISCOURAGED_REFERENCE);
	private static final Key PREF_PB_UNUSED_LABEL= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_LABEL);
	private static final Key PREF_PB_PARAMETER_ASSIGNMENT= getJDTCoreKey(JavaCore.COMPILER_PB_PARAMETER_ASSIGNMENT);
	private static final Key PREF_PB_FALLTHROUGH_CASE= getJDTCoreKey(JavaCore.COMPILER_PB_FALLTHROUGH_CASE);
	private static final Key PREF_PB_COMPARING_IDENTICAL= getJDTCoreKey(JavaCore.COMPILER_PB_COMPARING_IDENTICAL);
	private static final Key PREF_PB_MISSING_SYNCHRONIZED_ON_INHERITED_METHOD= getJDTCoreKey(JavaCore.COMPILER_PB_MISSING_SYNCHRONIZED_ON_INHERITED_METHOD);

	private static final Key PREF_PB_NULL_REFERENCE= getJDTCoreKey(JavaCore.COMPILER_PB_NULL_REFERENCE);
	private static final Key PREF_PB_POTENTIAL_NULL_REFERENCE= getJDTCoreKey(JavaCore.COMPILER_PB_POTENTIAL_NULL_REFERENCE);
	private static final Key PREF_PB_REDUNDANT_NULL_CHECK= getJDTCoreKey(JavaCore.COMPILER_PB_REDUNDANT_NULL_CHECK);

	private static final Key PREF_PB_INCLUDE_ASSERTS_IN_NULL_ANALYSIS= getJDTCoreKey(JavaCore.COMPILER_PB_INCLUDE_ASSERTS_IN_NULL_ANALYSIS);
	private static final Key PREF_PB_REDUNDANT_SUPERINTERFACE= getJDTCoreKey(JavaCore.COMPILER_PB_REDUNDANT_SUPERINTERFACE);

	private static final Key PREF_PB_UNUSED_WARNING_TOKEN= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_WARNING_TOKEN);

	private static final Key PREF_15_PB_UNCHECKED_TYPE_OPERATION= getJDTCoreKey(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION);
	private static final Key PREF_15_PB_FINAL_PARAM_BOUND= getJDTCoreKey(JavaCore.COMPILER_PB_FINAL_PARAMETER_BOUND);
	private static final Key PREF_15_PB_VARARGS_ARGUMENT_NEED_CAST= getJDTCoreKey(JavaCore.COMPILER_PB_VARARGS_ARGUMENT_NEED_CAST);
	private static final Key PREF_15_PB_AUTOBOXING_PROBLEM= getJDTCoreKey(JavaCore.COMPILER_PB_AUTOBOXING);

	private static final Key PREF_15_PB_MISSING_OVERRIDE_ANNOTATION= getJDTCoreKey(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION);
	private static final Key PREF_16_PB_MISSING_OVERRIDE_ANNOTATION_FOR_INTERFACE_METHOD_IMPLEMENTATION= getJDTCoreKey(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION_FOR_INTERFACE_METHOD_IMPLEMENTATION);
	private static final Key PREF_15_PB_ANNOTATION_SUPER_INTERFACE= getJDTCoreKey(JavaCore.COMPILER_PB_ANNOTATION_SUPER_INTERFACE);
	private static final Key PREF_15_PB_TYPE_PARAMETER_HIDING= getJDTCoreKey(JavaCore.COMPILER_PB_TYPE_PARAMETER_HIDING);
	private static final Key PREF_15_PB_INCOMPLETE_ENUM_SWITCH= getJDTCoreKey(JavaCore.COMPILER_PB_INCOMPLETE_ENUM_SWITCH);
	private static final Key PREF_15_PB_RAW_TYPE_REFERENCE= getJDTCoreKey(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE);
	private static final Key PREF_17_PB_REDUNDANT_TYPE_ARGUMENTS= getJDTCoreKey(JavaCore.COMPILER_PB_REDUNDANT_TYPE_ARGUMENTS);
	private static final Key PREF_15_PB_UNAVOIDABLE_GENERIC_TYPE_PROBLEMS= getJDTCoreKey(JavaCore.COMPILER_PB_UNAVOIDABLE_GENERIC_TYPE_PROBLEMS);

	private static final Key PREF_PB_SUPPRESS_WARNINGS= getJDTCoreKey(JavaCore.COMPILER_PB_SUPPRESS_WARNINGS);
	private static final Key PREF_PB_SUPPRESS_OPTIONAL_ERRORS= getJDTCoreKey(JavaCore.COMPILER_PB_SUPPRESS_OPTIONAL_ERRORS);
	private static final Key PREF_PB_UNHANDLED_WARNING_TOKEN= getJDTCoreKey(JavaCore.COMPILER_PB_UNHANDLED_WARNING_TOKEN);
	private static final Key PREF_PB_FATAL_OPTIONAL_ERROR= getJDTCoreKey(JavaCore.COMPILER_PB_FATAL_OPTIONAL_ERROR);

	private static final Key PREF_PB_MISSING_HASHCODE_METHOD= getJDTCoreKey(JavaCore.COMPILER_PB_MISSING_HASHCODE_METHOD);
	private static final Key PREF_PB_DEAD_CODE= getJDTCoreKey(JavaCore.COMPILER_PB_DEAD_CODE);
	private static final Key PREF_PB_UNUSED_OBJECT_ALLOCATION= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_OBJECT_ALLOCATION);
	private static final Key PREF_PB_MISSING_STATIC_ON_METHOD= getJDTCoreKey(JavaCore.COMPILER_PB_MISSING_STATIC_ON_METHOD);
	private static final Key PREF_PB_POTENTIALLY_MISSING_STATIC_ON_METHOD= getJDTCoreKey(JavaCore.COMPILER_PB_POTENTIALLY_MISSING_STATIC_ON_METHOD);

	// values
	private static final String ERROR= JavaCore.ERROR;
	private static final String WARNING= JavaCore.WARNING;
	private static final String IGNORE= JavaCore.IGNORE;

	private static final String ENABLED= JavaCore.ENABLED;
	private static final String DISABLED= JavaCore.DISABLED;

	private PixelConverter fPixelConverter;

	private FilteredPreferenceTree fFilteredPrefTree;

	public ProblemSeveritiesConfigurationBlock(IStatusChangeListener context, IProject project, IWorkbenchPreferenceContainer container) {
		super(context, project, getKeys(), container);

		// Compatibility code for the merge of the two option PB_SIGNAL_PARAMETER:
		if (ENABLED.equals(getValue(PREF_PB_SIGNAL_PARAMETER_IN_ABSTRACT))) {
			setValue(PREF_PB_SIGNAL_PARAMETER_IN_OVERRIDING, ENABLED);
		}
	}

	public static Key[] getKeys() {
		return new Key[] {
				PREF_PB_OVERRIDING_PACKAGE_DEFAULT_METHOD,
				PREF_PB_METHOD_WITH_CONSTRUCTOR_NAME, PREF_PB_DEPRECATION, PREF_PB_HIDDEN_CATCH_BLOCK, PREF_PB_UNUSED_LOCAL,
				PREF_PB_UNUSED_PARAMETER, PREF_PB_UNUSED_PARAMETER_INCLUDE_DOC_COMMENT_REFERENCE,
				PREF_PB_SYNTHETIC_ACCESS_EMULATION, PREF_PB_NON_EXTERNALIZED_STRINGS,
				PREF_PB_UNUSED_IMPORT, PREF_PB_UNUSED_LABEL,
				PREF_PB_STATIC_ACCESS_RECEIVER, PREF_PB_DEPRECATION_IN_DEPRECATED_CODE,
				PREF_PB_NO_EFFECT_ASSIGNMENT, PREF_PB_INCOMPATIBLE_INTERFACE_METHOD,
				PREF_PB_UNUSED_PRIVATE, PREF_PB_CHAR_ARRAY_IN_CONCAT, PREF_PB_UNNECESSARY_ELSE,
				PREF_PB_POSSIBLE_ACCIDENTAL_BOOLEAN_ASSIGNMENT, PREF_PB_LOCAL_VARIABLE_HIDING, PREF_PB_FIELD_HIDING,
				PREF_PB_SPECIAL_PARAMETER_HIDING_FIELD, PREF_PB_INDIRECT_STATIC_ACCESS,
				PREF_PB_EMPTY_STATEMENT, PREF_PB_SIGNAL_PARAMETER_IN_OVERRIDING, PREF_PB_SIGNAL_PARAMETER_IN_ABSTRACT,
				PREF_PB_UNNECESSARY_TYPE_CHECK, PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION, PREF_PB_UNQUALIFIED_FIELD_ACCESS,
				PREF_PB_UNDOCUMENTED_EMPTY_BLOCK, PREF_PB_FINALLY_BLOCK_NOT_COMPLETING, PREF_PB_DEPRECATION_WHEN_OVERRIDING,
				PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING, PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_INCLUDE_DOC_COMMENT_REFERENCE,
				PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_EXEMPT_EXCEPTION_AND_THROWABLE,
				PREF_PB_MISSING_SERIAL_VERSION, PREF_PB_PARAMETER_ASSIGNMENT, PREF_PB_NULL_REFERENCE, PREF_PB_POTENTIAL_NULL_REFERENCE,
				PREF_PB_REDUNDANT_NULL_CHECK, PREF_PB_INCLUDE_ASSERTS_IN_NULL_ANALYSIS,
				PREF_PB_FALLTHROUGH_CASE, PREF_PB_REDUNDANT_SUPERINTERFACE, PREF_PB_UNUSED_WARNING_TOKEN,
				PREF_15_PB_UNCHECKED_TYPE_OPERATION, PREF_15_PB_FINAL_PARAM_BOUND, PREF_15_PB_VARARGS_ARGUMENT_NEED_CAST,
				PREF_15_PB_AUTOBOXING_PROBLEM, PREF_15_PB_MISSING_OVERRIDE_ANNOTATION, PREF_16_PB_MISSING_OVERRIDE_ANNOTATION_FOR_INTERFACE_METHOD_IMPLEMENTATION,
				PREF_15_PB_ANNOTATION_SUPER_INTERFACE,
				PREF_15_PB_TYPE_PARAMETER_HIDING, PREF_15_PB_INCOMPLETE_ENUM_SWITCH, PREF_PB_MISSING_DEPRECATED_ANNOTATION,
				PREF_15_PB_RAW_TYPE_REFERENCE, PREF_15_PB_UNAVOIDABLE_GENERIC_TYPE_PROBLEMS, PREF_17_PB_REDUNDANT_TYPE_ARGUMENTS,
				PREF_PB_FATAL_OPTIONAL_ERROR,
				PREF_PB_FORBIDDEN_REFERENCE, PREF_PB_DISCOURRAGED_REFERENCE,
				PREF_PB_SUPPRESS_WARNINGS, PREF_PB_SUPPRESS_OPTIONAL_ERRORS,
				PREF_PB_UNHANDLED_WARNING_TOKEN,
				PREF_PB_COMPARING_IDENTICAL, PREF_PB_MISSING_SYNCHRONIZED_ON_INHERITED_METHOD, PREF_PB_MISSING_HASHCODE_METHOD,
				PREF_PB_DEAD_CODE, PREF_PB_UNUSED_OBJECT_ALLOCATION,
				PREF_PB_MISSING_STATIC_ON_METHOD, PREF_PB_POTENTIALLY_MISSING_STATIC_ON_METHOD
			};
	}

	/*
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		fPixelConverter= new PixelConverter(parent);
		setShell(parent.getShell());

		Composite mainComp= new Composite(parent, SWT.NONE);
		mainComp.setFont(parent.getFont());
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		mainComp.setLayout(layout);

		Composite commonComposite= createStyleTabContent(mainComp);
		GridData gridData= new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint= fPixelConverter.convertHeightInCharsToPixels(20);
		commonComposite.setLayoutData(gridData);

		validateSettings(null, null, null);

		return mainComp;
	}

	private Composite createStyleTabContent(Composite folder) {
		String[] errorWarningIgnore= new String[] { ERROR, WARNING, IGNORE };

		String[] errorWarningIgnoreLabels= new String[] {
			PreferencesMessages.ProblemSeveritiesConfigurationBlock_error,
			PreferencesMessages.ProblemSeveritiesConfigurationBlock_warning,
			PreferencesMessages.ProblemSeveritiesConfigurationBlock_ignore
		};

		String[] enabledDisabled= new String[] { ENABLED, DISABLED };
		String[] disabledEnabled= new String[] { DISABLED, ENABLED };

		fFilteredPrefTree= new FilteredPreferenceTree(this, folder, PreferencesMessages.ProblemSeveritiesConfigurationBlock_common_description);
		final ScrolledPageContent sc1= fFilteredPrefTree.getScrolledPageContent();
		
		int nColumns= 3;

		Composite composite= sc1.getBody();
		GridLayout layout= new GridLayout(nColumns, false);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		composite.setLayout(layout);

		int indentStep=  fPixelConverter.convertWidthInCharsToPixels(1);

		int defaultIndent= indentStep * 0;
		int extraIndent= indentStep * 3;
		String label;
		ExpandableComposite excomposite;
		Composite inner;
		PreferenceTreeNode section;
		PreferenceTreeNode node;
		Key twistieKey;

		// --- style

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_section_code_style;
		twistieKey= OptionsConfigurationBlock.getLocalKey("ProblemSeveritiesConfigurationBlock_section_code_style"); //$NON-NLS-1$
		section= fFilteredPrefTree.addExpandableComposite(composite, label, nColumns, twistieKey, null, false);
		excomposite= getExpandableComposite(twistieKey);

		inner= createInnerComposite(excomposite, nColumns, composite.getFont());

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_static_access_receiver_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_STATIC_ACCESS_RECEIVER, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_indirect_access_to_static_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_INDIRECT_STATIC_ACCESS, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unqualified_field_access_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNQUALIFIED_FIELD_ACCESS, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_undocumented_empty_block_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNDOCUMENTED_EMPTY_BLOCK, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_synth_access_emul_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_SYNTHETIC_ACCESS_EMULATION, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_method_naming_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_METHOD_WITH_CONSTRUCTOR_NAME, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_parameter_assignment;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_PARAMETER_ASSIGNMENT, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_non_externalized_strings_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_NON_EXTERNALIZED_STRINGS, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_missing_static_on_method_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_MISSING_STATIC_ON_METHOD, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);
		
		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_potentially_missing_static_on_method_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_POTENTIALLY_MISSING_STATIC_ON_METHOD, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);
		
		// --- potential_programming_problems

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_section_potential_programming_problems;
		twistieKey= OptionsConfigurationBlock.getLocalKey("ProblemSeveritiesConfigurationBlock_section_potential_programming_problems"); //$NON-NLS-1$
		section= fFilteredPrefTree.addExpandableComposite(composite, label, nColumns, twistieKey, null, false);
		excomposite= getExpandableComposite(twistieKey);

		inner= createInnerComposite(excomposite, nColumns, composite.getFont());

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_missing_serial_version_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_MISSING_SERIAL_VERSION, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_no_effect_assignment_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_NO_EFFECT_ASSIGNMENT, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_accidential_assignement_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_POSSIBLE_ACCIDENTAL_BOOLEAN_ASSIGNMENT, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_finally_block_not_completing_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_FINALLY_BLOCK_NOT_COMPLETING, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_empty_statement_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_EMPTY_STATEMENT, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_char_array_in_concat_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_CHAR_ARRAY_IN_CONCAT, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_hidden_catchblock_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_HIDDEN_CATCH_BLOCK, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_inexact_vararg_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_15_PB_VARARGS_ARGUMENT_NEED_CAST, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_autoboxing_problem_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_15_PB_AUTOBOXING_PROBLEM, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_incomplete_enum_switch_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_15_PB_INCOMPLETE_ENUM_SWITCH, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_fall_through_case;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_FALLTHROUGH_CASE, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_null_reference;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_NULL_REFERENCE, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_potential_null_reference;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_POTENTIAL_NULL_REFERENCE, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_comparing_identical;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_COMPARING_IDENTICAL, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_missing_synchronized_on_inherited_method;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_MISSING_SYNCHRONIZED_ON_INHERITED_METHOD, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_missing_hashcode_method;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_MISSING_HASHCODE_METHOD, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_dead_code;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_DEAD_CODE, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);
		
		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_object_allocation_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNUSED_OBJECT_ALLOCATION, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		// --- name_shadowing

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_section_name_shadowing;
		twistieKey= OptionsConfigurationBlock.getLocalKey("ProblemSeveritiesConfigurationBlock_section_name_shadowing"); //$NON-NLS-1$
		section= fFilteredPrefTree.addExpandableComposite(composite, label, nColumns, twistieKey, null, false);
		excomposite= getExpandableComposite(twistieKey);

		inner= createInnerComposite(excomposite, nColumns, composite.getFont());

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_field_hiding_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_FIELD_HIDING, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_local_variable_hiding_label;
		node= fFilteredPrefTree.addComboBox(inner, label, PREF_PB_LOCAL_VARIABLE_HIDING, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_special_param_hiding_label;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_PB_SPECIAL_PARAMETER_HIDING_FIELD, enabledDisabled, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_type_parameter_hiding_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_15_PB_TYPE_PARAMETER_HIDING, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_overriding_pkg_dflt_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_OVERRIDING_PACKAGE_DEFAULT_METHOD, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_incompatible_interface_method_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_INCOMPATIBLE_INTERFACE_METHOD, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		// --- API access rules

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_section_deprecations;
		twistieKey= OptionsConfigurationBlock.getLocalKey("ProblemSeveritiesConfigurationBlock_section_deprecations"); //$NON-NLS-1$
		section= fFilteredPrefTree.addExpandableComposite(composite, label, nColumns, twistieKey, null, false);
		excomposite= getExpandableComposite(twistieKey);

		inner= createInnerComposite(excomposite, nColumns, composite.getFont());

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_deprecation_label;
		node= fFilteredPrefTree.addComboBox(inner, label, PREF_PB_DEPRECATION, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_deprecation_in_deprecation_label;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_PB_DEPRECATION_IN_DEPRECATED_CODE, enabledDisabled, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_deprecation_when_overriding_label;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_PB_DEPRECATION_WHEN_OVERRIDING, enabledDisabled, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_forbidden_reference_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_FORBIDDEN_REFERENCE, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_discourraged_reference_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_DISCOURRAGED_REFERENCE, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);


		// --- unnecessary_code

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_section_unnecessary_code;
		twistieKey= OptionsConfigurationBlock.getLocalKey("ProblemSeveritiesConfigurationBlock_section_unnecessary_code"); //$NON-NLS-1$
		section= fFilteredPrefTree.addExpandableComposite(composite, label, nColumns, twistieKey, null, false);
		excomposite= getExpandableComposite(twistieKey);

		inner= createInnerComposite(excomposite, nColumns, composite.getFont());

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_local_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNUSED_LOCAL, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_parameter_label;
		node= fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNUSED_PARAMETER, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_signal_param_in_overriding_label;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_PB_SIGNAL_PARAMETER_IN_OVERRIDING, disabledEnabled, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_ignore_documented_unused_parameters;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_PB_UNUSED_PARAMETER_INCLUDE_DOC_COMMENT_REFERENCE, enabledDisabled, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_imports_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNUSED_IMPORT, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_private_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNUSED_PRIVATE, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_redundant_null_check;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_REDUNDANT_NULL_CHECK, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unnecessary_else_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNNECESSARY_ELSE, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unnecessary_type_check_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNNECESSARY_TYPE_CHECK, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_throwing_exception_label;
		node= fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_throwing_exception_when_overriding_label;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING, disabledEnabled, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_ignore_documented_unused_exceptions;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_INCLUDE_DOC_COMMENT_REFERENCE, enabledDisabled, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_throwing_exception_ignore_unchecked_label;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_EXEMPT_EXCEPTION_AND_THROWABLE, enabledDisabled, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_label_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNUSED_LABEL, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_redundant_super_interface_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_REDUNDANT_SUPERINTERFACE, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		// --- generics

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_section_generics;
		twistieKey= OptionsConfigurationBlock.getLocalKey("ProblemSeveritiesConfigurationBlock_section_generics"); //$NON-NLS-1$
		section= fFilteredPrefTree.addExpandableComposite(composite, label, nColumns, twistieKey, null, false);
		excomposite= getExpandableComposite(twistieKey);

		inner= createInnerComposite(excomposite, nColumns, composite.getFont());

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unsafe_type_op_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_15_PB_UNCHECKED_TYPE_OPERATION, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_raw_type_reference;
		fFilteredPrefTree.addComboBox(inner, label, PREF_15_PB_RAW_TYPE_REFERENCE, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_final_param_bound_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_15_PB_FINAL_PARAM_BOUND, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_redundant_type_arguments_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_17_PB_REDUNDANT_TYPE_ARGUMENTS, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unavoidable_generic_type_problems;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_15_PB_UNAVOIDABLE_GENERIC_TYPE_PROBLEMS, disabledEnabled, defaultIndent, section);
		
		// --- annotations

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_section_annotations;
		twistieKey= OptionsConfigurationBlock.getLocalKey("ProblemSeveritiesConfigurationBlock_section_annotations"); //$NON-NLS-1$
		section= fFilteredPrefTree.addExpandableComposite(composite, label, nColumns, twistieKey, null, false);
		excomposite= getExpandableComposite(twistieKey);

		inner= createInnerComposite(excomposite, nColumns, composite.getFont());

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_missing_override_annotation_label;
		node= fFilteredPrefTree.addComboBox(inner, label, PREF_15_PB_MISSING_OVERRIDE_ANNOTATION, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_missing_override_annotation_for_interface_method_implementations_label;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_16_PB_MISSING_OVERRIDE_ANNOTATION_FOR_INTERFACE_METHOD_IMPLEMENTATION, enabledDisabled, extraIndent, node);
		
		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_missing_deprecated_annotation_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_MISSING_DEPRECATED_ANNOTATION, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_annotation_super_interface_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_15_PB_ANNOTATION_SUPER_INTERFACE, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unhandled_surpresswarning_tokens;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNHANDLED_WARNING_TOKEN, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_enable_surpresswarning_annotation;
		node= fFilteredPrefTree.addCheckBox(inner, label, PREF_PB_SUPPRESS_WARNINGS, enabledDisabled, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_unused_suppresswarnings_token;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNUSED_WARNING_TOKEN, errorWarningIgnore, errorWarningIgnoreLabels, extraIndent, node);
		
		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_suppress_optional_errors_label;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_PB_SUPPRESS_OPTIONAL_ERRORS, enabledDisabled, extraIndent, node);

		GridData gd= new GridData();
		gd.verticalIndent= fPixelConverter.convertHeightInCharsToPixels(2);
		gd.horizontalSpan= 2;
		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_include_assert_in_null_analysis;
		addCheckBox(composite, label, PREF_PB_INCLUDE_ASSERTS_IN_NULL_ANALYSIS, enabledDisabled, defaultIndent);
		getCheckBox(PREF_PB_INCLUDE_ASSERTS_IN_NULL_ANALYSIS).setLayoutData(gd);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_treat_optional_as_fatal;
		addCheckBox(composite, label, PREF_PB_FATAL_OPTIONAL_ERROR, enabledDisabled, defaultIndent);

		IDialogSettings settingsSection= JavaPlugin.getDefault().getDialogSettings().getSection(SETTINGS_SECTION_NAME);
		restoreSectionExpansionStates(settingsSection);

		return sc1;
	}

	private Composite createInnerComposite(ExpandableComposite excomposite, int nColumns, Font font) {
		Composite inner= new Composite(excomposite, SWT.NONE);
		inner.setFont(font);
		inner.setLayout(new GridLayout(nColumns, false));
		excomposite.setClient(inner);
		return inner;
	}

	/* (non-javadoc)
	 * Update fields and validate.
	 * @param changedKey Key that changed, or null, if all changed.
	 */
	@Override
	protected void validateSettings(Key changedKey, String oldValue, String newValue) {
		if (!areSettingsEnabled()) {
			return;
		}

		if (changedKey != null) {
			if (PREF_PB_UNUSED_PARAMETER.equals(changedKey) ||
					PREF_PB_DEPRECATION.equals(changedKey) ||
					PREF_PB_LOCAL_VARIABLE_HIDING.equals(changedKey) ||
					PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION.equals(changedKey) ||
					PREF_15_PB_MISSING_OVERRIDE_ANNOTATION.equals(changedKey) ||
					PREF_PB_SUPPRESS_WARNINGS.equals(changedKey)) {
				updateEnableStates();
			} else if (PREF_PB_SIGNAL_PARAMETER_IN_OVERRIDING.equals(changedKey)) {
				// merging the two options
				setValue(PREF_PB_SIGNAL_PARAMETER_IN_ABSTRACT, newValue);
			} else {
				return;
			}
		} else {
			updateEnableStates();
		}
		fContext.statusChanged(new StatusInfo());
	}

	private void updateEnableStates() {
		boolean enableUnusedParams= !checkValue(PREF_PB_UNUSED_PARAMETER, IGNORE);
		getCheckBox(PREF_PB_SIGNAL_PARAMETER_IN_OVERRIDING).setEnabled(enableUnusedParams);
		getCheckBox(PREF_PB_UNUSED_PARAMETER_INCLUDE_DOC_COMMENT_REFERENCE).setEnabled(enableUnusedParams);

		boolean enableDeprecation= !checkValue(PREF_PB_DEPRECATION, IGNORE);
		getCheckBox(PREF_PB_DEPRECATION_IN_DEPRECATED_CODE).setEnabled(enableDeprecation);
		getCheckBox(PREF_PB_DEPRECATION_WHEN_OVERRIDING).setEnabled(enableDeprecation);

		boolean enableThrownExceptions= !checkValue(PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION, IGNORE);
		getCheckBox(PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING).setEnabled(enableThrownExceptions);
		getCheckBox(PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_INCLUDE_DOC_COMMENT_REFERENCE).setEnabled(enableThrownExceptions);
		getCheckBox(PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_EXEMPT_EXCEPTION_AND_THROWABLE).setEnabled(enableThrownExceptions);

		boolean enableHiding= !checkValue(PREF_PB_LOCAL_VARIABLE_HIDING, IGNORE);
		getCheckBox(PREF_PB_SPECIAL_PARAMETER_HIDING_FIELD).setEnabled(enableHiding);
		
		boolean enablemissingOverrideAnnotation= !checkValue(PREF_15_PB_MISSING_OVERRIDE_ANNOTATION, IGNORE);
		getCheckBox(PREF_16_PB_MISSING_OVERRIDE_ANNOTATION_FOR_INTERFACE_METHOD_IMPLEMENTATION).setEnabled(enablemissingOverrideAnnotation);
		
		boolean enableSuppressWarnings= checkValue(PREF_PB_SUPPRESS_WARNINGS, ENABLED);
		getCheckBox(PREF_PB_SUPPRESS_OPTIONAL_ERRORS).setEnabled(enableSuppressWarnings);
		setComboEnabled(PREF_PB_UNUSED_WARNING_TOKEN, enableSuppressWarnings);
	}

	@Override
	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		String title= PreferencesMessages.ProblemSeveritiesConfigurationBlock_needsbuild_title;
		String message;
		if (workspaceSettings) {
			message= PreferencesMessages.ProblemSeveritiesConfigurationBlock_needsfullbuild_message;
		} else {
			message= PreferencesMessages.ProblemSeveritiesConfigurationBlock_needsprojectbuild_message;
		}
		return new String[] { title, message };
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#dispose()
	 */
	@Override
	public void dispose() {
		IDialogSettings section= JavaPlugin.getDefault().getDialogSettings().addNewSection(SETTINGS_SECTION_NAME);
		storeSectionExpansionStates(section);
		super.dispose();
	}

}
