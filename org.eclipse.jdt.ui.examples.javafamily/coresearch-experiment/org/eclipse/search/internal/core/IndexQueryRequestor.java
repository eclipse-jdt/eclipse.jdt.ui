/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.search.internal.core;

import org.eclipse.core.runtime.IPath;

/**
 * TODO add spec
 */
public abstract class IndexQueryRequestor {

	/**
	 * Notification of an index match found. 
	 */
	public abstract void acceptIndexMatch(char[] category, char[] key, IPath documentPath);
	
}
