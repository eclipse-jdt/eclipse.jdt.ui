/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.search;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.widgets.Table;

import org.eclipse.jface.viewers.TableViewer;

import org.eclipse.search.ui.text.AbstractTextSearchResult;

public class JavaSearchTableContentProvider extends JavaSearchContentProvider {
	public JavaSearchTableContentProvider(JavaSearchResultPage page) {
		super(page);
	}
	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof AbstractTextSearchResult) {
			Set<Object> filteredElements= new HashSet<>();
			int limit= getPage().getElementLimit();
			for (Object rawElement : ((AbstractTextSearchResult)inputElement).getElements()) {
				if (getPage().getDisplayedMatchCount(rawElement) > 0) {
					filteredElements.add(rawElement);
					if (limit != -1 && limit < filteredElements.size()) {
						break;
					}
				}
			}
			return filteredElements.toArray();
		}
		return EMPTY_ARR;
	}

	@Override
	public void elementsChanged(Object[] updatedElements) {
		if (getSearchResult() == null)
			return;

		int addLimit= getAddLimit();

		TableViewer viewer= (TableViewer) getPage().getViewer();
		Set<Object> updated= new HashSet<>();
		Set<Object> added= new HashSet<>();
		Set<Object> removed= new HashSet<>();
		for (Object updatedElement : updatedElements) {
			if (getPage().getDisplayedMatchCount(updatedElement) > 0) {
				if (viewer.testFindItem(updatedElement) != null) {
					updated.add(updatedElement);
				} else {
					if (addLimit > 0) {
						added.add(updatedElement);
						addLimit--;
					}
				}
			} else {
				removed.add(updatedElement);
			}
		}

		viewer.add(added.toArray());
		viewer.update(updated.toArray(), new String[] { SearchLabelProvider.PROPERTY_MATCH_COUNT });
		viewer.remove(removed.toArray());
	}

	private int getAddLimit() {
		int limit= getPage().getElementLimit();
		if (limit != -1) {
			Table table= (Table) getPage().getViewer().getControl();
			int itemCount= table.getItemCount();
			if (itemCount >= limit) {
				return 0;
			}
			return limit - itemCount;
		}
		return Integer.MAX_VALUE;
	}

	@Override
	public void clear() {
		getPage().getViewer().refresh();
	}

}
