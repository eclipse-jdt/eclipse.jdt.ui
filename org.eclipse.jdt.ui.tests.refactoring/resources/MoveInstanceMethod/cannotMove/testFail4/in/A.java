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
package p1;

import p2.B;

public class A {
	public int foob() {
		return 1;	
	}
	
	public static Child extends A {
		public int m(B b) {
			return super.foob();
		}
		
		public int foob() {
			return 2;	
		}
	}
}