/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.participants;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;


public interface IRenameProcessor extends IRefactoringProcessor {

	public String getCurrentElementName();
	
	public RefactoringStatus checkNewElementName(String newName) throws CoreException;
	
	public void setNewElementName(String newName);
	
	public String getNewElementName();
	
	/**
	 * Returns the new element. This method must only return a
	 * valid result iff the refactoring as already been performed.
	 * Otherwise <code>null<code> can be returned.
	 * 
	 * @return the new element 
	 */
	public Object getNewElement() throws CoreException;
	
	public void propagateDataTo(IRenameParticipant participant)throws CoreException;	
}
