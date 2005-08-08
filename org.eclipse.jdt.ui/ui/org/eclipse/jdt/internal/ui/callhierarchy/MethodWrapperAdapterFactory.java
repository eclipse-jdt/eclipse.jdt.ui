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

package org.eclipse.jdt.internal.ui.callhierarchy;

import org.eclipse.core.runtime.IAdapterFactory;

import org.eclipse.ui.model.IWorkbenchAdapter;

import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

public class MethodWrapperAdapterFactory implements IAdapterFactory {

	private static Class[] TARGETS= new Class[] {
		IWorkbenchAdapter.class,
	};
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
	 */
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adapterType == IWorkbenchAdapter.class && adaptableObject instanceof MethodWrapper)
			return new MethodWrapperWorkbenchAdapter((MethodWrapper) adaptableObject);
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapterList()
	 */
	public Class[] getAdapterList() {
		return TARGETS;
	}

}
