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
package p1;
import p.A;
public class A1 {
	static void f(){
		A.Inner i;
		A.Inner.foo();
		A.Inner.t =  2;
		p.A.Inner.foo();
		p.A.Inner.t =  2;
		A a;
	}

}