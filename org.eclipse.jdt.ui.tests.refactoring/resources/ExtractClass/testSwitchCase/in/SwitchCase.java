/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package p;

public class SwitchCase {
	private static final int TEST2 = 6;
	public final int TEST = TEST2;
	public void foo(){
		int test=5;
		switch (test) {
		case TEST:
			break;
		default:
			break;
		}
	}
}