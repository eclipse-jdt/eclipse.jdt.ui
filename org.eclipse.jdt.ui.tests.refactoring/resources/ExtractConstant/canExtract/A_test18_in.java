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
//5, 16 -> 5, 17   AllowLoadtime == false,  qualifyReferencesWithClassName= true
package p;
class ClassName {
	int f() {
		return 0;
	}
	
	class Nested {
		{
			System.out.println(0);	
		}
		
		void f() {
			int i= 0;
		}
	}
}