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
import java.util.LinkedList;
import java.util.List;

class A{
	List<String> list= new ArrayList<String>();
	
	public List<String> getList() {
		return list;
	}
	
	public void setList(List<String> list) {
		this.list= list;
	}
}

class B {
	static {
		A a= new A();
		a.setList(new LinkedList<String>());
		List<String> list= a.getList();
		list.addAll(a.list);
	}
}