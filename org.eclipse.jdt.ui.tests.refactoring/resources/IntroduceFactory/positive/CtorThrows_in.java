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
package p;

public class CtorThrows_in {
	private int	fValue;

	public CtorThrows_in(int x) throws IllegalArgumentException {
		if (x < 0) throw IllegalArgumentException("Bad value: " + x);
		fValue= x;
	}

	public static void main(String[] args) {
		CtorThrows_in cti= /*[*/new CtorThrows_in(3)/*]*/;
	}
}
