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

public class Annotation1_in {
	public void foo() {
		Cell1 c= Cell1.createCell1();
	}
}
@interface Preliminary { }
class Cell1 {
	public static Cell1 createCell1() {
		return new Cell1();
	}

	private @Preliminary /*[*/Cell1/*]*/() { }
}
