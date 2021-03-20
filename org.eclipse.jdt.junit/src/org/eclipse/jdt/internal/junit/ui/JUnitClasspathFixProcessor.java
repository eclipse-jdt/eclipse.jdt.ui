/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.junit.ui;

import java.util.ArrayList;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.buildpath.BuildPathSupport;
import org.eclipse.jdt.internal.junit.util.JUnitStubUtility;

import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.ClasspathFixProcessor;

public class JUnitClasspathFixProcessor extends ClasspathFixProcessor {

	private static class JUnitClasspathFixProposal extends ClasspathFixProposal {

		private final int fJunitVersion;
		private final int fRelevance;
		private final IJavaProject fProject;
		public JUnitClasspathFixProposal(IJavaProject project, int junitVersion, int relevance) {
			fProject= project;
			fJunitVersion= junitVersion;
			fRelevance= relevance;
		}

		@Override
		public String getAdditionalProposalInfo() {
			if (fJunitVersion == 5) {
				return JUnitMessages.JUnitAddLibraryProposal_junit5_info;
			}
			if (fJunitVersion == 4) {
				return JUnitMessages.JUnitAddLibraryProposal_junit4_info;
			}
			return JUnitMessages.JUnitAddLibraryProposal_info;
		}

		@Override
		public Change createChange(IProgressMonitor monitor) throws CoreException {
			if (monitor == null) {
				monitor= new NullProgressMonitor();
			}
			monitor.beginTask(JUnitMessages.JUnitClasspathFixProcessor_progress_desc, 1);
			try {
				IClasspathEntry entry= null;
				switch (fJunitVersion) {
				case 5:
					entry= BuildPathSupport.getJUnit5ClasspathEntry();
					break;
				case 4:
					entry= BuildPathSupport.getJUnit4ClasspathEntry();
					break;
				default:
					entry= BuildPathSupport.getJUnit3ClasspathEntry();
					break;
				}
				IClasspathEntry[] oldEntries= fProject.getRawClasspath();
				ArrayList<IClasspathEntry> newEntries= new ArrayList<>(oldEntries.length + 1);
				boolean added= false;
				for (IClasspathEntry curr : oldEntries) {
					if (curr.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
						IPath path= curr.getPath();
						if (path.equals(entry.getPath())) {
							return new NullChange(); // already on build path
						} else if (path.matchingFirstSegments(entry.getPath()) > 0) {
							if (!added) {
								curr= entry; // replace
								added= true;
							} else {
								curr= null;
							}
						}
					} else if (curr.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
						IPath path= curr.getPath();
						if (path.segmentCount() > 0 && JUnitCorePlugin.JUNIT_HOME.equals(path.segment(0))) {
							if (!added) {
								curr= entry; // replace
								added= true;
							} else {
								curr= null;
							}
						}
					}
					if (curr != null) {
						newEntries.add(curr);
					}
				}
				if (!added) {
					newEntries.add(entry);
				}

				final IClasspathEntry[] newCPEntries= newEntries.toArray(new IClasspathEntry[newEntries.size()]);
				Change newClasspathChange= newClasspathChange(fProject, newCPEntries, fProject.getOutputLocation());
				if (newClasspathChange != null) {
					return newClasspathChange;
				}
			} finally {
				monitor.done();
			}
			return new NullChange();
		}

		@Override
		public String getDisplayString() {
			if (fJunitVersion == 5) {
				return JUnitMessages.JUnitAddLibraryProposa_junit5_label;
			}
			if (fJunitVersion == 4) {
				return JUnitMessages.JUnitAddLibraryProposa_junit4_label;
			}
			return JUnitMessages.JUnitAddLibraryProposal_label;
		}

		@Override
		public Image getImage() {
			return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_LIBRARY);
		}

		@Override
		public int getRelevance() {
			return fRelevance;
		}
	}

	private static final int JUNIT3= 1;
	private static final int JUNIT4= 2;
	private static final int JUNIT5= 4;


	@Override
	public ClasspathFixProposal[] getFixImportProposals(IJavaProject project, String missingType) throws CoreException {
		String s= missingType;
		int res= 0;
		if (s.startsWith("org.junit.jupiter") || s.startsWith("org.junit.platform")) { //$NON-NLS-1$ //$NON-NLS-2$
			res= JUNIT5;
		} else if (s.startsWith("org.junit.")) { //$NON-NLS-1$
			res= JUNIT4;
		} else if ("TestCase".equals(s) || "TestSuite".equals(s) || s.startsWith("junit.")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			res= JUNIT3;
		} else if ("Test".equals(s)) { //$NON-NLS-1$
			res= JUNIT3 | JUNIT4 | JUNIT5;
		} else if ("TestFactory".equals(s) || "Testable".equals(s) || "TestTemplate".equals(s) || "ParameterizedTest".equals(s) || "RepeatedTest".equals(s)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			res= JUNIT5;
		} else if ("RunWith".equals(s)) { //$NON-NLS-1$
			res= JUNIT4;
		}
		if (res != 0) {
			ArrayList<JUnitClasspathFixProposal> proposals= new ArrayList<>();
			if ((res & JUNIT5) != 0 && JUnitStubUtility.is18OrHigher(project)) {
				proposals.add(new JUnitClasspathFixProposal(project, 5, 15));
			}
			if ((res & JUNIT4) != 0 && JUnitStubUtility.is50OrHigher(project)) {
				proposals.add(new JUnitClasspathFixProposal(project, 4, 15));
			}
			if ((res & JUNIT3) != 0) {
				proposals.add(new JUnitClasspathFixProposal(project, 3, 15));
			}
			return proposals.toArray(new ClasspathFixProposal[proposals.size()]);
		}
		return null;
	}
}
