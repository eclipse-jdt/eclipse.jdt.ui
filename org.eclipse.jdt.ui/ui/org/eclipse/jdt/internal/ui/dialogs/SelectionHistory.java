/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.dialogs;


import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.jdt.internal.ui.util.TypeInfoLabelProvider;


public class SelectionHistory {
	
	private static abstract class AbstractHistoryComparator implements Comparator {
		
		private final Map fHistory;
		
		public AbstractHistoryComparator() {
			fHistory= new LinkedHashMap(80, 0.75f, false) {
				private static final long serialVersionUID= 1L;
				protected boolean removeEldestEntry(Map.Entry eldest) {
					return size() > CACHE_SIZE;
				}
			};
		}

		public void remember(Object o) {
			Object key= getKey(o);
			if (fHistory.containsKey(key))
				fHistory.remove(key);
			
			fHistory.put(key, o);
		}
		
		protected Map getHistory() {
			return fHistory;
		}

		public void clear() {
			fHistory.clear();
		}
		
		/**
		 * {@inheritDoc}
		 */
		public boolean equals(Object o) {
			if (compare(this, o) == 0)
				return true;
			
			return false;
		}
		
		protected abstract Object getKey(Object o);

		public boolean contains(Object o) {
			return fHistory.containsKey(getKey(o));
		}
		
	}
	
	private static final class OrganizeImportComparator extends AbstractHistoryComparator {
		
		private static final ILabelProvider labelProvider= new TypeInfoLabelProvider(TypeInfoLabelProvider.SHOW_FULLYQUALIFIED);

		/**
		 * {@inheritDoc}
		 */
		protected Object getKey(Object object) {
			return labelProvider.getText(object);
		}

		/**
		 * {@inheritDoc}
		 */
		public int compare(Object o1, Object o2) {
			Map history= getHistory();
			String key1= (String)getKey(o1);
			String key2= (String)getKey(o2);
			
			if (key1.equals(key2))
				return 0;
			
			if (history.containsKey(key1)) {
				if (history.containsKey(key2)) {
					Collection collection= history.values();
					for (Iterator iter= collection.iterator(); iter.hasNext();) {
						Object element= iter.next();
						String elementKey= (String)getKey(element);
						if (elementKey.equals(key1)) {
							return 1;
						} else if (elementKey.equals(key2)) {
							return -1;
						}
					}
					return 0;
				} else {
					return -1;
				}
			} else if (history.containsKey(key2)) {
				return 1;
			} else {
				return key1.compareTo(key2);
			}
		}
	}

	public static final int ORGANIZE_IMPORT_ID= 0;
	
	private static final int CACHE_SIZE= 50;
	private static final SelectionHistory[] INSTANCES= new SelectionHistory[1];
	
	public static SelectionHistory getInstance(int id) {
		if (id < 0 || id >= INSTANCES.length)
			throw new IllegalArgumentException("id out of range"); //$NON-NLS-1$
		
		switch (id) {
		case ORGANIZE_IMPORT_ID:
			if (INSTANCES[ORGANIZE_IMPORT_ID] != null)
				return INSTANCES[ORGANIZE_IMPORT_ID];

			AbstractHistoryComparator comparator= new OrganizeImportComparator();
			SelectionHistory oiHistory= new SelectionHistory(comparator);
			INSTANCES[ORGANIZE_IMPORT_ID]= oiHistory;
			return INSTANCES[ORGANIZE_IMPORT_ID];
			
		}
		return null;
	}
	
	private final AbstractHistoryComparator fComparator;
	
	public SelectionHistory(AbstractHistoryComparator comparator) {
		fComparator= comparator;
	}

	public void remember(Object object) {
		fComparator.remember(object);
	}

	public Comparator getComparator() {
		return fComparator;
	}

	public void clear() {
		fComparator.clear();
	}

	public boolean contains(Object o) {
		return fComparator.contains(o);
	}
	
}
