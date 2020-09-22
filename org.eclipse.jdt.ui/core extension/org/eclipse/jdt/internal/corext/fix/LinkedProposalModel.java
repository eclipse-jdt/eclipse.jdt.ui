/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *     Red Hat Inc. - modified to extend LinkedProposalModelCore
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.Iterator;

public class LinkedProposalModel extends LinkedProposalModelCore {

	@Override
	protected LinkedProposalPositionGroupCore createPositionGroup(String groupId) {
		LinkedProposalPositionGroupCore group = new LinkedProposalPositionGroup(groupId);
		return group;
	}

	@Override
	public LinkedProposalPositionGroup getPositionGroup(String groupId, boolean createIfNotExisting) {
		LinkedProposalPositionGroupCore group = super.getPositionGroup(groupId, createIfNotExisting);
		return group instanceof LinkedProposalPositionGroup ? (LinkedProposalPositionGroup)group : null;
	}

	private static class LinkedProposalPositionGroupIterator implements Iterator<LinkedProposalPositionGroup> {
		private Iterator<LinkedProposalPositionGroupCore> iterator;

		public LinkedProposalPositionGroupIterator(Iterator<LinkedProposalPositionGroupCore> iterator) {
			this.iterator= iterator;
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public LinkedProposalPositionGroup next() {
			return (LinkedProposalPositionGroup)iterator.next();
		}

		@Override
		public void remove() {
			iterator.remove();
		}
	}

	public Iterator<LinkedProposalPositionGroup> getPositionGroupIterator() {
		return new LinkedProposalPositionGroupIterator(super.getPositionGroupCoreIterator());
	}

}
