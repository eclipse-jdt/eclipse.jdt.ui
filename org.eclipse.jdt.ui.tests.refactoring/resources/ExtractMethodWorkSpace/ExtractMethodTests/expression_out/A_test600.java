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

public class A_test600 {

	public void foo() {
		int i= 10;
		if (extracted(i))
			foo();
	}

	protected boolean extracted(int i) {
		return /*[*/i < 10/*]*/;
	}	
}
