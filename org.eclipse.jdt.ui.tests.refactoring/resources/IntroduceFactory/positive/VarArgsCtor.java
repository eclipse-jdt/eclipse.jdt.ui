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

public class VarArgsCtor_in {
	public void foo() {
		Cell c= Cell.createCell("", "");
	}
}
class Cell {
	public static Cell createCell(String... args) {
		return new Cell(args);
	}

	private /*[*/Cell/*]*/(String ... args) { }
}
