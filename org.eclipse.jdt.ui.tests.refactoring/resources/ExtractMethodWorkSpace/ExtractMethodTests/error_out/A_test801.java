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
package error_out;

public class A_test801 {
	public void foo() {
		List l;
		extracted(l);
	}

	protected void extracted(List l) {
		/*[*/g(l);/*]*/
	}

	public void g(List l) {
	}
}
