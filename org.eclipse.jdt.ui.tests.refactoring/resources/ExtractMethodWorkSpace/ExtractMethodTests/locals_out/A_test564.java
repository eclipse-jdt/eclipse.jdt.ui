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

public class A_test564 {
	void foo(final int out){
		int i;
		if (out > 5){
			i = extracted();
		} else {
			i= 2;
		}
		i++;
	}

	protected int extracted() {
		int i;
		/*[*/i= 1;/*]*/
		return i;
	}
}
