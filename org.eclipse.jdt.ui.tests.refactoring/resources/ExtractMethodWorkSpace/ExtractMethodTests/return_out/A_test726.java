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
package return_out;

public class A_test726 {

	boolean flag;

	protected void foo() {
		int j= 0;
		for(int i= 0; i < 10; i++) {
			if (flag) {
				j = extracted();
			} else {
				read(j);
			}
		}
	}

	protected int extracted() {
		int j;
		/*[*/j= 10;/*]*/
		return j;
	}

	private void read(int i) {
	}
}
