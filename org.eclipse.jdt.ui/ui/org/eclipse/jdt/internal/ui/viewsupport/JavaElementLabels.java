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
package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ui.model.IWorkbenchAdapter;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;

public class JavaElementLabels {
	
	/**
	 * Method names contain parameter types.
	 * e.g. <code>foo(int)</code>
	 */
	public final static int M_PARAMETER_TYPES= 1 << 0;
	
	/**
	 * Method names contain parameter names.
	 * e.g. <code>foo(index)</code>
	 */
	public final static int M_PARAMETER_NAMES= 1 << 1;	
	
	/**
	 * Method names contain thrown exceptions.
	 * e.g. <code>foo throws IOException</code>
	 */
	public final static int M_EXCEPTIONS= 1 << 2;
	
	/**
	 * Method names contain return type (appended)
	 * e.g. <code>foo : int</code>
	 */
	public final static int M_APP_RETURNTYPE= 1 << 3;
	
	/**
	 * Method names contain return type (appended)
	 * e.g. <code>int foo</code>
	 */
	public final static int M_PRE_RETURNTYPE= 1 << 4;	

	/**
	 * Method names are fully qualified.
	 * e.g. <code>java.util.Vector.size</code>
	 */
	public final static int M_FULLY_QUALIFIED= 1 << 5;
	
	/**
	 * Method names are post qualified.
	 * e.g. <code>size - java.util.Vector</code>
	 */
	public final static int M_POST_QUALIFIED= 1 << 6;
	
	/**
	 * Initializer names are fully qualified.
	 * e.g. <code>java.util.Vector.{ ... }</code>
	 */
	public final static int I_FULLY_QUALIFIED= 1 << 7;
	
	/**
	 * Type names are post qualified.
	 * e.g. <code>{ ... } - java.util.Map</code>
	 */
	public final static int I_POST_QUALIFIED= 1 << 8;		
	
	/**
	 * Field names contain the declared type (appended)
	 * e.g. <code>fHello : int</code>
	 */
	public final static int F_APP_TYPE_SIGNATURE= 1 << 9;
	
	/**
	 * Field names contain the declared type (prepended)
	 * e.g. <code>int fHello</code>
	 */
	public final static int F_PRE_TYPE_SIGNATURE= 1 << 10;	

	/**
	 * Fields names are fully qualified.
	 * e.g. <code>java.lang.System.out</code>
	 */
	public final static int F_FULLY_QUALIFIED= 1 << 11;
	
	/**
	 * Fields names are post qualified.
	 * e.g. <code>out - java.lang.System</code>
	 */
	public final static int F_POST_QUALIFIED= 1 << 12;	
	
	/**
	 * Type names are fully qualified.
	 * e.g. <code>java.util.Map.MapEntry</code>
	 */
	public final static int T_FULLY_QUALIFIED= 1 << 13;
	
	/**
	 * Type names are type container qualified.
	 * e.g. <code>Map.MapEntry</code>
	 */
	public final static int T_CONTAINER_QUALIFIED= 1 << 14;
	
	/**
	 * Type names are post qualified.
	 * e.g. <code>MapEntry - java.util.Map</code>
	 */
	public final static int T_POST_QUALIFIED= 1 << 15;
	
	/**
	 * Declarations (import container / declarartion, package declarartion) are qualified.
	 * e.g. <code>java.util.Vector.class/import container</code>
	 */	
	public final static int D_QUALIFIED= 1 << 16;
	
	/**
	 * Declarations (import container / declarartion, package declarartion) are post qualified.
	 * e.g. <code>import container - java.util.Vector.class</code>
	 */	
	public final static int D_POST_QUALIFIED= 1 << 17;	

	/**
	 * Class file names are fully qualified.
	 * e.g. <code>java.util.Vector.class</code>
	 */	
	public final static int CF_QUALIFIED= 1 << 18;
	
	/**
	 * Class file names are post qualified.
	 * e.g. <code>Vector.class - java.util</code>
	 */	
	public final static int CF_POST_QUALIFIED= 1 << 19;
	
	/**
	 * Compilation unit names are fully qualified.
	 * e.g. <code>java.util.Vector.java</code>
	 */	
	public final static int CU_QUALIFIED= 1 << 20;
	
	/**
	 * Compilation unit names are post  qualified.
	 * e.g. <code>Vector.java - java.util</code>
	 */	
	public final static int CU_POST_QUALIFIED= 1 << 21;

	/**
	 * Package names are qualified.
	 * e.g. <code>MyProject/src/java.util</code>
	 */	
	public final static int P_QUALIFIED= 1 << 22;
	
	/**
	 * Package names are post qualified.
	 * e.g. <code>java.util - MyProject/src</code>
	 */	
	public final static int P_POST_QUALIFIED= 1 << 23;

	/**
	 * Package Fragment Roots contain variable name if from a variable.
	 * e.g. <code>JRE_LIB - c:\java\lib\rt.jar</code>
	 */
	public final static int ROOT_VARIABLE= 1 << 24;
	
	/**
	 * Package Fragment Roots contain the project name if not an archive (prepended).
	 * e.g. <code>MyProject/src</code>
	 */
	public final static int ROOT_QUALIFIED= 1 << 25;
	
	/**
	 * Package Fragment Roots contain the project name if not an archive (appended).
	 * e.g. <code>src - MyProject</code>
	 */
	public final static int ROOT_POST_QUALIFIED= 1 << 26;	
	
	/**
	 * Add root path to all elements except Package Fragment Roots and Java projects.
	 * e.g. <code>java.lang.Vector - c:\java\lib\rt.jar</code>
	 * Option only applies to getElementLabel
	 */
	public final static int APPEND_ROOT_PATH= 1 << 27;

	/**
	 * Add root path to all elements except Package Fragment Roots and Java projects.
	 * e.g. <code>java.lang.Vector - c:\java\lib\rt.jar</code>
	 * Option only applies to getElementLabel
	 */
	public final static int PREPEND_ROOT_PATH= 1 << 28;

	/**
	 * Package names are compressed.
	 * e.g. <code>o*.e*.search</code>
	 */	
	public final static int P_COMPRESSED= 1 << 29;
	
	/**
	 * Post qualify referenced package fragement roots. For example
	 * <code>jdt.jar - org.eclipse.jdt.ui</code> if the jar is referenced
	 * from another project.
	 */
	public final static int REFERENCED_ROOT_POST_QUALIFIED= 1 << 30; 
	
	/**
	 * Qualify all elements
	 */
	public final static int ALL_FULLY_QUALIFIED= F_FULLY_QUALIFIED | M_FULLY_QUALIFIED | I_FULLY_QUALIFIED | T_FULLY_QUALIFIED | D_QUALIFIED | CF_QUALIFIED | CU_QUALIFIED | P_QUALIFIED | ROOT_QUALIFIED;

	/**
	 * Post qualify all elements
	 */
	public final static int ALL_POST_QUALIFIED= F_POST_QUALIFIED | M_POST_QUALIFIED | I_POST_QUALIFIED | T_POST_QUALIFIED | D_POST_QUALIFIED | CF_POST_QUALIFIED | CU_POST_QUALIFIED | P_POST_QUALIFIED | ROOT_POST_QUALIFIED;

	/**
	 *  Default options (M_PARAMETER_TYPES enabled)
	 */
	public final static int ALL_DEFAULT= M_PARAMETER_TYPES;

	/**
	 *  Default qualify options (All except Root and Package)
	 */
	public final static int DEFAULT_QUALIFIED= F_FULLY_QUALIFIED | M_FULLY_QUALIFIED | I_FULLY_QUALIFIED | T_FULLY_QUALIFIED | D_QUALIFIED | CF_QUALIFIED | CU_QUALIFIED;

	/**
	 *  Default post qualify options (All except Root and Package)
	 */
	public final static int DEFAULT_POST_QUALIFIED= F_POST_QUALIFIED | M_POST_QUALIFIED | I_POST_QUALIFIED | T_POST_QUALIFIED | D_POST_QUALIFIED | CF_POST_QUALIFIED | CU_POST_QUALIFIED;


	public final static String CONCAT_STRING= JavaUIMessages.getString("JavaElementLabels.concat_string"); // " - "; //$NON-NLS-1$
	public final static String COMMA_STRING= JavaUIMessages.getString("JavaElementLabels.comma_string"); // ", "; //$NON-NLS-1$
	public final static String DECL_STRING= JavaUIMessages.getString("JavaElementLabels.declseparator_string"); // "  "; // use for return type //$NON-NLS-1$
	public final static String DEFAULT_PACKAGE= JavaUIMessages.getString("JavaElementLabels.default_package"); // "(default package)" //$NON-NLS-1$
	
	/*
	 * Package name compression
	 */
	private static String fgPkgNamePattern= ""; //$NON-NLS-1$
	private static String fgPkgNamePrefix;
	private static String fgPkgNamePostfix;
	private static int fgPkgNameChars;
	private static int fgPkgNameLength= -1;

	private JavaElementLabels() {
	}

	private static boolean getFlag(int flags, int flag) {
		return (flags & flag) != 0;
	}
	
	public static String getTextLabel(Object obj, int flags) {
		if (obj instanceof IJavaElement) {
			return getElementLabel((IJavaElement) obj, flags);
		} else if (obj instanceof IAdaptable) {
			IWorkbenchAdapter wbadapter= (IWorkbenchAdapter) ((IAdaptable)obj).getAdapter(IWorkbenchAdapter.class);
			if (wbadapter != null) {
				return wbadapter.getLabel(obj);
			}
		}
		return ""; //$NON-NLS-1$
	}
				
	/**
	 * Returns the label for a Java element. Flags as defined above.
	 */
	public static String getElementLabel(IJavaElement element, int flags) {
		StringBuffer buf= new StringBuffer(60);
		getElementLabel(element, flags, buf);
		return buf.toString();
	}

	/**
	 * Returns the label for a Java element. Flags as defined above.
	 */
	public static void getElementLabel(IJavaElement element, int flags, StringBuffer buf) {
		int type= element.getElementType();
		IPackageFragmentRoot root= null;
		
		if (type != IJavaElement.JAVA_MODEL && type != IJavaElement.JAVA_PROJECT && type != IJavaElement.PACKAGE_FRAGMENT_ROOT)
			root= JavaModelUtil.getPackageFragmentRoot(element);
		if (root != null && getFlag(flags, PREPEND_ROOT_PATH)) {
			getPackageFragmentRootLabel(root, ROOT_QUALIFIED, buf);
			buf.append(CONCAT_STRING);
		}		
		
		switch (type) {
			case IJavaElement.METHOD:
				getMethodLabel((IMethod) element, flags, buf);
				break;
			case IJavaElement.FIELD: 
				getFieldLabel((IField) element, flags, buf);
				break;
			case IJavaElement.INITIALIZER:
				getInitializerLabel((IInitializer) element, flags, buf);
				break;				
			case IJavaElement.TYPE: 
				getTypeLabel((IType) element, flags, buf);
				break;
			case IJavaElement.CLASS_FILE: 
				getClassFileLabel((IClassFile) element, flags, buf);
				break;					
			case IJavaElement.COMPILATION_UNIT: 
				getCompilationUnitLabel((ICompilationUnit) element, flags, buf);
				break;	
			case IJavaElement.PACKAGE_FRAGMENT: 
				getPackageFragmentLabel((IPackageFragment) element, flags, buf);
				break;
			case IJavaElement.PACKAGE_FRAGMENT_ROOT: 
				getPackageFragmentRootLabel((IPackageFragmentRoot) element, flags, buf);
				break;
			case IJavaElement.IMPORT_CONTAINER:
			case IJavaElement.IMPORT_DECLARATION:
			case IJavaElement.PACKAGE_DECLARATION:
				getDeclararionLabel(element, flags, buf);
				break;
			case IJavaElement.JAVA_PROJECT:
			case IJavaElement.JAVA_MODEL:
				buf.append(element.getElementName());
				break;
			default:
				buf.append(element.getElementName());
		}
		
		if (root != null && getFlag(flags, APPEND_ROOT_PATH)) {
			buf.append(CONCAT_STRING);
			getPackageFragmentRootLabel(root, ROOT_QUALIFIED, buf);
		}
	}

	/**
	 * Appends the label for a method to a StringBuffer. Considers the M_* flags.
	 */		
	public static void getMethodLabel(IMethod method, int flags, StringBuffer buf) {
		try {
			// return type
			if (getFlag(flags, M_PRE_RETURNTYPE) && method.exists() && !method.isConstructor()) {
				buf.append(Signature.getSimpleName(Signature.toString(method.getReturnType())));
				buf.append(' ');
			}
			
			// qualification
			if (getFlag(flags, M_FULLY_QUALIFIED)) {
				getTypeLabel(method.getDeclaringType(), T_FULLY_QUALIFIED | (flags & P_COMPRESSED), buf);
				buf.append('.');
			}
				
			buf.append(method.getElementName());
			
			// parameters
			if (getFlag(flags, M_PARAMETER_TYPES | M_PARAMETER_NAMES)) {
				buf.append('(');
				
				String[] types= getFlag(flags, M_PARAMETER_TYPES) ? method.getParameterTypes() : null;
				String[] names= (getFlag(flags, M_PARAMETER_NAMES) && method.exists()) ? method.getParameterNames() : null;
				int nParams= types != null ? types.length : names.length;
				
				for (int i= 0; i < nParams; i++) {
					if (i > 0) {
						buf.append(COMMA_STRING); //$NON-NLS-1$
					}
					if (types != null) {
						buf.append(Signature.getSimpleName(Signature.toString(types[i])));
					}
					if (names != null) {
						if (types != null) {
							buf.append(' ');
						}
						buf.append(names[i]);
					}
				}
				buf.append(')');
			}
					
			if (getFlag(flags, M_EXCEPTIONS) && method.exists()) {
				String[] types= method.getExceptionTypes();
				if (types.length > 0) {
					buf.append(" throws "); //$NON-NLS-1$
					for (int i= 0; i < types.length; i++) {
						if (i > 0) {
							buf.append(COMMA_STRING);
						}
						buf.append(Signature.getSimpleName(Signature.toString(types[i])));
					}
				}
			}
			
			if (getFlag(flags, M_APP_RETURNTYPE) && method.exists() && !method.isConstructor()) {
				buf.append(DECL_STRING);
				buf.append(Signature.getSimpleName(Signature.toString(method.getReturnType())));	
			}			
			
			// post qualification
			if (getFlag(flags, M_POST_QUALIFIED)) {
				buf.append(CONCAT_STRING);
				getTypeLabel(method.getDeclaringType(), T_FULLY_QUALIFIED | (flags & P_COMPRESSED), buf);
			}			
			
		} catch (JavaModelException e) {
			JavaPlugin.log(e); // NotExistsException will not reach this point
		}
	}
	
	/**
	 * Appends the label for a field to a StringBuffer. Considers the F_* flags.
	 */	
	public static void getFieldLabel(IField field, int flags, StringBuffer buf) {
		try {
			if (getFlag(flags, F_PRE_TYPE_SIGNATURE) && field.exists()) {
				buf.append(Signature.toString(field.getTypeSignature()));
				buf.append(' ');
			}
			
			// qualification
			if (getFlag(flags, F_FULLY_QUALIFIED)) {
				getTypeLabel(field.getDeclaringType(), T_FULLY_QUALIFIED | (flags & P_COMPRESSED), buf);
				buf.append('.');
			}
			buf.append(field.getElementName());
			
			if (getFlag(flags, F_APP_TYPE_SIGNATURE) && field.exists()) {
				buf.append(DECL_STRING);
				buf.append(Signature.toString(field.getTypeSignature()));
			}
			
			// post qualification
			if (getFlag(flags, F_POST_QUALIFIED)) {
				buf.append(CONCAT_STRING);
				getTypeLabel(field.getDeclaringType(), T_FULLY_QUALIFIED | (flags & P_COMPRESSED), buf);
			}
			
		} catch (JavaModelException e) {
			JavaPlugin.log(e); // NotExistsException will not reach this point
		}			
	}

	/**
	 * Appends the label for a initializer to a StringBuffer. Considers the I_* flags.
	 */	
	public static void getInitializerLabel(IInitializer initializer, int flags, StringBuffer buf) {
		// qualification
		if (getFlag(flags, I_FULLY_QUALIFIED)) {
			getTypeLabel(initializer.getDeclaringType(), T_FULLY_QUALIFIED | (flags & P_COMPRESSED), buf);
			buf.append('.');
		}
		buf.append(JavaUIMessages.getString("JavaElementLabels.initializer")); //$NON-NLS-1$

		// post qualification
		if (getFlag(flags, I_POST_QUALIFIED)) {
			buf.append(CONCAT_STRING);
			getTypeLabel(initializer.getDeclaringType(), T_FULLY_QUALIFIED | (flags & P_COMPRESSED), buf);
		}
	}

	/**
	 * Appends the label for a type to a StringBuffer. Considers the T_* flags.
	 */		
	public static void getTypeLabel(IType type, int flags, StringBuffer buf) {
		if (getFlag(flags, T_FULLY_QUALIFIED)) {
			IPackageFragment pack= type.getPackageFragment();
			if (!pack.isDefaultPackage()) {
				getPackageFragmentLabel(pack, (flags & P_COMPRESSED), buf);
				buf.append('.');
			}
			IType declaringType= type.getDeclaringType();
			if (declaringType != null) {
				buf.append(JavaModelUtil.getTypeQualifiedName(declaringType));
				buf.append('.');
			}
		} else if (getFlag(flags, T_CONTAINER_QUALIFIED)) {
			IType declaringType= type.getDeclaringType();
			if (declaringType != null) {
				buf.append(JavaModelUtil.getTypeQualifiedName(declaringType));
				buf.append('.');
			}
		}
		
		String typeName= type.getElementName();
		if (typeName.length() == 0) { // anonymous
			try {
				String superclassName= Signature.getSimpleName(type.getSuperclassName());
				typeName= JavaUIMessages.getFormattedString("JavaElementLabels.anonym_type" , superclassName); //$NON-NLS-1$
			} catch (JavaModelException e) {
				//ignore
				typeName= JavaUIMessages.getString("JavaElementLabels.anonym"); //$NON-NLS-1$
			}
		}
		buf.append(typeName);
		
		// post qualification
		if (getFlag(flags, T_POST_QUALIFIED)) {
			buf.append(CONCAT_STRING);
			IType declaringType= type.getDeclaringType();
			if (declaringType != null) {
				getTypeLabel(declaringType, T_FULLY_QUALIFIED | (flags & P_COMPRESSED), buf);
			} else {
				getPackageFragmentLabel(type.getPackageFragment(), (flags & P_COMPRESSED), buf);
			}
		}
	}

	/**
	 * Appends the label for a declaration to a StringBuffer. Considers the D_* flags.
	 */	
	public static void getDeclararionLabel(IJavaElement declaration, int flags, StringBuffer buf) {
		if (getFlag(flags, D_QUALIFIED)) {
			IJavaElement openable= (IJavaElement) declaration.getOpenable();
			if (openable != null) {
				buf.append(getElementLabel(openable, CF_QUALIFIED | CU_QUALIFIED));
				buf.append('/');
			}	
		}
		if (declaration.getElementType() == IJavaElement.IMPORT_CONTAINER) {
			buf.append(JavaUIMessages.getString("JavaElementLabels.import_container")); //$NON-NLS-1$
		} else {
			buf.append(declaration.getElementName());
		}
		// post qualification
		if (getFlag(flags, D_POST_QUALIFIED)) {
			IJavaElement openable= (IJavaElement) declaration.getOpenable();
			if (openable != null) {
				buf.append(CONCAT_STRING);
				buf.append(getElementLabel(openable, CF_QUALIFIED | CU_QUALIFIED));
			}				
		}
	}	
	
	/**
	 * Appends the label for a class file to a StringBuffer. Considers the CF_* flags.
	 */	
	public static void getClassFileLabel(IClassFile classFile, int flags, StringBuffer buf) {
		if (getFlag(flags, CF_QUALIFIED)) {
			IPackageFragment pack= (IPackageFragment) classFile.getParent();
			if (!pack.isDefaultPackage()) {
				buf.append(pack.getElementName());
				buf.append('.');
			}
		}
		buf.append(classFile.getElementName());
		
		if (getFlag(flags, CF_POST_QUALIFIED)) {
			buf.append(CONCAT_STRING);
			getPackageFragmentLabel((IPackageFragment) classFile.getParent(), 0, buf);
		}
	}

	/**
	 * Appends the label for a compilation unit to a StringBuffer. Considers the CU_* flags.
	 */
	public static void getCompilationUnitLabel(ICompilationUnit cu, int flags, StringBuffer buf) {
		if (getFlag(flags, CU_QUALIFIED)) {
			IPackageFragment pack= (IPackageFragment) cu.getParent();
			if (!pack.isDefaultPackage()) {
				buf.append(pack.getElementName());
				buf.append('.');
			}
		}
		buf.append(cu.getElementName());
		
		if (getFlag(flags, CU_POST_QUALIFIED)) {
			buf.append(CONCAT_STRING);
			getPackageFragmentLabel((IPackageFragment) cu.getParent(), 0, buf);
		}		
	}

	/**
	 * Appends the label for a package fragment to a StringBuffer. Considers the P_* flags.
	 */	
	public static void getPackageFragmentLabel(IPackageFragment pack, int flags, StringBuffer buf) {
		if (getFlag(flags, P_QUALIFIED)) {
			getPackageFragmentRootLabel((IPackageFragmentRoot) pack.getParent(), ROOT_QUALIFIED, buf);
			buf.append('/');
		}
		refreshPackageNamePattern();
		if (pack.isDefaultPackage()) {
			buf.append(DEFAULT_PACKAGE);
		} else if (getFlag(flags, P_COMPRESSED) && fgPkgNameLength >= 0) {
				String name= pack.getElementName();
				int start= 0;
				int dot= name.indexOf('.', start);
				while (dot > 0) {
					if (dot - start > fgPkgNameLength-1) {
						buf.append(fgPkgNamePrefix);
						if (fgPkgNameChars > 0)
							buf.append(name.substring(start, Math.min(start+ fgPkgNameChars, dot)));
						buf.append(fgPkgNamePostfix);
					} else
						buf.append(name.substring(start, dot + 1));
					start= dot + 1;
					dot= name.indexOf('.', start);
				}
				buf.append(name.substring(start));
		} else {
			buf.append(pack.getElementName());
		}
		if (getFlag(flags, P_POST_QUALIFIED)) {
			buf.append(CONCAT_STRING);
			getPackageFragmentRootLabel((IPackageFragmentRoot) pack.getParent(), ROOT_QUALIFIED, buf);
		}
	}

	/**
	 * Appends the label for a package fragment root to a StringBuffer. Considers the ROOT_* flags.
	 */	
	public static void getPackageFragmentRootLabel(IPackageFragmentRoot root, int flags, StringBuffer buf) {
		if (root.isArchive())
			getArchiveLabel(root, flags, buf);
		else
			getFolderLabel(root, flags, buf);
	}
	
	private static void getArchiveLabel(IPackageFragmentRoot root, int flags, StringBuffer buf) {
		// Handle variables different	
		if (getFlag(flags, ROOT_VARIABLE) && getVariableLabel(root, flags, buf))
			return;
		boolean external= root.isExternal();
		if (external)
			getExternalArchiveLabel(root, flags, buf);
		else
			getInternalArchiveLabel(root, flags, buf);
	}
	
	private static boolean getVariableLabel(IPackageFragmentRoot root, int flags, StringBuffer buf) {
		try {
			IClasspathEntry rawEntry= root.getRawClasspathEntry();
			if (rawEntry != null) {
				if (rawEntry.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
					buf.append(rawEntry.getPath().makeRelative());
					buf.append(CONCAT_STRING);
					if (root.isExternal())
						buf.append(root.getPath().toOSString());
					else
						buf.append(root.getPath().makeRelative().toString());
					return true;
				}
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e); // problems with class path
		}
		return false;
	}

	private static void getExternalArchiveLabel(IPackageFragmentRoot root, int flags, StringBuffer buf) {
		IPath path= root.getPath();
		if (getFlag(flags, REFERENCED_ROOT_POST_QUALIFIED)) {
			int segements= path.segmentCount();
			if (segements > 0) {
				buf.append(path.segment(segements - 1));
				if (segements > 1 || path.getDevice() != null) {
					buf.append(CONCAT_STRING);
					buf.append(path.removeLastSegments(1).toOSString());
				}
			} else {
				buf.append(path.toOSString());
			}
		} else {
			buf.append(path.toOSString());
		}
	}

	private static void getInternalArchiveLabel(IPackageFragmentRoot root, int flags, StringBuffer buf) {
		IResource resource= root.getResource();
		boolean rootQualified= getFlag(flags, ROOT_QUALIFIED);
		boolean referencedQualified= getFlag(flags, REFERENCED_ROOT_POST_QUALIFIED) && JavaModelUtil.isReferenced(root) && resource != null;
		if (rootQualified) {
			buf.append(root.getPath().makeRelative().toString());
		} else {
			buf.append(root.getElementName());
			if (referencedQualified) {
				buf.append(CONCAT_STRING);
				buf.append(resource.getParent().getFullPath().makeRelative().toString());
			} else if (getFlag(flags, ROOT_POST_QUALIFIED)) {
				buf.append(CONCAT_STRING);
				buf.append(root.getParent().getPath().makeRelative().toString());
			}
		}
	}

	private static void getFolderLabel(IPackageFragmentRoot root, int flags, StringBuffer buf) {
		IResource resource= root.getResource();
		boolean rootQualified= getFlag(flags, ROOT_QUALIFIED);
		boolean referencedQualified= getFlag(flags, REFERENCED_ROOT_POST_QUALIFIED) && JavaModelUtil.isReferenced(root) && resource != null;
		if (rootQualified) {
			buf.append(root.getPath().makeRelative().toString());
		} else {
			if (resource != null)
				buf.append(resource.getProjectRelativePath().toString());
			else
				buf.append(root.getElementName());
			if (referencedQualified) {
				buf.append(CONCAT_STRING);
				buf.append(resource.getProject().getName());
			} else if (getFlag(flags, ROOT_POST_QUALIFIED)) {
				buf.append(CONCAT_STRING);
				buf.append(root.getParent().getElementName());
			}
		}
	}

	private static void refreshPackageNamePattern() {
		String pattern= getPkgNamePatternForPackagesView();
		if (pattern.equals(fgPkgNamePattern))
			return;
		else if (pattern.equals("")) { //$NON-NLS-1$
			fgPkgNamePattern= ""; //$NON-NLS-1$
			fgPkgNameLength= -1;
			return;
		}
		fgPkgNamePattern= pattern;
		int i= 0;
		fgPkgNameChars= 0;
		fgPkgNamePrefix= ""; //$NON-NLS-1$
		fgPkgNamePostfix= ""; //$NON-NLS-1$
		while (i < pattern.length()) {
			char ch= pattern.charAt(i);
			if (Character.isDigit(ch)) {
				fgPkgNameChars= ch-48;
				if (i > 0)
					fgPkgNamePrefix= pattern.substring(0, i);
				if (i >= 0)
					fgPkgNamePostfix= pattern.substring(i+1);
				fgPkgNameLength= fgPkgNamePrefix.length() + fgPkgNameChars + fgPkgNamePostfix.length();					
				return;
			}
			i++;
		}
		fgPkgNamePrefix= pattern;
		fgPkgNameLength= pattern.length();
	}
	
	private static String getPkgNamePatternForPackagesView() {
		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		if (!store.getBoolean(PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES))
			return ""; //$NON-NLS-1$
		return store.getString(PreferenceConstants.APPEARANCE_PKG_NAME_PATTERN_FOR_PKG_VIEW);
	}	
}
