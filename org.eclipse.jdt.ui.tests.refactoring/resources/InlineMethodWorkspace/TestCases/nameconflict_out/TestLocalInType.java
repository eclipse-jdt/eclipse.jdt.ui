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

public class TestLocalInType {
	public void main() {
		int x= 10;
		int bar= 20;
		class T {
			void bar() {
				int x;
			}
		}
	}
	
	public void foo() {
		int x= 10;
		int bar= 20;
	}
}
