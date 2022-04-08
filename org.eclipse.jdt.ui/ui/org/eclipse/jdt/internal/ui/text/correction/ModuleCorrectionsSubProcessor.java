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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.jface.operation.IRunnableContext;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ltk.core.refactoring.Change;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.provisional.JavaModelAccess;
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

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.text.java.ClasspathFixProcessor.ClasspathFixProposal;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ChangeCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewCUUsingWizardProposal;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ClasspathFixSelectionDialog;

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

	private static class ModulepathFixCorrectionProposal extends CUCorrectionProposal {

		private final String fModuleSearchStr;

		protected ModulepathFixCorrectionProposal(ICompilationUnit cu, String moduleSearchStr) {
			super(CorrectionMessages.ReorgCorrectionsSubProcessor_project_seup_fix_description, cu, -10, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
			fModuleSearchStr= DefaultModulepathFixProcessor.MODULE_SEARCH + moduleSearchStr;
		}

		@Override
		public void apply(IDocument document) {
			IRunnableContext context= JavaPlugin.getActiveWorkbenchWindow();
			if (context == null) {
				context= new BusyIndicatorRunnableContext();
			}
			Shell shell= JavaPlugin.getActiveWorkbenchShell();
			ClasspathFixSelectionDialog.openClasspathFixSelectionDialog(shell, getCompilationUnit().getJavaProject(), fModuleSearchStr, context);
		}

		@Override
		public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
			return Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_project_seup_fix_info, BasicElementLabels.getJavaElementName(fModuleSearchStr));
		}
	}


	public static void getPackageDoesNotExistProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return;
		}
		Name node= null;
		if (selectedNode instanceof Name) {
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
				IPackageFragmentRoot root= (IPackageFragmentRoot) moduleDescription.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
				if (root != null) {
					String name= node.getFullyQualifiedName();
					IPackageFragment pack= root.getPackageFragment(name);
					proposals.add(new NewCUUsingWizardProposal(cu, null, NewCUUsingWizardProposal.K_CLASS, pack, IProposalRelevance.NEW_TYPE));
					proposals.add(new NewCUUsingWizardProposal(cu, null, NewCUUsingWizardProposal.K_INTERFACE, pack, IProposalRelevance.NEW_TYPE));
					proposals.add(new NewCUUsingWizardProposal(cu, null, NewCUUsingWizardProposal.K_ENUM, pack, IProposalRelevance.NEW_TYPE));
					proposals.add(new NewCUUsingWizardProposal(cu, null, NewCUUsingWizardProposal.K_ANNOTATION, pack, IProposalRelevance.NEW_TYPE));
				}

			}
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
				int oldCount= proposals.size();
				addModifyClassPathProposals(proposals, javaProject, node);
				if (oldCount == proposals.size()) {
					proposals.add(new ModulepathFixCorrectionProposal(context.getCompilationUnit(),  node.getFullyQualifiedName()));
				}
			}
		}
	}

	private static void addModifyClassPathProposals(Collection<ICommandAccess> proposals, IJavaProject javaProject, Name node) throws CoreException {
		if (node == null || javaProject == null) {
			return;
		}
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
		if (searchPattern == null) {
			return;
		}
		SearchParticipant[] participants= new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
		try {
			new SearchEngine().search(searchPattern, participants, scope, requestor, null);
		} catch (CoreException | OperationCanceledException e) {
			//do nothing
		}

		IClasspathEntry[] existingEntries= javaProject.readRawClasspath();
		if (existingEntries != null && existingEntries.length > 0) {
			for (IModuleDescription moduleDesc : moduleDescriptions) {
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
			case IClasspathEntry.CPE_VARIABLE:
				args= new String[] { JavaElementLabels.getElementLabel(root, JavaElementLabels.REFERENCED_ROOT_POST_QUALIFIED) };
				break;
			case IClasspathEntry.CPE_PROJECT:
				args= new String[] { root.getJavaProject().getElementName() };
				break;
			case IClasspathEntry.CPE_CONTAINER:
				try {
					IClasspathContainer container= JavaCore.getClasspathContainer(entry.getPath(), root.getJavaProject());
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
			default:
				break;
		}
		if (args != null) {
			return Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_move_cpentry_mpentry_description, args);
		}
		return null;
	}

	public static IModuleDescription getModuleDescription(IJavaElement element) {
		IModuleDescription projectModule= null;
		try {
			switch (element.getElementType()) {
				case IJavaElement.JAVA_PROJECT:
					IJavaProject project= (IJavaProject) element;
					if (JavaModelUtil.is9OrHigher(project)) {
						projectModule= project.getModuleDescription();
					}
					break;
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:
					IPackageFragmentRoot root= (IPackageFragmentRoot) element;
					projectModule= root.getModuleDescription();
					break;
				default:
					//do nothing
			}
			if (projectModule == null) {
				projectModule= JavaModelAccess.getAutomaticModuleDescription(element);
			}
		} catch (JavaModelException | IllegalArgumentException e) {
			JavaPlugin.log(e);
		}
		return projectModule;
	}

	private ModuleCorrectionsSubProcessor() {
	}

}
