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
//9, 18 -> 9, 25   AllowLoadtime == true
package p;

class R {
	int r;

	static class S extends R {
		String f() {
			fish(super.r);
			return null;	
		}
		static void fish(int parm) {
		}
	}	
}