/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package p;

public class ComplexExtractNested {
	private int test;
	protected int test2= 5;
	private int test3, test4= 5;
	
	public void foo(){
		test3++;
		test= 5+7;
		System.out.println(test+" "+test4);
	}
}