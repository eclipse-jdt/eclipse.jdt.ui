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

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import org.eclipse.swt.graphics.Image;

/**
 *
 */
public class Binding extends ASTAttribute {
	
	private IBinding fBinding;
	private String fLabel;
	private Object fParent;
	private boolean fIsRequired;
	
	public Binding(Object parent, String label, IBinding binding, boolean isRequired) {
		fParent= parent;
		fBinding= binding;
		fLabel= label;
		fIsRequired= isRequired;
	}
	
	/**
	 * @return Returns the isRequired.
	 */
	public boolean isRequired() {
		return fIsRequired;
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
			res.add(new BindingProperty(this, "KEY", fBinding.getKey())); //$NON-NLS-1$
			switch (fBinding.getKind()) {
				case IBinding.VARIABLE:
					IVariableBinding variableBinding= (IVariableBinding) fBinding;
					res.add(new BindingProperty(this, "IS FIELD", variableBinding.isField())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "VARIABLE ID", variableBinding.getVariableId())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "MODIFIERS", Flags.toString(fBinding.getModifiers()))); //$NON-NLS-1$
					res.add(new Binding(this, "TYPE", variableBinding.getType(), false)); //$NON-NLS-1$
					res.add(new Binding(this, "DECLARING CLASS", variableBinding.getDeclaringClass(), false)); //$NON-NLS-1$
					res.add(new Binding(this, "DECLARING METHOD", variableBinding.getDeclaringMethod(), false)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS SYNTHETIC", fBinding.isSynthetic())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS DEPRECATED", fBinding.isDeprecated())); //$NON-NLS-1$
					Object constVal= variableBinding.getConstantValue();
					res.add(new BindingProperty(this, "CONSTANT VALUE", constVal == null ? "null" : constVal.toString())); //$NON-NLS-1$ //$NON-NLS-2$
					break;
				case IBinding.PACKAGE:
					IPackageBinding packageBinding= (IPackageBinding) fBinding;
					res.add(new BindingProperty(this, "IS UNNAMED", packageBinding.isUnnamed())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS SYNTHETIC", fBinding.isSynthetic())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS DEPRECATED", fBinding.isDeprecated())); //$NON-NLS-1$
					break;
				case IBinding.TYPE:
					ITypeBinding typeBinding= (ITypeBinding) fBinding;
					StringBuffer kinds= new StringBuffer("KIND:"); //$NON-NLS-1$
					if (typeBinding.isAnnotation()) kinds.append(" isAnnotation"); //$NON-NLS-1$
					if (typeBinding.isAnonymous()) kinds.append(" isAnonymous"); //$NON-NLS-1$
					if (typeBinding.isArray()) kinds.append(" isArray"); //$NON-NLS-1$
					if (typeBinding.isClass()) kinds.append(" isClass"); //$NON-NLS-1$
					if (typeBinding.isEnum()) kinds.append(" isEnum"); //$NON-NLS-1$
					if (typeBinding.isGenericType()) kinds.append(" isGenericType"); //$NON-NLS-1$
					if (typeBinding.isInterface()) kinds.append(" isInterface"); //$NON-NLS-1$
					if (typeBinding.isNullType()) kinds.append(" isNullType"); //$NON-NLS-1$
					if (typeBinding.isParameterizedType()) kinds.append(" isParameterizedType"); //$NON-NLS-1$
					if (typeBinding.isPrimitive()) kinds.append(" isPrimitive"); //$NON-NLS-1$
					if (typeBinding.isRawType()) kinds.append(" isRawType"); //$NON-NLS-1$
					if (typeBinding.isTypeVariable()) kinds.append(" isTypeVariable"); //$NON-NLS-1$
					if (typeBinding.isWildcardType()) kinds.append(" isWildcardType"); //$NON-NLS-1$
					res.add(new BindingProperty(this, kinds.toString())); //$NON-NLS-1$

					res.add(new Binding(this, "ELEMENT TYPE", typeBinding.getElementType(), typeBinding.isArray())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "DIMENSIONS", typeBinding.getDimensions())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "TYPE BOUNDS", typeBinding.getTypeBounds())); //$NON-NLS-1$
					
					StringBuffer origin= new StringBuffer("ORIGIN:"); //$NON-NLS-1$
					if (typeBinding.isTopLevel()) origin.append(" isTopLevel"); //$NON-NLS-1$
					if (typeBinding.isNested()) origin.append(" isNested"); //$NON-NLS-1$
					if (typeBinding.isLocal()) origin.append(" isLocal"); //$NON-NLS-1$
					if (typeBinding.isMember()) origin.append(" isMember"); //$NON-NLS-1$
					if (typeBinding.isFromSource()) origin.append(" isFromSource"); //$NON-NLS-1$
					res.add(new BindingProperty(this, origin.toString()));
					
					res.add(new Binding(this, "PACKAGE", typeBinding.getPackage(), true)); //$NON-NLS-1$
					res.add(new Binding(this, "DECLARING CLASS", typeBinding.getDeclaringClass(), false)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "MODIFIERS", Flags.toString(fBinding.getModifiers()))); //$NON-NLS-1$
					res.add(new BindingProperty(this, "BINARY NAME", typeBinding.getBinaryName())); //$NON-NLS-1$

					res.add(new Binding(this, "ERASURE", typeBinding.getErasure(), true)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "TYPE PARAMETERS", typeBinding.getTypeParameters())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "TYPE ARGUMENTS", typeBinding.getTypeArguments())); //$NON-NLS-1$
					res.add(new Binding(this, "BOUND", typeBinding.getBound(), typeBinding.isWildcardType())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS UPPERBOUND", typeBinding.isUpperbound())); //$NON-NLS-1$

					res.add(new Binding(this, "SUPERCLASS", typeBinding.getSuperclass(), false)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "INTERFACES", typeBinding.getInterfaces())); //$NON-NLS-1$			
					res.add(new BindingProperty(this, "DECLARED MODIFIERS", Flags.toString(fBinding.getModifiers()))); //$NON-NLS-1$
					res.add(new BindingProperty(this, "DECLARED TYPES", typeBinding.getDeclaredTypes())); //$NON-NLS-1$			
					res.add(new BindingProperty(this, "DECLARED FIELDS", typeBinding.getDeclaredFields())); //$NON-NLS-1$			
					res.add(new BindingProperty(this, "DECLARED METHODS", typeBinding.getDeclaredMethods())); //$NON-NLS-1$			
					res.add(new BindingProperty(this, "IS SYNTHETIC", fBinding.isSynthetic())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS DEPRECATED", fBinding.isDeprecated())); //$NON-NLS-1$
					break;
				case IBinding.METHOD:
					IMethodBinding methodBinding= (IMethodBinding) fBinding;
					res.add(new BindingProperty(this, "IS CONSTRUCTOR", methodBinding.isConstructor())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS DEFAULT CONSTRUCTOR", methodBinding.isDefaultConstructor())); //$NON-NLS-1$
					res.add(new Binding(this, "DECLARING CLASS", methodBinding.getDeclaringClass(), true)); //$NON-NLS-1$
					res.add(new Binding(this, "RETURN TYPE", methodBinding.getReturnType(), true)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "MODIFIERS", Flags.toString(fBinding.getModifiers()))); //$NON-NLS-1$
					res.add(new BindingProperty(this, "PARAMETER TYPES", methodBinding.getParameterTypes())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS VARARGS", methodBinding.isVarargs())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "EXCEPTION TYPES", methodBinding.getExceptionTypes())); //$NON-NLS-1$
					
					StringBuffer genericsM= new StringBuffer("GENERICS:"); //$NON-NLS-1$
					if (methodBinding.isRawMethod()) genericsM.append(" isRawMethod"); //$NON-NLS-1$
					if (methodBinding.isGenericMethod()) genericsM.append(" isGenericMethod"); //$NON-NLS-1$
					if (methodBinding.isParameterizedMethod()) genericsM.append(" isParameterizedMethod"); //$NON-NLS-1$
					res.add(new BindingProperty(this, genericsM.toString()));
					
					res.add(new Binding(this, "ERASURE", methodBinding.getErasure(), true)); //$NON-NLS-1$
					res.add(new BindingProperty(this, "TYPE PARAMETERS", methodBinding.getTypeParameters())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "TYPE ARGUMENTS", methodBinding.getTypeArguments())); //$NON-NLS-1$			
					res.add(new BindingProperty(this, "IS SYNTHETIC", fBinding.isSynthetic())); //$NON-NLS-1$
					res.add(new BindingProperty(this, "IS DEPRECATED", fBinding.isDeprecated())); //$NON-NLS-1$

					break;
			}
			try {
				IJavaElement javaElement= fBinding.getJavaElement();
				res.add(new JavaElement(this, javaElement));
			} catch (RuntimeException e) {
				String label= ">java element: " + e.getClass().getName() + " for \"" + fBinding.getKey() + "\"";  //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				res.add(new Error(this, label, e));
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
					buf.append(typeBinding.getQualifiedName());
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
