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

public class WildcardParam_in {
	public void foo() {
		NumberCell<Integer> c1= new NumberCell<Integer>(3);
		NumberCell<Integer> c1a= new NumberCell<Integer>(c1);
		NumberCell<Float> c2= new NumberCell<Float>(3.14F);
		NumberCell<Float> c2a= new NumberCell<Float>(c2);
	}
}
class NumberCell<T extends Number> {
	T fNum;
	public NumberCell(T n) {
		fNum= n;
	}
	public /*[*/NumberCell/*]*/(NumberCell<? extends T> other) {
		fNum= other.fNum;
	}
}
