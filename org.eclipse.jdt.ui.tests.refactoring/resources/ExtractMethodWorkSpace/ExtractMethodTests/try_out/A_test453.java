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

public class A_test453 {

	public void foo() {
		extracted();
	}

	protected void extracted() {
		/*[*/try {
			g();
		} catch (Exception e) {
		}/*]*/
	}

	public void g() throws IOException {
	}
}
