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

public class TestGenericReadWrite<E extends String> {
	private E field;

	public void foo() {
		E temp = getField();
		setField(temp);
	}

	public void setField(E field) {
		this.field = field;
	}

	public E getField() {
		return field;
	}
}
