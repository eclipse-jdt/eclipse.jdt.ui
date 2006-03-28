/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.model;

public class TestRoot extends TestSuiteElement {

	public TestRoot() {
		super(null, "-1", "TESTROOT", 1); //$NON-NLS-1$//$NON-NLS-2$
	}

	public TestRoot getRoot() {
		return this;
	}
}
