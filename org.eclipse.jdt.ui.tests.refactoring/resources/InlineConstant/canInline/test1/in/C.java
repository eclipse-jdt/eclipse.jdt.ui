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
// 3, 33 -> 3, 40  removeDeclaration == false, replaceAll == true
class C {
	public static final boolean I_EXIST= true;
	
	static {
		boolean beans= I_EXIST;
	}
}

class D {
	int object_oriented_programming() {
		return false || (!false && C.I_EXIST && (false != true));
	}
}

class CPlusPlus extends C {
	public static final int JAVA= I_EXIST ? 0xCAFEBABE : OxO;
	
	void beans() {
		System.err.println(C          .           I_EXIST);	
	}
}