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

public class A_test541 {
	class Inner {
		public int x;
	}
	public void foo() {
		int[] array= new int[10];
		Inner inner= new Inner();
		
		extracted(array, inner);
	}
	protected void extracted(int[] array, Inner inner) {
		/*[*/array[inner.x]= 10;
		inner.x= 20;/*]*/
	}
}
