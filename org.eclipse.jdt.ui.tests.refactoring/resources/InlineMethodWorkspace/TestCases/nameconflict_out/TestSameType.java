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
package nameconflict_out;

public class TestSameType {
	public void main() {
		class T {
			public T() {}
		}
		class T1 {
			T1 t;
			public T1() {}
		}
	}
	
	public void foo() {
		class T {
			T t;
			public T() {}
		}
	}
}
