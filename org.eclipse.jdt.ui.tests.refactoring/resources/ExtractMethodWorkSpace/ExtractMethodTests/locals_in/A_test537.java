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
package locals_in;

public class A_test537 {
	public void foo() {
		final int i= 10;
		
		/*[*/Runnable run= new Runnable() {
			public void run() {
				System.out.println("" + i);
			}
		};/*]*/
		
		run.run();
	}
}
