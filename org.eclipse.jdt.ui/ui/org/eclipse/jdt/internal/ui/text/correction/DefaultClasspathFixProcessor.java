/*******************************************************************************
 * Copyright (c) 2007, 2019 IBM Corporation and others.
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

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.manipulation.TypeNameMatchCollector;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameMatch;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.text.java.ClasspathFixProcessor;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.proposals.AddModuleRequiresCorrectionProposal;

/**
 * Default contribution to org.eclipse.jdt.ui.classpathFixProcessors
 */
public class DefaultClasspathFixProcessor extends ClasspathFixProcessor {

	protected static class DefaultClasspathFixProposal extends ClasspathFixProposal {

		private String fName;
		private Change fChange;
		private String fDescription;
		private int fRelevance;

		public DefaultClasspathFixProposal(String name, Change change, String description, int relevance) {
			fName= name;
			fChange= change;
			fDescription= description;
			fRelevance= relevance;
		}

		@Override
		public String getAdditionalProposalInfo() {
			return fDescription;
		}

		@Override
		public Change createChange(IProgressMonitor monitor) {
			return fChange;
		}

		@Override
		public String getDisplayString() {
			return fName;
		}

		@Override
		public Image getImage() {
			return JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		}

		@Override
		public int getRelevance() {
			return fRelevance;
		}
	}

	@Override
	public ClasspathFixProposal[] getFixImportProposals(IJavaProject project, String missingType) throws CoreException {
		ArrayList<DefaultClasspathFixProposal> res= new ArrayList<>();
		if (!missingType.startsWith(DefaultModulepathFixProcessor.MODULE_SEARCH)) {
			collectProposals(project, missingType, res);
		}
		return res.toArray(new ClasspathFixProposal[res.size()]);
	}

	private void collectProposals(IJavaProject project, String name, Collection<DefaultClasspathFixProposal> proposals) throws CoreException {
		int idx= name.lastIndexOf('.');
		char[] packageName= idx != -1 ? name.substring(0, idx).toCharArray() : null; // no package provided
		char[] typeName= name.substring(idx + 1).toCharArray();

		if (typeName.length == 1 && typeName[0] == '*') {
			typeName= null;
		}

		IJavaSearchScope scope= SearchEngine.createWorkspaceScope();
		ArrayList<TypeNameMatch> res= new ArrayList<>();
		TypeNameMatchCollector requestor= new TypeNameMatchCollector(res);
		int matchMode= SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE;
		new SearchEngine().searchAllTypeNames(packageName, matchMode, typeName,
				matchMode, IJavaSearchConstants.TYPE, scope, requestor,
				IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, null);

		if (res.isEmpty()) {
			return;
		}
		IModuleDescription currentModuleDescription= null;
		if (JavaModelUtil.is9OrHigher(project)) {
			currentModuleDescription= project.getModuleDescription();
			if (currentModuleDescription != null && !currentModuleDescription.exists()) {
				currentModuleDescription= null;
			}
		}

		HashMap<IClasspathEntry, TypeNameMatch> classPathEntryToTypeNameMatch= new HashMap<>();
		HashMap<TypeNameMatch, String> typeNameMatchToModuleName= new HashMap<>();
		HashSet<IClasspathEntry> classpaths= new HashSet<>();
		HashSet<TypeNameMatch> typesWithModule= new HashSet<>();
		if (currentModuleDescription != null) {
			for (TypeNameMatch curr : res) {
				IType type= curr.getType();
				if (type != null) {
					IPackageFragmentRoot root= (IPackageFragmentRoot) type.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
					try {
						IClasspathEntry entry= root.getRawClasspathEntry();
						if (entry == null) {
							continue;
						}

						String moduleName= null;
						IModuleDescription projectModule= root.getModuleDescription();
						if (projectModule != null && projectModule.exists()) {
							moduleName= projectModule.getElementName();
						}

						if (classpaths.add(entry)) {
							classPathEntryToTypeNameMatch.put(entry, curr);
							typesWithModule.add(curr);
							if (moduleName != null) {
								typeNameMatchToModuleName.put(curr, moduleName);
							}
						} else {
							TypeNameMatch typeNameMatch= classPathEntryToTypeNameMatch.get(entry);
							if (typeNameMatch != null) {
								if (moduleName != null) {
									String modName= typeNameMatchToModuleName.get(typeNameMatch);
									if (!moduleName.equals(modName)) {
										// remove classpath module if there are multiple type matches
										// which belong to the same class path but different modules
										typesWithModule.remove(typeNameMatch);
										classPathEntryToTypeNameMatch.remove(entry);
									}
								} else {
									// remove classpath module if there are multiple type matches
									// which belong to the same class path but one has module and the other does not
									typesWithModule.remove(typeNameMatch);
									classPathEntryToTypeNameMatch.remove(entry);
								}
							}
						}
					} catch (JavaModelException e) {
						// ignore
					}
				}
			}
		}


		HashSet<Object> addedClaspaths= new HashSet<>();
		for (TypeNameMatch curr : res) {
			IType type= curr.getType();
			if (type != null) {
				IPackageFragmentRoot root= (IPackageFragmentRoot) type.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
				try {
					IClasspathEntry entry= root.getRawClasspathEntry();
					if (entry == null) {
						continue;
					}
					Change cuChange= null;
					String moduleName= null;
					boolean isModule= false;
					if (typesWithModule.contains(curr)) {
						moduleName= typeNameMatchToModuleName.get(curr);
						if (moduleName != null && currentModuleDescription != null) {
							ICompilationUnit currentCU= currentModuleDescription.getCompilationUnit();
							isModule= true;
							String[] args= { moduleName };
							final String changeName= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_add_requires_module_info, args);
							final String changeDescription= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_add_requires_module_description, args);
							AddModuleRequiresCorrectionProposal moduleRequiresProposal= new AddModuleRequiresCorrectionProposal(moduleName, changeName, changeDescription, currentCU, 0);
							cuChange= moduleRequiresProposal.getChange();
							if (cuChange != null) {
								cuChange.initializeValidationData(new NullProgressMonitor());
							}
						}
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
							if (cuChange != null) {
								String additionalLabel= cuChange.getName();
								additionalLabel= additionalLabel.substring(0, 1).toLowerCase() + additionalLabel.substring(1);
								change= new CompositeChange(change.getName(), new Change[] { change, cuChange });
								String[] arguments= { label, additionalLabel };
								label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_combine_two_proposals_info, arguments);
								desc= label;
							}
							DefaultClasspathFixProposal proposal= new DefaultClasspathFixProposal(label, change, desc, IProposalRelevance.ADD_PROJECT_TO_BUILDPATH);
							proposals.add(proposal);
						}
					}
					if (entryKind == IClasspathEntry.CPE_CONTAINER) {
						IPath entryPath= entry.getPath();
						if (isNonProjectSpecificContainer(entryPath)) {
							addLibraryProposal(project, root, entry, addedClaspaths, proposals, cuChange);
						} else {
							try {
								IClasspathContainer classpathContainer= JavaCore.getClasspathContainer(entryPath, root.getJavaProject());
								if (classpathContainer != null) {
									IClasspathEntry entryInContainer= JavaModelUtil.findEntryInContainer(classpathContainer, root.getPath());
									if (entryInContainer != null) {
										addLibraryProposal(project, root, entryInContainer, addedClaspaths, proposals, cuChange);
									}
								}
							} catch (CoreException e) {
								// ignore
							}
						}
					} else if ((entryKind == IClasspathEntry.CPE_LIBRARY || entryKind == IClasspathEntry.CPE_VARIABLE)) {
						addLibraryProposal(project, root, entry, addedClaspaths, proposals, cuChange);
					}
				} catch (JavaModelException e) {
					// ignore
				}
			}
		}
	}

	protected void addLibraryProposal(IJavaProject project, IPackageFragmentRoot root, IClasspathEntry entry, Collection<Object> addedClaspaths, Collection<DefaultClasspathFixProposal> proposals,
			Change additionalChange) throws JavaModelException {
		if (isJREContainer(entry.getPath()) && hasJREInClassPath(project)) {
			return;
		}
		if (addedClaspaths.add(entry)) {
			String label= getAddClasspathLabel(entry, root, project);
			if (label != null) {
				Change change= ClasspathFixProposal.newAddClasspathChange(project, entry);
				if (change != null) {
					if (additionalChange != null) {
						String additionalLabel= additionalChange.getName();
						additionalLabel= additionalLabel.substring(0, 1).toLowerCase() + additionalLabel.substring(1);
						String[] arguments= { label, additionalLabel };
						label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_combine_two_proposals_info, arguments);
						change= new CompositeChange(change.getName(), new Change[] { change, additionalChange });
					}
					DefaultClasspathFixProposal proposal= new DefaultClasspathFixProposal(label, change, label, IProposalRelevance.ADD_TO_BUILDPATH);
					proposals.add(proposal);
				}
			}
		}
	}

	protected boolean isNonProjectSpecificContainer(IPath containerPath) {
		if (containerPath.segmentCount() > 0) {
			String id= containerPath.segment(0);
			if (JavaCore.USER_LIBRARY_CONTAINER_ID.equals(id) || id.equals(JavaRuntime.JRE_CONTAINER)) {
				return true;
			}
		}
		return false;
	}

	protected boolean isJREContainer(IPath containerPath) {
		if (containerPath != null && containerPath.segmentCount() > 0) {
			String id= containerPath.segment(0);
			if (id.equals(JavaRuntime.JRE_CONTAINER)) {
				return true;
			}
		}
		return false;
	}

	protected boolean hasJREInClassPath(IJavaProject javaProject) {
		if (javaProject != null) {
			try {
				for (IClasspathEntry oldClasspath : javaProject.getRawClasspath()) {
					if (isJREContainer(oldClasspath.getPath())) {
						return true;
					}
				}
			} catch (JavaModelException e) {
				// do nothing
			}
		}
		return false;
	}

	protected static String getAddClasspathLabel(IClasspathEntry entry, IPackageFragmentRoot root, IJavaProject project) {
		switch (entry.getEntryKind()) {
			case IClasspathEntry.CPE_LIBRARY:
				if (root.isArchive()) {
					String[] args= { JavaElementLabels.getElementLabel(root, JavaElementLabels.REFERENCED_ROOT_POST_QUALIFIED), BasicElementLabels.getJavaElementName(project.getElementName()) };
					return Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_addcp_archive_description, args);
				} else {
					String[] args= { JavaElementLabels.getElementLabel(root, JavaElementLabels.REFERENCED_ROOT_POST_QUALIFIED), BasicElementLabels.getJavaElementName(project.getElementName()) };
					return Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_addcp_classfolder_description, args);
				}
			case IClasspathEntry.CPE_VARIABLE: {
				String[] args= { JavaElementLabels.getElementLabel(root, 0), BasicElementLabels.getJavaElementName(project.getElementName()) };
				return Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_addcp_variable_description, args);
			}
			case IClasspathEntry.CPE_CONTAINER:
				try {
					String[] args= { JavaElementLabels.getContainerEntryLabel(entry.getPath(), root.getJavaProject()), BasicElementLabels.getJavaElementName(project.getElementName()) };
					return Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_addcp_library_description, args);
				} catch (JavaModelException e) {
					// ignore
				}
				break;
			default:
				break;
		}
		return null;
	}

}
