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
//11, 20 -> 11, 26   AllowLoadtime == true
package p;

class R {
	static int rG() {
		return 2;
	}

	static class S extends R {
		private static final int CONSTANT= R.rG();

		int f(){
			int d= CONSTANT;
			return d;
		}
	}
}