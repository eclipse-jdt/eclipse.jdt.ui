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

public class ProtectConstructor1_in {
	private int		fX;

	public /*[*/ProtectConstructor1_in/*]*/(int x) {
		fX= x;
	}
	public int getX() {
		return fX;
	}
	public static void main(String[] args) {
		ProtectConstructor1_in	pc= new ProtectConstructor1_in(15);

		System.out.println("Value = " + Integer.toHexString(pc.getX()));
	}
}
