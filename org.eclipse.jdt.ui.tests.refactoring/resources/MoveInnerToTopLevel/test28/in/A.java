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
class A extends B
{
	private int foo() {return 0};
	public class Inner
	{
		Inner() {
			int f= foo();
			int g= bar();
		}
	}
	
	public A()
	{
		super();
		new A.Inner();
	}
}
class B {
	protected int bar() {return 0};
}