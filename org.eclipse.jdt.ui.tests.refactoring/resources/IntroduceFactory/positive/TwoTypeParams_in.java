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

public class TwoTypeParams_in {
	public void foo() {
		Pair<String,String> p1= new Pair<String,String>("","");
		Pair<Integer,String> p2= new Pair<Integer,String>(3,"");
		Pair<Float,Object> p3= new Pair<Float,Object>(3.14F, null);
	}
}
class Pair<T1,T2> {
	T1 fLeft;
	T2 fRight;
	public /*[*/Pair/*]*/(T1 t1, T2 t2) {
		fLeft= t1;
		fRight= t2;
	}
}
