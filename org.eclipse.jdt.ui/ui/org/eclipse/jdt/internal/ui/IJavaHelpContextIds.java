/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui;

import org.eclipse.jdt.ui.JavaUI;


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
	public static final String GETTERSETTER_ACTION= 											PREFIX + "getter_setter_action_context"; //$NON-NLS-1$
	public static final String ADD_METHODSTUB_ACTION= 										PREFIX + "add_methodstub_action_context"; //$NON-NLS-1$
	public static final String ADD_UNIMPLEMENTED_METHODS_ACTION= 					PREFIX + "add_unimplemented_methods_action_context"; //$NON-NLS-1$
	public static final String ADD_UNIMPLEMENTED_CONSTRUCTORS_ACTION= 			PREFIX + "add_unimplemented_constructors_action_context"; //$NON-NLS-1$	
	public static final String SHOW_IN_PACKAGEVIEW_ACTION= 								PREFIX + "show_in_packageview_action_context"; //$NON-NLS-1$
	public static final String SHOW_IN_HIERARCHYVIEW_ACTION= 							PREFIX + "show_in_hierarchyview_action_context"; //$NON-NLS-1$
	public static final String FOCUS_ON_SELECTION_ACTION= 								PREFIX + "focus_on_selection_action"; //$NON-NLS-1$
	public static final String FOCUS_ON_TYPE_ACTION= 										PREFIX + "focus_on_type_action"; //$NON-NLS-1$

	public static final String TYPEHIERARCHY_HISTORY_ACTION= 							PREFIX + "typehierarchy_history_action"; //$NON-NLS-1$
	public static final String FILTER_PUBLIC_ACTION= 											PREFIX + "filter_public_action"; //$NON-NLS-1$
	public static final String FILTER_FIELDS_ACTION= 											PREFIX + "filter_fields_action"; //$NON-NLS-1$
	public static final String FILTER_STATIC_ACTION= 											PREFIX + "filter_static_action"; //$NON-NLS-1$
	public static final String SHOW_INHERITED_ACTION= 										PREFIX + "show_inherited_action"; //$NON-NLS-1$
	public static final String SHOW_SUPERTYPES= 												PREFIX + "show_supertypes_action"; //$NON-NLS-1$
	public static final String SHOW_SUBTYPES= 													PREFIX + "show_subtypes_action"; //$NON-NLS-1$
	public static final String SHOW_HIERARCHY= 													PREFIX + "show_hierarchy_action"; //$NON-NLS-1$
	public static final String ENABLE_METHODFILTER_ACTION= 								PREFIX + "enable_methodfilter_action"; //$NON-NLS-1$
	public static final String ADD_IMPORT_ON_SELECTION_ACTION= 						PREFIX + "add_imports_on_selection_action_context"; //$NON-NLS-1$
	public static final String ORGANIZE_IMPORTS_ACTION= 									PREFIX + "organize_imports_action_context"; //$NON-NLS-1$

	public static final String TOGGLE_PRESENTATION_ACTION= 								PREFIX + "toggle_presentation_action_context"; //$NON-NLS-1$
	public static final String TOGGLE_TEXTHOVER_ACTION= 									PREFIX + "toggle_texthover_action_context"; //$NON-NLS-1$

	public static final String OPEN_CLASS_WIZARD_ACTION= 									PREFIX + "open_class_wizard_action"; //$NON-NLS-1$
	public static final String OPEN_INTERFACE_WIZARD_ACTION= 							PREFIX + "open_interface_wizard_action"; //$NON-NLS-1$
	public static final String OPEN_PACKAGE_WIZARD_ACTION= 								PREFIX + "open_package_wizard_action"; //$NON-NLS-1$
	public static final String OPEN_PROJECT_WIZARD_ACTION= 								PREFIX + "open_project_wizard_action"; //$NON-NLS-1$
	public static final String OPEN_SNIPPET_WIZARD_ACTION= 								PREFIX + "open_snippet_wizard_action"; //$NON-NLS-1$
	public static final String EDIT_WORKING_SET_ACTION= 									PREFIX + "edit_working_set_action"; //$NON-NLS-1$
	public static final String CLEAR_WORKING_SET_ACTION= 									PREFIX + "clear_working_set_action"; //$NON-NLS-1$
	public static final String GOTO_MARKER_ACTION= 											PREFIX + "goto_marker_action"; //$NON-NLS-1$
	public static final String GOTO_PACKAGE_ACTION= 											PREFIX + "goto_package_action"; //$NON-NLS-1$
	public static final String HISTORY_ACTION= 													PREFIX + "history_action"; //$NON-NLS-1$
	public static final String HISTORY_LIST_ACTION= 											PREFIX + "history_list_action"; //$NON-NLS-1$
	public static final String LEXICAL_SORTING_OUTLINE_ACTION= 							PREFIX + "lexical_sorting_outline_action"; //$NON-NLS-1$
	public static final String LEXICAL_SORTING_BROWSING_ACTION= 						PREFIX + "lexical_sorting_browsing_action"; //$NON-NLS-1$
	public static final String OPEN_JAVA_PERSPECTIVE_ACTION= 							PREFIX + "open_java_perspective_action"; //$NON-NLS-1$
	public static final String OPEN_JAVA_BROWSING_PERSPECTIVE_ACTION= 			PREFIX + "open_java_browsing_perspective_action"; //$NON-NLS-1$
	public static final String OPEN_PROJECT_ACTION= 											PREFIX + "open_project_action"; //$NON-NLS-1$
	public static final String OPEN_TYPE_ACTION= 												PREFIX + "open_type_action"; //$NON-NLS-1$
	public static final String OPEN_TYPE_IN_HIERARCHY_ACTION= 							PREFIX + "open_type_in_hierarchy_action"; //$NON-NLS-1$
	public static final String ADD_JAVADOC_STUB_ACTION= 									PREFIX + "add_javadoc_stub_action"; //$NON-NLS-1$
	public static final String ADD_TASK_ACTION= 												PREFIX + "add_task_action"; //$NON-NLS-1$
	public static final String EXTERNALIZE_STRINGS_ACTION= 								PREFIX + "externalize_strings_action"; //$NON-NLS-1$	
	public static final String EXTRACT_METHOD_ACTION= 										PREFIX + "extract_method_action"; //$NON-NLS-1$	
	public static final String EXTRACT_TEMP_ACTION= 											PREFIX + "extract_temp_action"; //$NON-NLS-1$	
	public static final String EXTRACT_CONSTANT_ACTION= 											PREFIX + "extract_constant_action"; //$NON-NLS-1$	
	public static final String EXTRACT_INTERFACE_ACTION= 									PREFIX + "extract_interface_action"; //$NON-NLS-1$	
	public static final String MOVE_INNER_TO_TOP_ACTION= 									PREFIX + "move_inner_to_top_level_action"; //$NON-NLS-1$
	public static final String USE_SUPERTYPE_ACTION= 										PREFIX + "use_supertype_action"; //$NON-NLS-1$
	public static final String FIND_DECLARATIONS_IN_HIERARCHY_ACTION= 				PREFIX + "find_declarations_in_hierarchy_action"; //$NON-NLS-1$	
	public static final String FIND_DECLARATIONS_IN_WORKING_SET_ACTION= 			PREFIX + "find_declarations_in_working_set_action"; //$NON-NLS-1$	
	public static final String FIND_IMPLEMENTORS_IN_WORKING_SET_ACTION= 			PREFIX + "find_implementors_in_working_set_action"; //$NON-NLS-1$			
	public static final String FIND_READ_REFERENCES_ACTION= 								PREFIX + "find_read_references_action"; //$NON-NLS-1$			
	public static final String FIND_READ_REFERENCES_IN_HIERARCHY_ACTION= 			PREFIX + "find_read_references_in_hierarchy_action"; //$NON-NLS-1$
	public static final String FIND_WRITE_REFERENCES_IN_HIERARCHY_ACTION= 		PREFIX + "find_write_references_in_hierarchy_action"; //$NON-NLS-1$
	public static final String FIND_READ_REFERENCES_IN_WORKING_SET_ACTION= 	PREFIX + "find_read_references_in_working_set_action"; //$NON-NLS-1$
	public static final String FIND_WRITE_REFERENCES_IN_WORKING_SET_ACTION=	PREFIX + "find_write_references_in_working_set_action"; //$NON-NLS-1$
	public static final String FIND_WRITE_REFERENCES_ACTION= 							PREFIX + "find_write_references_action"; //$NON-NLS-1$
	public static final String WORKING_SET_FIND_ACTION=									PREFIX + "working_set_find_action"; //$NON-NLS-1$
	public static final String FIND_STRINGS_TO_EXTERNALIZE_ACTION= 					PREFIX + "find_strings_to_externalize_action"; //$NON-NLS-1$
	public static final String INLINE_TEMP_ACTION= 												PREFIX + "inline_temp_action"; //$NON-NLS-1$
	public static final String MODIFY_PARAMETERS_ACTION= 									PREFIX + "modify_parameters_action"; //$NON-NLS-1$
	public static final String MOVE_ACTION= 														PREFIX + "move_action"; //$NON-NLS-1$
	public static final String OPEN_ACTION= 														PREFIX + "open_action"; //$NON-NLS-1$
	public static final String OPEN_EXTERNAL_JAVADOC_ACTION= 							PREFIX + "open_external_javadoc_action"; //$NON-NLS-1$
	public static final String OPEN_SUPER_IMPLEMENTATION_ACTION= 					PREFIX + "open_super_implementation_action"; //$NON-NLS-1$
	public static final String PULL_UP_ACTION= 													PREFIX + "pull_up_action"; //$NON-NLS-1$
	public static final String REFRESH_ACTION= 													PREFIX + "refresh_action"; //$NON-NLS-1$
	public static final String RENAME_ACTION= 													PREFIX + "rename_action"; //$NON-NLS-1$
	public static final String SELF_ENCAPSULATE_ACTION=									PREFIX + "self_encapsulate_action"; //$NON-NLS-1$
	public static final String SHOW_IN_NAVIGATOR_VIEW_ACTION= 						PREFIX + "show_in_navigator_action"; //$NON-NLS-1$
	public static final String SURROUND_WITH_TRY_CATCH_ACTION= 						PREFIX + "surround_with_try_catch_action"; //$NON-NLS-1$	
	public static final String OPEN_RESOURCE_ACTION= 										PREFIX + "open_resource_action"; //$NON-NLS-1$	
	public static final String SELECT_WORKING_SET_ACTION= 								PREFIX + "select_working_set_action"; //$NON-NLS-1$	
	public static final String STRUCTURED_SELECTION_HISTORY_ACTION= 				PREFIX + "structured_selection_history_action"; //$NON-NLS-1$	
	public static final String STRUCTURED_SELECT_ENCLOSING_ACTION= 					PREFIX + "structured_select_enclosing_action"; //$NON-NLS-1$	
	public static final String STRUCTURED_SELECT_NEXT_ACTION= 							PREFIX + "structured_select_next_action"; //$NON-NLS-1$	
	public static final String STRUCTURED_SELECT_PREVIOUS_ACTION= 					PREFIX + "structured_select_previous_action"; //$NON-NLS-1$	
	public static final String TOGGLE_ORIENTATION_ACTION= 								PREFIX + "toggle_orientations_action"; //$NON-NLS-1$		
	public static final String CUT_ACTION= 															PREFIX + "cut_action"; //$NON-NLS-1$	
	public static final String COPY_ACTION= 														PREFIX + "copy_action"; //$NON-NLS-1$	
	public static final String PASTE_ACTION= 														PREFIX + "paste_action"; //$NON-NLS-1$	
	public static final String DELETE_ACTION= 													PREFIX + "delete_action"; //$NON-NLS-1$	
	public static final String SELECT_ALL_ACTION= 												PREFIX + "select_all_action"; //$NON-NLS-1$
	public static final String OPEN_TYPE_HIERARCHY_ACTION= 								PREFIX + "open_type_hierarchy_action"; //$NON-NLS-1$	
	public static final String COLLAPSE_ALL_ACTION= 								PREFIX + "open_type_hierarchy_action"; //$NON-NLS-1$	

	// Dialogs
	public static final String MAINTYPE_SELECTION_DIALOG= PREFIX + "maintype_selection_dialog_context"; //$NON-NLS-1$
	public static final String OPEN_TYPE_DIALOG= PREFIX + "open_type_dialog_context"; //$NON-NLS-1$
	public static final String SOURCE_ATTACHMENT_DIALOG= PREFIX + "source_attachment_dialog_context"; //$NON-NLS-1$
	public static final String LIBRARIES_WORKBOOK_PAGE_ADVANCED_DIALOG= PREFIX + "advanced_dialog_context"; //$NON-NLS-1$
	public static final String CONFIRM_SAVE_MODIFIED_RESOURCES_DIALOG= PREFIX + "confirm_save_modified_resources_dialog_context"; //$NON-NLS-1$
	public static final String NEW_VARIABLE_ENTRY_DIALOG= PREFIX + "new_variable_dialog_context"; //$NON-NLS-1$
	public static final String COMPARE_DIALOG= PREFIX + "compare_dialog_context"; //$NON-NLS-1$
	public static final String NONNLS_DIALOG= PREFIX + "nonnls_dialog_context"; //$NON-NLS-1$
	public static final String MULTI_MAIN_TYPE_SELECTION_DIALOG= PREFIX + "multi_main_type_selection_dialog_context"; //$NON-NLS-1$
	public static final String MULTI_TYPE_SELECTION_DIALOG= PREFIX + "multi_type_selection_dialog_context"; //$NON-NLS-1$
	public static final String SUPER_INTERFACE_SELECTION_DIALOG= PREFIX + "super_interface_selection_dialog_context"; //$NON-NLS-1$
	public static final String OVERRIDE_TREE_SELECTION_DIALOG= PREFIX + "override_tree_selection_dialog_context"; //$NON-NLS-1$
	public static final String MOVE_DESTINATION_DIALOG= PREFIX + "move_destination_dialog_context"; //$NON-NLS-1$
	public static final String CHOOSE_VARIABLE_DIALOG= PREFIX + "choose_variable_dialog_context"; //$NON-NLS-1$	
	public static final String EDIT_TEMPLATE_DIALOG= PREFIX + "edit_template_dialog_context"; //$NON-NLS-1$	
	public static final String HISTORY_LIST_DIALOG= PREFIX + "history_list_dialog_context"; //$NON-NLS-1$	
	public static final String IMPORT_ORGANIZE_INPUT_DIALOG= PREFIX + "import_organize_input_dialog_context"; //$NON-NLS-1$	
	public static final String JAVADOC_PROPERTY_DIALOG= PREFIX + "javadoc_property_dialog_context"; //$NON-NLS-1$	
	public static final String NEW_CONTAINER_DIALOG= PREFIX + "new_container_dialog_context"; //$NON-NLS-1$	
	public static final String VARIABLE_CREATION_DIALOG= PREFIX + "variable_creation_dialog_context"; //$NON-NLS-1$	
	
	public static final String JAVA_SEARCH_PAGE= PREFIX + "java_search_page_context"; //$NON-NLS-1$
	public static final String NLS_SEARCH_PAGE= PREFIX + "nls_search_page_context"; //$NON-NLS-1$
	
	public static final String JAVA_EDITOR= PREFIX + "java_editor_context"; //$NON-NLS-1$
	
	// view parts
	public static final String TYPE_HIERARCHY_VIEW= PREFIX + "type_hierarchy_view_context"; //$NON-NLS-1$
	public static final String PACKAGES_VIEW= PREFIX + "package_view_context"; //$NON-NLS-1$
	public static final String PROJECTS_VIEW= PREFIX + "projects_view_context"; //$NON-NLS-1$
	public static final String PACKAGES_BROWSING_VIEW= PREFIX + "packages_browsing_view_context"; //$NON-NLS-1$
	public static final String TYPES_VIEW= PREFIX + "types_view_context"; //$NON-NLS-1$
	public static final String MEMBERS_VIEW= PREFIX + "members_view_context"; //$NON-NLS-1$

	// Preference/Property pages
	public static final String APPEARANCE_PREFERENCE_PAGE= 			PREFIX + "appearance_preference_page_context"; //$NON-NLS-1$
	public static final String SORT_ORDER_PREFERENCE_PAGE=		    PREFIX + "sort_order_preference_page_context"; //$NON-NLS-1$
	public static final String BUILD_PATH_PROPERTY_PAGE= 				PREFIX + "build_path_property_page_context"; //$NON-NLS-1$
	public static final String CP_VARIABLES_PREFERENCE_PAGE= 		PREFIX + "cp_variables_preference_page_context"; //$NON-NLS-1$
	public static final String CODEFORMATTER_PREFERENCE_PAGE= 	PREFIX + "codeformatter_preference_page_context"; //$NON-NLS-1$
	public static final String SOURCE_ATTACHMENT_PROPERTY_PAGE=	PREFIX + "source_attachment_property_page_context"; //$NON-NLS-1$

	public static final String CODE_MANIPULATION_PREFERENCE_PAGE= PREFIX + "code_manipulation_preference_context"; //$NON-NLS-1$
	public static final String ORGANIZE_IMPORTS_PREFERENCE_PAGE= PREFIX + "organizeimports_preference_page_context"; //$NON-NLS-1$
	public static final String JAVA_BASE_PREFERENCE_PAGE= PREFIX + "java_base_preference_page_context"; //$NON-NLS-1$
	public static final String REFACTORING_PREFERENCE_PAGE= PREFIX + "refactoring_preference_page_context"; //$NON-NLS-1$
	public static final String JAVA_EDITOR_PREFERENCE_PAGE= PREFIX + "java_editor_preference_page_context"; //$NON-NLS-1$
	public static final String COMPILER_PREFERENCE_PAGE= PREFIX + "compiler_preference_page_context"; //$NON-NLS-1$
	public static final String TEMPLATE_PREFERENCE_PAGE= PREFIX + "template_preference_page_context"; //$NON-NLS-1$
	public static final String JAVADOC_PREFERENCE_PAGE= PREFIX + "javadoc_preference_page_context"; //$NON-NLS-1$
	public static final String NEW_JAVA_PROJECT_PREFERENCE_PAGE= PREFIX + "new_java_project_preference_page_context"; //$NON-NLS-1$
	public static final String JAVADOC_CONFIGURATION_PROPERTY_PAGE= PREFIX + "new_java_project_preference_page_context"; //$NON-NLS-1$
	public static final String JAVA_ELEMENT_INFO_PAGE= PREFIX + "java_element_info_page_context"; //$NON-NLS-1$
		
	// Wizard pages
	public static final String NEW_JAVAPROJECT_WIZARD_PAGE= PREFIX + "new_javaproject_wizard_page_context"; //$NON-NLS-1$
	public static final String NEW_SNIPPET_WIZARD_PAGE= PREFIX + "new_snippet_wizard_page_context"; //$NON-NLS-1$
	public static final String NEW_PACKAGE_WIZARD_PAGE= PREFIX + "new_package_wizard_page_context"; //$NON-NLS-1$
	public static final String NEW_CLASS_WIZARD_PAGE= PREFIX + "new_class_wizard_page_context"; //$NON-NLS-1$
	public static final String NEW_INTERFACE_WIZARD_PAGE= PREFIX + "new_interface_wizard_page_context"; //$NON-NLS-1$
	public static final String NEW_PACKAGEROOT_WIZARD_PAGE= PREFIX + "new_packageroot_wizard_page_context"; //$NON-NLS-1$
	public static final String JARPACKAGER_WIZARD_PAGE= PREFIX + "jar_packager_wizard_page_context"; //$NON-NLS-1$
	public static final String JARMANIFEST_WIZARD_PAGE= PREFIX + "jar_manifest_wizard_page_context"; //$NON-NLS-1$
	public static final String JAROPTIONS_WIZARD_PAGE= PREFIX + "jar_options_wizard_page_context"; //$NON-NLS-1$
	public static final String JAVA_WORKING_SET_PAGE= PREFIX + "java_working_set_page_context"; //$NON-NLS-1$
	public static final String CLASSPATH_CONTAINER_DEFAULT_PAGE= PREFIX + "classpath_container_default_page_context"; //$NON-NLS-1$
	public static final String JAVADOC_STANDARD_PAGE= PREFIX + "javadoc_standard_page_context"; //$NON-NLS-1$
	public static final String JAVADOC_SPECIFICS_PAGE= PREFIX + "javadoc_specifics_page_context"; //$NON-NLS-1$
	public static final String JAVADOC_TREE_PAGE= PREFIX + "javadoc_tree_page_context"; //$NON-NLS-1$
	
	// same help for all refactoring preview pages
	public static final String REFACTORING_PREVIEW_WIZARD_PAGE= 				PREFIX + "refactoring_preview_wizard_page_context"; //$NON-NLS-1$
	
	public static final String MOVE_CU_ERROR_WIZARD_PAGE= 						PREFIX + "move_cu_error_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_PARAMS_WIZARD_PAGE= 						PREFIX + "rename_params_wizard_page"; //$NON-NLS-1$
	public static final String RENAME_PARAMS_ERROR_WIZARD_PAGE= 			PREFIX + "rename_params_error_wizard_page"; //$NON-NLS-1$
	public static final String EXTERNALIZE_WIZARD_KEYVALUE_PAGE= 				PREFIX + "externalize_wizard_keyvalue_page_context"; //$NON-NLS-1$
	public static final String EXTERNALIZE_WIZARD_PROPERTIES_FILE_PAGE= 	PREFIX + "externalize_wizard_properties_file_page_context"; //$NON-NLS-1$
	public static final String EXTERNALIZE_ERROR_WIZARD_PAGE= 					PREFIX + "externalize_error_wizard_page_context"; //$NON-NLS-1$
	public static final String EXTRACT_INTERFACE_WIZARD_PAGE= 					PREFIX + "extract_interface_temp_page_context"; //$NON-NLS-1$
	public static final String EXTRACT_INTERFACE_ERROR_WIZARD_PAGE= 		PREFIX + "extract_interface_error_wizard_page_context"; //$NON-NLS-1$
	public static final String EXTRACT_METHOD_WIZARD_PAGE= 					PREFIX + "extract_method_wizard_page_context"; //$NON-NLS-1$
	public static final String EXTRACT_METHOD_ERROR_WIZARD_PAGE= 			PREFIX + "extract_method_error_wizard_page_context"; //$NON-NLS-1$
	public static final String EXTRACT_TEMP_WIZARD_PAGE= 						PREFIX + "extract_temp_page_context"; //$NON-NLS-1$
	public static final String EXTRACT_TEMP_ERROR_WIZARD_PAGE= 				PREFIX + "extract_temp_error_wizard_page_context"; //$NON-NLS-1$
	public static final String EXTRACT_CONSTANT_WIZARD_PAGE= 						PREFIX + "extract_constant_page_context"; //$NON-NLS-1$
	public static final String EXTRACT_CONSTANT_ERROR_WIZARD_PAGE= 				PREFIX + "extract_constant_error_wizard_page_context"; //$NON-NLS-1$
	public static final String MODIFY_PARAMETERS_WIZARD_PAGE= 				PREFIX + "modify_parameters_wizard_page_context"; //$NON-NLS-1$
	public static final String MODIFY_PARAMETERS_ERROR_WIZARD_PAGE= 		PREFIX + "modify_parameters_error_wizard_page_context"; //$NON-NLS-1$
	public static final String MOVE_MEMBERS_WIZARD_PAGE= 						PREFIX + "move_members_wizard_page_context"; //$NON-NLS-1$
	public static final String MOVE_MEMBERS_ERROR_WIZARD_PAGE= 				PREFIX + "move_members_error_error_wizard_page_context"; //$NON-NLS-1$
	public static final String MOVE_INNER_TO_TOP_WIZARD_PAGE= 				PREFIX + "move_inner_to_top_wizard_page_context"; //$NON-NLS-1$
	public static final String MOVE_INNER_TO_TOP_ERROR_WIZARD_PAGE= 		PREFIX + "move_inner_to_top_error_error_wizard_page_context"; //$NON-NLS-1$
	public static final String PULL_UP_WIZARD_PAGE= 									PREFIX + "pull_up_wizard_page_context"; //$NON-NLS-1$
	public static final String PULL_UP_ERROR_WIZARD_PAGE= 						PREFIX + "pull_up_error_error_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_PACKAGE_WIZARD_PAGE= 						PREFIX + "rename_package_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_PACKAGE_ERROR_WIZARD_PAGE= 			PREFIX + "rename_package_error_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_TEMP_WIZARD_PAGE=  							PREFIX + "rename_local_variable_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_TEMP_ERROR_WIZARD_PAGE=  				PREFIX + "rename_local_variable_error_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_CU_WIZARD_PAGE= 								PREFIX + "rename_cu_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_CU_ERROR_WIZARD_PAGE= 					PREFIX + "rename_cu_error_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_METHOD_WIZARD_PAGE= 						PREFIX + "rename_method_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_METHOD_ERROR_WIZARD_PAGE= 			PREFIX + "rename_method_error_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_TYPE_WIZARD_PAGE= 							PREFIX + "rename_type_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_TYPE_ERROR_WIZARD_PAGE= 				PREFIX + "rename_type_error_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_FIELD_WIZARD_PAGE= 							PREFIX + "rename_field_wizard_page_context"; //$NON-NLS-1$
	public static final String RENAME_FIELD_ERROR_WIZARD_PAGE= 				PREFIX + "rename_field_error_wizard_page_context"; //$NON-NLS-1$
	public static final String SEF_WIZARD_PAGE= 										PREFIX + "self_encapsulate_field_wizard_page_context"; //$NON-NLS-1$
	public static final String SEF_ERROR_WIZARD_PAGE= 								PREFIX + "self_encapsulate_field_error_wizard_page_context"; //$NON-NLS-1$
	public static final String USE_SUPERTYPE_WIZARD_PAGE= 						PREFIX + "use_supertype_wizard_page_context"; //$NON-NLS-1$
	public static final String USE_SUPERTYPE_ERROR_WIZARD_PAGE= 				PREFIX + "use_supertype_error_wizard_page_context"; //$NON-NLS-1$
	public static final String INLINE_METHOD_WIZARD_PAGE=				PREFIX + "inline_method_wizard_page_context"; //$NON-NLS-1$
	public static final String INLINE_METHOD_ERROR_WIZARD_PAGE=				PREFIX + "inline_method_error_wizard_page_context"; //$NON-NLS-1$
	
	// reused ui-blocks
	public static final String BUILD_PATH_BLOCK= PREFIX + "build_paths_context"; //$NON-NLS-1$
	public static final String SOURCE_ATTACHMENT_BLOCK= PREFIX + "source_attachment_context"; //$NON-NLS-1$
	
	// Custom Filters
	public static final String CUSTOM_FILTERS_DIALOG= PREFIX + "open_custom_filters_dialog_context"; //$NON-NLS-1$
}