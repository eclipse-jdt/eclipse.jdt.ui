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

public class A_testInterfaceTypes_in {
	void foo(){
		B b = new C();
		b.toString();
	}
}

interface I { }
class A implements I { }
class B extends A implements I { }
class C extends B implements I { }
