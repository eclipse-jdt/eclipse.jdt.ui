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
package semicolon_out;

public class A_test406 {
	public void foo() {
		if (extracted())
			foo();
	}

	protected boolean extracted() {
		return /*[*/1 == 10/*]*/;
	}
}