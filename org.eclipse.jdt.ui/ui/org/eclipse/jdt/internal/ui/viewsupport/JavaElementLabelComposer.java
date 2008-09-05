/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;

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
import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;

/**
 * Implementation of {@link JavaElementLabels}.
 * 
 * @since 3.5
 */
public class JavaElementLabelComposer {
	
	private final static long QUALIFIER_FLAGS= JavaElementLabels.P_COMPRESSED | JavaElementLabels.USE_RESOLVED;
	
	private static final Styler QUALIFIER_STYLE= StyledString.QUALIFIER_STYLER;
	private static final Styler COUNTER_STYLE= StyledString.COUNTER_STYLER;
	private static final Styler DECORATIONS_STYLE= StyledString.DECORATIONS_STYLER;
	
	
	/*
	 * Package name compression
	 */
	private static String fgPkgNamePattern= ""; //$NON-NLS-1$
	private static String fgPkgNamePrefix;
	private static String fgPkgNamePostfix;
	private static int fgPkgNameChars;
	private static int fgPkgNameLength= -1;

	private static final boolean getFlag(long flags, long flag) {
		return (flags & flag) != 0;
	}
	
	/**
	 * Returns the label of the given object. The object must be of type {@link IJavaElement} or adapt to {@link IWorkbenchAdapter}.
	 * If not, the empty string is returned.
	 * 
	 * if the element type is not known.
	 * @param obj Object to get the label from.
	 * @param flags The rendering flags
	 * @return Returns the label or the empty string if the object type is not supported.
	 */
	public String getTextLabel(Object obj, long flags) {
		return getStyledTextLabel(obj, flags).getString();
	}
	
	/**
	 * Returns the styled label of the given object. The object must be of type {@link IJavaElement} or adapt to {@link IWorkbenchAdapter}.
	 * If not, the empty string is returned if the element type is not known.
	 * 
	 * @param obj Object to get the label from.
	 * @param flags The rendering flags
	 * @return Returns the label or the empty string if the object type is not supported.
	 */
	public StyledString getStyledTextLabel(Object obj, long flags) {
		if (obj instanceof IJavaElement) {
			return getStyledElementLabel((IJavaElement) obj, flags);
		} else if (obj instanceof IResource) {
			return getStyledResourceLabel((IResource) obj);
		} else if (obj instanceof ClassPathContainer) {
			ClassPathContainer container= (ClassPathContainer) obj;
			return getStyledContainerEntryLabel(container.getClasspathEntry().getPath(), container.getJavaProject());
		} else if (obj instanceof IStorage) {
			return getStyledStorageLabel((IStorage) obj);
		} else if (obj instanceof IAdaptable) {
			IWorkbenchAdapter wbadapter= (IWorkbenchAdapter) ((IAdaptable)obj).getAdapter(IWorkbenchAdapter.class);
			if (wbadapter != null) {
				return new StyledString(wbadapter.getLabel(obj));
			}
		}
		return new StyledString();
	}
	
	/**
	 * Returns the styled string for the given resource.
	 * 
	 * @param resource the resource
	 * @return the styled string
	 * @since 3.4
	 */
	private StyledString getStyledResourceLabel(IResource resource) {
		StyledString result= new StyledString(resource.getName());
		return Strings.markLTR(result);
		
	}
	
	/**
	 * Returns the styled string for the given storage.
	 * 
	 * @param storage the storage
	 * @return the styled string
	 * @since 3.4
	 */
	private StyledString getStyledStorageLabel(IStorage storage) {
		StyledString result= new StyledString(storage.getName());
		return Strings.markLTR(result);
		
	}
				
				
	/**
	 * Returns the label for a Java element with the flags as defined by this class.
	 * 
	 * @param element The element to render.
	 * @param flags The rendering flags.
	 * @return the label of the Java element
	 */
	public String getElementLabel(IJavaElement element, long flags) {
		return getStyledTextLabel(element, flags).getString();
	}
	
	/**
	 * Returns the styled label for a Java element with the flags as defined by this class.
	 * 
	 * @param element The element to render.
	 * @param flags The rendering flags.
	 * @return the label of the Java element
	 */
	public StyledString getStyledElementLabel(IJavaElement element, long flags) {
		StyledString result= new StyledString();
		getElementLabel(element, flags, result);
		return Strings.markLTR(result, "<>(),?:{}"); //$NON-NLS-1$
	}

	/**
	 * Returns the label for a Java element with the flags as defined by this class.
	 * 
	 * @param element The element to render.
	 * @param flags The rendering flags.
	 * @param buf The buffer to append the resulting label to.
	 */
	public void getElementLabel(IJavaElement element, long flags, StringBuffer buf) {
		buf.append(getElementLabel(element, flags));
	}
	
	/**
	 * Returns the styled label for a Java element with the flags as defined by this class.
	 * 
	 * @param element The element to render.
	 * @param flags The rendering flags.
	 * @param result The buffer to append the resulting label to.
	 */
	public void getElementLabel(IJavaElement element, long flags, StyledString result) {
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
//			case IJavaElement.TYPE_PARAMETER: //TODO
//				getTypeParameterLabel((ITypeParameter) element, flags, result);
//				break;
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
			
			if (getFlag(flags, JavaElementLabels.COLORIZE)) {
				result.setStyle(offset, result.length() - offset, QUALIFIER_STYLE);
			}
			
		}
	}

	

	/**
	 * Appends the label for a method to a {@link StringBuffer}. Considers the M_* flags.
	 * 	@param method The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'M_' are considered.
	 * @param buf The buffer to append the resulting label to.
	 */
	public void getMethodLabel(IMethod method, long flags, StringBuffer buf) {
		StyledString builder= new StyledString();
		getMethodLabel(method, flags, builder);
		buf.append(builder.getString());
	}

	
	/**
	 * Appends the label for a method to a {@link StyledString}. Considers the M_* flags.
	 * 	@param method The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'M_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */
	public void getMethodLabel(IMethod method, long flags, StyledString result) {
		try {
			BindingKey resolvedKey= getFlag(flags, JavaElementLabels.USE_RESOLVED) && method.isResolved() ? new BindingKey(method.getKey()) : null;
			String resolvedSig= (resolvedKey != null) ? resolvedKey.toSignature() : null;
			
			// type parameters
			if (getFlag(flags, JavaElementLabels.M_PRE_TYPE_PARAMETERS)) {
				if (resolvedKey != null) {
					if (resolvedKey.isParameterizedMethod()) {
						String[] typeArgRefs= resolvedKey.getTypeArguments();
						if (typeArgRefs.length > 0) {
							getTypeArgumentSignaturesLabel(method, typeArgRefs, flags, result);
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
				getTypeSignatureLabel(method, returnTypeSig, flags, result);
				result.append(' ');
			}
			
			// qualification
			if (getFlag(flags, JavaElementLabels.M_FULLY_QUALIFIED)) {
				getTypeLabel(method.getDeclaringType(), JavaElementLabels.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), result);
				result.append('.');
			}
				
			result.append(getElementName(method));
			
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
							getTypeSignatureLabel(method, Signature.getElementType(paramSig), flags, result);
							for (int k= 0; k < newDim; k++) {
								result.append('[').append(']');
							}
							result.append(JavaElementLabels.ELLIPSIS_STRING);
						} else {
							getTypeSignatureLabel(method, paramSig, flags, result);
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
						getTypeSignatureLabel(method, types[i], flags, result);
					}
				}
			}
			
			
			if (getFlag(flags, JavaElementLabels.M_APP_TYPE_PARAMETERS)) {
				int offset= result.length();
				if (resolvedKey != null) {
					if (resolvedKey.isParameterizedMethod()) {
						String[] typeArgRefs= resolvedKey.getTypeArguments();
						if (typeArgRefs.length > 0) {
							result.append(' ');
							getTypeArgumentSignaturesLabel(method, typeArgRefs, flags, result);
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
				if (getFlag(flags, JavaElementLabels.COLORIZE) && offset != result.length()) {
					result.setStyle(offset, result.length() - offset, DECORATIONS_STYLE);
				}
			}
			
			if (getFlag(flags, JavaElementLabels.M_APP_RETURNTYPE) && method.exists() && !method.isConstructor()) {
				int offset= result.length();
				result.append(JavaElementLabels.DECL_STRING);
				String returnTypeSig= resolvedSig != null ? Signature.getReturnType(resolvedSig) : method.getReturnType();
				getTypeSignatureLabel(method, returnTypeSig, flags, result);
				if (getFlag(flags, JavaElementLabels.COLORIZE)) {
					result.setStyle(offset, result.length() - offset, DECORATIONS_STYLE);
				}
			}

			// category
			if (getFlag(flags, JavaElementLabels.M_CATEGORY) && method.exists())
				getCategoryLabel(method, flags, result);
			
			// post qualification
			if (getFlag(flags, JavaElementLabels.M_POST_QUALIFIED)) {
				int offset= result.length();
				result.append(JavaElementLabels.CONCAT_STRING);
				getTypeLabel(method.getDeclaringType(), JavaElementLabels.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), result);
				if (getFlag(flags, JavaElementLabels.COLORIZE)) {
					result.setStyle(offset, result.length() - offset, QUALIFIER_STYLE);
				}
			}
			
		} catch (JavaModelException e) {
			JavaPlugin.log(e); // NotExistsException will not reach this point
		}
	}
	
	private void getCategoryLabel(IMember member, long flags, StyledString result) throws JavaModelException {
		String[] categories= member.getCategories();
		if (categories.length > 0) {
			int offset= result.length();
			StringBuffer categoriesBuf= new StringBuffer();
			for (int i= 0; i < categories.length; i++) {
				if (i > 0)
					categoriesBuf.append(JavaElementLabels.CATEGORY_SEPARATOR_STRING);
				categoriesBuf.append(categories[i]);
			}
			result.append(JavaElementLabels.CONCAT_STRING);
			result.append(Messages.format(JavaUIMessages.JavaElementLabels_category, categoriesBuf.toString()));
			if (getFlag(flags, JavaElementLabels.COLORIZE)) {
				result.setStyle(offset, result.length() - offset, COUNTER_STYLE);
			}
		}
	}
			
	/**
	 * Appends labels for type parameters from type binding array.
	 * 
	 * @param typeParameters the type parameters
	 * @param flags flags with render options
	 * @param result the resulting string buffer
	 */
	private void getTypeParametersLabel(ITypeParameter[] typeParameters, long flags, StyledString result) {
		if (typeParameters.length > 0) {
			result.append(getLT());
			for (int i = 0; i < typeParameters.length; i++) {
				if (i > 0) {
					result.append(JavaElementLabels.COMMA_STRING);
				}
				result.append(getElementName(typeParameters[i]));
			}
			result.append(getGT());
		}
	}
	
	/**
	 * Appends the label for a field to a {@link StringBuffer}. Considers the F_* flags.
	 * 
	 * 	@param field The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'F_' are considered.
	 * @param buf The buffer to append the resulting label to.
	 */
	public void getFieldLabel(IField field, long flags, StringBuffer buf) {
		StyledString builder= new StyledString();
		getFieldLabel(field, flags, builder);
		buf.append(builder.getString());
	}
	
	/**
	 * Appends the style label for a field to a {@link StyledString}. Considers the F_* flags.
	 * 
	 * 	@param field The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'F_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */
	public void getFieldLabel(IField field, long flags, StyledString result) {
		try {
			
			if (getFlag(flags, JavaElementLabels.F_PRE_TYPE_SIGNATURE) && field.exists() && !Flags.isEnum(field.getFlags())) {
				if (getFlag(flags, JavaElementLabels.USE_RESOLVED) && field.isResolved()) {
					getTypeSignatureLabel(field, new BindingKey(field.getKey()).toSignature(), flags, result);
				} else {
					getTypeSignatureLabel(field, field.getTypeSignature(), flags, result);
				}
				result.append(' ');
			}
			
			// qualification
			if (getFlag(flags, JavaElementLabels.F_FULLY_QUALIFIED)) {
				getTypeLabel(field.getDeclaringType(), JavaElementLabels.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), result);
				result.append('.');
			}
			result.append(getElementName(field));
			
			if (getFlag(flags, JavaElementLabels.F_APP_TYPE_SIGNATURE) && field.exists() && !Flags.isEnum(field.getFlags())) {
				int offset= result.length();
				result.append(JavaElementLabels.DECL_STRING);
				if (getFlag(flags, JavaElementLabels.USE_RESOLVED) && field.isResolved()) {
					getTypeSignatureLabel(field, new BindingKey(field.getKey()).toSignature(), flags, result);
				} else {
					getTypeSignatureLabel(field, field.getTypeSignature(), flags, result);
				}
				if (getFlag(flags, JavaElementLabels.COLORIZE)) {
					result.setStyle(offset, result.length() - offset, DECORATIONS_STYLE);
				}
			}

			// category
			if (getFlag(flags, JavaElementLabels.F_CATEGORY) && field.exists())
				getCategoryLabel(field, flags, result);

			// post qualification
			if (getFlag(flags, JavaElementLabels.F_POST_QUALIFIED)) {
				int offset= result.length();
				result.append(JavaElementLabels.CONCAT_STRING);
				getTypeLabel(field.getDeclaringType(), JavaElementLabels.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), result);
				if (getFlag(flags, JavaElementLabels.COLORIZE)) {
					result.setStyle(offset, result.length() - offset, QUALIFIER_STYLE);
				}
			}

		} catch (JavaModelException e) {
			JavaPlugin.log(e); // NotExistsException will not reach this point
		}
	}
	
	/**
	 * Appends the label for a local variable to a {@link StringBuffer}.
	 * 
	 * 	@param localVariable The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'F_' are considered.
	 * @param buf The buffer to append the resulting label to.
	 */
	public void getLocalVariableLabel(ILocalVariable localVariable, long flags, StringBuffer buf) {
		StyledString builder= new StyledString();
		getLocalVariableLabel(localVariable, flags, builder);
		buf.append(builder.getString());
	}
	
	/**
	 * Appends the styled label for a local variable to a {@link StyledString}.
	 * 
	 * 	@param localVariable The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'F_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */
	public void getLocalVariableLabel(ILocalVariable localVariable, long flags, StyledString result) {
		if (getFlag(flags, JavaElementLabels.F_PRE_TYPE_SIGNATURE)) {
			getTypeSignatureLabel(localVariable, localVariable.getTypeSignature(), flags, result);
			result.append(' ');
		}
		
		if (getFlag(flags, JavaElementLabels.F_FULLY_QUALIFIED)) {
			getElementLabel(localVariable.getParent(), JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_FULLY_QUALIFIED | JavaElementLabels.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), result);
			result.append('.');
		}
		
		result.append(getElementName(localVariable));
		
		if (getFlag(flags, JavaElementLabels.F_APP_TYPE_SIGNATURE)) {
			int offset= result.length();
			result.append(JavaElementLabels.DECL_STRING);
			getTypeSignatureLabel(localVariable, localVariable.getTypeSignature(), flags, result);
			if (getFlag(flags, JavaElementLabels.COLORIZE)) {
				result.setStyle(offset, result.length() - offset, DECORATIONS_STYLE);
			}
		}
		
		// post qualification
		if (getFlag(flags, JavaElementLabels.F_POST_QUALIFIED)) {
			result.append(JavaElementLabels.CONCAT_STRING);
			getElementLabel(localVariable.getParent(), JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_FULLY_QUALIFIED | JavaElementLabels.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), result);
		}
	}
	
	
	/**
	 * Appends the label for a initializer to a {@link StringBuffer}. Considers the I_* flags.
	 * 
	 * 	@param initializer The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'I_' are considered.
	 * @param buf The buffer to append the resulting label to.
	 */
	public void getInitializerLabel(IInitializer initializer, long flags, StringBuffer buf) {
		StyledString builder= new StyledString();
		getInitializerLabel(initializer, flags, builder);
		buf.append(builder.getString());
	}
	
	/**
	 * Appends the label for a initializer to a {@link StyledString}. Considers the I_* flags.
	 * 
	 * 	@param initializer The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'I_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */
	public void getInitializerLabel(IInitializer initializer, long flags, StyledString result) {
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
			if (getFlag(flags, JavaElementLabels.COLORIZE)) {
				result.setStyle(offset, result.length() - offset, QUALIFIER_STYLE);
			}
		}
	}
	
	protected void getTypeSignatureLabel(IJavaElement enclosingElement, String typeSig, long flags, StyledString result) {
		int sigKind= Signature.getTypeSignatureKind(typeSig);
		switch (sigKind) {
			case Signature.BASE_TYPE_SIGNATURE:
				result.append(Signature.toString(typeSig));
				break;
			case Signature.ARRAY_TYPE_SIGNATURE:
				getTypeSignatureLabel(enclosingElement, Signature.getElementType(typeSig), flags, result);
				for (int dim= Signature.getArrayCount(typeSig); dim > 0; dim--) {
					result.append('[').append(']');
				}
				break;
			case Signature.CLASS_TYPE_SIGNATURE:
				String baseType= getSimpleTypeName(enclosingElement, Signature.getTypeErasure(typeSig));
				result.append(baseType);
				
				String[] typeArguments= Signature.getTypeArguments(typeSig);
				getTypeArgumentSignaturesLabel(enclosingElement, typeArguments, flags, result);
				break;
			case Signature.TYPE_VARIABLE_SIGNATURE:
				result.append(getSimpleTypeName(enclosingElement, typeSig));
				break;
			case Signature.WILDCARD_TYPE_SIGNATURE:
				char ch= typeSig.charAt(0);
				if (ch == Signature.C_STAR) { //workaround for bug 85713
					result.append('?');
				} else {
					if (ch == Signature.C_EXTENDS) {
						result.append("? extends "); //$NON-NLS-1$
						getTypeSignatureLabel(enclosingElement, typeSig.substring(1), flags, result);
					} else if (ch == Signature.C_SUPER) {
						result.append("? super "); //$NON-NLS-1$
						getTypeSignatureLabel(enclosingElement, typeSig.substring(1), flags, result);
					}
				}
				break;
			case Signature.CAPTURE_TYPE_SIGNATURE:
				getTypeSignatureLabel(enclosingElement, typeSig.substring(1), flags, result);
				break;
			default:
				// unknown
		}
	}

	/**
	 * Returns the simple name of the given type signature.
	 * 
	 * @param enclosingElement the enclosing element in which to resolve the signature
	 * @param typeSig a {@link Signature#CLASS_TYPE_SIGNATURE} or {@link Signature#TYPE_VARIABLE_SIGNATURE}
	 * @return the simple name of the given type signature
	 */
	protected String getSimpleTypeName(IJavaElement enclosingElement, String typeSig) {
		return Signature.getSimpleName(Signature.toString(typeSig));
	}
	
	private void getTypeArgumentSignaturesLabel(IJavaElement enclosingElement, String[] typeArgsSig, long flags, StyledString result) {
		if (typeArgsSig.length > 0) {
			result.append(getLT());
			for (int i = 0; i < typeArgsSig.length; i++) {
				if (i > 0) {
					result.append(JavaElementLabels.COMMA_STRING);
				}
				getTypeSignatureLabel(enclosingElement, typeArgsSig[i], flags, result);
			}
			result.append(getGT());
		}
	}

	/**
	 * Appends labels for type parameters from a signature.
	 * 
	 * @param typeParamSigs the type parameter signature
	 * @param flags flags with render options
	 * @param result the resulting string buffer
	 */
	private void getTypeParameterSignaturesLabel(String[] typeParamSigs, long flags, StyledString result) {
		if (typeParamSigs.length > 0) {
			result.append(getLT());
			for (int i = 0; i < typeParamSigs.length; i++) {
				if (i > 0) {
					result.append(JavaElementLabels.COMMA_STRING);
				}
				result.append(Signature.getTypeVariable(typeParamSigs[i]));
			}
			result.append(getGT());
		}
	}

	/**
	 * Returns the string for rendering the '<code>&lt;</code>' character.
	 * 
	 * @return the string for rendering '<code>&lt;</code>'
	 */
	protected String getLT() {
		return "<"; //$NON-NLS-1$
	}
	
	/**
	 * Returns the string for rendering the '<code>&gt;</code>' character.
	 * 
	 * @return the string for rendering '<code>&gt;</code>'
	 */
	protected String getGT() {
		return ">"; //$NON-NLS-1$
	}

	/**
	 * Appends the label for a type to a {@link StringBuffer}. Considers the T_* flags.
	 * 
	 * 	@param type The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'T_' are considered.
	 * @param buf The buffer to append the resulting label to.
	 */
	public void getTypeLabel(IType type, long flags, StringBuffer buf) {
		StyledString builder= new StyledString();
		getTypeLabel(type, flags, builder);
		buf.append(builder.getString());
	}

	/**
	 * Appends the label for a type to a {@link StyledString}. Considers the T_* flags.
	 * 
	 * 	@param type The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'T_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */
	public void getTypeLabel(IType type, long flags, StyledString result) {
		
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
		
		String typeName= getElementName(type);
		if (typeName.length() == 0) { // anonymous
			try {
				if (type.getParent() instanceof IField && type.isEnum()) {
					typeName= '{' + JavaElementLabels.ELLIPSIS_STRING + '}';
				} else {
					String supertypeName;
					String[] superInterfaceSignatures= type.getSuperInterfaceTypeSignatures();
					if (superInterfaceSignatures.length > 0) {
						supertypeName= getSimpleTypeName(type, superInterfaceSignatures[0]);
					} else {
						supertypeName= getSimpleTypeName(type, type.getSuperclassTypeSignature());
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
					getTypeArgumentSignaturesLabel(type, typeArguments, flags, result);
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
				getCategoryLabel(type, flags, result);
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
			if (getFlag(flags, JavaElementLabels.COLORIZE)) {
				result.setStyle(offset, result.length() - offset, QUALIFIER_STYLE);
			}
		}
	}

	/**
	 * Returns the string for rendering the {@link IJavaElement#getElementName() element name} of the given element.
	 * 
	 * @param element the element to render
	 * @return the string for rendering the element name
	 */
	protected String getElementName(IJavaElement element) {
		return element.getElementName();
	}
	
	
	/**
	 * Appends the label for a import container, import or package declaration to a {@link StringBuffer}. Considers the D_* flags.
	 * 
	 * 	@param declaration The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'D_' are considered.
	 * @param buf The buffer to append the resulting label to.
	 */
	public void getDeclarationLabel(IJavaElement declaration, long flags, StringBuffer buf) {
		StyledString builder= new StyledString();
		getDeclarationLabel(declaration, flags, builder);
		buf.append(builder.getString());
	}
	
	/**
	 * Appends the label for a import container, import or package declaration to a {@link StyledString}. Considers the D_* flags.
	 * 
	 * 	@param declaration The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'D_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */
	public void getDeclarationLabel(IJavaElement declaration, long flags, StyledString result) {
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
			if (getFlag(flags, JavaElementLabels.COLORIZE)) {
				result.setStyle(offset, result.length() - offset, QUALIFIER_STYLE);
			}
		}
	}
	
	/**
	 * Appends the label for a class file to a {@link StringBuffer}. Considers the CF_* flags.
	 * 
	 * 	@param classFile The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'CF_' are considered.
	 * @param buf The buffer to append the resulting label to.
	 */
	public void getClassFileLabel(IClassFile classFile, long flags, StringBuffer buf) {
		StyledString builder= new StyledString();
		getClassFileLabel(classFile, flags, builder);
		buf.append(builder.getString());
	}
	
	/**
	 * Appends the label for a class file to a {@link StyledString}. Considers the CF_* flags.
	 * 
	 * 	@param classFile The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'CF_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */
	public void getClassFileLabel(IClassFile classFile, long flags, StyledString result) {
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
			if (getFlag(flags, JavaElementLabels.COLORIZE)) {
				result.setStyle(offset, result.length() - offset, QUALIFIER_STYLE);
			}
		}
	}

	/**
	 * Appends the label for a compilation unit to a {@link StringBuffer}. Considers the CU_* flags.
	 * 
	 * 	@param cu The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'CU_' are considered.
	 * @param buf The buffer to append the resulting label to.
	 */
	public void getCompilationUnitLabel(ICompilationUnit cu, long flags, StringBuffer buf) {
		StyledString builder= new StyledString();
		getCompilationUnitLabel(cu, flags, builder);
		buf.append(builder.getString());
	}
	
	/**
	 * Appends the label for a compilation unit to a {@link StyledString}. Considers the CU_* flags.
	 * 
	 * 	@param cu The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'CU_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */
	public void getCompilationUnitLabel(ICompilationUnit cu, long flags, StyledString result) {
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
			if (getFlag(flags, JavaElementLabels.COLORIZE)) {
				result.setStyle(offset, result.length() - offset, QUALIFIER_STYLE);
			}
		}
	}

	/**
	 * Appends the label for a package fragment to a {@link StringBuffer}. Considers the P_* flags.
	 * 
	 * 	@param pack The element to render.
	 * @param flags The rendering flags. Flags with names starting with P_' are considered.
	 * @param buf The buffer to append the resulting label to.
	 */
	public void getPackageFragmentLabel(IPackageFragment pack, long flags, StringBuffer buf) {
		StyledString builder= new StyledString();
		getPackageFragmentLabel(pack, flags, builder);
		buf.append(builder.getString());
	}
	
	/**
	 * Appends the label for a package fragment to a {@link StyledString}. Considers the P_* flags.
	 * 
	 * 	@param pack The element to render.
	 * @param flags The rendering flags. Flags with names starting with P_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */
	public void getPackageFragmentLabel(IPackageFragment pack, long flags, StyledString result) {
		if (getFlag(flags, JavaElementLabels.P_QUALIFIED)) {
			getPackageFragmentRootLabel((IPackageFragmentRoot) pack.getParent(), JavaElementLabels.ROOT_QUALIFIED, result);
			result.append('/');
		}
		if (pack.isDefaultPackage()) {
			result.append(JavaElementLabels.DEFAULT_PACKAGE);
		} else if (getFlag(flags, JavaElementLabels.P_COMPRESSED)) {
			getCompressedPackageFragment(pack, result);
		} else {
			result.append(pack.getElementName());
		}
		if (getFlag(flags, JavaElementLabels.P_POST_QUALIFIED)) {
			int offset= result.length();
			result.append(JavaElementLabels.CONCAT_STRING);
			getPackageFragmentRootLabel((IPackageFragmentRoot) pack.getParent(), JavaElementLabels.ROOT_QUALIFIED, result);
			if (getFlag(flags, JavaElementLabels.COLORIZE)) {
				result.setStyle(offset, result.length() - offset, QUALIFIER_STYLE);
			}
		}
	}
	
	private void getCompressedPackageFragment(IPackageFragment pack, StyledString result) {
		refreshPackageNamePattern();
		if (fgPkgNameLength < 0) {
			result.append(pack.getElementName());
			return;
		}
		String name= pack.getElementName();
		int start= 0;
		int dot= name.indexOf('.', start);
		while (dot > 0) {
			if (dot - start > fgPkgNameLength-1) {
				result.append(fgPkgNamePrefix);
				if (fgPkgNameChars > 0)
					result.append(name.substring(start, Math.min(start+ fgPkgNameChars, dot)));
				result.append(fgPkgNamePostfix);
			} else
				result.append(name.substring(start, dot + 1));
			start= dot + 1;
			dot= name.indexOf('.', start);
		}
		result.append(name.substring(start));
	}
	

	/**
	 * Appends the label for a package fragment root to a {@link StringBuffer}. Considers the ROOT_* flags.
	 * 
	 * 	@param root The element to render.
	 * @param flags The rendering flags. Flags with names starting with ROOT_' are considered.
	 * @param buf The buffer to append the resulting label to.
	 */
	public void getPackageFragmentRootLabel(IPackageFragmentRoot root, long flags, StringBuffer buf) {
		StyledString builder= new StyledString();
		getPackageFragmentRootLabel(root, flags, builder);
		buf.append(builder.getString());
	}
	
	/**
	 * Appends the label for a package fragment root to a {@link StyledString}. Considers the ROOT_* flags.
	 * 
	 * 	@param root The element to render.
	 * @param flags The rendering flags. Flags with names starting with ROOT_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */
	public void getPackageFragmentRootLabel(IPackageFragmentRoot root, long flags, StyledString result) {
		// Handle variables different
		if (getFlag(flags, JavaElementLabels.ROOT_VARIABLE) && getVariableLabel(root, flags, result))
			return;
		if (root.isArchive())
			getArchiveLabel(root, flags, result);
		else
			getFolderLabel(root, flags, result);
	}
	
	private void getArchiveLabel(IPackageFragmentRoot root, long flags, StyledString result) {
		boolean external= root.isExternal();
		if (external)
			getExternalArchiveLabel(root, flags, result);
		else
			getInternalArchiveLabel(root, flags, result);
	}
	
	private boolean getVariableLabel(IPackageFragmentRoot root, long flags, StyledString result) {
		try {
			IClasspathEntry rawEntry= root.getRawClasspathEntry();
			if (rawEntry != null && rawEntry.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
				IPath path= rawEntry.getPath().makeRelative();
				
				if (getFlag(flags, JavaElementLabels.REFERENCED_ROOT_POST_QUALIFIED)) {
					int segements= path.segmentCount();
					if (segements > 0) {
						result.append(path.segment(segements - 1));
						if (segements > 1) {
							int offset= result.length();
							result.append(JavaElementLabels.CONCAT_STRING);
							result.append(path.removeLastSegments(1).toOSString());
							if (getFlag(flags, JavaElementLabels.COLORIZE)) {
								result.setStyle(offset, result.length() - offset, QUALIFIER_STYLE);
							}
						}
					} else {
						result.append(path.toString());
					}
				} else {
					result.append(path.toString());
				}
				int offset= result.length();
				result.append(JavaElementLabels.CONCAT_STRING);
				if (root.isExternal())
					result.append(root.getPath().toOSString());
				else
					result.append(root.getPath().makeRelative().toString());
				
				if (getFlag(flags, JavaElementLabels.COLORIZE)) {
					result.setStyle(offset, result.length() - offset, QUALIFIER_STYLE);
				}
				return true;
			}
		} catch (JavaModelException e) {
			// problems with class path, ignore (bug 202792)
			return false;
		}
		return false;
	}

	private void getExternalArchiveLabel(IPackageFragmentRoot root, long flags, StyledString result) {
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
				if (getFlag(flags, JavaElementLabels.COLORIZE)) {
					result.setStyle(offset, result.length() - offset, QUALIFIER_STYLE);
				}
			} else {
				result.append(path.toOSString());
			}
		} else {
			result.append(path.toOSString());
		}
	}

	private void getInternalArchiveLabel(IPackageFragmentRoot root, long flags, StyledString result) {
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
			if (getFlag(flags, JavaElementLabels.COLORIZE)) {
				result.setStyle(offset, result.length() - offset, QUALIFIER_STYLE);
			}
		}
	}

	private void getFolderLabel(IPackageFragmentRoot root, long flags, StyledString result) {
		IResource resource= root.getResource();
		if (resource == null) {
			getExternalArchiveLabel(root, flags, result);
			return;
		}
		
		boolean rootQualified= getFlag(flags, JavaElementLabels.ROOT_QUALIFIED);
		boolean referencedQualified= getFlag(flags, JavaElementLabels.REFERENCED_ROOT_POST_QUALIFIED) && isReferenced(root);
		if (rootQualified) {
			result.append(root.getPath().makeRelative().toString());
		} else {
			IPath projectRelativePath= resource.getProjectRelativePath();
			if (projectRelativePath.segmentCount() == 0) {
				result.append(resource.getName());
				referencedQualified= false;
			} else {
				result.append(projectRelativePath.toString());
			}
				
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
			if (getFlag(flags, JavaElementLabels.COLORIZE)) {
				result.setStyle(offset, result.length() - offset, QUALIFIER_STYLE);
			}
		}
	}
	
	/**
	 * Returns <code>true</code> if the given package fragment root is
	 * referenced. This means it is own by a different project but is referenced
	 * by the root's parent. Returns <code>false</code> if the given root
	 * doesn't have an underlying resource.
	 * 
	 * @param root the package fragment root
	 * @return returns <code>true</code> if the given package fragment root is referenced
	 */
	private boolean isReferenced(IPackageFragmentRoot root) {
		IResource resource= root.getResource();
		if (resource != null) {
			IProject jarProject= resource.getProject();
			IProject container= root.getJavaProject().getProject();
			return !container.equals(jarProject);
		}
		return false;
	}

	private void refreshPackageNamePattern() {
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
	
	private String getPkgNamePatternForPackagesView() {
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
	public String getContainerEntryLabel(IPath containerPath, IJavaProject project) throws JavaModelException {
		IClasspathContainer container= JavaCore.getClasspathContainer(containerPath, project);
		if (container != null) {
			return container.getDescription();
		}
		ClasspathContainerInitializer initializer= JavaCore.getClasspathContainerInitializer(containerPath.segment(0));
		if (initializer != null) {
			return initializer.getDescription(containerPath, project);
		}
		return BasicElementLabels.getPathLabel(containerPath, false);
	}
	
	/**
	 * Returns the styled label of a classpath container
	 * @param containerPath The path of the container.
	 * @param project The project the container is resolved in.
	 * @return Returns the label of the classpath container
	 */
	public StyledString getStyledContainerEntryLabel(IPath containerPath, IJavaProject project) {
		try {
			IClasspathContainer container= JavaCore.getClasspathContainer(containerPath, project);
			String description= null;
			if (container != null) {
				description= container.getDescription();
			}
			if (description == null) {
				ClasspathContainerInitializer initializer= JavaCore.getClasspathContainerInitializer(containerPath.segment(0));
				if (initializer != null) {
					description= initializer.getDescription(containerPath, project);
				}
			}
			if (description != null) {
				StyledString str= new StyledString(description);
				if (containerPath.segmentCount() > 0 && JavaRuntime.JRE_CONTAINER.equals(containerPath.segment(0))) {
					int index= description.indexOf('[');
					if (index != -1) {
						str.setStyle(index, description.length() - index, DECORATIONS_STYLE);
					}
				}
				return Strings.markLTR(str);
			}
		} catch (JavaModelException e) {
			// ignore
		}
		return new StyledString(containerPath.toString());
	}
	
	
}
