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
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.TextFileChange;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.manipulation.ChangeCorrectionProposalCore;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTesterCore;
import org.eclipse.jdt.internal.corext.refactoring.sef.SelfEncapsulateFieldRefactoring;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;


public abstract class GetterSetterCorrectionBaseSubProcessor<T> {

	public static final String SELF_ENCAPSULATE_FIELD_COMMAND_ID= "org.eclipse.jdt.ui.correction.encapsulateField.assist"; //$NON-NLS-1$

	private static class ProposalParameter {
		public final boolean useSuper;
		public final ICompilationUnit compilationUnit;
		public final ASTRewrite astRewrite;
		public final Expression accessNode;
		public final Expression qualifier;
		public final IVariableBinding variableBinding;

		public ProposalParameter(boolean useSuper, ICompilationUnit compilationUnit, ASTRewrite rewrite, Expression accessNode, Expression qualifier, IVariableBinding variableBinding) {
			this.useSuper= useSuper;
			this.compilationUnit= compilationUnit;
			this.astRewrite= rewrite;
			this.accessNode= accessNode;
			this.qualifier= qualifier;
			this.variableBinding= variableBinding;
		}
	}

	public static class SelfEncapsulateFieldProposalCore extends ChangeCorrectionProposalCore { // public for tests

		private IField fField;

		public SelfEncapsulateFieldProposalCore(int relevance, IField field) {
			this(relevance, null, field);
		}

		public SelfEncapsulateFieldProposalCore(int relevance, Change change, IField field) {
			super(getDescription(field), change, relevance);
			fField= field;
			setCommandId(SELF_ENCAPSULATE_FIELD_COMMAND_ID);
		}

		public IField getField() {
			return fField;
		}

		public static SelfEncapsulateFieldRefactoring getChangeRefactoring(IField field) throws CoreException {
			final SelfEncapsulateFieldRefactoring refactoring= new SelfEncapsulateFieldRefactoring(field);
			refactoring.setVisibility(Flags.AccPublic);
			refactoring.setConsiderVisibility(false);//private field references are just searched in local file
			refactoring.checkInitialConditions(new NullProgressMonitor());
			refactoring.checkFinalConditions(new NullProgressMonitor());
			return refactoring;
		}

		public static Change getChange(IField field) throws CoreException {
			Refactoring refactoring= getChangeRefactoring(field);
			Change createdChange= refactoring.createChange(new NullProgressMonitor());
			return createdChange;
		}

		public static TextFileChange getChange(IField field, IFile file) throws CoreException {
			Change createdChange= getChange(field);
			if (createdChange instanceof CompositeChange c) {
				for (Change curr : c.getChildren()) {
					if (curr instanceof TextFileChange tfc && (file == null || tfc.getFile().equals(file))) {
						return tfc;
					}
				}
			}
			return null;
		}


		public static String getDescription(IField field) {
			return Messages.format(CorrectionMessages.GetterSetterCorrectionSubProcessor_creategetterunsingencapsulatefield_description, BasicElementLabels.getJavaElementName(field.getElementName()));
		}

		@Override
		public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
			return CorrectionMessages.GetterSetterCorrectionSubProcessor_additional_info;
		}
	}

	protected GetterSetterCorrectionBaseSubProcessor() {
	}

	/**
	 * Used by quick assist
	 * @param context the invocation context
	 * @param coveringNode the covering node
	 * @param locations the problems at the corrent location
	 * @param resultingCollections the resulting proposals
	 * @return <code>true</code> if the quick assist is applicable at this offset
	 */
	public boolean addGetterSetterProposals(IInvocationContext context, ASTNode coveringNode, IProblemLocation[] locations, ArrayList<T> resultingCollections) {
		if (locations != null) {
			for (IProblemLocation location : locations) {
				int problemId= location.getProblemId();
				if (problemId == IProblem.UnusedPrivateField)
					return false;
				if (problemId == IProblem.UnqualifiedFieldAccess)
					return false;
			}
		}
		return addGetterSetterProposals(context, coveringNode, resultingCollections, IProposalRelevance.GETTER_SETTER_QUICK_ASSIST);
	}

	public void addGetterSetterProposals(IInvocationContext context, IProblemLocation location, Collection<T> proposals, int relevance) {
		addGetterSetterProposals(context, location.getCoveringNode(context.getASTRoot()), proposals, relevance);
	}

	protected boolean addGetterSetterProposals(IInvocationContext context, ASTNode coveringNode, Collection<T> proposals, int relevance) {
		if (!(coveringNode instanceof SimpleName sn)) {
			return false;
		}

		IBinding binding= sn.resolveBinding();
		if (!(binding instanceof IVariableBinding variableBinding))
			return false;
		if (!variableBinding.isField())
			return false;

		if (proposals == null)
			return true;

		T proposal= getProposal(context.getCompilationUnit(), sn, variableBinding, relevance);
		if (proposal != null)
			proposals.add(proposal);
		return true;
	}

	private T getProposal(ICompilationUnit cu, SimpleName sn, IVariableBinding variableBinding, int relevance) {
		Expression accessNode= sn;
		Expression qualifier= null;
		boolean useSuper= false;

		ASTNode parent= sn.getParent();
		switch (parent.getNodeType()) {
			case ASTNode.QUALIFIED_NAME:
				accessNode= (Expression) parent;
				qualifier= ((QualifiedName) parent).getQualifier();
				break;
			case ASTNode.SUPER_FIELD_ACCESS:
				accessNode= (Expression) parent;
				qualifier= ((SuperFieldAccess) parent).getQualifier();
				useSuper= true;
				break;
		}
		ASTRewrite rewrite= ASTRewrite.create(sn.getAST());
		ProposalParameter gspc= new ProposalParameter(useSuper, cu, rewrite, accessNode, qualifier, variableBinding);
		if (ASTResolving.isWriteAccess(sn))
			return createSetterProposal(gspc, relevance);
		else
			return createGetterProposal(gspc, relevance);
	}

	protected abstract T createNonNullMethodGetterProposal(String label, ICompilationUnit compilationUnit, ASTRewrite astRewrite, int relevance);

	protected abstract T createFieldGetterProposal(int relevance, IField field);

	/**
	 * Proposes a getter for this field.
	 *
	 * @param context the proposal parameter
	 * @param relevance relevance of this proposal
	 * @return the proposal if available or null
	 */
	private T createGetterProposal(ProposalParameter context, int relevance) {
		IMethodBinding method= findGetter(context);
		if (method != null) {
			Expression mi= createMethodInvocation(context, method, null);
			context.astRewrite.replace(context.accessNode, mi, null);

			String label= Messages.format(CorrectionMessages.GetterSetterCorrectionSubProcessor_replacewithgetter_description, BasicElementLabels.getJavaCodeString(ASTNodes.asString(context.accessNode)));
			return createNonNullMethodGetterProposal(label, context.compilationUnit, context.astRewrite, relevance);
		} else {
			IJavaElement element= context.variableBinding.getJavaElement();
			if (element instanceof IField field) {
				try {
					if (RefactoringAvailabilityTesterCore.isSelfEncapsulateAvailable(field))
						return createFieldGetterProposal(relevance, field);
				} catch (JavaModelException e) {
					JavaManipulationPlugin.log(e);
				}
			}
		}
		return null;
	}

	private static IMethodBinding findGetter(ProposalParameter context) {
		ITypeBinding returnType= context.variableBinding.getType();
		String getterName= GetterSetterUtil.getGetterName(context.variableBinding, context.compilationUnit.getJavaProject(), null, isBoolean(context));
		ITypeBinding declaringType= context.variableBinding.getDeclaringClass();
		if (declaringType == null)
			return null;
		IMethodBinding getter= Bindings.findMethodInHierarchy(declaringType, getterName, new ITypeBinding[0]);
		if (getter != null && getter.getReturnType().isAssignmentCompatible(returnType) && Modifier.isStatic(getter.getModifiers()) == Modifier.isStatic(context.variableBinding.getModifiers()))
			return getter;
		return null;
	}

	private static Expression createMethodInvocation(ProposalParameter context, IMethodBinding method, Expression argument) {
		AST ast= context.astRewrite.getAST();
		Expression qualifier= context.qualifier;
		if (context.useSuper) {
			SuperMethodInvocation invocation= ast.newSuperMethodInvocation();
			invocation.setName(ast.newSimpleName(method.getName()));
			if (qualifier != null)
				invocation.setQualifier((Name) context.astRewrite.createCopyTarget(qualifier));
			if (argument != null)
				invocation.arguments().add(argument);
			return invocation;
		} else {
			MethodInvocation invocation= ast.newMethodInvocation();
			invocation.setName(ast.newSimpleName(method.getName()));
			if (qualifier != null)
				invocation.setExpression((Expression) context.astRewrite.createCopyTarget(qualifier));
			if (argument != null)
				invocation.arguments().add(argument);
			return invocation;
		}
	}

	protected abstract T createMethodSetterProposal(String label, ICompilationUnit compilationUnit, ASTRewrite astRewrite, int relevance);


	protected abstract T createFieldSetterProposal(int relevance, IField field);

	/**
	 * Proposes a setter for this field.
	 *
	 * @param context the proposal parameter
	 * @param relevance relevance of this proposal
	 * @return the proposal if available or null
	 */
	private T createSetterProposal(ProposalParameter context, int relevance) {
		boolean isBoolean= isBoolean(context);
		String setterName= GetterSetterUtil.getSetterName(context.variableBinding, context.compilationUnit.getJavaProject(), null, isBoolean);
		ITypeBinding declaringType= context.variableBinding.getDeclaringClass();
		if (declaringType == null)
			return null;

		IMethodBinding method= Bindings.findMethodInHierarchy(declaringType, setterName, new ITypeBinding[] { context.variableBinding.getType() });
		if (method != null && Bindings.isVoidType(method.getReturnType()) && (Modifier.isStatic(method.getModifiers()) == Modifier.isStatic(context.variableBinding.getModifiers()))) {
			Expression assignedValue= getAssignedValue(context);
			if (assignedValue == null)
				return null; //we don't know how to handle those cases.
			Expression mi= createMethodInvocation(context, method, assignedValue);
			context.astRewrite.replace(context.accessNode.getParent(), mi, null);

			String label= Messages.format(CorrectionMessages.GetterSetterCorrectionSubProcessor_replacewithsetter_description, BasicElementLabels.getJavaCodeString(ASTNodes.asString(context.accessNode)));
			T proposal= createMethodSetterProposal(label, context.compilationUnit, context.astRewrite, relevance);
			return proposal;
		} else {
			IJavaElement element= context.variableBinding.getJavaElement();
			if (element instanceof IField field) {
				try {
					if (RefactoringAvailabilityTesterCore.isSelfEncapsulateAvailable(field))
						return createFieldSetterProposal(relevance, field);
				} catch (JavaModelException e) {
					JavaManipulationPlugin.log(e);
				}
			}
		}
		return null;
	}

	private static boolean isBoolean(ProposalParameter context) {
		AST ast= context.astRewrite.getAST();
		boolean isBoolean= ast.resolveWellKnownType("boolean") == context.variableBinding.getType(); //$NON-NLS-1$
		if (!isBoolean)
			isBoolean= ast.resolveWellKnownType("java.lang.Boolean") == context.variableBinding.getType(); //$NON-NLS-1$
		return isBoolean;
	}

	private static Expression getAssignedValue(ProposalParameter context) {
		ASTNode parent= context.accessNode.getParent();
		ASTRewrite astRewrite= context.astRewrite;
		IMethodBinding getter= findGetter(context);
		Expression getterExpression= null;
		if (getter != null) {
			getterExpression= astRewrite.getAST().newSimpleName("placeholder"); //$NON-NLS-1$
		}
		ITypeBinding type= context.variableBinding.getType();
		Expression result= GetterSetterUtil.getAssignedValue(parent, astRewrite, getterExpression, type);
		if (result != null && getterExpression != null && getterExpression.getParent() != null) {
			getterExpression.getParent().setStructuralProperty(getterExpression.getLocationInParent(), createMethodInvocation(context, getter, null));
		}
		return result;
	}


}
