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

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

/**
 *
 */
public class BindingProperty {
	
	private IBinding fBinding;
	private ASTNode fParent;
	
	public BindingProperty(ASTNode parent, IBinding binding) {
		fParent= parent;
		fBinding= binding;
	}
	
	public ASTNode getParent() {
		return fParent;
	}
	
	public IBinding getBinding() {
		return fBinding;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buf= new StringBuffer();
		if (fBinding != null) {
			switch (fBinding.getKind()) {
				case IBinding.VARIABLE:
					IVariableBinding variableBinding= (IVariableBinding) fBinding;
					
					buf.append("(variable binding: "); //$NON-NLS-1$
					if (! variableBinding.isField()) {
						buf.append(variableBinding.toString());
					} else if (variableBinding.getDeclaringClass() == null) {
						buf.append("(array type):length"); //$NON-NLS-1$
					} else {
						buf.append(variableBinding.getDeclaringClass().getName());
						buf.append(':');
						buf.append(variableBinding.getName());				
					}
					break;
				case IBinding.PACKAGE:
					IPackageBinding packageBinding= (IPackageBinding) fBinding;
					buf.append("(package binding: "); //$NON-NLS-1$
					buf.append(packageBinding.getName());
					break;
				case IBinding.TYPE:
					ITypeBinding typeBinding= (ITypeBinding) fBinding;
					buf.append("(type binding: "); //$NON-NLS-1$
					buf.append(typeBinding.getQualifiedName());
					break;
				case IBinding.METHOD:
					IMethodBinding methodBinding= (IMethodBinding) fBinding;
					buf.append("(method binding: "); //$NON-NLS-1$
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
			buf.append(')');
		} else {
			buf.append("(binding: null)"); //$NON-NLS-1$
		}
		return buf.toString();
	}
	

}
