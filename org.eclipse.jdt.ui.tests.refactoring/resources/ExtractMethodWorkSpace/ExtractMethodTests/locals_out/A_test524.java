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

public class A_test524 {
	public volatile boolean flag;
	
	protected void foo() {
		int i= 0;
		try {
			i = extracted();
		} catch (Exception e) {
		}
		read(i);
	}

	protected int extracted() throws Exception {
		int i;
		/*[*/if (flag)
			throw new Exception();
		i= 10;/*]*/
		return i;
	}

	private void read(int i) {
	}
}
