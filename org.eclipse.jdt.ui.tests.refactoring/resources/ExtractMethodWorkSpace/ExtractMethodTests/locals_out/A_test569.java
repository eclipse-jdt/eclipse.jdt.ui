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
package locals_out;

public class A_test569 {
	public void foo() {
		String[] args[]= null;
		
		args = extracted();
		
		args[0]= args[1];
	}

	protected String[][] extracted() {
		String[][] args;
		/*[*/args= new String[1][4];/*]*/
		return args;
	}
}

