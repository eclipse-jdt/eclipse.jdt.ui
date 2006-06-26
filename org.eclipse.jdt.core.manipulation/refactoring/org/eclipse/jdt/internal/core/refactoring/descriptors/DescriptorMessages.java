/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.refactoring.descriptors;

import org.eclipse.osgi.util.NLS;

public class DescriptorMessages extends NLS {

	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.core.refactoring.descriptors.DescriptorMessages"; //$NON-NLS-1$

	public static String JavaRefactoringDescriptor_no_resulting_descriptor;

	public static String JavaRefactoringDescriptor_not_available;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, DescriptorMessages.class);
	}

	private DescriptorMessages() {
	}
}