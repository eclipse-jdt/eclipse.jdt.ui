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

public class DontMoveArgs1_in {
	private int		fN;

	public /*[*/DontMoveArgs1_in/*]*/(int N) {
		fN= N;
	}
	public int getN() {
		return fN;
	}
	public static void main(String[] args) {
		DontMoveArgs1_in	dma= new DontMoveArgs1_in(15);

		System.out.println("Value = " + Integer.toHexString(dma.getN()));
	}
}
