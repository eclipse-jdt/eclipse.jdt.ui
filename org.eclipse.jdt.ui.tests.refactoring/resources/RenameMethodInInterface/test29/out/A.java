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
interface I{
void k();
}
class A{
public void k(){};
}
class B1 extends A implements J{
}
class B2 extends A implements I{
}
interface J{
void k();
}
class C implements J{
public void k(){};
}