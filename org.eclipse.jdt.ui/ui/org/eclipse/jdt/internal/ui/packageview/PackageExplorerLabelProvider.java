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

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ITreeContentProvider;

import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.TreeHierarchyLayoutProblemsDecorator;

/**
 * Provides the labels for the Package Explorer.
 * <p>
 * It provides labels for the packages in hierarchical layout and in all
 * other cases delegates it to its super class.
 * </p>
 * @since 2.1
 */
class PackageExplorerLabelProvider extends AppearanceAwareLabelProvider {
	
	private ITreeContentProvider fContentProvider;

	private boolean fIsFlatLayout;
	private TreeHierarchyLayoutProblemsDecorator fProblemDecorator;

	PackageExplorerLabelProvider(long textFlags, int imageFlags, ITreeContentProvider cp) {
		super(textFlags, imageFlags);
		fProblemDecorator= new TreeHierarchyLayoutProblemsDecorator();
		addLabelDecorator(fProblemDecorator);
		Assert.isNotNull(cp);
		fContentProvider= cp;
	}


	public String getText(Object element) {
		
		if (fIsFlatLayout || !(element instanceof IPackageFragment))
			return super.getText(element);			

		IPackageFragment fragment = (IPackageFragment) element;
		
		if (fragment.isDefaultPackage()) {
			return super.getText(fragment);
		} else {
			Object parent= fContentProvider.getParent(fragment);
			if(parent instanceof IPackageFragment)
				return getNameDelta((IPackageFragment) parent, fragment);
			else return super.getText(fragment);
		}
	}
	
	private String getNameDelta(IPackageFragment topFragment, IPackageFragment bottomFragment) {
		
		String topName= topFragment.getElementName();
		String bottomName= bottomFragment.getElementName();
		
		if(topName.equals(bottomName))
			return topName;
		
		String deltaname= bottomName.substring(topName.length()+1);	
		return deltaname;
	}
	
	void setIsFlatLayout(boolean state) {
		fIsFlatLayout= state;
		fProblemDecorator.setIsFlatLayout(state);
	}
}
