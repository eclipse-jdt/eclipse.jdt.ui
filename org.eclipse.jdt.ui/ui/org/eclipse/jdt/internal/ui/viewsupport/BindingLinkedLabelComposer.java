/*******************************************************************************
 * Copyright (c) 2015, 2019 GK Software AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport;

import static org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks.JAVADOC_SCHEME;
import static org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks.createHeaderLink;
import static org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks.createURI;

import java.net.URISyntaxException;

import org.eclipse.jface.viewers.StyledString;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IModuleBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks.JavaElementLinkedLabelComposer;

public class BindingLinkedLabelComposer extends JavaElementLinkedLabelComposer {

	private static final String PARAMETER_ELLIPSIS_LABEL= "(...)"; //$NON-NLS-1$
	private static final String ANON_TYPE_TAIL= "() {...}"; //$NON-NLS-1$
	private static final String LAMBDA_LABEL= "() -&gt; {...}"; //$NON-NLS-1$
	private static final String MISSING_LABEL= "MISSING"; //$NON-NLS-1$

	private static final String INIT_NAME= ""; //$NON-NLS-1$

	private static final long M_ALL_QUALIFIED= JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_FULLY_QUALIFIED;

	private static final long IS_POST_QUALIFICATION= 1L << 63;
	private static final long TP_BOUNDS= 1L << 62;

	private IJavaElement fEnclosingElement;
	private boolean fIsFromSource;

	public BindingLinkedLabelComposer(IJavaElement enclosingElement, StringBuffer buffer, boolean isFromSource) {
		super(enclosingElement, buffer);
		fEnclosingElement= enclosingElement;
		fIsFromSource= isFromSource;
	}

	/**
	 * <p>Supported flags:
	 * <ul>
	 * <li>{@link JavaElementLabels#ALL_FULLY_QUALIFIED} (set)</li>
	 * <li>{@link JavaElementLabels#M_PRE_RETURNTYPE}</li>
	 * <li>{@link JavaElementLabels#M_PARAMETER_ANNOTATIONS}</li>
 	 * <li>{@link JavaElementLabels#M_PARAMETER_TYPES}</li>
	 * <li>{@link JavaElementLabels#M_PARAMETER_NAMES}</li>
	 * <li>{@link JavaElementLabels#M_EXCEPTIONS}</li>
	 * <li>{@link JavaElementLabels#M_PRE_TYPE_PARAMETERS}</li>
	 * <li>{@link JavaElementLabels#F_PRE_TYPE_SIGNATURE}</li>
	 * <li>{@link JavaElementLabels#T_TYPE_PARAMETERS}</li>
	 * <li>{@link JavaElementLabels#USE_RESOLVED}</li>
	 * <li>{@link JavaElementLabels#M_POST_QUALIFIED}</li>
	 * <li>{@link JavaElementLabels#F_POST_QUALIFIED}</li>
	 * <li>{@link JavaElementLabels#T_POST_QUALIFIED}</li>
	 * <li>{@link JavaElementLabels#TP_POST_QUALIFIED}</li>
	 * </ul>
	 * @param binding a binding to be rendered
	 * @param flags rendering flags, see above for supported values.
	 */
	public void appendBindingLabel(IBinding binding, long flags) {
		switch (binding.getKind()) {
			case IBinding.METHOD:
				appendMethodBindingLabel((IMethodBinding) binding, flags);
				return;
			case IBinding.TYPE:
				appendTypeBindingLabel((ITypeBinding) binding, flags | TP_BOUNDS);
				return;
			case IBinding.VARIABLE:
				appendVariableLabel((IVariableBinding) binding, flags);
				return;
			case IBinding.PACKAGE:
				appendPackageLabel((IPackageBinding) binding, flags);
				break;
			case IBinding.MODULE:
				appendModuleLabel((IModuleBinding) binding, flags);
				break;
			case IBinding.ANNOTATION:
			case IBinding.MEMBER_VALUE_PAIR:
				// not used for hovers
		}
	}

	private void appendMethodBindingLabel(IMethodBinding method, long flags) {
		long qualificationFlags = flags & (QUALIFIER_FLAGS | JavaElementLabels.ALL_FULLY_QUALIFIED | JavaElementLabels.T_CONTAINER_QUALIFIED);
		qualificationFlags |= TP_BOUNDS;
		if (getFlag(flags, JavaElementLabels.T_POST_QUALIFIED))
			flags |= JavaElementLabels.M_POST_QUALIFIED;
		flags &= ~(JavaElementLabels.T_POST_QUALIFIED | TP_BOUNDS);
		IMethodBinding origMethod= method;
		IBinding declaringMember= method.getDeclaringMember();
		if (declaringMember != null) {
			if (isEnclosingElement(method.getJavaElement())) {
				// don't render top-level as lambda
				method= method.getMethodDeclaration(); // for the main part show the SAM instead
			} else {
				appendLambdaLabel(method, flags);
				return;
			}
		}

		if (!getFlag(flags, JavaElementLabels.USE_RESOLVED)) {
			method= method.getMethodDeclaration();
		}
		if (fIsFromSource) {
			flags &= ~JavaElementLabels.T_FULLY_QUALIFIED;
		}
		boolean isInitializer= INIT_NAME.equals(method.getName());

		// type parameters
		if (getFlag(flags, JavaElementLabels.M_PRE_TYPE_PARAMETERS)) {
			ITypeBinding[] typeParameters= method.getTypeParameters();
			if (typeParameters.length > 0) {
				appendTypeArgumentsBindingLabel(typeParameters, null, flags | TP_BOUNDS);
				fBuffer.append(' ');
			}
		}

		// return type
		if (getFlag(flags, JavaElementLabels.M_PRE_RETURNTYPE) && !method.isConstructor() && !isInitializer) {
			appendTypeBindingLabel(method.getReturnType(), flags);
			fBuffer.append(' ');
		}

		// qualification
		if (getFlag(flags, JavaElementLabels.M_FULLY_QUALIFIED | JavaElementLabels.T_CONTAINER_QUALIFIED)) {
			appendTypeBindingLabel(method.getDeclaringClass(), qualificationFlags);
			fBuffer.append('.');
		}

		if (isInitializer) {
			fBuffer.append(JavaUIMessages.JavaElementLabels_initializer);
		} else {
			appendNameLink(method, origMethod);
		}

		if (!isInitializer) {
			// constructor type arguments
			if (getFlag(flags, JavaElementLabels.T_TYPE_PARAMETERS) && method.isConstructor()) {
				appendTypeArgumentsBindingLabel(method.getTypeArguments(), null, flags);
			}

			// parameters
			fBuffer.append('(');
			if (getFlag(flags, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES)) {
				ITypeBinding[] types= null;
				int nParams= 0;
				boolean renderVarargs= false;
				boolean isPolymorphic= false;
				IMethod iMethod= getIMethod(method);
				if (getFlag(flags, JavaElementLabels.M_PARAMETER_TYPES)) {
					types= method.getParameterTypes();
					nParams= types.length;
					renderVarargs= method.isVarargs();
					// retrieval of flag isPolymorphic uses strategy from JavaElementLabelComposer:
					if (getFlag(flags, JavaElementLabels.USE_RESOLVED)
							&& iMethod != null
							&& iMethod.isResolved()
							&& iMethod.getParameterTypes().length == 1
							&& JavaModelUtil.isPolymorphicSignature(iMethod)) {
						renderVarargs= false;
						isPolymorphic= true;
					}
				}
				String[] names= null;
				if (getFlag(flags, JavaElementLabels.M_PARAMETER_NAMES) && iMethod != null) {
					// mostly from JavaElementLabelComposer:
					try {
						names= iMethod.getParameterNames();
						if (isPolymorphic) {
							// handled specially below
						} else if (types == null) {
							nParams= names.length;
						} else { // types != null
							if (nParams != names.length) {
								if (types.length > names.length) {
									// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=99137
									nParams= names.length;
									ITypeBinding[] typesWithoutSyntheticParams= new ITypeBinding[nParams];
									System.arraycopy(types, types.length - nParams, typesWithoutSyntheticParams, 0, nParams);
									types= typesWithoutSyntheticParams;
								} else {
									// https://bugs.eclipse.org/bugs/show_bug.cgi?id=101029
									// JavaPlugin.logErrorMessage("JavaElementLabels: Number of param types(" + nParams + ") != number of names(" + names.length + "): " + method.getElementName());   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
									names= null; // no names rendered
								}
							}
						}
					} catch (JavaModelException e) {
						JavaPlugin.log(e);
					}
				}
				IAnnotationBinding[][] parameterAnnotations= null;
				if (nParams > 0 && getFlag(flags, JavaElementLabels.M_PARAMETER_ANNOTATIONS)) {
					parameterAnnotations= new IAnnotationBinding[nParams][];
					for (int i= 0; i < nParams; i++) {
						parameterAnnotations[i]= method.getParameterAnnotations(i);
					}
				}

				for (int i= 0; i < nParams; i++) {
					if (i > 0)
						fBuffer.append(JavaElementLabels.COMMA_STRING);
					if (parameterAnnotations != null && i < parameterAnnotations.length) {
						appendAnnotationLabels(parameterAnnotations[i], flags, false, true);
					}

					if (types != null) {
						ITypeBinding paramSig= types[i];
						if (renderVarargs && (i == nParams - 1)) {
							int newDim= paramSig.getDimensions() - 1;
							appendTypeBindingLabel(paramSig.getElementType(), flags);
							for (int k= 0; k < newDim; k++) {
								fBuffer.append('[').append(']');
							}
							fBuffer.append(JavaElementLabels.ELLIPSIS_STRING);
						} else {
							appendTypeBindingLabel(paramSig, flags);
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
				if (method.getParameterTypes().length > 0) {
					fBuffer.append(JavaElementLabels.ELLIPSIS_STRING);
				}
			}
			fBuffer.append(')');


			if (getFlag(flags, JavaElementLabels.M_EXCEPTIONS)) {
				ITypeBinding[] types= method.getExceptionTypes();
				if (types.length > 0) {
					fBuffer.append(" throws "); //$NON-NLS-1$
					for (int i= 0; i < types.length; i++) {
						if (i > 0)
							fBuffer.append(JavaElementLabels.COMMA_STRING);
						appendTypeBindingLabel(types[i], flags);
					}
				}
			}

			if (getFlag(flags, JavaElementLabels.M_APP_TYPE_PARAMETERS)) {
				int offset= fBuffer.length();
				if (method.isParameterizedMethod()) {
					appendTypeArgumentsBindingLabel(method.getTypeArguments(), String.valueOf(' '), flags);
				} else {
					appendTypeArgumentsBindingLabel(method.getTypeParameters(), String.valueOf(' '), flags|TP_BOUNDS);
				}
				if (getFlag(flags, JavaElementLabels.COLORIZE) && offset != fBuffer.length()) {
					if (fBuffer instanceof FlexibleBuffer)
						((FlexibleBuffer)fBuffer).setStyle(offset, fBuffer.length() - offset, StyledString.DECORATIONS_STYLER);
				}
			}
		}

		// post qualification
		if (getFlag(flags, JavaElementLabels.M_POST_QUALIFIED)) {
			fBuffer.append(JavaElementLabels.CONCAT_STRING);
			if (declaringMember != null && origMethod != method) {
				// show lambda as qualification of a SAM
				appendLambdaLabel(origMethod, qualificationFlags | JavaElementLabels.ALL_FULLY_QUALIFIED);
				fBuffer.append(' ');
				appendTypeBindingLabel(origMethod.getMethodDeclaration().getDeclaringClass(), M_ALL_QUALIFIED | IS_POST_QUALIFICATION);
			} else {
				appendTypeBindingLabel(method.getDeclaringClass(), getPostQualificationFlags(flags));
			}
		}
	}

	private void appendVariableLabel(IVariableBinding variable, long flags) {
		long qualificationFlags = flags & (QUALIFIER_FLAGS | JavaElementLabels.ALL_FULLY_QUALIFIED | JavaElementLabels.T_CONTAINER_QUALIFIED);
		qualificationFlags |= TP_BOUNDS;
		if (fIsFromSource) {
			flags &= ~JavaElementLabels.T_FULLY_QUALIFIED;
		}
		if (getFlag(flags, JavaElementLabels.T_POST_QUALIFIED))
			flags |= JavaElementLabels.F_POST_QUALIFIED;
		flags &= ~(JavaElementLabels.T_POST_QUALIFIED | TP_BOUNDS);

		if (getFlag(flags, JavaElementLabels.F_PRE_TYPE_SIGNATURE) && !Flags.isEnum(variable.getModifiers())) {
			appendTypeBindingLabel(variable.getType(), flags);
			fBuffer.append(' ');
		}

		// qualification
		if (getFlag(flags, JavaElementLabels.F_FULLY_QUALIFIED)) {
			appendVariableQualification(variable, qualificationFlags);
			fBuffer.append('.');
		}
		if (variable.isField()) {
			appendNameLink(variable, variable);
		} else {
			fBuffer.append(variable.getName());
		}

		if (getFlag(flags, JavaElementLabels.F_POST_QUALIFIED)) {
			fBuffer.append(JavaElementLabels.CONCAT_STRING);
			appendVariableQualification(variable, getPostQualificationFlags(qualificationFlags));
		}
	}

	private void appendVariableQualification(IVariableBinding variable, long flags) {
		if (variable.isField()) {
			appendTypeBindingLabel(variable.getDeclaringClass(), flags);
		} else {
			IMethodBinding declaringMethod= variable.getDeclaringMethod();
			if (declaringMethod != null) {
				IBinding declaringMember= declaringMethod.getDeclaringMember();
				if (declaringMember != null) {
					appendLambdaLabel(declaringMethod, flags);
					fBuffer.append(' ');
					appendMethodBindingLabel(declaringMethod.getMethodDeclaration(), (flags & QUALIFIER_FLAGS) | M_ALL_QUALIFIED);
				} else {
					appendMethodBindingLabel(declaringMethod, flags | M_ALL_QUALIFIED);
				}
			} else {
				// workaround for: local variable inside initializer doesn't yet expose the #getDeclaringMethod();
				IJavaElement element= variable.getJavaElement();
				if (element != null && element.getParent() != null)
					appendElementLabel(element.getParent(), flags);
				else
					fBuffer.append(MISSING_LABEL);
			}
		}
	}

	private void appendLambdaLabel(IMethodBinding lambdaBinding, long flags) {
		long qualificationFlags = flags & (QUALIFIER_FLAGS | JavaElementLabels.ALL_FULLY_QUALIFIED);
		if (fIsFromSource) {
			flags &= ~JavaElementLabels.T_FULLY_QUALIFIED;
		}
		if (getFlag(flags, JavaElementLabels.M_FULLY_QUALIFIED | JavaElementLabels.T_FULLY_QUALIFIED)) {
			appendBindingLabel(lambdaBinding.getDeclaringMember(), qualificationFlags);
			fBuffer.append('.');
			fBuffer.append(LAMBDA_LABEL);
		} else {
			IMethodBinding sam= lambdaBinding.getMethodDeclaration();
			appendMethodBindingLabel(sam, flags & ~JavaElementLabels.ALL_POST_QUALIFIED);
		}
		if (getFlag(flags, JavaElementLabels.M_POST_QUALIFIED|JavaElementLabels.T_POST_QUALIFIED)) {
			fBuffer.append(JavaElementLabels.CONCAT_STRING);
			qualificationFlags |= JavaElementLabels.ALL_FULLY_QUALIFIED;
			appendBindingLabel(lambdaBinding.getDeclaringMember(), qualificationFlags);
			fBuffer.append('.');
			fBuffer.append(LAMBDA_LABEL);
			fBuffer.append(' ');
			appendTypeBindingLabel(lambdaBinding.getDeclaringClass(), flags & (QUALIFIER_FLAGS | JavaElementLabels.ALL_FULLY_QUALIFIED) | IS_POST_QUALIFICATION);
		}
	}

	private void appendTypeBindingLabel(ITypeBinding typeBinding, long flags) {
		long typeRefFlags= flags & ~JavaElementLabels.ALL_POST_QUALIFIED;
		if (fIsFromSource) {
			typeRefFlags &= ~(JavaElementLabels.ALL_FULLY_QUALIFIED | TP_BOUNDS);
		}
		// qualification of anonymous (class or lambda):
		IBinding declaringMember= typeBinding.getDeclaringMember();
		if (declaringMember != null
				&& getFlag(flags, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.T_CONTAINER_QUALIFIED))
		{
			long qualificationFlags= flags;
			if (getFlag(flags, JavaElementLabels.T_FULLY_QUALIFIED))
				qualificationFlags |= JavaElementLabels.ALL_FULLY_QUALIFIED;
			else if (getFlag(flags, JavaElementLabels.T_CONTAINER_QUALIFIED))
				qualificationFlags |= (JavaElementLabels.F_FULLY_QUALIFIED | JavaElementLabels.M_FULLY_QUALIFIED);
			appendBindingLabel(declaringMember, qualificationFlags);
			fBuffer.append('.');
			flags &= ~(JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.M_FULLY_QUALIFIED | JavaElementLabels.T_CONTAINER_QUALIFIED); // qualification is done
		}
		if (!typeBinding.isArray()) { // different textual order for type annotations on array types
			appendAnnotationLabels(typeBinding.getTypeAnnotations(), typeRefFlags, false, true);
		}
		if (typeBinding.isPrimitive()) {
			fBuffer.append(typeBinding.getName());
		} else if (typeBinding.isArray()) {
			appendTypeBindingLabel(typeBinding.getElementType(), flags);
			ITypeBinding typeAtDimension = typeBinding;
			while (typeAtDimension.isArray()) {
				IAnnotationBinding[] typeAnnotations= typeAtDimension.getTypeAnnotations();
				if (typeAnnotations.length > 0) {
					appendAnnotationLabels(typeAnnotations, typeRefFlags, true, false);
				}
				fBuffer.append('[').append(']');
				typeAtDimension = typeAtDimension.getComponentType();
			}
		} else if (typeBinding.isClass() || typeBinding.isInterface() || typeBinding.isEnum()) {
			fBuffer.append(getTypeLink(typeBinding, flags));
			if (getFlag(flags, JavaElementLabels.T_TYPE_PARAMETERS)) {
				ITypeBinding[] typeArguments= typeBinding.getTypeArguments();
				if (typeArguments.length > 0) {
					appendTypeArgumentsBindingLabel(typeArguments, null, typeRefFlags);
				} else {
					ITypeBinding[] typeParameters= typeBinding.getTypeParameters();
					appendTypeArgumentsBindingLabel(typeParameters, null, typeRefFlags | TP_BOUNDS);
				}
			}
		} else if (typeBinding.isParameterizedType()) {
			fBuffer.append(getTypeLink(typeBinding.getTypeDeclaration(), flags));
			fBuffer.append(getLT());
			ITypeBinding[] typeArguments= typeBinding.getTypeArguments();
			for (int i= 0; i < typeArguments.length; i++) {
				if (i > 0)
					fBuffer.append(JavaElementLabels.COMMA_STRING);
				appendTypeBindingLabel(typeArguments[i], typeRefFlags);
			}
			fBuffer.append(getGT());
		} else if (typeBinding.isTypeVariable()) {
			appendNameLink(typeBinding, typeBinding);
			if (getFlag(flags, TP_BOUNDS)) {
				ITypeBinding[] bounds= typeBinding.getTypeBounds();
				if (hasRelevantBound(bounds)) {
					fBuffer.append(" extends "); //$NON-NLS-1$
					for (int i= 0; i < bounds.length; i++) {
						if (i > 0)
							fBuffer.append(" &amp; "); //$NON-NLS-1$
						appendTypeBindingLabel(bounds[i], (typeRefFlags | JavaElementLabels.T_TYPE_PARAMETERS) & ~TP_BOUNDS);
					}
				}
			}
			// post qualification
			if (getFlag(flags, JavaElementLabels.TP_POST_QUALIFIED)) {
				fBuffer.append(JavaElementLabels.CONCAT_STRING);
				if (typeBinding.getDeclaringClass() != null)
					appendTypeBindingLabel(typeBinding.getDeclaringClass(), getPostQualificationFlags(flags) & ~JavaElementLabels.T_TYPE_PARAMETERS);
				else
					appendMethodBindingLabel(typeBinding.getDeclaringMethod(), getPostQualificationFlags(flags) & ~JavaElementLabels.M_APP_TYPE_PARAMETERS);
			}
			return;
		} else if (typeBinding.isAnnotation()) {
			fBuffer.append(getTypeLink(typeBinding, flags));
		} else if (typeBinding.isWildcardType()) {
			ITypeBinding bound= typeBinding.getBound();
			if (bound == null || bound.getSuperclass() == null) { // no relevant bound / only j.l.Object
				fBuffer.append('?');
			} else {
				if (typeBinding.isUpperbound())
					fBuffer.append("? extends "); //$NON-NLS-1$
				else
					fBuffer.append("? super "); //$NON-NLS-1$
				appendTypeBindingLabel(bound, typeRefFlags);
			}
		} else if (typeBinding.isCapture()) {
			appendTypeBindingLabel(typeBinding.getWildcard(), flags);
		}
		// post qualification
		if (getFlag(flags, JavaElementLabels.T_POST_QUALIFIED)) {
			// search the innermost declaring thing:
			IBinding declaringBinding= typeBinding.getDeclaringMember();
			if (declaringBinding == null) {
				declaringBinding= typeBinding.getDeclaringMethod();
				if (declaringBinding == null)
					declaringBinding= typeBinding.getDeclaringClass();
				if (declaringBinding == null)
					declaringBinding= typeBinding.getPackage();
			}
			if (declaringBinding != null) {
				fBuffer.append(JavaElementLabels.CONCAT_STRING);
				// heuristics: JavaElementsLabelComposer does not include method parameters in post qualification of local type:
				appendBindingLabel(declaringBinding, getPostQualificationFlags(flags) & ~JavaElementLabels.M_PARAMETER_TYPES);
			}
		}
	}

	private void appendTypeArgumentsBindingLabel(ITypeBinding[] parameters, String separator, long flags) {
		if (parameters.length > 0) {
			if (separator != null)
				fBuffer.append(separator);
			fBuffer.append(getLT());
			for (int i = 0; i < parameters.length; i++) {
				if (i > 0)
					fBuffer.append(JavaElementLabels.COMMA_STRING);
				appendTypeBindingLabel(parameters[i], flags);
			}
			fBuffer.append(getGT());
		}
	}

	private void appendAnnotationLabels(IAnnotationBinding[] annotationBindings, long flags, boolean prependBlank, boolean appendBlank) {
		if ((flags & IS_POST_QUALIFICATION) != 0)
			return;
		if (prependBlank)
			fBuffer.append(' ');
		for (int i= 0; i < annotationBindings.length; i++) {
			appendAnnotationLabel(annotationBindings[i], flags);
			if (appendBlank || i < annotationBindings.length-1)
				fBuffer.append(' ');
		}
	}

	private void appendAnnotationLabel(IAnnotationBinding annotation, long flags) {
		fBuffer.append('@');
		appendTypeBindingLabel(annotation.getAnnotationType(), flags);
		IMemberValuePairBinding[] memberValuePairs= annotation.getDeclaredMemberValuePairs();
		if (memberValuePairs.length == 0)
			return;
		if (fIsFromSource) {
			flags &= ~JavaElementLabels.T_FULLY_QUALIFIED;
		}
		fBuffer.append('(');
		for (int i= 0; i < memberValuePairs.length; i++) {
			if (i > 0)
				fBuffer.append(JavaElementLabels.COMMA_STRING);
			IMemberValuePairBinding memberValuePair= memberValuePairs[i];
			fBuffer.append(getMemberName(fEnclosingElement, annotation.getName(), memberValuePair.getName()));
			fBuffer.append('=');
			long valueFlags= flags & ~(JavaElementLabels.F_PRE_TYPE_SIGNATURE|JavaElementLabels.M_PRE_RETURNTYPE|JavaElementLabels.ALL_POST_QUALIFIED);
			appendAnnotationValue(annotation, memberValuePair.getValue(), valueFlags);
		}
		fBuffer.append(')');
	}

	private void appendAnnotationValue(IAnnotationBinding annotation, Object value, long flags) {
		// Note: To be bug-compatible with Javadoc from Java 5/6/7, we currently don't escape HTML tags in String-valued annotations.
		if (value instanceof Object[]) {
			fBuffer.append('{');
			Object[] values= (Object[]) value;
			for (int j= 0; j < values.length; j++) {
				if (j > 0)
					fBuffer.append(JavaElementLabels.COMMA_STRING);
				value= values[j];
				appendAnnotationValue(annotation, value, flags);
			}
			fBuffer.append('}');
		} else {
			if (value instanceof ITypeBinding) {
				appendTypeBindingLabel((ITypeBinding) value, flags);
				fBuffer.append(".class"); //$NON-NLS-1$
			} else if (value instanceof String) {
				fBuffer.append(htmlEscape(ASTNodes.getEscapedStringLiteral((String) value)));
			} else if (value instanceof IVariableBinding) {
				appendVariableLabel((IVariableBinding) value, flags);
			} else if (value instanceof IAnnotationBinding) {
				appendAnnotationLabel((IAnnotationBinding) value, flags);
			} else if (value instanceof Character) {
				fBuffer.append(ASTNodes.getEscapedCharacterLiteral(((Character) value)));
			} else { // other primitive literals
				fBuffer.append(String.valueOf(value));
			}
		}
	}

	private void appendPackageLabel(IPackageBinding binding, long flags) {
		appendAnnotationLabels(binding.getAnnotations(), flags, false, true);
		String qualifiedName= binding.getName();
		if (qualifiedName.length() > 0) {
			IJavaElement packageElement= binding.getJavaElement();
			try {
				if (packageElement != null) {
					String uri= createURI(JAVADOC_SCHEME, packageElement);
					fBuffer.append(createHeaderLink(uri, qualifiedName));
				} else {
					fBuffer.append(qualifiedName);
				}
			} catch (URISyntaxException e) {
				JavaPlugin.log(e);
				fBuffer.append(qualifiedName);
			}
		} else {
			fBuffer.append("(default package)"); //$NON-NLS-1$
		}
	}

	protected void appendModuleLabel(IModuleBinding binding, long flags) {
		appendAnnotationLabels(binding.getAnnotations(), flags, false, true);
		fBuffer.append(binding.getName());
	}

	// consider only relevant bounds / ignore j.l.Object
	private boolean hasRelevantBound(ITypeBinding[] bounds) {
		if (bounds != null) {
			for (ITypeBinding bound : bounds) {
				if (bound.isInterface() || bound.getSuperclass() != null) {
					return true;
				}
			}
		}
		return false;
	}

	private long getPostQualificationFlags(long flags) {
		flags |= JavaElementLabels.ALL_FULLY_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES | IS_POST_QUALIFICATION;
		flags &= ~JavaElementLabels.ALL_POST_QUALIFIED;
		flags &= ~(JavaElementLabels.T_TYPE_PARAMETERS |
					JavaElementLabels.M_PRE_TYPE_PARAMETERS | JavaElementLabels.M_PRE_RETURNTYPE | JavaElementLabels.M_PARAMETER_NAMES);
		return flags;
	}

	// applicable for types, methods & fields
	private void appendNameLink(IBinding binding, IBinding origBinding) {
		try {
			IJavaElement javaElement= origBinding.getJavaElement();
			if (!isEnclosingElement(javaElement)) {
				IJavaElement element= binding.getJavaElement();
				if (element == null) // extra safety, not seen during tests
					element= fEnclosingElement;
				String uri= createURI(JAVADOC_SCHEME, element);
				String title= getLinkTitle(binding);
				if (title != null) {
					title= Messages.format(JavaUIMessages.JavaElementLinks_title, title);
				} else {
					title= ""; //$NON-NLS-1$
				}
				fBuffer.append(createHeaderLink(uri, binding.getName(), title));
				return;
			}
		} catch (URISyntaxException e) {
			JavaPlugin.log(e);
		}
		fBuffer.append(binding.getName());
	}

	private String getLinkTitle(IBinding binding) {
		if (binding instanceof ITypeBinding) {
			IMethodBinding declaringMethod= ((ITypeBinding) binding).getDeclaringMethod();
			String title= null;
			ITypeBinding declaringClass= null;
			if (declaringMethod != null) {
				title= '.'+declaringMethod.getName()+PARAMETER_ELLIPSIS_LABEL;
				declaringClass= declaringMethod.getDeclaringClass();
			} else {
				declaringClass= ((ITypeBinding) binding).getDeclaringClass();
			}
			if (declaringClass != null) {
				String typeName= getTypeName(declaringClass, JavaElementLabels.ALL_FULLY_QUALIFIED);
				return title != null ? typeName+title : typeName;
			}
		} else if (binding instanceof IMethodBinding) {
			return getTypeName(((IMethodBinding) binding).getDeclaringClass(), JavaElementLabels.ALL_FULLY_QUALIFIED);
		} else if (binding instanceof IVariableBinding) {
			return getTypeName(((IVariableBinding) binding).getDeclaringClass(), JavaElementLabels.ALL_FULLY_QUALIFIED);
		}
		return null;
	}

	private String getTypeLink(ITypeBinding typeBinding, long flags) {
		if (isEnclosingElement(typeBinding.getJavaElement())) {
			return getTypeName(typeBinding, flags);
		}
		typeBinding= typeBinding.getTypeDeclaration();
		String typeName = getTypeName(typeBinding, flags);
		String qualifiedName = getTypeName(typeBinding, JavaElementLabels.T_FULLY_QUALIFIED);
		String title= ""; //$NON-NLS-1$
		int qualifierLength= qualifiedName.length() - typeName.length() - 1;
		if (qualifierLength > 0) {
			if (qualifiedName.endsWith(typeName)) {
				title= qualifiedName.substring(0, qualifierLength);
				title= Messages.format(JavaUIMessages.JavaElementLinks_title, title);
			} else {
				title= qualifiedName; // Not expected. Just show the whole qualifiedName.
			}
		}

		try {
			String uri= createURI(JAVADOC_SCHEME, fEnclosingElement, qualifiedName, null, null);
			return createHeaderLink(uri, typeName, title);
		} catch (URISyntaxException e) {
			JavaPlugin.log(e);
			return typeName;
		}
	}

	private String getTypeName(ITypeBinding typeBinding, long flags) {
		if (typeBinding.isLocal()) {
			StringBuilder buf= new StringBuilder();
			IMethodBinding declaringMethod= typeBinding.getDeclaringMethod();
			if (getFlag(flags, JavaElementLabels.T_FULLY_QUALIFIED|JavaElementLabels.T_CONTAINER_QUALIFIED)) {
				if (declaringMethod != null) {
					buf.append(getTypeName(declaringMethod.getDeclaringClass(), flags));
					buf.append('.');
					buf.append(declaringMethod.getName());
					if (declaringMethod.getParameterTypes().length > 0) {
						buf.append(PARAMETER_ELLIPSIS_LABEL);
					} else {
						buf.append("()"); //$NON-NLS-1$
					}
				} else {
					buf.append(getTypeName(typeBinding.getDeclaringClass(), flags));
					IBinding declaringMember= typeBinding.getDeclaringMember();
					if (declaringMember != null) {
						buf.append('.');
						if (declaringMember instanceof ITypeBinding) {
							buf.append(getTypeName((ITypeBinding) declaringMember, flags));
						} else { // field or method
							String name= declaringMember.getName();
							if (INIT_NAME.equals(name))
								buf.append(JavaUIMessages.JavaElementLabels_initializer);
							else
								buf.append(name);
						}
					}
				}
				buf.append('.');
			}
			if (typeBinding.isAnonymous()) {
				buf.append("new "); //$NON-NLS-1$
				ITypeBinding[] interfaces= typeBinding.getInterfaces();
				if (interfaces.length > 0) {
					buf.append(interfaces[0].getName());
				} else if (typeBinding.getSuperclass() != null){
					buf.append(typeBinding.getSuperclass().getName());
				} else {
					buf.append(MISSING_LABEL);
				}
				buf.append(ANON_TYPE_TAIL);
			} else {
				buf.append(typeBinding.getName());
			}
			return buf.toString();
		}
		if (getFlag(flags, JavaElementLabels.T_FULLY_QUALIFIED)) {
			return typeBinding.getQualifiedName();
		} else if (getFlag(flags, JavaElementLabels.T_CONTAINER_QUALIFIED)) {
			ITypeBinding declaringClass= typeBinding.getDeclaringClass();
			if (declaringClass != null) {
				StringBuilder buf= new StringBuilder(getTypeName(declaringClass, flags));
				buf.append('.');
				buf.append(typeBinding.getName());
				return buf.toString();
			}
		}
		return typeBinding.getName();
	}

	private IMethod getIMethod(IMethodBinding method) {
		IJavaElement element= method.getJavaElement();
		if (element == null || element.getElementType() != IJavaElement.METHOD)
			return null; // extra safety, not seen in tests (triggered from ctor of local class inside initializer)
		IMethod iMethod= (IMethod) element;
		if (isEnclosingElement(iMethod)) {
			return (IMethod) fEnclosingElement; // benefit from better details on LambdaMethod
		}
		return iMethod;
	}

	private boolean isEnclosingElement(IJavaElement element) {
		if (element == null)
			return false;
		if (element.equals(fEnclosingElement))
			return true;
		String enclosingKey= null;
		// unify different representations of a lambda: LambdaMethod, LambdaExpression, IMethod (SAM)
		switch (fEnclosingElement.getElementType()) {
			case IJavaElement.TYPE:
				if (((IType)fEnclosingElement).isLambda() && element.getElementType() == IJavaElement.METHOD) {
					// navigate from LambdaExpression to LambdaMethod to be comparable to IMethod:
					try {
						IJavaElement[] children= ((IType) fEnclosingElement).getChildren();
						if (children != null && children.length == 1) {
							if (children[0].getElementType() == IJavaElement.METHOD) {
								enclosingKey= ((IMethod) children[0]).getKey();
							}
						}
					} catch (JavaModelException e) {
						// continue without key
					}
				} else {
					enclosingKey= ((IType) fEnclosingElement).getKey();
				}
				break;
			case IJavaElement.METHOD:
				enclosingKey= ((IMethod) fEnclosingElement).getKey();
				break;
		}
		if (enclosingKey != null) {
			switch (element.getElementType()) {
				case IJavaElement.TYPE:
					return enclosingKey.equals(((IType) element).getKey());
				case IJavaElement.METHOD:
					return enclosingKey.equals(((IMethod) element).getKey());
			}
		}
		return false;
	}

	private String htmlEscape(String escaped) {
		return escaped.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	}
}
