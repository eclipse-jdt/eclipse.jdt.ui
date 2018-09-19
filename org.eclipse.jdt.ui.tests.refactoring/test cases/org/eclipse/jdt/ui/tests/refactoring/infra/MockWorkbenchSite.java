/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring.infra;

import java.util.List;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.PlatformObject;

import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class MockWorkbenchSite extends PlatformObject implements IWorkbenchSite {

	private ISelectionProvider fProvider;

	public MockWorkbenchSite(ISelectionProvider provider){
		setSelectionProvider(provider);
	}

	public MockWorkbenchSite(Object[] elements){
		this(new SimpleSelectionProvider(elements));
	}

	public MockWorkbenchSite(List<?> elements){
		this(new SimpleSelectionProvider(elements));
	}

	@Override
	public IWorkbenchPage getPage() {
		return null;
	}

	@Override
	public ISelectionProvider getSelectionProvider() {
		return fProvider;
	}

	@Override
	public Shell getShell() {
		return JavaPlugin.getActiveWorkbenchShell();
	}

	@Override
	public IWorkbenchWindow getWorkbenchWindow() {
		return null;
	}

	@Override
	public void setSelectionProvider(ISelectionProvider provider) {
		Assert.isNotNull(provider);
		fProvider= provider;
	}

	@Override
	public <T> T getService(Class<T> api) {
		return null;
	}

	@Override
	public boolean hasService(Class<?> key) {
		return false;
	}
}
