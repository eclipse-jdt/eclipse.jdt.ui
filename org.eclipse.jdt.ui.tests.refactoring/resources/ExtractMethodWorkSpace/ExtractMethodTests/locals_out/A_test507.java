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

public class A_test507 {
	public void foo() {
		int x= 0;
		
		x = extracted();
		
		int y= x;
	}

	protected int extracted() {
		int x;
		/*[*/x= 10;/*]*/
		return x;
	}	
}