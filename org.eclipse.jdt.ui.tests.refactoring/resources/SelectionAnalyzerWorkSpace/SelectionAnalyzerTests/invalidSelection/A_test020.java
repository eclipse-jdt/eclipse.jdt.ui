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
package invalidSelection;

public class A_test020 {
	public void foo(int x) {
		switch(x) {
			/*]*/case 10:
				f();
				break;/*[*/
			case 11:
				g();
				break;
			default:
				f();
				g();		
		}
	}
	
	public void f() {
	}
	public void g() {
	}
}