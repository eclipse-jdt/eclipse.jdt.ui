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
package p;//9, 20 - 9, 23


class A  {
	void f() {
		String x;
		String temp= "i";
		if (true)
			try{
				x= temp;
			} catch (Exception e){
				x= temp;
			}
	}
}