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

public class MoveExtensionManager {
	
	private static final String PROCESSOR_EXT_POINT= "moveProcessors"; //$NON-NLS-1$
	private static final String PARTICIPANT_EXT_POINT= "moveParticipants"; //$NON-NLS-1$
	
	private static ExtensionManager fgInstance= new ExtensionManager("Move", PROCESSOR_EXT_POINT, PARTICIPANT_EXT_POINT); //$NON-NLS-1$
	
	public static IMoveProcessor getProcessor(Object[] elements) throws CoreException {
		return (IMoveProcessor)fgInstance.getProcessor(
			elements, fgInstance.createProcessorEvaluationContext(elements));
	}
	
	public static IMoveParticipant[] getParticipants(IRefactoringProcessor processor) throws CoreException {
		return getParticipants(processor, processor.getDerivedElements());		
	}		

	public static IMoveParticipant[] getParticipants(IRefactoringProcessor processor, Object[] elements) throws CoreException {
		IRefactoringParticipant[] participants= fgInstance.getParticipants(processor, elements, 
			fgInstance.createParticipantEvaluationContext(elements, processor), new SharableParticipants());
		IMoveParticipant[] result= new IMoveParticipant[participants.length];
		System.arraycopy(participants, 0, result, 0, participants.length);
		return result;
	}
}
