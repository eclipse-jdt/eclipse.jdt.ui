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

import java.util.HashMap;
import java.util.Map;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.search.ui.ISearchResultView;
import org.eclipse.search.ui.SearchUI;

/**
 * @author tma
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class JavaSearchSorter extends ViewerSorter {
	
	private Map fLabelCache= new HashMap();

	public boolean isSorterProperty(Object element, String property) {
		return true;
	}
	
	protected String getLabel(Object element) {
		String label= (String) fLabelCache.get(element);
		if (label != null)
			return label;
		return fLabelProvider.getText(element);
	}

	/**
	 * Sets up the label provider according to the result from
	 * {@link #getLabelAppearance()}.
	 * @return true if the sort can proceed, false otherwise.
	 */
	protected boolean setupLabelProvider() {
		// Set label provider to show "element - path"
		ISearchResultView view= SearchUI.getSearchResultView();
		if (view == null)
			return false;
		fLabelProvider= view.getLabelProvider();
		if (fLabelProvider instanceof JavaSearchResultLabelProvider) {
			((JavaSearchResultLabelProvider)fLabelProvider).setAppearance(getLabelAppearance());
			return true;
		}
		return false;
	}

	/**
	 * @return The appearance flag for this sort order.
	 * 	One of the <code>int</code> constants in {@link JavaSearchResultLabelProvider}
	 */
	protected abstract int getLabelAppearance();

	protected ILabelProvider fLabelProvider;

	public void sort(Viewer viewer, Object[] elements) {
		if (!setupLabelProvider())
			return;
		cacheLabels(elements);
		super.sort(viewer, elements);
		fLabelCache.clear();
	}

	private void cacheLabels(Object[] elements) {
		for (int i= 0; i < elements.length; i++) {
			String label= fLabelProvider.getText(elements[i]);
			if (label != null)
				fLabelCache.put(elements[i], label);
		}
	}

}
