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

public class A_test962 {
	private Object object;
	public A_test962() {
	  this.object = new Object();
	  System.out.println(extracted());
	}
	protected Object extracted() {
		return /*[*/this.object/*]*/;
	}
}
