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
package locals_in;

public class A_test536 {

	public void bar() {
		{ int k= 27; k++; }
		int i= 37;
		if (i == 0) {
			int k= 17;
			/*[*/k= k + 1;
			k += 2;
			i += 2;
			k++;/*]*/
		}
		i++;
	}
}
