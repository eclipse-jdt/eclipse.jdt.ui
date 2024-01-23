/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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
 *     Renaud Waldura &lt;renaud+eclipse@waldura.com&gt; - New class/interface with wizard
 *     Rabea Gransberger <rgransberger@gmx.de> - [quick fix] Fix several visibility issues - https://bugs.eclipse.org/394692
 *     Jens Reimann <jreimann@redhat.com> Bug 38201: [quick assist] Allow creating abstract method - https://bugs.eclipse.org/38201
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import org.eclipse.ltk.core.refactoring.Change;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.manipulation.ChangeCorrectionProposalCore;
import org.eclipse.jdt.core.manipulation.TypeKinds;

import org.eclipse.jdt.internal.corext.util.QualifiedTypeNameHistory;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposalCore;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ChangeCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.proposals.AddArgumentCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.AddArgumentCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.AddTypeParameterProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.AddTypeParameterProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CastCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CastCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ILinkedCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewAnnotationMemberProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewAnnotationMemberProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewCUUsingWizardProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewMethodCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewMethodCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewVariableCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewVariableCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.QualifyTypeProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.QualifyTypeProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.RenameNodeCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.RenameNodeCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ReplaceCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ReplaceCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

public class UnresolvedElementsSubProcessor extends UnresolvedElementsBaseSubProcessor<ICommandAccess> {
	UnresolvedElementsSubProcessor() {
	}

	private static Image findImage(int id) {
		switch( id) {
			case 100:
			case 101:
			case 400:
				return JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
			case 102:
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
			case 200:
			case 211:
			case 221:
				return JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PUBLIC);
			case 210:
			case 220:
				return JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PRIVATE);
			case 310:
			case 830:
			case 910:
				return JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			case 820:
			case 1310:
			case 2200:
				return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_IMPDECL);
			case 1410:
			case 1430:
			case 1475:
			case 2401:
				return JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC);
			case 1420:
				return JavaPluginImages.get(JavaPluginImages.IMG_MISC_PRIVATE);
			case 1480:
				return JavaPluginImages.get(JavaPluginImages.IMG_MISC_PROTECTED);
			case 1650:
				return JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CAST);
			case 1700:
			case 1850:
				return JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD);
			case 1750:
			case 1810:
				return JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_REMOVE);
			case 1950:
			case 1970:
			case 1980:
			case 2000:
				return JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			case 2100:
				return JavaElementImageProvider.getDecoratedImage(JavaPluginImages.DESC_MISC_PUBLIC, JavaElementImageDescriptor.CONSTRUCTOR, JavaElementImageProvider.SMALL_SIZE);

		}
		return null;
	}

	public static void getVariableProposals(IInvocationContextCore context, IProblemLocationCore problem, IVariableBinding resolvedField, Collection<ICommandAccess> proposals) throws CoreException {
		new UnresolvedElementsSubProcessor().collectVariableProposals(context, problem, resolvedField, proposals);
	}


	public static void getTypeProposals(IInvocationContextCore context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) throws CoreException {
		new UnresolvedElementsSubProcessor().collectTypeProposals(context, problem, proposals);
	}

	public static void addNewTypeProposals(ICompilationUnit cu, Name refNode, int kind, int relevance, Collection<ICommandAccess> proposals) throws CoreException {
		new UnresolvedElementsSubProcessor().collectNewTypeProposals(cu, refNode, kind, relevance, proposals);
	}

	public static void addRequiresModuleProposals(ICompilationUnit cu, Name node, int relevance, Collection<ICommandAccess> proposals, boolean isOnDemand) throws CoreException {
		new UnresolvedElementsSubProcessor().collectRequiresModuleProposals(cu, node, relevance, proposals, isOnDemand);
	}

	public static void getMethodProposals(IInvocationContextCore context, IProblemLocationCore problem, boolean isOnlyParameterMismatch, Collection<ICommandAccess> proposals) throws CoreException {
		new UnresolvedElementsSubProcessor().collectMethodProposals(context, problem, isOnlyParameterMismatch, proposals);
	}

	public static void getConstructorProposals(IInvocationContextCore context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) throws CoreException {
		new UnresolvedElementsSubProcessor().collectConstructorProposals(context, problem, proposals);
	}

	public static void getAmbiguosTypeReferenceProposals(IInvocationContextCore context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) throws CoreException {
		new UnresolvedElementsSubProcessor().collectAmbiguosTypeReferenceProposals(context, problem, proposals);
	}

	public static void getArrayAccessProposals(IInvocationContextCore context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) {
		new UnresolvedElementsSubProcessor().collectArrayAccessProposals(context, problem, proposals);
	}

	public static void getAnnotationMemberProposals(IInvocationContextCore context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) throws CoreException {
		new UnresolvedElementsSubProcessor().collectAnnotationMemberProposals(context, problem, proposals);
	}

	@Override
	protected ReorgCorrectionsBaseSubProcessor<ICommandAccess> getReorgSubProcessor() {
		return new ReorgCorrectionsSubProcessor();
	}
	@Override
	protected TypeMismatchBaseSubProcessor<ICommandAccess> getTypeMismatchSubProcessor() {
		return new TypeMismatchSubProcessor();
	}

	@Override
	protected ChangeCorrectionProposalCore getOriginalProposalFromT(ICommandAccess proposal) {
		if( proposal instanceof CUCorrectionProposal) {
			return ((CUCorrectionProposal)proposal).getDelegate();
		}
		if( proposal instanceof ChangeCorrectionProposalCore) {
			return (ChangeCorrectionProposalCore)proposal;
		}
		return null;
	}

	@Override
	protected ILinkedCorrectionProposalCore createLinkedCorrectionProposal(String label, ICompilationUnit cu, ASTRewrite rewrite, int relevance, int uid) {
		return new LinkedCorrectionProposal(label, cu, rewrite, relevance, findImage(uid));
	}

	@Override
	protected ICommandAccess newVariableCorrectionProposalToT(NewVariableCorrectionProposalCore core, int uid) {
		return new NewVariableCorrectionProposal(core, findImage(uid));
	}

	@Override
	protected ICommandAccess renameNodeCorrectionProposalToT(RenameNodeCorrectionProposalCore core, int uid) {
		return new RenameNodeCorrectionProposal(core);
	}

	@Override
	protected ICommandAccess compositeProposalToT(ChangeCorrectionProposalCore compositeProposal, int uid) {
		return changeCorrectionProposalToT(compositeProposal, uid);
	}

	@Override
	protected int getQualifiedTypeNameHistoryBoost(String qualifiedName, int min, int max) {
		return QualifiedTypeNameHistory.getBoost(qualifiedName, min, max);
	}

	@Override
	protected ICommandAccess linkedProposalToT(ILinkedCorrectionProposalCore proposal, int uid) {
		if( proposal instanceof LinkedCorrectionProposal ) {
			return (LinkedCorrectionProposal)proposal;
		}
		if( proposal instanceof LinkedCorrectionProposalCore) {
			return new LinkedCorrectionProposal((LinkedCorrectionProposalCore)proposal, findImage(uid));
		}
		return null;
	}

	@Override
	protected ICommandAccess changeCorrectionProposalToT(final ChangeCorrectionProposalCore proposal, int uid) {
		ChangeCorrectionProposal ret= new ChangeCorrectionProposal(proposal.getName(), null, proposal.getRelevance()) {
			@Override
			protected Change createChange() throws CoreException {
				return proposal.getChange();
			}
		};
		ret.setImage(findImage(uid));
		return ret;
	}

	@Override
	protected ICommandAccess qualifyTypeProposalToT(QualifyTypeProposalCore proposal, int uid) {
		return new QualifyTypeProposal(proposal);
	}

	@Override
	protected ICommandAccess addTypeParametersToT(AddTypeParameterProposalCore proposal, int uid) {
		return new AddTypeParameterProposal(proposal);
	}

	@Override
	protected ICommandAccess addModuleRequiresProposalToT(ChangeCorrectionProposalCore proposal, int uid) {
		return null;
	}

	@Override
	protected ICommandAccess replaceCorrectionProposalToT(ReplaceCorrectionProposalCore proposal, int uid) {
		return new ReplaceCorrectionProposal(proposal);
	}

	@Override
	protected ICommandAccess castCorrectionProposalToT(CastCorrectionProposalCore c, int uid) {
		return new CastCorrectionProposal(c);
	}

	@Override
	protected ICommandAccess addArgumentCorrectionProposalToT(AddArgumentCorrectionProposalCore proposal, int uid) {
		return new AddArgumentCorrectionProposal(proposal);
	}

	@Override
	protected ICommandAccess changeMethodSignatureProposalToT(ChangeMethodSignatureProposalCore proposal, int uid) {
		return new ChangeMethodSignatureProposal(proposal, findImage(uid));
	}

	@Override
	protected ICommandAccess newMethodProposalToT(NewMethodCorrectionProposalCore core, int uid) {
		return new NewMethodCorrectionProposal(core, findImage(uid));
	}

	@Override
	protected ICommandAccess rewriteProposalToT(ASTRewriteCorrectionProposalCore proposal, int uid) {
		return new ASTRewriteCorrectionProposal(proposal, findImage(uid));
	}

	@Override
	protected ICommandAccess newAnnotationProposalToT(NewAnnotationMemberProposalCore core, int uid) {
		return new NewAnnotationMemberProposal(core, findImage(uid));
	}

	@Override
	protected ICommandAccess renameNodeProposalToT(RenameNodeCorrectionProposalCore core, int uid) {
		return new RenameNodeCorrectionProposal(core);
	}

	@Override
	protected void addNewTypeProposalsInteractiveInnerLoop(ICompilationUnit cu, Name node, IJavaElement enclosing, int rel, int kind, Name refNode, Collection<ICommandAccess> proposals) throws CoreException {
		if ((kind & TypeKinds.CLASSES) != 0) {
			proposals.add(new NewCUUsingWizardProposal(cu, node, NewCUUsingWizardProposal.K_CLASS, enclosing, rel+3));
			if (canUseRecord(cu.getJavaProject(), refNode)) {
				proposals.add(new NewCUUsingWizardProposal(cu, node, NewCUUsingWizardProposal.K_RECORD, enclosing, rel + 3));
			}
		}
		if ((kind & TypeKinds.INTERFACES) != 0) {
			proposals.add(new NewCUUsingWizardProposal(cu, node, NewCUUsingWizardProposal.K_INTERFACE, enclosing, rel+2));
		}
		if ((kind & TypeKinds.ENUMS) != 0) {
			proposals.add(new NewCUUsingWizardProposal(cu, node, NewCUUsingWizardProposal.K_ENUM, enclosing, rel));
		}
		if ((kind & TypeKinds.ANNOTATIONS) != 0) {
			proposals.add(new NewCUUsingWizardProposal(cu, node, NewCUUsingWizardProposal.K_ANNOTATION, enclosing, rel + 1));
			addNullityAnnotationTypesProposals(cu, node, proposals);
		}

	}
}
