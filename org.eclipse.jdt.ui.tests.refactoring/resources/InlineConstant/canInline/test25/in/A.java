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
//5, 32, 5, 32
package p;

class A {
	private static final int CONST = 9;

	/*
	 * (non-Javadoc) comment
	 */
	public void foo() {
		final int lineNumber2= /*preserve*/CONST/*this*/;
	}
}
