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
//selection: 12, 16, 12, 52
//name: i -> length
package simple.out;

import java.util.Vector;

public class ConstantExpression2 {
	private Vector fBeginners;
	private Vector fAdvanced;
	
	private int count(int length) {
		return length;
	}
	public void use() {
		count(fBeginners.size() + fAdvanced.size());
	}
}
