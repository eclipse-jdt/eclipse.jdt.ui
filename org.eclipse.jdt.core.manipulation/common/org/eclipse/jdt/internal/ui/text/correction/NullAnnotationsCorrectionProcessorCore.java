package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.fix.NullAnnotationsFixCore;
import org.eclipse.jdt.internal.corext.fix.NullAnnotationsRewriteOperations.ChangeKind;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.fix.NullAnnotationsCleanUpCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ExtractToNullCheckedLocalProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.MakeLocalVariableNonNullProposalCore;

public abstract class NullAnnotationsCorrectionProcessorCore<T> {
	protected final int CORRECTION_CHANGE= 1;

	// pre: changeKind != OVERRIDDEN
	public void getReturnAndArgumentTypeProposal(IInvocationContext context, IProblemLocation problem, ChangeKind changeKind,
			Collection<T> proposals) {
		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(astRoot);

		boolean isArgumentProblem= NullAnnotationsFixCore.isComplainingAboutArgument(selectedNode);
		if (isArgumentProblem || NullAnnotationsFixCore.isComplainingAboutReturn(selectedNode))
			getNullAnnotationInSignatureProposal(context, problem, proposals, changeKind, isArgumentProblem);
	}

	public void getNullAnnotationInSignatureProposal(IInvocationContext context, IProblemLocation problem,
			Collection<T> proposals, ChangeKind changeKind, boolean isArgumentProblem) {
		NullAnnotationsFixCore fix= NullAnnotationsFixCore.createNullAnnotationInSignatureFix(context.getASTRoot(), problem,
				changeKind, isArgumentProblem);

		if (fix != null) {
			Map<String, String> options= new Hashtable<>();
			if (fix.getCu() != context.getASTRoot()) {
				// workaround: adjust the unit to operate on, depending on the findings of RewriteOperations.createAddAnnotationOperation(..)
				final CompilationUnit cu= fix.getCu();
				final IInvocationContext originalContext= context;
				context= new IInvocationContext() {
					@Override
					public int getSelectionOffset() {
						return originalContext.getSelectionOffset();
					}

					@Override
					public int getSelectionLength() {
						return originalContext.getSelectionLength();
					}

					@Override
					public ASTNode getCoveringNode() {
						return originalContext.getCoveringNode();
					}

					@Override
					public ASTNode getCoveredNode() {
						return originalContext.getCoveredNode();
					}

					@Override
					public ICompilationUnit getCompilationUnit() {
						return (ICompilationUnit) cu.getJavaElement();
					}

					@Override
					public CompilationUnit getASTRoot() {
						return cu;
					}
				};
			}
			int relevance= (changeKind == ChangeKind.OVERRIDDEN) ? IProposalRelevance.CHANGE_NULLNESS_ANNOTATION_IN_OVERRIDDEN_METHOD : IProposalRelevance.CHANGE_NULLNESS_ANNOTATION; //raise local change above change in overridden method
			FixCorrectionProposalCore proposal= new FixCorrectionProposalCore(fix, new NullAnnotationsCleanUpCore(options, problem.getProblemId()), relevance, context);
			proposals.add(fixCorrectionProposalCoreToT(proposal, CORRECTION_CHANGE));
		}
	}

	public void getRemoveRedundantAnnotationProposal(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		NullAnnotationsFixCore fix= NullAnnotationsFixCore.createRemoveRedundantNullAnnotationsFix(context.getASTRoot(), problem);
		if (fix == null)
			return;

		Map<String, String> options= new Hashtable<>();
		FixCorrectionProposalCore proposal= new FixCorrectionProposalCore(fix, new NullAnnotationsCleanUpCore(options, problem.getProblemId()), IProposalRelevance.REMOVE_REDUNDANT_NULLNESS_ANNOTATION, context);
		proposals.add(fixCorrectionProposalCoreToT(proposal, CORRECTION_CHANGE));
	}

	public void getExtractCheckedLocalProposal(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		CompilationUnit compilationUnit = context.getASTRoot();
		ICompilationUnit cu= (ICompilationUnit) compilationUnit.getJavaElement();

		ASTNode selectedNode= problem.getCoveringNode(compilationUnit);

		SimpleName name= findProblemFieldName(selectedNode, problem.getProblemId());
		if (name == null)
			return;

		ASTNode method= ASTNodes.getParent(selectedNode, MethodDeclaration.class);
		if (method == null)
			method= ASTNodes.getParent(selectedNode, Initializer.class);
		if (method == null)
			return;

		ExtractToNullCheckedLocalProposalCore proposal= new ExtractToNullCheckedLocalProposalCore(cu, compilationUnit, name, method);
		proposals.add(extractToNullCheckedLocalProposalCoreToT(proposal, CORRECTION_CHANGE));
	}

	private static SimpleName findProblemFieldName(ASTNode selectedNode, int problemID) {
		// if a field access occurs in an compatibility situation (assignment/return/argument)
		// with expected type declared @NonNull we first need to find the SimpleName inside:
		if (selectedNode instanceof FieldAccess)
			selectedNode= ((FieldAccess) selectedNode).getName();
		else if (selectedNode instanceof QualifiedName)
			selectedNode= ((QualifiedName) selectedNode).getName();

		if (selectedNode instanceof SimpleName) {
			SimpleName name= (SimpleName) selectedNode;
			if (problemID == IProblem.NullableFieldReference)
				return name;
			// not field dereference, but compatibility issue - is value a field reference?
			IBinding binding= name.resolveBinding();
			if ((binding instanceof IVariableBinding) && ((IVariableBinding) binding).isField())
				return name;
		}
		return null;
	}

	public void getLocalVariableAnnotationProposal(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		ICompilationUnit cu= context.getCompilationUnit();
		CompilationUnit astRoot= context.getASTRoot();

		String nonNullAnnotationName= NullAnnotationsFixCore.getNonNullAnnotationName(astRoot.getJavaElement(), false);

		if (nonNullAnnotationName == null) {
			return;
		}

		ASTNode selectedNode= problem.getCoveredNode(astRoot);
		if (!(selectedNode instanceof Expression)) {
			return;
		}
		Expression nodeToCast= (Expression) selectedNode;
		IBinding callerBinding= Bindings.resolveExpressionBinding(nodeToCast, false);
		if (callerBinding instanceof IVariableBinding) {
			IVariableBinding variableBinding= (IVariableBinding) callerBinding;

			if (variableBinding.isField()) {
				return;
			}
			ITypeBinding type= variableBinding.getType();
			if (type == null || type.isArray()) {
				return;
			}
			MakeLocalVariableNonNullProposalCore proposal= new MakeLocalVariableNonNullProposalCore(cu, variableBinding, astRoot, IProposalRelevance.CHANGE_NULLNESS_ANNOTATION, nonNullAnnotationName);
			proposals.add(makeLocalVariableNonNullProposalCoreToT(proposal));
		}
	}

	protected abstract T makeLocalVariableNonNullProposalCoreToT(MakeLocalVariableNonNullProposalCore core);
	protected abstract T extractToNullCheckedLocalProposalCoreToT(ExtractToNullCheckedLocalProposalCore core, int uid);
	protected abstract T fixCorrectionProposalCoreToT(FixCorrectionProposalCore core, int uid);
}
