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
import p.Inner;
public class A1 {
	static void f(){
		Inner i;
		Inner.foo();
		Inner.t =  2;
		p.Inner.foo();
		p.Inner.t =  2;
	}

}