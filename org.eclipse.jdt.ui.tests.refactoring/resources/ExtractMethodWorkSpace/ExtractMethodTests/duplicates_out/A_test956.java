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
package duplicates_out;

// don't extract second occurence of
// 2 since it is in a inner class
public class A_test956 {
	void foo() {
		int y= extracted();
	}
	protected int extracted() {
		return /*[*/2/*]*/;
	}
	class Inner {
		void foo() {
			int y1= 2;
		}
	}	
}
