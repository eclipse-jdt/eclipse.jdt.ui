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
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.browsing.LogicalPackage;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.search.ui.ISearchPageScoreComputer;

public class JavaSearchPageScoreComputer implements ISearchPageScoreComputer {

	public int computeScore(String id, Object element) {
		if (!JavaSearchPage.EXTENSION_POINT_ID.equals(id))
			// Can't decide
			return ISearchPageScoreComputer.UNKNOWN;
		
		if (element instanceof IJavaElement || element instanceof IClassFileEditorInput || element instanceof LogicalPackage)
			return 90;
		
		if (element instanceof IMarker) {
			Object handleId= null;
			try {
				handleId= ((IMarker)element).getAttribute(IJavaSearchUIConstants.ATT_JE_HANDLE_ID);
			} catch (CoreException ex) {
				ExceptionHandler.handle(ex, SearchMessages.getString("Search.Error.markerAttributeAccess.title"), SearchMessages.getString("Search.Error.markerAttributeAccess.message")); //$NON-NLS-2$ //$NON-NLS-1$
				// handleId is null
			}
			if (handleId != null)
				return 90;
		}
		return ISearchPageScoreComputer.LOWEST;
	}
}
