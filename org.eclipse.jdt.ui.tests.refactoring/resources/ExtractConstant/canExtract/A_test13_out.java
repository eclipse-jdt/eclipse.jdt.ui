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
//9, 16 -> 9, 28   AllowLoadtime == true
package p;

class S {
	public static S instance= new S();
	private static final int CONSTANT= instance.f();
	
	int f(){

		int v= CONSTANT;
		return 0;	
	}
}