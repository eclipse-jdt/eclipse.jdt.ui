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

public class A_test534 {
	class Inner {
		public int x;
	}
	
	public void foo() {
		Inner inner= new Inner();
		
		extracted(inner);
		
		int y= inner.x;
	}

	protected void extracted(Inner inner) {
		/*[*/inner.x= 10;/*]*/
	}
}