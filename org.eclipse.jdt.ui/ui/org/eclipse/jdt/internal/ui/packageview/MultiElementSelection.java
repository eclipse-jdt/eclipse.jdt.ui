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
package org.eclipse.jdt.internal.ui.packageview;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.jface.viewers.StructuredSelection;

/**
 * A special structured selection that carries additional information
 * about parent information since the package explorer show elements
 * multiple times
 */
public class MultiElementSelection extends StructuredSelection {
	
	private static final TreeItem[] EMPTY_TREE_ITEM_ARRAY= new TreeItem[0];
	private static final Object[][] EMPTY_PARENT_CHAINS= new Object[0][0];
	
	private Map fElement2TreeItem= new HashMap();
	
	public MultiElementSelection(List elements, Map element2TreeItem) {
		super(elements);
		fElement2TreeItem= element2TreeItem;
	}

	public int getNumberOfItems(Object element) {
		Object obj= fElement2TreeItem.get(element);
		if (obj == null)
			return 0;
		if (obj instanceof TreeItem)
			return 1;
		return ((List)obj).size();
	}
	
	public TreeItem[] getItems(Object element) {
		Object obj= fElement2TreeItem.get(element);
		if (obj == null)
			return EMPTY_TREE_ITEM_ARRAY;
		if (obj instanceof TreeItem)
			return new TreeItem[] {(TreeItem)obj};
		List l= (List)obj;
		return (TreeItem[])l.toArray(new TreeItem[l.size()]);
	}
	
	public Object[][] getParentChains(Object element) {
		Object obj= fElement2TreeItem.get(element);
		if (obj == null)
			return EMPTY_PARENT_CHAINS;
		if (obj instanceof TreeItem)
			return new Object[][]{ getParents((TreeItem)obj)};
		List l= (List)obj;
		List result= new ArrayList(l.size());
		for (Iterator iter= l.iterator(); iter.hasNext();) {
			result.add(getParents((TreeItem)iter.next()));
		}
		return (Object[][])result.toArray(new Object[result.size()][]);
	}
	
	private Object[] getParents(TreeItem item) {
		List result= new ArrayList();
		TreeItem parent= item.getParentItem();
		while (parent != null) {
			// add null as well to signal unknown element
			result.add(parent.getData());
			parent= parent.getParentItem();
		}
		result.add(item.getParent().getData());
		return result.toArray();
	}
}
