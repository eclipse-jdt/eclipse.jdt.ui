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
package object_out;

public class TestArrayRead {
	private Object[] field;

	public TestArrayRead() {
		setField(new Object[0]);
	}
	private void setField(Object[] field) {
		this.field = field;
	}
	private Object[] getField() {
		return field;
	}
	public void basicRun() {
		System.err.println(getField().length);
	}
}
