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
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.ui.IWorkingSet;

import org.eclipse.jdt.internal.ui.viewsupport.TreeHierarchyLayoutProblemsDecorator;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;


public class PackageExplorerProblemsDecorator extends TreeHierarchyLayoutProblemsDecorator {

	public PackageExplorerProblemsDecorator() {
		super();
	}
	
	public PackageExplorerProblemsDecorator(boolean isFlatLayout) {
		super(isFlatLayout);
	}
	
	protected int computeAdornmentFlags(Object obj) {
		if (!(obj instanceof IWorkingSet))
			return super.computeAdornmentFlags(obj);
		
		IWorkingSet workingSet= (IWorkingSet)obj;
		IAdaptable[] elements= workingSet.getElements();
		int result= 0;
		for (int i= 0; i < elements.length; i++) {
			IAdaptable element= elements[i];
			int flags= super.computeAdornmentFlags(element);
			if ((flags & JavaElementImageDescriptor.ERROR) != 0)
				return JavaElementImageDescriptor.ERROR;
			if ((flags & JavaElementImageDescriptor.WARNING) != 0)
				result= JavaElementImageDescriptor.WARNING;
		}
		return result;
	}
}
