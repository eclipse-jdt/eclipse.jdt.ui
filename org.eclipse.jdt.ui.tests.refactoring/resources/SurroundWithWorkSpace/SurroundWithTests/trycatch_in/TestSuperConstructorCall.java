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
package trycatch_in;

import java.net.MalformedURLException;

public class TestSuperConstructorCall {
	static class A {
		public A(int i) throws MalformedURLException {
		}
	}
	
	static class B extends A {
		public B() {
			super(10);
		}
	}
}
