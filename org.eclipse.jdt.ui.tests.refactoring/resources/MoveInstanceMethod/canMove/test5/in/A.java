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
// Move mA1 to field fB, unqualified static member references are qualified
package p1;

import p2.B;

public class A {
	public static String fgHello= "Hello from A!";
	
	public B fB;
	
	public static void talk(B b) {
		System.out.println("How are you?");
	}
	
	public void mA1() {
		System.out.println(fgHello);
		talk(fB);
		System.out.println(this);
	}
}