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
package simple_in;

public class TestTwoCalls {
	public void main() {
		toInline(10);
		baz();
		toInline(10);
	}
	void toInline(int i) {
		i= 10;
		bar();
		bar();
	}
	void baz() {
	}
	void bar() {
	}
}
