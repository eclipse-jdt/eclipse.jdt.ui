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
package p2;

import p3.N1;
import p3.N1.N2;
import p3.N1.N2.N3;

public class B {

	public void m() {
		N3 anN3= new N1().new N2().new N3();
	}
}