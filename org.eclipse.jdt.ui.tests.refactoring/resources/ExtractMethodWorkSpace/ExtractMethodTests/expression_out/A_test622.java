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
package expression_out;

public class A_test622 {
	public A_test622(int i) {
	}
	static class Inner extends A_test622 {
		public Inner() {
			super(extracted());
		}

		protected static int extracted() {
			return /*[*/5 + 6/*]*/;
		}
	}
}
