/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
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