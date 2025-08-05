/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
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
 *     Benjamin Muskalla <bmuskalla@innoopract.com> - [quick fix] 'Remove invalid modifiers' does not appear for enums and annotations - https://bugs.eclipse.org/bugs/show_bug.cgi?id=110589
 *     Benjamin Muskalla <b.muskalla@gmx.net> - [quick fix] Quick fix for missing synchronized modifier - https://bugs.eclipse.org/bugs/show_bug.cgi?id=245250
 *     Rabea Gransberger <rgransberger@gmx.de> - [quick fix] Fix several visibility issues - https://bugs.eclipse.org/394692
 *     Microsoft Corporation - split into ModifierCorrectionSubProcessorCore in core manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.IProposableFix;
import org.eclipse.jdt.internal.corext.fix.Java50FixCore;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposalCore;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.fix.Java50CleanUp;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ModifierChangeCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ModifierChangeCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewDefiningMethodProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewDefiningMethodProposalCore;

public class ModifierCorrectionSubProcessor extends ModifierCorrectionSubProcessorCore<ICommandAccess> {

	public static void addNonAccessibleReferenceProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals, int kind, int relevance) throws CoreException {
		new ModifierCorrectionSubProcessor().getNonAccessibleReferenceProposal(context, problem, proposals, kind, relevance);
	}

	public static void addChangeOverriddenModifierProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals, int kind) throws JavaModelException {
		new ModifierCorrectionSubProcessor().getChangeOverriddenModifierProposal(context, problem, proposals, kind);
	}

	public static void addNonFinalLocalProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new ModifierCorrectionSubProcessor().getNonFinalLocalProposal(context, problem, proposals);
	}

	public static void addRemoveInvalidModifiersProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals, int relevance) {
		new ModifierCorrectionSubProcessor().getRemoveInvalidModifiersProposal(context, problem, proposals, relevance);
	}

	public static void addMethodRequiresBodyProposals (IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new ModifierCorrectionSubProcessor().getMethodRequiresBodyProposals(context, problem, proposals);
	}

	public static void addAbstractMethodProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new ModifierCorrectionSubProcessor().getAbstractMethodProposals(context, problem, proposals);
	}

	public static void addAbstractTypeProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new ModifierCorrectionSubProcessor().getAbstractTypeProposals(context, problem, proposals);
	}

	public static void addNativeMethodProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new ModifierCorrectionSubProcessor().getNativeMethodProposals(context, problem, proposals);
	}

	public static void addNeedToEmulateProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new ModifierCorrectionSubProcessor().getNeedToEmulateProposal(context, problem, proposals);
	}

	public static void addOverrideAnnotationProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		IProposableFix fix= Java50FixCore.createAddOverrideAnnotationFix(context.getASTRoot(), problem);
		if (fix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			Map<String, String> options= new Hashtable<>();
			options.put(CleanUpConstants.ADD_MISSING_ANNOTATIONS, CleanUpOptions.TRUE);
			options.put(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE, CleanUpOptions.TRUE);
			options.put(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION, CleanUpOptions.TRUE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new Java50CleanUp(options), IProposalRelevance.ADD_OVERRIDE_ANNOTATION, image, context);
			proposals.add(proposal);
		}
	}

	public static void addDeprecatedAnnotationProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		IProposableFix fix= Java50FixCore.createAddDeprectatedAnnotation(context.getASTRoot(), problem);
		if (fix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			Map<String, String> options= new Hashtable<>();
			options.put(CleanUpConstants.ADD_MISSING_ANNOTATIONS, CleanUpOptions.TRUE);
			options.put(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED, CleanUpOptions.TRUE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new Java50CleanUp(options), IProposalRelevance.ADD_DEPRECATED_ANNOTATION, image, context);
			proposals.add(proposal);
		}
	}

	public static void addOverridingDeprecatedMethodProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new ModifierCorrectionSubProcessor().getOverridingDeprecatedMethodProposal(context, problem, proposals);
	}

	public static void removeOverrideAnnotationProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
		new ModifierCorrectionSubProcessor().getRemoveOverrideAnnotationProposal(context, problem, proposals);
	}

	public static void addSynchronizedMethodProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		addAddMethodModifierProposal(context, problem, proposals, Modifier.SYNCHRONIZED, CorrectionMessages.ModifierCorrectionSubProcessor_addsynchronized_description);
	}

	public static void addStaticMethodProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		addAddMethodModifierProposal(context, problem, proposals, Modifier.STATIC, CorrectionMessages.ModifierCorrectionSubProcessor_addstatic_description);
	}

	private static void addAddMethodModifierProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals, int modifier, String label) {
		new ModifierCorrectionSubProcessor().getAddMethodModifierProposal(context, problem, proposals, modifier, label);
	}

	public static void addSealedMissingModifierProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new ModifierCorrectionSubProcessor().getSealedMissingModifierProposal(context, problem, proposals);
	}

	@Override
	protected ICommandAccess astRewriteCorrectionProposalToT(ASTRewriteCorrectionProposalCore core, int uid) {
		return new ASTRewriteCorrectionProposal(core, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
	}

	@Override
	protected ICommandAccess modifierChangeCorrectionProposalCoreToT(ModifierChangeCorrectionProposalCore core, int uid) {
		return new ModifierChangeCorrectionProposal(core, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
	}

	@Override
	protected ICommandAccess fixCorrectionProposalCoreToT(FixCorrectionProposalCore core, int uid) {
		return new FixCorrectionProposal(core, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
	}

	@Override
	protected void collectConstructorProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		try {
			new UnresolvedElementsSubProcessor().collectConstructorProposals(context, problem, proposals);
		} catch (CoreException e) {
		}
	}

	@Override
	protected void getVariableProposals(IInvocationContext context, IProblemLocation problem, IVariableBinding bindingDecl, Collection<ICommandAccess> proposals) {
		try {
			new UnresolvedElementsSubProcessor().collectVariableProposals(context, problem, bindingDecl, proposals);
		} catch (CoreException e) {
		}
	}

	@Override
	protected ICommandAccess newDefiningMethodProposalCoreToT(NewDefiningMethodProposalCore core, int uid) {
		return new NewDefiningMethodProposal(core.getName(), core.getCompilationUnit(), core.getInvocationNode(), core.getSenderBinding(), core.getMethodBinding(), core.getParameterNames(), false, core.getRelevance());
	}

}
