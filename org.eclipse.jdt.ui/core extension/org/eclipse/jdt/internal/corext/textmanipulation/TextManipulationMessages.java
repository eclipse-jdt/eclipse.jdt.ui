/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.textmanipulation;

import org.eclipse.osgi.util.NLS;

/**
 * @deprecated Use file buffers instead
 */
public final class TextManipulationMessages extends NLS {

	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.corext.textmanipulation.Messages";//$NON-NLS-1$

	private TextManipulationMessages() {
		// Do not instantiate
	}

	public static String TextBuffer_wrongRange;
	public static String TextBufferFactory_bufferNotManaged;

	static {
		NLS.initializeMessages(BUNDLE_NAME, TextManipulationMessages.class);
	}
}