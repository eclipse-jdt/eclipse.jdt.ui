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

import java.util.List;

class A {
	List l= null;
	
	void add(String s) {
		l.add(s);
	}
	
	String[] get() {
		return (String[]) l.toArray(new String[l.size()]);
	}
}
