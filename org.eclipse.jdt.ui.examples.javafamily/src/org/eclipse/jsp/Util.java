/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jsp;

public class Util {
	
	static char[] getChars(String s) {
		int l= s.length();
		char[] cc= new char[l];
		if (l > 0)
			s.getChars(0, l, cc, 0);
		return cc;
	}

}
