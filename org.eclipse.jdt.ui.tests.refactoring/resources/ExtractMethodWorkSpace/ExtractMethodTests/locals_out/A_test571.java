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
package locals_out;

public class A_test571 {

	public void foo() {
		int i= 0;
		for(Object element: getArray(i++)) {
			extracted();
		}
	}

	protected void extracted() {
		int i;
		/*[*/i= 10;/*]*/
	}
	
	private Object[] getArray(int i) {
		return null;
	}
}

