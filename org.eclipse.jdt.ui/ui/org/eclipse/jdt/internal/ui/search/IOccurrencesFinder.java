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

import java.util.List;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.IDocument;
import org.eclipse.search.ui.IGroupByKeyComputer;
import org.eclipse.search.ui.ISearchResultView;
import org.eclipse.search.ui.text.Match;

public interface IOccurrencesFinder {
	
	/** @deprecated not used for new search */
	public static class SearchGroupByKeyComputer implements IGroupByKeyComputer {
		public Object computeGroupByKey(IMarker marker) {
			return marker; 
		}
	}
	
	public String initialize(CompilationUnit root, int offset, int length);
	
	public List perform();
	
	/** @deprecated not used for new search */
	public IMarker[] createMarkers(IResource file, IDocument document) throws CoreException;
	
	/** @deprecated not used for new search */
	public void searchStarted(ISearchResultView view, String inputName);
	
	public String getJobLabel();

	public String getPluralLabelPattern(String documentName);
	
	public String getSingularLabel(String documentName);
	
	public Match[] getOccurrenceMatches(IJavaElement element, IDocument document);
	
}
