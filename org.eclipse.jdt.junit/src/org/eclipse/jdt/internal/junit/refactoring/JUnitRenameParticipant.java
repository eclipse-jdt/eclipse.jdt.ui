/*******************************************************************************
 * Copyright (c) 2004 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.jdt.internal.junit.refactoring;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;


public abstract class JUnitRenameParticipant extends RenameParticipant {

	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context) {
		return new RefactoringStatus();
	}

	public Change createChange(IProgressMonitor pm) throws CoreException {
		if (!getArguments().getUpdateReferences()) 
			return null;	
		
		ILaunchManager manager= DebugPlugin.getDefault().getLaunchManager();
		List launchConfigTypes= JUnitPlugin.getDefault().getJUnitLaunchConfigTypeIDs();
		List changes= new ArrayList();
		for (Iterator types= launchConfigTypes.iterator(); types.hasNext();) {
			String typeId= (String) types.next();
			ILaunchConfigurationType type= manager.getLaunchConfigurationType(typeId);
			ILaunchConfiguration configs[]= manager.getLaunchConfigurations(type);
			createChangeForConfigs(changes, configs);
			if (pm.isCanceled())
				throw new OperationCanceledException();
		}
		if (changes.size() > 0)
			return new CompositeChange(getChangeName(), (Change[]) changes.toArray(new Change[changes.size()])); //$NON-NLS-1$
		return null; 
	}

	protected String getChangeName() {
		return JUnitMessages.getString("TypeRenameParticipant.change.name"); //$NON-NLS-1$
	}

	protected abstract void createChangeForConfigs(List changes, ILaunchConfiguration[] configs) throws CoreException;
}