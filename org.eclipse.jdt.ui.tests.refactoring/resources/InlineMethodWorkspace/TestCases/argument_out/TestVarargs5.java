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
package argument_out;

public class TestVarargs5 {

	public void bar(Object... args) {
		for(Object arg: args) {
			System.out.println(arg);
		}
	}
	
	public void main() {
		String[] strings= {"Hello", "Eclipse"};
		for(Object arg: strings) {
			System.out.println(arg);
		}
	}
}
