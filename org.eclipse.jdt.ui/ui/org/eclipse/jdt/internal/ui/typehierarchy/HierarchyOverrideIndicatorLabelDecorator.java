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
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.OverrideIndicatorLabelDecorator;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;


class HierarchyOverrideIndicatorLabelDecorator extends OverrideIndicatorLabelDecorator {
	
	private TypeHierarchyLifeCycle fHierarchy;
	
	public HierarchyOverrideIndicatorLabelDecorator(TypeHierarchyLifeCycle lifeCycle) {
		super(null);
		fHierarchy= lifeCycle;
	}
	
	/* (non-Javadoc)
	 * @see OverrideIndicatorLabelDecorator#getOverrideIndicators(IMethod)
	 */
	protected int getOverrideIndicators(IMethod method) throws JavaModelException {
		IType type= (IType) JavaModelUtil.toOriginal(method.getDeclaringType());
		ITypeHierarchy hierarchy= fHierarchy.getHierarchy();
		if (hierarchy != null) {
			return findInHierarchy(type, hierarchy, method.getElementName(), method.getParameterTypes());
		}
		return 0;
	}
}