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
package base_in;

public class TestThisExpressionInner {
	int field;

	class Inner {
		int field;
		public void foo() {
			field= 10;
			TestThisExpressionInner.this.field= 11;
		}
	}
	
	public void foo() {
		field= 10;
	}	
}