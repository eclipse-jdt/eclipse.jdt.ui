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

public class A_test724 {
	protected void foo() {
		int i= 0;
		int j= 1;
		switch (j) {
			case 1 :
				i = extracted();
				break;
			default :
				i= -1;
				break;
		}
		System.out.println(i);
	}

	protected int extracted() {
		int i;
		/*[*/
		i= 1;/*]*/
		return i;
	}
}
