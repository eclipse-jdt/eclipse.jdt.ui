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
package org.eclipse.ltk.core.refactoring.participants;

import org.eclipse.core.runtime.CoreException;

public abstract class DeleteProcessor extends RefactoringProcessor {

	private SharableParticipants fSharedParticipants= new SharableParticipants();
	
	private static final RefactoringParticipant[] EMPTY_PARTICIPANT_ARRAY= new RefactoringParticipant[0];

	public DeleteParticipant[] getElementParticipants() throws CoreException {
		return ExtensionManagers.getDeleteParticipants(this, getElements(), getSharedParticipants());
	}
	
	public RefactoringParticipant[] getSecondaryParticipants() throws CoreException {
		return EMPTY_PARTICIPANT_ARRAY;
	}
	
	public SharableParticipants getSharedParticipants() {
		return fSharedParticipants;
	}
}
