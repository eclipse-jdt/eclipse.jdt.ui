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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPluginRegistry;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jdt.internal.ui.JavaPlugin;


public class RenameExtensionManager {
	
	private static final String PROCESSOR_EXT_POINT= "renameProcessor"; //$NON-NLS-1$
	private static final String PARTICIPANT_EXT_POINT= "renameParticipants"; //$NON-NLS-1$
	
	private List fProcessors= new ArrayList(5);
	private List fParticipants= new ArrayList(20);
	
	private static RenameExtensionManager fInstance;
	
	public RenameExtensionManager() {
		init();
	}
	
	public static IRenameProcessor getProcessor(RenameRefactoring refactoring, Object element) throws CoreException {
		if (fInstance == null)
			fInstance= new RenameExtensionManager();
		return fInstance.internalGetProcessor(refactoring, element);
	}

	public static IRenameParticipant[] getParticipants(RenameRefactoring refactoring) throws CoreException {
		if (fInstance == null)
			fInstance= new RenameExtensionManager();
		return fInstance.internalGetParticipants(refactoring);
	}
	
	private IRenameParticipant[] internalGetParticipants(RenameRefactoring refactoring) throws CoreException {
		List result= new ArrayList();
		IRenameProcessor processor= refactoring.getProcessor();
		Object[] elements= processor.getProcessableElements();
		for (int i= 0; i < elements.length; i++) {
			Object element= elements[i];
			for (Iterator iter= fParticipants.iterator(); iter.hasNext();) {
				RenameParticipantElement participant= (RenameParticipantElement)iter.next();
				if (participant.matches(element)) {
					IRenameParticipant pp= participant.createParticipant();
					pp.initialize(refactoring, element);
					if (pp.isAvailable())
						result.add(pp);
			    }
			}
		}
		
		return (IRenameParticipant[])result.toArray(new IRenameParticipant[result.size()]);
	}
	
	private IRenameProcessor internalGetProcessor(RenameRefactoring refactoring, Object element) throws CoreException {
		List selected= new ArrayList();
		for (Iterator p= fProcessors.iterator(); p.hasNext();) {
			RenameProcessorElement ce= (RenameProcessorElement)p.next();
			if (ce.matches(element)) {
				selected.add(ce);
			}
		}
		if (selected.size() == 0)
			return null;
			
		if (selected.size() == 1) {
			return createProcessor((RenameProcessorElement)selected.get(0), 
				refactoring, element);
		} else {
			Comparator sorter= new Comparator() {
				public int compare(Object o1, Object o2) {
					RenameProcessorElement e1= (RenameProcessorElement)o1;
					RenameProcessorElement e2= (RenameProcessorElement)o2;
				
					String oid1= e1.getOverrideId();
					String oid2= e2.getOverrideId();
				
					if (oid1 != null && oid2 == null) {
						if (oid1.equals(e2.getId()))
							return 1;
					} else if (oid1 == null && oid2 != null) {
						if (oid2.equals(e1.getId()))
							return -1;
					}
					return 0;
				}
				public boolean equals(Object obj) {
					return super.equals(obj);
				}
			};
		
			Collections.sort(selected, sorter);
			for (int i= selected.size() - 1; i >= 0; i--) {
				IRenameProcessor result= createProcessor(
					(RenameProcessorElement)selected.get(i), refactoring, element);
				if (result != null)
					return result;
			}
		}
		return null;
	}
	
	private void init() {
		IPluginRegistry registry= Platform.getPluginRegistry();
		IConfigurationElement[] ces= registry.getConfigurationElementsFor(
			JavaPlugin.getPluginId(), 
			PROCESSOR_EXT_POINT);
		for (int i= 0; i < ces.length; i++) {
			fProcessors.add(new RenameProcessorElement(ces[i]));
		}
		
		ces= registry.getConfigurationElementsFor(
			JavaPlugin.getPluginId(), 
			PARTICIPANT_EXT_POINT);
		for (int i= 0; i < ces.length; i++) {
			fParticipants.add(new RenameParticipantElement(ces[i]));
		}
	}
	
	private IRenameProcessor createProcessor(RenameProcessorElement processor, RenameRefactoring refactoring, Object element) throws CoreException {
		IRenameProcessor result= processor.createProcessor();
		result.initialize(refactoring, element);
		if (result.isAvailable())
			return result;
		return null;
	}
}
