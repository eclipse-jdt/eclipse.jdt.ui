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

public class TestOneRead {
	public void main() {
		/*]*/foo(value());/*[*/
	}
	
	public void foo(int x) {
		int i= x;
	}
	
	public int value() {
		return 10;
	}
}
