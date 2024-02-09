/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.core.resources.IProject;

import org.eclipse.ltk.core.refactoring.Change;
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

import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.IProposableFix;
import org.eclipse.jdt.internal.corext.fix.UnusedCodeFixCore;
import org.eclipse.jdt.internal.corext.refactoring.changes.CreatePackageChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.MoveCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameCompilationUnitChange;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.fix.UnusedCodeCleanUpCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CorrectPackageDeclarationProposalCore;

public abstract class ReorgCorrectionsBaseSubProcessor<T> {

	protected ReorgCorrectionsBaseSubProcessor() {
	}


	public void addWrongTypeNameProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		ICompilationUnit cu= context.getCompilationUnit();
		boolean isLinked= cu.getResource().isLinked();

		IJavaProject javaProject= cu.getJavaProject();
		String sourceLevel= javaProject.getOption(JavaCore.COMPILER_SOURCE, true);
		String compliance= javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true);
		String previewEnabled= javaProject.getOption(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, true);

		CompilationUnit root= context.getASTRoot();

		ASTNode coveredNode= problem.getCoveredNode(root);
		if (!(coveredNode instanceof SimpleName simpleCovered))
			return;

		ASTNode parentType= coveredNode.getParent();
		if (!(parentType instanceof AbstractTypeDeclaration))
			return;

		String currTypeName= simpleCovered.getIdentifier();
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
			T prop1= createCorrectMainTypeNameProposal(cu, context, currTypeName, newTypeName, IProposalRelevance.RENAME_TYPE);
			if (prop1 != null)
				proposals.add(prop1);
		}

		if (!hasOtherPublicTypeBefore) {
			String newCUName= JavaModelUtil.getRenamedCUName(cu, currTypeName);
			ICompilationUnit newCU= ((IPackageFragment) (cu.getParent())).getCompilationUnit(newCUName);
			if (!newCU.exists() && !isLinked && !JavaConventions.validateCompilationUnitName(newCUName, sourceLevel, compliance).matches(IStatus.ERROR)) {
				RenameCompilationUnitChange change= new RenameCompilationUnitChange(cu, newCUName);

				// rename CU
				String label= Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_renamecu_description, BasicElementLabels.getResourceName(newCUName));
				T p2= createRenameCUProposal(label, change, IProposalRelevance.RENAME_CU);
				if( p2 != null )
					proposals.add(p2);
			}
		}
	}


	public void addWrongPackageDeclNameProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();
		boolean isLinked= cu.getResource().isLinked();

		// correct package declaration
		int relevance= cu.getPackageDeclarations().length == 0 ? IProposalRelevance.MISSING_PACKAGE_DECLARATION : IProposalRelevance.CORRECT_PACKAGE_DECLARATION; // bug 38357
		if (CorrectPackageDeclarationProposalCore.isValidProposal(cu)) {
			T p1= createCorrectPackageDeclarationProposal(cu, problem, relevance);
			if (p1 != null)
				proposals.add(p1);
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
				String packageLabel= JavaElementLabelsCore.getElementLabel(newPack, JavaElementLabelsCore.ALL_DEFAULT);
				label= Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_movecu_description, new Object[] { BasicElementLabels.getFileName(cu), packageLabel });
			}
			CompositeChange composite= new CompositeChange(label);
			composite.add(new CreatePackageChange(newPack));
			composite.add(new MoveCompilationUnitChange(cu, newPack));

			T p1= createMoveToNewPackageProposal(label, composite, IProposalRelevance.MOVE_CU_TO_PACKAGE);
			if (p1 != null)
				proposals.add(p1);
		}
	}


	public void addRemoveImportStatementProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		IProposableFix fix= UnusedCodeFixCore.createRemoveUnusedImportFix(context.getASTRoot(), problem);
		if (fix != null) {
			Map<String, String> options= new Hashtable<>();
			options.put(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS, CleanUpOptions.TRUE);
			T proposal= createRemoveUnusedImportProposal(fix, new UnusedCodeCleanUpCore(options), IProposalRelevance.REMOVE_UNUSED_IMPORT, context);
			if (proposal != null)
				proposals.add(proposal);
		}

		final ICompilationUnit cu= context.getCompilationUnit();
		String name= CorrectionMessages.ReorgCorrectionsSubProcessor_organizeimports_description;
		T proposal= createOrganizeImportsProposal(name, null, cu, IProposalRelevance.ORGANIZE_IMPORTS);
		if (proposal != null)
			proposals.add(proposal);
	}


	public void addProjectSetupFixProposals(IInvocationContext context, IProblemLocation problem, String missingType, Collection<T> proposals) {
		T prop= createProjectSetupFixProposal(context, problem, missingType, proposals);
		if (prop != null) {
			proposals.add(prop);
		}
	}


	/**
	 * Adds a proposal to increase the compiler compliance level
	 * @param context the context
	 * @param problem the current problem
	 * @param proposals the resulting proposals
	 * @param requiredVersion the minimal required Java compiler version
	 */
	public void addNeedHigherComplianceProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals, String requiredVersion) {
		addNeedHigherComplianceProposals(context, problem, proposals, false, requiredVersion);
	}

	public void addNeedHigherComplianceProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		String[] args= problem.getProblemArguments();
		if (args != null && args.length == 2) {
			addNeedHigherComplianceProposals(context, problem, proposals, false, args[1]);
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
	protected void addNeedHigherComplianceProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals, boolean enablePreviews, String requiredVersion) {
		IJavaProject project= context.getCompilationUnit().getJavaProject();
		String label1= Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_change_project_compliance_description, requiredVersion);
		if (enablePreviews) {
			label1= Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_combine_two_quickfixes, new String[] {label1, CorrectionMessages.PreviewFeaturesSubProcessor_enable_preview_features});
			proposals.add(createChangeToRequiredCompilerComplianceProposal(label1, project, false, requiredVersion, enablePreviews, IProposalRelevance.CHANGE_PROJECT_COMPLIANCE));
		} else {
			proposals.add(createChangeToRequiredCompilerComplianceProposal(label1, project, false, requiredVersion, IProposalRelevance.CHANGE_PROJECT_COMPLIANCE));
		}


		if (project.getOption(JavaCore.COMPILER_COMPLIANCE, false) == null) {
			String label2= Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_change_workspace_compliance_description, requiredVersion);
			if (enablePreviews) {
				label2= Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_combine_two_quickfixes, new String[] {label2, CorrectionMessages.PreviewFeaturesSubProcessor_enable_preview_features_workspace});
			}
			T p1= createChangeToRequiredCompilerComplianceProposal(label2, project, true, requiredVersion, enablePreviews, IProposalRelevance.CHANGE_WORKSPACE_COMPLIANCE);
			if (p1 != null)
				proposals.add(p1);
		}
	}


	/**
	 * Adds a proposal that opens the build path dialog
	 * @param context the context
	 * @param problem the current problem
	 * @param proposals the resulting proposals
	 */
	public void addIncorrectBuildPathProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		IProject project= context.getCompilationUnit().getJavaProject().getProject();
		String label= CorrectionMessages.ReorgCorrectionsSubProcessor_configure_buildpath_label;
		T proposal= createOpenBuildPathCorrectionProposal(project, label, IProposalRelevance.CONFIGURE_BUILD_PATH, null);
		if (proposal != null)
			proposals.add(proposal);
	}

	public void addAccessRulesProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		IBinding referencedElement= null;
		ASTNode node= problem.getCoveredNode(context.getASTRoot());
		if (node instanceof Type type) {
			referencedElement= type.resolveBinding();
		} else if (node instanceof Name name) {
			referencedElement= name.resolveBinding();
		}
		if (referencedElement != null && canModifyAccessRules(referencedElement)) {
			IProject project= context.getCompilationUnit().getJavaProject().getProject();
			String label= CorrectionMessages.ReorgCorrectionsSubProcessor_accessrules_description;
			T proposal= createOpenBuildPathCorrectionProposal(project, label, IProposalRelevance.CONFIGURE_ACCESS_RULES, referencedElement);
			if (proposal != null)
				proposals.add(proposal);
		}
	}


	/* answers false if the problem location is not an import declaration, and hence no proposal have been added. */
	public boolean addImportNotFoundProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();
		UnresolvedElementsBaseSubProcessor<T> unresolvedElements = getUnresolvedElementsSubProcessor();
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
			unresolvedElements.collectRequiresModuleProposals(cu, name, IProposalRelevance.IMPORT_NOT_FOUND_ADD_REQUIRES_MODULE, proposals, false);
			unresolvedElements.collectNewTypeProposals(cu, name, kind, IProposalRelevance.IMPORT_NOT_FOUND_NEW_TYPE, proposals);
		} else {
			Name name= importDeclaration.getName();
			unresolvedElements.collectRequiresModuleProposals(cu, name, IProposalRelevance.IMPORT_NOT_FOUND_ADD_REQUIRES_MODULE, proposals, true);
		}
		String name= ASTNodes.asString(importDeclaration.getName());
		if (importDeclaration.isOnDemand()) {
			name= JavaModelUtil.concatenateName(name, "*"); //$NON-NLS-1$
		}
		addProjectSetupFixProposals(context, problem, name, proposals);
		return true;
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

	public abstract UnresolvedElementsBaseSubProcessor<T> getUnresolvedElementsSubProcessor();

	public abstract T createRenameCUProposal(String label, RenameCompilationUnitChange change, int relevance);

	public abstract T createCorrectMainTypeNameProposal(ICompilationUnit cu, IInvocationContext context, String currTypeName, String newTypeName, int relevance);

	protected abstract T createCorrectPackageDeclarationProposal(ICompilationUnit cu, IProblemLocation problem, int relevance);

	protected abstract T createMoveToNewPackageProposal(String label, CompositeChange composite, int relevance);

	protected abstract T createOrganizeImportsProposal(String name, Change change, ICompilationUnit cu, int relevance);

	protected abstract T createRemoveUnusedImportProposal(IProposableFix fix, UnusedCodeCleanUpCore unusedCodeCleanUp, int relevance, IInvocationContext context);

	public abstract T createProjectSetupFixProposal(IInvocationContext context, IProblemLocation problem, String missingType, Collection<T> proposals);

	protected abstract T createChangeToRequiredCompilerComplianceProposal(String label1, IJavaProject project, boolean changeOnWorkspace, String requiredVersion, int relevance);

	protected abstract T createChangeToRequiredCompilerComplianceProposal(String label2, IJavaProject project, boolean changeOnWorkspace, String requiredVersion, boolean enablePreviews,
			int relevance);

	protected abstract T createOpenBuildPathCorrectionProposal(IProject project, String label, int relevance, IBinding referencedElement);

}
