/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.infra;

import java.util.Collection;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;

public class SimpleSelectionProvider implements ISelectionProvider {
	private ISelection fSelection;

	public SimpleSelectionProvider(Collection collection) {
		this(collection.toArray());
	}

	public SimpleSelectionProvider(Object[] elements) {
		Assert.isNotNull(elements);
		fSelection= new StructuredSelection(elements);
	}

	public void addSelectionChangedListener(ISelectionChangedListener listener) {
	}

	public ISelection getSelection() {
		return fSelection;
	}

	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
	}

	public void setSelection(ISelection selection) {
	}

}
