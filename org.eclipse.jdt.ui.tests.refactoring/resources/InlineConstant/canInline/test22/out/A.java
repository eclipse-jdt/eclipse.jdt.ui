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
//11, 16 -> 11, 19  replaceAll == true, removeDeclaration == true
package p;

import static p.A.getCount2;
import static q.Consts.I;
import static r.Third.B;
import r.Third;

public class A {
	public static final int getCount() { return 42; }
	public static final int getCount2() { return 42; }
	
	int getIII() {
		return I + A.getCount() + getCount2() + Third.A + B;
	}
}
