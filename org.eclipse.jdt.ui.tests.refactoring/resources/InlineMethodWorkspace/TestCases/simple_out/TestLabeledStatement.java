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
package simple_out;

public class TestLabeledStatement {

	public static void main() {
		the_label:
		while(true) {
			break the_label;
		}
	}

	public static void foo() {
		the_label:
		while(true) {
			break the_label;
		}
	}
}
