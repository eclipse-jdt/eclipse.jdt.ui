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

public class TestLiteralReferenceRead {
	public void main() {
		int i= 10;
		bar(10);
	}
	
	public void foo(int x) {
		int i= x;
		bar(x);
	}
	
	public void bar(int z) {
	}
}
