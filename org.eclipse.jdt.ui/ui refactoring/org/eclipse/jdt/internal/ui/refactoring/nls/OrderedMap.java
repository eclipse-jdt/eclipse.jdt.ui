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
package org.eclipse.jdt.internal.ui.refactoring.nls;

import java.util.HashMap;
import java.util.Stack;

public class OrderedMap {
	
	private Stack fStack= new Stack();
	private HashMap fMap= new HashMap();
	
	public void push(Object key, Object value) {
		remove(key);
		fMap.put(key, value);
		fStack.push(value);
	}
	
	public Object peek() {
		if (fStack.isEmpty())
			return null;
		return fStack.peek();
	}

	public void remove(Object key) {
		Object value= fMap.remove(key);
		if (value != null)
			fStack.remove(value);
	}
}
