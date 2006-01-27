/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;

public class PositionGroup {

	private final String fGroupId;
	private final List/*<ITrackedNodePosition>*/ fPositions;
	private final List/*<String>*/ fProposals;
	private final List/*<String>*/ fDisplayStrings;
	private ITrackedNodePosition fFirstPosition;
	
	public ITrackedNodePosition getFirstPosition() {
		return fFirstPosition;
	}

	public PositionGroup(String groupID) {
		fGroupId= groupID;
		fPositions= new ArrayList();
		fProposals= new ArrayList();
		fDisplayStrings= new ArrayList();
	}

	public void addPosition(ITrackedNodePosition position) {
		fPositions.add(position);
	}
	
	public void addFirstPosition(ITrackedNodePosition position) {
		addPosition(position);
		fFirstPosition= position;
	}

	public void addProposal(String displayString, String proposal) {
		fProposals.add(proposal);
		fDisplayStrings.add(displayString);
	}

	public String getGroupId() {
		return fGroupId;
	}

	public ITrackedNodePosition[] getPositions() {
		return (ITrackedNodePosition[])fPositions.toArray(new ITrackedNodePosition[fPositions.size()]);
	}

	public String[] getDisplayStrings() {
		return (String[])fDisplayStrings.toArray(new String[fDisplayStrings.size()]);
	}

	public String[] getProposals() {
		return (String[])fProposals.toArray(new String[fProposals.size()]);
	}
}