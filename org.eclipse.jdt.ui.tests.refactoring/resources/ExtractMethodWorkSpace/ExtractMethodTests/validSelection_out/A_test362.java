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
package validSelection_out;

public class A_test362 {
	A_test362(int i){
	}
	void n(){
		final int y= 0;
		extracted(y);
	}
	protected void extracted(final int y) {
		/*[*/new A_test362(y){
			void f(){
				int y= 9;
			}
		};/*]*/
	}
}
