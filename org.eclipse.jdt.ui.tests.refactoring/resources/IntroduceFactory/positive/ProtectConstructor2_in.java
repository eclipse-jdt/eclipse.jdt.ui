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

public class ProtectConstructor2_in {
	private int		fX, fY;

	public /*[*/ProtectConstructor2_in/*]*/(int x, int y) {
		fX= x;
		fY= y;
	}
	public int getX() {
		return fX;
	}
	public int getY() {
		return fY;
	}
	public static void main(String[] args) {
		int						y=  20;
		ProtectConstructor2_in	pc= new ProtectConstructor2_in(15, y);

		System.out.println("Value = " + Integer.toHexString(pc.getX() + pc.getY()));
	}
}
