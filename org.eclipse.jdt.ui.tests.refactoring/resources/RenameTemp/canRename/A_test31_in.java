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
//renaming to kk, j
package p;
class A{
	private void m(){
		final int /*[*/i/*]*/= 0;
		int j= 0;
		new Object(){
			int kk;
			void fred(){
				kk= 0;
			}
		};
	}
}