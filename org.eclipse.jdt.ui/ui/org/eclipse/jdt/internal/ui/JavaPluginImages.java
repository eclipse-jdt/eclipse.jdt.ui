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
package org.eclipse.jdt.internal.ui;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;


/**
 * Bundle of most images used by the Java plug-in.
 */
public class JavaPluginImages {

	private static final String NAME_PREFIX= "org.eclipse.jdt.ui."; //$NON-NLS-1$
	private static final int    NAME_PREFIX_LENGTH= NAME_PREFIX.length();

	private static URL fgIconBaseURL= null;
	
	// Determine display depth. If depth > 4 then we use high color images. Otherwise low color
	// images are used
	static {
		fgIconBaseURL= JavaPlugin.getDefault().getBundle().getEntry("/icons/full/"); //$NON-NLS-1$
	}
	
	// The plug-in registry
	private static ImageRegistry fgImageRegistry= null;
	private static HashMap fgAvoidSWTErrorMap= null;

	private static final String T_OBJ= "obj16"; 		//$NON-NLS-1$
	private static final String T_OVR= "ovr16"; 		//$NON-NLS-1$
	private static final String T_WIZBAN= "wizban"; 	//$NON-NLS-1$
	private static final String T_ELCL= "elcl16"; 	//$NON-NLS-1$
	private static final String T_DLCL= "dlcl16"; 	//$NON-NLS-1$
	private static final String T_ETOOL= "etool16"; 	//$NON-NLS-1$
	private static final String T_EVIEW= "eview16"; //$NON-NLS-1$

	/*
	 * Available cached Images in the Java plug-in image registry.
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
	public static final String IMG_DLCL_VIEW_MENU= NAME_PREFIX + T_DLCL + "view_menu.gif"; //$NON-NLS-1$
	
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
	public static final String IMG_OBJS_ENUM= NAME_PREFIX + "enum_obj.gif"; //$NON-NLS-1$
	
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
	public static final String IMG_OBJS_MISSING_PACKFRAG_ROOT= NAME_PREFIX + "packagefolder_nonexist_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_MISSING_JAR= NAME_PREFIX + "jar_nonexist_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_JAR= NAME_PREFIX + "jar_obj.gif"; 				//$NON-NLS-1$
	public static final String IMG_OBJS_EXTJAR= NAME_PREFIX + "jar_l_obj.gif"; 			//$NON-NLS-1$
	public static final String IMG_OBJS_JAR_WSRC= NAME_PREFIX + "jar_src_obj.gif"; 		//$NON-NLS-1$
	public static final String IMG_OBJS_EXTJAR_WSRC= NAME_PREFIX + "jar_lsrc_obj.gif";	//$NON-NLS-1$
	public static final String IMG_OBJS_ENV_VAR= NAME_PREFIX + "envvar_obj.gif"; 			//$NON-NLS-1$
	public static final String IMG_OBJS_MISSING_ENV_VAR= NAME_PREFIX + "envvar_nonexist_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_JAVA_MODEL= NAME_PREFIX + "java_model_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_UNKNOWN= NAME_PREFIX + "unknown_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_LOCAL_VARIABLE= NAME_PREFIX + "localvariable_obj.gif"; //$NON-NLS-1$
	
	public static final String IMG_OBJS_LIBRARY= NAME_PREFIX + "library_obj.gif"; 		//$NON-NLS-1$
	
	public static final String IMG_OBJS_JAVADOCTAG= NAME_PREFIX + "jdoc_tag_obj.gif"; 	//$NON-NLS-1$
	public static final String IMG_OBJS_HTMLTAG= NAME_PREFIX + "html_tag_obj.gif"; 		//$NON-NLS-1$
	
	public static final String IMG_OBJS_TEMPLATE= NAME_PREFIX + "template_obj.gif"; 		//$NON-NLS-1$

	public static final String IMG_OBJS_EXCEPTION= NAME_PREFIX + "jexception_obj.gif"; 	//$NON-NLS-1$
	public static final String IMG_OBJS_ERROR= NAME_PREFIX + "jrtexception_obj.gif"; 		//$NON-NLS-1$
	
	public static final String IMG_OBJS_BREAKPOINT_INSTALLED= NAME_PREFIX + "brkpi_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_QUICK_ASSIST= NAME_PREFIX + "quickassist_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_FIXABLE_PROBLEM= NAME_PREFIX + "quickfix_warning_obj.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_FIXABLE_ERROR= NAME_PREFIX + "quickfix_error_obj.gif"; //$NON-NLS-1$

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
	/*
	 * Set of predefined Image Descriptors.
	 */

	public static final ImageDescriptor DESC_VIEW_ERRORWARNING_TAB= create(T_EVIEW, "errorwarning_tab.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_VIEW_CLASSFILEGENERATION_TAB= create(T_EVIEW, "classfilegeneration_tab.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_VIEW_JDKCOMPLIANCE_TAB= create(T_EVIEW, "jdkcompliance_tab.gif"); //$NON-NLS-1$
	
	public static final ImageDescriptor DESC_ELCL_FILTER= create(T_ELCL, "filter_ps.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_DLCL_FILTER= create(T_DLCL, "filter_ps.gif"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_ELCL_CODE_ASSIST= create(T_ELCL, "metharg_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_DLCL_CODE_ASSIST= create(T_DLCL, "metharg_obj.gif"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_ELCL_VIEW_MENU= createManaged(T_ELCL, NAME_PREFIX + "view_menu.gif", IMG_ELCL_VIEW_MENU); //$NON-NLS-1$
	public static final ImageDescriptor DESC_DLCL_VIEW_MENU= createManaged(T_DLCL, NAME_PREFIX + "view_menu.gif", IMG_DLCL_VIEW_MENU); //$NON-NLS-1$
	
	public static final ImageDescriptor DESC_MISC_PUBLIC= createManaged(T_OBJ, IMG_MISC_PUBLIC);
	public static final ImageDescriptor DESC_MISC_PROTECTED= createManaged(T_OBJ, IMG_MISC_PROTECTED);
	public static final ImageDescriptor DESC_MISC_PRIVATE= createManaged(T_OBJ, IMG_MISC_PRIVATE);
	public static final ImageDescriptor DESC_MISC_DEFAULT= createManaged(T_OBJ, IMG_MISC_DEFAULT);

	public static final ImageDescriptor DESC_FIELD_PUBLIC= createManaged(T_OBJ, IMG_FIELD_PUBLIC); //$NON-NLS-1$
	public static final ImageDescriptor DESC_FIELD_PROTECTED= createManaged(T_OBJ, IMG_FIELD_PROTECTED); //$NON-NLS-1$
	public static final ImageDescriptor DESC_FIELD_PRIVATE= createManaged(T_OBJ, IMG_FIELD_PRIVATE); //$NON-NLS-1$
	public static final ImageDescriptor DESC_FIELD_DEFAULT= createManaged(T_OBJ, IMG_FIELD_DEFAULT); //$NON-NLS-1$
	
	public static final ImageDescriptor DESC_MENU_SHIFT_RIGHT= create(T_ETOOL, "shift_r_edit.gif"); 	//$NON-NLS-1$
	public static final ImageDescriptor DESC_MENU_SHIFT_LEFT= create(T_ETOOL, "shift_l_edit.gif"); 	//$NON-NLS-1$

	public static final ImageDescriptor DESC_OBJS_GHOST= createManaged(T_OBJ, IMG_OBJS_GHOST);
	public static final ImageDescriptor DESC_OBJS_PACKDECL= createManaged(T_OBJ, IMG_OBJS_PACKDECL);
	public static final ImageDescriptor DESC_OBJS_IMPDECL= createManaged(T_OBJ, IMG_OBJS_IMPDECL);
	public static final ImageDescriptor DESC_OBJS_IMPCONT= createManaged(T_OBJ, IMG_OBJS_IMPCONT);
	public static final ImageDescriptor DESC_OBJS_JSEARCH= createManaged(T_OBJ, IMG_OBJS_JSEARCH);
	public static final ImageDescriptor DESC_OBJS_SEARCH_DECL= createManaged(T_OBJ, IMG_OBJS_SEARCH_DECL);
	public static final ImageDescriptor DESC_OBJS_SEARCH_REF= createManaged(T_OBJ, IMG_OBJS_SEARCH_REF);
	public static final ImageDescriptor DESC_OBJS_CUNIT= createManaged(T_OBJ, IMG_OBJS_CUNIT);
	public static final ImageDescriptor DESC_OBJS_CUNIT_RESOURCE= createManaged(T_OBJ, IMG_OBJS_CUNIT_RESOURCE);
	public static final ImageDescriptor DESC_OBJS_CFILE= createManaged(T_OBJ, IMG_OBJS_CFILE); 
	public static final ImageDescriptor DESC_OBJS_CFILECLASS= createManaged(T_OBJ, IMG_OBJS_CFILECLASS); 
	public static final ImageDescriptor DESC_OBJS_CFILEINT= createManaged(T_OBJ, IMG_OBJS_CFILEINT); 
	public static final ImageDescriptor DESC_OBJS_PACKAGE= createManaged(T_OBJ, IMG_OBJS_PACKAGE);
	public static final ImageDescriptor DESC_OBJS_EMPTY_LOGICAL_PACKAGE= createManaged(T_OBJ, IMG_OBJS_EMPTY_LOGICAL_PACKAGE);
	public static final ImageDescriptor DESC_OBJS_LOGICAL_PACKAGE= createManaged(T_OBJ, IMG_OBJS_LOGICAL_PACKAGE);
	public static final ImageDescriptor DESC_OBJS_EMPTY_PACKAGE_RESOURCES= createManaged(T_OBJ, IMG_OBJS_EMPTY_PACK_RESOURCE);
	public static final ImageDescriptor DESC_OBJS_EMPTY_PACKAGE= createManaged(T_OBJ, IMG_OBJS_EMPTY_PACKAGE);	
	public static final ImageDescriptor DESC_OBJS_PACKFRAG_ROOT= createManaged(T_OBJ, IMG_OBJS_PACKFRAG_ROOT);
	public static final ImageDescriptor DESC_OBJS_MISSING_PACKFRAG_ROOT= createManaged(T_OBJ, IMG_OBJS_MISSING_PACKFRAG_ROOT);
	public static final ImageDescriptor DESC_OBJS_JAVA_MODEL= createManaged(T_OBJ, IMG_OBJS_JAVA_MODEL);

	public static final ImageDescriptor DESC_OBJS_CLASS= createManaged(T_OBJ, IMG_OBJS_CLASS);
	public static final ImageDescriptor DESC_OBJS_CLASS_DEFAULT= createManaged(T_OBJ, IMG_OBJS_CLASS_DEFAULT);

	public static final ImageDescriptor DESC_OBJS_INNER_CLASS_PUBLIC= createManaged(T_OBJ, IMG_OBJS_INNER_CLASS_PUBLIC);
	public static final ImageDescriptor DESC_OBJS_INNER_CLASS_DEFAULT= createManaged(T_OBJ, IMG_OBJS_INNER_CLASS_DEFAULT);
	public static final ImageDescriptor DESC_OBJS_INNER_CLASS_PROTECTED= createManaged(T_OBJ, IMG_OBJS_INNER_CLASS_PROTECTED);
	public static final ImageDescriptor DESC_OBJS_INNER_CLASS_PRIVATE= createManaged(T_OBJ, IMG_OBJS_INNER_CLASS_PRIVATE);
	
	public static final ImageDescriptor DESC_OBJS_CLASSALT= createManaged(T_OBJ, IMG_OBJS_CLASSALT);

	public static final ImageDescriptor DESC_OBJS_INTERFACE= createManaged(T_OBJ, IMG_OBJS_INTERFACE);
	public static final ImageDescriptor DESC_OBJS_INTERFACE_DEFAULT= createManaged(T_OBJ, IMG_OBJS_INTERFACE_DEFAULT);
	
	public static final ImageDescriptor DESC_OBJS_INNER_INTERFACE_PUBLIC= createManaged(T_OBJ, IMG_OBJS_INNER_INTERFACE_PUBLIC);
	public static final ImageDescriptor DESC_OBJS_INNER_INTERFACE_DEFAULT= createManaged(T_OBJ, IMG_OBJS_INNER_INTERFACE_DEFAULT);
	public static final ImageDescriptor DESC_OBJS_INNER_INTERFACE_PROTECTED= createManaged(T_OBJ, IMG_OBJS_INNER_INTERFACE_PROTECTED);
	public static final ImageDescriptor DESC_OBJS_INNER_INTERFACE_PRIVATE= createManaged(T_OBJ, IMG_OBJS_INNER_INTERFACE_PRIVATE);
	
	public static final ImageDescriptor DESC_OBJS_INTERFACEALT= createManaged(T_OBJ, IMG_OBJS_INTERFACEALT);
	
	public static final ImageDescriptor DESC_OBJS_ANNOTATION= createManaged(T_OBJ, IMG_OBJS_ANNOTATION);
	public static final ImageDescriptor DESC_OBJS_ENUM= createManaged(T_OBJ, IMG_OBJS_ENUM);
	
	public static final ImageDescriptor DESC_OBJS_JAR= createManaged(T_OBJ, IMG_OBJS_JAR);
	public static final ImageDescriptor DESC_OBJS_MISSING_JAR= createManaged(T_OBJ, IMG_OBJS_MISSING_JAR);
	public static final ImageDescriptor DESC_OBJS_EXTJAR= createManaged(T_OBJ, IMG_OBJS_EXTJAR);
	public static final ImageDescriptor DESC_OBJS_JAR_WSRC= createManaged(T_OBJ, IMG_OBJS_JAR_WSRC);
	public static final ImageDescriptor DESC_OBJS_EXTJAR_WSRC= createManaged(T_OBJ, IMG_OBJS_EXTJAR_WSRC);
	public static final ImageDescriptor DESC_OBJS_ENV_VAR= createManaged(T_OBJ, IMG_OBJS_ENV_VAR);
	public static final ImageDescriptor DESC_OBJS_MISSING_ENV_VAR= createManaged(T_OBJ, IMG_OBJS_MISSING_ENV_VAR);
	
	public static final ImageDescriptor DESC_OBJS_LIBRARY= createManaged(T_OBJ, IMG_OBJS_LIBRARY);
	
	public static final ImageDescriptor DESC_OBJS_JAVADOCTAG= createManaged(T_OBJ, IMG_OBJS_JAVADOCTAG);
	public static final ImageDescriptor DESC_OBJS_HTMLTAG= createManaged(T_OBJ, IMG_OBJS_HTMLTAG);

	public static final ImageDescriptor DESC_OBJS_TEMPLATE= createManaged(T_OBJ, IMG_OBJS_TEMPLATE);
	
	public static final ImageDescriptor DESC_OBJS_EXCEPTION= createManaged(T_OBJ, IMG_OBJS_EXCEPTION);
	public static final ImageDescriptor DESC_OBJS_BREAKPOINT_INSTALLED= createManaged(T_OBJ, IMG_OBJS_BREAKPOINT_INSTALLED);
	public static final ImageDescriptor DESC_OBJS_ERROR= createManaged(T_OBJ, IMG_OBJS_ERROR);
	public static final ImageDescriptor DESC_OBJS_QUICK_ASSIST= createManaged(T_OBJ, IMG_OBJS_QUICK_ASSIST);
	public static final ImageDescriptor DESC_OBJS_FIXABLE_PROBLEM= createManaged(T_OBJ, IMG_OBJS_FIXABLE_PROBLEM);
	public static final ImageDescriptor DESC_OBJS_FIXABLE_ERROR= createManaged(T_OBJ, IMG_OBJS_FIXABLE_ERROR);
	
	// public static final ImageDescriptor DESC_OBJS_SNIPPET_EVALUATING= createManaged(T_OBJ, IMG_OBJS_SNIPPET_EVALUATING);
	
	public static final ImageDescriptor DESC_OBJS_DEFAULT_CHANGE= create(T_OBJ, "change.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OBJS_COMPOSITE_CHANGE= create(T_OBJ, "composite_change.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OBJS_CU_CHANGE= create(T_OBJ, "cu_change.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OBJS_FILE_CHANGE= create(T_OBJ, "file_change.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OBJS_TEXT_EDIT= create(T_OBJ, "text_edit.gif"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_OBJS_EXCLUSION_FILTER_ATTRIB= create(T_OBJ, "exclusion_filter_attrib.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OBJS_INCLUSION_FILTER_ATTRIB= create(T_OBJ, "inclusion_filter_attrib.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OBJS_OUTPUT_FOLDER_ATTRIB= create(T_OBJ, "output_folder_attrib.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OBJS_SOURCE_ATTACH_ATTRIB= create(T_OBJ, "source_attach_attrib.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OBJS_JAVADOC_LOCATION_ATTRIB= create(T_OBJ, "javadoc_location_attrib.gif"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_OBJS_REFACTORING_FATAL= createManaged(T_OBJ, IMG_OBJS_REFACTORING_FATAL);
	public static final ImageDescriptor DESC_OBJS_REFACTORING_ERROR= createManaged(T_OBJ, IMG_OBJS_REFACTORING_ERROR);
	public static final ImageDescriptor DESC_OBJS_REFACTORING_WARNING= createManaged(T_OBJ, IMG_OBJS_REFACTORING_WARNING);
	public static final ImageDescriptor DESC_OBJS_REFACTORING_INFO= createManaged(T_OBJ, IMG_OBJS_REFACTORING_INFO);
	
	public static final ImageDescriptor DESC_OBJS_NLS_TRANSLATE= createManaged(T_OBJ, IMG_OBJS_NLS_TRANSLATE);
	public static final ImageDescriptor DESC_OBJS_NLS_NEVER_TRANSLATE= createManaged(T_OBJ, IMG_OBJS_NLS_NEVER_TRANSLATE);
	public static final ImageDescriptor DESC_OBJS_NLS_SKIP= createManaged(T_OBJ, IMG_OBJS_NLS_SKIP);
	
	public static final ImageDescriptor DESC_OBJS_UNKNOWN= createManaged(T_OBJ, IMG_OBJS_UNKNOWN);

	public static final ImageDescriptor DESC_OBJS_SEARCH_READACCESS= createManaged(T_OBJ, IMG_OBJS_SEARCH_READACCESS);
	public static final ImageDescriptor DESC_OBJS_SEARCH_WRITEACCESS= createManaged(T_OBJ, IMG_OBJS_SEARCH_WRITEACCESS);
	public static final ImageDescriptor DESC_OBJS_SEARCH_OCCURRENCE= createManaged(T_OBJ, IMG_OBJS_SEARCH_OCCURRENCE);

	public static final ImageDescriptor DESC_OBJS_LOCAL_VARIABLE= createManaged(T_OBJ, IMG_OBJS_LOCAL_VARIABLE);
	
	public static final ImageDescriptor DESC_OBJ_OVERRIDES= create(T_OBJ, "over_co.gif");  					//$NON-NLS-1$
	public static final ImageDescriptor DESC_OBJ_IMPLEMENTS= create(T_OBJ, "implm_co.gif");  				//$NON-NLS-1$
	
	public static final ImageDescriptor DESC_OVR_STATIC= create(T_OVR, "static_co.gif"); 						//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_FINAL= create(T_OVR, "final_co.gif"); 						//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_ABSTRACT= create(T_OVR, "abstract_co.gif"); 					//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_SYNCH= create(T_OVR, "synch_co.gif"); 						//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_RUN= create(T_OVR, "run_co.gif"); 							//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_WARNING= create(T_OVR, "warning_co.gif"); 					//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_ERROR= create(T_OVR, "error_co.gif"); 						//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_OVERRIDES= create(T_OVR, "over_co.gif");  					//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_IMPLEMENTS= create(T_OVR, "implm_co.gif");  				//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_SYNCH_AND_OVERRIDES= create(T_OVR, "sync_over.gif");  	//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_SYNCH_AND_IMPLEMENTS= create(T_OVR, "sync_impl.gif");   //$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_CONSTRUCTOR= create(T_OVR, "constr_ovr.gif");			//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_DEPRECATED= create(T_OVR, "deprecated.gif");			//$NON-NLS-1$
	public static final ImageDescriptor DESC_OVR_FOCUS= create(T_OVR, "focus_ovr.gif"); //$NON-NLS-1$

    // Call Hierarchy
    public static final ImageDescriptor DESC_OVR_RECURSIVE= create(T_OVR, "recursive_co.gif");              //$NON-NLS-1$
    public static final ImageDescriptor DESC_OVR_MAX_LEVEL= create(T_OVR, "maxlevel_co.gif");                    //$NON-NLS-1$
		
	public static final ImageDescriptor DESC_WIZBAN_NEWCLASS= create(T_WIZBAN, "newclass_wiz.gif"); 			//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_NEWINT= create(T_WIZBAN, "newint_wiz.gif"); 				//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_NEWJPRJ= create(T_WIZBAN, "newjprj_wiz.gif"); 			//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_NEWSRCFOLDR= create(T_WIZBAN, "newsrcfldr_wiz.gif"); 	//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_NEWMETH= create(T_WIZBAN, "newmeth_wiz.gif"); 			//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_NEWPACK= create(T_WIZBAN, "newpack_wiz.gif"); 			//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_NEWSCRAPPAGE= create(T_WIZBAN, "newsbook_wiz.gif");		//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_JAVA_LAUNCH= create(T_WIZBAN, "java_app_wiz.gif"); 		//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_JAVA_ATTACH= create(T_WIZBAN, "java_attach_wiz.gif"); 	//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_REFACTOR= create(T_WIZBAN, "refactor_wiz.gif"); 			//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_REFACTOR_FIELD= create(T_WIZBAN, "fieldrefact_wiz.gif");	//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_REFACTOR_METHOD= create(T_WIZBAN, "methrefact_wiz.gif");	//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_REFACTOR_TYPE= create(T_WIZBAN, "typerefact_wiz.gif"); 	//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_REFACTOR_PACKAGE= create(T_WIZBAN, "packrefact_wiz.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_REFACTOR_CODE= create(T_WIZBAN, "coderefact_wiz.gif"); 	//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_REFACTOR_CU= create(T_WIZBAN, "compunitrefact_wiz.gif");	//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_REFACTOR_PULL_UP= create(T_WIZBAN, "pullup_wiz.gif");	//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_JAR_PACKAGER= create(T_WIZBAN, "jar_pack_wiz.gif"); 		//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_JAVA_WORKINGSET= create(T_WIZBAN, "java_workingset_wiz.gif");//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_EXPORT_JAVADOC= create(T_WIZBAN, "export_javadoc_wiz.gif");//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_EXTERNALIZE_STRINGS= create(T_WIZBAN, "extstr_wiz.gif");//$NON-NLS-1$
	public static final ImageDescriptor DESC_WIZBAN_ADD_LIBRARY= create(T_WIZBAN, "addlibrary_wiz.gif");//$NON-NLS-1$

		
	public static final ImageDescriptor DESC_TOOL_SHOW_EMPTY_PKG= create(T_ETOOL, "show_empty_pkg.gif"); 		//$NON-NLS-1$
	public static final ImageDescriptor DESC_TOOL_SHOW_SEGMENTS= create(T_ETOOL, "segment_edit.gif"); 		//$NON-NLS-1$

	public static final ImageDescriptor DESC_TOOL_OPENTYPE= create(T_ETOOL, "opentype.gif"); 					//$NON-NLS-1$
	public static final ImageDescriptor DESC_TOOL_NEWPROJECT= create(T_ETOOL, "newjprj_wiz.gif"); 			//$NON-NLS-1$
	public static final ImageDescriptor DESC_TOOL_NEWPACKAGE= create(T_ETOOL, "newpack_wiz.gif"); 			//$NON-NLS-1$
	public static final ImageDescriptor DESC_TOOL_NEWCLASS= create(T_ETOOL, "newclass_wiz.gif"); 				//$NON-NLS-1$
	public static final ImageDescriptor DESC_TOOL_NEWINTERFACE= create(T_ETOOL, "newint_wiz.gif"); 			//$NON-NLS-1$
	public static final ImageDescriptor DESC_TOOL_NEWSNIPPET= create(T_ETOOL, "newsbook_wiz.gif"); 			//$NON-NLS-1$
	public static final ImageDescriptor DESC_TOOL_NEWPACKROOT= create(T_ETOOL, "newpackfolder_wiz.gif"); 		//$NON-NLS-1$

	public static final ImageDescriptor DESC_TOOL_CLASSPATH_ORDER= create(T_OBJ, "cp_order_obj.gif"); 		//$NON-NLS-1$

	// Keys for correction proposal. We have to put the image into the registry since "code assist" doesn't
	// have a life cycle. So no change to dispose icons.
	
	public static final String IMG_CORRECTION_CHANGE= NAME_PREFIX + "correction_change.gif"; //$NON-NLS-1$
	public static final String IMG_CORRECTION_MOVE= NAME_PREFIX + "correction_move.gif"; //$NON-NLS-1$
	public static final String IMG_CORRECTION_RENAME= NAME_PREFIX + "correction_rename.gif"; //$NON-NLS-1$
	public static final String IMG_CORRECTION_DELETE_IMPORT= NAME_PREFIX + "correction_delete_import.gif"; //$NON-NLS-1$
	public static final String IMG_CORRECTION_LOCAL= NAME_PREFIX + "localvariable_obj.gif"; //$NON-NLS-1$
	public static final String IMG_CORRECTION_REMOVE= NAME_PREFIX + "remove_correction.gif"; //$NON-NLS-1$
	public static final String IMG_CORRECTION_ADD= NAME_PREFIX + "add_correction.gif"; //$NON-NLS-1$
	public static final String IMG_CORRECTION_CAST= NAME_PREFIX + "correction_cast.gif"; //$NON-NLS-1$

	static {
		createManaged(T_OBJ, IMG_CORRECTION_CHANGE);
		createManaged(T_OBJ, IMG_CORRECTION_MOVE);
		createManaged(T_OBJ, IMG_CORRECTION_RENAME);
		createManaged(T_OBJ, IMG_CORRECTION_DELETE_IMPORT);
		createManaged(T_OBJ, IMG_CORRECTION_LOCAL);
		createManaged(T_OBJ, IMG_CORRECTION_REMOVE);
		createManaged(T_OBJ, IMG_CORRECTION_ADD);
		createManaged(T_OBJ, IMG_CORRECTION_CAST);
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
			fgImageRegistry= new ImageRegistry();
			for (Iterator iter= fgAvoidSWTErrorMap.keySet().iterator(); iter.hasNext();) {
				String key= (String) iter.next();
				fgImageRegistry.put(key, (ImageDescriptor) fgAvoidSWTErrorMap.get(key));
			}
			fgAvoidSWTErrorMap= null;
		}
		return fgImageRegistry;
	}

	//---- Helper methods to access icons on the file system --------------------------------------

	private static void setImageDescriptors(IAction action, String type, String relPath) {
		
		try {
			ImageDescriptor id= ImageDescriptor.createFromURL(makeIconFileURL("d" + type, relPath)); //$NON-NLS-1$
			if (id != null)
				action.setDisabledImageDescriptor(id);
		} catch (MalformedURLException e) {
		}
	
		/*
		try {
			ImageDescriptor id= ImageDescriptor.createFromURL(makeIconFileURL("c" + type, relPath)); //$NON-NLS-1$
			if (id != null)
				action.setHoverImageDescriptor(id);
		} catch (MalformedURLException e) {
		}
		*/
	
		ImageDescriptor descriptor= create("e" + type, relPath); //$NON-NLS-1$
		action.setHoverImageDescriptor(descriptor);
		action.setImageDescriptor(descriptor); 
	}
	
	private static ImageDescriptor createManaged(String prefix, String name) {
		try {
			ImageDescriptor result= ImageDescriptor.createFromURL(makeIconFileURL(prefix, name.substring(NAME_PREFIX_LENGTH)));
			if (fgAvoidSWTErrorMap == null) {
				fgAvoidSWTErrorMap= new HashMap();
			}
			fgAvoidSWTErrorMap.put(name, result);
			if (fgImageRegistry != null) {
				JavaPlugin.logErrorMessage("Image registry already defined"); //$NON-NLS-1$
			}
			return result;
		} catch (MalformedURLException e) {
			return ImageDescriptor.getMissingImageDescriptor();
		}
	}
	
	private static ImageDescriptor createManaged(String prefix, String name, String key) {
		try {
			ImageDescriptor result= ImageDescriptor.createFromURL(makeIconFileURL(prefix, name.substring(NAME_PREFIX_LENGTH)));
			if (fgAvoidSWTErrorMap == null) {
				fgAvoidSWTErrorMap= new HashMap();
			}
			fgAvoidSWTErrorMap.put(key, result);
			if (fgImageRegistry != null) {
				JavaPlugin.logErrorMessage("Image registry already defined"); //$NON-NLS-1$
			}
			return result;
		} catch (MalformedURLException e) {
			return ImageDescriptor.getMissingImageDescriptor();
		}
	}
	
	private static ImageDescriptor create(String prefix, String name) {
		try {
			return ImageDescriptor.createFromURL(makeIconFileURL(prefix, name));
		} catch (MalformedURLException e) {
			return ImageDescriptor.getMissingImageDescriptor();
		}
	}
	
	private static URL makeIconFileURL(String prefix, String name) throws MalformedURLException {
		if (fgIconBaseURL == null)
			throw new MalformedURLException();
			
		StringBuffer buffer= new StringBuffer(prefix);
		buffer.append('/');
		buffer.append(name);
		return new URL(fgIconBaseURL, buffer.toString());
	}	
}
