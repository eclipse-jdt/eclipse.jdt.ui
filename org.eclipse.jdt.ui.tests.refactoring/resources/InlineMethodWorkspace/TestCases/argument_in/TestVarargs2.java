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
package argument_in;

public class TestVarargs2 {

	public void bar(int i, String... args) {
		System.out.println(args[i]);
	}
	
	public void main() {
		/*]*/bar(1, "Hello", "Eclipse");/*[*/
	}
}
