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
package trycatch_out;

import java.io.File;
import java.net.MalformedURLException;

public class TestWrappedLocal1 {

	public void foo() {
		File file= null;
		
		/*[*/int i;
		try {
			i = 10;
			file.toURL();/*]*/
		} catch (MalformedURLException e) {
		}
		i= 20;
	}

}
