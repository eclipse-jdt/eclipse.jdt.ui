/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.search2;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.search.IJavaSearchUIConstants;
import org.eclipse.jdt.internal.ui.search.SearchUtil;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.search.ui.IGroupByKeyComputer;

public class GroupByKeyComputer implements IGroupByKeyComputer {

	IJavaElement fLastJavaElement= null;
	String fLastHandle= null;

	public Object computeGroupByKey(IMarker marker) {
		if (marker == null)
			return null;
		
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
			ExceptionHandler.handle(ex, "Search Error", "An error occurred while accessing a marker attribute");  
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
			fLastJavaElement= SearchUtil.getJavaElement(marker);
			if (fLastJavaElement != null)
				fLastHandle= fLastJavaElement.getHandleIdentifier();
			else
				fLastHandle= null;
		}
		return fLastJavaElement;
	}
}
