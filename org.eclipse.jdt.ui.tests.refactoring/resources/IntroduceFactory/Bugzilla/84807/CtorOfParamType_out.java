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
package p;

public class CtorOfParamType<T> {
	public static <T> CtorOfParamType<T> createCtorOfParamType(T t) {
		return new CtorOfParamType<T>(t);
	}

	private CtorOfParamType(T t) { }
}

class call {
	void foo() {
		CtorOfParamType<String> x= CtorOfParamType.createCtorOfParamType("");
	}
}