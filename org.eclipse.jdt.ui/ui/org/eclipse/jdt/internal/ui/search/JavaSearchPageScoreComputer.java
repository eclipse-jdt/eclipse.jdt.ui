package org.eclipse.jdt.internal.ui.search;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
import org.omg.CORBA.UNKNOWN;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.search.ui.ISearchPageScoreComputer;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditorInput;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class JavaSearchPageScoreComputer implements ISearchPageScoreComputer {

	public int computeScore(String id, Object element) {
		if (!JavaSearchPage.EXTENSION_POINT_ID.equals(id))
			// Can't decide
			return ISearchPageScoreComputer.UNKNOWN;
		
		if (element instanceof IJavaElement || element instanceof ClassFileEditorInput)
			return 90;
		
		if (element instanceof IMarker) {
			Object handleId= null;
			try {
				handleId= ((IMarker)element).getAttribute(IJavaSearchUIConstants.ATT_JE_HANDLE_ID);
			} catch (CoreException ex) {
				ExceptionHandler.handle(ex, JavaPlugin.getResourceBundle(), "Search.Error.markerAttributeAccess.");
				// handleId is null
			}
			if (handleId != null)
				return 90;
		}
		return ISearchPageScoreComputer.LOWEST;
	}
}