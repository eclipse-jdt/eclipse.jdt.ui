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
//selection: 7, 17, 7, 19
package invalid;

class PartName1 {
	public static int foo() { return 17; }
	void bar() {
		int a= foo();
	}
	void use() {
		bar();
	}
}
