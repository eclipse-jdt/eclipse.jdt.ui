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
package p;

public class B {
	private String fName;
	
	public B(String name) {
		fName= name;
	}
		
	public String toString() {
		return fName;
	}

	/**
	 * Print
	 */
	public void print() {
		System.out.println(
			new StarDecorator() {
				public String decorate(String in) {
					return "(" + super.decorate(in) + ")";
				}
			}.decorate(toString())
		);
	}
}
