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
public class A{
	public static A F;
	public static void m(){
		F= null;
		new A().F= null;
		new A().i().F= null;
		new A().i().i().F= null;
		F.F= null;
		F.F.F= null;
	}
	A i(){
		return this;
	}
}