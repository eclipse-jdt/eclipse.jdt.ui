/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others. All
 * rights reserved. This program and the accompanying materials are made
 * available under the terms of the Common Public License v0.5 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/cpl-v05.
 * html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.jdt.internal.ui.search;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swt.graphics.Image;

/**
 * Label provider for the SearchUsagesInFile results.
 * @see SearchUsagesInFileAction
 */
class OccurrencesInFileLabelProvider extends JavaSearchResultLabelProvider {

	public String getText(Object o) {
		IMarker marker= getMarker(o);
		try {
			String text= (String)marker.getAttribute(IMarker.MESSAGE);
			if (text != null)
				return text.replace('\t', ' ');
		} catch (CoreException ex) {
			return "";  //$NON-NLS-1$
		}
		return super.getText(o);
	}
	
	public Image getImage(Object element) {
		IMarker marker= getMarker(element);
		if (isVariableAccess(marker)) {
			if (isWriteAccess(marker))
				return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_SEARCH_WRITEACCESS);
			return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_SEARCH_READACCESS);
		}
		return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_SEARCH_OCCURRENCE);
	}
	
	private boolean isWriteAccess(IMarker marker) {
		Boolean write= null;
		boolean writeValue= false;
		try {
			write= (Boolean)marker.getAttribute(FindOccurrencesEngine.IS_WRITEACCESS);
			writeValue= write != null && write.booleanValue();
		} catch (CoreException e) {
		}
		return writeValue;
	}
	
	private boolean isVariableAccess(IMarker marker) {
		Boolean variable= null;
		boolean variableValue= false;
		try {
			variable= (Boolean)marker.getAttribute(FindOccurrencesEngine.IS_VARIABLE);
			variableValue= variable != null && variable.booleanValue();
		} catch (CoreException e) {
		}
		return variableValue;
	}
}