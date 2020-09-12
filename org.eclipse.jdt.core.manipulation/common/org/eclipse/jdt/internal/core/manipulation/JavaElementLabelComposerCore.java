/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
 *     Red Hat Inc. - copy to JavaElementLabelComposerCore
 *******************************************************************************/
package org.eclipse.jdt.internal.core.manipulation;

import java.util.jar.Attributes.Name;

import org.eclipse.osgi.util.NLS;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.BindingKey;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.SourceRange;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

/**
 * Implementation of {@link JavaElementLabelsCore}.
 *
 * @since 1.10
 */
public class JavaElementLabelComposerCore {

	/**
	 * An adapter for buffer supported by the label composer.
	 */
	public static abstract class FlexibleBufferCore {

		/**
		 * Appends the string representation of the given character to the buffer.
		 *
		 * @param ch the character to append
		 * @return a reference to this object
		 */
		public abstract FlexibleBufferCore append(char ch);

		/**
		 * Appends the given string to the buffer.
		 *
		 * @param string the string to append
		 * @return a reference to this object
		 */
		public abstract FlexibleBufferCore append(String string);

		/**
		 * Returns the length of the the buffer.
		 *
		 * @return the length of the current string
		 */
		public abstract int length();

	}

	public static class FlexibleStringBufferCore extends FlexibleBufferCore {
		private final StringBuffer fStringBuffer;

		public FlexibleStringBufferCore(StringBuffer stringBuffer) {
			fStringBuffer= stringBuffer;
		}

		@Override
		public FlexibleBufferCore append(char ch) {
			fStringBuffer.append(ch);
			return this;
		}

		@Override
		public FlexibleBufferCore append(String string) {
			fStringBuffer.append(string);
			return this;
		}

		@Override
		public int length() {
			return fStringBuffer.length();
		}

		@Override
		public String toString() {
			return fStringBuffer.toString();
		}
	}

	final static long QUALIFIER_FLAGS= JavaElementLabelsCore.P_COMPRESSED | JavaElementLabelsCore.USE_RESOLVED;

	/*
	 * Package name compression
	 */
	protected static String fgPkgNamePrefix;
	protected static String fgPkgNamePostfix;
	protected static int fgPkgNameChars;
	protected static int fgPkgNameLength= -1;

	protected final FlexibleBufferCore fBuffer;

	protected static final boolean getFlag(long flags, long flag) {
		return (flags & flag) != 0;
	}

	/**
	 * Creates a new java element composer based on the given buffer.
	 *
	 * @param buffer the buffer
	 */
	public JavaElementLabelComposerCore(FlexibleBufferCore buffer) {
		fBuffer= buffer;
	}

	/**
	 * Creates a new java element composer based on the given buffer.
	 *
	 * @param buffer the string buffer
	 */
	public JavaElementLabelComposerCore(StringBuffer buffer) {
		this(new FlexibleStringBufferCore(buffer));
	}

	/**
	 * Appends the label for a Java element with the flags as defined by this class.
	 *
	 * @param element the element to render
	 * @param flags the rendering flags.
	 */
	public void appendElementLabel(IJavaElement element, long flags) {
		int type= element.getElementType();
		IPackageFragmentRoot root= null;

		if (type != IJavaElement.JAVA_MODEL && type != IJavaElement.JAVA_PROJECT && type != IJavaElement.PACKAGE_FRAGMENT_ROOT)
			root= JavaModelUtil.getPackageFragmentRoot(element);
		if (root != null && getFlag(flags, JavaElementLabelsCore.PREPEND_ROOT_PATH)) {
			appendPackageFragmentRootLabel(root, JavaElementLabelsCore.ROOT_QUALIFIED);
			fBuffer.append(JavaElementLabelsCore.CONCAT_STRING);
		}

		switch (type) {
			case IJavaElement.METHOD:
				appendMethodLabel((IMethod) element, flags);
				break;
			case IJavaElement.FIELD:
				appendFieldLabel((IField) element, flags);
				break;
			case IJavaElement.LOCAL_VARIABLE:
				appendLocalVariableLabel((ILocalVariable) element, flags);
				break;
			case IJavaElement.TYPE_PARAMETER:
				appendTypeParameterLabel((ITypeParameter) element, flags);
				break;
			case IJavaElement.INITIALIZER:
				appendInitializerLabel((IInitializer) element, flags);
				break;
			case IJavaElement.TYPE:
				appendTypeLabel((IType) element, flags);
				break;
			case IJavaElement.CLASS_FILE:
				appendClassFileLabel((IClassFile) element, flags);
				break;
			case IJavaElement.COMPILATION_UNIT:
				appendCompilationUnitLabel((ICompilationUnit) element, flags);
				break;
			case IJavaElement.PACKAGE_FRAGMENT:
				appendPackageFragmentLabel((IPackageFragment) element, flags);
				break;
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				appendPackageFragmentRootLabel((IPackageFragmentRoot) element, flags);
				break;
			case IJavaElement.JAVA_MODULE:
				appendModuleLabel((IModuleDescription) element, flags);
				break;
			case IJavaElement.IMPORT_CONTAINER:
			case IJavaElement.IMPORT_DECLARATION:
			case IJavaElement.PACKAGE_DECLARATION:
				appendDeclarationLabel(element, flags);
				break;
			case IJavaElement.JAVA_PROJECT:
			case IJavaElement.JAVA_MODEL:
				fBuffer.append(element.getElementName());
				break;
			default:
				fBuffer.append(element.getElementName());
		}

		if (root != null && getFlag(flags, JavaElementLabelsCore.APPEND_ROOT_PATH)) {
			int offset= fBuffer.length();
			fBuffer.append(JavaElementLabelsCore.CONCAT_STRING);
			appendPackageFragmentRootLabel(root, JavaElementLabelsCore.ROOT_QUALIFIED);

			if (getFlag(flags, JavaElementLabelsCore.COLORIZE)) {
				setQualifierStyle(offset);
			}

		}
	}

	protected void setQualifierStyle(@SuppressWarnings("unused") int offset) {
		// core does not handle StyledString
	}

	protected void setDecorationsStyle(@SuppressWarnings("unused") int offset) {
		// core does not handle StyledString
	}

	/**
	 * Appends the label for a method. Considers the M_* flags.
	 *
	 * @param method the element to render
	 * @param flags the rendering flags. Flags with names starting with 'M_' are considered.
	 */
	public void appendMethodLabel(IMethod method, long flags) {
		try {
			BindingKey resolvedKey= getFlag(flags, JavaElementLabelsCore.USE_RESOLVED) && method.isResolved() ? new BindingKey(method.getKey()) : null;
			String resolvedSig= (resolvedKey != null) ? resolvedKey.toSignature() : null;

			// type parameters
			if (getFlag(flags, JavaElementLabelsCore.M_PRE_TYPE_PARAMETERS)) {
				if (resolvedKey != null) {
					if (resolvedKey.isParameterizedMethod()) {
						String[] typeArgRefs= resolvedKey.getTypeArguments();
						if (typeArgRefs.length > 0) {
							appendTypeArgumentSignaturesLabel(method, typeArgRefs, flags);
							fBuffer.append(' ');
						}
					} else {
						String[] typeParameterSigs= Signature.getTypeParameters(resolvedSig);
						if (typeParameterSigs.length > 0) {
							appendTypeParameterSignaturesLabel(typeParameterSigs, flags);
							fBuffer.append(' ');
						}
					}
				} else if (method.exists()) {
					ITypeParameter[] typeParameters= method.getTypeParameters();
					if (typeParameters.length > 0) {
						appendTypeParametersLabels(typeParameters, flags);
						fBuffer.append(' ');
					}
				}
			}

			// return type
			if (getFlag(flags, JavaElementLabelsCore.M_PRE_RETURNTYPE) && method.exists() && !method.isConstructor()) {
				String returnTypeSig= resolvedSig != null ? Signature.getReturnType(resolvedSig) : method.getReturnType();
				appendTypeSignatureLabel(method, returnTypeSig, flags);
				fBuffer.append(' ');
			}

			// qualification
			if (getFlag(flags, JavaElementLabelsCore.M_FULLY_QUALIFIED)) {
				appendTypeLabel(method.getDeclaringType(), JavaElementLabelsCore.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS));
				fBuffer.append('.');
			}

			fBuffer.append(getElementName(method));

			// constructor type arguments
			if (getFlag(flags, JavaElementLabelsCore.T_TYPE_PARAMETERS) && method.exists() && method.isConstructor()) {
				if (resolvedSig != null && resolvedKey.isParameterizedType()) {
					BindingKey declaringType= resolvedKey.getDeclaringType();
					if (declaringType != null) {
						String[] declaringTypeArguments= declaringType.getTypeArguments();
						appendTypeArgumentSignaturesLabel(method, declaringTypeArguments, flags);
					}
				}
			}

			// parameters
			fBuffer.append('(');
			String[] declaredParameterTypes= method.getParameterTypes();
			if (getFlag(flags, JavaElementLabelsCore.M_PARAMETER_TYPES | JavaElementLabelsCore.M_PARAMETER_NAMES)) {
				String[] types= null;
				int nParams= 0;
				boolean renderVarargs= false;
				boolean isPolymorphic= false;
				if (getFlag(flags, JavaElementLabelsCore.M_PARAMETER_TYPES)) {
					if (resolvedSig != null) {
						types= Signature.getParameterTypes(resolvedSig);
					} else {
						types= declaredParameterTypes;
					}
					nParams= types.length;
					renderVarargs= method.exists() && Flags.isVarargs(method.getFlags());
					if (renderVarargs
							&& resolvedSig != null
							&& declaredParameterTypes.length == 1
							&& JavaModelUtil.isPolymorphicSignature(method)) {
						renderVarargs= false;
						isPolymorphic= true;
					}
				}
				String[] names= null;
				if (getFlag(flags, JavaElementLabelsCore.M_PARAMETER_NAMES) && method.exists()) {
					names= method.getParameterNames();
					if (isPolymorphic) {
						// handled specially below
					} else	if (types == null) {
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

				ILocalVariable[] annotatedParameters= null;
				if (nParams > 0 && getFlag(flags, JavaElementLabelsCore.M_PARAMETER_ANNOTATIONS)) {
					annotatedParameters= method.getParameters();
				}

				for (int i= 0; i < nParams; i++) {
					if (i > 0) {
						fBuffer.append(JavaElementLabelsCore.COMMA_STRING);
					}
					if (annotatedParameters != null && i < annotatedParameters.length) {
						appendAnnotationLabels(annotatedParameters[i].getAnnotations(), flags);
					}

					if (types != null) {
						String paramSig= types[i];
						if (renderVarargs && (i == nParams - 1)) {
							int newDim= Signature.getArrayCount(paramSig) - 1;
							appendTypeSignatureLabel(method, Signature.getElementType(paramSig), flags);
							for (int k= 0; k < newDim; k++) {
								fBuffer.append('[').append(']');
							}
							fBuffer.append(JavaElementLabelsCore.ELLIPSIS_STRING);
						} else {
							appendTypeSignatureLabel(method, paramSig, flags);
						}
					}
					if (names != null) {
						if (types != null) {
							fBuffer.append(' ');
						}
						if (isPolymorphic) {
							fBuffer.append(names[0] + i);
						} else {
							fBuffer.append(names[i]);
						}
					}
				}
			} else {
				if (declaredParameterTypes.length > 0) {
					fBuffer.append(JavaElementLabelsCore.ELLIPSIS_STRING);
				}
			}
			fBuffer.append(')');

			if (getFlag(flags, JavaElementLabelsCore.M_EXCEPTIONS)) {
				String[] types;
				if (resolvedKey != null) {
					types= resolvedKey.getThrownExceptions();
				} else {
					types= method.exists() ? method.getExceptionTypes() : new String[0];
				}
				if (types.length > 0) {
					fBuffer.append(" throws "); //$NON-NLS-1$
					for (int i= 0; i < types.length; i++) {
						if (i > 0) {
							fBuffer.append(JavaElementLabelsCore.COMMA_STRING);
						}
						appendTypeSignatureLabel(method, types[i], flags);
					}
				}
			}


			if (getFlag(flags, JavaElementLabelsCore.M_APP_TYPE_PARAMETERS)) {
				int offset= fBuffer.length();
				if (resolvedKey != null) {
					if (resolvedKey.isParameterizedMethod()) {
						String[] typeArgRefs= resolvedKey.getTypeArguments();
						if (typeArgRefs.length > 0) {
							fBuffer.append(' ');
							appendTypeArgumentSignaturesLabel(method, typeArgRefs, flags);
						}
					} else {
						String[] typeParameterSigs= Signature.getTypeParameters(resolvedSig);
						if (typeParameterSigs.length > 0) {
							fBuffer.append(' ');
							appendTypeParameterSignaturesLabel(typeParameterSigs, flags);
						}
					}
				} else if (method.exists()) {
					ITypeParameter[] typeParameters= method.getTypeParameters();
					if (typeParameters.length > 0) {
						fBuffer.append(' ');
						appendTypeParametersLabels(typeParameters, flags);
					}
				}
				if (getFlag(flags, JavaElementLabelsCore.COLORIZE) && offset != fBuffer.length()) {
					setDecorationsStyle(offset);
				}
			}

			if (getFlag(flags, JavaElementLabelsCore.M_APP_RETURNTYPE) && method.exists() && !method.isConstructor()) {
				int offset= fBuffer.length();
				fBuffer.append(JavaElementLabelsCore.DECL_STRING);
				String returnTypeSig= resolvedSig != null ? Signature.getReturnType(resolvedSig) : method.getReturnType();
				appendTypeSignatureLabel(method, returnTypeSig, flags);
				if (getFlag(flags, JavaElementLabelsCore.COLORIZE)) {
					setDecorationsStyle(offset);
				}
			}

			// category
			if (getFlag(flags, JavaElementLabelsCore.M_CATEGORY) && method.exists())
				appendCategoryLabel(method, flags);

			// post qualification
			if (getFlag(flags, JavaElementLabelsCore.M_POST_QUALIFIED)) {
				int offset= fBuffer.length();
				fBuffer.append(JavaElementLabelsCore.CONCAT_STRING);
				appendTypeLabel(method.getDeclaringType(), JavaElementLabelsCore.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS));
				if (getFlag(flags, JavaElementLabelsCore.COLORIZE)) {
					setQualifierStyle(offset);
				}
			}

		} catch (JavaModelException e) {
			if(e.getStatus().getCode() == IJavaModelStatusConstants.ELEMENT_DOES_NOT_EXIST) {
				// don't care, we are decorating already removed element
				return;
			}
			Platform.getLog(this.getClass()).error("Error rendering method label", e); //$NON-NLS-1$ // NotExistsException will not reach this point
		}
	}

	@SuppressWarnings("unused")
	protected void appendCategoryLabel(IMember member, long flags) throws JavaModelException {
		// core does not implement this
	}


	protected void appendAnnotationLabels(IAnnotation[] annotations, long flags) throws JavaModelException {
		for (IAnnotation annotation : annotations) {
			appendAnnotationLabel(annotation, flags);
			fBuffer.append(' ');
		}
	}

	public void appendAnnotationLabel(IAnnotation annotation, long flags) throws JavaModelException {
		fBuffer.append('@');
		appendTypeSignatureLabel(annotation, Signature.createTypeSignature(annotation.getElementName(), false), flags);
		IMemberValuePair[] memberValuePairs= annotation.getMemberValuePairs();
		if (memberValuePairs.length == 0) {
			return;
		}
		fBuffer.append('(');
		for (int i= 0; i < memberValuePairs.length; i++) {
			if (i > 0) {
				fBuffer.append(JavaElementLabelsCore.COMMA_STRING);
			}
			IMemberValuePair memberValuePair= memberValuePairs[i];
			fBuffer.append(getMemberName(annotation, annotation.getElementName(), memberValuePair.getMemberName()));
			fBuffer.append('=');
			appendAnnotationValue(annotation, memberValuePair.getValue(), memberValuePair.getValueKind(), flags);
		}
		fBuffer.append(')');
	}

	public void appendAnnotationValue(IAnnotation annotation, Object value, int valueKind, long flags) throws JavaModelException {
		// Note: To be bug-compatible with Javadoc from Java 5/6/7, we currently don't escape HTML tags in String-valued annotations.
		if (value instanceof Object[]) {
			fBuffer.append('{');
			Object[] values= (Object[]) value;
			for (int j= 0; j < values.length; j++) {
				if (j > 0) {
					fBuffer.append(JavaElementLabelsCore.COMMA_STRING);
				}
				value= values[j];
				appendAnnotationValue(annotation, value, valueKind, flags);
			}
			fBuffer.append('}');
		} else {
			switch (valueKind) {
				case IMemberValuePair.K_CLASS:
					appendTypeSignatureLabel(annotation, Signature.createTypeSignature((String) value, false), flags);
					fBuffer.append(".class"); //$NON-NLS-1$
					break;
				case IMemberValuePair.K_QUALIFIED_NAME:
					String name= (String) value;
					int lastDot= name.lastIndexOf('.');
					if (lastDot != -1) {
						String type= name.substring(0, lastDot);
						String field= name.substring(lastDot + 1);
						appendTypeSignatureLabel(annotation, Signature.createTypeSignature(type, false), flags);
						fBuffer.append('.');
						fBuffer.append(getMemberName(annotation, type, field));
						break;
					}
					//				case IMemberValuePair.K_SIMPLE_NAME: // can't implement, since parent type is not known
					//$FALL-THROUGH$
				case IMemberValuePair.K_ANNOTATION:
					appendAnnotationLabel((IAnnotation) value, flags);
					break;
				case IMemberValuePair.K_STRING:
					fBuffer.append(ASTNodes.getEscapedStringLiteral((String) value));
					break;
				case IMemberValuePair.K_CHAR:
					fBuffer.append(ASTNodes.getEscapedCharacterLiteral(((Character) value)));
					break;
				default:
					fBuffer.append(String.valueOf(value));
					break;
			}
		}
	}

	/**
	 * Appends labels for type parameters from type binding array.
	 *
	 * @param typeParameters the type parameters
	 * @param flags flags with render options
	 * @throws JavaModelException ...
	 */
	private void appendTypeParametersLabels(ITypeParameter[] typeParameters, long flags) throws JavaModelException {
		if (typeParameters.length > 0) {
			fBuffer.append(getLT());
			for (int i = 0; i < typeParameters.length; i++) {
				if (i > 0) {
					fBuffer.append(JavaElementLabelsCore.COMMA_STRING);
				}
				appendTypeParameterWithBounds(typeParameters[i], flags);
			}
			fBuffer.append(getGT());
		}
	}

	/**
	 * Appends the style label for a field. Considers the F_* flags.
	 *
	 * @param field the element to render
	 * @param flags the rendering flags. Flags with names starting with 'F_' are considered.
	 */
	public void appendFieldLabel(IField field, long flags) {
		try {

			if (getFlag(flags, JavaElementLabelsCore.F_PRE_TYPE_SIGNATURE) && field.exists() && !Flags.isEnum(field.getFlags())) {
				if (getFlag(flags, JavaElementLabelsCore.USE_RESOLVED) && field.isResolved()) {
					appendTypeSignatureLabel(field, new BindingKey(field.getKey()).toSignature(), flags);
				} else {
					appendTypeSignatureLabel(field, field.getTypeSignature(), flags);
				}
				fBuffer.append(' ');
			}

			// qualification
			if (getFlag(flags, JavaElementLabelsCore.F_FULLY_QUALIFIED)) {
				appendTypeLabel(field.getDeclaringType(), JavaElementLabelsCore.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS));
				fBuffer.append('.');
			}
			fBuffer.append(getElementName(field));

			if (getFlag(flags, JavaElementLabelsCore.F_APP_TYPE_SIGNATURE) && field.exists() && !Flags.isEnum(field.getFlags())) {
				int offset= fBuffer.length();
				fBuffer.append(JavaElementLabelsCore.DECL_STRING);
				if (getFlag(flags, JavaElementLabelsCore.USE_RESOLVED) && field.isResolved()) {
					appendTypeSignatureLabel(field, new BindingKey(field.getKey()).toSignature(), flags);
				} else {
					appendTypeSignatureLabel(field, field.getTypeSignature(), flags);
				}
				if (getFlag(flags, JavaElementLabelsCore.COLORIZE)) {
					setDecorationsStyle(offset);
				}
			}

			// category
			if (getFlag(flags, JavaElementLabelsCore.F_CATEGORY) && field.exists())
				appendCategoryLabel(field, flags);

			// post qualification
			if (getFlag(flags, JavaElementLabelsCore.F_POST_QUALIFIED)) {
				int offset= fBuffer.length();
				fBuffer.append(JavaElementLabelsCore.CONCAT_STRING);
				appendTypeLabel(field.getDeclaringType(), JavaElementLabelsCore.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS));
				if (getFlag(flags, JavaElementLabelsCore.COLORIZE)) {
					setQualifierStyle(offset);
				}
			}

		} catch (JavaModelException e) {
			JavaManipulationPlugin.log(e); // NotExistsException will not reach this point
		}
	}

	/**
	 * Appends the styled label for a local variable.
	 *
	 * @param localVariable the element to render
	 * @param flags the rendering flags. Flags with names starting with 'F_' are considered.
	 */
	public void appendLocalVariableLabel(ILocalVariable localVariable, long flags) {
		if (getFlag(flags, JavaElementLabelsCore.F_PRE_TYPE_SIGNATURE)) {
			appendTypeSignatureLabel(localVariable, localVariable.getTypeSignature(), flags);
			fBuffer.append(' ');
		}

		if (getFlag(flags, JavaElementLabelsCore.F_FULLY_QUALIFIED)) {
			appendElementLabel(localVariable.getDeclaringMember(), JavaElementLabelsCore.M_PARAMETER_TYPES | JavaElementLabelsCore.M_FULLY_QUALIFIED | JavaElementLabelsCore.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS));
			fBuffer.append('.');
		}

		fBuffer.append(getElementName(localVariable));

		if (getFlag(flags, JavaElementLabelsCore.F_APP_TYPE_SIGNATURE)) {
			int offset= fBuffer.length();
			fBuffer.append(JavaElementLabelsCore.DECL_STRING);
			appendTypeSignatureLabel(localVariable, localVariable.getTypeSignature(), flags);
			if (getFlag(flags, JavaElementLabelsCore.COLORIZE)) {
				setDecorationsStyle(offset);
			}
		}

		// post qualification
		if (getFlag(flags, JavaElementLabelsCore.F_POST_QUALIFIED)) {
			fBuffer.append(JavaElementLabelsCore.CONCAT_STRING);
			appendElementLabel(localVariable.getDeclaringMember(), JavaElementLabelsCore.M_PARAMETER_TYPES | JavaElementLabelsCore.M_FULLY_QUALIFIED | JavaElementLabelsCore.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS));
		}
	}

	/**
	 * Appends the styled label for a type parameter.
	 *
	 * @param typeParameter the element to render
	 * @param flags the rendering flags. Flags with names starting with 'T_' are considered.
	 */
	public void appendTypeParameterLabel(ITypeParameter typeParameter, long flags) {
		try {
			appendTypeParameterWithBounds(typeParameter, flags);

			// post qualification
			if (getFlag(flags, JavaElementLabelsCore.TP_POST_QUALIFIED)) {
				fBuffer.append(JavaElementLabelsCore.CONCAT_STRING);
				IMember declaringMember= typeParameter.getDeclaringMember();
				appendElementLabel(declaringMember, JavaElementLabelsCore.M_PARAMETER_TYPES | JavaElementLabelsCore.M_FULLY_QUALIFIED | JavaElementLabelsCore.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS));
			}

		} catch (JavaModelException e) {
			Platform.getLog(this.getClass()).error("Error rendering type parameters", e); //$NON-NLS-1$ // NotExistsException will not reach this point
		}
	}

	private void appendTypeParameterWithBounds(ITypeParameter typeParameter, long flags) throws JavaModelException {
		fBuffer.append(getElementName(typeParameter));

		if (typeParameter.exists()) {
			String[] bounds= typeParameter.getBoundsSignatures();
			if (bounds.length > 0 &&
					(bounds.length != 1 || !"Ljava.lang.Object;".equals(bounds[0]))) { //$NON-NLS-1$
				fBuffer.append(" extends "); //$NON-NLS-1$
				for (int j= 0; j < bounds.length; j++) {
					if (j > 0) {
						fBuffer.append(" & "); //$NON-NLS-1$
					}
					appendTypeSignatureLabel(typeParameter, bounds[j], flags);
				}
			}
		}
	}

	/**
	 * Appends the label for a initializer. Considers the I_* flags.
	 *
	 * @param initializer the element to render
	 * @param flags the rendering flags. Flags with names starting with 'I_' are considered.
	 */
	public void appendInitializerLabel(IInitializer initializer, long flags) {
		// qualification
		if (getFlag(flags, JavaElementLabelsCore.I_FULLY_QUALIFIED)) {
			appendTypeLabel(initializer.getDeclaringType(), JavaElementLabelsCore.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS));
			fBuffer.append('.');
		}
		fBuffer.append(JavaElementLabelsMessages.JavaElementLabels_initializer);

		// post qualification
		if (getFlag(flags, JavaElementLabelsCore.I_POST_QUALIFIED)) {
			int offset= fBuffer.length();
			fBuffer.append(JavaElementLabelsCore.CONCAT_STRING);
			appendTypeLabel(initializer.getDeclaringType(), JavaElementLabelsCore.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS));
			if (getFlag(flags, JavaElementLabelsCore.COLORIZE)) {
				setQualifierStyle(offset);
			}
		}
	}

	protected void appendTypeSignatureLabel(IJavaElement enclosingElement, String typeSig, long flags) {
		int sigKind= Signature.getTypeSignatureKind(typeSig);
		switch (sigKind) {
			case Signature.BASE_TYPE_SIGNATURE:
				fBuffer.append(Signature.toString(typeSig));
				break;
			case Signature.ARRAY_TYPE_SIGNATURE:
				appendTypeSignatureLabel(enclosingElement, Signature.getElementType(typeSig), flags);
				for (int dim= Signature.getArrayCount(typeSig); dim > 0; dim--) {
					fBuffer.append('[').append(']');
				}
				break;
			case Signature.CLASS_TYPE_SIGNATURE:
				String baseType= getSimpleTypeName(enclosingElement, typeSig);
				fBuffer.append(baseType);

				String[] typeArguments= Signature.getTypeArguments(typeSig);
				appendTypeArgumentSignaturesLabel(enclosingElement, typeArguments, flags);
				break;
			case Signature.TYPE_VARIABLE_SIGNATURE:
				fBuffer.append(getSimpleTypeName(enclosingElement, typeSig));
				break;
			case Signature.WILDCARD_TYPE_SIGNATURE:
				char ch= typeSig.charAt(0);
				if (ch == Signature.C_STAR) { //workaround for bug 85713
					fBuffer.append('?');
				} else {
					if (ch == Signature.C_EXTENDS) {
						fBuffer.append("? extends "); //$NON-NLS-1$
						appendTypeSignatureLabel(enclosingElement, typeSig.substring(1), flags);
					} else if (ch == Signature.C_SUPER) {
						fBuffer.append("? super "); //$NON-NLS-1$
						appendTypeSignatureLabel(enclosingElement, typeSig.substring(1), flags);
					}
				}
				break;
			case Signature.CAPTURE_TYPE_SIGNATURE:
				appendTypeSignatureLabel(enclosingElement, typeSig.substring(1), flags);
				break;
			case Signature.INTERSECTION_TYPE_SIGNATURE:
				String[] typeBounds= Signature.getIntersectionTypeBounds(typeSig);
				appendTypeBoundsSignaturesLabel(enclosingElement, typeBounds, flags, false);
				break;
			case Signature.UNION_TYPE_SIGNATURE:
				typeBounds= Signature.getUnionTypeBounds(typeSig);
				appendTypeBoundsSignaturesLabel(enclosingElement, typeBounds, flags, true);
				break;
			default:
				// unknown
		}
	}

	private void appendTypeBoundsSignaturesLabel(IJavaElement enclosingElement, String[] typeArgsSig, long flags, boolean isIntersection) {
		for (int i = 0; i < typeArgsSig.length; i++) {
			if (i > 0) {
				if (isIntersection) {
					fBuffer.append(" & "); //$NON-NLS-1$
				} else {
					fBuffer.append(" | "); //$NON-NLS-1$
				}
			}
			appendTypeSignatureLabel(enclosingElement, typeArgsSig[i], flags);
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
		return Signature.getSimpleName(Signature.toString(Signature.getTypeErasure(typeSig)));
	}

	/**
	 * Returns the simple name of the given member.
	 *
	 * @param enclosingElement the enclosing element
	 * @param typeName the name of the member's declaring type
	 * @param memberName the name of the member
	 * @return the simple name of the member
	 */
	protected String getMemberName(IJavaElement enclosingElement, String typeName, String memberName) {
		return memberName;
	}

	private void appendTypeArgumentSignaturesLabel(IJavaElement enclosingElement, String[] typeArgsSig, long flags) {
		if (typeArgsSig.length > 0) {
			fBuffer.append(getLT());
			for (int i = 0; i < typeArgsSig.length; i++) {
				if (i > 0) {
					fBuffer.append(JavaElementLabelsCore.COMMA_STRING);
				}
				appendTypeSignatureLabel(enclosingElement, typeArgsSig[i], flags);
			}
			fBuffer.append(getGT());
		}
	}

	/**
	 * Appends labels for type parameters from a signature.
	 *
	 * @param typeParamSigs the type parameter signature
	 * @param flags flags with render options
	 */
	private void appendTypeParameterSignaturesLabel(String[] typeParamSigs, long flags) {
		if (typeParamSigs.length > 0) {
			fBuffer.append(getLT());
			for (int i = 0; i < typeParamSigs.length; i++) {
				if (i > 0) {
					fBuffer.append(JavaElementLabelsCore.COMMA_STRING);
				}
				fBuffer.append(Signature.getTypeVariable(typeParamSigs[i]));
			}
			fBuffer.append(getGT());
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
	 * Appends the label for a type. Considers the T_* flags.
	 *
	 * @param type the element to render
	 * @param flags the rendering flags. Flags with names starting with 'T_' are considered.
	 */
	public void appendTypeLabel(IType type, long flags) {

		if (getFlag(flags, JavaElementLabelsCore.T_FULLY_QUALIFIED)) {
			IPackageFragment pack= type.getPackageFragment();
			if (!pack.isDefaultPackage()) {
				appendPackageFragmentLabel(pack, (flags & QUALIFIER_FLAGS));
				fBuffer.append('.');
			}
		}
		IJavaElement parent= type.getParent();
		if (getFlag(flags, JavaElementLabelsCore.T_FULLY_QUALIFIED | JavaElementLabelsCore.T_CONTAINER_QUALIFIED)) {
			IType declaringType= type.getDeclaringType();
			if (declaringType != null) {
				appendTypeLabel(declaringType, JavaElementLabelsCore.T_CONTAINER_QUALIFIED | (flags & QUALIFIER_FLAGS));
				fBuffer.append('.');
			}
			int parentType= parent.getElementType();
			if (parentType == IJavaElement.METHOD || parentType == IJavaElement.FIELD || parentType == IJavaElement.INITIALIZER) { // anonymous or local
				appendElementLabel(parent, 0);
				fBuffer.append('.');
			}
		}

		String typeName;
		boolean isAnonymous= false;
		if (type.isLambda()) {
			typeName= "() -> {...}"; //$NON-NLS-1$
			try {
				String[] superInterfaceSignatures= type.getSuperInterfaceTypeSignatures();
				if (superInterfaceSignatures.length > 0) {
					typeName= typeName + ' ' + getSimpleTypeName(type, superInterfaceSignatures[0]);
				}
			} catch (JavaModelException e) {
				//ignore
			}

		} else {
			typeName= getElementName(type);
			try {
				isAnonymous= type.isAnonymous();
			} catch (JavaModelException e1) {
				// should not happen, but let's play safe:
				isAnonymous= typeName.length() == 0;
			}
			if (isAnonymous) {
				try {
					if (parent instanceof IField && type.isEnum()) {
						typeName= '{' + JavaElementLabelsCore.ELLIPSIS_STRING + '}';
					} else {
						String supertypeName= null;
						String[] superInterfaceSignatures= type.getSuperInterfaceTypeSignatures();
						if (superInterfaceSignatures.length > 0) {
							supertypeName= getSimpleTypeName(type, superInterfaceSignatures[0]);
						} else {
							String supertypeSignature= type.getSuperclassTypeSignature();
							if (supertypeSignature != null) {
								supertypeName= getSimpleTypeName(type, supertypeSignature);
							}
						}
						if (supertypeName == null) {
							typeName= JavaElementLabelsMessages.JavaElementLabels_anonym;
						} else {
							typeName= Messages.format(JavaElementLabelsMessages.JavaElementLabels_anonym_type, supertypeName);
						}
					}
				} catch (JavaModelException e) {
					//ignore
					typeName= JavaElementLabelsMessages.JavaElementLabels_anonym;
				}
			}
		}
		fBuffer.append(typeName);

		if (getFlag(flags, JavaElementLabelsCore.T_TYPE_PARAMETERS)) {
			if (getFlag(flags, JavaElementLabelsCore.USE_RESOLVED) && type.isResolved()) {
				BindingKey key= new BindingKey(type.getKey());
				if (key.isParameterizedType()) {
					String[] typeArguments= key.getTypeArguments();
					appendTypeArgumentSignaturesLabel(type, typeArguments, flags);
				} else {
					String[] typeParameters= Signature.getTypeParameters(key.toSignature());
					appendTypeParameterSignaturesLabel(typeParameters, flags);
				}
			} else if (type.exists()) {
				try {
					appendTypeParametersLabels(type.getTypeParameters(), flags);
				} catch (JavaModelException e) {
					// ignore
				}
			}
		}

		// category
		if (getFlag(flags, JavaElementLabelsCore.T_CATEGORY) && type.exists()) {
			try {
				appendCategoryLabel(type, flags);
			} catch (JavaModelException e) {
				// ignore
			}
		}

		// post qualification
		if (getFlag(flags, JavaElementLabelsCore.T_POST_QUALIFIED)) {
			int offset= fBuffer.length();
			fBuffer.append(JavaElementLabelsCore.CONCAT_STRING);
			IType declaringType= type.getDeclaringType();
			if (declaringType == null && type.isBinary() && isAnonymous) {
				// workaround for Bug 87165: [model] IType#getDeclaringType() does not work for anonymous binary type
				String tqn= type.getTypeQualifiedName();
				int lastDollar= tqn.lastIndexOf('$');
				if (lastDollar != 1) {
					String declaringTypeCF= tqn.substring(0, lastDollar) + ".class"; //$NON-NLS-1$
					declaringType= type.getPackageFragment().getOrdinaryClassFile(declaringTypeCF).getType();
					try {
						ISourceRange typeSourceRange= type.getSourceRange();
						if (declaringType.exists() && SourceRange.isAvailable(typeSourceRange)) {
							IJavaElement realParent= declaringType.getTypeRoot().getElementAt(typeSourceRange.getOffset() - 1);
							if (realParent != null) {
								parent= realParent;
							}
						}
					} catch (JavaModelException e) {
						// ignore
					}
				}
			}
			if (declaringType != null) {
				appendTypeLabel(declaringType, JavaElementLabelsCore.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS));
				int parentType= parent.getElementType();
				if (parentType == IJavaElement.METHOD || parentType == IJavaElement.FIELD || parentType == IJavaElement.INITIALIZER) { // anonymous or local
					fBuffer.append('.');
					appendElementLabel(parent, 0);
				}
			} else {
				appendPackageFragmentLabel(type.getPackageFragment(), flags & QUALIFIER_FLAGS);
			}
			if (getFlag(flags, JavaElementLabelsCore.COLORIZE)) {
				setQualifierStyle(offset);
			}
		}
	}

	/**
	 * Returns the string for rendering the {@link IJavaElement#getElementName() element name} of
	 * the given element.
	 * <p>
	 * <strong>Note:</strong> This class only calls this helper for those elements where (
	 * JavaElementLinks) has the need to render the name differently.
	 * </p>
	 *
	 * @param element the element to render
	 * @return the string for rendering the element name
	 */
	protected String getElementName(IJavaElement element) {
		return element.getElementName();
	}

	/**
	 * Appends the label for a import container, import or package declaration. Considers the D_* flags.
	 *
	 * @param declaration the element to render
	 * @param flags the rendering flags. Flags with names starting with 'D_' are considered.
	 */
	public void appendDeclarationLabel(IJavaElement declaration, long flags) {
		if (getFlag(flags, JavaElementLabelsCore.D_QUALIFIED)) {
			IJavaElement openable= (IJavaElement) declaration.getOpenable();
			if (openable != null) {
				appendElementLabel(openable, JavaElementLabelsCore.CF_QUALIFIED | JavaElementLabelsCore.CU_QUALIFIED | (flags & QUALIFIER_FLAGS));
				fBuffer.append('/');
			}
		}
		if (declaration.getElementType() == IJavaElement.IMPORT_CONTAINER) {
			fBuffer.append(JavaElementLabelsMessages.JavaElementLabels_import_container);
		} else {
			fBuffer.append(getElementName(declaration));
		}
		// post qualification
		if (getFlag(flags, JavaElementLabelsCore.D_POST_QUALIFIED)) {
			int offset= fBuffer.length();
			IJavaElement openable= (IJavaElement) declaration.getOpenable();
			if (openable != null) {
				fBuffer.append(JavaElementLabelsCore.CONCAT_STRING);
				appendElementLabel(openable, JavaElementLabelsCore.CF_QUALIFIED | JavaElementLabelsCore.CU_QUALIFIED | (flags & QUALIFIER_FLAGS));
			}
			if (getFlag(flags, JavaElementLabelsCore.COLORIZE)) {
				setQualifierStyle(offset);
			}
		}
	}

	/**
	 * Appends the label for a class file. Considers the CF_* flags.
	 *
	 * @param classFile the element to render
	 * @param flags the rendering flags. Flags with names starting with 'CF_' are considered.
	 */
	public void appendClassFileLabel(IClassFile classFile, long flags) {
		if (getFlag(flags, JavaElementLabelsCore.CF_QUALIFIED)) {
			IPackageFragment pack= (IPackageFragment) classFile.getParent();
			if (!pack.isDefaultPackage()) {
				appendPackageFragmentLabel(pack, (flags & QUALIFIER_FLAGS));
				fBuffer.append('.');
			}
		}
		fBuffer.append(classFile.getElementName());

		if (getFlag(flags, JavaElementLabelsCore.CF_POST_QUALIFIED)) {
			int offset= fBuffer.length();
			fBuffer.append(JavaElementLabelsCore.CONCAT_STRING);
			appendPackageFragmentLabel((IPackageFragment) classFile.getParent(), flags & QUALIFIER_FLAGS);
			if (getFlag(flags, JavaElementLabelsCore.COLORIZE)) {
				setQualifierStyle(offset);
			}
		}
	}

	/**
	 * Appends the label for a compilation unit. Considers the CU_* flags.
	 *
	 * @param cu the element to render
	 * @param flags the rendering flags. Flags with names starting with 'CU_' are considered.
	 */
	public void appendCompilationUnitLabel(ICompilationUnit cu, long flags) {
		if (getFlag(flags, JavaElementLabelsCore.CU_QUALIFIED)) {
			IPackageFragment pack= (IPackageFragment) cu.getParent();
			if (!pack.isDefaultPackage()) {
				appendPackageFragmentLabel(pack, (flags & QUALIFIER_FLAGS));
				fBuffer.append('.');
			}
		}
		fBuffer.append(cu.getElementName());

		if (getFlag(flags, JavaElementLabelsCore.CU_POST_QUALIFIED)) {
			int offset= fBuffer.length();
			fBuffer.append(JavaElementLabelsCore.CONCAT_STRING);
			appendPackageFragmentLabel((IPackageFragment) cu.getParent(), flags & QUALIFIER_FLAGS);
			if (getFlag(flags, JavaElementLabelsCore.COLORIZE)) {
				setQualifierStyle(offset);
			}
		}
	}

	/**
	 * Appends the label for a package fragment. Considers the P_* flags.
	 *
	 * @param pack the element to render
	 * @param flags the rendering flags. Flags with names starting with P_' are considered.
	 */
	public void appendPackageFragmentLabel(IPackageFragment pack, long flags) {
		if (getFlag(flags, JavaElementLabelsCore.P_QUALIFIED)) {
			appendPackageFragmentRootLabel((IPackageFragmentRoot) pack.getParent(), JavaElementLabelsCore.ROOT_QUALIFIED);
			fBuffer.append('/');
		}
		if (pack.isDefaultPackage()) {
			fBuffer.append(JavaElementLabelsCore.DEFAULT_PACKAGE);
		} else if (getFlag(flags, JavaElementLabelsCore.P_COMPRESSED)) {
			if (isPackageNameAbbreviationEnabled())
				appendAbbreviatedPackageFragment(pack);
			else
				appendCompressedPackageFragment(pack);
		} else {
			fBuffer.append(getElementName(pack));
		}
		if (getFlag(flags, JavaElementLabelsCore.P_POST_QUALIFIED)) {
			int offset= fBuffer.length();
			fBuffer.append(JavaElementLabelsCore.CONCAT_STRING);
			appendPackageFragmentRootLabel((IPackageFragmentRoot) pack.getParent(), JavaElementLabelsCore.ROOT_QUALIFIED);
			if (getFlag(flags, JavaElementLabelsCore.COLORIZE)) {
				setQualifierStyle(offset);
			}
		}
	}

	protected boolean isPackageNameAbbreviationEnabled() {
		return false;
	}

	protected void appendCompressedPackageFragment(IPackageFragment pack) {
		appendCompressedPackageFragment(pack.getElementName());
	}

	protected void refreshPackageNamePattern() {
		// core does nothing for this
	}

	protected void appendCompressedPackageFragment(String elementName) {
		refreshPackageNamePattern();
		if (fgPkgNameLength < 0) {
			fBuffer.append(elementName);
			return;
		}
		String name= elementName;
		int start= 0;
		int dot= name.indexOf('.', start);
		while (dot > 0) {
			if (dot - start > fgPkgNameLength-1) {
				fBuffer.append(fgPkgNamePrefix);
				if (fgPkgNameChars > 0)
					fBuffer.append(name.substring(start, Math.min(start+ fgPkgNameChars, dot)));
				fBuffer.append(fgPkgNamePostfix);
			} else
				fBuffer.append(name.substring(start, dot + 1));
			start= dot + 1;
			dot= name.indexOf('.', start);
		}
		fBuffer.append(name.substring(start));
	}

	@SuppressWarnings("unused")
	protected void appendAbbreviatedPackageFragment(IPackageFragment pack) {
		// core does not do this
	}


	/**
	 * Appends the label for a package fragment root. Considers the ROOT_* flags.
	 *
	 * @param root the element to render
	 * @param flags the rendering flags. Flags with names starting with ROOT_' are considered.
	 */
	public void appendPackageFragmentRootLabel(IPackageFragmentRoot root, long flags) {
		// Handle variables different
		if (getFlag(flags, JavaElementLabelsCore.ROOT_VARIABLE) && appendVariableLabel(root, flags)) {
			return;
		}
		if (root.isArchive()) {
			appendArchiveLabel(root, flags);
		} else {
			appendFolderLabel(root, flags);
		}
	}

	protected void appendModuleLabel(IModuleDescription module, long flags) {
		fBuffer.append(module.getElementName());
		// category
		if (getFlag(flags, JavaElementLabelsCore.MOD_CATEGORY) && module.exists()) {
			try {
				appendCategoryLabel(module, flags);
			} catch (JavaModelException e) {
				// ignore
			}
		}
	}

	private void appendArchiveLabel(IPackageFragmentRoot root, long flags) {
		boolean external= root.isExternal();
		if (external) {
			appendExternalArchiveLabel(root, flags);
		} else {
			appendInternalArchiveLabel(root, flags);
		}
	}

	private boolean appendVariableLabel(IPackageFragmentRoot root, long flags) {
		try {
			IClasspathEntry rawEntry= root.getRawClasspathEntry();
			if (rawEntry.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
				IClasspathEntry entry= JavaModelUtil.getClasspathEntry(root);
				if (entry.getReferencingEntry() != null) {
					return false; // not the variable entry itself, but a referenced entry
				}
				IPath path= rawEntry.getPath().makeRelative();

				if (getFlag(flags, JavaElementLabelsCore.REFERENCED_ROOT_POST_QUALIFIED)) {
					int segements= path.segmentCount();
					if (segements > 0) {
						fBuffer.append(path.segment(segements - 1));
						if (segements > 1) {
							fBuffer.append(JavaElementLabelsCore.CONCAT_STRING);
							fBuffer.append(path.removeLastSegments(1).toOSString());
						}
					} else {
						fBuffer.append(path.toString());
					}
				} else {
					fBuffer.append(path.toString());
				}
				fBuffer.append(JavaElementLabelsCore.CONCAT_STRING);
				if (root.isExternal()) {
					fBuffer.append(root.getPath().toOSString());
				} else {
					fBuffer.append(root.getPath().makeRelative().toString());
				}

				return true;
			}
		} catch (JavaModelException e) {
			// problems with class path, ignore (bug 202792)
			return false;
		}
		return false;
	}

	private void appendExternalArchiveLabel(IPackageFragmentRoot root, long flags) {
		IPath path;
		IClasspathEntry classpathEntry= null;
		try {
			classpathEntry= JavaModelUtil.getClasspathEntry(root);
			IPath rawPath= classpathEntry.getPath();
			if (classpathEntry.getEntryKind() != IClasspathEntry.CPE_CONTAINER && !rawPath.isAbsolute()) {
				path= rawPath;
			} else {
				path= root.getPath();
			}
		} catch (JavaModelException e) {
			path= root.getPath();
		}
		if (getFlag(flags, JavaElementLabelsCore.REFERENCED_ROOT_POST_QUALIFIED)) {
			int segments= path.segmentCount();
			if (segments > 0) {
				fBuffer.append(path.segment(segments - 1));
				if (segments > 1 || path.getDevice() != null) {
					fBuffer.append(JavaElementLabelsCore.CONCAT_STRING);
					fBuffer.append(path.removeLastSegments(1).toOSString());
				}
				if (classpathEntry != null) {
					IClasspathEntry referencingEntry= classpathEntry.getReferencingEntry();
					if (referencingEntry != null) {
						fBuffer.append(NLS.bind(JavaElementLabelsMessages.JavaElementLabels_onClassPathOf, new Object[] { Name.CLASS_PATH.toString(), referencingEntry.getPath().lastSegment() }));
					}
				}
			} else {
				fBuffer.append(path.toOSString());
			}
		} else {
			fBuffer.append(path.toOSString());
		}
	}

	private void appendInternalArchiveLabel(IPackageFragmentRoot root, long flags) {
		IResource resource= root.getResource();
		boolean rootQualified= getFlag(flags, JavaElementLabelsCore.ROOT_QUALIFIED);
		if (rootQualified) {
			fBuffer.append(root.getPath().makeRelative().toString());
		} else {
			fBuffer.append(root.getElementName());
			boolean referencedPostQualified= getFlag(flags, JavaElementLabelsCore.REFERENCED_ROOT_POST_QUALIFIED);
			if (referencedPostQualified && isReferenced(root)) {
				fBuffer.append(JavaElementLabelsCore.CONCAT_STRING);
				fBuffer.append(resource.getParent().getFullPath().makeRelative().toString());
			} else if (getFlag(flags, JavaElementLabelsCore.ROOT_POST_QUALIFIED)) {
				fBuffer.append(JavaElementLabelsCore.CONCAT_STRING);
				fBuffer.append(root.getParent().getPath().makeRelative().toString());
			}
			if (referencedPostQualified) {
				try {
					IClasspathEntry referencingEntry= JavaModelUtil.getClasspathEntry(root).getReferencingEntry();
					if (referencingEntry != null) {
						fBuffer.append(NLS.bind(JavaElementLabelsMessages.JavaElementLabels_onClassPathOf, new Object[] { Name.CLASS_PATH.toString(), referencingEntry.getPath().lastSegment() }));
					}
				} catch (JavaModelException e) {
					// ignore
				}
			}
		}
	}

	private void appendFolderLabel(IPackageFragmentRoot root, long flags) {
		IResource resource= root.getResource();
		if (resource == null) {
			appendExternalArchiveLabel(root, flags);
			return;
		}

		boolean rootQualified= getFlag(flags, JavaElementLabelsCore.ROOT_QUALIFIED);
		boolean referencedQualified= getFlag(flags, JavaElementLabelsCore.REFERENCED_ROOT_POST_QUALIFIED) && isReferenced(root);
		if (rootQualified) {
			fBuffer.append(root.getPath().makeRelative().toString());
		} else {
			IPath projectRelativePath= resource.getProjectRelativePath();
			if (projectRelativePath.segmentCount() == 0) {
				fBuffer.append(resource.getName());
				referencedQualified= false;
			} else {
				fBuffer.append(projectRelativePath.toString());
			}

			if (referencedQualified) {
				fBuffer.append(JavaElementLabelsCore.CONCAT_STRING);
				fBuffer.append(resource.getProject().getName());
			} else if (getFlag(flags, JavaElementLabelsCore.ROOT_POST_QUALIFIED)) {
				fBuffer.append(JavaElementLabelsCore.CONCAT_STRING);
				fBuffer.append(root.getParent().getElementName());
			} else {
				return;
			}
		}
	}

	/**
	 * Returns <code>true</code> if the given package fragment root is
	 * referenced. This means it is a descendant of a different project but is referenced
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
}
