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
package invalidSelection;

public class A_test142 {
	private boolean flag;
	public int foo() {
		int i= 10;
		/*]*/if (flag) {
			i++;
			return i;
		}/*[*/
		int y= i + 10;
		return y;
	}
}