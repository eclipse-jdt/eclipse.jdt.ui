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

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.internal.core.refactoring.Assert;


public abstract class RenameProcessor extends RefactoringProcessor {

	private int fStyle;
	private String fNewElementName;
	private SharableParticipants fSharedParticipants= new SharableParticipants();
	
	private static final RefactoringParticipant[] EMPTY_PARTICIPANT_ARRAY= new RefactoringParticipant[0];
	
	protected RenameProcessor() {
		fStyle= RefactoringStyles.NEEDS_PREVIEW;	
	}
	
	protected RenameProcessor(int style) {
		fStyle= style;	
	}

	public int getStyle() {
		return fStyle;
	}
	
	public void setNewElementName(String newName) {
		Assert.isNotNull(newName);
		fNewElementName= newName;
	}

	public String getNewElementName() {
		return fNewElementName;
	}
	
	public void setArgumentsTo(RenameParticipant participant) throws CoreException {
		participant.setArguments(getArguments());
	}
	
	public SharableParticipants getSharedParticipants() {
		return fSharedParticipants;
	}
	
	public RenameParticipant[] getElementParticipants() throws CoreException {
		return ExtensionManagers.getRenameParticipants(this, getElements(), getSharedParticipants());
	}
	
	public RefactoringParticipant[] getSecondaryParticipants() throws CoreException {
		return EMPTY_PARTICIPANT_ARRAY;
	}
	
	public abstract boolean getUpdateReferences();
	
	public RenameArguments getArguments() {
		return new RenameArguments(fNewElementName, getUpdateReferences());
	}

	public abstract String getCurrentElementName();
	
	public abstract RefactoringStatus checkNewElementName(String newName) throws CoreException;
	
	/**
	 * Returns the new element. This method must only return a
	 * valid result iff the refactoring as already been performed.
	 * Otherwise <code>null<code> can be returned.
	 * 
	 * @return the new element 
	 */
	public abstract Object getNewElement() throws CoreException;	
}
