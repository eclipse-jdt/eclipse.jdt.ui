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
package locals_out;

public class A_test525 {
	private static class Exception1 extends Exception {
	}
	private static class Exception2 extends Exception {
	}
	
	public volatile boolean flag;
	
	protected void foo() {
		int i= 10;
		try {
			i = extracted();
		} catch (Exception2 e) {
		}
		read(i);
	}

	protected int extracted() throws Exception2 {
		int i;
		/*[*/try {
			if (flag)
				throw new Exception1();
			if (!flag)
				throw new Exception2();
		} catch (Exception1 e) {
		}
		i= 10;/*]*/
		return i;
	}

	private void read(int i) {
	}
}
