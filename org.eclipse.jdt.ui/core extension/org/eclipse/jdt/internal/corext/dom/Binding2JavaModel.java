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
package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IMethodBinding;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

/**
 * A helper class to convert compiler bindings into corresponding 
 * Java elements.
 */
public class Binding2JavaModel {
	
	private Binding2JavaModel(){}


	/**
	 * @deprecated Please review. Too specific for a method on Bindings 
	 */
	public static IMethod findIncludingSupertypes(IMethodBinding method, IType type, IProgressMonitor pm) throws JavaModelException {
		IMethod inThisType= Bindings.findMethod(method, type);
		if (inThisType != null)
			return inThisType;
		IType[] superTypes= JavaModelUtil.getAllSuperTypes(type, pm);
		for (int i= 0; i < superTypes.length; i++) {
			IMethod m= Bindings.findMethod(method, superTypes[i]);
			if (m != null)
				return m;
		}
		return null;
	}

}

