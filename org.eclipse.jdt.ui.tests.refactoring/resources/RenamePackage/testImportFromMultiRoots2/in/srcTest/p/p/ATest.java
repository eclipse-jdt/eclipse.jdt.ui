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
package p.p;

import p.p.*; //myself

public class ATest {
	A aFromOtherPackageFragment;
	p.p.A aQualifiedFromNamesake;
	
	public void test1() {
		TestHelper.log("x");
	}
}
