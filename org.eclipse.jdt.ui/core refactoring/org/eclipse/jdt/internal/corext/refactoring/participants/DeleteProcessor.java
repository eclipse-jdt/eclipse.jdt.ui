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

public abstract class DeleteProcessor implements IRefactoringProcessor {

	private static class DeleteData implements IDeleteData {
	}

	protected static final IRefactoringParticipant[] EMPTY_PARTICIPANT_ARRAY= new IRefactoringParticipant[0];
	protected static final Object[] EMPTY_OBJECT_ARRAY= new Object[0];

	private SharableParticipants fSharedParticipants= new SharableParticipants();

	public final IDeleteParticipant[] getElementParticipants() throws CoreException {
		return DeleteExtensionManager.getParticipants(this, getElements(), fSharedParticipants);	
	}
	
	public final IDeleteParticipant[] getDerivedParticipants() throws CoreException {
		return DeleteExtensionManager.getParticipants(this, getDerivedElements(), fSharedParticipants);
	}
	
	public final IRefactoringParticipant[] getMappedParticipants() throws CoreException {
		return getMappedParticipants(fSharedParticipants);
	}

	public Object[] getDerivedElements() throws CoreException {
		return EMPTY_OBJECT_ARRAY;
	}

	protected IRefactoringParticipant[] getMappedParticipants(SharableParticipants shared) throws CoreException {
		return EMPTY_PARTICIPANT_ARRAY;
	}
	
	/**
	 * @deprecated Use #getMappedParticipants instead
	 */	
	public final IResourceModifications getResourceModifications() throws CoreException {
		// TODO Remove method from IRefactoringProcessor
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter.equals(IDeleteData.class))
			return new DeleteData();
		return null;
	}
}
