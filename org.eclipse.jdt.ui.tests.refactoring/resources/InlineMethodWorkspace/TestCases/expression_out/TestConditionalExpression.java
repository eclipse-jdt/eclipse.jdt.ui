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
package expression_out;

public class TestConditionalExpression {
	int i(Object s, int k) {
		return k == 3 ? s.hashCode() : 3;
	}
	void f(int p) {
		int u = (p == 3 ? this.hashCode() : 3);
	}
}