/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
 package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.core.runtime.IAdapterFactory;

import org.eclipse.ltk.internal.ui.refactoring.IChangeElementChildrenCreator;

import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;

public class RefactoringAdapterFactory implements IAdapterFactory {

	private static final Class[] ADAPTER_LIST= new Class[] {
		IChangeElementChildrenCreator.class
	};

	public Class[] getAdapterList() {
		return ADAPTER_LIST;
	}

	public Object getAdapter(Object object, Class key) {
		if (!IChangeElementChildrenCreator.class.equals(key))
			return null;
		if (!(object instanceof CompilationUnitChange))
			return null;
		return new CompilationUnitChangeChildrenCreator();
	}
}