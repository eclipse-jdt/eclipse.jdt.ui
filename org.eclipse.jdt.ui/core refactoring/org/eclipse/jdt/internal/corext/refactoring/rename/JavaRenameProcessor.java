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

import org.eclipse.jdt.internal.corext.refactoring.participants.ResourceModifications;
import org.eclipse.ltk.core.refactoring.participants.ExtensionManagers;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;

public abstract class JavaRenameProcessor extends RenameProcessor {
	
	protected RefactoringParticipant[] createSecondaryParticipants(Object[] derivedElements, RenameArguments arguments, ResourceModifications resourceModifications) throws CoreException {
		List result= new ArrayList();
		if (derivedElements != null && derivedElements.length > 0) {
			RenameParticipant[] participants= ExtensionManagers.getRenameParticipants(this, derivedElements, getSharedParticipants());
			for (int i= 0; i < participants.length; i++) {
				participants[i].setArguments(arguments);
			}
			result.addAll(Arrays.asList(participants));
		}
		if (resourceModifications != null) {
			result.addAll(Arrays.asList(resourceModifications.getParticipants(this, getSharedParticipants())));
		}
		return (RefactoringParticipant[])result.toArray(new RefactoringParticipant[result.size()]);
	}
}
