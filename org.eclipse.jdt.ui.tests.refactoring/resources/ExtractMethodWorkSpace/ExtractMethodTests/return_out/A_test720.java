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
package return_out;

public class A_test720 {
	private boolean flag;
	public boolean foo() {
		if (flag) {
			return extracted();
		}
		return true;
	}
	protected boolean extracted() {
		/*[*/try {
			foo();
		} catch(Exception e) {
		}
		return false;/*]*/
	}
}

