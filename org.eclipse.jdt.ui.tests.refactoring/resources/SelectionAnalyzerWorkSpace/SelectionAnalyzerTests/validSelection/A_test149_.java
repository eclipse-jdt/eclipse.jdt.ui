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
package validSelection;

public class A_test149_ {
	boolean flag;
	public boolean foo() {
		int i= 0;
		/*]*/switch (i) {
			case 1:
				break;
			case 2:
				return true;
			default:
				return false;
		}/*[*/
		return false;
	}
}