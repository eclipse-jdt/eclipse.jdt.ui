/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ui.model.IWorkbenchAdapter;

import org.eclipse.jdt.core.BindingKey;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.viewsupport.StorageLabelProvider;

/**
 * <code>JavaElementLabels</code> provides helper methods to render names of Java elements.
 * 
 * @since 3.1
 */
public class JavaElementLabels {
	
	
	/**
	 * Method names contain parameter types.
	 * e.g. <code>foo(int)</code>
	 */
	public final static long M_PARAMETER_TYPES= 1L << 0;
	
	/**
	 * Method names contain parameter names.
	 * e.g. <code>foo(index)</code>
	 */
	public final static long M_PARAMETER_NAMES= 1L << 1;	
	
	/**
	 * Method names contain type parameters prepended.
	 * e.g. <code>&lt;A&gt; foo(A index)</code>
	 */
	public final static long M_PRE_TYPE_PARAMETERS= 1L << 2;
	
	/**
	 * Method names contain type parameters appended. 
	 * e.g. <code>foo(A index) &lt;A&gt;</code>
	 */
	public final static long M_APP_TYPE_PARAMETERS= 1L << 3;
	
	/**
	 * Method names contain thrown exceptions.
	 * e.g. <code>foo throws IOException</code>
	 */
	public final static long M_EXCEPTIONS= 1L << 4;
	
	/**
	 * Method names contain return type (appended)
	 * e.g. <code>foo : int</code>
	 */
	public final static long M_APP_RETURNTYPE= 1L << 5;
	
	/**
	 * Method names contain return type (appended)
	 * e.g. <code>int foo</code>
	 */
	public final static long M_PRE_RETURNTYPE= 1L << 6;	

	/**
	 * Method names are fully qualified.
	 * e.g. <code>java.util.Vector.size</code>
	 */
	public final static long M_FULLY_QUALIFIED= 1L << 7;
	
	/**
	 * Method names are post qualified.
	 * e.g. <code>size - java.util.Vector</code>
	 */
	public final static long M_POST_QUALIFIED= 1L << 8;
	
	/**
	 * Initializer names are fully qualified.
	 * e.g. <code>java.util.Vector.{ ... }</code>
	 */
	public final static long I_FULLY_QUALIFIED= 1L << 10;
	
	/**
	 * Type names are post qualified.
	 * e.g. <code>{ ... } - java.util.Map</code>
	 */
	public final static long I_POST_QUALIFIED= 1L << 11;		
	
	/**
	 * Field names contain the declared type (appended)
	 * e.g. <code>fHello : int</code>
	 */
	public final static long F_APP_TYPE_SIGNATURE= 1L << 14;
	
	/**
	 * Field names contain the declared type (prepended)
	 * e.g. <code>int fHello</code>
	 */
	public final static long F_PRE_TYPE_SIGNATURE= 1L << 15;	

	/**
	 * Fields names are fully qualified.
	 * e.g. <code>java.lang.System.out</code>
	 */
	public final static long F_FULLY_QUALIFIED= 1L << 16;
	
	/**
	 * Fields names are post qualified.
	 * e.g. <code>out - java.lang.System</code>
	 */
	public final static long F_POST_QUALIFIED= 1L << 17;	
	
	/**
	 * Type names are fully qualified.
	 * e.g. <code>java.util.Map.MapEntry</code>
	 */
	public final static long T_FULLY_QUALIFIED= 1L << 18;
	
	/**
	 * Type names are type container qualified.
	 * e.g. <code>Map.MapEntry</code>
	 */
	public final static long T_CONTAINER_QUALIFIED= 1L << 19;
	
	/**
	 * Type names are post qualified.
	 * e.g. <code>MapEntry - java.util.Map</code>
	 */
	public final static long T_POST_QUALIFIED= 1L << 20;
	
	/**
	 * Type names contain type parameters.
	 * e.g. <code>Map&lt;S, T&gt;</code>
	 */
	public final static long T_TYPE_PARAMETERS= 1L << 21;
	
	/**
	 * Declarations (import container / declaration, package declaration) are qualified.
	 * e.g. <code>java.util.Vector.class/import container</code>
	 */	
	public final static long D_QUALIFIED= 1L << 24;
	
	/**
	 * Declarations (import container / declaration, package declaration) are post qualified.
	 * e.g. <code>import container - java.util.Vector.class</code>
	 */	
	public final static long D_POST_QUALIFIED= 1L << 25;	

	/**
	 * Class file names are fully qualified.
	 * e.g. <code>java.util.Vector.class</code>
	 */	
	public final static long CF_QUALIFIED= 1L << 27;
	
	/**
	 * Class file names are post qualified.
	 * e.g. <code>Vector.class - java.util</code>
	 */	
	public final static long CF_POST_QUALIFIED= 1L << 28;
	
	/**
	 * Compilation unit names are fully qualified.
	 * e.g. <code>java.util.Vector.java</code>
	 */	
	public final static long CU_QUALIFIED= 1L << 31;
	
	/**
	 * Compilation unit names are post  qualified.
	 * e.g. <code>Vector.java - java.util</code>
	 */	
	public final static long CU_POST_QUALIFIED= 1L << 32;

	/**
	 * Package names are qualified.
	 * e.g. <code>MyProject/src/java.util</code>
	 */	
	public final static long P_QUALIFIED= 1L << 35;
	
	/**
	 * Package names are post qualified.
	 * e.g. <code>java.util - MyProject/src</code>
	 */	
	public final static long P_POST_QUALIFIED= 1L << 36;
	
	/**
	 * Package names are compressed.
	 * e.g. <code>o*.e*.search</code>
	 */	
	public final static long P_COMPRESSED= 1L << 37;

	/**
	 * Package Fragment Roots contain variable name if from a variable.
	 * e.g. <code>JRE_LIB - c:\java\lib\rt.jar</code>
	 */
	public final static long ROOT_VARIABLE= 1L << 40;
	
	/**
	 * Package Fragment Roots contain the project name if not an archive (prepended).
	 * e.g. <code>MyProject/src</code>
	 */
	public final static long ROOT_QUALIFIED= 1L << 41;
	
	/**
	 * Package Fragment Roots contain the project name if not an archive (appended).
	 * e.g. <code>src - MyProject</code>
	 */
	public final static long ROOT_POST_QUALIFIED= 1L << 42;	
	
	/**
	 * Add root path to all elements except Package Fragment Roots and Java projects.
	 * e.g. <code>java.lang.Vector - c:\java\lib\rt.jar</code>
	 * Option only applies to getElementLabel
	 */
	public final static long APPEND_ROOT_PATH= 1L << 43;

	/**
	 * Add root path to all elements except Package Fragment Roots and Java projects.
	 * e.g. <code>java.lang.Vector - c:\java\lib\rt.jar</code>
	 * Option only applies to getElementLabel
	 */
	public final static long PREPEND_ROOT_PATH= 1L << 44;

	/**
	 * Post qualify referenced package fragment roots. For example
	 * <code>jdt.jar - org.eclipse.jdt.ui</code> if the jar is referenced
	 * from another project.
	 */
	public final static long REFERENCED_ROOT_POST_QUALIFIED= 1L << 45; 
	
	/**
	 * Specified to use the resolved information of a IType, IMethod or IField. See {@link IType#isResolved()}.
	 * If resolved information is available, types will be rendered with type parameters of the instantiated type.
	 * Resolved method render with the parameter types of the method instance.
	 * <code>Vector&lt;String&gt;.get(String)</code>
	 */
	public final static long USE_RESOLVED= 1L << 48;
	
	/**
	 * Prepend first category (if any) to field.
	 * @since 3.2 
	 */
	public final static long F_CATEGORY= 1L << 49;
	/**
	 * Prepend first category (if any) to method.
	 * @since 3.2
	 */
	public final static long M_CATEGORY= 1L << 50;
	/**
	 * Prepend first category (if any) to type.
	 * @since 3.2 
	 */
	public final static long T_CATEGORY= 1L << 51;
	
	/**
	 * Show category for all elements.
	 * @since 3.2
	 */
	public final static long ALL_CATEGORY= new Long(JavaElementLabels.F_CATEGORY | JavaElementLabels.M_CATEGORY | JavaElementLabels.T_CATEGORY).longValue();
	
	/**
	 * Qualify all elements
	 */
	public final static long ALL_FULLY_QUALIFIED= new Long(F_FULLY_QUALIFIED | M_FULLY_QUALIFIED | I_FULLY_QUALIFIED | T_FULLY_QUALIFIED | D_QUALIFIED | CF_QUALIFIED | CU_QUALIFIED | P_QUALIFIED | ROOT_QUALIFIED).longValue();

	
	/**
	 * Post qualify all elements
	 */
	public final static long ALL_POST_QUALIFIED= new Long(F_POST_QUALIFIED | M_POST_QUALIFIED | I_POST_QUALIFIED | T_POST_QUALIFIED | D_POST_QUALIFIED | CF_POST_QUALIFIED | CU_POST_QUALIFIED | P_POST_QUALIFIED | ROOT_POST_QUALIFIED).longValue();

	/**
	 *  Default options (M_PARAMETER_TYPES,  M_APP_TYPE_PARAMETERS & T_TYPE_PARAMETERS enabled)
	 */
	public final static long ALL_DEFAULT= new Long(M_PARAMETER_TYPES | M_APP_TYPE_PARAMETERS | T_TYPE_PARAMETERS).longValue();

	/**
	 *  Default qualify options (All except Root and Package)
	 */
	public final static long DEFAULT_QUALIFIED= new Long(F_FULLY_QUALIFIED | M_FULLY_QUALIFIED | I_FULLY_QUALIFIED | T_FULLY_QUALIFIED | D_QUALIFIED | CF_QUALIFIED | CU_QUALIFIED).longValue();

	/**
	 *  Default post qualify options (All except Root and Package)
	 */
	public final static long DEFAULT_POST_QUALIFIED= new Long(F_POST_QUALIFIED | M_POST_QUALIFIED | I_POST_QUALIFIED | T_POST_QUALIFIED | D_POST_QUALIFIED | CF_POST_QUALIFIED | CU_POST_QUALIFIED).longValue();

	/**
	 * User-readable string for separating post qualified names (e.g. " - ").
	 */
	public final static String CONCAT_STRING= JavaUIMessages.JavaElementLabels_concat_string; 
	/**
	 * User-readable string for separating list items (e.g. ", ").
	 */
	public final static String COMMA_STRING= JavaUIMessages.JavaElementLabels_comma_string; 
	/**
	 * User-readable string for separating the return type (e.g. " : ").
	 */
	public final static String DECL_STRING= JavaUIMessages.JavaElementLabels_declseparator_string; 
	/**
	 * User-readable string for concatenating categories (e.g. " ").
	 * XXX: to be made API post 3.2
	 * @since 3.2
	 */
	private final static String CATEGORY_SEPARATOR_STRING= JavaUIMessages.JavaElementLabels_category_separator_string; 
	/**
	 * User-readable string for ellipsis ("...").
	 */
	public final static String ELLIPSIS_STRING= "..."; //$NON-NLS-1$
	/**
	 * User-readable string for the default package name (e.g. "(default package)").
	 */
	public final static String DEFAULT_PACKAGE= JavaUIMessages.JavaElementLabels_default_package; 
	
	
	private final static long QUALIFIER_FLAGS= P_COMPRESSED | USE_RESOLVED;
	
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

	private static final boolean getFlag(long flags, long flag) {
		return (flags & flag) != 0;
	}
	
	/**
	 * Returns the label of the given object. The object must be of type {@link IJavaElement} or adapt to {@link IWorkbenchAdapter}. The empty string is returned
	 * if the element type is not known.
	 * @param obj Object to get the label from.
	 * @param flags The rendering flags
	 * @return Returns the label or the empty string if the object type is not supported.
	 */
	public static String getTextLabel(Object obj, long flags) {
		if (obj instanceof IJavaElement) {
			return getElementLabel((IJavaElement) obj, flags);
		} else if (obj instanceof IResource) {
			return ((IResource) obj).getName();
		} else if (obj instanceof IStorage) {
			return new StorageLabelProvider().getText(obj);
		} else if (obj instanceof IAdaptable) {
			IWorkbenchAdapter wbadapter= (IWorkbenchAdapter) ((IAdaptable)obj).getAdapter(IWorkbenchAdapter.class);
			if (wbadapter != null) {
				return wbadapter.getLabel(obj);
			}
		}
		return ""; //$NON-NLS-1$
	}
				
	/**
	 * Returns the label for a Java element with the flags as defined by this class.
	 * @param element The element to render.
	 * @param flags The rendering flags.
	 * @return the label of the Java element
	 */
	public static String getElementLabel(IJavaElement element, long flags) {
		StringBuffer buf= new StringBuffer(60);
		getElementLabel(element, flags, buf);
		return buf.toString();
	}
	
	/**
	 * Returns the label for a Java element with the flags as defined by this class.
	 * @param element The element to render.
	 * @param flags The rendering flags.
	 * @param buf The buffer to append the resulting label to.
	 */
	public static void getElementLabel(IJavaElement element, long flags, StringBuffer buf) {
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
			case IJavaElement.LOCAL_VARIABLE: 
				getLocalVariableLabel((ILocalVariable) element, flags, buf);
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
				getDeclarationLabel(element, flags, buf);
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
	 * Appends the label for a method to a {@link StringBuffer}. Considers the M_* flags.
	 * 	@param method The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'M_' are considered.
	 * @param buf The buffer to append the resulting label to.
	 */		
	public static void getMethodLabel(IMethod method, long flags, StringBuffer buf) {
		try {
			BindingKey resolvedKey= getFlag(flags, USE_RESOLVED) && method.isResolved() ? new BindingKey(method.getKey()) : null;
			String resolvedSig= (resolvedKey != null) ? resolvedKey.toSignature() : null;
			
			// type parameters
			if (getFlag(flags, M_PRE_TYPE_PARAMETERS)) {
				if (resolvedKey != null) {
					if (resolvedKey.isParameterizedMethod()) {
						String[] typeArgRefs= resolvedKey.getTypeArguments();
						if (typeArgRefs.length > 0) {
							getTypeArgumentSignaturesLabel(typeArgRefs, flags, buf);
							buf.append(' ');
						}
					} else {
						String[] typeParameterSigs= Signature.getTypeParameters(resolvedSig);
						if (typeParameterSigs.length > 0) {
							getTypeParameterSignaturesLabel(typeParameterSigs, flags, buf);
							buf.append(' ');
						}
					}
				} else if (method.exists()) {
					ITypeParameter[] typeParameters= method.getTypeParameters();
					if (typeParameters.length > 0) {
						getTypeParametersLabel(typeParameters, flags, buf);
						buf.append(' ');
					}
				}
			}
			
			// return type
			if (getFlag(flags, M_PRE_RETURNTYPE) && method.exists() && !method.isConstructor()) {
				String returnTypeSig= resolvedSig != null ? Signature.getReturnType(resolvedSig) : method.getReturnType();
				getTypeSignatureLabel(returnTypeSig, flags, buf);
				buf.append(' ');
			}
			
			// qualification
			if (getFlag(flags, M_FULLY_QUALIFIED)) {
				getTypeLabel(method.getDeclaringType(), T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), buf);
				buf.append('.');
			}
				
			buf.append(method.getElementName());
			
			// parameters
			buf.append('(');
			if (getFlag(flags, M_PARAMETER_TYPES | M_PARAMETER_NAMES)) {
				String[] types= null;
				int nParams= 0;
				boolean renderVarargs= false;
				if (getFlag(flags, M_PARAMETER_TYPES)) {
					if (resolvedSig != null) {
						types= Signature.getParameterTypes(resolvedSig);
					} else {
						types= method.getParameterTypes();
					}
					nParams= types.length;
					renderVarargs= method.exists() && Flags.isVarargs(method.getFlags());
				}
				String[] names= null;
				if (getFlag(flags, M_PARAMETER_NAMES) && method.exists()) {
					names= method.getParameterNames();
					if (types == null) {
						nParams= names.length;
					} else {
						if (nParams != names.length) {
							// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=99137 and
							// https://bugs.eclipse.org/bugs/show_bug.cgi?id=101029
							// JavaPlugin.logErrorMessage("JavaElementLabels: Number of param types(" + nParams + ") != number of names(" + names.length + "): " + method.getElementName());   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
							names= null; // no names rendered
						}
					}
				}
				
				for (int i= 0; i < nParams; i++) {
					if (i > 0) {
						buf.append(COMMA_STRING);
					}
					if (types != null) {
						String paramSig= types[i];
						if (renderVarargs && (i == nParams - 1)) {
							int newDim= Signature.getArrayCount(paramSig) - 1;
							getTypeSignatureLabel(Signature.getElementType(paramSig), flags, buf);
							for (int k= 0; k < newDim; k++) {
								buf.append('[').append(']');
							}
							buf.append(ELLIPSIS_STRING);
						} else {
							getTypeSignatureLabel(paramSig, flags, buf);
						}
					}
					if (names != null) {
						if (types != null) {
							buf.append(' ');
						}
						buf.append(names[i]);
					}
				}
			} else {
				if (method.getParameterTypes().length > 0) {
					buf.append(ELLIPSIS_STRING);
				}
			}
			buf.append(')');
					
			if (getFlag(flags, M_EXCEPTIONS)) {
				String[] types;
				if (resolvedKey != null) {
					types= resolvedKey.getThrownExceptions();
				} else {
					types= method.exists() ? method.getExceptionTypes() : new String[0];
				}
				if (types.length > 0) {
					buf.append(" throws "); //$NON-NLS-1$
					for (int i= 0; i < types.length; i++) {
						if (i > 0) {
							buf.append(COMMA_STRING);
						}
						getTypeSignatureLabel(types[i], flags, buf);
					}
				}
			}
			
			if (getFlag(flags, M_APP_TYPE_PARAMETERS)) {
				if (resolvedKey != null) {
					if (resolvedKey.isParameterizedMethod()) {
						String[] typeArgRefs= resolvedKey.getTypeArguments();
						if (typeArgRefs.length > 0) {
							buf.append(' ');
							getTypeArgumentSignaturesLabel(typeArgRefs, flags, buf);
						}
					} else {
						String[] typeParameterSigs= Signature.getTypeParameters(resolvedSig);
						if (typeParameterSigs.length > 0) {
							buf.append(' ');
							getTypeParameterSignaturesLabel(typeParameterSigs, flags, buf);
						}
					}
				} else if (method.exists()) {
					ITypeParameter[] typeParameters= method.getTypeParameters();
					if (typeParameters.length > 0) {
						buf.append(' ');
						getTypeParametersLabel(typeParameters, flags, buf);
					}
				}					
			}
			
			if (getFlag(flags, M_APP_RETURNTYPE) && method.exists() && !method.isConstructor()) {
				buf.append(DECL_STRING);
				String returnTypeSig= resolvedSig != null ? Signature.getReturnType(resolvedSig) : method.getReturnType();
				getTypeSignatureLabel(returnTypeSig, flags, buf);
			}			

			// category
			if (getFlag(flags, M_CATEGORY) && method.exists()) 
				getCategoryLabel(method, buf);
			
			// post qualification
			if (getFlag(flags, M_POST_QUALIFIED)) {
				buf.append(CONCAT_STRING);
				getTypeLabel(method.getDeclaringType(), T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), buf);
			}
			
		} catch (JavaModelException e) {
			JavaPlugin.log(e); // NotExistsException will not reach this point
		}
	}

	private static void getCategoryLabel(IMember member, StringBuffer buf) throws JavaModelException {
		String[] categories= member.getCategories();
		if (categories.length > 0) {
			StringBuffer categoriesBuf= new StringBuffer(30);
			for (int i= 0; i < categories.length; i++) {
				if (i > 0)
					categoriesBuf.append(CATEGORY_SEPARATOR_STRING);
				categoriesBuf.append(categories[i]);
			}
			buf.append(CONCAT_STRING);
			buf.append(Messages.format(JavaUIMessages.JavaElementLabels_category , categoriesBuf.toString()));
		}
	}
	
	private static void getTypeParametersLabel(ITypeParameter[] typeParameters, long flags, StringBuffer buf) {
		if (typeParameters.length > 0) {
			buf.append('<');
			for (int i = 0; i < typeParameters.length; i++) {
				if (i > 0) {
					buf.append(COMMA_STRING);
				}
				buf.append(typeParameters[i].getElementName());
			}
			buf.append('>');
		}
	}
	
	/**
	 * Appends the label for a field to a {@link StringBuffer}. Considers the F_* flags.
	 * 	@param field The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'F_' are considered.
	 * @param buf The buffer to append the resulting label to.
	 */	
	public static void getFieldLabel(IField field, long flags, StringBuffer buf) {
		try {
			
			if (getFlag(flags, F_PRE_TYPE_SIGNATURE) && field.exists() && !Flags.isEnum(field.getFlags())) {
				if (getFlag(flags, USE_RESOLVED) && field.isResolved()) {
					getTypeSignatureLabel(new BindingKey(field.getKey()).toSignature(), flags, buf);
				} else {
					getTypeSignatureLabel(field.getTypeSignature(), flags, buf);
				}
				buf.append(' ');
			}
			
			// qualification
			if (getFlag(flags, F_FULLY_QUALIFIED)) {
				getTypeLabel(field.getDeclaringType(), T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), buf);
				buf.append('.');
			}
			buf.append(field.getElementName());
			
			if (getFlag(flags, F_APP_TYPE_SIGNATURE) && field.exists() && !Flags.isEnum(field.getFlags())) {
				buf.append(DECL_STRING);
				if (getFlag(flags, USE_RESOLVED) && field.isResolved()) {
					getTypeSignatureLabel(new BindingKey(field.getKey()).toSignature(), flags, buf);
				} else {
					getTypeSignatureLabel(field.getTypeSignature(), flags, buf);
				}
			}

			// category
			if (getFlag(flags, F_CATEGORY) && field.exists())
				getCategoryLabel(field, buf);

			// post qualification
			if (getFlag(flags, F_POST_QUALIFIED)) {
				buf.append(CONCAT_STRING);
				getTypeLabel(field.getDeclaringType(), T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), buf);
			}

		} catch (JavaModelException e) {
			JavaPlugin.log(e); // NotExistsException will not reach this point
		}			
	}
	
	/**
	 * Appends the label for a local variable to a {@link StringBuffer}.
	 * 	@param localVariable The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'F_' are considered.
	 * @param buf The buffer to append the resulting label to.
	 */	
	public static void getLocalVariableLabel(ILocalVariable localVariable, long flags, StringBuffer buf) {
		if (getFlag(flags, F_PRE_TYPE_SIGNATURE)) {
			getTypeSignatureLabel(localVariable.getTypeSignature(), flags, buf);
			buf.append(' ');
		}
		
		if (getFlag(flags, F_FULLY_QUALIFIED)) {
			getElementLabel(localVariable.getParent(), M_PARAMETER_TYPES | M_FULLY_QUALIFIED | T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), buf);
			buf.append('.');
		}
		
		buf.append(localVariable.getElementName());
		
		if (getFlag(flags, F_APP_TYPE_SIGNATURE)) {
			buf.append(DECL_STRING);
			getTypeSignatureLabel(localVariable.getTypeSignature(), flags, buf);
		}
		
		// post qualification
		if (getFlag(flags, F_POST_QUALIFIED)) {
			buf.append(CONCAT_STRING);
			getElementLabel(localVariable.getParent(), M_PARAMETER_TYPES | M_FULLY_QUALIFIED | T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), buf);
		}
	}
	
	/**
	 * Appends the label for a initializer to a {@link StringBuffer}. Considers the I_* flags.
	 * 	@param initializer The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'I_' are considered.
	 * @param buf The buffer to append the resulting label to.
	 */	
	public static void getInitializerLabel(IInitializer initializer, long flags, StringBuffer buf) {
		// qualification
		if (getFlag(flags, I_FULLY_QUALIFIED)) {
			getTypeLabel(initializer.getDeclaringType(), T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), buf);
			buf.append('.');
		}
		buf.append(JavaUIMessages.JavaElementLabels_initializer); 

		// post qualification
		if (getFlag(flags, I_POST_QUALIFIED)) {
			buf.append(CONCAT_STRING);
			getTypeLabel(initializer.getDeclaringType(), T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), buf);
		}
	}
	
	private static void getTypeSignatureLabel(String typeSig, long flags, StringBuffer buf) {
		int sigKind= Signature.getTypeSignatureKind(typeSig);
		switch (sigKind) {
			case Signature.BASE_TYPE_SIGNATURE:
				buf.append(Signature.toString(typeSig));
				break;
			case Signature.ARRAY_TYPE_SIGNATURE:
				getTypeSignatureLabel(Signature.getElementType(typeSig), flags, buf);
				for (int dim= Signature.getArrayCount(typeSig); dim > 0; dim--) {
					buf.append('[').append(']');
				}
				break;
			case Signature.CLASS_TYPE_SIGNATURE:
				String baseType= Signature.toString(Signature.getTypeErasure(typeSig));
				buf.append(Signature.getSimpleName(baseType));
				
				String[] typeArguments= Signature.getTypeArguments(typeSig);
				getTypeArgumentSignaturesLabel(typeArguments, flags, buf);
				break;
			case Signature.TYPE_VARIABLE_SIGNATURE:
				buf.append(Signature.toString(typeSig));
				break;
			case Signature.WILDCARD_TYPE_SIGNATURE:
				char ch= typeSig.charAt(0);
				if (ch == Signature.C_STAR) { //workaround for bug 85713
					buf.append('?');
				} else {
					if (ch == Signature.C_EXTENDS) {
						buf.append("? extends "); //$NON-NLS-1$
						getTypeSignatureLabel(typeSig.substring(1), flags, buf);
					} else if (ch == Signature.C_SUPER) {
						buf.append("? super "); //$NON-NLS-1$
						getTypeSignatureLabel(typeSig.substring(1), flags, buf);
					}
				}
				break;
			case Signature.CAPTURE_TYPE_SIGNATURE:
				getTypeSignatureLabel(typeSig.substring(1), flags, buf);
				break;
			default:
				// unknown
		}
	}
	
	private static void getTypeArgumentSignaturesLabel(String[] typeArgsSig, long flags, StringBuffer buf) {
		if (typeArgsSig.length > 0) {
			buf.append('<');
			for (int i = 0; i < typeArgsSig.length; i++) {
				if (i > 0) {
					buf.append(COMMA_STRING);
				}
				getTypeSignatureLabel(typeArgsSig[i], flags, buf);
			}
			buf.append('>');
		}
	}
	
	private static void getTypeParameterSignaturesLabel(String[] typeParamSigs, long flags, StringBuffer buf) {
		if (typeParamSigs.length > 0) {
			buf.append('<');
			for (int i = 0; i < typeParamSigs.length; i++) {
				if (i > 0) {
					buf.append(COMMA_STRING);
				}
				buf.append(Signature.getTypeVariable(typeParamSigs[i]));
			}
			buf.append('>');
		}
	}
	

	/**
	 * Appends the label for a type to a {@link StringBuffer}. Considers the T_* flags.
	 * 	@param type The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'T_' are considered.
	 * @param buf The buffer to append the resulting label to.
	 */		
	public static void getTypeLabel(IType type, long flags, StringBuffer buf) {
		
		if (getFlag(flags, T_FULLY_QUALIFIED)) {
			IPackageFragment pack= type.getPackageFragment();
			if (!pack.isDefaultPackage()) {
				getPackageFragmentLabel(pack, (flags & QUALIFIER_FLAGS), buf);
				buf.append('.');
			}
		}
		if (getFlag(flags, T_FULLY_QUALIFIED | T_CONTAINER_QUALIFIED)) {
			IType declaringType= type.getDeclaringType();
			if (declaringType != null) {
				getTypeLabel(declaringType, T_CONTAINER_QUALIFIED | (flags & QUALIFIER_FLAGS), buf);
				buf.append('.');
			}
			int parentType= type.getParent().getElementType();
			if (parentType == IJavaElement.METHOD || parentType == IJavaElement.FIELD || parentType == IJavaElement.INITIALIZER) { // anonymous or local
				getElementLabel(type.getParent(), 0, buf);
				buf.append('.');
			}
		}
		
		String typeName= type.getElementName();
		if (typeName.length() == 0) { // anonymous
			try {
				if (type.getParent() instanceof IField && type.isEnum()) {
					typeName= '{' + ELLIPSIS_STRING + '}'; 
				} else {
					String supertypeName;
					String[] superInterfaceNames= type.getSuperInterfaceNames();
					if (superInterfaceNames.length > 0) {
						supertypeName= Signature.getSimpleName(superInterfaceNames[0]);
					} else {
						supertypeName= Signature.getSimpleName(type.getSuperclassName());
					}
					typeName= Messages.format(JavaUIMessages.JavaElementLabels_anonym_type , supertypeName); 
				}
			} catch (JavaModelException e) {
				//ignore
				typeName= JavaUIMessages.JavaElementLabels_anonym; 
			}
		}
		buf.append(typeName);
		if (getFlag(flags, T_TYPE_PARAMETERS)) {
			if (getFlag(flags, USE_RESOLVED) && type.isResolved()) {
				BindingKey key= new BindingKey(type.getKey());
				if (key.isParameterizedType()) {
					String[] typeArguments= key.getTypeArguments();
					getTypeArgumentSignaturesLabel(typeArguments, flags, buf);
				} else {
					String[] typeParameters= Signature.getTypeParameters(key.toSignature());
					getTypeParameterSignaturesLabel(typeParameters, flags, buf);
				}
			} else if (type.exists()) {
				try {
					getTypeParametersLabel(type.getTypeParameters(), flags, buf);
				} catch (JavaModelException e) {
					// ignore
				}
			}
		}
		
		// category
		if (getFlag(flags, T_CATEGORY) && type.exists()) {
			try {
				getCategoryLabel(type, buf);
			} catch (JavaModelException e) {
				// ignore
			}
		}

		// post qualification
		if (getFlag(flags, T_POST_QUALIFIED)) {
			buf.append(CONCAT_STRING);
			IType declaringType= type.getDeclaringType();
			if (declaringType != null) {
				getTypeLabel(declaringType, T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), buf);
				int parentType= type.getParent().getElementType();
				if (parentType == IJavaElement.METHOD || parentType == IJavaElement.FIELD || parentType == IJavaElement.INITIALIZER) { // anonymous or local
					buf.append('.');
					getElementLabel(type.getParent(), 0, buf);
				}
			} else {
				getPackageFragmentLabel(type.getPackageFragment(), flags & QUALIFIER_FLAGS, buf);
			}
		}
	}

	/**
	 * Appends the label for a import container, import or package declaration to a {@link StringBuffer}. Considers the D_* flags.
	 * 	@param declaration The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'D_' are considered.
	 * @param buf The buffer to append the resulting label to.
	 */	
	public static void getDeclarationLabel(IJavaElement declaration, long flags, StringBuffer buf) {
		if (getFlag(flags, D_QUALIFIED)) {
			IJavaElement openable= (IJavaElement) declaration.getOpenable();
			if (openable != null) {
				buf.append(getElementLabel(openable, CF_QUALIFIED | CU_QUALIFIED | (flags & QUALIFIER_FLAGS)));
				buf.append('/');
			}	
		}
		if (declaration.getElementType() == IJavaElement.IMPORT_CONTAINER) {
			buf.append(JavaUIMessages.JavaElementLabels_import_container); 
		} else {
			buf.append(declaration.getElementName());
		}
		// post qualification
		if (getFlag(flags, D_POST_QUALIFIED)) {
			IJavaElement openable= (IJavaElement) declaration.getOpenable();
			if (openable != null) {
				buf.append(CONCAT_STRING);
				buf.append(getElementLabel(openable, CF_QUALIFIED | CU_QUALIFIED | (flags & QUALIFIER_FLAGS)));
			}				
		}
	}	
	
	/**
	 * Appends the label for a class file to a {@link StringBuffer}. Considers the CF_* flags.
	 * 	@param classFile The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'CF_' are considered.
	 * @param buf The buffer to append the resulting label to.
	 */	
	public static void getClassFileLabel(IClassFile classFile, long flags, StringBuffer buf) {
		if (getFlag(flags, CF_QUALIFIED)) {
			IPackageFragment pack= (IPackageFragment) classFile.getParent();
			if (!pack.isDefaultPackage()) {
				getPackageFragmentLabel(pack, (flags & QUALIFIER_FLAGS), buf);
				buf.append('.');
			}
		}
		buf.append(classFile.getElementName());
		
		if (getFlag(flags, CF_POST_QUALIFIED)) {
			buf.append(CONCAT_STRING);
			getPackageFragmentLabel((IPackageFragment) classFile.getParent(), flags & QUALIFIER_FLAGS, buf);
		}
	}

	/**
	 * Appends the label for a compilation unit to a {@link StringBuffer}. Considers the CU_* flags.
	 * 	@param cu The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'CU_' are considered.
	 * @param buf The buffer to append the resulting label to.
	 */
	public static void getCompilationUnitLabel(ICompilationUnit cu, long flags, StringBuffer buf) {
		if (getFlag(flags, CU_QUALIFIED)) {
			IPackageFragment pack= (IPackageFragment) cu.getParent();
			if (!pack.isDefaultPackage()) {
				getPackageFragmentLabel(pack, (flags & QUALIFIER_FLAGS), buf);
				buf.append('.');
			}
		}
		buf.append(cu.getElementName());
		
		if (getFlag(flags, CU_POST_QUALIFIED)) {
			buf.append(CONCAT_STRING);
			getPackageFragmentLabel((IPackageFragment) cu.getParent(), flags & QUALIFIER_FLAGS, buf);
		}		
	}

	/**
	 * Appends the label for a package fragment to a {@link StringBuffer}. Considers the P_* flags.
	 * 	@param pack The element to render.
	 * @param flags The rendering flags. Flags with names starting with P_' are considered.
	 * @param buf The buffer to append the resulting label to.
	 */	
	public static void getPackageFragmentLabel(IPackageFragment pack, long flags, StringBuffer buf) {
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
	 * Appends the label for a package fragment root to a {@link StringBuffer}. Considers the ROOT_* flags.
	 * 	@param root The element to render.
	 * @param flags The rendering flags. Flags with names starting with ROOT_' are considered.
	 * @param buf The buffer to append the resulting label to.
	 */	
	public static void getPackageFragmentRootLabel(IPackageFragmentRoot root, long flags, StringBuffer buf) {
		if (root.isArchive())
			getArchiveLabel(root, flags, buf);
		else
			getFolderLabel(root, flags, buf);
	}
	
	private static void getArchiveLabel(IPackageFragmentRoot root, long flags, StringBuffer buf) {
		// Handle variables different	
		if (getFlag(flags, ROOT_VARIABLE) && getVariableLabel(root, flags, buf))
			return;
		boolean external= root.isExternal();
		if (external)
			getExternalArchiveLabel(root, flags, buf);
		else
			getInternalArchiveLabel(root, flags, buf);
	}
	
	private static boolean getVariableLabel(IPackageFragmentRoot root, long flags, StringBuffer buf) {
		try {
			IClasspathEntry rawEntry= root.getRawClasspathEntry();
			if (rawEntry != null && rawEntry.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
				IPath path= rawEntry.getPath().makeRelative();
				if (getFlag(flags, REFERENCED_ROOT_POST_QUALIFIED)) {
					int segements= path.segmentCount();
					if (segements > 0) {
						buf.append(path.segment(segements - 1));
						if (segements > 1) {
							buf.append(CONCAT_STRING);
							buf.append(path.removeLastSegments(1).toOSString());
						}
					} else {
						buf.append(path.toString());
					}
				} else {
					buf.append(path.toString());
				}
				buf.append(CONCAT_STRING);
				if (root.isExternal())
					buf.append(root.getPath().toOSString());
				else
					buf.append(root.getPath().makeRelative().toString());
				return true;
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e); // problems with class path
		}
		return false;
	}

	private static void getExternalArchiveLabel(IPackageFragmentRoot root, long flags, StringBuffer buf) {
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

	private static void getInternalArchiveLabel(IPackageFragmentRoot root, long flags, StringBuffer buf) {
		IResource resource= root.getResource();
		boolean rootQualified= getFlag(flags, ROOT_QUALIFIED);
		boolean referencedQualified= getFlag(flags, REFERENCED_ROOT_POST_QUALIFIED) && isReferenced(root);
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

	private static void getFolderLabel(IPackageFragmentRoot root, long flags, StringBuffer buf) {
		IResource resource= root.getResource();
		boolean rootQualified= getFlag(flags, ROOT_QUALIFIED);
		boolean referencedQualified= getFlag(flags, REFERENCED_ROOT_POST_QUALIFIED) && isReferenced(root);
		if (rootQualified) {
			buf.append(root.getPath().makeRelative().toString());
		} else {
			if (resource != null) {
				IPath projectRelativePath= resource.getProjectRelativePath();
				if (projectRelativePath.segmentCount() == 0) {
					buf.append(resource.getName());
					referencedQualified= false;
				} else {
					buf.append(projectRelativePath.toString());
				}
			} else
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
	
	/**
	 * Returns <code>true</code> if the given package fragment root is
	 * referenced. This means it is own by a different project but is referenced
	 * by the root's parent. Returns <code>false</code> if the given root
	 * doesn't have an underlying resource.
	 *
	 * @since 3.2
	 */
	private static boolean isReferenced(IPackageFragmentRoot root) {
		IResource resource= root.getResource();
		if (resource != null) {
			IProject jarProject= resource.getProject();
			IProject container= root.getJavaProject().getProject();
			return !container.equals(jarProject);
		}
		return false;
	}

	private static void refreshPackageNamePattern() {
		String pattern= getPkgNamePatternForPackagesView();
		final String EMPTY_STRING= ""; //$NON-NLS-1$
		if (pattern.equals(fgPkgNamePattern))
			return;
		else if (pattern.length() == 0) {
			fgPkgNamePattern= EMPTY_STRING;
			fgPkgNameLength= -1;
			return;
		}
		fgPkgNamePattern= pattern;
		int i= 0;
		fgPkgNameChars= 0;
		fgPkgNamePrefix= EMPTY_STRING;
		fgPkgNamePostfix= EMPTY_STRING;
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
	
	/**
	 * Returns the label of a classpath container
	 * @param containerPath The path of the container.
	 * @param project The project the container is resolved in.
	 * @return Returns the label of the classpath container
	 * @throws JavaModelException Thrown when the resolving of the container failed.
	 */
	public static String getContainerEntryLabel(IPath containerPath, IJavaProject project) throws JavaModelException {
		IClasspathContainer container= JavaCore.getClasspathContainer(containerPath, project);
		if (container != null) {
			return container.getDescription();
		}
		ClasspathContainerInitializer initializer= JavaCore.getClasspathContainerInitializer(containerPath.segment(0));
		if (initializer != null) {
			return initializer.getDescription(containerPath, project);
		}
		return containerPath.toString();
	}
	
}
