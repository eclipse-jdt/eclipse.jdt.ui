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

import org.eclipse.jdt.internal.ui.search.JavaSearchResultLabelProvider;

class NLSSearchResultLabelProvider extends JavaSearchResultLabelProvider {

	public String getText(Object o) {
		IMarker marker= getMarker(o);
		try {
			String text= (String)marker.getAttribute(IMarker.MESSAGE);
			if (text != null)
				return text;
		} catch (CoreException ex) {
		}
		// Error accessing the marker or missing IMarker.MESSAGE attribute
		return super.getText(o);
	}
}
