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
package controlStatement_out;

public class TestForAssignmentTwo {
	public void main() {
		int x;
		for (int i= 0; i < 10; i++) {
			int x1;
			x= 20;
		}
	}
	
	public int foo() {
		int x;
		return 20;
	}
}
