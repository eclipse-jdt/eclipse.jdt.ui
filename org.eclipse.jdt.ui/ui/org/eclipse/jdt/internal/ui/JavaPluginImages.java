/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;

/**
 * Bundle of all images used by the Java plugin.
 */
public class JavaPluginImages {

	private static final String NAME_PREFIX= "org.eclipse.jdt.ui.";
	private static final int    NAME_PREFIX_LENGTH= NAME_PREFIX.length();

	// Subdirectory (under the package containing this class) where 16 color images are
	private static URL fgIconBaseURL= null;
	static {
		try {
			fgIconBaseURL= new URL(JavaPlugin.getDefault().getDescriptor().getInstallURL(), "icons/" );
		} catch (MalformedURLException e) {
			// do nothing
		}
	}
	
	// The plugin registry
	private final static ImageRegistry IMAGE_REGISTRY= new ImageRegistry();


	/**
	 * Available cached Images in the Java plugin image registry.
	 */	
	public static final String IMG_MISC_PUBLIC= NAME_PREFIX + "methpub_obj.gif";
	public static final String IMG_MISC_PROTECTED= NAME_PREFIX + "methpro_obj.gif";
	public static final String IMG_MISC_PRIVATE= NAME_PREFIX + "methpri_obj.gif";
	public static final String IMG_MISC_DEFAULT= NAME_PREFIX + "methdef_obj.gif";
	
	// public static final String IMG_LCL_FILTER_INTERFACE= NAME_PREFIX + "filter_int.gif";
	// public static final String IMG_LCL_SHOW_INHERITED= NAME_PREFIX + "inher_co.gif";
	// public static final String IMG_LCL_SHOW_STATIC= NAME_PREFIX + "static_co.gif";
	// public static final String IMG_LCL_SHOW_FIELDS= NAME_PREFIX + "fields_co.gif";
	// public static final String IMG_LCL_TERMINATE= NAME_PREFIX + "terminate.gif";
	// public static final String IMG_LCL_REFRESH= NAME_PREFIX + "refresh.gif";
	// public static final String IMG_LCL_LOCK_VIEW= NAME_PREFIX + "lock_view.gif";
	// public static final String IMG_LCL_SUBTYPES_VIEW= NAME_PREFIX + "subtypes_view.gif";
	// public static final String IMG_LCL_SUPERTYPES_VIEW= NAME_PREFIX + "supertypes_view.gif";
	// public static final String IMG_LCL_MEMBER_FILTER= NAME_PREFIX + "member_filter.gif";
	
	// public static final String IMG_MENU_SHIFT_RIGHT= NAME_PREFIX + "shift_r_edit.gif";
	// public static final String IMG_MENU_SHIFT_LEFT= NAME_PREFIX + "shift_l_edit.gif";

	public static final String IMG_OBJS_GHOST= NAME_PREFIX + "ghost.gif";
	public static final String IMG_OBJS_SEARCH_TSK= NAME_PREFIX + "search_tsk.gif";
	public static final String IMG_OBJS_PACKDECL= NAME_PREFIX + "packd_obj.gif";
	public static final String IMG_OBJS_IMPDECL= NAME_PREFIX + "imp_obj.gif";
	public static final String IMG_OBJS_IMPCONT= NAME_PREFIX + "impc_obj.gif";
	public static final String IMG_OBJS_JSEARCH= NAME_PREFIX + "jsearch_obj.gif";
	public static final String IMG_OBJS_SEARCH_DECL= NAME_PREFIX + "search_decl_obj.gif";
	public static final String IMG_OBJS_SEARCH_REF= NAME_PREFIX + "search_ref_obj.gif";
	public static final String IMG_OBJS_CLASS= NAME_PREFIX + "class_obj.gif";
	public static final String IMG_OBJS_PCLASS= NAME_PREFIX + "classp_obj.gif";
	public static final String IMG_OBJS_INTERFACE= NAME_PREFIX + "int_obj.gif";
	public static final String IMG_OBJS_PINTERFACE= NAME_PREFIX + "intp_obj.gif";
	public static final String IMG_OBJS_CUNIT= NAME_PREFIX + "jcu_obj.gif";
	public static final String IMG_OBJS_CFILE= NAME_PREFIX + "classf_obj.gif"; 
	public static final String IMG_OBJS_CFILECLASS= NAME_PREFIX + "classfc_obj.gif"; 
	public static final String IMG_OBJS_CFILEINT= NAME_PREFIX + "classfi_obj.gif"; 
	public static final String IMG_OBJS_PACKAGE= NAME_PREFIX + "package_obj.gif";
	public static final String IMG_OBJS_PACKFRAG_ROOT= NAME_PREFIX + "packagefolder_obj.gif";
	public static final String IMG_OBJS_JAR= NAME_PREFIX + "jar_obj.gif";
	public static final String IMG_OBJS_EXTJAR= NAME_PREFIX + "jar_l_obj.gif";
	public static final String IMG_OBJS_JAR_WSRC= NAME_PREFIX + "jar_src_obj.gif";
	public static final String IMG_OBJS_EXTJAR_WSRC= NAME_PREFIX + "jar_lsrc_obj.gif";	
	public static final String IMG_OBJS_ENV_VAR= NAME_PREFIX + "envvar_obj.gif";
	
	
	public static final String IMG_OBJS_JAVADOCTAG= NAME_PREFIX + "jdoc_tag_obj.gif";
	public static final String IMG_OBJS_HTMLTAG= NAME_PREFIX + "html_tag_obj.gif";
		
	public static final String IMG_OBJS_EXCEPTION= NAME_PREFIX + "jexception_obj.gif";
	public static final String IMG_OBJS_ERROR= NAME_PREFIX + "jrtexception_obj.gif";
	
	public static final String IMG_OBJS_BREAKPOINT_INSTALLED= NAME_PREFIX + "jbreakpoint_installed_obj.gif";

	public static final String IMG_OBJS_STACK_FRAME_SYNCH= NAME_PREFIX + "jstackframe_synch_obj.gif";

	public static final String IMG_OBJS_REFACTORING_FATAL= NAME_PREFIX + "rf_fatal.gif";
	public static final String IMG_OBJS_REFACTORING_ERROR= NAME_PREFIX + "rf_error.gif";
	public static final String IMG_OBJS_REFACTORING_WARNING= NAME_PREFIX + "rf_warn.gif";
	public static final String IMG_OBJS_REFACTORING_INFO= NAME_PREFIX + "rf_info.gif";

	// public static final String IMG_OVR_STATIC= NAME_PREFIX + "static_co.gif";
	// public static final String IMG_OVR_FINAL= NAME_PREFIX + "final_co.gif";
	// public static final String IMG_OVR_ABSTRACT= NAME_PREFIX + "abstract_co.gif";
	// public static final String IMG_OVR_SYNCH= NAME_PREFIX + "synch_co.gif";
	// public static final String IMG_OVR_RUN= NAME_PREFIX + "run_co.gif";
		
	// public static final String IMG_WIZBAN_NEWCLASS= NAME_PREFIX + "newclass_wiz.gif";
	// public static final String IMG_WIZBAN_NEWFIELD= NAME_PREFIX + "newfield_wiz.gif";
	// public static final String IMG_WIZBAN_NEWINT= NAME_PREFIX + "newint_wiz.gif";
	// public static final String IMG_WIZBAN_NEWJPRJ= NAME_PREFIX + "newjprj_wiz.gif";
	// public static final String IMG_WIZBAN_NEWMETH= NAME_PREFIX + "newmeth_wiz.gif";
	// public static final String IMG_WIZBAN_NEWPACK= NAME_PREFIX + "newpack_wiz.gif";
	
	// public static final String IMG_TOOL_DISPLAYSNIPPET= NAME_PREFIX + "x_display_snp.gif";
	// public static final String IMG_TOOL_RUNSNIPPET= NAME_PREFIX + "x_run_snp.gif";
	// public static final String IMG_TOOL_PACKSNIPPET= NAME_PREFIX + "x_package_snp.gif";
	// public static final String IMG_TOOL_TERMSNIPPET= NAME_PREFIX + "x_terminate_snp.gif";
	// public static final String IMG_TOOL_ADD_EXCEPTION= NAME_PREFIX + "x_add_exception.gif";
	// public static final String IMG_TOOL_DELETE= NAME_PREFIX + "x_delete.gif";
	// public static final String IMG_TOOL_DELETE_ALL= NAME_PREFIX + "x_delete_all.gif";
	// public static final String IMG_TOOL_SHOW_EMPTY_PKG= NAME_PREFIX + "x_show_empty_pkg.gif";

	// public static final String IMG_TOOL_CLASSPATH_ORDER= NAME_PREFIX + "cp_order.gif";


	/**
	 * Set of predefined Image Descriptors.
	 */
	private static final String T_OBJ= "full/obj16";
	private static final String T_OVR= "full/ovr16";
	private static final String T_WIZBAN= "full/wizban";
	private static final String T_LCL= "full/clcl16";
	private static final String T_CTOOL= "full/ctool16";

	public static final ImageDescriptor DESC_MISC_PUBLIC= createManaged(T_OBJ, IMG_MISC_PUBLIC);
	public static final ImageDescriptor DESC_MISC_PROTECTED= createManaged(T_OBJ, IMG_MISC_PROTECTED);
	public static final ImageDescriptor DESC_MISC_PRIVATE= createManaged(T_OBJ, IMG_MISC_PRIVATE);
	public static final ImageDescriptor DESC_MISC_DEFAULT= createManaged(T_OBJ, IMG_MISC_DEFAULT);
	
	public static final ImageDescriptor DESC_LCL_SHOW_INHERITED= create(T_LCL, "inher_co.gif");
	public static final ImageDescriptor DESC_LCL_SHOW_STATIC= create(T_LCL, "static_co.gif");
	public static final ImageDescriptor DESC_LCL_SHOW_FINAL= create(T_LCL, "final_co.gif");
	public static final ImageDescriptor DESC_LCL_SHOW_FIELDS= create(T_LCL, "fields_co.gif");
	public static final ImageDescriptor DESC_LCL_LOCK_VIEW= create(T_LCL, "lock_close.gif");
	public static final ImageDescriptor DESC_LCL_SUBTYPES_VIEW= create(T_LCL, "sub_co.gif");
	public static final ImageDescriptor DESC_LCL_SUPERTYPES_VIEW= create(T_LCL, "super_co.gif");
	public static final ImageDescriptor DESC_LCL_VAJHIERARCHY_VIEW= create(T_LCL, "hierarchy_co.gif");
	public static final ImageDescriptor DESC_LCL_MEMBER_FILTER= create(T_LCL, "impl_co.gif");	
	public static final ImageDescriptor DESC_LCL_ADD_EXCEPTION= create(T_LCL, "exc_catch.gif");
	public static final ImageDescriptor DESC_LCL_DELETE= create(T_LCL, "remove_exc.gif");
	public static final ImageDescriptor DESC_LCL_DELETE_ALL= create(T_LCL, "removea_exc.gif");
	
	public static final ImageDescriptor DESC_MENU_SHIFT_RIGHT= create(T_CTOOL, "shift_r_edit.gif");
	public static final ImageDescriptor DESC_MENU_SHIFT_LEFT= create(T_CTOOL, "shift_l_edit.gif");

	public static final ImageDescriptor DESC_OBJS_GHOST= createManaged(T_OBJ, IMG_OBJS_GHOST);
	public static final ImageDescriptor DESC_OBJS_PACKDECL= createManaged(T_OBJ, IMG_OBJS_PACKDECL);
	public static final ImageDescriptor DESC_OBJS_IMPDECL= createManaged(T_OBJ, IMG_OBJS_IMPDECL);
	public static final ImageDescriptor DESC_OBJS_IMPCONT= createManaged(T_OBJ, IMG_OBJS_IMPCONT);
	public static final ImageDescriptor DESC_OBJS_JSEARCH= createManaged(T_OBJ, IMG_OBJS_JSEARCH);
	public static final ImageDescriptor DESC_OBJS_SEARCH_DECL= createManaged(T_OBJ, IMG_OBJS_SEARCH_DECL);
	public static final ImageDescriptor DESC_OBJS_SEARCH_REF= createManaged(T_OBJ, IMG_OBJS_SEARCH_REF);
	public static final ImageDescriptor DESC_OBJS_CLASS= createManaged(T_OBJ, IMG_OBJS_CLASS);
	public static final ImageDescriptor DESC_OBJS_PCLASS= createManaged(T_OBJ, IMG_OBJS_PCLASS);
	public static final ImageDescriptor DESC_OBJS_INTERFACE= createManaged(T_OBJ, IMG_OBJS_INTERFACE);
	public static final ImageDescriptor DESC_OBJS_PINTERFACE= createManaged(T_OBJ, IMG_OBJS_PINTERFACE);
	public static final ImageDescriptor DESC_OBJS_CUNIT= createManaged(T_OBJ, IMG_OBJS_CUNIT);
	public static final ImageDescriptor DESC_OBJS_CFILE= createManaged(T_OBJ, IMG_OBJS_CFILE); 
	public static final ImageDescriptor DESC_OBJS_CFILECLASS= createManaged(T_OBJ, IMG_OBJS_CFILECLASS); 
	public static final ImageDescriptor DESC_OBJS_CFILEINT= createManaged(T_OBJ, IMG_OBJS_CFILEINT); 
	public static final ImageDescriptor DESC_OBJS_PACKAGE= createManaged(T_OBJ, IMG_OBJS_PACKAGE);
	public static final ImageDescriptor DESC_OBJS_PACKFRAG_ROOT= createManaged(T_OBJ, IMG_OBJS_PACKFRAG_ROOT);

	public static final ImageDescriptor DESC_OBJS_JAR= createManaged(T_OBJ, IMG_OBJS_JAR);
	public static final ImageDescriptor DESC_OBJS_EXTJAR= createManaged(T_OBJ, IMG_OBJS_EXTJAR);
	public static final ImageDescriptor DESC_OBJS_JAR_WSRC= createManaged(T_OBJ, IMG_OBJS_JAR_WSRC);
	public static final ImageDescriptor DESC_OBJS_EXTJAR_WSRC= createManaged(T_OBJ, IMG_OBJS_EXTJAR_WSRC);
	public static final ImageDescriptor DESC_OBJS_ENV_VAR= createManaged(T_OBJ, IMG_OBJS_ENV_VAR);
		
	public static final ImageDescriptor DESC_OBJS_JAVADOCTAG= createManaged(T_OBJ, IMG_OBJS_ENV_VAR);
	public static final ImageDescriptor DESC_OBJS_HTMLTAG= createManaged(T_OBJ, IMG_OBJS_HTMLTAG);

	public static final ImageDescriptor DESC_OBJS_EXCEPTION= createManaged(T_OBJ, IMG_OBJS_EXCEPTION);
	public static final ImageDescriptor DESC_OBJS_BREAKPOINT_INSTALLED= createManaged(T_OBJ, IMG_OBJS_BREAKPOINT_INSTALLED);
	public static final ImageDescriptor DESC_OBJS_ERROR= createManaged(T_OBJ, IMG_OBJS_ERROR);
	public static final ImageDescriptor DESC_OBJS_STACK_FRAME_SYNCH= createManaged(T_OBJ, IMG_OBJS_STACK_FRAME_SYNCH);

	public static final ImageDescriptor DESC_OBJS_REFACTORING_FATAL= createManaged(T_OBJ, IMG_OBJS_REFACTORING_FATAL);
	public static final ImageDescriptor DESC_OBJS_REFACTORING_ERROR= createManaged(T_OBJ, IMG_OBJS_REFACTORING_ERROR);
	public static final ImageDescriptor DESC_OBJS_REFACTORING_WARNING= createManaged(T_OBJ, IMG_OBJS_REFACTORING_WARNING);
	public static final ImageDescriptor DESC_OBJS_REFACTORING_INFO= createManaged(T_OBJ, IMG_OBJS_REFACTORING_INFO);
	
	public static final ImageDescriptor DESC_OVR_STATIC= create(T_OVR, "static_co.gif");
	public static final ImageDescriptor DESC_OVR_FINAL= create(T_OVR, "final_co.gif");
	public static final ImageDescriptor DESC_OVR_ABSTRACT= create(T_OVR, "abstract_co.gif");
	public static final ImageDescriptor DESC_OVR_SYNCH= create(T_OVR, "synch_co.gif");
	public static final ImageDescriptor DESC_OVR_RUN= create(T_OVR, "run_co.gif");
	public static final ImageDescriptor DESC_OVR_WARNING= create(T_OVR, "warning_co.gif");
	public static final ImageDescriptor DESC_OVR_ERROR= create(T_OVR, "error_co.gif");
		
	public static final ImageDescriptor DESC_WIZBAN_NEWCLASS= create(T_WIZBAN, "newclass_wiz.gif");
	public static final ImageDescriptor DESC_WIZBAN_NEWFIELD= create(T_WIZBAN, "newfield_wiz.gif");
	public static final ImageDescriptor DESC_WIZBAN_NEWINT= create(T_WIZBAN, "newint_wiz.gif");
	public static final ImageDescriptor DESC_WIZBAN_NEWJPRJ= create(T_WIZBAN, "newjprj_wiz.gif");
	public static final ImageDescriptor DESC_WIZBAN_NEWMETH= create(T_WIZBAN, "newmeth_wiz.gif");
	public static final ImageDescriptor DESC_WIZBAN_NEWPACK= create(T_WIZBAN, "newpack_wiz.gif");
	public static final ImageDescriptor DESC_WIZBAN_JAVA_LAUNCH= create(T_WIZBAN, "java_app_wiz.gif");
	public static final ImageDescriptor DESC_WIZBAN_JAVA_ATTACH= create(T_WIZBAN, "java_attach_wiz.gif");
	
	public static final ImageDescriptor DESC_TOOL_DISPLAYSNIPPET= create(T_CTOOL, "disp_sbook.gif");
	public static final ImageDescriptor DESC_TOOL_RUNSNIPPET= create(T_CTOOL, "run_sbook.gif");
	public static final ImageDescriptor DESC_TOOL_INSPSNIPPET= create(T_CTOOL, "insp_sbook.gif");
	public static final ImageDescriptor DESC_TOOL_PACKSNIPPET= create(T_CTOOL, "pack_sbook.gif");
	public static final ImageDescriptor DESC_TOOL_TERMSNIPPET= create(T_CTOOL, "term_sbook.gif");
	public static final ImageDescriptor DESC_TOOL_SHOW_EMPTY_PKG= create(T_CTOOL, "show_empty_pkg.gif");
	public static final ImageDescriptor DESC_TOOL_SHOW_SEGMENTS= create(T_CTOOL, "segment_edit.gif");
	public static final ImageDescriptor DESC_TOOL_GOTO_NEXT_ERROR= create(T_CTOOL, "next_error_nav.gif");
	public static final ImageDescriptor DESC_TOOL_GOTO_PREV_ERROR= create(T_CTOOL, "prev_error_nav.gif");

	public static final ImageDescriptor DESC_TOOL_OPENTYPE= create(T_CTOOL, "opentype.gif");
	public static final ImageDescriptor DESC_TOOL_NEWPROJECT= create(T_CTOOL, "newjprj_wiz.gif");
	public static final ImageDescriptor DESC_TOOL_NEWPACKAGE= create(T_CTOOL, "newpack_wiz.gif");
	public static final ImageDescriptor DESC_TOOL_NEWCLASS= create(T_CTOOL, "newclass_wiz.gif");
	public static final ImageDescriptor DESC_TOOL_NEWINTERFACE= create(T_CTOOL, "newint_wiz.gif");

	public static final ImageDescriptor DESC_TOOL_CLASSPATH_ORDER= create(T_OBJ, "cp_order_obj.gif");
		
	public static Image get(String key) {
		return IMAGE_REGISTRY.get(key);
	}
	
	/* package */ static ImageRegistry getImageRegistry() {
		return IMAGE_REGISTRY;
	}

	private static ImageDescriptor createManaged(String prefix, String name) {
		try {
			ImageDescriptor result= ImageDescriptor.createFromURL(makeIconFileURL(prefix, name.substring(NAME_PREFIX_LENGTH)));
			IMAGE_REGISTRY.put(name, result);
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