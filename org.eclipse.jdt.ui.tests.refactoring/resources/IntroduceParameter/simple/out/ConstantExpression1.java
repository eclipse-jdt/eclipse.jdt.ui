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
package simple.out;

public class ConstantExpression1 {
	public static final int ZERO= -1;
	public int m(int a, int second) {
		int b= second;
		return m(3 * a, ZERO - 2);
	}
	public void use() {
		m(17, ZERO - 2);
		m(17 * m(18, ZERO - 2), ZERO - 2);
	}
}
