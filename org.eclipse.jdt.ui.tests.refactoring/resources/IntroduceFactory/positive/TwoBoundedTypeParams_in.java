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

public class TwoBoundedTypeParams_in {
	public void foo() {
		Complex<Integer,Integer> c1= new Complex<Integer,Integer>(0,-1);
		Complex<Float,Float> c2= new Complex<Float,Float>(0.0F, 3.14F);
	}
}
class Complex<TX extends Number, TY extends Number> {
	TX fLeft;
	TY fRight;
	public /*[*/Complex/*]*/(TX x, TY y) {
		fLeft= x;
		fRight= y;
	}
}
