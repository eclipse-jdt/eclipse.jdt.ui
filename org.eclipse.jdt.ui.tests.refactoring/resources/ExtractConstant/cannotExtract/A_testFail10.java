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
//15, 20 -> 15, 37   AllowLoadtime == false
package p;

class R {
	static R instance() {
		return null;
	}
	
	int rF() {
		return 1;	
	}
	
	static class S extends R {
		int f(){
			int d= R.instance().rF();		
			return 0;	
		}
	}
}