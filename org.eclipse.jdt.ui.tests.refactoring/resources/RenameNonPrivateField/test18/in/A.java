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
class A{
	static int field;
	static {
		String doIt= "field"; //field
		doIt= "A.field"; //A.field
		doIt= "B. #field"; //B. #field
		doIt= "p.A#field"; //p.A#field
		String dont= "x.p.A#field"; //x.p.A#field
		dont= "xp.A.field"; //xp.A.field
		dont= "B.field"; //B.field
	}	
}