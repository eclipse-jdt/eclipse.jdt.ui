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

/**
  */
public interface ICorrectionContext extends IAssistContext {
	
	/**
	 * Returns the id of problem that is associated with this context. See {@link org.eclipse.jdt.core.compiler.IProblem} for
	 * id definitions
	 * @return int
	 */
	int getProblemId();
	
	/**
	 * Returns the problem argument is associated with this context. <code>null</code> is returned
	 * if no problem exists. 
	 * @return String[]
	 */
	String[] getProblemArguments();	

}
