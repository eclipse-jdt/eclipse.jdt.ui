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

public class TestTwoCalls {
	public void main() {
		int i = 10;
		i= 10;
		bar();
		bar();
		baz();
		int i1 = 10;
		i1= 10;
		bar();
		bar();
	}
	void baz() {
	}
	void bar() {
	}
}
