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
class Example {
	public static final Example A = new Example("A1", "A2");
	public static final Example B = Example.getExample("B2", "B1");
	
	public static final Example C;    
	public static final Example D;
	static {
		C = new Example("C1", "C2");
		D = Example.getExample("D2", "D1");
	}
	
	public Example(String arg1, String arg2) {

	}
	
	public static Example getExample(String b, String a) {
		return new Example(a, b);
	}
}
