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

public class CopyExtensionManager {
	
	private static final String PROCESSOR_EXT_POINT= "copyProcessors"; //$NON-NLS-1$
	private static final String PARTICIPANT_EXT_POINT= "copyParticipants"; //$NON-NLS-1$
	
	private static ExtensionManager fgInstance= new ExtensionManager("Copy", PROCESSOR_EXT_POINT, PARTICIPANT_EXT_POINT); //$NON-NLS-1$
	
	public static ICopyProcessor getProcessor(Object[] elements) throws CoreException {
		return (ICopyProcessor)fgInstance.getProcessor(elements, fgInstance.createProcessorEvaluationContext(elements));
	}
	
	public static ICopyParticipant[] getParticipants(IRefactoringProcessor processor) throws CoreException {
		return getParticipants(processor, processor.getDerivedElements());		
	}		

	public static ICopyParticipant[] getParticipants(IRefactoringProcessor processor, Object[] elements) throws CoreException {
		IRefactoringParticipant[] participants= fgInstance.getParticipants(processor, elements, 
			fgInstance.createParticipantEvaluationContext(elements, processor), new SharableParticipants());
		ICopyParticipant[] result= new ICopyParticipant[participants.length];
		System.arraycopy(participants, 0, result, 0, participants.length);
		return result;
	}
}
