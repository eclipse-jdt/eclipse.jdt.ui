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
import java.util.List;

public class A {
	void foo() {
		List l= new ArrayList();
		l.add("Eclipse");
		l.add(new Integer(10));
		bar(l);
	}
	void bar(List l) {
		l.add(new A());
	}
}