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
	void k();
}
interface I2{
	void k();
}
class A{
public void k(){}
}
class B extends A implements I{
public void k(){}
}
class C extends A implements I2{
public void k(){}
}