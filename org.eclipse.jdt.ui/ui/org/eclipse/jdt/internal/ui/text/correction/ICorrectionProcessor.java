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
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

/**
  */
public interface ICorrectionProcessor {
	
	/**
	 * Collects corrections or code manipulations for the given context
	 * @param context Defines current compilation unit, position and -if at a problem location- problem ID.
	 * @param resultingCollections 
	 */
	
	void process(ICorrectionContext context, List resultingCollections) throws CoreException;
	
}
