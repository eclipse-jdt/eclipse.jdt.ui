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
package nameconflict_in;

public class TestSameTypeInSibling {
	public void main() {
		class T {
			public T() {}
		}
		int x= 10;
		/*]*/foo();/*[*/
	}
	
	public void foo() {
		class T {
			T t;
			public T() {}
		}
		class X {
			T t;
			void foo() {
				int x;
				T t;
			}
		}
	}
}
