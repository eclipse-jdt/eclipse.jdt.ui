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

import p.ATest.ATestI;
import p.ATest.ATestI.ATestIIb;
import p.A.ARunner;

public class B {
	public class BRunner implements Runnable {
		public void run() {
			(new ATestI()).new ATestII();
			new ATestIIb();
		}
	}
	ARunner ar;
}
