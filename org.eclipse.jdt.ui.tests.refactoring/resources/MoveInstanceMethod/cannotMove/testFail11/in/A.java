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
// Move mA1 to parameter b, do not inline delegator
package p1;

import p2.B;

public class A {
	public void mA1(B b, int a) {
		mA2();
	}
	
	public void mA2() {}
}