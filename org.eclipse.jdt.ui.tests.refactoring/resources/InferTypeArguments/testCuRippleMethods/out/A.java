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

import java.util.Enumeration;
import java.util.Vector;

public class A {
	private void createTestList(TestCollector collector) {
		Enumeration<String> each= collector.collectTests();
		while (each.hasMoreElements()) {
			String s= each.nextElement();
		}
	}
}

interface TestCollector {
	public Enumeration<String> collectTests();
}

class Collector implements TestCollector {
	public Enumeration<String> collectTests() {
		Vector<String> v= new Vector<String>();
		v.add("Test1");
		return v.elements();
	}
	
}