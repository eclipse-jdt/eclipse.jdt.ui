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


/** typecomment template*/
interface I {

	/** method m javadoc comment */
	public abstract void m();

	/** field I javadoc comment */
	int I= 9;

	/* method m1 regular comment */
	public abstract void m1();

	/* field i1 regular comment */
	int I1= 9;

	// method m2 line comment
	public abstract void m2();

	// field i2 line comment
	int I2= 9;

	public abstract void m4(); /* method m4 regular comment */

	int I4= 9; /* field i4 regular comment */

	public abstract void m5(); // method m5 line comment

	int I5= 9; // field i5 line comment

}