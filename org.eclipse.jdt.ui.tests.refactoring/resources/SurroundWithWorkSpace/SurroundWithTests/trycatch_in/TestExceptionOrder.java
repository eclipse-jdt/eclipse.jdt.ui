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
package trycatch_in;

public class TestExceptionOrder {
	public static class Exception_1 extends Exception {
	}
	public static class Exception_2 extends Exception_1 {
	}
	
	public void throw1() throws Exception_1 {
	}
	
	public void throw2() throws Exception_2 {
	}
	
	public void foo() {
		/*[*/throw1();
		throw2();/*]*/
	}	
}
