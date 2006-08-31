/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IFolder;


import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;

/**
 * Provides the labels for the Package Explorer.
 * <p>
 * It provides labels for the packages in hierarchical layout and in all
 * other cases delegates it to its super class.
 * </p>
 * @since 2.1
 */
public class PackageExplorerLabelProvider extends AppearanceAwareLabelProvider {
	
	private PackageExplorerContentProvider fContentProvider;

	private boolean fIsFlatLayout;
	private PackageExplorerProblemsDecorator fProblemDecorator;

	public PackageExplorerLabelProvider(long textFlags, int imageFlags, PackageExplorerContentProvider cp) {
		super(textFlags, imageFlags);
		fProblemDecorator= new PackageExplorerProblemsDecorator();
		addLabelDecorator(fProblemDecorator);
		Assert.isNotNull(cp);
		fContentProvider= cp;
	}


	public String getText(Object element) {
		if (!fIsFlatLayout && element instanceof IPackageFragment) {
			IPackageFragment fragment = (IPackageFragment) element;
			Object parent= fContentProvider.getPackageFragmentProvider().getHierarchicalPackageParent(fragment);
			if (parent instanceof IPackageFragment) {
				return getNameDelta((IPackageFragment) parent, fragment);
			} else if (parent instanceof IFolder) { // bug 152735
				return getNameDelta((IFolder) parent, fragment);
			}
		}
		return super.getText(element);
	}
	
	private String getNameDelta(IPackageFragment parent, IPackageFragment fragment) {
		String prefix= parent.getElementName() + '.';
		String fullName= fragment.getElementName();
		if (fullName.startsWith(prefix)) {
			return fullName.substring(prefix.length());
		}
		return fullName;
	}
	
	private String getNameDelta(IFolder parent, IPackageFragment fragment) {
		IPath prefix= parent.getFullPath();
		IPath fullPath= fragment.getPath();
		if (prefix.isPrefixOf(fullPath)) {
			StringBuffer buf= new StringBuffer();
			for (int i= prefix.segmentCount(); i < fullPath.segmentCount(); i++) {
				if (buf.length() > 0)
					buf.append('.');
				buf.append(fullPath.segment(i));
			}
			return buf.toString();
		}
		return fragment.getElementName();
	}
	
	public void setIsFlatLayout(boolean state) {
		fIsFlatLayout= state;
		fProblemDecorator.setIsFlatLayout(state);
	}
}
