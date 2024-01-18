package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposalCore;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public class AddImportCorrectionProposalCore extends ASTRewriteCorrectionProposalCore {

	static String JAVA_BASE= "java.base"; //$NON-NLS-1$

	private final String fTypeName;

	private final String fQualifierName;

	protected AddModuleRequiresCorrectionProposalCore fAdditionalProposal= null;

	public AddImportCorrectionProposalCore(String name, ICompilationUnit cu, int relevance, String qualifierName, String typeName, SimpleName node) {
		super(name, cu, ASTRewrite.create(node.getAST()), relevance);
		fTypeName= typeName;
		fQualifierName= qualifierName;
		fAdditionalProposal= getAdditionalChangeCorrectionProposal();
	}

	public String getQualifiedTypeName() {
		return fQualifierName + '.' + fTypeName;
	}

	public AddModuleRequiresCorrectionProposalCore getAdditionalProposal() {
		return fAdditionalProposal;
	}

	public AddModuleRequiresCorrectionProposalCore getAdditionalChangeCorrectionProposal() {
		ICompilationUnit cu= getCompilationUnit();
		AddModuleRequiresCorrectionProposalCore additionalChangeCorrectionProposal= null;
		IJavaProject currentJavaProject= cu.getJavaProject();
		if (currentJavaProject == null || !JavaModelUtil.is9OrHigher(currentJavaProject)) {
			return null;
		}
		IModuleDescription currentModuleDescription= null;
		try {
			currentModuleDescription= currentJavaProject.getModuleDescription();
		} catch (JavaModelException e1) {
			//DO NOTHING
		}
		if (currentModuleDescription == null) {
			return null;
		}
		ICompilationUnit currentModuleCompilationUnit= currentModuleDescription.getCompilationUnit();
		if (currentModuleCompilationUnit == null || !currentModuleCompilationUnit.exists()) {
			return null;
		}

		String qualifiedName= getQualifiedTypeName();
		List<IPackageFragment> packageFragments= AddModuleRequiresCorrectionProposalCore.getPackageFragmentsOfMatchingTypesImpl(qualifiedName, IJavaSearchConstants.TYPE, currentJavaProject);
		IPackageFragment enclosingPackage= null;
		if (packageFragments.size() == 1) {
			enclosingPackage= packageFragments.get(0);
		}
		if (enclosingPackage != null) {
			IModuleDescription projectModule= null;
			if (enclosingPackage.isReadOnly()) {
				IPackageFragmentRoot root= (IPackageFragmentRoot) enclosingPackage.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
				projectModule= AddModuleRequiresCorrectionProposalCore.getModuleDescription(root);
			} else {
				IJavaProject project= enclosingPackage.getJavaProject();
				projectModule= AddModuleRequiresCorrectionProposalCore.getModuleDescription(project);
			}
			if (projectModule != null && ((projectModule.exists() && !projectModule.equals(currentModuleDescription))
					|| projectModule.isAutoModule()) && !AddImportCorrectionProposalCore.JAVA_BASE.equals(projectModule.getElementName())) {
				String moduleName= projectModule.getElementName();
				String[] args= { moduleName };
				final String changeName= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_add_requires_module_info, args);
				final String changeDescription= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_add_requires_module_description, args);
				additionalChangeCorrectionProposal= new AddModuleRequiresCorrectionProposalCore(moduleName, changeName, changeDescription, currentModuleCompilationUnit, getRelevance());
			}
		}
		return additionalChangeCorrectionProposal;
	}
}