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

import java.awt.List;
import java.util.ArrayList;
import java.util.Hashtable;

class A_TestImport_in {
	public void foo(Hashtable table){
		table = new Hashtable();
		table.put("foo", "bar");
		List awtList = null;
		java.util.List al = new ArrayList();
	}
}
