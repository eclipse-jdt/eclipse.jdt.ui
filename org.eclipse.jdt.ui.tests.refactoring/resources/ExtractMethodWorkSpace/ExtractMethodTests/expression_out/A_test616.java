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

import java.io.File;

public class A_test616 {
	public void foo() {
		A a= null;
		/*]*/extracted(a).getName();
	}

	protected File extracted(A a) {
		return a.getFile()/*]*/;
	}
}
