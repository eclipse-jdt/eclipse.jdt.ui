/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui;import org.eclipse.jdt.ui.JavaUI;


/**
 * Help context ids for the java ui.
 * <p>
 * This interface contains constants only; it is not intended to be implemented
 * or extended.
 * </p>
 * 
 */
public interface IJavaHelpContextIds {
	public static final String PREFIX= JavaUI.ID_PLUGIN + ".";

	// Actions
	public static final String GETTERSETTER_ACTION= PREFIX + "getter_setter_action_context";
	public static final String ADD_METHODBREAKPOINT_ACTION= PREFIX + "add_methodbreakpoint_action_context";
	public static final String ADD_METHODSTUB_ACTION= PREFIX + "add_methodstub_action_context";
	public static final String ADD_UNIMPLEMENTED_METHODS_ACTION= PREFIX + "add_unimplemented_methods_action_context";
	public static final String OPEN_IMPORTDECL_ACTION= PREFIX + "open_import_decl_action_context";
	public static final String SHOW_IN_PACKAGEVIEW_ACTION= PREFIX + "show_in_packageview_action_context";
	public static final String SHOW_IN_HIERARCHYVIEW_ACTION= PREFIX + "show_in_hierarchyview_action_context";

	// Dialogs
	public static final String MAINTYPE_SELECTION_DIALOG= PREFIX + "maintype_selection_dialog_context";
	public static final String OPEN_TYPE_DIALOG= PREFIX + "opem_type_dialog_context";
	public static final String EDIT_JRE_DIALOG= PREFIX + "edit_jre_dialog_context";
	public static final String SOURCE_ATTACHMENT_DIALOG= PREFIX + "source_attachment_dialog_context";
	public static final String VARIABLE_SELECTION_DIALOG= PREFIX + "variable_selection_dialog_context";
	public static final String VARIABLE_CREATION_DIALOG= PREFIX + "variable_creation_dialog_context";

	// Editors

	// Preference/Property pages
	public static final String BUILD_PATH_PROPERTY_PAGE= PREFIX + "build_path_property_page_context";
	public static final String CP_VARIABLES_PREFERENCE_PAGE= PREFIX + "cp_variables_preference_page_context";
	public static final String CODEFORMATTER_PREFERENCE_PAGE= PREFIX + "codeformatter_preference_page_context";
	public static final String SOURCE_ATTACHMENT_PROPERTY_PAGE= PREFIX + "source_attachment_property_page_context";
	public static final String JRE_PREFERENCE_PAGE= PREFIX + "jre_preference_page_context";
	public static final String SOURCE_LOOKUP_PROPERTY_PAGE= PREFIX + "source_lookup_property_page_context";
	

	// Wizard pages
	public static final String NEW_JAVAPROJECT_WIZARD_PAGE= PREFIX + "new_javaproject_wizard_page_context";
	public static final String NEW_SNIPPET_WIZARD_PAGE= PREFIX + "new_snippet_wizard_page_context";
	public static final String NEW_PACKAGE_WIZARD_PAGE= PREFIX + "new_package_wizard_page_context";
	public static final String NEW_CLASS_WIZARD_PAGE= PREFIX + "new_class_wizard_page_context";
	public static final String NEW_INTERFACE_WIZARD_PAGE= PREFIX + "new_interface_wizard_page_context";
	public static final String NEW_PACKAGEROOT_WIZARD_PAGE= PREFIX + "new_packageroot_wizard_page_context";
	public static final String JAVA_APPLICATION_WIZARD_PAGE= PREFIX + "java_application_page_context";
	public static final String JDI_ATTACH_LAUNCHER_WIZARD_PAGE= PREFIX + "jdi_attach_launcher_page_context";
	public static final String JARPACKAGER_WIZARD_PAGE= PREFIX + "jar_packager_wizard_page_context";
	public static final String JARMANIFEST_WIZARD_PAGE= PREFIX + "jar_manifest_wizard_page_context";
	public static final String JAROPTIONS_WIZARD_PAGE= PREFIX + "jar_options_wizard_page_context";
	
	// reused ui-blocks
	public static final String BUILD_PATH_BLOCK= PREFIX + "build_paths_context";
	public static final String SOURCE_ATTACHMENT_BLOCK= PREFIX + "source_attachment_context";
	
	
}