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
//selection: 10, 28, 10, 37
//name: ship -> ship
package simple;

public class Javadoc2 {
	/**
	 * Run it.
	 * @param ship TODO
	 */
	public void go(float ship) {
		System.out.println(ship);
	}
	private float getShip() {
		return 3.0f;
	}
}