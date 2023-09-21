/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
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
 *******************************************************************************/

package org.eclipse.jdt.internal.corext.refactoring.changes;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.undo.snapshot.IResourceSnapshot;
import org.eclipse.core.resources.undo.snapshot.ResourceSnapshotFactory;

import org.eclipse.ltk.core.refactoring.Change;

public class UndoablePackageDeleteChange extends DynamicValidationStateChange {

	private final List<IResource> fPackageDeletes;

	public UndoablePackageDeleteChange(String name, List<IResource> packageDeletes) {
		super(name);
		fPackageDeletes= packageDeletes;
	}

	@Override
	public Change perform(IProgressMonitor pm) throws CoreException {
		int count= fPackageDeletes.size();
		pm.beginTask("", count * 3); //$NON-NLS-1$
		List<IResourceSnapshot<IResource>> snapshots = new ArrayList<>();
		for (int i= 0; i < fPackageDeletes.size(); i++) {
			IResource resource= fPackageDeletes.get(i);
			snapshots.add(ResourceSnapshotFactory.fromResource(resource));
			pm.worked(1);
		}

		DynamicValidationStateChange result= (DynamicValidationStateChange) super.perform(new SubProgressMonitor(pm, count));

		for (int i= 0; i < fPackageDeletes.size(); i++) {
			IResource resource= fPackageDeletes.get(i);
			IResourceSnapshot<IResource> resourceDescription= snapshots.get(i);
			resourceDescription.recordStateFromHistory(resource, new SubProgressMonitor(pm, 1));
			result.add(new UndoDeleteResourceChange(resourceDescription));
		}
		return result;
	}
}
