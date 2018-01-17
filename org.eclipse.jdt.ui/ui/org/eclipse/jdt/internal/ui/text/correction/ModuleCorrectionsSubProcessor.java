/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.ltk.core.refactoring.Change;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.text.java.ClasspathFixProcessor.ClasspathFixProposal;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.ChangeCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class ModuleCorrectionsSubProcessor {

	private static class ModulepathFixProposal extends ChangeCorrectionProposal {

		private String fDescription;

		public ModulepathFixProposal(String name, Change change, String description, int relevance) {
			super(name, change, relevance);
			fDescription= description;
		}

		@Override
		public String getAdditionalProposalInfo() {
			return fDescription;
		}

		@Override
		public Image getImage() {
			return JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		}
	}


	public static void getUndefinedModuleProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return;
		}
		Name node= null;
		if (selectedNode instanceof SimpleType) {
			node= ((SimpleType) selectedNode).getName();
		} else if (selectedNode instanceof NameQualifiedType) {
			node= ((NameQualifiedType) selectedNode).getName();
		} else if (selectedNode instanceof Name) {
			node= (Name) selectedNode;
		} else {
			return;
		}

		IJavaProject javaProject= cu.getJavaProject();

		IModuleDescription moduleDescription= cu.getModule();
		if (moduleDescription != null && moduleDescription.exists()
				&& javaProject != null && JavaModelUtil.is9OrHigher(javaProject)) {
			ICompilationUnit moduleCompilationUnit= moduleDescription.getCompilationUnit();
			if (cu.equals(moduleCompilationUnit)) {
				IJavaElement[] elements= new IJavaElement[1];
				elements[0]= javaProject;
				IJavaSearchScope scope= SearchEngine.createJavaSearchScope(elements);
				List<IModuleDescription> moduleDescriptions= new ArrayList<>();
				SearchRequestor requestor= new SearchRequestor() {
					@Override
					public void acceptSearchMatch(SearchMatch match) throws CoreException {
						Object element= match.getElement();
						if (element instanceof IModuleDescription) {
							IModuleDescription moduleDesc= (IModuleDescription) element;
							if (moduleDesc.exists() || moduleDesc.isAutoModule()) {
								moduleDescriptions.add(moduleDesc);
							}
						}
					}
				};

				SearchPattern searchPattern= SearchPattern.createPattern(node.getFullyQualifiedName(), IJavaSearchConstants.MODULE, IJavaSearchConstants.DECLARATIONS,
						SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
				SearchParticipant[] participants= new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
				try {
					new SearchEngine().search(searchPattern, participants, scope, requestor, null);
				} catch (CoreException e) {
					//do nothing
				} catch (OperationCanceledException e) {
					//do nothing
				}
				
				IClasspathEntry[] existingEntries= javaProject.readRawClasspath();
				if (existingEntries != null && existingEntries.length > 0) {
					for (int i= 0; i < moduleDescriptions.size(); i++) {
						IModuleDescription moduleDesc= moduleDescriptions.get(i);
						IPackageFragmentRoot root= (IPackageFragmentRoot) moduleDesc.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
						if (root != null) {
							IClasspathEntry entry= null;
							int index= -1;
							if (root.getKind() == IPackageFragmentRoot.K_BINARY) {
								entry= root.getRawClasspathEntry();
								index= getClassPathPresentByEntry(existingEntries, entry);
							} else if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
								IJavaProject project= root.getJavaProject();
								IPath path= project.getPath();
								index= getClassPathPresentByPath(existingEntries, path);
								if (index != -1) {
									entry= existingEntries[index];
								}
							}
							if (entry != null && index != -1) {
								modifyClasspathProposal(javaProject, root, existingEntries, index, proposals);
							}
						}
					}
				}
			}
		}
	}

	private static int getClassPathPresentByEntry(IClasspathEntry[] existingEntries, IClasspathEntry entry) {
		int index= -1;
		if (existingEntries != null && existingEntries.length > 0 && entry != null) {
			for (int i= 0; i < existingEntries.length; i++) {
				if (existingEntries[i].equals(entry)) {
					index= i;
					break;
				}
			}
		}
		return index;
	}

	private static int getClassPathPresentByPath(IClasspathEntry[] existingEntries, IPath path) {
		int index= -1;
		if (existingEntries != null && existingEntries.length > 0 && path != null) {
			for (int i= 0; i < existingEntries.length; i++) {
				if (existingEntries[i].getPath().equals(path)) {
					index= i;
					break;
				}
			}
		}
		return index;
	}

	private static IClasspathAttribute[] addModuleAttributeIfNeeded(IClasspathAttribute[] extraAttributes) {
		String TRUE= "true"; //$NON-NLS-1$
		for (int j= 0; j < extraAttributes.length; j++) {
			IClasspathAttribute classpathAttribute= extraAttributes[j];
			if (IClasspathAttribute.MODULE.equals(classpathAttribute.getName())) {
				if (TRUE.equals(classpathAttribute.getValue())) {
					return null; // no change required
				}
				extraAttributes[j]= JavaCore.newClasspathAttribute(IClasspathAttribute.MODULE, TRUE);
				return extraAttributes;
			}
		}
		extraAttributes= Arrays.copyOf(extraAttributes, extraAttributes.length + 1);
		extraAttributes[extraAttributes.length - 1]= JavaCore.newClasspathAttribute(IClasspathAttribute.MODULE, TRUE);
		return extraAttributes;
	}

	private static void modifyClasspathProposal(IJavaProject project, IPackageFragmentRoot root, IClasspathEntry[] entries, int position, Collection<ICommandAccess> proposals)
			throws JavaModelException {
		IClasspathEntry[] existingEntries= Arrays.copyOf(entries, entries.length);
		IClasspathEntry entry= existingEntries[position];
		String label= getModifyClasspathLabel(entry, root);
		if (label != null && position != -1) {
			IClasspathAttribute[] attributes= entry.getExtraAttributes();
			IClasspathAttribute[] newAttributes= addModuleAttributeIfNeeded(attributes);
			if (newAttributes != null) {
				IClasspathEntry entryModular= addAttributes(entry, newAttributes);
				if (entryModular != null) {
					existingEntries[position]= entryModular;
				}
				Change change= ClasspathFixProposal.newClasspathChange(project, existingEntries, project.getOutputLocation());
				if (change != null) {
					ModulepathFixProposal proposal= new ModulepathFixProposal(label, change, label, IProposalRelevance.ADD_TO_BUILDPATH);
					proposals.add(proposal);
				}
			}
		}
	}

	private static IClasspathEntry addAttributes(IClasspathEntry entry, IClasspathAttribute[] extraAttributes) {
		switch (entry.getEntryKind()) {
			case IClasspathEntry.CPE_LIBRARY:
				return JavaCore.newLibraryEntry(entry.getPath(), entry.getSourceAttachmentPath(),
						entry.getSourceAttachmentRootPath(), entry.getAccessRules(), extraAttributes, entry.isExported());
			case IClasspathEntry.CPE_PROJECT:
				return JavaCore.newProjectEntry(entry.getPath(), entry.getAccessRules(), entry.combineAccessRules(),
						extraAttributes, entry.isExported());
			case IClasspathEntry.CPE_CONTAINER:
				return JavaCore.newContainerEntry(entry.getPath(), entry.getAccessRules(), extraAttributes, entry.isExported());
			case IClasspathEntry.CPE_VARIABLE:
				return JavaCore.newVariableEntry(entry.getPath(), entry.getSourceAttachmentPath(), entry.getSourceAttachmentRootPath(), 
						entry.getAccessRules(), extraAttributes, entry.isExported());
			default:
				return entry; // other kinds are not handled
		}
	}

	private static String getModifyClasspathLabel(IClasspathEntry entry, IPackageFragmentRoot root) {
		String[] args= null;
		switch (entry.getEntryKind()) {
			case IClasspathEntry.CPE_LIBRARY:
				args= new String[] { JavaElementLabels.getElementLabel(root, JavaElementLabels.REFERENCED_ROOT_POST_QUALIFIED) };
				break;
			case IClasspathEntry.CPE_PROJECT:
				args= new String[] { root.getJavaProject().getElementName() };
				break;
			case IClasspathEntry.CPE_CONTAINER:
				try {
					IClasspathContainer container= null;
					container= JavaCore.getClasspathContainer(entry.getPath(), root.getJavaProject());
					if (container != null) {
						String name= container.getDescription();
						if (name != null && name.length() > 0) {
							args= new String[] { name };
						}
					}
				} catch (JavaModelException e) {
					//do nothing
				}
				if (args == null) {
					args= new String[] { root.getElementName() };
				}
				break;
			case IClasspathEntry.CPE_VARIABLE:
				args= new String[] { JavaElementLabels.getElementLabel(root, JavaElementLabels.REFERENCED_ROOT_POST_QUALIFIED) };
				break;
			default:
				break;
		}
		if (args != null) {
			return Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_move_cpentry_mpentry_description, args);
		}
		return null;
	}

}
