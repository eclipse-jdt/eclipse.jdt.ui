/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 83258 [jar exporter] Deploy java application as executable jar
 *     Frits Jalvingh <jal@etc.to> - Contribution for Bug 459831 - [launching] Support attaching external annotations to a JRE container
 *******************************************************************************/
package org.eclipse.jdt.internal.ui;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.function.IntFunction;

import org.osgi.framework.Bundle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;


/**
 * Bundle of most images used by the Java plug-in.
 */
public class JavaPluginImages {

	public static final IPath ICONS_PATH= new Path("$nl$/icons/full"); //$NON-NLS-1$

	private static final String NAME_PREFIX= "org.eclipse.jdt.ui."; //$NON-NLS-1$
	private static final int    NAME_PREFIX_LENGTH= NAME_PREFIX.length();

	// The plug-in registry
	private static ImageRegistry fgImageRegistry= null;
	private static HashMap<String, ImageDescriptor> fgAvoidSWTErrorMap= null;

	private static final String T_OBJ= "obj16"; 		//$NON-NLS-1$
	private static final String T_OVR= "ovr16"; 		//$NON-NLS-1$
	private static final String T_WIZBAN= "wizban"; 	//$NON-NLS-1$
	private static final String T_ELCL= "elcl16"; 	//$NON-NLS-1$
	private static final String T_ETOOL= "etool16"; 	//$NON-NLS-1$
	private static final String T_EVIEW= "eview16"; //$NON-NLS-1$

	/*
	 * Keys for images available from the Java-UI plug-in image registry.
	 * Caveat (bug 465521): Can't use *.png in all existing keys, since some of them are made API via org.eclipse.jdt.ui.ISharedImages!
	 */
	public static final String IMG_MISC_PUBLIC= NAME_PREFIX + "methpub_obj.gif"; 			//$NON-NLS-1$
	public static final String IMG_MISC_PROTECTED= NAME_PREFIX + "methpro_obj.gif"; 		//$NON-NLS-1$
	public static final String IMG_MISC_PRIVATE= NAME_PREFIX + "methpri_obj.gif"; 		//$NON-NLS-1$
	public static final String IMG_MISC_DEFAULT= NAME_PREFIX + "methdef_obj.gif"; 		//$NON-NLS-1$

	public static final String IMG_FIELD_PUBLIC= NAME_PREFIX + "field_public_obj.gif"; 			//$NON-NLS-1$
	public static final String IMG_FIELD_PROTECTED= NAME_PREFIX + "field_protected_obj.gif"; 		//$NON-NLS-1$
	public static final String IMG_FIELD_PRIVATE= NAME_PREFIX + "field_private_obj.gif"; 		//$NON-NLS-1$
	public static final String IMG_FIELD_DEFAULT= NAME_PREFIX + "field_default_obj.gif"; 		//$NON-NLS-1$

	public static final String IMG_ELCL_VIEW_MENU= NAME_PREFIX + T_ELCL + "view_menu.gif"; //$NON-NLS-1$

	public static final String IMG_OBJS_GHOST= NAME_PREFIX + "ghost.gif"; 				//$NON-NLS-1$
	public static final String IMG_OBJS_SEARCH_TSK= NAME_PREFIX + "search_tsk.gif"; 		//$NON-NLS-1$
	public static final String IMG_OBJS_PACKDECL= NAME_PREFIX + "packd_obj.gif"; 			//$NON-NLS-1$
	public static final String IMG_OBJS_IMPDECL= NAME_PREFIX + "imp_obj.gif"; 			//$NON-NLS-1$
	public static final String IMG_OBJS_IMPCONT= NAME_PREFIX + "impc_obj.gif"; 			//$NON-NLS-1$
	public static final String IMG_OBJS_JSEARCH= NAME_PREFIX + "jsearch_obj.gif"; 		//$NON-NLS-1$
	public static final String IMG_OBJS_SEARCH_DECL= NAME_PREFIX + "search_decl_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_SEARCH_REF= NAME_PREFIX + "search_ref_obj.gif"; 	//$NON-NLS-1$
	public static final String IMG_OBJS_CLASS= NAME_PREFIX + "class_obj.gif"; 			//$NON-NLS-1$
	public static final String IMG_OBJS_CLASSALT= NAME_PREFIX + "classfo_obj.gif"; 			//$NON-NLS-1$
	public static final String IMG_OBJS_CLASS_DEFAULT= NAME_PREFIX + "class_default_obj.gif"; 			//$NON-NLS-1$

	public static final String IMG_OBJS_INNER_CLASS_PUBLIC= NAME_PREFIX + "innerclass_public_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_INNER_CLASS_DEFAULT= NAME_PREFIX + "innerclass_default_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_INNER_CLASS_PROTECTED= NAME_PREFIX + "innerclass_protected_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_INNER_CLASS_PRIVATE= NAME_PREFIX + "innerclass_private_obj.gif"; //$NON-NLS-1$

	public static final String IMG_OBJS_INTERFACE= NAME_PREFIX + "int_obj.gif"; 			//$NON-NLS-1$
	public static final String IMG_OBJS_INTERFACEALT= NAME_PREFIX + "intf_obj.gif"; 			//$NON-NLS-1$
	public static final String IMG_OBJS_INTERFACE_DEFAULT= NAME_PREFIX + "int_default_obj.gif"; 		//$NON-NLS-1$

	public static final String IMG_OBJS_INNER_INTERFACE_PUBLIC= NAME_PREFIX + "innerinterface_public_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_INNER_INTERFACE_DEFAULT= NAME_PREFIX + "innerinterface_default_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_INNER_INTERFACE_PROTECTED= NAME_PREFIX + "innerinterface_protected_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_INNER_INTERFACE_PRIVATE= NAME_PREFIX + "innerinterface_private_obj.gif"; //$NON-NLS-1$

	public static final String IMG_OBJS_ANNOTATION= NAME_PREFIX + "annotation_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_ANNOTATION_DEFAULT= NAME_PREFIX + "annotation_default_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_ANNOTATION_PROTECTED= NAME_PREFIX + "annotation_protected_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_ANNOTATION_PRIVATE= NAME_PREFIX + "annotation_private_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_ANNOTATION_ALT= NAME_PREFIX + "annotation_alt_obj.gif"; //$NON-NLS-1$

	public static final String IMG_OBJS_ENUM= NAME_PREFIX + "enum_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_ENUM_DEFAULT= NAME_PREFIX + "enum_default_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_ENUM_PROTECTED= NAME_PREFIX + "enum_protected_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_ENUM_PRIVATE= NAME_PREFIX + "enum_private_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_ENUM_ALT= NAME_PREFIX + "enum_alt_obj.gif"; //$NON-NLS-1$

	public static final String IMG_OBJS_RECORD= NAME_PREFIX + "record_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_RECORD_DEFAULT= NAME_PREFIX + "record_default_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_RECORD_PROTECTED= NAME_PREFIX + "record_protected_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_RECORD_PRIVATE= NAME_PREFIX + "record_private_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_RECORD_ALT= NAME_PREFIX + "record_alt_obj.gif"; //$NON-NLS-1$

	public static final String IMG_OBJS_CUNIT= NAME_PREFIX + "jcu_obj.gif"; 				//$NON-NLS-1$
	public static final String IMG_OBJS_CUNIT_RESOURCE= NAME_PREFIX + "jcu_resource_obj.gif"; 				//$NON-NLS-1$
	public static final String IMG_OBJS_CFILE= NAME_PREFIX + "classf_obj.gif";  			//$NON-NLS-1$
	public static final String IMG_OBJS_CFILECLASS= NAME_PREFIX + "class_obj.gif";  		//$NON-NLS-1$
	public static final String IMG_OBJS_CFILEINT= NAME_PREFIX + "int_obj.gif";  			//$NON-NLS-1$
	public static final String IMG_OBJS_LOGICAL_PACKAGE= NAME_PREFIX + "logical_package_obj.gif";//$NON-NLS-1$
	public static final String IMG_OBJS_EMPTY_LOGICAL_PACKAGE= NAME_PREFIX + "empty_logical_package_obj.gif";//$NON-NLS-1$
	public static final String IMG_OBJS_PACKAGE= NAME_PREFIX + "package_obj.gif"; 		//$NON-NLS-1$
	public static final String IMG_OBJS_EMPTY_PACK_RESOURCE= NAME_PREFIX + "empty_pack_fldr_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_EMPTY_PACKAGE= NAME_PREFIX + "empty_pack_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_PACKFRAG_ROOT= NAME_PREFIX + "packagefolder_obj.gif"; //$NON-NLS-1$
	// workspace folder as classpath entry that is marked as only visible for tests
	public static final String IMG_OBJS_PACKFRAG_ROOT_TEST= NAME_PREFIX + "packagefolder_test_obj.svg"; //$NON-NLS-1$
	// source folder that is marked to contain test sources
	public static final String IMG_OBJS_PACKFRAG_ROOT_TESTSOURCES= NAME_PREFIX + "packagefolder_testsources_obj.svg"; //$NON-NLS-1$
	public static final String IMG_OBJS_JAR= NAME_PREFIX + "jar_obj.gif"; 				//$NON-NLS-1$
	public static final String IMG_OBJS_JAR_TEST= NAME_PREFIX + "jar_test_obj.svg"; 				//$NON-NLS-1$
	public static final String IMG_OBJS_EXTJAR= NAME_PREFIX + "jar_l_obj.gif"; 			//$NON-NLS-1$
	public static final String IMG_OBJS_EXTJAR_TEST= NAME_PREFIX + "jar_l_test_obj.gif"; 			//$NON-NLS-1$
	public static final String IMG_OBJS_JAR_WSRC= NAME_PREFIX + "jar_src_obj.gif"; 		//$NON-NLS-1$
	public static final String IMG_OBJS_JAR_WSRC_TEST= NAME_PREFIX + "jar_src_test_obj.gif"; 		//$NON-NLS-1$
	public static final String IMG_OBJS_EXTJAR_WSRC= NAME_PREFIX + "jar_lsrc_obj.gif";	//$NON-NLS-1$
	public static final String IMG_OBJS_EXTJAR_WSRC_TEST= NAME_PREFIX + "jar_lsrc_test_obj.gif";	//$NON-NLS-1$
	public static final String IMG_OBJS_CLASSFOLDER= NAME_PREFIX + "cf_obj.gif"; 			//$NON-NLS-1$
	public static final String IMG_OBJS_CLASSFOLDER_TEST= NAME_PREFIX + "cf_test_obj.gif"; 			//$NON-NLS-1$
	public static final String IMG_OBJS_CLASSFOLDER_WSRC= NAME_PREFIX + "cf_src_obj.gif"; 		//$NON-NLS-1$
	public static final String IMG_OBJS_CLASSFOLDER_WSRC_TEST= NAME_PREFIX + "cf_src_test_obj.gif"; 		//$NON-NLS-1$
	public static final String IMG_OBJS_PROJECT_TEST= NAME_PREFIX + "project_test_obj.svg"; //$NON-NLS-1$
	public static final String IMG_OBJS_TEST_CASE= NAME_PREFIX + "new_testcase.svg"; //$NON-NLS-1$

	public static final String IMG_OBJS_ENV_VAR= NAME_PREFIX + "envvar_obj.gif"; 			//$NON-NLS-1$
	public static final String IMG_OBJS_ENV_VAR_TEST= NAME_PREFIX + "envvar_test_obj.gif"; 			//$NON-NLS-1$
	public static final String IMG_OBJS_JAVA_MODEL= NAME_PREFIX + "java_model_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_MODULE= NAME_PREFIX + "module_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_ERROR_ALT= NAME_PREFIX + "error_alt_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_WARNING_ALT= NAME_PREFIX + "warning_alt_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_INFO_ALT= NAME_PREFIX + "info_alt_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_UNKNOWN= NAME_PREFIX + "unknown_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_LOCAL_VARIABLE= NAME_PREFIX + "localvariable_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_PROJECT_SETTINGS= NAME_PREFIX + "settings_obj.gif"; //$NON-NLS-1$

	public static final String IMG_OBJS_LIBRARY= NAME_PREFIX + "library_obj.gif"; 		//$NON-NLS-1$
	public static final String IMG_OBJS_LIBRARY_TEST= NAME_PREFIX + "library_test_obj.svg"; 		//$NON-NLS-1$

	public static final String IMG_OBJS_JAVADOCTAG= NAME_PREFIX + "jdoc_tag_obj.gif"; 	//$NON-NLS-1$
	public static final String IMG_OBJS_EXTERNAL_ANNOTATIONS = NAME_PREFIX + "external_annotation_location_attrib.gif"; 	//$NON-NLS-1$
	public static final String IMG_OBJS_HTMLTAG= NAME_PREFIX + "html_tag_obj.gif"; 		//$NON-NLS-1$

	public static final String IMG_OBJS_TEMPLATE= NAME_PREFIX + "template_obj.gif"; 		//$NON-NLS-1$
	public static final String IMG_OBJS_SWT_TEMPLATE= NAME_PREFIX + "template_swt_obj.gif"; 		//$NON-NLS-1$
	public static final String IMG_OBJS_TYPEVARIABLE= NAME_PREFIX + "typevariable_obj.gif"; 		//$NON-NLS-1$

	public static final String IMG_OBJS_EXCEPTION= NAME_PREFIX + "jexception_obj.gif"; 	//$NON-NLS-1$
	public static final String IMG_OBJS_ERROR= NAME_PREFIX + "jrtexception_obj.gif"; 		//$NON-NLS-1$

	public static final String IMG_OBJS_BREAKPOINT_INSTALLED= NAME_PREFIX + "brkpi_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_QUICK_ASSIST= NAME_PREFIX + "quickassist_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_FIXABLE_WARNING= NAME_PREFIX + "quickfix_warning_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_FIXABLE_ERROR= NAME_PREFIX + "quickfix_error_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_FIXABLE_INFO= NAME_PREFIX + "quickfix_info_obj.gif"; //$NON-NLS-1$

	public static final String IMG_OBJS_ACCESSRULES_ATTRIB= NAME_PREFIX + "access_restriction_attrib.gif"; //$NON-NLS-1$

	public static final String IMG_OBJS_REFACTORING_FATAL= NAME_PREFIX + "fatalerror_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_REFACTORING_ERROR= NAME_PREFIX + "error_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_REFACTORING_WARNING= NAME_PREFIX + "warning_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_REFACTORING_INFO= NAME_PREFIX + "info_obj.gif"; 	//$NON-NLS-1$

	public static final String IMG_OBJS_NLS_TRANSLATE= NAME_PREFIX + "translate.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_NLS_NEVER_TRANSLATE= NAME_PREFIX + "never_translate.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_NLS_SKIP= NAME_PREFIX + "skip.gif"; //$NON-NLS-1$

	public static final String IMG_OBJS_SEARCH_READACCESS= NAME_PREFIX + "occ_read.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_SEARCH_WRITEACCESS= NAME_PREFIX + "occ_write.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_SEARCH_OCCURRENCE= NAME_PREFIX + "occ_match.gif"; //$NON-NLS-1$

	public static final String IMG_OBJS_HELP= NAME_PREFIX + "help.gif"; //$NON-NLS-1$

	public static final String IMG_CONFIGURE_PROBLEM_SEVERITIES= NAME_PREFIX + "configure_problem_severity.svg"; //$NON-NLS-1$

	public static final String IMG_BLANK= NAME_PREFIX + "blank.svg"; //$NON-NLS-1$

	/*
	 * Set of predefined Image Descriptors.
	 */
	public static final ImageDescriptor DESC_VIEW_ERRORWARNING_TAB= createUnManaged(T_EVIEW, "errorwarning_tab.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_VIEW_CLASSFILEGENERATION_TAB= createUnManaged(T_EVIEW, "classfilegeneration_tab.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_VIEW_JDKCOMPLIANCE_TAB= createUnManaged(T_EVIEW, "jdkcompliance_tab.svg"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_ELCL_FILTER= createUnManaged(T_ELCL, "filter_ps.svg"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_ELCL_CODE_ASSIST= createUnManaged(T_ELCL, "metharg_obj.svg"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_ELCL_VIEW_MENU= createManaged(T_ELCL, "view_menu.svg", IMG_ELCL_VIEW_MENU); //$NON-NLS-1$

	public static final ImageDescriptor DESC_MISC_PUBLIC= createManagedFromKey(T_OBJ, IMG_MISC_PUBLIC);
	public static final ImageDescriptor DESC_MISC_PROTECTED= createManagedFromKey(T_OBJ, IMG_MISC_PROTECTED);
	public static final ImageDescriptor DESC_MISC_PRIVATE= createManagedFromKey(T_OBJ, IMG_MISC_PRIVATE);
	public static final ImageDescriptor DESC_MISC_DEFAULT= createManagedFromKey(T_OBJ, IMG_MISC_DEFAULT);

	public static final ImageDescriptor DESC_FIELD_PUBLIC= createManagedFromKey(T_OBJ, IMG_FIELD_PUBLIC);
	public static final ImageDescriptor DESC_FIELD_PROTECTED= createManagedFromKey(T_OBJ, IMG_FIELD_PROTECTED);
	public static final ImageDescriptor DESC_FIELD_PRIVATE= createManagedFromKey(T_OBJ, IMG_FIELD_PRIVATE);
	public static final ImageDescriptor DESC_FIELD_DEFAULT= createManagedFromKey(T_OBJ, IMG_FIELD_DEFAULT);

	public static final ImageDescriptor DESC_MENU_SHIFT_RIGHT= createUnManaged(T_ETOOL, "shift_r_edit.svg"); 	//$NON-NLS-1$
	public static final ImageDescriptor DESC_MENU_SHIFT_LEFT= createUnManaged(T_ETOOL, "shift_l_edit.svg"); 	//$NON-NLS-1$
	public static final ImageDescriptor DESC_CLASSPATH_ROOT= createUnManaged(T_ETOOL, "classpath_obj.svg"); 	//$NON-NLS-1$

	public static final ImageDescriptor DESC_BUTTON_MOVE_UP= createUnManaged(T_ETOOL, "move_up.png"); 	//$NON-NLS-1$

	public static final ImageDescriptor DESC_OBJS_GHOST= createManagedFromKey(T_OBJ, IMG_OBJS_GHOST);
	public static final ImageDescriptor DESC_OBJS_PACKDECL= createManagedFromKey(T_OBJ, IMG_OBJS_PACKDECL);
	public static final ImageDescriptor DESC_OBJS_IMPDECL= createManagedFromKey(T_OBJ, IMG_OBJS_IMPDECL);
	public static final ImageDescriptor DESC_OBJS_IMPCONT= createManagedFromKey(T_OBJ, IMG_OBJS_IMPCONT);
	public static final ImageDescriptor DESC_OBJS_JSEARCH= createManagedFromKey(T_OBJ, IMG_OBJS_JSEARCH);
	public static final ImageDescriptor DESC_OBJS_SEARCH_DECL= createManagedFromKey(T_OBJ, IMG_OBJS_SEARCH_DECL);
	public static final ImageDescriptor DESC_OBJS_SEARCH_REF= createManagedFromKey(T_OBJ, IMG_OBJS_SEARCH_REF);
	public static final ImageDescriptor DESC_OBJS_CUNIT= createManagedFromKey(T_OBJ, IMG_OBJS_CUNIT);
	public static final ImageDescriptor DESC_OBJS_CUNIT_RESOURCE= createManagedFromKey(T_OBJ, IMG_OBJS_CUNIT_RESOURCE);
	public static final ImageDescriptor DESC_OBJS_CFILE= createManagedFromKey(T_OBJ, IMG_OBJS_CFILE);
	public static final ImageDescriptor DESC_OBJS_CFILECLASS= createManagedFromKey(T_OBJ, IMG_OBJS_CFILECLASS);
	public static final ImageDescriptor DESC_ELCL_CLEAR= createUnManaged(T_ELCL, "clear_co.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OBJS_CFILEINT= createManagedFromKey(T_OBJ, IMG_OBJS_CFILEINT);
	public static final ImageDescriptor DESC_OBJS_PACKAGE= createManagedFromKey(T_OBJ, IMG_OBJS_PACKAGE);
	public static final ImageDescriptor DESC_OBJS_EMPTY_LOGICAL_PACKAGE= createManagedFromKey(T_OBJ, IMG_OBJS_EMPTY_LOGICAL_PACKAGE);
	public static final ImageDescriptor DESC_OBJS_LOGICAL_PACKAGE= createManagedFromKey(T_OBJ, IMG_OBJS_LOGICAL_PACKAGE);
	public static final ImageDescriptor DESC_OBJS_EMPTY_PACKAGE_RESOURCES= createManagedFromKey(T_OBJ, IMG_OBJS_EMPTY_PACK_RESOURCE);
	public static final ImageDescriptor DESC_OBJS_EMPTY_PACKAGE= createManagedFromKey(T_OBJ, IMG_OBJS_EMPTY_PACKAGE);
	public static final ImageDescriptor DESC_OBJS_PACKFRAG_ROOT= createManagedFromKey(T_OBJ, IMG_OBJS_PACKFRAG_ROOT);
	public static final ImageDescriptor DESC_OBJS_PACKFRAG_ROOT_TEST= createManagedFromKey(T_OBJ, IMG_OBJS_PACKFRAG_ROOT_TEST);
	public static final ImageDescriptor DESC_OBJS_PACKFRAG_ROOT_TESTSOURCES= createManagedFromKey(T_OBJ, IMG_OBJS_PACKFRAG_ROOT_TESTSOURCES);
	public static final ImageDescriptor DESC_OBJS_PROJECT_SETTINGS= createManagedFromKey(T_OBJ, IMG_OBJS_PROJECT_SETTINGS);
	public static final ImageDescriptor DESC_OBJS_PROJECT_TEST= createManagedFromKey(T_OBJ, IMG_OBJS_PROJECT_TEST);
	public static final ImageDescriptor DESC_OBJS_TEST_CASE= createManagedFromKey(T_ETOOL, IMG_OBJS_TEST_CASE);


	public static final ImageDescriptor DESC_OBJS_JAVA_MODEL= createManagedFromKey(T_OBJ, IMG_OBJS_JAVA_MODEL);
	public static final ImageDescriptor DESC_OBJS_MODULE= createManagedFromKey(T_OBJ, IMG_OBJS_MODULE);

	public static final ImageDescriptor DESC_OBJS_CLASS= createManagedFromKey(T_OBJ, IMG_OBJS_CLASS);
	public static final ImageDescriptor DESC_OBJS_CLASS_DEFAULT= createManagedFromKey(T_OBJ, IMG_OBJS_CLASS_DEFAULT);

	public static final ImageDescriptor DESC_OBJS_INNER_CLASS_PUBLIC= createManagedFromKey(T_OBJ, IMG_OBJS_INNER_CLASS_PUBLIC);
	public static final ImageDescriptor DESC_OBJS_INNER_CLASS_DEFAULT= createManagedFromKey(T_OBJ, IMG_OBJS_INNER_CLASS_DEFAULT);
	public static final ImageDescriptor DESC_OBJS_INNER_CLASS_PROTECTED= createManagedFromKey(T_OBJ, IMG_OBJS_INNER_CLASS_PROTECTED);
	public static final ImageDescriptor DESC_OBJS_INNER_CLASS_PRIVATE= createManagedFromKey(T_OBJ, IMG_OBJS_INNER_CLASS_PRIVATE);

	public static final ImageDescriptor DESC_OBJS_CLASSALT= createManagedFromKey(T_OBJ, IMG_OBJS_CLASSALT);

	public static final ImageDescriptor DESC_OBJS_INTERFACE= createManagedFromKey(T_OBJ, IMG_OBJS_INTERFACE);
	public static final ImageDescriptor DESC_OBJS_INTERFACE_DEFAULT= createManagedFromKey(T_OBJ, IMG_OBJS_INTERFACE_DEFAULT);

	public static final ImageDescriptor DESC_OBJS_INNER_INTERFACE_PUBLIC= createManagedFromKey(T_OBJ, IMG_OBJS_INNER_INTERFACE_PUBLIC);
	public static final ImageDescriptor DESC_OBJS_INNER_INTERFACE_DEFAULT= createManagedFromKey(T_OBJ, IMG_OBJS_INNER_INTERFACE_DEFAULT);
	public static final ImageDescriptor DESC_OBJS_INNER_INTERFACE_PROTECTED= createManagedFromKey(T_OBJ, IMG_OBJS_INNER_INTERFACE_PROTECTED);
	public static final ImageDescriptor DESC_OBJS_INNER_INTERFACE_PRIVATE= createManagedFromKey(T_OBJ, IMG_OBJS_INNER_INTERFACE_PRIVATE);

	public static final ImageDescriptor DESC_OBJS_INTERFACEALT= createManagedFromKey(T_OBJ, IMG_OBJS_INTERFACEALT);

	public static final ImageDescriptor DESC_OBJS_ANNOTATION= createManagedFromKey(T_OBJ, IMG_OBJS_ANNOTATION);
	public static final ImageDescriptor DESC_OBJS_ANNOTATION_DEFAULT= createManagedFromKey(T_OBJ, IMG_OBJS_ANNOTATION_DEFAULT);
	public static final ImageDescriptor DESC_OBJS_ANNOTATION_PROTECTED= createManagedFromKey(T_OBJ, IMG_OBJS_ANNOTATION_PROTECTED);
	public static final ImageDescriptor DESC_OBJS_ANNOTATION_PRIVATE= createManagedFromKey(T_OBJ, IMG_OBJS_ANNOTATION_PRIVATE);
	public static final ImageDescriptor DESC_OBJS_ANNOTATION_ALT= createManagedFromKey(T_OBJ, IMG_OBJS_ANNOTATION_ALT);

	public static final ImageDescriptor DESC_OBJS_ENUM= createManagedFromKey(T_OBJ, IMG_OBJS_ENUM);
	public static final ImageDescriptor DESC_OBJS_ENUM_DEFAULT= createManagedFromKey(T_OBJ, IMG_OBJS_ENUM_DEFAULT);
	public static final ImageDescriptor DESC_OBJS_ENUM_PROTECTED= createManagedFromKey(T_OBJ, IMG_OBJS_ENUM_PROTECTED);
	public static final ImageDescriptor DESC_OBJS_ENUM_PRIVATE= createManagedFromKey(T_OBJ, IMG_OBJS_ENUM_PRIVATE);
	public static final ImageDescriptor DESC_OBJS_ENUM_ALT= createManagedFromKey(T_OBJ, IMG_OBJS_ENUM_ALT);

	public static final ImageDescriptor DESC_OBJS_RECORD= createManagedFromKey(T_OBJ, IMG_OBJS_RECORD);
	public static final ImageDescriptor DESC_OBJS_RECORD_DEFAULT= createManagedFromKey(T_OBJ, IMG_OBJS_RECORD_DEFAULT);
	public static final ImageDescriptor DESC_OBJS_RECORD_PROTECTED= createManagedFromKey(T_OBJ, IMG_OBJS_RECORD_PROTECTED);
	public static final ImageDescriptor DESC_OBJS_RECORD_PRIVATE= createManagedFromKey(T_OBJ, IMG_OBJS_RECORD_PRIVATE);
	public static final ImageDescriptor DESC_OBJS_RECORD_ALT= createManagedFromKey(T_OBJ, IMG_OBJS_RECORD_ALT);

	public static final ImageDescriptor DESC_OBJS_JAR= createManagedFromKey(T_OBJ, IMG_OBJS_JAR);
	public static final ImageDescriptor DESC_OBJS_JAR_TEST= createManagedFromKey(T_OBJ, IMG_OBJS_JAR_TEST);
	public static final ImageDescriptor DESC_OBJS_EXTJAR= createManagedFromKey(T_OBJ, IMG_OBJS_EXTJAR);
	public static final ImageDescriptor DESC_OBJS_EXTJAR_TEST= createManagedFromKey(T_OBJ, IMG_OBJS_EXTJAR_TEST);
	public static final ImageDescriptor DESC_OBJS_JAR_WSRC= createManagedFromKey(T_OBJ, IMG_OBJS_JAR_WSRC);
	public static final ImageDescriptor DESC_OBJS_JAR_WSRC_TEST= createManagedFromKey(T_OBJ, IMG_OBJS_JAR_WSRC_TEST);
	public static final ImageDescriptor DESC_OBJS_EXTJAR_WSRC= createManagedFromKey(T_OBJ, IMG_OBJS_EXTJAR_WSRC);
	public static final ImageDescriptor DESC_OBJS_EXTJAR_WSRC_TEST= createManagedFromKey(T_OBJ, IMG_OBJS_EXTJAR_WSRC_TEST);

	public static final ImageDescriptor DESC_OBJS_CLASSFOLDER= createManagedFromKey(T_OBJ, IMG_OBJS_CLASSFOLDER);
	public static final ImageDescriptor DESC_OBJS_CLASSFOLDER_TEST= createManagedFromKey(T_OBJ, IMG_OBJS_CLASSFOLDER_TEST);
	public static final ImageDescriptor DESC_OBJS_CLASSFOLDER_WSRC= createManagedFromKey(T_OBJ, IMG_OBJS_CLASSFOLDER_WSRC);
	public static final ImageDescriptor DESC_OBJS_CLASSFOLDER_WSRC_TEST= createManagedFromKey(T_OBJ, IMG_OBJS_CLASSFOLDER_WSRC_TEST);

	public static final ImageDescriptor DESC_OBJS_ENV_VAR= createManagedFromKey(T_OBJ, IMG_OBJS_ENV_VAR);
	public static final ImageDescriptor DESC_OBJS_ENV_VAR_TEST= createManagedFromKey(T_OBJ, IMG_OBJS_ENV_VAR_TEST);

	public static final ImageDescriptor DESC_OBJS_LIBRARY= createManagedFromKey(T_OBJ, IMG_OBJS_LIBRARY);
	public static final ImageDescriptor DESC_OBJS_LIBRARY_TEST= createManagedFromKey(T_OBJ, IMG_OBJS_LIBRARY_TEST);

	public static final ImageDescriptor DESC_OBJS_JAVADOCTAG= createManagedFromKey(T_OBJ, IMG_OBJS_JAVADOCTAG);
	public static final ImageDescriptor DESC_OBJS_HTMLTAG= createManagedFromKey(T_OBJ, IMG_OBJS_HTMLTAG);

	public static final ImageDescriptor DESC_OBJS_TEMPLATE= createManagedFromKey(T_OBJ, IMG_OBJS_TEMPLATE);
	public static final ImageDescriptor DESC_OBJS_SWT_TEMPLATE= createManagedFromKey(T_OBJ, IMG_OBJS_SWT_TEMPLATE);

	public static final ImageDescriptor DESC_OBJS_TYPEVARIABLE= createManagedFromKey(T_OBJ, IMG_OBJS_TYPEVARIABLE);

	public static final ImageDescriptor DESC_OBJS_EXCEPTION= createManagedFromKey(T_OBJ, IMG_OBJS_EXCEPTION);
	public static final ImageDescriptor DESC_OBJS_BREAKPOINT_INSTALLED= createManagedFromKey(T_OBJ, IMG_OBJS_BREAKPOINT_INSTALLED);
	public static final ImageDescriptor DESC_OBJS_ERROR= createManagedFromKey(T_OBJ, IMG_OBJS_ERROR);
	public static final ImageDescriptor DESC_OBJS_QUICK_ASSIST= createManagedFromKey(T_OBJ, IMG_OBJS_QUICK_ASSIST);
	public static final ImageDescriptor DESC_OBJS_FIXABLE_WARNING= createManagedFromKey(T_OBJ, IMG_OBJS_FIXABLE_WARNING);
	public static final ImageDescriptor DESC_OBJS_FIXABLE_ERROR= createManagedFromKey(T_OBJ, IMG_OBJS_FIXABLE_ERROR);
	public static final ImageDescriptor DESC_OBJS_FIXABLE_INFO= createManagedFromKey(T_OBJ, IMG_OBJS_FIXABLE_INFO);

	public static final ImageDescriptor DESC_OBJS_DEFAULT_CHANGE= createUnManaged(T_OBJ, "change.svg"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_OBJS_EXCLUSION_FILTER_ATTRIB= createUnManaged(T_OBJ, "exclusion_filter_attrib.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OBJS_INCLUSION_FILTER_ATTRIB= createUnManaged(T_OBJ, "inclusion_filter_attrib.svg"); //$NON-NLS-1$
    public static final ImageDescriptor DESC_OBJS_OUTPUT_FOLDER_ATTRIB= createUnManaged(T_OBJ, "output_folder_attrib.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OBJS_SOURCE_ATTACH_ATTRIB= createUnManaged(T_OBJ, "source_attach_attrib.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OBJS_JAVADOC_LOCATION_ATTRIB= createUnManaged(T_OBJ, "javadoc_location_attrib.svg"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_OBJS_MODULE_ATTRIB= createUnManaged(T_OBJ, "module_attrib.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OBJS_TEST_ATTRIB= createUnManaged(T_OBJ, "test_attrib.svg"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_OBJS_EXTERNAL_ANNOTATION_LOCATION_ATTRIB= createManagedFromKey(T_OBJ, IMG_OBJS_EXTERNAL_ANNOTATIONS);

	public static final ImageDescriptor DESC_OBJS_ACCESSRULES_ATTRIB= createManagedFromKey(T_OBJ, IMG_OBJS_ACCESSRULES_ATTRIB);
	public static final ImageDescriptor DESC_OBJS_NATIVE_LIB_PATH_ATTRIB= createUnManaged(T_OBJ, "native_lib_path_attrib.svg"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_OBJS_REFACTORING_FATAL= createManagedFromKey(T_OBJ, IMG_OBJS_REFACTORING_FATAL);
	public static final ImageDescriptor DESC_OBJS_REFACTORING_ERROR= createManagedFromKey(T_OBJ, IMG_OBJS_REFACTORING_ERROR);
	public static final ImageDescriptor DESC_OBJS_REFACTORING_WARNING= createManagedFromKey(T_OBJ, IMG_OBJS_REFACTORING_WARNING);
	public static final ImageDescriptor DESC_OBJS_REFACTORING_INFO= createManagedFromKey(T_OBJ, IMG_OBJS_REFACTORING_INFO);

	public static final ImageDescriptor DESC_OBJS_NLS_TRANSLATE= createManagedFromKey(T_OBJ, IMG_OBJS_NLS_TRANSLATE);
	public static final ImageDescriptor DESC_OBJS_NLS_NEVER_TRANSLATE= createManagedFromKey(T_OBJ, IMG_OBJS_NLS_NEVER_TRANSLATE);
	public static final ImageDescriptor DESC_OBJS_NLS_SKIP= createManagedFromKey(T_OBJ, IMG_OBJS_NLS_SKIP);

	public static final ImageDescriptor DESC_OBJS_UNKNOWN= createManagedFromKey(T_OBJ, IMG_OBJS_UNKNOWN);

	public static final ImageDescriptor DESC_OBJS_TYPE_SEPARATOR= createUnManaged(T_OBJ, "type_separator.svg");  //$NON-NLS-1$

	public static final ImageDescriptor DESC_OBJS_SEARCH_READACCESS= createManagedFromKey(T_OBJ, IMG_OBJS_SEARCH_READACCESS);
	public static final ImageDescriptor DESC_OBJS_SEARCH_WRITEACCESS= createManagedFromKey(T_OBJ, IMG_OBJS_SEARCH_WRITEACCESS);
	public static final ImageDescriptor DESC_OBJS_SEARCH_OCCURRENCE= createManagedFromKey(T_OBJ, IMG_OBJS_SEARCH_OCCURRENCE);

	public static final ImageDescriptor DESC_OBJS_LOCAL_VARIABLE= createManagedFromKey(T_OBJ, IMG_OBJS_LOCAL_VARIABLE);

	public static final ImageDescriptor DESC_OBJS_HELP= createManagedFromKey(T_ELCL, IMG_OBJS_HELP);

    public static final ImageDescriptor DESC_ELCL_ADD_TO_BP= createUnManaged(T_ELCL, "add_to_buildpath.svg"); //$NON-NLS-1$
    public static final ImageDescriptor DESC_ELCL_REMOVE_FROM_BP= createUnManaged(T_ELCL, "remove_from_buildpath.svg"); //$NON-NLS-1$
    public static final ImageDescriptor DESC_ELCL_INCLUSION= createUnManaged(T_ELCL, "inclusion_filter_attrib.svg"); //$NON-NLS-1$
    public static final ImageDescriptor DESC_ELCL_EXCLUSION= createUnManaged(T_ELCL, "exclusion_filter_attrib.svg"); //$NON-NLS-1$

    public static final ImageDescriptor DESC_ELCL_ADD_LINKED_SOURCE_TO_BUILDPATH= createUnManaged(T_ELCL, "add_linked_source_to_buildpath.svg"); //$NON-NLS-1$

    public static final ImageDescriptor DESC_ELCL_CONFIGURE_BUILDPATH= createUnManaged(T_ELCL, "configure_build_path.svg"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_ELCL_CONFIGURE_ANNOTATIONS= createUnManaged(T_ELCL, "configure_annotations.svg"); //$NON-NLS-1$

    public static final ImageDescriptor DESC_ELCL_CONFIGURE_BUILDPATH_FILTERS= createUnManaged(T_ELCL, "configure_buildpath_filters.svg"); //$NON-NLS-1$

    public static final ImageDescriptor DESC_ELCL_CONFIGURE_OUTPUT_FOLDER= createUnManaged(T_ELCL, "configure_output_folder.svg"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_ELCL_CONFIGURE_PROBLEM_SEVERITIES= createManagedFromKey(T_ELCL, IMG_CONFIGURE_PROBLEM_SEVERITIES);

    public static final ImageDescriptor DESC_ELCL_EXCLUDE_FROM_BUILDPATH= createUnManaged(T_ELCL, "exclude_from_buildpath.svg");  //$NON-NLS-1$

    public static final ImageDescriptor DESC_ELCL_INCLUDE_ON_BUILDPATH= createUnManaged(T_ELCL, "include_on_buildpath.svg");  //$NON-NLS-1$

    public static final ImageDescriptor DESC_ELCL_ADD_AS_SOURCE_FOLDER= createUnManaged(T_ELCL, "add_as_source_folder.svg");  //$NON-NLS-1$

    public static final ImageDescriptor DESC_ELCL_REMOVE_AS_SOURCE_FOLDER= createUnManaged(T_ELCL, "remove_as_source_folder.svg");  //$NON-NLS-1$

    public static final ImageDescriptor DESC_ELCL_COPY_QUALIFIED_NAME= createUnManaged(T_ELCL, "cpyqual_menu.svg"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_ELCL_OPEN_BROWSER= createUnManaged(T_ELCL, "open_browser.svg"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_OBJ_OVERRIDES= createUnManaged(T_OBJ, "over_co.svg");  					//$NON-NLS-1$
	public static final ImageDescriptor DESC_OBJ_IMPLEMENTS= createUnManaged(T_OBJ, "implm_co.svg");  				//$NON-NLS-1$

	// Image descriptor used for default methods and annotation type elements with a default value
	public static final ImageDescriptor DESC_OVR_ANNOTATION_DEFAULT_METHOD= createUnManagedCached(T_OVR, "default_co.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_STATIC= createUnManagedCached(T_OVR, "static_co.svg"); 						//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_FINAL= createUnManagedCached(T_OVR, "final_co.svg"); 						//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_ABSTRACT= createUnManagedCached(T_OVR, "abstract_co.svg"); 					//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_SYNCH= createUnManagedCached(T_OVR, "synch_co.svg"); 						//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_VOLATILE= createUnManagedCached(T_OVR, "volatile_co.svg"); 						//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_TRANSIENT= createUnManagedCached(T_OVR, "transient_co.svg"); 						//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_NATIVE= createUnManagedCached(T_OVR, "native_co.svg"); 						//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_SEALED= createUnManagedCached(T_OVR, "sealed_co.svg"); 						//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_NON_SEALED= createUnManagedCached(T_OVR, "non-sealed_co.svg"); 						//$NON-NLS-1$

	public static final ImageDescriptor DESC_OVR_INFO= createUnManagedCached(T_OVR, "info_co.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_RUN= createUnManagedCached(T_OVR, "run_co.svg"); 							//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_WARNING= createUnManagedCached(T_OVR, "warning_co.svg"); 					//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_IGNORE_OPTIONAL_PROBLEMS= createUnManagedCached(T_OVR, "ignore_optional_problems_ovr.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_ERROR= createUnManagedCached(T_OVR, "error_co.svg"); 						//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_BUILDPATH_ERROR= createUnManagedCached(T_OVR, "error_co_buildpath.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_OVERRIDES= createUnManagedCached(T_OVR, "over_co.svg");  					//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_IMPLEMENTS= createUnManagedCached(T_OVR, "implm_co.svg");  				//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_SYNCH_AND_OVERRIDES= createUnManagedCached(T_OVR, "sync_over.svg");  	//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_SYNCH_AND_IMPLEMENTS= createUnManagedCached(T_OVR, "sync_impl.svg");   //$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_CONSTRUCTOR= createUnManagedCached(T_OVR, "constr_ovr.svg");			//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_DEPRECATED= createUnManagedCached(T_OVR, "deprecated.svg");			//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_DEFAULT= createUnManagedCached(T_OVR, "default_tsk.svg");			//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_FOCUS= createUnManagedCached(T_OVR, "focus_ovr.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_ANNOTATION= createUnManagedCached(T_OVR, "annotation_tsk.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_ENUM= createUnManagedCached(T_OVR, "enum_tsk.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_RECORD= createUnManagedCached(T_OVR, "record_tsk.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_INTERFACE= createUnManagedCached(T_OVR, "interface_tsk.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_CLASS= createUnManagedCached(T_OVR, "class_tsk.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_ABSTRACT_CLASS= createUnManagedCached(T_OVR, "class_abs_tsk.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_LIBRARY= createUnManagedCached(T_OVR, "library_ovr.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_SYSTEM_MOD= createUnManagedCached(T_OVR, "system_mod_ovr.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_AUTO_MOD= createUnManagedCached(T_OVR, "auto_mod_ovr.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_EXPORTS= createUnManagedCached(T_OVR, "exports_pkg_ovr.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_OPENS= createUnManagedCached(T_OVR, "opens_pkg_ovr.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_READS= createUnManagedCached(T_OVR, "reads_mod_ovr.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_PATCH= createUnManagedCached(T_OVR, "patch_ovr.svg"); //$NON-NLS-1$

    // Call Hierarchy
    public static final ImageDescriptor DESC_OVR_RECURSIVE= createUnManaged(T_OVR, "recursive_co.svg");              //$NON-NLS-1$
    public static final ImageDescriptor DESC_OVR_MAX_LEVEL= createUnManaged(T_OVR, "maxlevel_co.svg");                    //$NON-NLS-1$

	public static final ImageDescriptor DESC_WIZBAN_NEWCLASS= createUnManaged(T_WIZBAN, "newclass_wiz.svg"); 			//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_NEWINT= createUnManaged(T_WIZBAN, "newint_wiz.svg"); 				//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_NEWENUM= createUnManaged(T_WIZBAN, "newenum_wiz.svg"); 				//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_NEWRECORD= createUnManaged(T_WIZBAN, "newrecord_wiz.png"); 				//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_NEWANNOT= createUnManaged(T_WIZBAN, "newannotation_wiz.svg"); 				//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_NEWJPRJ= createUnManaged(T_WIZBAN, "newjprj_wiz.svg"); 			//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_NEWSRCFOLDR= createUnManaged(T_WIZBAN, "newsrcfldr_wiz.svg"); 	//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_NEWPACK= createUnManaged(T_WIZBAN, "newpack_wiz.svg"); 			//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_NEWMODULE= createUnManaged(T_WIZBAN, "newmodule_wiz.svg"); 			//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_NEWSCRAPPAGE= createUnManaged(T_WIZBAN, "newsbook_wiz.svg");		//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_JAVA_LAUNCH= createUnManaged(T_WIZBAN, "java_app_wiz.svg"); 		//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_JAVA_ATTACH= createUnManaged(T_WIZBAN, "java_attach_wiz.svg"); 	//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_REFACTOR= createUnManaged(T_WIZBAN, "refactor_wiz.svg"); 			//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_REFACTOR_FIELD= createUnManaged(T_WIZBAN, "fieldrefact_wiz.svg");	//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_REFACTOR_METHOD= createUnManaged(T_WIZBAN, "methrefact_wiz.svg");	//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_REFACTOR_TYPE= createUnManaged(T_WIZBAN, "typerefact_wiz.svg"); 	//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_REFACTOR_PACKAGE= createUnManaged(T_WIZBAN, "packrefact_wiz.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_REFACTOR_CODE= createUnManaged(T_WIZBAN, "coderefact_wiz.svg"); 	//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_REFACTOR_CU= createUnManaged(T_WIZBAN, "compunitrefact_wiz.svg");	//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_REFACTOR_PULL_UP= createUnManaged(T_WIZBAN, "pullup_wiz.svg");	//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_REFACTOR_FIX_DEPRECATION= createUnManaged(T_WIZBAN, "fixdepr_wiz.svg");	//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_JAR_PACKAGER= createUnManaged(T_WIZBAN, "jar_pack_wiz.svg"); 		//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_FAT_JAR_PACKAGER= createUnManaged(T_WIZBAN, "export_runnable_jar_wiz.svg"); 		//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_REFACTOR_EXTRACT_SUPERTYPE= createUnManaged(T_WIZBAN, "extractsupertype_wiz.svg");	//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_REPLACE_JAR= createUnManaged(T_WIZBAN, "replacejar_wiz.svg"); 		//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_JAVA_WORKINGSET= createUnManaged(T_WIZBAN, "java_workingset_wiz.svg");//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_EXPORT_JAVADOC= createUnManaged(T_WIZBAN, "export_javadoc_wiz.svg");//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_EXTERNALIZE_STRINGS= createUnManaged(T_WIZBAN, "extstr_wiz.svg");//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_ADD_LIBRARY= createUnManaged(T_WIZBAN, "addlibrary_wiz.svg");//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_CLEAN_UP= createUnManaged(T_WIZBAN, "cleanup_wiz.svg"); //$NON-NLS-1$


	public static final ImageDescriptor DESC_TOOL_SHOW_SEGMENTS= createUnManaged(T_ETOOL, "segment_edit.svg"); 		//$NON-NLS-1$

	public static final ImageDescriptor DESC_TOOL_OPENTYPE= createUnManaged(T_ETOOL, "opentype.svg"); 					//$NON-NLS-1$
	public static final ImageDescriptor DESC_TOOL_NEWPROJECT= createUnManaged(T_ETOOL, "newjprj_wiz.svg"); 			//$NON-NLS-1$
	public static final ImageDescriptor DESC_TOOL_NEWPACKAGE= createUnManaged(T_ETOOL, "newpack_wiz.svg"); 			//$NON-NLS-1$
	public static final ImageDescriptor DESC_TOOL_NEWCLASS= createUnManaged(T_ETOOL, "newclass_wiz.svg"); 				//$NON-NLS-1$
	public static final ImageDescriptor DESC_TOOL_NEWINTERFACE= createUnManaged(T_ETOOL, "newint_wiz.svg"); 			//$NON-NLS-1$
	public static final ImageDescriptor DESC_TOOL_NEWSNIPPET= createUnManaged(T_ETOOL, "newsbook_wiz.svg"); 			//$NON-NLS-1$
    public static final ImageDescriptor DESC_TOOL_NEWPACKROOT= createUnManaged(T_ETOOL, "newpackfolder_wiz.svg");         //$NON-NLS-1$

	public static final ImageDescriptor DESC_TOOL_CLASSPATH_ORDER= createUnManaged(T_OBJ, "cp_order_obj.svg"); 		//$NON-NLS-1$
	public static final ImageDescriptor DESC_ELCL_EXPANDALL= createUnManaged(T_ELCL, "expandall.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ELCL_COLLAPSEALL= createUnManaged(T_ELCL, "collapseall.svg"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_ELCL_MODIFYALL= createUnManaged(T_ELCL, "modifyall.svg"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_ELCL_REMOVE_EXTRA_LINES= createUnManaged(T_ELCL, "remove_extra_lines.svg"); //$NON-NLS-1$

	// Image descriptors used for formatter line wrapping preferences
	public static final ImageDescriptor DESC_ELCL_INDENT_COLUMN= createUnManaged(T_ELCL, "indent_column.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ELCL_INDENT_DEFAULT= createUnManaged(T_ELCL, "indent_default.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ELCL_INDENT_ONE= createUnManaged(T_ELCL, "indent_one.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ELCL_WRAP_AFTER= createUnManaged(T_ELCL, "wrap_after.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ELCL_WRAP_ALL= createUnManaged(T_ELCL, "wrap_all.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ELCL_WRAP_ALL_INDENT= createUnManaged(T_ELCL, "wrap_all_indent.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ELCL_WRAP_ALL_NOT_FIRST= createUnManaged(T_ELCL, "wrap_all_not_first.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ELCL_WRAP_BEFORE= createUnManaged(T_ELCL, "wrap_before.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ELCL_WRAP_FIRST_NECESSARY= createUnManaged(T_ELCL, "wrap_first_necessary.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ELCL_WRAP_FORCE= createUnManaged(T_ELCL, "wrap_force.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ELCL_WRAP_NECESSARY= createUnManaged(T_ELCL, "wrap_necessary.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ELCL_WRAP_NOT= createUnManaged(T_ELCL, "wrap_not.svg"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_ETOOL_JDOC_HOVER_EDIT= createUnManaged(T_ETOOL, "jdoc_hover_edit.svg"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_DTOOL_JDOC_HOVER_EDIT= ImageDescriptor.createWithFlags(DESC_ETOOL_JDOC_HOVER_EDIT, SWT.IMAGE_DISABLE);

	// Keys for correction proposal. We have to put the image into the registry since "code assist" doesn't
	// have a life cycle. So no chance to dispose icons.

	public static final String IMG_CORRECTION_CHANGE= NAME_PREFIX + "correction_change.gif"; //$NON-NLS-1$
	public static final String IMG_CORRECTION_MOVE= NAME_PREFIX + "correction_move.gif"; //$NON-NLS-1$
	public static final String IMG_CORRECTION_RENAME= NAME_PREFIX + "correction_rename.gif"; //$NON-NLS-1$
	public static final String IMG_CORRECTION_LINKED_RENAME= NAME_PREFIX + "correction_linked_rename.gif"; //$NON-NLS-1$
	public static final String IMG_CORRECTION_DELETE_IMPORT= NAME_PREFIX + "correction_delete_import.gif"; //$NON-NLS-1$
	public static final String IMG_CORRECTION_LOCAL= NAME_PREFIX + "localvariable_obj.gif"; //$NON-NLS-1$
	public static final String IMG_CORRECTION_REMOVE= NAME_PREFIX + "remove_correction.gif"; //$NON-NLS-1$
	public static final String IMG_CORRECTION_ADD= NAME_PREFIX + "add_correction.gif"; //$NON-NLS-1$
	public static final String IMG_CORRECTION_CAST= NAME_PREFIX + "correction_cast.gif"; //$NON-NLS-1$
	public static final String IMG_CORRECTION_MULTI_FIX= NAME_PREFIX + "correction_multi_fix.gif"; //$NON-NLS-1$

	static {
		createManagedFromKey(T_OBJ, IMG_CORRECTION_CHANGE);
		createManagedFromKey(T_OBJ, IMG_CORRECTION_MOVE);
		createManagedFromKey(T_OBJ, IMG_CORRECTION_RENAME);
		createManagedFromKey(T_OBJ, IMG_CORRECTION_LINKED_RENAME);
		createManagedFromKey(T_OBJ, IMG_CORRECTION_DELETE_IMPORT);
		createManagedFromKey(T_OBJ, IMG_CORRECTION_LOCAL);
		createManagedFromKey(T_OBJ, IMG_CORRECTION_REMOVE);
		createManagedFromKey(T_OBJ, IMG_CORRECTION_ADD);
		createManagedFromKey(T_OBJ, IMG_CORRECTION_CAST);
		createManagedFromKey(T_OBJ, IMG_CORRECTION_MULTI_FIX);
		createManagedFromKey(T_OBJ, IMG_OBJS_ERROR_ALT);
		createManagedFromKey(T_OBJ, IMG_OBJS_WARNING_ALT);
		createManagedFromKey(T_OBJ, IMG_OBJS_INFO_ALT);
		createManagedFromKey(T_OBJ, IMG_BLANK);
	}

	private static class SmallIntMap<V> {
		private static int fgSize = 1;

		private int[] keys = new int[fgSize];
		@SuppressWarnings("unchecked")
		private V[] values = (V[]) new Object[fgSize];

		/**
		 * @param key any int except for 0
		 * @param computer computes the value for the given key
		 * @return the cached or computed value for the given key
		 */
		public V computeIfAbsent(int key, IntFunction<V> computer) {
			int i= 0;
			for (; i < keys.length; i++) {
				if (keys[i] == key) {
					return values[i];
				} else if (keys[i] == 0) {
					keys[i]= key;
					V value= computer.apply(key);
					values[i]= value;
					return value;
				}
			}
			V value= computer.apply(key);
			int newLength= i + 1;
			if (newLength > fgSize) {
				fgSize++;
			}
			keys= Arrays.copyOf(keys, newLength);
			keys[i]= key;
			values= Arrays.copyOf(values, newLength);
			values[i]= value;
			return value;
		}
	}

	private static final class CachedImageDescriptor extends ImageDescriptor {
		private ImageDescriptor fDescriptor;
		private SmallIntMap<ImageData> fData= new SmallIntMap<>();

		public CachedImageDescriptor(ImageDescriptor descriptor) {
			fDescriptor= descriptor;
		}

		@Override
		public ImageData getImageData(int zoom) {
			ImageData cached= fData.computeIfAbsent(zoom, fDescriptor::getImageData);
			return cached;
		}
	}

	/**
	 * Returns the image managed under the given key in this registry.
	 *
	 * @param key the image's key
	 * @return the image managed under the given key
	 */
	public static Image get(String key) {
		return getImageRegistry().get(key);
	}

	/**
	 * Returns the image descriptor for the given key in this registry. Might be called in a non-UI thread.
	 *
	 * @param key the image's key
	 * @return the image descriptor for the given key
	 */
	public static ImageDescriptor getDescriptor(String key) {
		if (fgImageRegistry == null) {
			return fgAvoidSWTErrorMap.get(key);
		}
		return getImageRegistry().getDescriptor(key);
	}

	/**
	 * Sets the three image descriptors for enabled, disabled, and hovered to an action. The actions
	 * are retrieved from the *tool16 folders.
	 *
	 * @param action	the action
	 * @param iconName	the icon name
	 */
	public static void setToolImageDescriptors(IAction action, String iconName) {
		setImageDescriptors(action, "tool16", iconName); //$NON-NLS-1$
	}

	/**
	 * Sets the three image descriptors for enabled, disabled, and hovered to an action. The actions
	 * are retrieved from the *lcl16 folders.
	 *
	 * @param action	the action
	 * @param iconName	the icon name
	 */
	public static void setLocalImageDescriptors(IAction action, String iconName) {
		setImageDescriptors(action, "lcl16", iconName); //$NON-NLS-1$
	}

	/*
	 * Helper method to access the image registry from the JavaPlugin class.
	 */
	/* package */ static ImageRegistry getImageRegistry() {
		if (fgImageRegistry == null) {
			Display display = Display.getCurrent();
			if (display == null) {
				display = Display.getDefault();
			}
			fgImageRegistry= new ImageRegistry(display);
			for (Entry<String, ImageDescriptor> entry : fgAvoidSWTErrorMap.entrySet()) {
				fgImageRegistry.put(entry.getKey(), entry.getValue());
			}
			fgAvoidSWTErrorMap= null;
		}
		return fgImageRegistry;
	}

	//---- Helper methods to access icons on the file system --------------------------------------

	private static void setImageDescriptors(IAction action, String type, String relPath) {
		ImageDescriptor id= create("d" + type, relPath, false); //$NON-NLS-1$
		if (id != null)
			action.setDisabledImageDescriptor(id);

		ImageDescriptor descriptor= create("e" + type, relPath, true); //$NON-NLS-1$
		action.setImageDescriptor(descriptor);
	}

	private static ImageDescriptor createManagedFromKey(String prefix, String key) {
		return createManaged(prefix, key.substring(NAME_PREFIX_LENGTH), key);
	}

	private static ImageDescriptor createManaged(String prefix, String name, String key) {
		ImageDescriptor result= create(prefix, name, true);

		if (fgAvoidSWTErrorMap == null) {
			fgAvoidSWTErrorMap= new HashMap<>();
		}
		fgAvoidSWTErrorMap.put(key, result);
		if (fgImageRegistry != null) {
			JavaPlugin.logErrorMessage("Image registry already defined"); //$NON-NLS-1$
		}
		return result;
	}

	/*
	 * Creates an image descriptor for the given prefix and name in the JDT UI bundle. The path can
	 * contain variables like $NL$.
	 * If no image could be found, <code>useMissingImageDescriptor</code> decides if either
	 * the 'missing image descriptor' is returned or <code>null</code>.
	 * or <code>null</code>.
	 */
	private static ImageDescriptor create(String prefix, String name, boolean useMissingImageDescriptor) {
		IPath path= ICONS_PATH.append(prefix).append(name);
		return createImageDescriptor(JavaPlugin.getDefault().getBundle(), path, useMissingImageDescriptor);
	}

	/*
	 * Creates an image descriptor for the given prefix and name in the JDT UI bundle. The path can
	 * contain variables like $NL$.
	 * If no image could be found, the 'missing image descriptor' is returned.
	 */
	private static ImageDescriptor createUnManaged(String prefix, String name) {
		return create(prefix, name, true);
	}

	/*
	 * Creates an image descriptor for the given prefix and name in the JDT UI bundle and let type descriptor cache the image data.
	 * If no image could be found, the 'missing image descriptor' is returned.
	 */
	private static ImageDescriptor createUnManagedCached(String prefix, String name) {
		return new CachedImageDescriptor(create(prefix, name, true));
	}

	/*
	 * Creates an image descriptor for the given path in a bundle. The path can contain variables
	 * like $NL$.
	 * If no image could be found, <code>useMissingImageDescriptor</code> decides if either
	 * the 'missing image descriptor' is returned or <code>null</code>.
	 * Added for 3.1.1.
	 */
	public static ImageDescriptor createImageDescriptor(Bundle bundle, IPath path, boolean useMissingImageDescriptor) {
		// Bug 465521: Can't use *.svg in all existing keys, since some of them are made API via org.eclipse.jdt.ui.ISharedImages.
		// Workaround is to keep keep keys as *.gif and convert them dynamically here (only for jdt.ui, but not for jdt.junit!).
		if (bundle.equals(JavaPlugin.getDefault().getBundle()) && "gif".equals(path.getFileExtension())) { //$NON-NLS-1$
			path= path.removeFileExtension().addFileExtension("svg"); //$NON-NLS-1$
		}
		// Don't resolve the URL here, but create a URL using the
		// "platform:/plugin" protocol, which also supports fragments,
		// and for which URLImageDescriptor can find an "@2x" version.
		// Caveat: The resulting URL may contain $nl$ etc., which is not
		// directly supported by PlatformURLConnection and needs to go through
		// FileLocator#find(URL), see bug 250432.
		IPath uriPath= new Path("/plugin").append(bundle.getSymbolicName()).append(path); //$NON-NLS-1$
		URL url= null;
		try {
			URI uri= new URI("platform", null, uriPath.toString(), null); //$NON-NLS-1$
			url= uri.toURL();
		} catch (MalformedURLException | URISyntaxException e) {
			// no image
		}
		URL foundUrl= FileLocator.find(url);
		if (foundUrl != null) {
			return ImageDescriptor.createFromURL(url);
		}
		if (useMissingImageDescriptor) {
			return ImageDescriptor.getMissingImageDescriptor();
		}
		return null;
	}

	private JavaPluginImages() {
	}
}
