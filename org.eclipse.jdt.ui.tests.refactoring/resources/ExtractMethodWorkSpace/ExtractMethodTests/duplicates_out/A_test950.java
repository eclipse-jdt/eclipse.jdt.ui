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

public class A_test950 {
	void f(){
		int i= 0;
		int j= 1;
		int k= extracted(i, j);

		int i1= 0;
		int j1= 1;
		int k1= extracted(i, j);
	}

	protected int extracted(int i, int j) {
		return /*[*/i+j/*]*/;
	}
}
