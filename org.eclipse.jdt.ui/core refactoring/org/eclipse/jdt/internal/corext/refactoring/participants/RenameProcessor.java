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

import org.eclipse.jdt.internal.corext.Assert;

public abstract class RenameProcessor implements IRenameProcessor {

	private int fStyle;
	protected String fNewElementName;
	
	public int getStyle() {
		return fStyle;
	}
	
	protected RenameProcessor() {
		fStyle= RefactoringStyles.NEEDS_PREVIEW;	
	}
	
	protected RenameProcessor(int style) {
		fStyle= style;	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IRenameProcessor#setNewElementName(java.lang.String)
	 */
	public void setNewElementName(String newName) {
		Assert.isNotNull(newName);
		fNewElementName= newName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IRenameProcessor#getNewElementName()
	 */
	public String getNewElementName() {
		return fNewElementName;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IRefactoringProcessor#getDerivedElements()
	 */
	public Object[] getDerivedElements() throws CoreException {
		return new Object[0];
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IRenameProcessor#propagateNewElementNameTo(org.eclipse.jdt.internal.corext.refactoring.participants.IRenameParticipant)
	 */
	public void propagateNewElementNameTo(IRenameParticipant participant) {
		participant.setNewElementName(fNewElementName);
	}
}
