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
package org.eclipse.jdt.internal.corext.refactoring.participants.xml;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

/* package */ class LRUCache {
	
	private LinkedHashMap fCache;
	
	public LRUCache(final int cacheSize) {
		fCache= new LinkedHashMap(cacheSize * 2, 0.5f, true) {
			protected boolean removeEldestEntry(Entry eldest) {
				return size() > cacheSize;			}
		};
	}
	
	public Object get(Object key) {
		return fCache.get(key);
	}
	
	public void put(Object key, Object value) {
		fCache.put(key, value);
	}
}
