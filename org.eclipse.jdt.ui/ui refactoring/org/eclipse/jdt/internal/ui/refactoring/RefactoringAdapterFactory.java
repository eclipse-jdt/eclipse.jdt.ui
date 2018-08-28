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
 package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.core.runtime.IAdapterFactory;

import org.eclipse.ltk.core.refactoring.TextEditBasedChange;
import org.eclipse.ltk.ui.refactoring.TextEditChangeNode;

import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.refactoring.changes.MultiStateCompilationUnitChange;

public class RefactoringAdapterFactory implements IAdapterFactory {

	private static final Class<?>[] ADAPTER_LIST= new Class[] {
		TextEditChangeNode.class
	};

	@Override
	public Class<?>[] getAdapterList() {
		return ADAPTER_LIST;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Object object, Class<T> key) {
		if (!TextEditChangeNode.class.equals(key))
			return null;
		if (!(object instanceof CompilationUnitChange) && !(object instanceof MultiStateCompilationUnitChange))
			return null;
		return (T) new CompilationUnitChangeNode((TextEditBasedChange)object);
	}
}
