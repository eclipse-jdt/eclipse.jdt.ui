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
public class T10031 {
	private static Object fValue;
	
	public static void foo() {
		setValue(null);
	}

	public static void setValue(Object value) {
		fValue= value;
	}

	public static Object getValue() {
		return fValue;
	}
}
