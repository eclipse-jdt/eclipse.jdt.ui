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
public class E {
	boolean flag;
	public void foo() {
		int i= 0;
		if (flag) {
			i= 1;
		} else {
			i= 2;
		}
		System.out.println(i);
	}
}
