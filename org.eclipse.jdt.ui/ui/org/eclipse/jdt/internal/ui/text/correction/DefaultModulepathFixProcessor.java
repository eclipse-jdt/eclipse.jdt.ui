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
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.ltk.core.refactoring.Change;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

public class DefaultModulepathFixProcessor extends DefaultClasspathFixProcessor {

	public static final String MODULE_SEARCH= "Module/:"; //$NON-NLS-1$

	@Override
	public ClasspathFixProposal[] getFixImportProposals(IJavaProject project, String missingType) throws CoreException {
		ArrayList<DefaultClasspathFixProposal> res= new ArrayList<>();
		if (missingType.startsWith(MODULE_SEARCH)) {
			collectModuleProposals(project, missingType.substring(MODULE_SEARCH.length()), res);
		}
		return res.toArray(new ClasspathFixProposal[res.size()]);
	}

	private void collectModuleProposals(IJavaProject project, String name, Collection<DefaultClasspathFixProposal> proposals) throws CoreException {

		IModuleDescription currentModuleDescription= null;
		if (JavaModelUtil.is9OrHigher(project)) {
			currentModuleDescription= project.getModuleDescription();
			if (currentModuleDescription != null && !currentModuleDescription.exists()) {
				currentModuleDescription= null;
			}
		}
		if (currentModuleDescription == null) {
			return;
		}

		IJavaSearchScope scope= SearchEngine.createWorkspaceScope();
		ArrayList<IModuleDescription> res= new ArrayList<>();
		SearchRequestor requestor= new SearchRequestor() {
			@Override
			public void acceptSearchMatch(SearchMatch match) throws CoreException {
				Object element= match.getElement();
				if (element instanceof IModuleDescription) {
					IModuleDescription moduleDesc= (IModuleDescription) element;
					if (moduleDesc.exists() || moduleDesc.isAutoModule()) {
						res.add(moduleDesc);
					}
				}
			}
		};
		SearchPattern searchPattern= SearchPattern.createPattern(name, IJavaSearchConstants.MODULE, IJavaSearchConstants.DECLARATIONS,
				SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
		if (searchPattern == null) {
			return;
		}
		SearchParticipant[] participants= new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
		try {
			new SearchEngine().search(searchPattern, participants, scope, requestor, null);
		} catch (CoreException | OperationCanceledException e) {
			//do nothing
		}

		if (res.isEmpty()) {
			return;
		}


		HashMap<IClasspathEntry, IModuleDescription> classPathEntryToModuleMap= new HashMap<>();
		HashSet<IClasspathEntry> classpaths= new HashSet<>();
		HashSet<String> typesWithModule= new HashSet<>();
		for (IModuleDescription curr : res) {
			if (curr != null) {
				IPackageFragmentRoot root= (IPackageFragmentRoot) curr.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
				try {
					IClasspathEntry entry= root.getRawClasspathEntry();
					if (entry == null) {
						continue;
					}

					String moduleName= curr.getElementName();


					if (classpaths.add(entry) && moduleName != null) {
						classPathEntryToModuleMap.put(entry, curr);
						typesWithModule.add(moduleName);
					}
				} catch (JavaModelException e) {
					// ignore
				}
			}
		}


		HashSet<Object> addedClaspaths= new HashSet<>();
		for (IModuleDescription curr : res) {
			if (curr != null) {
				IPackageFragmentRoot root= (IPackageFragmentRoot) curr.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
				try {
					IClasspathEntry entry= root.getRawClasspathEntry();
					if (entry == null) {
						continue;
					}
					String moduleName= curr.getElementName();
					boolean isModule= false;
					if (moduleName != null && typesWithModule.contains(moduleName)) {
						isModule= true;
					}
					IJavaProject other= root.getJavaProject();
					int entryKind= entry.getEntryKind();
					if ((entry.isExported() || entryKind == IClasspathEntry.CPE_SOURCE) && addedClaspaths.add(other)) {
						IClasspathEntry newEntry= null;
						if (isModule) {
							IClasspathAttribute[] extraAttributes= new IClasspathAttribute[] {
									JavaCore.newClasspathAttribute(IClasspathAttribute.MODULE, "true") //$NON-NLS-1$
							};
							newEntry= JavaCore.newProjectEntry(other.getPath(), null, true, extraAttributes, false);
						} else {
							newEntry= JavaCore.newProjectEntry(other.getPath());
						}
						Change change= ClasspathFixProposal.newAddClasspathChange(project, newEntry);
						if (change != null) {
							String[] args= { BasicElementLabels.getResourceName(other.getElementName()), BasicElementLabels.getResourceName(project.getElementName()) };
							String label= Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_addcp_project_description, args);
							String desc= label;
							DefaultClasspathFixProposal proposal= new DefaultClasspathFixProposal(label, change, desc, IProposalRelevance.ADD_PROJECT_TO_BUILDPATH);
							proposals.add(proposal);
						}
					}
					if (entryKind == IClasspathEntry.CPE_CONTAINER) {
						IPath entryPath= entry.getPath();
						if (isNonProjectSpecificContainer(entryPath)) {
							addLibraryProposal(project, root, entry, addedClaspaths, proposals, null);
						} else {
							try {
								IClasspathContainer classpathContainer= JavaCore.getClasspathContainer(entryPath, root.getJavaProject());
								if (classpathContainer != null) {
									IClasspathEntry entryInContainer= JavaModelUtil.findEntryInContainer(classpathContainer, root.getPath());
									if (entryInContainer != null) {
										addLibraryProposal(project, root, entryInContainer, addedClaspaths, proposals, null);
									}
								}
							} catch (CoreException e) {
								// ignore
							}
						}
					} else if ((entryKind == IClasspathEntry.CPE_LIBRARY || entryKind == IClasspathEntry.CPE_VARIABLE)) {
						addLibraryProposal(project, root, entry, addedClaspaths, proposals, null);
					}
				} catch (JavaModelException e) {
					// ignore
				}
			}
		}
	}
}
