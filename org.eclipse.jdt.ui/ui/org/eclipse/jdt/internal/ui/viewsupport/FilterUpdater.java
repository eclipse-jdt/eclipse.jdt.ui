/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.swt.widgets.Control;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;

import org.eclipse.jdt.core.JavaCore;


public class FilterUpdater implements IResourceChangeListener {

	private ProblemTreeViewer fViewer;

	public FilterUpdater(ProblemTreeViewer viewer) {
		Assert.isNotNull(viewer);
		fViewer= viewer;
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		if (fViewer.getInput() == null) {
			return;
		}
		IResourceDelta delta= event.getDelta();
		if (delta == null)
			return;
		for (IResourceDelta deltachild : delta.getAffectedChildren(IResourceDelta.CHANGED)) {
			if ((deltachild.getFlags() & IResourceDelta.DESCRIPTION) != 0) {
				IProject project= (IProject) deltachild.getResource();
				if (needsRefiltering(project)) {
					final Control ctrl= fViewer.getControl();
					if (ctrl != null && !ctrl.isDisposed()) {
						// async is needed due to bug 33783
						ctrl.getDisplay().asyncExec(() -> {
							if (!ctrl.isDisposed())
								fViewer.refresh(false);
						});
					}
					return; // one refresh is good enough
				}
			}
		}
	}

	private boolean needsRefiltering(IProject project) {
		try {
			Object element= project;
			if (project.hasNature(JavaCore.NATURE_ID)) {
				element= JavaCore.create(project);
			}
			boolean inView= fViewer.testFindItem(element) != null;
			boolean afterFilter= !fViewer.isFiltered(element, fViewer.getInput());

			return inView != afterFilter;
		} catch (CoreException e) {
			return true;
		}
	}
}
