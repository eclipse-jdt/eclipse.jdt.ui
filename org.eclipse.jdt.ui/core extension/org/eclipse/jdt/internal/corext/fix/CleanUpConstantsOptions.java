/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alex Blewitt - https://bugs.eclipse.org/bugs/show_bug.cgi?id=168954
 *     Red Hat Inc. - split from CleanUpConstants
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.Iterator;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.fix.UnimplementedCodeCleanUp;

public class CleanUpConstantsOptions extends CleanUpConstants {

	private static void setEclipseDefaultSettings(CleanUpOptions options) {

		//Member Accesses
		options.setOption(MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS, CleanUpOptions.FALSE);
		options.setOption(MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS, CleanUpOptions.FALSE);
		options.setOption(MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY, CleanUpOptions.TRUE);

		options.setOption(MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS, CleanUpOptions.FALSE);
		options.setOption(MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS, CleanUpOptions.FALSE);
		options.setOption(MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY, CleanUpOptions.TRUE);

		options.setOption(MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS, CleanUpOptions.TRUE);
		options.setOption(MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD, CleanUpOptions.FALSE);
		options.setOption(MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD, CleanUpOptions.FALSE);
		options.setOption(MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS, CleanUpOptions.TRUE);
		options.setOption(MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS, CleanUpOptions.TRUE);

		//Control Statements
		options.setOption(CONTROL_STATEMENTS_USE_BLOCKS, CleanUpOptions.FALSE);
		options.setOption(CONTROL_STATMENTS_USE_BLOCKS_ALWAYS, CleanUpOptions.TRUE);
		options.setOption(CONTROL_STATMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW, CleanUpOptions.FALSE);
		options.setOption(CONTROL_STATMENTS_USE_BLOCKS_NEVER, CleanUpOptions.FALSE);

		options.setOption(CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED, CleanUpOptions.FALSE);

		//Expressions
		options.setOption(EXPRESSIONS_USE_PARENTHESES, CleanUpOptions.FALSE);
		options.setOption(EXPRESSIONS_USE_PARENTHESES_NEVER, CleanUpOptions.TRUE);
		options.setOption(EXPRESSIONS_USE_PARENTHESES_ALWAYS, CleanUpOptions.FALSE);

		//Variable Declarations
		options.setOption(VARIABLE_DECLARATIONS_USE_FINAL, CleanUpOptions.FALSE);
		options.setOption(VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES, CleanUpOptions.TRUE);
		options.setOption(VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS, CleanUpOptions.FALSE);
		options.setOption(VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS, CleanUpOptions.TRUE);

		//Functional Interfaces
		options.setOption(CONVERT_FUNCTIONAL_INTERFACES, CleanUpOptions.FALSE);
		options.setOption(USE_LAMBDA, CleanUpOptions.TRUE);
		options.setOption(USE_ANONYMOUS_CLASS_CREATION, CleanUpOptions.FALSE);

		//Unused Code
		options.setOption(REMOVE_UNUSED_CODE_IMPORTS, CleanUpOptions.TRUE);
		options.setOption(REMOVE_UNUSED_CODE_PRIVATE_MEMBERS, CleanUpOptions.FALSE);
		options.setOption(REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS, CleanUpOptions.TRUE);
		options.setOption(REMOVE_UNUSED_CODE_PRIVATE_FELDS, CleanUpOptions.TRUE);
		options.setOption(REMOVE_UNUSED_CODE_PRIVATE_METHODS, CleanUpOptions.TRUE);
		options.setOption(REMOVE_UNUSED_CODE_PRIVATE_TYPES, CleanUpOptions.TRUE);
		options.setOption(REMOVE_UNUSED_CODE_LOCAL_VARIABLES, CleanUpOptions.FALSE);

		//Unnecessary Code
		options.setOption(REMOVE_UNNECESSARY_CASTS, CleanUpOptions.TRUE);
		options.setOption(REMOVE_UNNECESSARY_NLS_TAGS, CleanUpOptions.TRUE);
		options.setOption(INSERT_INFERRED_TYPE_ARGUMENTS, CleanUpOptions.FALSE);
		options.setOption(REMOVE_REDUNDANT_TYPE_ARGUMENTS, CleanUpOptions.FALSE);
		options.setOption(REMOVE_REDUNDANT_MODIFIERS, CleanUpOptions.FALSE);
		options.setOption(REMOVE_REDUNDANT_SEMICOLONS, CleanUpOptions.FALSE);

		//Missing Code
		options.setOption(ADD_MISSING_ANNOTATIONS, CleanUpOptions.TRUE);
		options.setOption(ADD_MISSING_ANNOTATIONS_OVERRIDE, CleanUpOptions.TRUE);
		options.setOption(ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION, CleanUpOptions.TRUE);
		options.setOption(ADD_MISSING_ANNOTATIONS_DEPRECATED, CleanUpOptions.TRUE);

		options.setOption(ADD_MISSING_SERIAL_VERSION_ID, CleanUpOptions.FALSE);
		options.setOption(ADD_MISSING_SERIAL_VERSION_ID_GENERATED, CleanUpOptions.FALSE);
		options.setOption(ADD_MISSING_SERIAL_VERSION_ID_DEFAULT, CleanUpOptions.TRUE);

		options.setOption(ADD_MISSING_NLS_TAGS, CleanUpOptions.FALSE);

		options.setOption(ADD_MISSING_METHODES, CleanUpOptions.FALSE);
		options.setOption(UnimplementedCodeCleanUp.MAKE_TYPE_ABSTRACT, CleanUpOptions.FALSE);

		//Code Organizing
		options.setOption(FORMAT_SOURCE_CODE, CleanUpOptions.FALSE);
		options.setOption(FORMAT_SOURCE_CODE_CHANGES_ONLY, CleanUpOptions.FALSE);

		options.setOption(FORMAT_REMOVE_TRAILING_WHITESPACES, CleanUpOptions.FALSE);
		options.setOption(FORMAT_REMOVE_TRAILING_WHITESPACES_ALL, CleanUpOptions.TRUE);
		options.setOption(FORMAT_REMOVE_TRAILING_WHITESPACES_IGNORE_EMPTY, CleanUpOptions.FALSE);

		options.setOption(FORMAT_CORRECT_INDENTATION, CleanUpOptions.FALSE);

		options.setOption(ORGANIZE_IMPORTS, CleanUpOptions.FALSE);

		options.setOption(SORT_MEMBERS, CleanUpOptions.FALSE);
		options.setOption(SORT_MEMBERS_ALL, CleanUpOptions.FALSE);
	}

	private static void setSaveParticipantSettings(CleanUpOptions options) {

		//Member Accesses
		options.setOption(MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS, CleanUpOptions.FALSE);
		options.setOption(MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS, CleanUpOptions.FALSE);
		options.setOption(MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY, CleanUpOptions.TRUE);

		options.setOption(MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS, CleanUpOptions.FALSE);
		options.setOption(MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_ALWAYS, CleanUpOptions.FALSE);
		options.setOption(MEMBER_ACCESSES_NON_STATIC_METHOD_USE_THIS_IF_NECESSARY, CleanUpOptions.TRUE);

		options.setOption(MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS, CleanUpOptions.FALSE);
		options.setOption(MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_FIELD, CleanUpOptions.FALSE);
		options.setOption(MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_METHOD, CleanUpOptions.FALSE);
		options.setOption(MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS, CleanUpOptions.TRUE);
		options.setOption(MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS, CleanUpOptions.TRUE);

		//Control Statements
		options.setOption(CONTROL_STATEMENTS_USE_BLOCKS, CleanUpOptions.FALSE);
		options.setOption(CONTROL_STATMENTS_USE_BLOCKS_ALWAYS, CleanUpOptions.TRUE);
		options.setOption(CONTROL_STATMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW, CleanUpOptions.FALSE);
		options.setOption(CONTROL_STATMENTS_USE_BLOCKS_NEVER, CleanUpOptions.FALSE);

		options.setOption(CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED, CleanUpOptions.FALSE);

		//Expressions
		options.setOption(EXPRESSIONS_USE_PARENTHESES, CleanUpOptions.FALSE);
		options.setOption(EXPRESSIONS_USE_PARENTHESES_NEVER, CleanUpOptions.TRUE);
		options.setOption(EXPRESSIONS_USE_PARENTHESES_ALWAYS, CleanUpOptions.FALSE);

		//Variable Declarations
		options.setOption(VARIABLE_DECLARATIONS_USE_FINAL, CleanUpOptions.FALSE);
		options.setOption(VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES, CleanUpOptions.TRUE);
		options.setOption(VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS, CleanUpOptions.FALSE);
		options.setOption(VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS, CleanUpOptions.TRUE);

		//Functional Interfaces
		options.setOption(CONVERT_FUNCTIONAL_INTERFACES, CleanUpOptions.FALSE);
		options.setOption(USE_LAMBDA, CleanUpOptions.TRUE);
		options.setOption(USE_ANONYMOUS_CLASS_CREATION, CleanUpOptions.FALSE);

		//Unused Code
		options.setOption(REMOVE_UNUSED_CODE_IMPORTS, CleanUpOptions.FALSE);
		options.setOption(REMOVE_UNUSED_CODE_PRIVATE_MEMBERS, CleanUpOptions.FALSE);
		options.setOption(REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS, CleanUpOptions.TRUE);
		options.setOption(REMOVE_UNUSED_CODE_PRIVATE_FELDS, CleanUpOptions.TRUE);
		options.setOption(REMOVE_UNUSED_CODE_PRIVATE_METHODS, CleanUpOptions.TRUE);
		options.setOption(REMOVE_UNUSED_CODE_PRIVATE_TYPES, CleanUpOptions.TRUE);
		options.setOption(REMOVE_UNUSED_CODE_LOCAL_VARIABLES, CleanUpOptions.FALSE);

		//Unnecessary Code
		options.setOption(REMOVE_UNNECESSARY_CASTS, CleanUpOptions.TRUE);
		options.setOption(REMOVE_UNNECESSARY_NLS_TAGS, CleanUpOptions.FALSE);
		options.setOption(INSERT_INFERRED_TYPE_ARGUMENTS, CleanUpOptions.FALSE);
		options.setOption(REMOVE_REDUNDANT_TYPE_ARGUMENTS, CleanUpOptions.FALSE);
		options.setOption(REMOVE_REDUNDANT_MODIFIERS, CleanUpOptions.FALSE);
		options.setOption(REMOVE_REDUNDANT_SEMICOLONS, CleanUpOptions.FALSE);

		//Missing Code
		options.setOption(ADD_MISSING_ANNOTATIONS, CleanUpOptions.TRUE);
		options.setOption(ADD_MISSING_ANNOTATIONS_OVERRIDE, CleanUpOptions.TRUE);
		options.setOption(ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION, CleanUpOptions.TRUE);
		options.setOption(ADD_MISSING_ANNOTATIONS_DEPRECATED, CleanUpOptions.TRUE);

		options.setOption(ADD_MISSING_SERIAL_VERSION_ID, CleanUpOptions.FALSE);
		options.setOption(ADD_MISSING_SERIAL_VERSION_ID_GENERATED, CleanUpOptions.FALSE);
		options.setOption(ADD_MISSING_SERIAL_VERSION_ID_DEFAULT, CleanUpOptions.TRUE);

		options.setOption(ADD_MISSING_NLS_TAGS, CleanUpOptions.FALSE);

		options.setOption(ADD_MISSING_METHODES, CleanUpOptions.FALSE);
		options.setOption(UnimplementedCodeCleanUp.MAKE_TYPE_ABSTRACT, CleanUpOptions.FALSE);

		//Code Organizing
		options.setOption(FORMAT_SOURCE_CODE, CleanUpOptions.FALSE);
		options.setOption(FORMAT_SOURCE_CODE_CHANGES_ONLY, CleanUpOptions.FALSE);

		options.setOption(FORMAT_REMOVE_TRAILING_WHITESPACES, CleanUpOptions.FALSE);
		options.setOption(FORMAT_REMOVE_TRAILING_WHITESPACES_ALL, CleanUpOptions.TRUE);
		options.setOption(FORMAT_REMOVE_TRAILING_WHITESPACES_IGNORE_EMPTY, CleanUpOptions.FALSE);

		options.setOption(FORMAT_CORRECT_INDENTATION, CleanUpOptions.FALSE);

		options.setOption(ORGANIZE_IMPORTS, CleanUpOptions.TRUE);

		options.setOption(SORT_MEMBERS, CleanUpOptions.FALSE);
		options.setOption(SORT_MEMBERS_ALL, CleanUpOptions.FALSE);

		options.setOption(CLEANUP_ON_SAVE_ADDITIONAL_OPTIONS, CleanUpOptions.FALSE);
	}

	public static void initDefaults(IPreferenceStore store) {
		CleanUpOptions settings= JavaPlugin.getDefault().getCleanUpRegistry().getDefaultOptions(CleanUpConstants.DEFAULT_CLEAN_UP_OPTIONS);
		for (Iterator<String> iterator= settings.getKeys().iterator(); iterator.hasNext();) {
			String key= iterator.next();
			store.setDefault(key, settings.getValue(key));
		}

		store.setDefault(SHOW_CLEAN_UP_WIZARD, true);
		store.setDefault(CLEANUP_PROFILE, DEFAULT_PROFILE);
		store.setDefault(CLEANUP_ON_SAVE_PROFILE, DEFAULT_SAVE_PARTICIPANT_PROFILE);
	}

	public static void setDefaultOptions(int kind, CleanUpOptions options) {
		switch (kind) {
			case CleanUpConstants.DEFAULT_CLEAN_UP_OPTIONS:
				CleanUpConstantsOptions.setEclipseDefaultSettings(options);
				break;
			case CleanUpConstants.DEFAULT_SAVE_ACTION_OPTIONS:
				CleanUpConstantsOptions.setSaveParticipantSettings(options);
				break;
			default:
				Assert.isTrue(false, "Unknown Clean Up option kind: " + kind); //$NON-NLS-1$
				break;
		}
	}

}
