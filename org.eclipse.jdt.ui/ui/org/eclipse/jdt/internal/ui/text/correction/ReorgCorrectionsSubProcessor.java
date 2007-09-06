/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Matt Chapman, mpchapman@gmail.com - 89977 Make JDT .java agnostic
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.IProgressService;

import org.eclipse.ltk.core.refactoring.CompositeChange;

import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameMatch;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.fix.UnusedCodeFix;
import org.eclipse.jdt.internal.corext.refactoring.changes.ClasspathChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CreatePackageChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.MoveCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameCompilationUnitChange;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.TypeNameMatchCollector;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.actions.OrganizeImportsAction;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.fix.UnusedCodeCleanUp;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.preferences.BuildPathsPropertyPage;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CorrectMainTypeNameProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CorrectPackageDeclarationProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;
import org.eclipse.jdt.internal.ui.util.CoreUtility;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;

public class ReorgCorrectionsSubProcessor {

	public static void getWrongTypeNameProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();
		boolean isLinked= cu.getResource().isLinked();

		IJavaProject javaProject= cu.getJavaProject();
		String sourceLevel= javaProject.getOption(JavaCore.COMPILER_SOURCE, true);
		String compliance= javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true);

		CompilationUnit root= context.getASTRoot();

		ASTNode coveredNode= problem.getCoveredNode(root);
		if (!(coveredNode instanceof SimpleName))
			return;

		ASTNode parentType= coveredNode.getParent();
		if (!(parentType instanceof AbstractTypeDeclaration))
			return;

		String currTypeName= ((SimpleName) coveredNode).getIdentifier();
		String newTypeName= JavaCore.removeJavaLikeExtension(cu.getElementName());

		boolean hasOtherPublicTypeBefore= false;

		boolean found= false;
		List types= root.types();
		for (int i= 0; i < types.size(); i++) {
			AbstractTypeDeclaration curr= (AbstractTypeDeclaration) types.get(i);
			if (parentType != curr) {
				if (newTypeName.equals(curr.getName().getIdentifier())) {
					return;
				}
				if (!found && Modifier.isPublic(curr.getModifiers())) {
					hasOtherPublicTypeBefore= true;
				}
			} else {
				found= true;
			}
		}
		if (!JavaConventions.validateJavaTypeName(newTypeName, sourceLevel, compliance).matches(IStatus.ERROR)) {
			proposals.add(new CorrectMainTypeNameProposal(cu, context, currTypeName, newTypeName, 5));
		}

		if (!hasOtherPublicTypeBefore) {
			String newCUName= JavaModelUtil.getRenamedCUName(cu, currTypeName);
			ICompilationUnit newCU= ((IPackageFragment) (cu.getParent())).getCompilationUnit(newCUName);
			if (!newCU.exists() && !isLinked && !JavaConventions.validateCompilationUnitName(newCUName, sourceLevel, compliance).matches(IStatus.ERROR)) {
				RenameCompilationUnitChange change= new RenameCompilationUnitChange(cu, newCUName);

				// rename CU
				String label= Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_renamecu_description, newCUName);
				proposals.add(new ChangeCorrectionProposal(label, change, 6, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_RENAME)));
			}
		}
	}

	public static void getWrongPackageDeclNameProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();
		boolean isLinked= cu.getResource().isLinked();

		// correct package declaration
		int relevance= cu.getPackageDeclarations().length == 0 ? 7 : 5; // bug 38357
		proposals.add(new CorrectPackageDeclarationProposal(cu, problem, relevance));

		// move to package
		IPackageDeclaration[] packDecls= cu.getPackageDeclarations();
		String newPackName= packDecls.length > 0 ? packDecls[0].getElementName() : ""; //$NON-NLS-1$

		IPackageFragmentRoot root= JavaModelUtil.getPackageFragmentRoot(cu);
		IPackageFragment newPack= root.getPackageFragment(newPackName);

		ICompilationUnit newCU= newPack.getCompilationUnit(cu.getElementName());
		if (!newCU.exists() && !isLinked) {
			String label;
			if (newPack.isDefaultPackage()) {
				label= Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_movecu_default_description, cu.getElementName());
			} else {
				label= Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_movecu_description, new Object[] { cu.getElementName(), newPack.getElementName() });
			}
			CompositeChange composite= new CompositeChange(label);
			composite.add(new CreatePackageChange(newPack));
			composite.add(new MoveCompilationUnitChange(cu, newPack));

			proposals.add(new ChangeCorrectionProposal(label, composite, 6, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_MOVE)));
		}
	}

	public static void removeImportStatementProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		IFix fix= UnusedCodeFix.createRemoveUnusedImportFix(context.getASTRoot(), problem);
		if (fix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_DELETE_IMPORT);
			Map options= new Hashtable();
			options.put(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS, CleanUpConstants.TRUE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new UnusedCodeCleanUp(options), 6, image, context);
			proposals.add(proposal);
		}

		final ICompilationUnit cu= context.getCompilationUnit();
		String name= CorrectionMessages.ReorgCorrectionsSubProcessor_organizeimports_description;
		ChangeCorrectionProposal proposal= new ChangeCorrectionProposal(name, null, 5, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE)) {
			public void apply(IDocument document) {
				IEditorInput input= new FileEditorInput((IFile) cu.getResource());
				IWorkbenchPage p= JavaPlugin.getActivePage();
				if (p == null) {
					return;
				}
				IEditorPart part= p.findEditor(input);
				if (part instanceof JavaEditor) {
					OrganizeImportsAction action= new OrganizeImportsAction((JavaEditor) part);
					action.run(cu);
				}
			}
		};
		proposals.add(proposal);
	}

	public static void importNotFoundProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();
		IJavaProject project= cu.getJavaProject();

		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode != null) {
			ImportDeclaration importDeclaration= (ImportDeclaration) ASTNodes.getParent(selectedNode, ASTNode.IMPORT_DECLARATION);
			if (importDeclaration == null) {
				return;
			}
			if (!importDeclaration.isOnDemand()) {
				int kind= JavaModelUtil.is50OrHigher(cu.getJavaProject()) ? SimilarElementsRequestor.REF_TYPES : SimilarElementsRequestor.CLASSES | SimilarElementsRequestor.INTERFACES;
				UnresolvedElementsSubProcessor.addNewTypeProposals(cu, importDeclaration.getName(), kind, 5, proposals);
			}


			String name= ASTNodes.asString(importDeclaration.getName());
			char[] packageName;
			char[] typeName= null;
			if (importDeclaration.isOnDemand()) {
				packageName= name.toCharArray();
			} else {
				packageName= Signature.getQualifier(name).toCharArray();
				typeName= Signature.getSimpleName(name).toCharArray();
			}
			IJavaSearchScope scope= SearchEngine.createWorkspaceScope();
			ArrayList res= new ArrayList();
			TypeNameMatchCollector requestor= new TypeNameMatchCollector(res);
			int matchMode= SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE;
			new SearchEngine().searchAllTypeNames(packageName, matchMode, typeName,
					matchMode, IJavaSearchConstants.TYPE, scope, requestor,
					IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, null);

			if (res.isEmpty()) {
				return;
			}
			HashSet addedClaspaths= new HashSet();
			for (int i= 0; i < res.size(); i++) {
				TypeNameMatch curr= (TypeNameMatch) res.get(i);
				IType type= curr.getType();
				if (type != null) {
					IPackageFragmentRoot root= (IPackageFragmentRoot) type.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
					IClasspathEntry entry= root.getRawClasspathEntry();
					if (entry == null) {
						continue;
					}
					IJavaProject other= root.getJavaProject();
					int entryKind= entry.getEntryKind();
					if ((entry.isExported() || entryKind == IClasspathEntry.CPE_SOURCE) && addedClaspaths.add(other)) {
						String[] args= { other.getElementName(), project.getElementName() };
						String label= Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_addcp_project_description, args);
						IClasspathEntry newEntry= JavaCore.newProjectEntry(other.getPath());
						ClasspathChange change= ClasspathChange.addEntryChange(project, newEntry);
						if (change != null) {
							ChangeCorrectionProposal proposal= new ChangeCorrectionProposal(label, change, 8, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
							proposals.add(proposal);
						}
					}
					if ((entryKind == IClasspathEntry.CPE_LIBRARY || entryKind == IClasspathEntry.CPE_VARIABLE || entryKind == IClasspathEntry.CPE_CONTAINER) && addedClaspaths.add(entry)) {
						String label= getAddClasspathLabel(entry, root, project);
						if (label != null) {
							ClasspathChange change= ClasspathChange.addEntryChange(project, entry);
							if (change != null) {
								ChangeCorrectionProposal proposal= new ChangeCorrectionProposal(label, change, 7, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
								proposals.add(proposal);
							}
						}
					}
				}
			}
		}
	}

	private static String getAddClasspathLabel(IClasspathEntry entry, IPackageFragmentRoot root, IJavaProject project) {
		switch (entry.getEntryKind()) {
			case IClasspathEntry.CPE_LIBRARY:
				if (root.isArchive()) {
					String[] args= { JavaElementLabels.getElementLabel(root, JavaElementLabels.REFERENCED_ROOT_POST_QUALIFIED), project.getElementName() };
					return Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_addcp_archive_description, args);
				} else {
					String[] args= { JavaElementLabels.getElementLabel(root, JavaElementLabels.REFERENCED_ROOT_POST_QUALIFIED), project.getElementName() };
					return Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_addcp_classfolder_description, args);
				}
			case IClasspathEntry.CPE_VARIABLE: {
				String[] args= { JavaElementLabels.getElementLabel(root, 0), project.getElementName() };
				return Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_addcp_variable_description, args);
			}
			case IClasspathEntry.CPE_CONTAINER:
				try {
					String[] args= { JavaElementLabels.getContainerEntryLabel(entry.getPath(), root.getJavaProject()), project.getElementName() };
					return Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_addcp_library_description, args);
				} catch (JavaModelException e) {
					// ignore
				}
				break;
		}
		return null;
	}

	private static final class OpenBuildPathCorrectionProposal extends ChangeCorrectionProposal {
		private final IProject fProject;
		private final IBinding fReferencedType;
		private OpenBuildPathCorrectionProposal(IProject project, String label, int relevance, IBinding referencedType) {
			super(label, null, relevance, null);
			fProject= project;
			fReferencedType= referencedType;
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ACCESSRULES_ATTRIB));
		}
		public void apply(IDocument document) {
			Map data= null;
			if (fReferencedType != null) {
				IJavaElement elem= fReferencedType.getJavaElement();
				if (elem != null) {
					IPackageFragmentRoot root= (IPackageFragmentRoot) elem.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
					if (root != null) {
						try {
							IClasspathEntry entry= root.getRawClasspathEntry();
							if (entry != null) {
								data= new HashMap(1);
								data.put(BuildPathsPropertyPage.DATA_REVEAL_ENTRY, entry);
								if (entry.getEntryKind() != IClasspathEntry.CPE_CONTAINER) {
									data.put(BuildPathsPropertyPage.DATA_REVEAL_ATTRIBUTE_KEY, CPListElement.ACCESSRULES);
								}
							}
						} catch (JavaModelException e) {
							// ignore
						}
					}
				}
			}
			PreferencesUtil.createPropertyDialogOn(JavaPlugin.getActiveWorkbenchShell(), fProject, BuildPathsPropertyPage.PROP_ID, null, data).open();
		}
		public String getAdditionalProposalInfo() {
			return Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_configure_buildpath_description, fProject.getName());
		}
	}

	private static final class ChangeTo50Compliance extends ChangeCorrectionProposal implements IWorkspaceRunnable {

		private final IJavaProject fProject;
		private final boolean fChangeOnWorkspace;

		private Job fUpdateJob;
		private boolean f50JREFound;

		public ChangeTo50Compliance(String name, IJavaProject project, boolean changeOnWorkspace, int relevance) {
			super(name, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
			fProject= project;
			fChangeOnWorkspace= changeOnWorkspace;
			fUpdateJob= null;
			f50JREFound= false;
		}

		private boolean is50orGreaterVMInstall(IVMInstall install) {
			if (install instanceof IVMInstall2) {
				String compliance= JavaModelUtil.getCompilerCompliance((IVMInstall2) install, JavaCore.VERSION_1_3);
				return JavaModelUtil.is50OrHigher(compliance);
			}
			return false;
		}

		private IVMInstall find50OrGreaterVMInstall() {
			IVMInstallType[] installTypes= JavaRuntime.getVMInstallTypes();
			for (int i= 0; i < installTypes.length; i++) {
				IVMInstall[] installs= installTypes[i].getVMInstalls();
				for (int k= 0; k < installs.length; k++) {
					if (is50orGreaterVMInstall(installs[k])) {
						return installs[k];
					}
				}
			}
			return null;
		}

		public void run(IProgressMonitor monitor) throws CoreException {
			boolean needsBuild= updateJRE(monitor);
			if (needsBuild) {
				fUpdateJob= CoreUtility.getBuildJob(fChangeOnWorkspace ? null : fProject.getProject());
			}
		}

		private boolean updateJRE( IProgressMonitor monitor) throws CoreException, JavaModelException {
			try {
				IVMInstall vm50Install= find50OrGreaterVMInstall();
				f50JREFound= vm50Install != null;
				if (vm50Install != null) {
					IVMInstall install= JavaRuntime.getVMInstall(fProject); // can be null
					if (fChangeOnWorkspace) {
						monitor.beginTask(CorrectionMessages.ReorgCorrectionsSubProcessor_50_compliance_operation, 4);
						IVMInstall defaultVM= JavaRuntime.getDefaultVMInstall(); // can be null
						if (defaultVM != null && !defaultVM.equals(install)) {
							IPath newPath= new Path(JavaRuntime.JRE_CONTAINER);
							updateClasspath(newPath, new SubProgressMonitor(monitor, 1));
						} else {
							monitor.worked(1);
						}
						if (defaultVM == null || !is50orGreaterVMInstall(defaultVM)) {
							JavaRuntime.setDefaultVMInstall(vm50Install, new SubProgressMonitor(monitor, 3), true);
							return false;
						}
						return true;
					} else {
						if (install == null || !is50orGreaterVMInstall(install)) {
							IPath newPath= new Path(JavaRuntime.JRE_CONTAINER).append(vm50Install.getVMInstallType().getId()).append(vm50Install.getName());
							updateClasspath(newPath, monitor);
							return false;
						}
					}
				}
			} finally {
				monitor.done();
			}
			return true;
		}

		private void updateClasspath(IPath newPath, IProgressMonitor monitor) throws JavaModelException {
			IClasspathEntry[] classpath= fProject.getRawClasspath();
			IPath jreContainerPath= new Path(JavaRuntime.JRE_CONTAINER);
			for (int i= 0; i < classpath.length; i++) {
				IClasspathEntry curr= classpath[i];
				if (curr.getEntryKind() == IClasspathEntry.CPE_CONTAINER && curr.getPath().matchingFirstSegments(jreContainerPath) > 0) {
					classpath[i]= JavaCore.newContainerEntry(newPath, curr.getAccessRules(), curr.getExtraAttributes(), curr.isExported());
				}
			}
			fProject.setRawClasspath(classpath, monitor);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
		 */
		public String getAdditionalProposalInfo() {
			StringBuffer message= new StringBuffer();
			if (fChangeOnWorkspace) {
				message.append(CorrectionMessages.ReorgCorrectionsSubProcessor_50_compliance_changeworkspace_description);
			} else {
				message.append(CorrectionMessages.ReorgCorrectionsSubProcessor_50_compliance_changeproject_description);
			}

			IVMInstall vm50Install= find50OrGreaterVMInstall();
			if (vm50Install != null) {
				try {
					IVMInstall install= JavaRuntime.getVMInstall(fProject); // can be null
					if (fChangeOnWorkspace) {
						IVMInstall defaultVM= JavaRuntime.getDefaultVMInstall(); // can be null
						if (defaultVM != null && !defaultVM.equals(install)) {
							message.append(CorrectionMessages.ReorgCorrectionsSubProcessor_50_compliance_changeProjectJREToDefault_description);
						}
						if (defaultVM == null || !is50orGreaterVMInstall(defaultVM)) {
							message.append(Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_50_compliance_changeWorkspaceJRE_description, vm50Install.getName()));
						}
					} else {
						if (install == null || !is50orGreaterVMInstall(install)) {
							message.append(Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_50_compliance_changeProjectJRE_description, vm50Install.getName()));
						}
					}
				} catch (CoreException e) {
					// ignore
				}
			}
			return message.toString();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#apply(IDocument)
		 */
		public void apply(IDocument document) {
			if (fChangeOnWorkspace) {
				Hashtable map= JavaCore.getOptions();
				JavaModelUtil.set50CompilanceOptions(map);
				JavaCore.setOptions(map);
			} else {
				Map map= fProject.getOptions(false);
				int optionsCount= map.size();
				JavaModelUtil.set50CompilanceOptions(map);
				if (map.size() > optionsCount) {
					// options have been added -> ensure that all compliance options from preference page set
					JavaModelUtil.setDefaultClassfileOptions(map, JavaCore.VERSION_1_5);
				}
				fProject.setOptions(map);
			}
			try {
				IProgressService progressService= PlatformUI.getWorkbench().getProgressService();
				progressService.run(true, true, new WorkbenchRunnableAdapter(this));
			} catch (InvocationTargetException e) {
				JavaPlugin.log(e);
			} catch (InterruptedException e) {
				return;
			}

			if (fUpdateJob != null) {
				fUpdateJob.schedule();
			}

			if (!f50JREFound) {
				MessageDialog.openInformation(JavaPlugin.getActiveWorkbenchShell(), CorrectionMessages.ReorgCorrectionsSubProcessor_no_50jre_title, CorrectionMessages.ReorgCorrectionsSubProcessor_no_50jre_message);
			}
		}
	}

	/**
	 * Adds a proposal to change to 5.0 compliance
	 * @param context the context
	 * @param problem the current problem
	 * @param proposals the resulting proposals
	 */
	public static void getNeed50ComplianceProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		IJavaProject project= context.getCompilationUnit().getJavaProject();

		String label1= CorrectionMessages.ReorgCorrectionsSubProcessor_50_project_compliance_description;
		proposals.add(new ChangeTo50Compliance(label1, project, false, 5));

		if (project.getOption(JavaCore.COMPILER_COMPLIANCE, false) == null) {
			String label2= CorrectionMessages.ReorgCorrectionsSubProcessor_50_workspace_compliance_description;
			proposals.add(new ChangeTo50Compliance(label2, project, true, 6));
		}
	}

	/**
	 * Adds a proposal that opens the build path dialog
	 * @param context the context
	 * @param problem the current problem
	 * @param proposals the resulting proposals
	 */
	public static void getIncorrectBuildPathProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		IProject project= context.getCompilationUnit().getJavaProject().getProject();
		String label= CorrectionMessages.ReorgCorrectionsSubProcessor_configure_buildpath_label;
		OpenBuildPathCorrectionProposal proposal= new OpenBuildPathCorrectionProposal(project, label, 5, null);
		proposals.add(proposal);
	}

	public static void getAccessRulesProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		IBinding referencedElement= null;
		ASTNode node= problem.getCoveredNode(context.getASTRoot());
		if (node instanceof Type) {
			referencedElement= ((Type) node).resolveBinding();
		} else if (node instanceof Name) {
			referencedElement= ((Name) node).resolveBinding();
		}
		if (referencedElement != null && canModifyAccessRules(referencedElement)) {
			IProject project= context.getCompilationUnit().getJavaProject().getProject();
			String label= CorrectionMessages.ReorgCorrectionsSubProcessor_accessrules_description;
			OpenBuildPathCorrectionProposal proposal= new OpenBuildPathCorrectionProposal(project, label, 5, referencedElement);
			proposals.add(proposal);
		}
	}

	private static boolean canModifyAccessRules(IBinding binding) {
		IJavaElement element= binding.getJavaElement();
		if (element == null)
			return false;

		IPackageFragmentRoot root= JavaModelUtil.getPackageFragmentRoot(element);
		if (root == null)
			return false;

		try {
			IClasspathEntry classpathEntry= root.getRawClasspathEntry();
			if (classpathEntry == null)
				return false;
			if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY)
				return true;
			if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
				ClasspathContainerInitializer classpathContainerInitializer= JavaCore.getClasspathContainerInitializer(classpathEntry.getPath().segment(0));
				IStatus status= classpathContainerInitializer.getAccessRulesStatus(classpathEntry.getPath(), root.getJavaProject());
				return status.isOK();
			}
		} catch (JavaModelException e) {
			return false;
		}
		return false;
	}


}
