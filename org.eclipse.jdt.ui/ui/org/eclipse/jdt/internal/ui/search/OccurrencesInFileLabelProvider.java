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

package org.eclipse.jdt.internal.ui.search;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Label provider for the <code>FindOccurrencesInFile</code> results.
 * @see org.eclipse.jdt.ui.actions.FindOccurrencesInFileAction
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
			write= (Boolean)marker.getAttribute(OccurrencesFinder.IS_WRITEACCESS);
			writeValue= write != null && write.booleanValue();
		} catch (CoreException e) {
		}
		return writeValue;
	}
	
	private boolean isVariableAccess(IMarker marker) {
		Boolean variable= null;
		boolean variableValue= false;
		try {
			variable= (Boolean)marker.getAttribute(OccurrencesFinder.IS_VARIABLE);
			variableValue= variable != null && variable.booleanValue();
		} catch (CoreException e) {
		}
		return variableValue;
	}
}
