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
// 7, 19 -> 7, 28
class BodyDeclOnSameLine {
	private final static String BAR= "c";
	private final static String FOO=  "a";  /* ambiguous */
	private static final String CONSTANT= FOO + BAR; String strange= "b"; //$NON-NLS-1$ //$NON-NLS-2$

	void m() {
		String s= CONSTANT;
	}
}