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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPluginRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jdt.internal.corext.Assert;

import org.eclipse.jdt.internal.ui.JavaPlugin;


public class ExtensionManager {
	
	private String fProcessorID;
	private List fProcessors= new ArrayList(5);
	
	private String fParticipantID;
	private List fParticipants= new ArrayList(20);
	
	public ExtensionManager(String processorId, String participantId) {
		Assert.isNotNull(processorId);
		Assert.isNotNull(participantId);
		fProcessorID= processorId;
		fParticipantID= participantId;
		init();
	}
	
	public IRefactoringProcessor getProcessor(Object element) throws CoreException {
		List selected= new ArrayList();
		for (Iterator p= fProcessors.iterator(); p.hasNext();) {
			ProcessorDescriptor ce= (ProcessorDescriptor)p.next();
			if (ce.matches(element)) {
				selected.add(ce);
			}
		}
		if (selected.size() == 0)
			return null;
			
		if (selected.size() == 1) {
			return createProcessor((ProcessorDescriptor)selected.get(0), element);
		} else {
			Comparator sorter= new Comparator() {
				public int compare(Object o1, Object o2) {
					ProcessorDescriptor d1= (ProcessorDescriptor)o1;
					ProcessorDescriptor d2= (ProcessorDescriptor)o2;
				
					String oid1= d1.getOverrideId();
					String oid2= d2.getOverrideId();
				
					if (oid1 != null && oid2 == null) {
						if (oid1.equals(d2.getId()))
							return 1;
					} else if (oid1 == null && oid2 != null) {
						if (oid2.equals(d1.getId()))
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
				IRefactoringProcessor result= createProcessor((ProcessorDescriptor)selected.get(i), element);
				if (result != null)
					return result;
			}
		}
		return null;
	}
	
	public IRefactoringParticipant[] getParticipants(IRefactoringProcessor processor, Object[] elements) throws CoreException {
		Map shared= new HashMap();
		List result= new ArrayList();
		for (int i= 0; i < elements.length; i++) {
			Object element= elements[i];
			for (Iterator iter= fParticipants.iterator(); iter.hasNext();) {
				ParticipantDescriptor descriptor= (ParticipantDescriptor)iter.next();
				if (descriptor.matches(processor, element)) {
					IRefactoringParticipant participant= (IRefactoringParticipant)shared.get(descriptor);
					if (participant != null) {
						((ISharableParticipant)participant).add(element);
					} else {
						participant= descriptor.createParticipant();
						participant.initialize(processor, element);
						if (participant.isAvailable()) {
							result.add(participant);
							if (participant instanceof ISharableParticipant)
								shared.put(descriptor, participant);
						}
					}
				}
			}
		}
		
		return (IRefactoringParticipant[])result.toArray(new IRefactoringParticipant[result.size()]);
	}

	private void init() {
		IPluginRegistry registry= Platform.getPluginRegistry();
		IConfigurationElement[] ces= registry.getConfigurationElementsFor(
			JavaPlugin.getPluginId(), 
			fProcessorID);
		for (int i= 0; i < ces.length; i++) {
			fProcessors.add(new ProcessorDescriptor(ces[i]));
		}
		
		ces= registry.getConfigurationElementsFor(
			JavaPlugin.getPluginId(), 
			fParticipantID);
		for (int i= 0; i < ces.length; i++) {
			ParticipantDescriptor descriptor= new ParticipantDescriptor(ces[i]);
			IStatus status= descriptor.checkSyntax();
			switch (status.getSeverity()) {
				case IStatus.ERROR:
					JavaPlugin.log(status);
					break;
				case IStatus.WARNING:
				case IStatus.INFO:
					JavaPlugin.log(status);
					// fall through
				default:
					fParticipants.add(descriptor);
			}
		}
	}
	
	private IRefactoringProcessor createProcessor(ProcessorDescriptor processor, Object element) throws CoreException {
		IRefactoringProcessor result= processor.createProcessor();
		result.initialize(element);
		if (result.isAvailable())
			return result;
		return null;
	}
}
