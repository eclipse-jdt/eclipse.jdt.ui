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

public class A_test538 {
	public void foo() {
		int i= 0;
		int[] array= new int[10];
		
		extracted(i, array);
	}

	protected void extracted(int i, int[] array) {
		/*[*/array[i]= 10;/*]*/
	}
}
