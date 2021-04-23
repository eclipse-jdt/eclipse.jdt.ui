/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - copied from LinkedProposalModel
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;

import org.eclipse.jdt.internal.corext.fix.LinkedProposalPositionGroupCore.PositionInformation;

public class LinkedProposalModelCore {

	private Map<String, LinkedProposalPositionGroupCore> fPositionGroups;
	private LinkedProposalPositionGroupCore.PositionInformation fEndPosition;

	public void addPositionGroup(LinkedProposalPositionGroupCore positionGroup) {
		if (positionGroup == null) {
			throw new IllegalArgumentException("positionGroup must not be null"); //$NON-NLS-1$
		}

		if (fPositionGroups == null) {
			fPositionGroups= new HashMap<>();
		}
		fPositionGroups.put(positionGroup.getGroupId(), positionGroup);
	}

	protected LinkedProposalPositionGroupCore createPositionGroup(String groupId) {
		return new LinkedProposalPositionGroupCore(groupId);
	}

	public LinkedProposalPositionGroupCore getPositionGroup(String groupId, boolean createIfNotExisting) {
		LinkedProposalPositionGroupCore group= fPositionGroups != null ? fPositionGroups.get(groupId) : null;
		if (createIfNotExisting && group == null) {
			group= createPositionGroup(groupId);
			addPositionGroup(group);
		}
		return group;
	}

	public Iterator<LinkedProposalPositionGroupCore> getPositionGroupCoreIterator() {
		if (fPositionGroups == null) {
			return new Iterator<LinkedProposalPositionGroupCore>() {
				@Override
				public boolean hasNext() {return false;}
				@Override
				public LinkedProposalPositionGroupCore next() {throw new NoSuchElementException();}
				@Override
				public void remove() {throw new UnsupportedOperationException();}
			};
		}
		return fPositionGroups.values().iterator();
	}


	/**
	 * Sets the end position of the linked mode to the end of the passed range.
	 * @param position The position that describes the end position of the linked mode.
	 */
	public void setEndPosition(PositionInformation position) {
		fEndPosition= position;
	}

	public void setEndPosition(ITrackedNodePosition position) {
		setEndPosition(LinkedProposalPositionGroupCore.createPositionInformation(position, 1));
	}

	public PositionInformation getEndPosition() {
		return fEndPosition;
	}

	public boolean hasLinkedPositions() {
		return fPositionGroups != null && !fPositionGroups.isEmpty();
	}

	public void clear() {
		fPositionGroups= null;
		fEndPosition= null;
	}

}
