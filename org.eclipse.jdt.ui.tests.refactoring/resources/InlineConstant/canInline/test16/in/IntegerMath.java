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
//8, 23 -> 8, 23  replaceAll == true, removeDeclaration == true
package p;

class IntegerMath<E> {
	/**
	 * This is {@link #PI}
	 */
	static final int PI= 3;
	/**
	 * This uses {@link #PI}
	 */
	int getCircumference(int radius) {
		return 2 * radius * PI/*.14159265*/;
	}
}

/**
 * @see IntegerMath#PI
 */
class Test {
	int c= new IntegerMath<String>().getCircumference(IntegerMath.PI);
}