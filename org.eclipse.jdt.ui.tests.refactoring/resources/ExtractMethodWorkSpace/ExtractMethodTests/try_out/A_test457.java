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
package try_out;

import java.io.IOException;

public class A_test457 {

	public void foo() {
		Exception[] e= new Exception[] { new IOException("Message") };
		try {
			extracted(e);
		} catch (Exception x) {
		}
	}

	protected void extracted(Exception[] e) throws Exception {
		/*[*/throw e[0];/*]*/
	}
}
