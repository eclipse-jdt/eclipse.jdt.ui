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
package argument_out;

public class TestLocalReferenceRead {
	public void main() {
		int foo = 0;
		int bar = foo;
		bar++;
		System.out.println(foo);
	}
	
	public void inlineMe(int bar) {
		bar++;
	}
}
