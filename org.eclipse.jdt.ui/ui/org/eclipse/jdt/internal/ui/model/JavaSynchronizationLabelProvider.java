/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.model;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.team.core.diff.IDiffNode;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.mapping.ISynchronizationContext;

import org.eclipse.ltk.core.refactoring.RefactoringDescriptorProxy;
import org.eclipse.ltk.ui.refactoring.model.AbstractRefactoringSynchronizationLabelProvider;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Java-aware synchronization label provider.
 * 
 * @since 3.2
 */
public final class JavaSynchronizationLabelProvider extends AbstractRefactoringSynchronizationLabelProvider {

	/** The delegate label provider, or <code>null</code> */
	private ILabelProvider fLabelProvider= null;

	/** The model root, or <code>null</code> */
	private Object fModelRoot= null;

	/** The package image, or <code>null</code> */
	private Image fPackageImage= null;

	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		if (fPackageImage != null && !fPackageImage.isDisposed())
			fPackageImage.dispose();
		super.dispose();
	}

	/**
	 * {@inheritDoc}
	 */
	public Image getDelegateImage(final Object element) {
		if (element instanceof IPackageFragment) {
			final IPackageFragment fragment= (IPackageFragment) element;
			final IResource resource= fragment.getResource();
			if (!resource.exists()) {
				if (fPackageImage == null)
					fPackageImage= JavaPluginImages.DESC_OBJS_PACKAGE.createImage();
				return fPackageImage;
			}
		}
		return super.getDelegateImage(element);
	}

	/**
	 * {@inheritDoc}
	 */
	protected ILabelProvider getDelegateLabelProvider() {
		if (fLabelProvider == null)
			fLabelProvider= new JavaModelLabelProvider(ModelMessages.JavaModelLabelProvider_project_preferences_label, ModelMessages.JavaModelLabelProvider_refactorings_label);
		return fLabelProvider;
	}

	/**
	 * {@inheritDoc}
	 */
	protected int getDirection(final RefactoringDescriptorProxy proxy) {
		return IThreeWayDiff.INCOMING;
	}

	/**
	 * {@inheritDoc}
	 */
	protected int getKind(final RefactoringDescriptorProxy proxy) {
		return IDiffNode.CHANGE;
	}

	/**
	 * {@inheritDoc}
	 */
	protected Object getModelRoot() {
		if (fModelRoot == null)
			fModelRoot= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
		return fModelRoot;
	}

	/**
	 * {@inheritDoc}
	 */
	protected IDiffNode getDiff(final Object element) {
		final ISynchronizationContext context= getContext();
		final IResource resource= JavaModelProvider.getResource(element);
		if (context != null && resource != null) {
			final IDiffNode[] nodes= JavaSynchronizationContentProvider.getDiffs(context, element);
			for (int index= 0; index < nodes.length; index++) {
				if (context.getDiffTree().getResource(nodes[index]).equals(resource))
					return nodes[index];
			}
		}
		return super.getDiff(element);
	}
}
