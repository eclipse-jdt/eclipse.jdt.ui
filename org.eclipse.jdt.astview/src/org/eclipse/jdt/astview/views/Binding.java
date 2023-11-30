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
 *******************************************************************************/
package org.eclipse.jdt.astview.views;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Function;

import org.eclipse.jdt.astview.ASTViewPlugin;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IModuleBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.StringLiteral;

public class Binding extends ASTAttribute {

	private final IBinding fBinding;
	private final String fLabel;
	private final Object fParent;
	private final boolean fIsRelevant;

	public Binding(Object parent, String label, IBinding binding, boolean isRelevant) {
		fParent= parent;
		fBinding= binding;
		fLabel= label;
		fIsRelevant= isRelevant;
	}

	@Override
	public Object getParent() {
		return fParent;
	}

	public IBinding getBinding() {
		return fBinding;
	}


	public boolean hasBindingProperties() {
		return fBinding != null;
	}

	public boolean isRelevant() {
		return fIsRelevant;
	}


	private static boolean isType(int typeKinds, int kind) {
		return (typeKinds & kind) != 0;
	}

	@Override
	public Object[] getChildren() {
		try {
			if (fBinding != null) {
				fBinding.getKey();
			}
		} catch (RuntimeException e) {
			ASTViewPlugin.log("Exception thrown in IBinding#getKey() for \"" + fBinding + "\"", e);
			return new Object[] { new Error(this, "BrokenBinding: " + fBinding, null) };
		}
		if (fBinding != null) {
			ArrayList<ASTAttribute> res= new ArrayList<>();
			res.add(new BindingProperty(this, "NAME", fBinding.getName(), true)); //$NON-NLS-1$
			res.add(new BindingProperty(this, "KEY", fBinding.getKey(), true)); //$NON-NLS-1$
			res.add(new BindingProperty(this, "IS RECOVERED", fBinding.isRecovered(), true)); //$NON-NLS-1$
			switch (fBinding.getKind()) {
				case IBinding.VARIABLE:
					IVariableBinding variableBinding= (IVariableBinding) fBinding;
					res.add(new BindingProperty(this, "IS FIELD", variableBinding.isField(), true)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS ENUM CONSTANT", variableBinding.isEnumConstant(), true)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS PARAMETER", variableBinding.isParameter(), true)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS RECORD COMPONENT", variableBinding.isRecordComponent(), true)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "VARIABLE ID", variableBinding.getVariableId(), true)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "MODIFIERS", getModifiersString(fBinding.getModifiers(), false), true)); //$NON-NLS-1$
					res.add(new Binding(this, "TYPE", variableBinding.getType(), true)); //$NON-NLS-1$
					res.add(new Binding(this, "DECLARING CLASS", variableBinding.getDeclaringClass(), true)); //$NON-NLS-1$
					res.add(new Binding(this, "DECLARING METHOD", variableBinding.getDeclaringMethod(), true)); //$NON-NLS-1$
					res.add(new Binding(this, "VARIABLE DECLARATION", variableBinding.getVariableDeclaration(), true)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS SYNTHETIC", fBinding.isSynthetic(), true)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS DEPRECATED", fBinding.isDeprecated(), true)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "CONSTANT VALUE", variableBinding.getConstantValue(), true)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS EFFECTIVELY FINAL", variableBinding.isEffectivelyFinal(), true)); //$NON-NLS-1$
					break;

				case IBinding.PACKAGE:
					IPackageBinding packageBinding= (IPackageBinding) fBinding;
					res.add(new BindingProperty(this, "IS UNNAMED", packageBinding.isUnnamed(), true)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS SYNTHETIC", fBinding.isSynthetic(), true)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS DEPRECATED", fBinding.isDeprecated(), true)); //$NON-NLS-1$
					break;

				case IBinding.TYPE:
					ITypeBinding typeBinding= (ITypeBinding) fBinding;
					res.add(new BindingProperty(this, "QUALIFIED NAME", typeBinding.getQualifiedName(), true)); //$NON-NLS-1$

					int typeKind= getTypeKind(typeBinding);
					boolean isRefType= isType(typeKind, REF_TYPE);
					final boolean isNonPrimitive= ! isType(typeKind, PRIMITIVE_TYPE);

					StringBuilder kinds= new StringBuilder("KIND:"); //$NON-NLS-1$
					if (typeBinding.isArray()) kinds.append(" isArray"); //$NON-NLS-1$
					if (typeBinding.isCapture()) kinds.append(" isCapture"); //$NON-NLS-1$
					if (typeBinding.isNullType()) kinds.append(" isNullType"); //$NON-NLS-1$
					if (typeBinding.isPrimitive()) kinds.append(" isPrimitive"); //$NON-NLS-1$
					if (typeBinding.isTypeVariable()) kinds.append(" isTypeVariable"); //$NON-NLS-1$
					if (typeBinding.isWildcardType()) kinds.append(" isWildcardType"); //$NON-NLS-1$
					// ref types
					if (typeBinding.isAnnotation()) kinds.append(" isAnnotation"); //$NON-NLS-1$
					if (typeBinding.isClass()) kinds.append(" isClass"); //$NON-NLS-1$
					if (typeBinding.isInterface()) kinds.append(" isInterface"); //$NON-NLS-1$
					if (typeBinding.isEnum()) kinds.append(" isEnum"); //$NON-NLS-1$
					res.add(new BindingProperty(this, kinds, true));

					StringBuilder generics= new StringBuilder("GENERICS:"); //$NON-NLS-1$
					if (typeBinding.isRawType()) generics.append(" isRawType"); //$NON-NLS-1$
					if (typeBinding.isGenericType()) generics.append(" isGenericType"); //$NON-NLS-1$
					if (typeBinding.isParameterizedType()) generics.append(" isParameterizedType"); //$NON-NLS-1$
					if (!isType(typeKind, GENERIC | PARAMETRIZED)) {
						generics.append(" (non-generic, non-parameterized)");
					}
					res.add(new BindingProperty(this, generics, isRefType));

					res.add(new Binding(this, "ELEMENT TYPE", typeBinding.getElementType(), isType(typeKind, ARRAY_TYPE))); //$NON-NLS-1$
					res.add(new Binding(this, "COMPONENT TYPE", typeBinding.getComponentType(), isType(typeKind, ARRAY_TYPE))); //$NON-NLS-1$
					res.add(new BindingProperty(this, "DIMENSIONS", typeBinding.getDimensions(), isType(typeKind, ARRAY_TYPE))); //$NON-NLS-1$
					final String createArrayTypeLabel= "CREATE ARRAY TYPE (+1)";
					try {
						ITypeBinding arrayType= typeBinding.createArrayType(1);
						res.add(new Binding(this, createArrayTypeLabel, arrayType, true));
					} catch (RuntimeException e) {
						String msg= e.getClass().getName() + ": " + e.getLocalizedMessage();
						boolean isRelevant= ! typeBinding.getName().equals(PrimitiveType.VOID.toString()) && ! typeBinding.isRecovered();
						if (isRelevant) {
							res.add(new Error(this, createArrayTypeLabel + ": " + msg, e));
						} else {
							res.add(new BindingProperty(this, createArrayTypeLabel, msg, false));
						}
					}

					StringBuilder origin= new StringBuilder("ORIGIN:"); //$NON-NLS-1$
					if (typeBinding.isTopLevel()) origin.append(" isTopLevel"); //$NON-NLS-1$
					if (typeBinding.isNested()) origin.append(" isNested"); //$NON-NLS-1$
					if (typeBinding.isLocal()) origin.append(" isLocal"); //$NON-NLS-1$
					if (typeBinding.isMember()) origin.append(" isMember"); //$NON-NLS-1$
					if (typeBinding.isAnonymous()) origin.append(" isAnonymous"); //$NON-NLS-1$
					res.add(new BindingProperty(this, origin, isRefType));

					res.add(new BindingProperty(this, "IS FROM SOURCE", typeBinding.isFromSource(), isType(typeKind, REF_TYPE | VARIABLE_TYPE | CAPTURE_TYPE))); //$NON-NLS-1$

					res.add(new Binding(this, "PACKAGE", typeBinding.getPackage(), isRefType)); //$NON-NLS-1$
					res.add(new Binding(this, "DECLARING CLASS", typeBinding.getDeclaringClass(), isType(typeKind, REF_TYPE | VARIABLE_TYPE | CAPTURE_TYPE))); //$NON-NLS-1$
					res.add(new Binding(this, "DECLARING METHOD", typeBinding.getDeclaringMethod(), isType(typeKind, REF_TYPE | VARIABLE_TYPE | CAPTURE_TYPE))); //$NON-NLS-1$
					res.add(new Binding(this, "DECLARING MEMBER", typeBinding.getDeclaringMember(), typeBinding.isLocal())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "MODIFIERS", getModifiersString(fBinding.getModifiers(), false), isRefType)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "BINARY NAME", typeBinding.getBinaryName(), true)); //$NON-NLS-1$

					String isTypeDeclaration= typeBinding == typeBinding.getTypeDeclaration() ? " ( == this)" : " ( != this)";
					res.add(new Binding(this, "TYPE DECLARATION" + isTypeDeclaration, typeBinding.getTypeDeclaration(), true)); //$NON-NLS-1$
					String isErasure= typeBinding == typeBinding.getErasure() ? " ( == this)" : " ( != this)";
					res.add(new Binding(this, "ERASURE" + isErasure, typeBinding.getErasure(), isNonPrimitive)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "TYPE PARAMETERS", typeBinding.getTypeParameters(), isType(typeKind, GENERIC))); //$NON-NLS-1$
					res.add(new BindingProperty(this, "TYPE ARGUMENTS", typeBinding.getTypeArguments(), isType(typeKind, PARAMETRIZED))); //$NON-NLS-1$
					res.add(new BindingProperty(this, "TYPE BOUNDS", typeBinding.getTypeBounds(), isType(typeKind, VARIABLE_TYPE | WILDCARD_TYPE | CAPTURE_TYPE))); //$NON-NLS-1$
					res.add(new Binding(this, "BOUND", typeBinding.getBound(), isType(typeKind, WILDCARD_TYPE))); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS UPPERBOUND", typeBinding.isUpperbound(), isType(typeKind, WILDCARD_TYPE))); //$NON-NLS-1$
					res.add(new Binding(this, "GENERIC TYPE OF WILDCARD TYPE", typeBinding.getGenericTypeOfWildcardType(), isType(typeKind, WILDCARD_TYPE))); //$NON-NLS-1$
					res.add(new BindingProperty(this, "RANK", typeBinding.getRank(), isType(typeKind, WILDCARD_TYPE))); //$NON-NLS-1$
					res.add(new Binding(this, "WILDCARD", typeBinding.getWildcard(), isType(typeKind, CAPTURE_TYPE))); //$NON-NLS-1$

					res.add(new Binding(this, "SUPERCLASS", typeBinding.getSuperclass(), isRefType)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "INTERFACES", typeBinding.getInterfaces(), isRefType)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "DECLARED TYPES", typeBinding.getDeclaredTypes(), isRefType)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "DECLARED FIELDS", typeBinding.getDeclaredFields(), isRefType)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "DECLARED METHODS", typeBinding.getDeclaredMethods(), isRefType)); //$NON-NLS-1$
					res.add(new Binding(this, "FUNCTIONAL INTERFACE METHOD", typeBinding.getFunctionalInterfaceMethod(), typeBinding.isInterface())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS SYNTHETIC", fBinding.isSynthetic(), isNonPrimitive)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS DEPRECATED", fBinding.isDeprecated(), isRefType)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "TYPE ANNOTATIONS", typeBinding.getTypeAnnotations(), true)); //$NON-NLS-1$
					break;

				case IBinding.METHOD:
					IMethodBinding methodBinding= (IMethodBinding) fBinding;
					res.add(new BindingProperty(this, "IS CONSTRUCTOR", methodBinding.isConstructor(), true)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS DEFAULT CONSTRUCTOR", methodBinding.isDefaultConstructor(), true)); //$NON-NLS-1$
					res.add(new Binding(this, "DECLARING CLASS", methodBinding.getDeclaringClass(), true)); //$NON-NLS-1$
					res.add(new Binding(this, "DECLARING MEMBER", methodBinding.getDeclaringMember(), methodBinding.getDeclaringMember() != null)); //$NON-NLS-1$
					res.add(new Binding(this, "RETURN TYPE", methodBinding.getReturnType(), true)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "MODIFIERS", getModifiersString(fBinding.getModifiers(), true), true)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "PARAMETER TYPES", methodBinding.getParameterTypes(), true)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS VARARGS", methodBinding.isVarargs(), true)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "EXCEPTION TYPES", methodBinding.getExceptionTypes(), true)); //$NON-NLS-1$

					StringBuilder genericsM= new StringBuilder("GENERICS:"); //$NON-NLS-1$
					if (methodBinding.isRawMethod()) genericsM.append(" isRawMethod"); //$NON-NLS-1$
					if (methodBinding.isGenericMethod()) genericsM.append(" isGenericMethod"); //$NON-NLS-1$
					if (methodBinding.isParameterizedMethod()) genericsM.append(" isParameterizedMethod"); //$NON-NLS-1$
					res.add(new BindingProperty(this, genericsM, true));

					String isMethodDeclaration= methodBinding == methodBinding.getMethodDeclaration() ? " ( == this)" : " ( != this)";
					res.add(new Binding(this, "METHOD DECLARATION" + isMethodDeclaration, methodBinding.getMethodDeclaration(), true)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "TYPE PARAMETERS", methodBinding.getTypeParameters(), true)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "TYPE ARGUMENTS", methodBinding.getTypeArguments(), true)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS SYNTHETIC", fBinding.isSynthetic(), true)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS DEPRECATED", fBinding.isDeprecated(), true)); //$NON-NLS-1$

					res.add(new BindingProperty(this, "IS ANNOTATION MEMBER", methodBinding.isAnnotationMember(), true)); //$NON-NLS-1$
					res.add(Binding.createValueAttribute(this, "DEFAULT VALUE", methodBinding.getDefaultValue()));

					int parameterCount= methodBinding.getParameterTypes().length;
					BindingProperty[] parametersAnnotations= new BindingProperty[parameterCount];
					for (int i= 0; i < parameterCount; i++) {
						parametersAnnotations[i]= new BindingProperty(this, "Parameter " + String.valueOf(i), methodBinding.getParameterAnnotations(i), true);
					}
					res.add(new BindingProperty(this, "PARAMETER ANNOTATIONS", parametersAnnotations, true));
					break;

				case IBinding.ANNOTATION:
					IAnnotationBinding annotationBinding= (IAnnotationBinding) fBinding;
					res.add(new Binding(this, "ANNOTATION TYPE", annotationBinding.getAnnotationType(), true));
					res.add(new BindingProperty(this, "DECLARED MEMBER VALUE PAIRS", annotationBinding.getDeclaredMemberValuePairs(), true));
					res.add(new BindingProperty(this, "ALL MEMBER VALUE PAIRS", annotationBinding.getAllMemberValuePairs(), true));
					break;

				case IBinding.MEMBER_VALUE_PAIR:
					IMemberValuePairBinding memberValuePairBinding= (IMemberValuePairBinding) fBinding;
					res.add(new Binding(this, "METHOD BINDING", memberValuePairBinding.getMethodBinding(), true));
					res.add(new BindingProperty(this, "IS DEFAULT", memberValuePairBinding.isDefault(), true));
					res.add(Binding.createValueAttribute(this, "VALUE", memberValuePairBinding.getValue()));
					break;

				case IBinding.MODULE:
					IModuleBinding moduleBinding= (IModuleBinding) fBinding;
					res.add(new BindingProperty(this, "REQUIRED MODULES", moduleBinding.getRequiredModules(), true));
					res.add(createPropertiesWithSecondary(moduleBinding.getExportedPackages(), "EXPORTED PACKAGES", "PACKAGE", "TO",
							moduleBinding::getExportedTo));
					res.add(createPropertiesWithSecondary(moduleBinding.getOpenedPackages(), "OPENED PACKAGES", "PACKAGE", "TO",
							moduleBinding::getOpenedTo));
					res.add(new BindingProperty(this, "USES", moduleBinding.getUses(), true));
					res.add(createPropertiesWithSecondary(moduleBinding.getServices(), "SERVICES", "PROVIDES", "WITH",
							moduleBinding::getImplementations));
					break;

				default:
					break;
			}
			try {
				IAnnotationBinding[] annotations= fBinding.getAnnotations();
				res.add(new BindingProperty(this, "ANNOTATIONS", annotations, true)); //$NON-NLS-1$
			} catch (RuntimeException e) {
				String label= "Error in IBinding#getAnnotations() for \"" + fBinding.getKey() + "\"";
				res.add(new Error(this, label, e));
				ASTViewPlugin.log("Exception thrown in IBinding#getAnnotations() for \"" + fBinding.getKey() + "\"", e);
			}
			try {
				IJavaElement javaElement= fBinding.getJavaElement();
				res.add(new JavaElement(this, javaElement));
			} catch (RuntimeException e) {
				String label= ">java element: " + e.getClass().getName() + " for \"" + fBinding.getKey() + "\"";  //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				res.add(new Error(this, label, e));
				ASTViewPlugin.log("Exception thrown in IBinding#getJavaElement() for \"" + fBinding.getKey() + "\"", e);
			}
			return res.toArray();
		}
		return EMPTY;
	}

	private <T extends IBinding> BindingProperty createPropertiesWithSecondary(T[] bindings, String propertiesName, String propertyName, String secondaryName,
			Function<T, Object[]> secondaryAccessor) {
		int count= bindings.length;
		ASTAttribute[] attributes= new ASTAttribute[count * 2];
		BindingProperty property= new BindingProperty(this, propertiesName, attributes, true) {
			@Override
			public String getLabel() {
				return propertiesName + " (" + count + ")";
			}
		};
		for (int i= 0; i < count; i++) {
			attributes[i * 2]= new Binding(this, propertyName, bindings[i], true);
			Object[] secondaryPropertyValues= secondaryAccessor.apply(bindings[i]);
			if (secondaryPropertyValues instanceof IBinding[]) {
				attributes[i * 2 + 1]= new BindingProperty(this, secondaryName, (IBinding[]) secondaryPropertyValues, true);
			} else {
				GeneralAttribute[] secondaryProperties= new GeneralAttribute[secondaryPropertyValues.length];
				for (int j= 0; j < secondaryProperties.length; j++) {
					secondaryProperties[j]= new GeneralAttribute(property, String.valueOf(j), String.valueOf(secondaryPropertyValues[j]));
				}
				attributes[i * 2 + 1]= new BindingProperty(this, secondaryName, secondaryProperties, true);
			}
		}
		return property;
	}

	private final static int ARRAY_TYPE= 1 << 0;
	private final static int NULL_TYPE= 1 << 1;
	private final static int VARIABLE_TYPE= 1 << 2;
	private final static int WILDCARD_TYPE= 1 << 3;
	private final static int CAPTURE_TYPE= 1 << 4;
	private final static int PRIMITIVE_TYPE= 1 << 5;

	private final static int REF_TYPE= 1 << 6;

	private final static int GENERIC= 1 << 8;
	private final static int PARAMETRIZED= 1 << 9;

	private int getTypeKind(ITypeBinding typeBinding) {
		if (typeBinding.isArray()) return ARRAY_TYPE;
		if (typeBinding.isCapture()) return CAPTURE_TYPE;
		if (typeBinding.isNullType()) return NULL_TYPE;
		if (typeBinding.isPrimitive()) return PRIMITIVE_TYPE;
		if (typeBinding.isTypeVariable()) return VARIABLE_TYPE;
		if (typeBinding.isWildcardType()) return WILDCARD_TYPE;

		if (typeBinding.isGenericType())  return REF_TYPE | GENERIC;
		if (typeBinding.isParameterizedType() || typeBinding.isRawType()) return REF_TYPE | PARAMETRIZED;

		return REF_TYPE;
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public String getLabel() {
		StringBuffer buf= new StringBuffer(fLabel);
		buf.append(": "); //$NON-NLS-1$
		if (fBinding != null) {
			switch (fBinding.getKind()) {
				case IBinding.VARIABLE:
					IVariableBinding variableBinding= (IVariableBinding) fBinding;
					if (!variableBinding.isField()) {
						buf.append(variableBinding.getName());
					} else {
						if (variableBinding.getDeclaringClass() == null) {
							buf.append("<some array type>"); //$NON-NLS-1$
						} else {
							buf.append(variableBinding.getDeclaringClass().getName());
						}
						buf.append('.');
						buf.append(variableBinding.getName());
					}
					break;
				case IBinding.PACKAGE:
					IPackageBinding packageBinding= (IPackageBinding) fBinding;
					buf.append(packageBinding.getName());
					break;
				case IBinding.TYPE:
					ITypeBinding typeBinding= (ITypeBinding) fBinding;
					appendAnnotatedQualifiedName(buf, typeBinding);
					break;
				case IBinding.METHOD:
					IMethodBinding methodBinding= (IMethodBinding) fBinding;
					buf.append(methodBinding.getDeclaringClass().getName());
					buf.append('.');
					buf.append(methodBinding.getName());
					buf.append('(');
					ITypeBinding[] parameters= methodBinding.getParameterTypes();
					for (int i= 0; i < parameters.length; i++) {
						if (i > 0) {
							buf.append(", "); //$NON-NLS-1$
						}
						ITypeBinding parameter= parameters[i];
						buf.append(parameter.getName());
					}
					buf.append(')');
					break;
				case IBinding.ANNOTATION:
				case IBinding.MEMBER_VALUE_PAIR:
					buf.append(fBinding.toString());
					break;
				case IBinding.MODULE:
					buf.append(fBinding.getName());
			}

		} else {
			buf.append("null"); //$NON-NLS-1$
		}
		return buf.toString();

	}

	public static void appendAnnotatedQualifiedName(StringBuffer buf, ITypeBinding typeBinding) {
		String debugString= typeBinding.toString(); // XXX: hack, but that's OK for a debugging tool...
		if (debugString.indexOf('\n') == -1 || typeBinding.getTypeAnnotations().length != 0) {
			// one-liner || outermost type has type annotations
			buf.append(debugString);
		} else {
			buf.append(typeBinding.getQualifiedName());
		}
	}

	@Override
	public Image getImage() {
		return null;
	}

	@Override
	public String toString() {
		return getLabel();
	}

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || !obj.getClass().equals(getClass())) {
			return false;
		}

		Binding other= (Binding) obj;
		if (!Objects.equals(fParent, other.fParent)) {
			return false;
		}

		if (!Objects.equals(fBinding, other.fBinding)) {
			return false;
		}

		if (!Objects.equals(fLabel, other.fLabel)) {
			return false;
		}

		return true;
	}

	/*
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int result= fParent != null ? fParent.hashCode() : 0;
		result+= (fBinding != null && fBinding.getKey() != null) ? fBinding.getKey().hashCode() : 0;
		result+= fLabel != null ? fLabel.hashCode() : 0;
		return result;
	}

	public static String getBindingLabel(IBinding binding) {
		String label;
		if (binding == null) {
			label= ">binding"; //$NON-NLS-1$
		} else {
			switch (binding.getKind()) {
				case IBinding.VARIABLE:
					label= "> variable binding"; //$NON-NLS-1$
					break;
				case IBinding.TYPE:
					label= "> type binding"; //$NON-NLS-1$
					break;
				case IBinding.METHOD:
					label= "> method binding"; //$NON-NLS-1$
					break;
				case IBinding.PACKAGE:
					label= "> package binding"; //$NON-NLS-1$
					break;
				case IBinding.ANNOTATION:
					label= "> annotation binding"; //$NON-NLS-1$
					break;
				case IBinding.MEMBER_VALUE_PAIR:
					label= "> member value pair binding"; //$NON-NLS-1$
					break;
				case IBinding.MODULE:
					label= "> module binding"; //$NON-NLS-1$
					break;
				default:
					label= "> unknown binding"; //$NON-NLS-1$
			}
		}
		return label;
	}

	/**
	 * Creates an {@link ASTAttribute} for a value from
	 * {@link IMemberValuePairBinding#getValue()} or from
	 * {@link IMethodBinding#getDefaultValue()}.
	 *
	 * @param parent the parent node
	 * @param name the attribute name
	 * @param value the attribute value
	 * @return an ASTAttribute
	 */
	public static ASTAttribute createValueAttribute(ASTAttribute parent, String name, Object value) {
		ASTAttribute res;
		if (value instanceof IBinding) {
			IBinding binding= (IBinding) value;
			res= new Binding(parent, name + ": " + getBindingLabel(binding), binding, true);

		} else if (value instanceof String) {
			res= new GeneralAttribute(parent, name, getEscapedStringLiteral((String) value));

		} else if (value instanceof Object[]) {
			res= new GeneralAttribute(parent, name, (Object[]) value);

		} else if (value instanceof ASTAttribute) {
			res= (ASTAttribute) value;

		} else {
			res= new GeneralAttribute(parent, name, value);
		}
		return res;
	}

	public static String getEscapedStringLiteral(String stringValue) {
		StringLiteral stringLiteral= AST.newAST(ASTView.JLS_LATEST, false).newStringLiteral();
		stringLiteral.setLiteralValue(stringValue);
		return stringLiteral.getEscapedValue();
	}

	public static String getEscapedCharLiteral(char charValue) {
		CharacterLiteral charLiteral= AST.newAST(ASTView.JLS_LATEST, false).newCharacterLiteral();
		charLiteral.setCharValue(charValue);
		return charLiteral.getEscapedValue();
	}

	private static StringBuffer getModifiersString(int flags, boolean isMethod) {
		StringBuffer sb = new StringBuffer().append("0x").append(Integer.toHexString(flags)).append(" (");
		int prologLen= sb.length();
		int rest= flags;

		rest&= ~ appendFlag(sb, flags, Modifier.PUBLIC, "public ");
		rest&= ~ appendFlag(sb, flags, Modifier.PRIVATE, "private ");
		rest&= ~ appendFlag(sb, flags, Modifier.PROTECTED, "protected ");
		rest&= ~ appendFlag(sb, flags, Modifier.STATIC, "static ");
		rest&= ~ appendFlag(sb, flags, Modifier.FINAL, "final ");
		if (isMethod) {
			rest&= ~ appendFlag(sb, flags, Modifier.SYNCHRONIZED, "synchronized ");
			rest&= ~ appendFlag(sb, flags, Modifier.DEFAULT, "default ");
		} else {
			rest&= ~ appendFlag(sb, flags, Modifier.VOLATILE, "volatile ");
			rest&= ~ appendFlag(sb, flags, Modifier.TRANSIENT, "transient ");
		}
		rest&= ~ appendFlag(sb, flags, Modifier.NATIVE, "native ");
		rest&= ~ appendFlag(sb, flags, Modifier.ABSTRACT, "abstract ");
		rest&= ~ appendFlag(sb, flags, Modifier.STRICTFP, "strictfp ");

		if (rest != 0)
			sb.append("unknown:0x").append(Integer.toHexString(rest)).append(" ");
		int len = sb.length();
		if (len != prologLen)
			sb.setLength(len - 1);
		sb.append(")");
		return sb;
	}

	private static int appendFlag(StringBuffer sb, int flags, int flag, String name) {
		if ((flags & flag) != 0) {
			sb.append(name);
			return flag;
		} else {
			return 0;
		}
	}
}
