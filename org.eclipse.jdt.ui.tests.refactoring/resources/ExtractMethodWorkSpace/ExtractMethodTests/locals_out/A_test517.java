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

public class A_test517 {
	public void foo() {
		int i;
		int j = extracted();
		i= j + 10;
	}

	protected int extracted() {
		/*[*/int i;
		int j;
		j= 20;/*]*/
		return j;
	}
}