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
//10, 21 -> 10, 21  replaceAll == true, removeDeclaration == true
package p;

class Test {
	private enum Color {
		PINK, YELLOW;
		final static Color CORPORATE_COLOR= Color.PINK;
	}
	private enum Box {
		FIRST(Color.CORPORATE_COLOR);
		public Box(Color c) {}
	}
	Color c= Color.CORPORATE_COLOR;
}
