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

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;

public class JavaSearchTableContentProvider extends JavaSearchContentProvider implements IStructuredContentProvider {
	private TableViewer fTableViewer;

	public JavaSearchTableContentProvider(TableViewer viewer) {
		fTableViewer= viewer;
	}
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof JavaSearchResult)
			return ((JavaSearchResult)inputElement).getElements();
		return EMPTY_ARR;
	}

	public void elementsChanged(Object[] updatedElements) {
		int addCount= 0;
		int removeCount= 0;
		for (int i= 0; i < updatedElements.length; i++) {
			if (fResult.getMatchCount(updatedElements[i]) > 0) {
				if (fTableViewer.testFindItem(updatedElements[i]) != null)
					fTableViewer.refresh(updatedElements[i]);
				else
					fTableViewer.add(updatedElements[i]);
				addCount++;
			} else {
				fTableViewer.remove(updatedElements[i]);
				removeCount++;
			}
		}
	}

	public void clear() {
		fTableViewer.refresh();
	}

}
