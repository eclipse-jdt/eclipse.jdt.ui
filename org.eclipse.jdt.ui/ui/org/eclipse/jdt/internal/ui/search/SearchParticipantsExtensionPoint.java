/*
 * Created on Apr 13, 2004
 * 
 * TODO To change the template for this generated file go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.jdt.internal.ui.search;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.internal.ui.JavaPlugin;

public class SearchParticipantsExtensionPoint {

	private Set fActiveParticipants= null;
	private static SearchParticipantsExtensionPoint fgInstance;

	public boolean hasAnyParticipants() {
		return Platform.getExtensionRegistry().getConfigurationElementsFor(JavaSearchPage.PARTICIPANT_EXTENSION_POINT).length > 0;
	}

	private synchronized Set getAllParticipants() {
		if (fActiveParticipants != null)
			return fActiveParticipants;
		IConfigurationElement[] allParticipants= Platform.getExtensionRegistry().getConfigurationElementsFor(JavaSearchPage.PARTICIPANT_EXTENSION_POINT);
		fActiveParticipants= new HashSet(allParticipants.length);
		for (int i= 0; i < allParticipants.length; i++) {
			SearchParticipantDescriptor descriptor= new SearchParticipantDescriptor(allParticipants[i]);
			IStatus status= descriptor.checkSyntax();
			if (status.isOK()) {
				fActiveParticipants.add(descriptor); //$NON-NLS-1$
			} else {
				JavaPlugin.log(status);
			}
		}
		return fActiveParticipants;
	}

	private void collectParticipants(Set participants, IProject[] projects) {
		Iterator activeParticipants= getAllParticipants().iterator();
		Set seenParticipants= new HashSet();
		while (activeParticipants.hasNext()) {
			SearchParticipantDescriptor participant= (SearchParticipantDescriptor) activeParticipants.next();
			if (participant.isEnabled()) {
				String id= participant.getID();
				for (int i= 0; i < projects.length; i++) {
					if (seenParticipants.contains(id))
						continue;
					try {
						if (projects[i].hasNature(participant.getNature())) {
							participants.add(new SearchParticipantRecord(participant, participant.create()));
							seenParticipants.add(id);
						}
					} catch (CoreException e) {
						JavaPlugin.log(e.getStatus());
						participant.disable();
					}
				}
			}
		}
	}



	public SearchParticipantRecord[] getSearchParticipants(IProject[] concernedProjects) throws CoreException {
		Set participantSet= new HashSet();
		collectParticipants(participantSet, concernedProjects);
		SearchParticipantRecord[] participants= new SearchParticipantRecord[participantSet.size()];
		return (SearchParticipantRecord[]) participantSet.toArray(participants);
	}

	public static SearchParticipantsExtensionPoint getInstance() {
		if (fgInstance == null)
			fgInstance= new SearchParticipantsExtensionPoint();
		return fgInstance;
	}
	
	public static void debugSetInstance(SearchParticipantsExtensionPoint instance) {
		fgInstance= instance;
	}
}