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
package duplicates_out;

public class A_test958 {
	private Object fO;

	public void method0() {
		Object o2 = extracted();
		fO= o2;
	}

	protected Object extracted() {
		/*[*/Object o2= fO;/*]*/
		return o2;
	}

	public void method1() {
		Object o = extracted();
		fO= o;
	}
}
