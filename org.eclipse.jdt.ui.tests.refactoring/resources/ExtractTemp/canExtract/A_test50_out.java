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
	void f(boolean flag){
		for (int i= 0; i < 10; i++) {
			boolean temp= i==1;
			f(temp);
		}
		for (int i= 0; i < 10; i++) {
			f(i==1);
		}
	}
}