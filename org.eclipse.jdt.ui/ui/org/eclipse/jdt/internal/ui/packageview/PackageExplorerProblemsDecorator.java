/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.ui.IWorkingSet;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;

import org.eclipse.jdt.internal.ui.viewsupport.TreeHierarchyLayoutProblemsDecorator;


public class PackageExplorerProblemsDecorator extends TreeHierarchyLayoutProblemsDecorator {

	/**
	 * Use of this constant is <b>FORBIDDEN</b> for external clients.
	 * <p>
	 * TODO: Make API in 3.7, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=308672
	 * 
	 * @see JavaElementImageDescriptor#BUILDPATH_ERROR
	 * @since 3.6
	 */
	public final static int BUILDPATH_ERROR= 0x2000;

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
			if ((flags & PackageExplorerProblemsDecorator.BUILDPATH_ERROR) != 0)
				return PackageExplorerProblemsDecorator.BUILDPATH_ERROR;
			result|= flags;
		}
		if ((result & JavaElementImageDescriptor.ERROR) != 0)
			return JavaElementImageDescriptor.ERROR;
		else if ((result & JavaElementImageDescriptor.WARNING) != 0)
			return JavaElementImageDescriptor.WARNING;
		return 0;
	}
}
