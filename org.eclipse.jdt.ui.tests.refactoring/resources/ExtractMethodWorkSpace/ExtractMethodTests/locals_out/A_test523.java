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

public class A_test523 {
	public volatile boolean flag;

	protected void foo() {
		int i= 0;
		i = extracted(i);
		read(i);
	}

	protected int extracted(int i) {
		/*[*/try {
			if (flag)
				throw new Exception();
			i= 10;
		} catch (Exception e) {
		}/*]*/
		return i;
	}

	private void read(int i) {
	}
}
