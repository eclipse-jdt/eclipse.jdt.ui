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
//selection: 9, 20, 9, 46
//name: iterator -> iter
package simple;

import java.util.ArrayList;

public class NewInstanceImport {
	public void m(int a) {
		boolean b= new ArrayList().iterator().hasNext();
	}
	public void use() {
		m(17);
	}
}
