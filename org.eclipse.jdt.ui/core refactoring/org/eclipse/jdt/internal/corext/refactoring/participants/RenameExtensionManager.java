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

public class RenameExtensionManager {
	
	private static final String PROCESSOR_EXT_POINT= "renameProcessors"; //$NON-NLS-1$
	private static final String PARTICIPANT_EXT_POINT= "renameParticipants"; //$NON-NLS-1$
	
	private static ExtensionManager fgInstance= new ExtensionManager("Rename", PROCESSOR_EXT_POINT, PARTICIPANT_EXT_POINT); //$NON-NLS-1$
	
	public static boolean hasProcessor(Object[] elements) throws CoreException {
		return fgInstance.hasProcessor(fgInstance.createProcessorPool(elements));
	}
	
	public static IRenameProcessor getProcessor(Object[] elements) throws CoreException {
		return (IRenameProcessor)fgInstance.getProcessor(elements, fgInstance.createProcessorPool(elements));
	}

	public static IRenameParticipant[] getParticipants(IRefactoringProcessor processor, Object[] elements) throws CoreException {
		IRefactoringParticipant[] participants= fgInstance.getParticipants(
			processor, elements, fgInstance.createParticipantPool(elements, processor), 
			new SharableParticipants());
		IRenameParticipant[] result= new IRenameParticipant[participants.length];
		System.arraycopy(participants, 0, result, 0, participants.length);
		return result;
	}	
}
