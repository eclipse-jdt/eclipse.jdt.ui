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


enum A {
	ONE(1), TWO(2), THREE(3)
	;
	
	int fOrdinal;
	private A(int value) {
		fOrdinal= value;
	}
}

class U {
	int one= A.ONE.fOrdinal;
}