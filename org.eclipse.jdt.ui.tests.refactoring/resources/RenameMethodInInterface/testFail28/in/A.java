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
//can't rename I.m to k
package p;
interface I{
void m();
}
class A1 implements I, I1{
public void m(){}
}
interface I1{
void m();
}
class A2 implements I1, I2{
public void m(){}
}
interface I2{
void m();
}
class A3 implements I3, I2{
public void m(){}
public void k(){}
}
interface I3{
void m();
void k();
}