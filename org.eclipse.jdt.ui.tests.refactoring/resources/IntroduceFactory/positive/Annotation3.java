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

public class Annotation3_in {
	public void foo() {
		Cell3 c= Cell3.createCell3();
	}
}
@interface Authorship {
	String name();
	String purpose();
}
class Cell3 {
	public static Cell3 createCell3() {
		return new Cell3();
	}

	private @Authorship(
		name="Rene Descartes",
		purpose="None whatsoever") /*[*/Cell3/*]*/() { }
}
