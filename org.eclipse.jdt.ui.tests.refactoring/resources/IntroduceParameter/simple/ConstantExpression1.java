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
//selection: 8, 16, 8, 24
//name: i -> second
package simple;

public class ConstantExpression1 {
	public static final int ZERO= -1;
	public int m(int a) {
		int b= ZERO - 2;
		return m(3 * a);
	}
	public void use() {
		m(17);
		m(17 * m(18));
	}
}
