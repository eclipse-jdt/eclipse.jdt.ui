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

class A {
	public static void exec() {
        ArrayList v1= new ArrayList();
        ArrayList v2= new ArrayList();
        v2.add("");
        Iterator iterator1 = v1.iterator();
        Iterator iterator2 = v2.iterator();
	}
}
