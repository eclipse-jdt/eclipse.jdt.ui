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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class A {
	void sets() {
		Map map= new HashMap();
		map.put("key", new Integer(17));
		Iterator iter= map.entrySet().iterator();
		Map.Entry entry= (Map.Entry) iter.next();
	}
}
