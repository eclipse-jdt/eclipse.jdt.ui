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
package duplicates_in;

public class A_test953 {
	void foo() {
		int i =10;
		/*[*/bar(i);/*]*/
		
		int j= 10;
		bar(j);
	}

	void bar(int x) {
	}
}
