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

	public abstract T f();
}
abstract class B<S> extends A<String>{

	public abstract String f();
}
class C extends A<Object>{
	public Object f(){return null;}
}