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
package wiki_in;

public class A_test2003 {

	int field= 0;

	void fun() {
		int i= 0;
		/*[*/
		if (field == 1) {
			i= 1;
			System.out.println("i, field == " + i + ", " + field);
		} else {
			System.out.println("i, field == " + i + ", " + field);
		}
		/*]*/
	}
}