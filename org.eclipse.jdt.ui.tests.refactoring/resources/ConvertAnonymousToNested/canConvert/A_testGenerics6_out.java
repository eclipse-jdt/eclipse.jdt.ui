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
//private, static, final
class A<T>{
	private static final class Inner<S, T> extends A<S> {
		T t;
	}
	A(){
	}
	<S> void f(){
		new Inner<S, T>();
	}
}
