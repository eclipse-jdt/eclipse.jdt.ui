/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui;import org.eclipse.jdt.ui.JavaUI;


/**
 * Help context ids for the Java UI.
 * <p>
 * This interface contains constants only; it is not intended to be implemented
 * or extended.
 * </p>
 * 
 */
public interface IJavaHelpContextIds {
	public static final String PREFIX= JavaUI.ID_PLUGIN + '.';

	// Actions
	public static final String GETTERSETTER_ACTION= PREFIX + "getter_setter_action_context"; //$NON-NLS-1$
	public static final String ADD_METHODBREAKPOINT_ACTION= PREFIX + "add_methodbreakpoint_action_context"; //$NON-NLS-1$
	public static final String ADD_WATCHPOINT_ACTION= PREFIX + "add_watchpoint_action_context"; //$NON-NLS-1$
	public static final String ADD_METHODSTUB_ACTION= PREFIX + "add_methodstub_action_context"; //$NON-NLS-1$
	public static final String ADD_UNIMPLEMENTED_METHODS_ACTION= PREFIX + "add_unimplemented_methods_action_context"; //$NON-NLS-1$
	public static final String SHOW_IN_PACKAGEVIEW_ACTION= PREFIX + "show_in_packageview_action_context"; //$NON-NLS-1$
	public static final String SHOW_IN_HIERARCHYVIEW_ACTION= PREFIX + "show_in_hierarchyview_action_context"; //$NON-NLS-1$
	public static final String FOCUS_ON_SELECTION_ACTION= PREFIX + "focus_on_selection_action"; //$NON-NLS-1$
	public static final String FOCUS_ON_TYPE_ACTION= PREFIX + "focus_on_type_action"; //$NON-NLS-1$

	public static final String TYPEHIERARCHY_FORWARD_ACTION= PREFIX + "typehierarchy_forward_action"; //$NON-NLS-1$
	public static final String TYPEHIERARCHY_BACKWARD_ACTION= PREFIX + "typehierarchy_backward_action"; //$NON-NLS-1$
	public static final String FILTER_PUBLIC_ACTION= PREFIX + "filter_public_action"; //$NON-NLS-1$
	public static final String FILTER_FIELDS_ACTION= PREFIX + "filter_fields_action"; //$NON-NLS-1$
	public static final String FILTER_STATIC_ACTION= PREFIX + "filter_static_action"; //$NON-NLS-1$
	public static final String SHOW_INHERITED_ACTION= PREFIX + "show_inherited_action"; //$NON-NLS-1$
	public static final String SHOW_SUPERTYPES= PREFIX + "show_supertypes_action"; //$NON-NLS-1$
	public static final String SHOW_SUBTYPES= PREFIX + "show_subtypes_action"; //$NON-NLS-1$
	public static final String SHOW_HIERARCHY= PREFIX + "show_hierarchy_action"; //$NON-NLS-1$
	public static final String ENABLE_METHODFILTER_ACTION= PREFIX + "enable_methodfilter_action"; //$NON-NLS-1$
	public static final String ADD_IMPORT_ON_SELECTION_ACTION= PREFIX + "add_imports_on_selection_action_context"; //$NON-NLS-1$
	public static final String ORGANIZE_IMPORTS_ACTION= PREFIX + "organize_imports_action_context"; //$NON-NLS-1$

	public static final String RUN_TO_LINE_ACTION= PREFIX + "run_to_line_action_context"; //$NON-NLS-1$
	public static final String TOGGLE_PRESENTATION_ACTION= PREFIX + "toggle_presentation_action_context"; //$NON-NLS-1$
	public static final String TOGGLE_TEXTHOVER_ACTION= PREFIX + "toggle_texthover_action_context"; //$NON-NLS-1$
	public static final String DISPLAY_ACTION= PREFIX + "display_action_context"; //$NON-NLS-1$

	// Dialogs
	public static final String MAINTYPE_SELECTION_DIALOG= PREFIX + "maintype_selection_dialog_context"; //$NON-NLS-1$
	public static final String OPEN_TYPE_DIALOG= PREFIX + "open_type_dialog_context"; //$NON-NLS-1$
	public static final String EDIT_JRE_DIALOG= PREFIX + "edit_jre_dialog_context"; //$NON-NLS-1$
	public static final String SOURCE_ATTACHMENT_DIALOG= PREFIX + "source_attachment_dialog_context"; //$NON-NLS-1$
	public static final String VARIABLE_SELECTION_DIALOG= PREFIX + "variable_selection_dialog_context"; //$NON-NLS-1$
	public static final String VARIABLE_CREATION_DIALOG= PREFIX + "variable_creation_dialog_context"; //$NON-NLS-1$

	public static final String JAVA_SEARCH_PAGE= PREFIX + "java_search_page_context"; //$NON-NLS-1$


	// view parts
	public static final String PACKAGE_VIEW= PREFIX + "package_view_context"; //$NON-NLS-1$
	public static final String TYPE_HIERARCHY_VIEW= PREFIX + "type_hierarchy_view_context"; //$NON-NLS-1$
	public static final String DISPLAY_VIEW= PREFIX + "display_view_context"; //$NON-NLS-1$


	// Preference/Property pages
	public static final String BUILD_PATH_PROPERTY_PAGE= PREFIX + "build_path_property_page_context"; //$NON-NLS-1$
	public static final String CP_VARIABLES_PREFERENCE_PAGE= PREFIX + "cp_variables_preference_page_context"; //$NON-NLS-1$
	public static final String CODEFORMATTER_PREFERENCE_PAGE= PREFIX + "codeformatter_preference_page_context"; //$NON-NLS-1$
	public static final String SOURCE_ATTACHMENT_PROPERTY_PAGE= PREFIX + "source_attachment_property_page_context"; //$NON-NLS-1$
	public static final String JRE_PREFERENCE_PAGE= PREFIX + "jre_preference_page_context"; //$NON-NLS-1$
	public static final String SOURCE_LOOKUP_PROPERTY_PAGE= PREFIX + "source_lookup_property_page_context"; //$NON-NLS-1$
	public static final String LAUNCH_JRE_PROPERYY_PAGE= PREFIX + "launch_jre_property_page_context"; //$NON-NLS-1$

	public static final String ORGANIZE_IMPORTS_PREFERENCE_PAGE= PREFIX + "organizeimports_preference_page_context"; //$NON-NLS-1$
	public static final String JAVA_BASE_PREFERENCE_PAGE= PREFIX + "java_base_preference_page_context"; //$NON-NLS-1$
	public static final String REFACTORING_PREFERENCE_PAGE= PREFIX + "refactoring_preference_page_context"; //$NON-NLS-1$
	public static final String JAVA_EDITOR_PREFERENCE_PAGE= PREFIX + "java_editor_preference_page_context"; //$NON-NLS-1$
	public static final String COMPILER_PREFERENCE_PAGE= PREFIX + "compiler_preference_page_context"; //$NON-NLS-1$


	// Wizard pages
	public static final String NEW_JAVAPROJECT_WIZARD_PAGE= PREFIX + "new_javaproject_wizard_page_context"; //$NON-NLS-1$
	public static final String NEW_SNIPPET_WIZARD_PAGE= PREFIX + "new_snippet_wizard_page_context"; //$NON-NLS-1$
	public static final String NEW_PACKAGE_WIZARD_PAGE= PREFIX + "new_package_wizard_page_context"; //$NON-NLS-1$
	public static final String NEW_CLASS_WIZARD_PAGE= PREFIX + "new_class_wizard_page_context"; //$NON-NLS-1$
	public static final String NEW_INTERFACE_WIZARD_PAGE= PREFIX + "new_interface_wizard_page_context"; //$NON-NLS-1$
	public static final String NEW_PACKAGEROOT_WIZARD_PAGE= PREFIX + "new_packageroot_wizard_page_context"; //$NON-NLS-1$
	public static final String JAVA_APPLICATION_WIZARD_PAGE= PREFIX + "java_application_page_context"; //$NON-NLS-1$
	public static final String JDI_ATTACH_LAUNCHER_WIZARD_PAGE= PREFIX + "jdi_attach_launcher_page_context"; //$NON-NLS-1$
	public static final String JARPACKAGER_WIZARD_PAGE= PREFIX + "jar_packager_wizard_page_context"; //$NON-NLS-1$
	public static final String JARMANIFEST_WIZARD_PAGE= PREFIX + "jar_manifest_wizard_page_context"; //$NON-NLS-1$
	public static final String JAROPTIONS_WIZARD_PAGE= PREFIX + "jar_options_wizard_page_context"; //$NON-NLS-1$

	// same help for all refactoring preview pages
	public static final String REFACTORING_PREVIEW_WIZARD_PAGE= PREFIX + "refactoring_preview_wizard_page_context"; //$NON-NLS-1$
	
	public static final String MOVE_CU_WIZARD_PAGE= PREFIX + "move_cu_wizard_page_context"; //$NON-NLS-1$
	public static final String MOVE_CU_ERROR_WIZARD_PAGE= PREFIX + "move_cu_error_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_PARAMS_WIZARD_PAGE= PREFIX + "rename_params_wizard_page"; //$NON-NLS-1$
	public static final String RENAME_PARAMS_ERROR_WIZARD_PAGE= PREFIX + "rename_params_error_wizard_page"; //$NON-NLS-1$
	public static final String EXTRACT_METHOD_WIZARD_PAGE= PREFIX + "extract_method_wizard_page_context"; //$NON-NLS-1$
	public static final String EXTRACT_METHOD_ERROR_WIZARD_PAGE= PREFIX + "extract_method_error_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_PACKAGE_WIZARD_PAGE= PREFIX + "rename_package_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_PACKAGE_ERROR_WIZARD_PAGE= PREFIX + "rename_package_error_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_CU_WIZARD_PAGE= PREFIX + "rename_cu_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_CU_ERROR_WIZARD_PAGE= PREFIX + "rename_cu_error_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_METHOD_WIZARD_PAGE= PREFIX + "rename_method_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_METHOD_ERROR_WIZARD_PAGE= PREFIX + "rename_method_error_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_TYPE_WIZARD_PAGE= PREFIX + "rename_type_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_TYPE_ERROR_WIZARD_PAGE= PREFIX + "rename_type_error_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_FIELD_WIZARD_PAGE= PREFIX + "rename_field_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_FIELD_ERROR_WIZARD_PAGE= PREFIX + "rename_field_error_wizard_page_context"; //$NON-NLS-1$

	// reused ui-blocks
	public static final String BUILD_PATH_BLOCK= PREFIX + "build_paths_context"; //$NON-NLS-1$
	public static final String SOURCE_ATTACHMENT_BLOCK= PREFIX + "source_attachment_context"; //$NON-NLS-1$
	
	
}