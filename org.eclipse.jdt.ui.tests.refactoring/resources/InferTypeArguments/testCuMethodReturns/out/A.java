/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package p;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class A {
	public void foo() {
		for (Iterator<String> iter= getList().iterator(); iter.hasNext();) {
			String element= iter.next();
			System.out.println(element);
		}
	}
	
	public List<String> getList() {
		ArrayList<String> result= new ArrayList<String>();
		result.add("Tom");
		result.add("Jerry");
		return result;
	}
	
	public Iterator<String> getIterator(List<String> list) {
		list= getList();
		return list.iterator();
	}
	
	public Iterator getIterator2(List list2) {
		return null;
	}
}