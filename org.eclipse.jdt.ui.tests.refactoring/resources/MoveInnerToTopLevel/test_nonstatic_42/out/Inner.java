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

import static java.lang.Math.cos;
import p.A.Stat;

class Inner {
	static class InnerInner {
		static class InnerInnerInner {}
	}
	public void doit() {
		A.foo();
		A.fred++;
		double d= cos(0);
		new Stat();
	}
}