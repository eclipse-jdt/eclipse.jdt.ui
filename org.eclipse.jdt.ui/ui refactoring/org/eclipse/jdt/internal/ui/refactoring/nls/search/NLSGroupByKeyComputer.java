/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.nls.search;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.search.ui.IGroupByKeyComputer;

import org.eclipse.jdt.internal.ui.search.IJavaSearchUIConstants;
import org.eclipse.jdt.internal.ui.search.SearchMessages;
import org.eclipse.jdt.internal.ui.search.SearchUtil;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/** @deprecated */
class NLSGroupByKeyComputer implements IGroupByKeyComputer {

	IJavaElement fLastJavaElement= null;
	String fLastHandle= null;

	public Object computeGroupByKey(IMarker marker) {
		if (marker == null || marker.getResource() == null)
			return null;
		
		if (!marker.getResource().getName().endsWith(".java")) //$NON-NLS-1$
			return marker.getResource();
		
		IJavaElement jElement= getJavaElement(marker);
		if (jElement != null && jElement.exists()) {
			// no help from JavaModel to rename yet
			// return getJavaElement(marker);
			return fLastHandle;
		}	
		return null;
	}

	private String getJavaElementHandleId(IMarker marker) {
		try {
			return (String)marker.getAttribute(IJavaSearchUIConstants.ATT_JE_HANDLE_ID);
		} catch (CoreException ex) {
			ExceptionHandler.handle(ex, SearchMessages.getString("Search.Error.markerAttributeAccess.title"), SearchMessages.getString("Search.Error.markerAttributeAccess.message")); //$NON-NLS-2$ //$NON-NLS-1$
			return null;
		}
	}
	
	private IJavaElement getJavaElement(IMarker marker) {
		String handle= getJavaElementHandleId(marker);
		if (handle == null) {
			fLastHandle= null;
			fLastJavaElement= null;
			return null;
		}
		
		if (!handle.equals(fLastHandle)) {
			fLastHandle= handle;
			fLastJavaElement= SearchUtil.getJavaElement(marker);
			IResource handleResource= null;
			try {
				if (fLastJavaElement != null)
					handleResource= fLastJavaElement.getCorrespondingResource();
			} catch (JavaModelException  ex) {
				ExceptionHandler.handle(ex, SearchMessages.getString("Search.Error.javaElementAccess.title"), SearchMessages.getString("Search.Error.javaElementAccess.message")); //$NON-NLS-2$ //$NON-NLS-1$
				// handleResource= null;
			}
			if (fLastJavaElement != null && marker.getResource().equals(handleResource)) {
				// need to get and set new handle here
			}
		}
		return fLastJavaElement;
	}
}
