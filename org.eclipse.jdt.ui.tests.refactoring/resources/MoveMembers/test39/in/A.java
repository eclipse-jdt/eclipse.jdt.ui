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

import other.C; //(-import)

public class A {
	Inner i; //+import r.B.Inner
	A.Inner ii; //+import r.B
	p.A.Inner iii;
	public static int a;
	public static class Inner { //move to r.B
		Inner buddy;
		public Inner(A.Inner other) {
					// ^ is direct access to enclosing type
			buddy= C.ii; //+import other.C
			int ia= a;
		}
	}
}
