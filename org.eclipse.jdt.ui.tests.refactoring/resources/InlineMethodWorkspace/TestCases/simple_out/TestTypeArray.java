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
package simple_out;

public class TestTypeArray {
	public void main() {
		TestTypeArray[][] x = null;
		x= new TestTypeArray[10][];
	}
	
	public void foo(TestTypeArray[][] x) {
		x= new TestTypeArray[10][];
	}
}
