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
//13, 20 -> 13, 35   AllowLoadtime == true
package p;

class R {
	public static R instance= new R();
	
	int rF() {
		return 1;
	}
	
	static class S extends R {
		private static final int CONSTANT= R.instance.rF();

		int f(){
			int d= CONSTANT;		
			return 0;	
		}
	}
}