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
// class qualify referenced type name to top level, original receiver not used in method
package p1;

import p2.B;

public class Nestor {
	public static class Nestee {
		public static int fgN;
	}
	
	public void m(B b) {
		b.m();
	}
}