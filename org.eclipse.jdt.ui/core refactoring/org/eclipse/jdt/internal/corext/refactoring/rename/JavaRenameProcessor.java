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

import org.eclipse.core.resources.IProject;

import org.eclipse.core.expressions.EvaluationContext;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.participants.RefactoringProcessors;
import org.eclipse.jdt.internal.corext.refactoring.participants.ResourceModifications;
import org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating;
import org.eclipse.ltk.core.refactoring.participants.ExtensionManagers;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;

public abstract class JavaRenameProcessor extends RenameProcessor implements INameUpdating {
	
	private String fNewElementName;
	
	public RenameParticipant[] getElementParticipants() throws CoreException {
		return ExtensionManagers.getRenameParticipants(this, getElements(), getAffectedProjects(), getSharedParticipants());
	}
	
	protected RefactoringParticipant[] createSecondaryParticipants(Object[] derivedElements, RenameArguments arguments, ResourceModifications resourceModifications) throws CoreException {
		String[] natures= RefactoringProcessors.getNatures(getAffectedProjects());
		
		List result= new ArrayList();
		if (derivedElements != null && derivedElements.length > 0) {
			EvaluationContext evalContext= ExtensionManagers.createStandardEvaluationContext(this, derivedElements, natures);
			RenameParticipant[] participants= ExtensionManagers.getRenameParticipants(this, derivedElements, evalContext, getSharedParticipants());
			for (int i= 0; i < participants.length; i++) {
				participants[i].setArguments(arguments);
			}
			result.addAll(Arrays.asList(participants));
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
	
	protected abstract IProject[] getAffectedProjects() throws CoreException;	
}
