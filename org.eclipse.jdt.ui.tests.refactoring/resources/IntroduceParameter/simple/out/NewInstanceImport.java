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
//selection: 8, 20, 8, 46
package simple.out;

import java.util.ArrayList;
import java.util.Iterator;

public class NewInstanceImport {
	public void m(int a, Iterator iter) {
		boolean b= iter.hasNext();
	}
	public void use() {
		m(17, new ArrayList().iterator());
	}
}
