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
package varargs_in;

public class A_test1201 {
	public void foo(String... args) {
		/*[*/System.out.println(args[1]);
		System.out.println(args[2]);/*]*/
	}
}
