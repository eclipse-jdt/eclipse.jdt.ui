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
package org.eclipse.ltk.core.refactoring.participants;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.internal.core.refactoring.Assert;


public class MoveRefactoring extends ProcessorBasedRefactoring {

	private MoveProcessor fProcessor;
	private MoveParticipant[] fElementParticipants;
	private RefactoringParticipant[] fDerivedParticipants;

	public MoveRefactoring(MoveProcessor processor) {
		Assert.isNotNull(processor);
		fProcessor= processor; 
	}
	
	public MoveProcessor getMoveProcessor() {
		return fProcessor;
	}

	protected RefactoringProcessor getProcessor() {
		return fProcessor;
	}

	protected RefactoringParticipant[] getElementParticipants(boolean setArguments) throws CoreException {
		if (fElementParticipants == null)
			fElementParticipants= fProcessor.loadElementParticipants();
		if (setArguments) {
			for (int i= 0; i < fElementParticipants.length; i++) {
				fProcessor.setArgumentsTo(fElementParticipants[i]);
			}
		}
		RefactoringParticipant[]result= new RefactoringParticipant[fElementParticipants.length];
		for (int i= 0; i < result.length; i++) {
			result[i]= fElementParticipants[i];
		}
		return result;
	}

	protected RefactoringParticipant[] getDerivedParticipants() throws CoreException {
		if (fDerivedParticipants == null)
			fDerivedParticipants= fProcessor.loadDerivedParticipants();
		return fDerivedParticipants;
	}	
}
