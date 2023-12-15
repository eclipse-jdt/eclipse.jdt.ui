/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Matt Chapman, mpchapman@gmail.com - 89977 Make JDT .java agnostic
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;

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
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.manipulation.TypeKinds;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.codemanipulation.AddImportsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.AddImportsOperation.IChooseImportQuery;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.IProposableFix;
import org.eclipse.jdt.internal.corext.fix.UnusedCodeFix;
import org.eclipse.jdt.internal.corext.refactoring.changes.CreatePackageChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.MoveCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameCompilationUnitChange;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.actions.OrganizeImportsAction;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ChangeCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.fix.UnusedCodeCleanUp;
import org.eclipse.jdt.internal.ui.javaeditor.AddImportOnSelectionAction;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.preferences.BuildPathsPropertyPage;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CorrectMainTypeNameProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CorrectPackageDeclarationProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CorrectPackageDeclarationProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.util.ClasspathVMUtil;
import org.eclipse.jdt.internal.ui.util.CoreUtility;
import org.eclipse.jdt.internal.ui.util.Progress;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ClasspathFixSelectionDialog;

public class ReorgCorrectionsSubProcessor {

	public static void getWrongTypeNameProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) {
		ICompilationUnit cu= context.getCompilationUnit();
		boolean isLinked= cu.getResource().isLinked();

		IJavaProject javaProject= cu.getJavaProject();
		String sourceLevel= javaProject.getOption(JavaCore.COMPILER_SOURCE, true);
		String compliance= javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true);
		String previewEnabled= javaProject.getOption(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, true);

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
		List<AbstractTypeDeclaration> types= root.types();
		for (AbstractTypeDeclaration curr : types) {
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
		if (!JavaConventions.validateJavaTypeName(newTypeName, sourceLevel, compliance, previewEnabled).matches(IStatus.ERROR)) {
			proposals.add(new CorrectMainTypeNameProposal(cu, context, currTypeName, newTypeName, IProposalRelevance.RENAME_TYPE));
		}

		if (!hasOtherPublicTypeBefore) {
			String newCUName= JavaModelUtil.getRenamedCUName(cu, currTypeName);
			ICompilationUnit newCU= ((IPackageFragment) (cu.getParent())).getCompilationUnit(newCUName);
			if (!newCU.exists() && !isLinked && !JavaConventions.validateCompilationUnitName(newCUName, sourceLevel, compliance).matches(IStatus.ERROR)) {
				RenameCompilationUnitChange change= new RenameCompilationUnitChange(cu, newCUName);

				// rename CU
				String label= Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_renamecu_description, BasicElementLabels.getResourceName(newCUName));
				proposals.add(new ChangeCorrectionProposal(label, change, IProposalRelevance.RENAME_CU, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_RENAME)));
			}
		}
	}

	public static void getWrongPackageDeclNameProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();
		boolean isLinked= cu.getResource().isLinked();

		// correct package declaration
		int relevance= cu.getPackageDeclarations().length == 0 ? IProposalRelevance.MISSING_PACKAGE_DECLARATION : IProposalRelevance.CORRECT_PACKAGE_DECLARATION; // bug 38357
		if (CorrectPackageDeclarationProposalCore.isValidProposal(cu)) {
			proposals.add(new CorrectPackageDeclarationProposal(cu, problem, relevance));
		}

		// move to package
		IPackageDeclaration[] packDecls= cu.getPackageDeclarations();
		String newPackName= packDecls.length > 0 ? packDecls[0].getElementName() : ""; //$NON-NLS-1$

		IPackageFragmentRoot root= JavaModelUtil.getPackageFragmentRoot(cu);
		IPackageFragment newPack= root.getPackageFragment(newPackName);

		ICompilationUnit newCU= newPack.getCompilationUnit(cu.getElementName());
		if (!newCU.exists() && !isLinked) {
			String label;
			if (newPack.isDefaultPackage()) {
				label= Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_movecu_default_description, BasicElementLabels.getFileName(cu));
			} else {
				String packageLabel= JavaElementLabels.getElementLabel(newPack, JavaElementLabels.ALL_DEFAULT);
				label= Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_movecu_description, new Object[] { BasicElementLabels.getFileName(cu), packageLabel });
			}
			CompositeChange composite= new CompositeChange(label);
			composite.add(new CreatePackageChange(newPack));
			composite.add(new MoveCompilationUnitChange(cu, newPack));

			proposals.add(new ChangeCorrectionProposal(label, composite, IProposalRelevance.MOVE_CU_TO_PACKAGE, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_MOVE)));
		}
	}

	public static void removeImportStatementProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) {
		IProposableFix fix= UnusedCodeFix.createRemoveUnusedImportFix(context.getASTRoot(), problem);
		if (fix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_DELETE_IMPORT);
			Map<String, String> options= new Hashtable<>();
			options.put(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS, CleanUpOptions.TRUE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new UnusedCodeCleanUp(options), IProposalRelevance.REMOVE_UNUSED_IMPORT, image, context);
			proposals.add(proposal);
		}

		final ICompilationUnit cu= context.getCompilationUnit();
		String name= CorrectionMessages.ReorgCorrectionsSubProcessor_organizeimports_description;
		ChangeCorrectionProposal proposal= new ChangeCorrectionProposal(name, null, IProposalRelevance.ORGANIZE_IMPORTS, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE)) {
			@Override
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

	public static class ClasspathFixCorrectionProposal extends CUCorrectionProposal {

		private final int fOffset;
		private final int fLength;
		private final String fMissingType;

		private TextEdit fResultingEdit;

		public ClasspathFixCorrectionProposal(ICompilationUnit cu, int offset, int length, String missingType) {
			super(CorrectionMessages.ReorgCorrectionsSubProcessor_project_seup_fix_description, cu, -10, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
			fOffset= offset;
			fLength= length;
			fMissingType= missingType;
		}

		@Override
		public void apply(IDocument document) {
			IRunnableContext context= JavaPlugin.getActiveWorkbenchWindow();
			if (context == null) {
				context= new BusyIndicatorRunnableContext();
			}
			Shell shell= JavaPlugin.getActiveWorkbenchShell();
			if (ClasspathFixSelectionDialog.openClasspathFixSelectionDialog(shell, getCompilationUnit().getJavaProject(), fMissingType, context)) {
				if (fMissingType.indexOf('.') == -1) {
					try {
						IChooseImportQuery query= AddImportOnSelectionAction.newDialogQuery(shell);
						AddImportsOperation op= new AddImportsOperation(getCompilationUnit(), fOffset, fLength, query, false, false);
						IProgressService progressService= PlatformUI.getWorkbench().getProgressService();
						progressService.runInUI(context, new WorkbenchRunnableAdapter(op, op.getScheduleRule()), op.getScheduleRule());
						fResultingEdit= op.getResultingEdit();
						super.apply(document);
					} catch (InvocationTargetException e) {
						JavaPlugin.log(e);
					} catch (InterruptedException e) {
						// ignore
					}
				}
			}
		}

		@Override
		protected boolean useDelegateToCreateTextChange() {
			return false;
		}

		@Override
		protected void addEdits(IDocument document, TextEdit editRoot) throws CoreException {
			if (fResultingEdit != null) {
				editRoot.addChild(fResultingEdit);
			}
		}

		@Override
		public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
			return Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_project_seup_fix_info, BasicElementLabels.getJavaElementName(fMissingType));
		}
	}

	public static void addProjectSetupFixProposal(IInvocationContext context, IProblemLocationCore problem, String missingType, Collection<ICommandAccess> proposals) {
		proposals.add(new ClasspathFixCorrectionProposal(context.getCompilationUnit(), problem.getOffset(), problem.getLength(), missingType));
	}

	/* answers false if the problem location is not an import declaration, and hence no proposal have been added. */
	public static boolean importNotFoundProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();

		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return false;
		}
		ImportDeclaration importDeclaration= (ImportDeclaration) ASTNodes.getParent(selectedNode, ASTNode.IMPORT_DECLARATION);
		if (importDeclaration == null) {
			return false;
		}
		if (!importDeclaration.isOnDemand()) {
			Name name= importDeclaration.getName();
			if (importDeclaration.isStatic() && name.isQualifiedName()) {
				name= ((QualifiedName) name).getQualifier();
			}
			int kind= JavaModelUtil.is50OrHigher(cu.getJavaProject()) ? TypeKinds.REF_TYPES : TypeKinds.CLASSES | TypeKinds.INTERFACES;
			UnresolvedElementsSubProcessor.addRequiresModuleProposals(cu, name, IProposalRelevance.IMPORT_NOT_FOUND_ADD_REQUIRES_MODULE, proposals, false);
			UnresolvedElementsSubProcessor.addNewTypeProposals(cu, name, kind, IProposalRelevance.IMPORT_NOT_FOUND_NEW_TYPE, proposals);
		} else {
			Name name= importDeclaration.getName();
			UnresolvedElementsSubProcessor.addRequiresModuleProposals(cu, name, IProposalRelevance.IMPORT_NOT_FOUND_ADD_REQUIRES_MODULE, proposals, true);
		}
		String name= ASTNodes.asString(importDeclaration.getName());
		if (importDeclaration.isOnDemand()) {
			name= JavaModelUtil.concatenateName(name, "*"); //$NON-NLS-1$
		}
		addProjectSetupFixProposal(context, problem, name, proposals);
		return true;
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
		@Override
		public void apply(IDocument document) {
			Map<Object, Object> data= null;
			if (fReferencedType != null) {
				IJavaElement elem= fReferencedType.getJavaElement();
				if (elem != null) {
					IPackageFragmentRoot root= (IPackageFragmentRoot) elem.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
					if (root != null) {
						try {
							IClasspathEntry entry= root.getRawClasspathEntry();
							if (entry != null) {
								data= new HashMap<>(1);
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
		/*
		 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension5#getAdditionalProposalInfo(org.eclipse.core.runtime.IProgressMonitor)
		 * @since 3.5
		 */
		@Override
		public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
			return Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_configure_buildpath_description, BasicElementLabels.getResourceName(fProject));
		}
	}

	private static final class ChangeToRequiredCompilerCompliance extends ChangeCorrectionProposal implements IWorkspaceRunnable {

		private final IJavaProject fProject;
		private final boolean fChangeOnWorkspace;
		private final String fRequiredVersion;
		private final boolean fEnablePreviews;

		private Job fUpdateJob;
		private boolean fRequiredJREFound;

		public ChangeToRequiredCompilerCompliance(String name, IJavaProject project, boolean changeOnWorkspace, String requiredVersion, int relevance) {
			this(name, project, changeOnWorkspace, requiredVersion, false, relevance);
		}

		public ChangeToRequiredCompilerCompliance(String name, IJavaProject project, boolean changeOnWorkspace, String requiredVersion, boolean enablePreviews, int relevance) {
			super(name, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
			fProject= project;
			fChangeOnWorkspace= changeOnWorkspace;
			fRequiredVersion= requiredVersion;
			fUpdateJob= null;
			fRequiredJREFound= false;
			fEnablePreviews= enablePreviews;
		}

		private boolean isRequiredOrGreaterVMInstall(IVMInstall install) {
			if (install instanceof IVMInstall2) {
				String compliance= JavaModelUtil.getCompilerCompliance((IVMInstall2) install, JavaCore.VERSION_1_3);
				return !JavaModelUtil.isVersionLessThan(compliance, fRequiredVersion);
			}
			return false;
		}

		@Override
		public void run(IProgressMonitor monitor) throws CoreException {
			boolean needsBuild= updateJRE(monitor);
			if (needsBuild) {
				fUpdateJob= CoreUtility.getBuildJob(fChangeOnWorkspace ? null : fProject.getProject());
			}
		}

		private boolean updateJRE( IProgressMonitor monitor) throws CoreException, JavaModelException {
			// Caveat: Returns true iff the classpath has not been changed.
			// If the classpath is changed, JDT Core triggers a build for free.
			// If the classpath is not changed, we have to trigger a build because we changed
			// the compiler compliance in #apply(IDocument).
			try {
				if (fChangeOnWorkspace) {
					IVMInstall vmInstall= ClasspathVMUtil.findRequiredOrGreaterVMInstall(fRequiredVersion, false, false);
					fRequiredJREFound= vmInstall != null;
					if (vmInstall != null) {
						IVMInstall install= JavaRuntime.getVMInstall(fProject); // can be null
						monitor.beginTask(CorrectionMessages.ReorgCorrectionsSubProcessor_50_compliance_operation, 4);
						IVMInstall defaultVM= JavaRuntime.getDefaultVMInstall(); // can be null
						if (defaultVM != null && !defaultVM.equals(install)) {
							IPath newPath= new Path(JavaRuntime.JRE_CONTAINER);
							ClasspathVMUtil.updateClasspath(newPath, fProject, Progress.subMonitor(monitor, 1));
						} else {
							monitor.worked(1);
						}
						if (defaultVM == null || !isRequiredOrGreaterVMInstall(defaultVM)) {
							JavaRuntime.setDefaultVMInstall(vmInstall, Progress.subMonitor(monitor, 3), true);
							return false;
						}
						return true;
					}

				} else {
					IExecutionEnvironment bestEE= ClasspathVMUtil.findBestMatchingEE(fRequiredVersion);
					fRequiredJREFound= bestEE != null;
					if (bestEE != null) {
						IPath newPath= JavaRuntime.newJREContainerPath(bestEE);
						boolean classpathUpdated= ClasspathVMUtil.updateClasspath(newPath, fProject, monitor);
						return !classpathUpdated;
					}
				}
			} finally {
				monitor.done();
			}
			return true;
		}

		@Override
		public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
			StringBuilder message= new StringBuilder();
			if (fChangeOnWorkspace) {
				message.append(Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_required_compliance_changeworkspace_description, fRequiredVersion));
			} else {
				message.append(Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_required_compliance_changeproject_description, fRequiredVersion));
			}

			try {
				IVMInstall install= JavaRuntime.getVMInstall(fProject); // can be null
				if (fChangeOnWorkspace) {
					IVMInstall vmInstall= ClasspathVMUtil.findRequiredOrGreaterVMInstall(fRequiredVersion, false, false);
					if (vmInstall != null) {
						IVMInstall defaultVM= JavaRuntime.getDefaultVMInstall(); // can be null
						if (defaultVM != null && !defaultVM.equals(install)) {
							message.append(CorrectionMessages.ReorgCorrectionsSubProcessor_50_compliance_changeProjectJREToDefault_description);
						}
						if (defaultVM == null || !isRequiredOrGreaterVMInstall(defaultVM)) {
							message.append(Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_50_compliance_changeWorkspaceJRE_description, vmInstall.getName()));
						}
					}
				} else {
					IExecutionEnvironment bestEE= ClasspathVMUtil.findBestMatchingEE(fRequiredVersion);
					if (bestEE != null) {
						if (install == null || !isEEOnClasspath(bestEE)) {
							message.append(Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_50_compliance_changeProjectJRE_description, bestEE.getId()));
						}
					}
				}
			} catch (CoreException e) {
				// ignore
			}
			if (fEnablePreviews) {
				if (fChangeOnWorkspace) {
					message.append(CorrectionMessages.PreviewFeaturesSubProcessor_enable_preview_features_workspace_info);
				} else {
					message.append(CorrectionMessages.PreviewFeaturesSubProcessor_enable_preview_features_info);
				}
			}
			return message.toString();
		}

		private boolean isEEOnClasspath(IExecutionEnvironment ee) throws JavaModelException {
			IPath eePath= JavaRuntime.newJREContainerPath(ee);

			for (IClasspathEntry entry: fProject.getRawClasspath()) {
				if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER && entry.getPath().equals(eePath))
					return true;
			}
			return false;
		}

		@Override
		public void apply(IDocument document) {
			if (fChangeOnWorkspace) {
				Hashtable<String, String> map= JavaCore.getOptions();
				JavaModelUtil.setComplianceOptions(map, fRequiredVersion);
				if (fEnablePreviews) {
					map.put(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, JavaCore.ENABLED);
				}
				JavaCore.setOptions(map);
			} else {
				Map<String, String> map= fProject.getOptions(false);
				int optionsCount= map.size();
				JavaModelUtil.setComplianceOptions(map, fRequiredVersion);
				if (map.size() > optionsCount) {
					// options have been added -> ensure that all compliance options from preference page set
					JavaModelUtil.setDefaultClassfileOptions(map, fRequiredVersion);
				}
				if (fEnablePreviews) {
					map.put(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, JavaCore.ENABLED);
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

			if (!fRequiredJREFound) {
				MessageDialog.openInformation(JavaPlugin.getActiveWorkbenchShell(),
						Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_no_required_jre_title, fRequiredVersion),
						Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_no_required_jre_message, fRequiredVersion));
			}
		}
	}

	/**
	 * Adds a proposal to increase the compiler compliance level
	 * @param context the context
	 * @param problem the current problem
	 * @param proposals the resulting proposals
	 * @param requiredVersion the minimal required Java compiler version
	 */
	public static void getNeedHigherComplianceProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals, String requiredVersion) {
		getNeedHigherComplianceProposals(context, problem, proposals, false, requiredVersion);
	}

	public static void getNeedHigherComplianceProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) {
		String[] args= problem.getProblemArguments();
		if (args != null && args.length == 2) {
			ReorgCorrectionsSubProcessor.getNeedHigherComplianceProposals(context, problem, proposals, false, args[1]);
		}
	}

	/**
	 * Adds a proposal to increase the compiler compliance level as well as set --enable-previews
	 * option.
	 *
	 * @param context the context
	 * @param problem the current problem
	 * @param proposals the resulting proposals
	 * @param enablePreviews --enable-previews option will be enabled if set to true
	 * @param requiredVersion the minimal required Java compiler version
	 */
	static void getNeedHigherComplianceProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals, boolean enablePreviews, String requiredVersion) {
		IJavaProject project= context.getCompilationUnit().getJavaProject();
		String label1= Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_change_project_compliance_description, requiredVersion);
		if (enablePreviews) {
			label1= Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_combine_two_quickfixes, new String[] {label1, CorrectionMessages.PreviewFeaturesSubProcessor_enable_preview_features});
			proposals.add(new ChangeToRequiredCompilerCompliance(label1, project, false, requiredVersion, enablePreviews, IProposalRelevance.CHANGE_PROJECT_COMPLIANCE));
		} else {
			proposals.add(new ChangeToRequiredCompilerCompliance(label1, project, false, requiredVersion, IProposalRelevance.CHANGE_PROJECT_COMPLIANCE));
		}


		if (project.getOption(JavaCore.COMPILER_COMPLIANCE, false) == null) {
			String label2= Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_change_workspace_compliance_description, requiredVersion);
			if (enablePreviews) {
				label2= Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_combine_two_quickfixes, new String[] {label2, CorrectionMessages.PreviewFeaturesSubProcessor_enable_preview_features_workspace});
			}
			proposals.add(new ChangeToRequiredCompilerCompliance(label2, project, true, requiredVersion, enablePreviews, IProposalRelevance.CHANGE_WORKSPACE_COMPLIANCE));
		}
	}

	/**
	 * Adds a proposal that opens the build path dialog
	 * @param context the context
	 * @param problem the current problem
	 * @param proposals the resulting proposals
	 */
	public static void getIncorrectBuildPathProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) {
		IProject project= context.getCompilationUnit().getJavaProject().getProject();
		String label= CorrectionMessages.ReorgCorrectionsSubProcessor_configure_buildpath_label;
		OpenBuildPathCorrectionProposal proposal= new OpenBuildPathCorrectionProposal(project, label, IProposalRelevance.CONFIGURE_BUILD_PATH, null);
		proposals.add(proposal);
	}

	public static void getAccessRulesProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) {
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
			OpenBuildPathCorrectionProposal proposal= new OpenBuildPathCorrectionProposal(project, label, IProposalRelevance.CONFIGURE_ACCESS_RULES, referencedElement);
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

	private ReorgCorrectionsSubProcessor() {
	}


}
