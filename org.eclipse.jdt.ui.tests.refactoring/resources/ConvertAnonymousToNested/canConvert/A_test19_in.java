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
class A{
	void f(){
		final int u= 8;
		new A(){
			int l= 9;
			int p0= 2, k= u, k1= k;
			int l1= l+1, p, q= p+u;
		};
	}
}