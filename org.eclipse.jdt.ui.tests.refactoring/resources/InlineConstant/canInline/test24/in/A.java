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
//14, 17, 14, 17  replaceAll == true, removeDeclaration == true
package p;

import static q.Consts.III;
import q.Consts;

public class A {
	public static final int getCount() { return 42; }
	public static final int getCount2() { return 42; }
	
	int getIII() {
		int i= q.Consts.III;
		int ii= Consts.III;
		return III + i + ii;
	}
}
