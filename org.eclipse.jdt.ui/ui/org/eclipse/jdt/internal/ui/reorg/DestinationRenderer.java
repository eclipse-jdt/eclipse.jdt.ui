/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
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
					return ReorgMessages.getString("DestinationRenderer.packages"); //$NON-NLS-1$
			}
		} catch (JavaModelException e) {
		}
		return super.getText(element);
	}
}