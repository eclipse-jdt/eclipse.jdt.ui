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
package org.eclipse.jdt.astview.views;

import java.util.ArrayList;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.Flags;

import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

/**
 *
 */
public class Binding extends ASTAttribute {
	
	private IBinding fBinding;
	private String fLabel;
	private Object fParent;
	
	public Binding(Object parent, IBinding binding) {
		fParent= parent;
		fBinding= binding;
		if (binding == null) {
			fLabel= ">binding"; //$NON-NLS-1$
		} else {
			switch (binding.getKind()) {
				case IBinding.VARIABLE:
					fLabel= ">variable binding"; //$NON-NLS-1$
					break;
				case IBinding.TYPE:
					fLabel= ">type binding"; //$NON-NLS-1$
					break;
				case IBinding.METHOD:
					fLabel= ">method binding"; //$NON-NLS-1$
					break;
				case IBinding.PACKAGE:
					fLabel= ">package binding"; //$NON-NLS-1$
					break;
				default:
					fLabel= ">unknown binding"; //$NON-NLS-1$
			}
		}
	}
	
	public Binding(Object parent, String label, IBinding binding) {
		fParent= parent;
		fBinding= binding;
		fLabel= label;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.astview.views.ASTAttribute#getParent()
	 */
	public Object getParent() {
		return fParent;
	}
	
	public IBinding getBinding() {
		return fBinding;
	}
	

	public boolean hasBindingProperties() {
		return fBinding != null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.astview.views.ASTAttribute#getChildren()
	 */
	public Object[] getChildren() {
		
		if (fBinding != null) {
			ArrayList res= new ArrayList();
			res.add(new BindingProperty(this, "NAME", fBinding.getName())); //$NON-NLS-1$
			res.add(new BindingProperty(this, "MODIFIERS", Flags.toString(fBinding.getModifiers()))); //$NON-NLS-1$
			res.add(new BindingProperty(this, "IS SYNTHETIC", fBinding.isSynthetic())); //$NON-NLS-1$
			res.add(new BindingProperty(this, "IS DEPRECATED", fBinding.isDeprecated())); //$NON-NLS-1$
			res.add(new BindingProperty(this, "KEY", fBinding.getKey())); //$NON-NLS-1$
			switch (fBinding.getKind()) {
				case IBinding.VARIABLE:
					IVariableBinding variableBinding= (IVariableBinding) fBinding;
					res.add(new BindingProperty(this, "IS FIELD", variableBinding.isField())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "VARIABLE ID", variableBinding.getVariableId())); //$NON-NLS-1$
					res.add(new Binding(this, "TYPE", variableBinding.getType())); //$NON-NLS-1$
					res.add(new Binding(this, "DECLARING CLASS", variableBinding.getDeclaringClass())); //$NON-NLS-1$
					Object constVal= variableBinding.getConstantValue();
					res.add(new BindingProperty(this, "CONSTANT VALUE", constVal == null ? "null" : constVal.toString())); //$NON-NLS-1$ //$NON-NLS-2$
					break;
				case IBinding.PACKAGE:
					IPackageBinding packageBinding= (IPackageBinding) fBinding;
					res.add(new BindingProperty(this, "IS UNNAMED", packageBinding.isUnnamed())); //$NON-NLS-1$
					break;
				case IBinding.TYPE:
					ITypeBinding typeBinding= (ITypeBinding) fBinding;
					res.add(new Binding(this, "PACKAGE", typeBinding.getPackage())); //$NON-NLS-1$
					res.add(new Binding(this, "DECLARING CLASS", typeBinding.getDeclaringClass())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "BINARY NAME", typeBinding.getBinaryName())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS PRIMITIVE", typeBinding.isPrimitive())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS NULL TYPE", typeBinding.isNullType())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS ARRAY", typeBinding.isArray())); //$NON-NLS-1$
					res.add(new Binding(this, "ELEMENT TYPE", typeBinding.getElementType())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "DIMENSIONS", typeBinding.getDimensions())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS CLASS", typeBinding.isClass())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS INTERFACE", typeBinding.isInterface())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS ENUM", typeBinding.isEnum())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS ANNOTATION", typeBinding.isAnnotation())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "TYPE PARAMETERS", typeBinding.getTypeParameters())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS TYPEVARIABLE", typeBinding.isTypeVariable())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "TYPE BOUNDS", typeBinding.getTypeBounds())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS PARAMETRIZED TYPE", typeBinding.isParameterizedType())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "TYPE ARGUMENTS", typeBinding.getTypeArguments())); //$NON-NLS-1$			
					res.add(new Binding(this, "ERASURE", typeBinding.getErasure())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS RAW TYPE", typeBinding.isRawType())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS WILCARD TYPE", typeBinding.isWildcardType())); //$NON-NLS-1$
					res.add(new Binding(this, "BOUND", typeBinding.getErasure())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS UPPERBOUND", typeBinding.isUpperbound())); //$NON-NLS-1$

					res.add(new Binding(this, "SUPERCLASS", typeBinding.getSuperclass())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "INTERFACES", typeBinding.getInterfaces())); //$NON-NLS-1$			
					res.add(new BindingProperty(this, "DECLARED MODIFIERS", Flags.toString(fBinding.getModifiers()))); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS TOP LEVEL", typeBinding.isTopLevel())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS NESTED", typeBinding.isNested())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS MEMBER", typeBinding.isMember())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS LOCAL", typeBinding.isLocal())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS ANONYMOUS", typeBinding.isAnonymous())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "DECLARED TYPES", typeBinding.getDeclaredTypes())); //$NON-NLS-1$			
					res.add(new BindingProperty(this, "DECLARED FIELDS", typeBinding.getDeclaredFields())); //$NON-NLS-1$			
					res.add(new BindingProperty(this, "DECLARED METHODS", typeBinding.getDeclaredMethods())); //$NON-NLS-1$			
					res.add(new BindingProperty(this, "IS FROM SOURCE", typeBinding.isFromSource())); //$NON-NLS-1$
					break;
				case IBinding.METHOD:
					IMethodBinding methodBinding= (IMethodBinding) fBinding;
					res.add(new BindingProperty(this, "IS CONSTRUCTOR", methodBinding.isConstructor())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS DEFAULT CONSTRUCTOR", methodBinding.isDefaultConstructor())); //$NON-NLS-1$
					res.add(new Binding(this, "DECLARING CLASS", methodBinding.getDeclaringClass())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "PARAMETER TYPES", methodBinding.getParameterTypes())); //$NON-NLS-1$
					res.add(new Binding(this, "RETURN TYPE", methodBinding.getReturnType())); //$NON-NLS-1$

					res.add(new BindingProperty(this, "EXCEPTION TYPES", methodBinding.getExceptionTypes())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "TYPE PARAMETERS", methodBinding.getTypeParameters())); //$NON-NLS-1$
					break;
			}
			return res.toArray();
		}
		return EMPTY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.astview.views.ASTAttribute#getLabel()
	 */
	public String getLabel() {
		StringBuffer buf= new StringBuffer(fLabel);
		buf.append(": "); //$NON-NLS-1$
		if (fBinding != null) {
			switch (fBinding.getKind()) {
				case IBinding.VARIABLE:
					IVariableBinding variableBinding= (IVariableBinding) fBinding;
					if (!variableBinding.isField()) {
						buf.append(variableBinding.getName());
					} else if (variableBinding.getDeclaringClass() == null) {
						buf.append("array type"); //$NON-NLS-1$
					} else {
						buf.append(variableBinding.getDeclaringClass().getName());
						buf.append(':');
						buf.append(variableBinding.getName());				
					}
					break;
				case IBinding.PACKAGE:
					IPackageBinding packageBinding= (IPackageBinding) fBinding;
					buf.append(packageBinding.getName());
					break;
				case IBinding.TYPE:
					ITypeBinding typeBinding= (ITypeBinding) fBinding;
					buf.append(typeBinding.getQualifiedName());
					break;
				case IBinding.METHOD:
					IMethodBinding methodBinding= (IMethodBinding) fBinding;
					buf.append(methodBinding.getDeclaringClass().getName());
					buf.append(':');
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
			}
		} else {
			buf.append("null"); //$NON-NLS-1$
		}
		return buf.toString();

	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.astview.views.ASTAttribute#getImage()
	 */
	public Image getImage() {
		return null;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getLabel();
	}
	
	
}
