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
//use Object
class A{
	public void foo(){};
}
class Test{
	void test() throws CloneNotSupportedException, InterruptedException{
		Object a= new A();
		a.getClass();
		a.equals(null);
		a.hashCode();
		a.notify();
		a.notifyAll();
		a.toString();
		a.wait();
		a.wait(0);
		a.wait(0, 0);
	}
}