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
package a.b.c;

import p.ATest;

public class A {
	public class ARunner implements Runnable {
		public void run() {
			new ATest.ATestI();
			new ATest.ATestI.ATestIIb();
		}
	}
	B.BRunner br;
}
