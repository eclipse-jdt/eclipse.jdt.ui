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
//16, 18, 16, 21
package p;

import static p.Color.RED;

enum Color {
	RED, BLUE(), YELLOW() {};
	public static final Color fColor= RED;
}

class ColorUser {
	void use() {
		Color c= Color.fColor;
		c= RED;
		switch (c) {
			case RED : //extract constant "RED"
				break;
			default :
				break;
		}
	}
}