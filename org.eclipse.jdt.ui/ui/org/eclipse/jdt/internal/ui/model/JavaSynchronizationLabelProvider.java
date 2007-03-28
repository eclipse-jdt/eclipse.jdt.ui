/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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

import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IDiffTree;
import org.eclipse.team.core.mapping.ISynchronizationContext;

import org.eclipse.ltk.ui.refactoring.model.AbstractSynchronizationLabelProvider;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.ProblemsLabelDecorator;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Java-aware synchronization label provider.
 * 
 * @since 3.2
 */
public final class JavaSynchronizationLabelProvider extends AbstractSynchronizationLabelProvider {

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
			if (resource == null || !resource.exists()) {
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
			fLabelProvider= new DecoratingLabelProvider(new JavaModelLabelProvider(ModelMessages.JavaModelLabelProvider_project_preferences_label, ModelMessages.JavaModelLabelProvider_refactorings_label), new ProblemsLabelDecorator(null));
		return fLabelProvider;
	}

	/**
	 * {@inheritDoc}
	 */
	protected IDiff getDiff(final Object element) {
		final ISynchronizationContext context= getContext();
		final IResource resource= JavaModelProvider.getResource(element);
		if (context != null && resource != null) {
			final IDiff[] diff= JavaSynchronizationContentProvider.getDiffs(context, element);
			for (int index= 0; index < diff.length; index++) {
				if (context.getDiffTree().getResource(diff[index]).equals(resource))
					return diff[index];
			}
		}
		return super.getDiff(element);
	}

	/**
	 * {@inheritDoc}
	 */
	protected int getMarkerSeverity(final Object element) {
		// Decoration label provider is handling this
		return -1;
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
	protected boolean hasDecendantConflicts(final Object element) {
		final ISynchronizationContext context= getContext();
		final IResource resource= JavaModelProvider.getResource(element);
		if (context != null && resource != null)
			return context.getDiffTree().getProperty(resource.getFullPath(), IDiffTree.P_HAS_DESCENDANT_CONFLICTS);
		return super.hasDecendantConflicts(element);
	}

	/**
	 * {@inheritDoc}
	 */
	protected boolean isBusy(final Object element) {
		final ISynchronizationContext context= getContext();
		final IResource resource= JavaModelProvider.getResource(element);
		if (context != null && resource != null)
			return context.getDiffTree().getProperty(resource.getFullPath(), IDiffTree.P_BUSY_HINT);
		return super.isBusy(element);
	}

	/**
	 * {@inheritDoc}
	 */
	protected boolean isIncludeOverlays() {
		return true;
	}
}
