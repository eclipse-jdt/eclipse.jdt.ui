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
//6, 26 -> 6, 26  replaceAll == false, removeDeclaration == false
package p;

class Test {
	Runnable getExecutor() {
		return new Runnable() {
			public void run() { }
		};
	}
}

class Runnables {
	public static final Runnable DO_NOTHING= new Runnable() {
		public void run() { }
	};
}
