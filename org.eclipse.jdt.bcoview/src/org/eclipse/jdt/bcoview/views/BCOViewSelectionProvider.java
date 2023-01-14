/*******************************************************************************
 * Copyright (c) 2023 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.bcoview.BytecodeOutlinePlugin;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;

/**
 * Adapter for different selection provider in one view - text control and table control. See
 * <a href="http://forge.objectweb.org/tracker/?func=detail&atid=100023&aid=304424&group_id=23">bug
 * 304424</a> The main problem is, that
 *
 * <pre>
 * getSite().setSelectionProvider(viewSelectionProvider);
 * </pre>
 *
 * could be set only once per view, so that we cannot switch existing selection provider on the fly
 * (or I have no idea how to do this simplier way).
 */
class BCOViewSelectionProvider implements IPostSelectionProvider {

	private IPostSelectionProvider realProvider;

	private final List<IPostSelectionProvider> selProviders;

	private ISelection selection;

	public BCOViewSelectionProvider() {
		super();
		selProviders = new ArrayList<>();
	}

	public void setCurrentSelectionProvider(IPostSelectionProvider provider) {
		if (!selProviders.contains(provider)) {
			BytecodeOutlinePlugin.log(new Exception("Current selection provider is not registered yet"), IStatus.WARNING); //$NON-NLS-1$
			return;
		}
		realProvider = provider;
		if (selection != null) {
			realProvider.setSelection(selection);
		}
	}

	public void registerSelectionProvider(IPostSelectionProvider provider) {
		if (!selProviders.contains(provider)) {
			selProviders.add(provider);
		}
	}

	@Override
	public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
		for (IPostSelectionProvider provider : selProviders) {
			provider.addPostSelectionChangedListener(listener);
		}
	}

	@Override
	public void removePostSelectionChangedListener(ISelectionChangedListener listener) {
		for (IPostSelectionProvider provider : selProviders) {
			provider.removePostSelectionChangedListener(listener);
		}
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		for (IPostSelectionProvider provider : selProviders) {
			provider.addSelectionChangedListener(listener);
		}
	}

	@Override
	public ISelection getSelection() {
		return realProvider != null ? realProvider.getSelection() : null;
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		for (IPostSelectionProvider provider : selProviders) {
			provider.removeSelectionChangedListener(listener);
		}
	}

	@Override
	public void setSelection(ISelection selection) {
		this.selection = selection;
		if (realProvider != null) {
			realProvider.setSelection(selection);
		}
	}

}
