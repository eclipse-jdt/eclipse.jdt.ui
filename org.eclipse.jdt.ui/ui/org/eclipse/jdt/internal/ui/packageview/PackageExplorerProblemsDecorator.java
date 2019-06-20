/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.ui.IWorkingSet;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;

import org.eclipse.jdt.internal.ui.viewsupport.TreeHierarchyLayoutProblemsDecorator;


public class PackageExplorerProblemsDecorator extends TreeHierarchyLayoutProblemsDecorator {

	public PackageExplorerProblemsDecorator() {
		super();
	}

	public PackageExplorerProblemsDecorator(boolean isFlatLayout) {
		super(isFlatLayout);
	}

	@Override
	protected int computeAdornmentFlags(Object obj) {
		if (!(obj instanceof IWorkingSet))
			return super.computeAdornmentFlags(obj);

		IWorkingSet workingSet= (IWorkingSet)obj;
		int result= 0;
		for (IAdaptable element : workingSet.getElements()) {
			int flags= super.computeAdornmentFlags(element);
			if ((flags & JavaElementImageDescriptor.BUILDPATH_ERROR) != 0)
				return JavaElementImageDescriptor.BUILDPATH_ERROR;
			result|= flags;
		}
		if ((result & JavaElementImageDescriptor.ERROR) != 0)
			return JavaElementImageDescriptor.ERROR;
		else if ((result & JavaElementImageDescriptor.WARNING) != 0)
			return JavaElementImageDescriptor.WARNING;
		else if ((result & JavaElementImageDescriptor.INFO) != 0)
			return JavaElementImageDescriptor.INFO;
		return 0;
	}
}
