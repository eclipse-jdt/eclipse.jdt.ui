/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.resources.IProject;

import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

public class DestinationRenderer extends JavaElementLabelProvider {
	public DestinationRenderer(int flags) {
		super(flags);
	}

	public String getText(Object element) {
		try {
			if (element instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot root= (IPackageFragmentRoot)element;
				if (root.getUnderlyingResource() instanceof IProject)
					return "packages";
			}
		} catch (JavaModelException e) {
		}
		return super.getText(element);
	}
}