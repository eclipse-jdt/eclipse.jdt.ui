/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.participants.ResourceModifications;
import org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;

public abstract class JavaRenameProcessor extends RenameProcessor implements INameUpdating {
	
	private String fNewElementName;
	
	public RenameParticipant[] loadElementParticipants() throws CoreException {
		Object[] elements= getElements();
		String[] natures= getAffectedProjectNatures();
		List result= new ArrayList();
		for (int i= 0; i < elements.length; i++) {
			result.addAll(Arrays.asList(ParticipantManager.getRenameParticipants(this, elements[i], natures, getSharedParticipants())));
		}
		return (RenameParticipant[])result.toArray(new RenameParticipant[result.size()]);
	}
	
	protected RefactoringParticipant[] loadDerivedParticipants(Object[] derivedElements, RenameArguments arguments, ResourceModifications resourceModifications) throws CoreException {
		String[] natures= getAffectedProjectNatures();
		
		List result= new ArrayList();
		if (derivedElements != null) {
			for (int i= 0; i < derivedElements.length; i++) {
				RenameParticipant[] participants= ParticipantManager.getRenameParticipants(this, derivedElements[i], natures, getSharedParticipants());
				for (int p= 0; p < participants.length; p++) {
					participants[p].setArguments(arguments);
				}
				result.addAll(Arrays.asList(participants));
			}
		}
		if (resourceModifications != null) {
			result.addAll(Arrays.asList(resourceModifications.getParticipants(this, natures, getSharedParticipants())));
		}
		return (RefactoringParticipant[])result.toArray(new RefactoringParticipant[result.size()]);
	}
	
	public void setNewElementName(String newName) {
		Assert.isNotNull(newName);
		fNewElementName= newName;
	}

	public String getNewElementName() {
		return fNewElementName;
	}
	
	protected abstract String[] getAffectedProjectNatures() throws CoreException;	
}
