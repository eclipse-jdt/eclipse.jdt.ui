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

public class DeleteExtensionManager {
	
	private static final String PROCESSOR_EXT_POINT= "deleteProcessors"; //$NON-NLS-1$
	private static final String PARTICIPANT_EXT_POINT= "deleteParticipants"; //$NON-NLS-1$
	
	private static ExtensionManager fgInstance= new ExtensionManager("Delete", PROCESSOR_EXT_POINT, PARTICIPANT_EXT_POINT); //$NON-NLS-1$
	
	public static boolean hasProcessor(Object[] elements) throws CoreException {
		return fgInstance.hasProcessor(fgInstance.createProcessorEvaluationContext(elements));
	}
	
	public static DeleteProcessor getProcessor(Object[] elements) throws CoreException {
		return (DeleteProcessor)fgInstance.getProcessor(elements, fgInstance.createProcessorEvaluationContext(elements));
	}
	
	public static IDeleteParticipant[] getParticipants(IRefactoringProcessor processor, Object[] elements, SharableParticipants shared) throws CoreException {
		IRefactoringParticipant[] participants= fgInstance.getParticipants(processor, elements, 
			fgInstance.createParticipantEvaluationContext(elements, processor), shared);
		IDeleteParticipant[] result= new IDeleteParticipant[participants.length];
		System.arraycopy(participants, 0, result, 0, participants.length);
		return result;
	}
}
