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
package base_out;

public class TestThisExpression {
	private int field;
	
	public void foo() {
		this.setField(10);
		new TestThisExpression().setField(11);
	}

	void setField(int field) {
		this.field = field;
	}

	int getField() {
		return field;
	}
}