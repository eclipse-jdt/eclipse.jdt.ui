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
//cannot rename i to j
package p;
class A{
	void m(){
		final int /*[*/i/*]*/= 0;
		new A(){
			void f(){
				int j= 0;
				int i2= i;
			}
		};
	};
}