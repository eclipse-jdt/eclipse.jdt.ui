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
package expression_in;

public class A_test608 {

	public static class Scanner {
		public int x;
		public int y;
	}
	public static class Selection {
		public int start;
		public int end;
	}

	public void foo(Selection selection) {
		Scanner scanner= new Scanner();
		
		if (/*[*/scanner.x < selection.start && selection.start < scanner.y/*]*/) {
			g();
		}
	}

	public void g() {
	}
}
