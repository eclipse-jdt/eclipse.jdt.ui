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

public class A {
	private void foo() {
		X idealEndPos[][] = null;
		X ideal[] = null;
		ideal[2] = (false
				? idealEndPos[3][2]
								 : idealEndPos[2][1]);
		int j = ideal.length;
	}
}
