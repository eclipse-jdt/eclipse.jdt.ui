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
import java.util.Hashtable;

class A_testUpdateNotPossible_in {
	public void foo {
		Hashtable h1 = new Hashtable();
		Hashtable h2 = new Hashtable();
		h1 = h2;
		h2 = h1;
	}
}
