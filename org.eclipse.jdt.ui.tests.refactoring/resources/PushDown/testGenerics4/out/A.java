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
abstract class A<T>{
	public abstract T m();
}
class B extends A<String>{

	public String m() {return null;}
}
class B1 extends B{
}
class C extends A<String>{

	public String m() {return null;}
}