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
// 7, 13 -> 7, 24
package p;

class A {
	public boolean fState= false;
	public void foo() {
		if (this.fState) {
			this.fState= false;
		} else {
			this.fState= this.fState;
		}
	}
}