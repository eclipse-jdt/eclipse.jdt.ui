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
package expression_out;

public class A_test606 {

	public void foo() {
		int i= 0;
		foo();
		do {
			foo();
		} while (extracted(i));
	}

	protected boolean extracted(int i) {
		return /*[*/i <= 10/*]*/;
	}	
}
