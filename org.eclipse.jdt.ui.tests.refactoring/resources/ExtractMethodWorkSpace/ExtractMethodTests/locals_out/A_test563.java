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

public class A_test563 {
	public boolean flag;
	public void foo() {
		int x= 0;
		do {
			int y= x;
			extracted();
		} while ((x= 20) < 10);
	}
	protected void extracted() {
		int x;
		/*[*/x= 20;/*]*/
	}
}

