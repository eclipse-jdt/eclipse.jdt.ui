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
class A{
public void m(){};
}
class B1 extends A implements J{
		public void k(){}
}
class B2 extends A implements I{

}
interface J{
void m();
void k();
}