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
package try_out;

public class A_test458 {
	public void foo() throws Throwable{
		try{
			new A_test458();
		} catch (Throwable t){
			extracted(t);
		}
	}

	protected void extracted(Throwable t) throws Throwable {
		/*[*/throw t;/*]*/
	}
}
