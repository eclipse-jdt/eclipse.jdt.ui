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
package invalidSelection;

public class A_test140 {
	public boolean flag;
	public int foo() {
		int i= 10;
		/*]*/switch(i) {
			case 1:
				if (flag)
					break;
				foo();	
			case 2:
				return 10;
			default:
				throw new NullPointerException();
		}/*[*/
			
		return 10;	
	}
}