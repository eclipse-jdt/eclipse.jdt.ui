/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.nls.search;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

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