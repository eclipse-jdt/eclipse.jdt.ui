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
//renaming I.m to k
interface I{
	void m();
}
interface I2{
	void m();
}
class A{
public void m(){}
}
class B extends A implements I{
public void m(){}
}
class C extends A implements I2{
public void m(){}
}