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

import java.util.Enumeration;

public class A_test048 {
	public boolean flag;
	public void foo() {
		for (/*]*/Enumeration e= tests()/*[*/; e.hasMoreElements(); ) {
			if (flag)
				break;
		}
	}	
	public Enumeration tests() {
		return null;
	}
}