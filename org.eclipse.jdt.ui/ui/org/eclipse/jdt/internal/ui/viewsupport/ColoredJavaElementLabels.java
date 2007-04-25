/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

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

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.viewsupport.ColoredString.Style;

public class ColoredJavaElementLabels {

	public static final Style QUALIFIER_STYLE= new Style() {
		private Color fColor;
		public Color getForeground(Display display) {
			if (fColor == null)
				fColor= display.getSystemColor(SWT.COLOR_DARK_GRAY);
			return fColor;
		}
	};
	public static final Style COUNTER_STYLE= new Style() {
		private Color fColor;
		public Color getForeground(Display display) {
			if (fColor == null)
				fColor= JavaUI.getColorManager().getColor(new RGB(0, 127, 174));
			return fColor;
		}
	};
	public static final Style DECORATIONS_STYLE= new Style() {
		private Color fColor;
		public Color getForeground(Display display) {
			if (fColor == null)
				fColor= JavaUI.getColorManager().getColor(new RGB(149, 125, 71));
			return fColor;
		}
	};
	
	public final static long COLORIZE= 1L << 55;
	
	private final static long QUALIFIER_FLAGS= JavaElementLabels.P_COMPRESSED | JavaElementLabels.USE_RESOLVED;
	

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
	public static ColoredString getTextLabel(Object obj, long flags) {
		if (obj instanceof IJavaElement) {
			return getElementLabel((IJavaElement) obj, flags);
		} else if (obj instanceof IResource) {
			return new ColoredString(((IResource) obj).getName());
		}
		return new ColoredString(JavaElementLabels.getTextLabel(obj, flags));
	}
				
	/**
	 * Returns the label for a Java element with the flags as defined by this class.
	 * @param element The element to render.
	 * @param flags The rendering flags.
	 * @return the label of the Java element
	 */
	public static ColoredString getElementLabel(IJavaElement element, long flags) {
		ColoredString result= new ColoredString();
		getElementLabel(element, flags, result);
		return result;
	}
	
	/**
	 * Returns the label for a Java element with the flags as defined by this class.
	 * @param element The element to render.
	 * @param flags The rendering flags.
	 * @param result The buffer to append the resulting label to.
	 */
	public static void getElementLabel(IJavaElement element, long flags, ColoredString result) {
		int type= element.getElementType();
		IPackageFragmentRoot root= null;
		
		if (type != IJavaElement.JAVA_MODEL && type != IJavaElement.JAVA_PROJECT && type != IJavaElement.PACKAGE_FRAGMENT_ROOT)
			root= JavaModelUtil.getPackageFragmentRoot(element);
		if (root != null && getFlag(flags, JavaElementLabels.PREPEND_ROOT_PATH)) {
			getPackageFragmentRootLabel(root, JavaElementLabels.ROOT_QUALIFIED, result);
			result.append(JavaElementLabels.CONCAT_STRING);
		}		
		
		switch (type) {
			case IJavaElement.METHOD:
				getMethodLabel((IMethod) element, flags, result);
				break;
			case IJavaElement.FIELD: 
				getFieldLabel((IField) element, flags, result);
				break;
			case IJavaElement.LOCAL_VARIABLE: 
				getLocalVariableLabel((ILocalVariable) element, flags, result);
				break;
			case IJavaElement.INITIALIZER:
				getInitializerLabel((IInitializer) element, flags, result);
				break;				
			case IJavaElement.TYPE: 
				getTypeLabel((IType) element, flags, result);
				break;
			case IJavaElement.CLASS_FILE: 
				getClassFileLabel((IClassFile) element, flags, result);
				break;					
			case IJavaElement.COMPILATION_UNIT: 
				getCompilationUnitLabel((ICompilationUnit) element, flags, result);
				break;	
			case IJavaElement.PACKAGE_FRAGMENT: 
				getPackageFragmentLabel((IPackageFragment) element, flags, result);
				break;
			case IJavaElement.PACKAGE_FRAGMENT_ROOT: 
				getPackageFragmentRootLabel((IPackageFragmentRoot) element, flags, result);
				break;
			case IJavaElement.IMPORT_CONTAINER:
			case IJavaElement.IMPORT_DECLARATION:
			case IJavaElement.PACKAGE_DECLARATION:
				getDeclarationLabel(element, flags, result);
				break;
			case IJavaElement.JAVA_PROJECT:
			case IJavaElement.JAVA_MODEL:
				result.append(element.getElementName());
				break;
			default:
				result.append(element.getElementName());
		}
		
		if (root != null && getFlag(flags, JavaElementLabels.APPEND_ROOT_PATH)) {
			int offset= result.length();
			result.append(JavaElementLabels.CONCAT_STRING);
			getPackageFragmentRootLabel(root, JavaElementLabels.ROOT_QUALIFIED, result);
			
			if (getFlag(flags, COLORIZE)) {
				result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
			}
			
		}
	}

	/**
	 * Appends the label for a method to a {@link ColoredString}. Considers the M_* flags.
	 * 	@param method The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'M_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */		
	public static void getMethodLabel(IMethod method, long flags, ColoredString result) {
		try {
			BindingKey resolvedKey= getFlag(flags, JavaElementLabels.USE_RESOLVED) && method.isResolved() ? new BindingKey(method.getKey()) : null;
			String resolvedSig= (resolvedKey != null) ? resolvedKey.toSignature() : null;
			
			// type parameters
			if (getFlag(flags, JavaElementLabels.M_PRE_TYPE_PARAMETERS)) {
				if (resolvedKey != null) {
					if (resolvedKey.isParameterizedMethod()) {
						String[] typeArgRefs= resolvedKey.getTypeArguments();
						if (typeArgRefs.length > 0) {
							getTypeArgumentSignaturesLabel(typeArgRefs, flags, result);
							result.append(' ');
						}
					} else {
						String[] typeParameterSigs= Signature.getTypeParameters(resolvedSig);
						if (typeParameterSigs.length > 0) {
							getTypeParameterSignaturesLabel(typeParameterSigs, flags, result);
							result.append(' ');
						}
					}
				} else if (method.exists()) {
					ITypeParameter[] typeParameters= method.getTypeParameters();
					if (typeParameters.length > 0) {
						getTypeParametersLabel(typeParameters, flags, result);
						result.append(' ');
					}
				}
			}
			
			// return type
			if (getFlag(flags, JavaElementLabels.M_PRE_RETURNTYPE) && method.exists() && !method.isConstructor()) {
				String returnTypeSig= resolvedSig != null ? Signature.getReturnType(resolvedSig) : method.getReturnType();
				getTypeSignatureLabel(returnTypeSig, flags, result);
				result.append(' ');
			}
			
			// qualification
			if (getFlag(flags, JavaElementLabels.M_FULLY_QUALIFIED)) {
				getTypeLabel(method.getDeclaringType(), JavaElementLabels.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), result);
				result.append('.');
			}
				
			result.append(method.getElementName());
			
			// parameters
			result.append('(');
			if (getFlag(flags, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES)) {
				String[] types= null;
				int nParams= 0;
				boolean renderVarargs= false;
				if (getFlag(flags, JavaElementLabels.M_PARAMETER_TYPES)) {
					if (resolvedSig != null) {
						types= Signature.getParameterTypes(resolvedSig);
					} else {
						types= method.getParameterTypes();
					}
					nParams= types.length;
					renderVarargs= method.exists() && Flags.isVarargs(method.getFlags());
				}
				String[] names= null;
				if (getFlag(flags, JavaElementLabels.M_PARAMETER_NAMES) && method.exists()) {
					names= method.getParameterNames();
					if (types == null) {
						nParams= names.length;
					} else { // types != null
						if (nParams != names.length) {
							if (resolvedSig != null && types.length > names.length) {
								// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=99137
								nParams= names.length;
								String[] typesWithoutSyntheticParams= new String[nParams];
								System.arraycopy(types, types.length - nParams, typesWithoutSyntheticParams, 0, nParams);
								types= typesWithoutSyntheticParams;
							} else {
								// https://bugs.eclipse.org/bugs/show_bug.cgi?id=101029
								// JavaPlugin.logErrorMessage("JavaElementLabels: Number of param types(" + nParams + ") != number of names(" + names.length + "): " + method.getElementName());   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
								names= null; // no names rendered
							}
						}
					}
				}
				
				for (int i= 0; i < nParams; i++) {
					if (i > 0) {
						result.append(JavaElementLabels.COMMA_STRING);
					}
					if (types != null) {
						String paramSig= types[i];
						if (renderVarargs && (i == nParams - 1)) {
							int newDim= Signature.getArrayCount(paramSig) - 1;
							getTypeSignatureLabel(Signature.getElementType(paramSig), flags, result);
							for (int k= 0; k < newDim; k++) {
								result.append('[').append(']');
							}
							result.append(JavaElementLabels.ELLIPSIS_STRING);
						} else {
							getTypeSignatureLabel(paramSig, flags, result);
						}
					}
					if (names != null) {
						if (types != null) {
							result.append(' ');
						}
						result.append(names[i]);
					}
				}
			} else {
				if (method.getParameterTypes().length > 0) {
					result.append(JavaElementLabels.ELLIPSIS_STRING);
				}
			}
			result.append(')');
					
			if (getFlag(flags, JavaElementLabels.M_EXCEPTIONS)) {
				String[] types;
				if (resolvedKey != null) {
					types= resolvedKey.getThrownExceptions();
				} else {
					types= method.exists() ? method.getExceptionTypes() : new String[0];
				}
				if (types.length > 0) {
					result.append(" throws "); //$NON-NLS-1$
					for (int i= 0; i < types.length; i++) {
						if (i > 0) {
							result.append(JavaElementLabels.COMMA_STRING);
						}
						getTypeSignatureLabel(types[i], flags, result);
					}
				}
			}
			
			if (getFlag(flags, JavaElementLabels.M_APP_TYPE_PARAMETERS)) {
				if (resolvedKey != null) {
					if (resolvedKey.isParameterizedMethod()) {
						String[] typeArgRefs= resolvedKey.getTypeArguments();
						if (typeArgRefs.length > 0) {
							result.append(' ');
							getTypeArgumentSignaturesLabel(typeArgRefs, flags, result);
						}
					} else {
						String[] typeParameterSigs= Signature.getTypeParameters(resolvedSig);
						if (typeParameterSigs.length > 0) {
							result.append(' ');
							getTypeParameterSignaturesLabel(typeParameterSigs, flags, result);
						}
					}
				} else if (method.exists()) {
					ITypeParameter[] typeParameters= method.getTypeParameters();
					if (typeParameters.length > 0) {
						result.append(' ');
						getTypeParametersLabel(typeParameters, flags, result);
					}
				}					
			}
			
			if (getFlag(flags, JavaElementLabels.M_APP_RETURNTYPE) && method.exists() && !method.isConstructor()) {
				int offset= result.length();
				result.append(JavaElementLabels.DECL_STRING);
				String returnTypeSig= resolvedSig != null ? Signature.getReturnType(resolvedSig) : method.getReturnType();
				getTypeSignatureLabel(returnTypeSig, flags, result);
				if (getFlag(flags, COLORIZE)) {
					result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
				}
			}			

			// category
			if (getFlag(flags, JavaElementLabels.M_CATEGORY) && method.exists()) 
				getCategoryLabel(method, result);
			
			// post qualification
			if (getFlag(flags, JavaElementLabels.M_POST_QUALIFIED)) {
				int offset= result.length();
				result.append(JavaElementLabels.CONCAT_STRING);
				getTypeLabel(method.getDeclaringType(), JavaElementLabels.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), result);
				if (getFlag(flags, COLORIZE)) {
					result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
				}
			}
			
		} catch (JavaModelException e) {
			JavaPlugin.log(e); // NotExistsException will not reach this point
		}
	}

	private static void getCategoryLabel(IMember member, ColoredString result) throws JavaModelException {
		String[] categories= member.getCategories();
		if (categories.length > 0) {
			ColoredString categoriesBuf= new ColoredString();
			for (int i= 0; i < categories.length; i++) {
				if (i > 0)
					categoriesBuf.append(JavaUIMessages.JavaElementLabels_category_separator_string);
				categoriesBuf.append(categories[i]);
			}
			result.append(JavaElementLabels.CONCAT_STRING);
			result.append(Messages.format(JavaUIMessages.JavaElementLabels_category , categoriesBuf.toString()));
		}
	}
	
	private static void getTypeParametersLabel(ITypeParameter[] typeParameters, long flags, ColoredString result) {
		if (typeParameters.length > 0) {
			result.append('<');
			for (int i = 0; i < typeParameters.length; i++) {
				if (i > 0) {
					result.append(JavaElementLabels.COMMA_STRING);
				}
				result.append(typeParameters[i].getElementName());
			}
			result.append('>');
		}
	}
	
	/**
	 * Appends the label for a field to a {@link ColoredString}. Considers the F_* flags.
	 * 	@param field The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'F_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */	
	public static void getFieldLabel(IField field, long flags, ColoredString result) {
		try {
			
			if (getFlag(flags, JavaElementLabels.F_PRE_TYPE_SIGNATURE) && field.exists() && !Flags.isEnum(field.getFlags())) {
				if (getFlag(flags, JavaElementLabels.USE_RESOLVED) && field.isResolved()) {
					getTypeSignatureLabel(new BindingKey(field.getKey()).toSignature(), flags, result);
				} else {
					getTypeSignatureLabel(field.getTypeSignature(), flags, result);
				}
				result.append(' ');
			}
			
			// qualification
			if (getFlag(flags, JavaElementLabels.F_FULLY_QUALIFIED)) {
				getTypeLabel(field.getDeclaringType(), JavaElementLabels.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), result);
				result.append('.');
			}
			result.append(field.getElementName());
			
			if (getFlag(flags, JavaElementLabels.F_APP_TYPE_SIGNATURE) && field.exists() && !Flags.isEnum(field.getFlags())) {
				int offset= result.length();
				result.append(JavaElementLabels.DECL_STRING);
				if (getFlag(flags, JavaElementLabels.USE_RESOLVED) && field.isResolved()) {
					getTypeSignatureLabel(new BindingKey(field.getKey()).toSignature(), flags, result);
				} else {
					getTypeSignatureLabel(field.getTypeSignature(), flags, result);
				}
				if (getFlag(flags, COLORIZE)) {
					result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
				}
			}

			// category
			if (getFlag(flags, JavaElementLabels.F_CATEGORY) && field.exists())
				getCategoryLabel(field, result);

			// post qualification
			if (getFlag(flags, JavaElementLabels.F_POST_QUALIFIED)) {
				int offset= result.length();
				result.append(JavaElementLabels.CONCAT_STRING);
				getTypeLabel(field.getDeclaringType(), JavaElementLabels.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), result);
				if (getFlag(flags, COLORIZE)) {
					result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
				}
			}

		} catch (JavaModelException e) {
			JavaPlugin.log(e); // NotExistsException will not reach this point
		}			
	}
	
	/**
	 * Appends the label for a local variable to a {@link ColoredString}.
	 * 	@param localVariable The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'F_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */	
	public static void getLocalVariableLabel(ILocalVariable localVariable, long flags, ColoredString result) {
		if (getFlag(flags, JavaElementLabels.F_PRE_TYPE_SIGNATURE)) {
			getTypeSignatureLabel(localVariable.getTypeSignature(), flags, result);
			result.append(' ');
		}
		
		if (getFlag(flags, JavaElementLabels.F_FULLY_QUALIFIED)) {
			getElementLabel(localVariable.getParent(), JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_FULLY_QUALIFIED | JavaElementLabels.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), result);
			result.append('.');
		}
		
		result.append(localVariable.getElementName());
		
		if (getFlag(flags, JavaElementLabels.F_APP_TYPE_SIGNATURE)) {
			int offset= result.length();
			result.append(JavaElementLabels.DECL_STRING);
			getTypeSignatureLabel(localVariable.getTypeSignature(), flags, result);
			if (getFlag(flags, COLORIZE)) {
				result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
			}
		}
		
		// post qualification
		if (getFlag(flags, JavaElementLabels.F_POST_QUALIFIED)) {
			result.append(JavaElementLabels.CONCAT_STRING);
			getElementLabel(localVariable.getParent(), JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_FULLY_QUALIFIED | JavaElementLabels.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), result);
		}
	}
	
	/**
	 * Appends the label for a initializer to a {@link ColoredString}. Considers the I_* flags.
	 * 	@param initializer The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'I_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */	
	public static void getInitializerLabel(IInitializer initializer, long flags, ColoredString result) {
		// qualification
		if (getFlag(flags, JavaElementLabels.I_FULLY_QUALIFIED)) {
			getTypeLabel(initializer.getDeclaringType(), JavaElementLabels.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), result);
			result.append('.');
		}
		result.append(JavaUIMessages.JavaElementLabels_initializer); 

		// post qualification
		if (getFlag(flags, JavaElementLabels.I_POST_QUALIFIED)) {
			int offset= result.length();
			result.append(JavaElementLabels.CONCAT_STRING);
			getTypeLabel(initializer.getDeclaringType(), JavaElementLabels.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), result);
			if (getFlag(flags, COLORIZE)) {
				result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
			}
		}
	}
	
	private static void getTypeSignatureLabel(String typeSig, long flags, ColoredString result) {
		int sigKind= Signature.getTypeSignatureKind(typeSig);
		switch (sigKind) {
			case Signature.BASE_TYPE_SIGNATURE:
				result.append(Signature.toString(typeSig));
				break;
			case Signature.ARRAY_TYPE_SIGNATURE:
				getTypeSignatureLabel(Signature.getElementType(typeSig), flags, result);
				for (int dim= Signature.getArrayCount(typeSig); dim > 0; dim--) {
					result.append('[').append(']');
				}
				break;
			case Signature.CLASS_TYPE_SIGNATURE:
				String baseType= Signature.toString(Signature.getTypeErasure(typeSig));
				result.append(Signature.getSimpleName(baseType));
				
				String[] typeArguments= Signature.getTypeArguments(typeSig);
				getTypeArgumentSignaturesLabel(typeArguments, flags, result);
				break;
			case Signature.TYPE_VARIABLE_SIGNATURE:
				result.append(Signature.toString(typeSig));
				break;
			case Signature.WILDCARD_TYPE_SIGNATURE:
				char ch= typeSig.charAt(0);
				if (ch == Signature.C_STAR) { //workaround for bug 85713
					result.append('?');
				} else {
					if (ch == Signature.C_EXTENDS) {
						result.append("? extends "); //$NON-NLS-1$
						getTypeSignatureLabel(typeSig.substring(1), flags, result);
					} else if (ch == Signature.C_SUPER) {
						result.append("? super "); //$NON-NLS-1$
						getTypeSignatureLabel(typeSig.substring(1), flags, result);
					}
				}
				break;
			case Signature.CAPTURE_TYPE_SIGNATURE:
				getTypeSignatureLabel(typeSig.substring(1), flags, result);
				break;
			default:
				// unknown
		}
	}
	
	private static void getTypeArgumentSignaturesLabel(String[] typeArgsSig, long flags, ColoredString result) {
		if (typeArgsSig.length > 0) {
			result.append('<');
			for (int i = 0; i < typeArgsSig.length; i++) {
				if (i > 0) {
					result.append(JavaElementLabels.COMMA_STRING);
				}
				getTypeSignatureLabel(typeArgsSig[i], flags, result);
			}
			result.append('>');
		}
	}
	
	private static void getTypeParameterSignaturesLabel(String[] typeParamSigs, long flags, ColoredString result) {
		if (typeParamSigs.length > 0) {
			result.append('<');
			for (int i = 0; i < typeParamSigs.length; i++) {
				if (i > 0) {
					result.append(JavaElementLabels.COMMA_STRING);
				}
				result.append(Signature.getTypeVariable(typeParamSigs[i]));
			}
			result.append('>');
		}
	}
	

	/**
	 * Appends the label for a type to a {@link ColoredString}. Considers the T_* flags.
	 * 	@param type The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'T_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */		
	public static void getTypeLabel(IType type, long flags, ColoredString result) {
		
		if (getFlag(flags, JavaElementLabels.T_FULLY_QUALIFIED)) {
			IPackageFragment pack= type.getPackageFragment();
			if (!pack.isDefaultPackage()) {
				getPackageFragmentLabel(pack, (flags & QUALIFIER_FLAGS), result);
				result.append('.');
			}
		}
		if (getFlag(flags, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.T_CONTAINER_QUALIFIED)) {
			IType declaringType= type.getDeclaringType();
			if (declaringType != null) {
				getTypeLabel(declaringType, JavaElementLabels.T_CONTAINER_QUALIFIED | (flags & QUALIFIER_FLAGS), result);
				result.append('.');
			}
			int parentType= type.getParent().getElementType();
			if (parentType == IJavaElement.METHOD || parentType == IJavaElement.FIELD || parentType == IJavaElement.INITIALIZER) { // anonymous or local
				getElementLabel(type.getParent(), 0, result);
				result.append('.');
			}
		}
		
		String typeName= type.getElementName();
		if (typeName.length() == 0) { // anonymous
			try {
				if (type.getParent() instanceof IField && type.isEnum()) {
					typeName= '{' + JavaElementLabels.ELLIPSIS_STRING + '}'; 
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
		result.append(typeName);
		if (getFlag(flags, JavaElementLabels.T_TYPE_PARAMETERS)) {
			if (getFlag(flags, JavaElementLabels.USE_RESOLVED) && type.isResolved()) {
				BindingKey key= new BindingKey(type.getKey());
				if (key.isParameterizedType()) {
					String[] typeArguments= key.getTypeArguments();
					getTypeArgumentSignaturesLabel(typeArguments, flags, result);
				} else {
					String[] typeParameters= Signature.getTypeParameters(key.toSignature());
					getTypeParameterSignaturesLabel(typeParameters, flags, result);
				}
			} else if (type.exists()) {
				try {
					getTypeParametersLabel(type.getTypeParameters(), flags, result);
				} catch (JavaModelException e) {
					// ignore
				}
			}
		}
		
		// category
		if (getFlag(flags, JavaElementLabels.T_CATEGORY) && type.exists()) {
			try {
				getCategoryLabel(type, result);
			} catch (JavaModelException e) {
				// ignore
			}
		}

		// post qualification
		if (getFlag(flags, JavaElementLabels.T_POST_QUALIFIED)) {
			int offset= result.length();
			result.append(JavaElementLabels.CONCAT_STRING);
			IType declaringType= type.getDeclaringType();
			if (declaringType != null) {
				getTypeLabel(declaringType, JavaElementLabels.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), result);
				int parentType= type.getParent().getElementType();
				if (parentType == IJavaElement.METHOD || parentType == IJavaElement.FIELD || parentType == IJavaElement.INITIALIZER) { // anonymous or local
					result.append('.');
					getElementLabel(type.getParent(), 0, result);
				}
			} else {
				getPackageFragmentLabel(type.getPackageFragment(), flags & QUALIFIER_FLAGS, result);
			}
			if (getFlag(flags, COLORIZE)) {
				result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
			}
		}
	}

	/**
	 * Appends the label for a import container, import or package declaration to a {@link ColoredString}. Considers the D_* flags.
	 * 	@param declaration The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'D_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */	
	public static void getDeclarationLabel(IJavaElement declaration, long flags, ColoredString result) {
		if (getFlag(flags, JavaElementLabels.D_QUALIFIED)) {
			IJavaElement openable= (IJavaElement) declaration.getOpenable();
			if (openable != null) {
				result.append(getElementLabel(openable, JavaElementLabels.CF_QUALIFIED | JavaElementLabels.CU_QUALIFIED | (flags & QUALIFIER_FLAGS)));
				result.append('/');
			}	
		}
		if (declaration.getElementType() == IJavaElement.IMPORT_CONTAINER) {
			result.append(JavaUIMessages.JavaElementLabels_import_container); 
		} else {
			result.append(declaration.getElementName());
		}
		// post qualification
		if (getFlag(flags, JavaElementLabels.D_POST_QUALIFIED)) {
			int offset= result.length();
			IJavaElement openable= (IJavaElement) declaration.getOpenable();
			if (openable != null) {
				result.append(JavaElementLabels.CONCAT_STRING);
				result.append(getElementLabel(openable, JavaElementLabels.CF_QUALIFIED | JavaElementLabels.CU_QUALIFIED | (flags & QUALIFIER_FLAGS)));
			}
			if (getFlag(flags, COLORIZE)) {
				result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
			}
		}
	}	
	
	/**
	 * Appends the label for a class file to a {@link ColoredString}. Considers the CF_* flags.
	 * 	@param classFile The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'CF_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */	
	public static void getClassFileLabel(IClassFile classFile, long flags, ColoredString result) {
		if (getFlag(flags, JavaElementLabels.CF_QUALIFIED)) {
			IPackageFragment pack= (IPackageFragment) classFile.getParent();
			if (!pack.isDefaultPackage()) {
				getPackageFragmentLabel(pack, (flags & QUALIFIER_FLAGS), result);
				result.append('.');
			}
		}
		result.append(classFile.getElementName());
		
		if (getFlag(flags, JavaElementLabels.CF_POST_QUALIFIED)) {
			int offset= result.length();
			result.append(JavaElementLabels.CONCAT_STRING);
			getPackageFragmentLabel((IPackageFragment) classFile.getParent(), flags & QUALIFIER_FLAGS, result);
			if (getFlag(flags, COLORIZE)) {
				result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
			}
		}
	}

	/**
	 * Appends the label for a compilation unit to a {@link ColoredString}. Considers the CU_* flags.
	 * 	@param cu The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'CU_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */
	public static void getCompilationUnitLabel(ICompilationUnit cu, long flags, ColoredString result) {
		if (getFlag(flags, JavaElementLabels.CU_QUALIFIED)) {
			IPackageFragment pack= (IPackageFragment) cu.getParent();
			if (!pack.isDefaultPackage()) {
				getPackageFragmentLabel(pack, (flags & QUALIFIER_FLAGS), result);
				result.append('.');
			}
		}
		result.append(cu.getElementName());
		
		if (getFlag(flags, JavaElementLabels.CU_POST_QUALIFIED)) {
			int offset= result.length();
			result.append(JavaElementLabels.CONCAT_STRING);
			getPackageFragmentLabel((IPackageFragment) cu.getParent(), flags & QUALIFIER_FLAGS, result);
			if (getFlag(flags, COLORIZE)) {
				result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
			}
		}		
	}

	/**
	 * Appends the label for a package fragment to a {@link ColoredString}. Considers the P_* flags.
	 * 	@param pack The element to render.
	 * @param flags The rendering flags. Flags with names starting with P_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */	
	public static void getPackageFragmentLabel(IPackageFragment pack, long flags, ColoredString result) {
		if (getFlag(flags, JavaElementLabels.P_QUALIFIED)) {
			getPackageFragmentRootLabel((IPackageFragmentRoot) pack.getParent(), JavaElementLabels.ROOT_QUALIFIED, result);
			result.append('/');
		}
		if (pack.isDefaultPackage()) {
			result.append(JavaElementLabels.DEFAULT_PACKAGE);
		} else if (getFlag(flags, JavaElementLabels.P_COMPRESSED)) {
			StringBuffer buf= new StringBuffer();
			JavaElementLabels.getPackageFragmentLabel(pack, JavaElementLabels.P_COMPRESSED, buf);
			result.append(buf.toString());
		} else {
			result.append(pack.getElementName());
		}
		if (getFlag(flags, JavaElementLabels.P_POST_QUALIFIED)) {
			int offset= result.length();
			result.append(JavaElementLabels.CONCAT_STRING);
			getPackageFragmentRootLabel((IPackageFragmentRoot) pack.getParent(), JavaElementLabels.ROOT_QUALIFIED, result);
			if (getFlag(flags, COLORIZE)) {
				result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
			}
		}
	}

	/**
	 * Appends the label for a package fragment root to a {@link ColoredString}. Considers the ROOT_* flags.
	 * 	@param root The element to render.
	 * @param flags The rendering flags. Flags with names starting with ROOT_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */	
	public static void getPackageFragmentRootLabel(IPackageFragmentRoot root, long flags, ColoredString result) {
		if (root.isArchive())
			getArchiveLabel(root, flags, result);
		else
			getFolderLabel(root, flags, result);
	}
	
	private static void getArchiveLabel(IPackageFragmentRoot root, long flags, ColoredString result) {
		// Handle variables different	
		if (getFlag(flags, JavaElementLabels.ROOT_VARIABLE) && getVariableLabel(root, flags, result))
			return;
		boolean external= root.isExternal();
		if (external)
			getExternalArchiveLabel(root, flags, result);
		else
			getInternalArchiveLabel(root, flags, result);
	}
	
	private static boolean getVariableLabel(IPackageFragmentRoot root, long flags, ColoredString result) {
		try {
			IClasspathEntry rawEntry= root.getRawClasspathEntry();
			if (rawEntry != null && rawEntry.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
				IPath path= rawEntry.getPath().makeRelative();
				int offset= result.length();
				if (getFlag(flags, JavaElementLabels.REFERENCED_ROOT_POST_QUALIFIED)) {
					int segements= path.segmentCount();
					if (segements > 0) {
						result.append(path.segment(segements - 1));
						if (segements > 1) {
							result.append(JavaElementLabels.CONCAT_STRING);
							result.append(path.removeLastSegments(1).toOSString());
						}
					} else {
						result.append(path.toString());
					}
				} else {
					result.append(path.toString());
				}
				result.append(JavaElementLabels.CONCAT_STRING);
				if (root.isExternal())
					result.append(root.getPath().toOSString());
				else
					result.append(root.getPath().makeRelative().toString());
				
				if (getFlag(flags, COLORIZE)) {
					result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
				}
				return true;
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e); // problems with class path
		}
		return false;
	}

	private static void getExternalArchiveLabel(IPackageFragmentRoot root, long flags, ColoredString result) {
		IPath path= root.getPath();
		if (getFlag(flags, JavaElementLabels.REFERENCED_ROOT_POST_QUALIFIED)) {
			int segements= path.segmentCount();
			if (segements > 0) {
				result.append(path.segment(segements - 1));
				int offset= result.length();
				if (segements > 1 || path.getDevice() != null) {
					result.append(JavaElementLabels.CONCAT_STRING);
					result.append(path.removeLastSegments(1).toOSString());
				}
				if (getFlag(flags, COLORIZE)) {
					result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
				}
			} else {
				result.append(path.toOSString());
			}
		} else {
			result.append(path.toOSString());
		}
	}

	private static void getInternalArchiveLabel(IPackageFragmentRoot root, long flags, ColoredString result) {
		IResource resource= root.getResource();
		boolean rootQualified= getFlag(flags, JavaElementLabels.ROOT_QUALIFIED);
		boolean referencedQualified= getFlag(flags, JavaElementLabels.REFERENCED_ROOT_POST_QUALIFIED) && isReferenced(root);
		if (rootQualified) {
			result.append(root.getPath().makeRelative().toString());
		} else {
			result.append(root.getElementName());
			int offset= result.length();
			if (referencedQualified) {
				result.append(JavaElementLabels.CONCAT_STRING);
				result.append(resource.getParent().getFullPath().makeRelative().toString());
			} else if (getFlag(flags, JavaElementLabels.ROOT_POST_QUALIFIED)) {
				result.append(JavaElementLabels.CONCAT_STRING);
				result.append(root.getParent().getPath().makeRelative().toString());
			} else {
				return;
			}
			if (getFlag(flags, COLORIZE)) {
				result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
			}
		}
	}

	private static void getFolderLabel(IPackageFragmentRoot root, long flags, ColoredString result) {
		IResource resource= root.getResource();
		boolean rootQualified= getFlag(flags, JavaElementLabels.ROOT_QUALIFIED);
		boolean referencedQualified= getFlag(flags, JavaElementLabels.REFERENCED_ROOT_POST_QUALIFIED) && isReferenced(root);
		if (rootQualified) {
			result.append(root.getPath().makeRelative().toString());
		} else {
			if (resource != null) {
				IPath projectRelativePath= resource.getProjectRelativePath();
				if (projectRelativePath.segmentCount() == 0) {
					result.append(resource.getName());
					referencedQualified= false;
				} else {
					result.append(projectRelativePath.toString());
				}
			} else
				result.append(root.getElementName());
			int offset= result.length();
			if (referencedQualified) {
				result.append(JavaElementLabels.CONCAT_STRING);
				result.append(resource.getProject().getName());
			} else if (getFlag(flags, JavaElementLabels.ROOT_POST_QUALIFIED)) {
				result.append(JavaElementLabels.CONCAT_STRING);
				result.append(root.getParent().getElementName());
			} else {
				return;
			}
			if (getFlag(flags, COLORIZE)) {
				result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
			}
		}
	}
	
	/**
	 * @param root
	 * @return <code>true</code> if the given package fragment root is
	 * referenced. This means it is owned by a different project but is referenced
	 * by the root's parent. Returns <code>false</code> if the given root
	 * doesn't have an underlying resource.
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

	public static ColoredString decorateColoredString(ColoredString string, String decorated, Style color) {
		String label= string.getString();
		int originalStart= decorated.indexOf(label);
		if (originalStart == -1) {
			return new ColoredString(decorated); // the decorator did something wild
		}
		if (originalStart > 0) {
			ColoredString newString= new ColoredString(decorated.substring(0, originalStart), color);
			newString.append(string);
			string= newString;
		}
		if (decorated.length() > originalStart + label.length()) { // decorator appended something
			return string.append(decorated.substring(originalStart + label.length()), color);
		}
		return string; // no change
	}
	
}
