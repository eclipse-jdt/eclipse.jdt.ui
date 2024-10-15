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
 *     Pierre-Yves B. <pyvesdev@gmail.com> - [inline] Allow inlining of local variable initialized to null. - https://bugs.eclipse.org/93850
 *     Microsoft Corporation - copied to jdt.core.manipulation
 *     Microsoft Corporation - read formatting options from the compilation unit
 *     Nikolay Metchev - <nikolaymetchev@gmail.com> - [inline] Inline Local Variable does not qualify accesses to obscured types - https://bugs.eclipse.org/367536
 ********************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.RangeMarker;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.InlineLocalVariableDescriptor;

import org.eclipse.jdt.internal.core.manipulation.BindingLabelProviderCore;
import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.manipulation.dom.NecessaryParenthesesChecker;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.core.manipulation.util.Strings;
import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptorUtil;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStringStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringAnalyzeUtil;
import org.eclipse.jdt.internal.corext.refactoring.rename.TempDeclarationFinder;
import org.eclipse.jdt.internal.corext.refactoring.rename.TempOccurrenceAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TightSourceRangeComputer;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.util.Progress;

public class InlineTempRefactoring extends Refactoring {

	private int fSelectionStart;
	private int fSelectionLength;
	private ICompilationUnit fCu;

	//the following fields are set after the construction
	private VariableDeclaration fVariableDeclaration;
	private SimpleName[] fReferences;
	private CompilationUnit fASTRoot;
	private boolean fCheckResultForCompileProblems;
	private Set<SimpleName> fReferencesNeedingParams= new HashSet<>();
	private Map<SimpleName, ITrackedNodePosition> fTrackedReferences= new HashMap<>();
	private Map<SimpleName, ISourceRange> fResolvedTrackedReferences= new HashMap<>();
	private CompilationUnitChange fChange;

	/**
	 * Creates a new inline constant refactoring.
	 * @param unit the compilation unit, or <code>null</code> if invoked by scripting
	 * @param node compilation unit node, or <code>null</code>
	 * @param selectionStart start
	 * @param selectionLength length
	 */
	public InlineTempRefactoring(ICompilationUnit unit, CompilationUnit node, int selectionStart, int selectionLength) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fCu= unit;

		fASTRoot= node;
		fVariableDeclaration= null;
		fCheckResultForCompileProblems= true;
		fChange= null;
	}

	/**
	 * Creates a new inline constant refactoring.
	 * @param unit the compilation unit, or <code>null</code> if invoked by scripting
	 * @param selectionStart start
	 * @param selectionLength length
	 */
	public InlineTempRefactoring(ICompilationUnit unit, int selectionStart, int selectionLength) {
		this(unit, null, selectionStart, selectionLength);
	}

	public InlineTempRefactoring(VariableDeclaration decl) {
		fVariableDeclaration= decl;
		ASTNode astRoot= decl.getRoot();
		Assert.isTrue(astRoot instanceof CompilationUnit);
		fASTRoot= (CompilationUnit) astRoot;
		Assert.isTrue(fASTRoot.getJavaElement() instanceof ICompilationUnit);

		fSelectionStart= decl.getStartPosition();
		fSelectionLength= decl.getLength();
		fCu= (ICompilationUnit) fASTRoot.getJavaElement();
		fCheckResultForCompileProblems= true;
		fChange= null;
	}

    public InlineTempRefactoring(JavaRefactoringArguments arguments, RefactoringStatus status) {
   		this(null, null, 0, 0);
   		RefactoringStatus initializeStatus= initialize(arguments);
   		status.merge(initializeStatus);
    }

	public void setCheckResultForCompileProblems(boolean checkResultForCompileProblems) {
		fCheckResultForCompileProblems = checkResultForCompileProblems;
	}

	public RefactoringStatus checkIfTempSelected() {
		VariableDeclaration decl= getVariableDeclaration();
		if (decl == null) {
			return CodeRefactoringUtil.checkMethodSyntaxErrors(fSelectionStart, fSelectionLength, getASTRoot(), RefactoringCoreMessages.InlineTempRefactoring_select_temp);
		}
		if (decl.getParent() instanceof FieldDeclaration) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineTemRefactoring_error_message_fieldsCannotBeInlined);
		}
		return new RefactoringStatus();
	}

	private CompilationUnit getASTRoot() {
		if (fASTRoot == null) {
			fASTRoot= RefactoringASTParser.parseWithASTProvider(fCu, true, null);
		}
		return fASTRoot;
	}

	public VariableDeclaration getVariableDeclaration() {
		if (fVariableDeclaration == null) {
			fVariableDeclaration= TempDeclarationFinder.findTempDeclaration(getASTRoot(), fSelectionStart, fSelectionLength);
		}
		return fVariableDeclaration;
	}

	private ASTNode getSelectedNode() {
		return TempDeclarationFinder.getSelectedNode(getASTRoot(), fSelectionStart, fSelectionLength);
	}

	@Override
	public String getName() {
		return RefactoringCoreMessages.InlineTempRefactoring_name;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask("", 1); //$NON-NLS-1$

			RefactoringStatus result= Checks.validateModifiesFiles(ResourceUtil.getFiles(new ICompilationUnit[]{fCu}), getValidationContext(), pm);
			if (result.hasFatalError())
				return result;

			ASTNode selected= getSelectedNode();
			VariableDeclaration declaration= getVariableDeclaration();

			result.merge(checkSelection(selected, declaration));

			result.merge(checkClashes(declaration));

			return result;
		} finally {
			pm.done();
		}
	}

	private RefactoringStatus checkClashes(VariableDeclaration declaration) {
		for (SimpleName reference : getReferences()) {
			Set<IVariableBinding> newVariables= getNewVariables(reference);
			final List<Name> initializerNames= getInitializerNames(declaration.getInitializer());
			for (Name name : initializerNames) {
				if (clashesWithNewVariables(newVariables, name)) {
					List<Expression> alternativeQualifications= getAlternativeQualifications(reference, name);
					if (alternativeQualifications.size() > 0) {
						boolean inlinable= false;
						for (Expression alternative : alternativeQualifications) {
							if (!clashesWithNewVariables(newVariables, alternative)) {
								inlinable= true;
								break;
							}
						}
						if (!inlinable) {
							return RefactoringStatus.createFatalErrorStatus(
									Messages.format(RefactoringCoreMessages.InlineTemRefactoring_error_message_inliningClashes, name));
						}
					}
				}
			}
		}
		return null;
	}

	private RefactoringStatus checkSelection(ASTNode selectedNode, VariableDeclaration decl) {
		ASTNode parent= decl.getParent();
		if (parent instanceof MethodDeclaration) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineTempRefactoring_method_parameter);
		}

		if (parent instanceof CatchClause) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineTempRefactoring_exceptions_declared);
		}

		if (parent instanceof VariableDeclarationExpression && parent.getLocationInParent() == ForStatement.INITIALIZERS_PROPERTY) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineTempRefactoring_for_initializers);
		}

		if (parent instanceof VariableDeclarationExpression && parent.getLocationInParent() == TryStatement.RESOURCES2_PROPERTY) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineTempRefactoring_resource_in_try_with_resources);
		}

		if (selectedNode instanceof Name && selectedNode.getLocationInParent() == TryStatement.RESOURCES2_PROPERTY) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineTempRefactoring_resource_used_in_try_with_resources);
		}

		if (decl.getInitializer() == null) {
			String message= Messages.format(RefactoringCoreMessages.InlineTempRefactoring_not_initialized, BasicElementLabels.getJavaElementName(decl.getName().getIdentifier()));
			return RefactoringStatus.createFatalErrorStatus(message);
		}

		SimpleName[] references= getReferences();
		for (SimpleName ref : references) {
			if (ref.getLocationInParent() == TryStatement.RESOURCES2_PROPERTY) {
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineTempRefactoring_resource_used_in_try_with_resources);
			}
		}

		return checkAssignments(decl);
	}

	private RefactoringStatus checkAssignments(VariableDeclaration decl) {
		TempAssignmentFinder assignmentFinder= new TempAssignmentFinder(decl);
		getASTRoot().accept(assignmentFinder);
		if (!assignmentFinder.hasAssignments())
			return new RefactoringStatus();
		ASTNode firstAssignment= assignmentFinder.getFirstAssignment();
		int start= firstAssignment.getStartPosition();
		int length= firstAssignment.getLength();
		ISourceRange range= new SourceRange(start, length);
		RefactoringStatusContext context= JavaStatusContext.create(fCu, range);
		String message= Messages.format(RefactoringCoreMessages.InlineTempRefactoring_assigned_more_once, BasicElementLabels.getJavaElementName(decl.getName().getIdentifier()));
		return RefactoringStatus.createFatalErrorStatus(message, context);
	}


	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask("", 1); //$NON-NLS-1$
			CompilationUnitRewrite cuRewrite= new CompilationUnitRewrite(fCu, fASTRoot);

			inlineTemp(cuRewrite);
			removeTemp(cuRewrite);

			// If we have some tracked references, it means there were implied type parameters
			// in the in-line source and we might have to add these when we in-line, but don't
			// know.  To tell, we will have to compile the new source and look for type mismatch
			// errors and then line them up with our tracked positions.  To get the tracked
			// positions, we need to do this ahead of the createChange() call below which
			// will not allow us to do so after.
			IDocument doc= null;
			if (!fTrackedReferences.isEmpty()) {
				doc = new Document(fCu.getSource());
				try {
					cuRewrite.getASTRewrite().rewriteAST().apply(doc);
					for (Entry<SimpleName, ITrackedNodePosition> entry : fTrackedReferences.entrySet()) {
						SimpleName name= entry.getKey();
						ITrackedNodePosition position= entry.getValue();
						int start= position.getStartPosition();
						int length= position.getLength();
						ISourceRange range= new SourceRange(start, length);
						fResolvedTrackedReferences.put(name, range);
					}
				} catch (MalformedTreeException | BadLocationException e) {
					// do nothing
				}
			}

			fChange= cuRewrite.createChange(RefactoringCoreMessages.InlineTempRefactoring_inline, false, Progress.subMonitor(pm, 1));

			if (fCheckResultForCompileProblems) {
				RefactoringStatus result= doc == null ? RefactoringAnalyzeUtil.checkNewSource(fChange, fCu, fASTRoot, pm)
						: RefactoringAnalyzeUtil.checkNewSource(doc, fCu, fASTRoot, pm);
				if (!result.isOK()) {
					for (RefactoringStatusEntry entry : result.getEntries()) {
						JavaStringStatusContext context= (JavaStringStatusContext) entry.getContext();
						if (entry.getMessage().startsWith("Type mismatch:")) { //$NON-NLS-1$
							ISourceRange range= context.getSourceRange();
							int rangeEnd= range.getOffset() + range.getLength();
							try {
								for (Entry<SimpleName, ISourceRange> trackedEntry : fResolvedTrackedReferences.entrySet()) {
									int trackedEntryEnd= trackedEntry.getValue().getOffset() + trackedEntry.getValue().getLength();
									if (trackedEntry.getValue().getOffset() >= range.getOffset() &&
											trackedEntryEnd <= rangeEnd) {
										fReferencesNeedingParams.add(trackedEntry.getKey());
									}
								}
							} catch (MalformedTreeException e) {
								// do nothing
							}
						}
					}
					// if we found candidates to add type parameters, redo the refactoring and this
					// time add the type parameters
					if (!fReferencesNeedingParams.isEmpty()) {
						cuRewrite.clearASTRewrite();
						inlineTemp(cuRewrite);
						removeTemp(cuRewrite);
						fChange= cuRewrite.createChange(RefactoringCoreMessages.InlineTempRefactoring_inline, false, Progress.subMonitor(pm, 1));
						result= RefactoringAnalyzeUtil.checkNewSource(fChange, fCu, fASTRoot, pm);
					}
					return result;
				}
			}
			return new RefactoringStatus();
		} finally {
			pm.done();
		}
	}

	//----- changes

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.InlineTempRefactoring_preview, 2);

			final InlineLocalVariableDescriptor descriptor= createRefactoringDescriptor();
			fChange.setDescriptor(new RefactoringChangeDescriptor(descriptor));
			return fChange;
		} finally {
			pm.done();
		}
	}

	private InlineLocalVariableDescriptor createRefactoringDescriptor() {
		final Map<String, String> arguments= new HashMap<>();
		String project= null;
		IJavaProject javaProject= fCu.getJavaProject();
		if (javaProject != null)
			project= javaProject.getElementName();

		final IVariableBinding binding= getVariableDeclaration().resolveBinding();
		String text= null;
		final IMethodBinding method= binding.getDeclaringMethod();
		if (method != null)
			text= BindingLabelProviderCore.getBindingLabel(method, JavaElementLabelsCore.ALL_FULLY_QUALIFIED);
		else
			text= BasicElementLabels.getJavaElementName('{' + JavaElementLabelsCore.ELLIPSIS_STRING + '}');
		final String description= Messages.format(RefactoringCoreMessages.InlineTempRefactoring_descriptor_description_short, BasicElementLabels.getJavaElementName(binding.getName()));
		final String header= Messages.format(RefactoringCoreMessages.InlineTempRefactoring_descriptor_description, new String[] { BindingLabelProviderCore.getBindingLabel(binding, JavaElementLabelsCore.ALL_FULLY_QUALIFIED), text});
		final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
		comment.addSetting(Messages.format(RefactoringCoreMessages.InlineTempRefactoring_original_pattern, BindingLabelProviderCore.getBindingLabel(binding, JavaElementLabelsCore.ALL_FULLY_QUALIFIED)));
		final InlineLocalVariableDescriptor descriptor= RefactoringSignatureDescriptorFactory.createInlineLocalVariableDescriptor(project, description, comment.asString(), arguments, RefactoringDescriptor.NONE);
		arguments.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT, JavaRefactoringDescriptorUtil.elementToHandle(project, fCu));
		arguments.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_SELECTION, String.valueOf(fSelectionStart) + ' ' + String.valueOf(fSelectionLength));
		return descriptor;
	}

	private void inlineTemp(CompilationUnitRewrite cuRewrite) throws JavaModelException {
		TextEditGroup groupDesc= cuRewrite.createGroupDescription(RefactoringCoreMessages.InlineTempRefactoring_inline_edit_name);
		ASTRewrite rewrite= cuRewrite.getASTRewrite();

		for (SimpleName curr : getReferences()) {
			ASTNode initializerCopy= getInitializerSource(cuRewrite, curr, groupDesc);
			rewrite.replace(curr, initializerCopy, groupDesc);
		}
	}

    private void removeTemp(CompilationUnitRewrite cuRewrite) {
		VariableDeclaration variableDeclaration= getVariableDeclaration();
		TextEditGroup groupDesc= cuRewrite.createGroupDescription(RefactoringCoreMessages.InlineTempRefactoring_remove_edit_name);
		ASTNode parent= variableDeclaration.getParent();
		ASTRewrite rewrite= cuRewrite.getASTRewrite();
		TightSourceRangeComputer sourceRangeComputer= new TightSourceRangeComputer();
		rewrite.setTargetSourceRangeComputer(sourceRangeComputer);
		if (parent instanceof VariableDeclarationStatement && ((VariableDeclarationStatement) parent).fragments().size() == 1) {
			sourceRangeComputer.addTightSourceNode(parent);
			rewrite.remove(parent, groupDesc);
		} else {
			sourceRangeComputer.addTightSourceNode(variableDeclaration);
			rewrite.remove(variableDeclaration, groupDesc);
		}
	}

	private Expression getInitializerSource(CompilationUnitRewrite rewrite, SimpleName reference, TextEditGroup groupDesc) throws JavaModelException {
		Expression copy= getModifiedInitializerSource(rewrite, reference, groupDesc);
		if (NecessaryParenthesesChecker.needsParentheses(copy, reference.getParent(), reference.getLocationInParent())) {
			ParenthesizedExpression parentExpr= rewrite.getAST().newParenthesizedExpression();
			parentExpr.setExpression(copy);
			return parentExpr;
		}
		return copy;
	}

	private Expression getModifiedInitializerSource(CompilationUnitRewrite rewrite, SimpleName reference, TextEditGroup groupDesc) throws JavaModelException {
		VariableDeclaration varDecl= getVariableDeclaration();
		Expression initializer= varDecl.getInitializer();
		List<Name> initializerNames= getInitializerNames(initializer);
		if (!initializerNames.isEmpty()) {
			Map<Name, Expression> replacements= getClashingReplacements(reference, initializerNames);
			if (!replacements.isEmpty()) {
				return replaceClashingNames(rewrite, groupDesc, initializer, replacements);
			}
		}
		ASTNode referenceContext= reference.getParent();
		boolean trackExpression= false;
		if (Invocations.isResolvedTypeInferredFromExpectedType(initializer)) {
			if (!(referenceContext instanceof VariableDeclarationFragment)
					&& !(referenceContext instanceof SingleVariableDeclaration)
					&& !(referenceContext instanceof Assignment)) {
				ITypeBinding[] typeArguments= Invocations.getInferredTypeArguments(initializer);
				if (typeArguments != null) {
					fCheckResultForCompileProblems= true;
					if (fReferencesNeedingParams.contains(reference)) {
						String newSource= createParameterizedInvocation(initializer, typeArguments, rewrite);
						return (Expression) rewrite.getASTRewrite().createStringPlaceholder(newSource, initializer.getNodeType());
					} else {
						trackExpression= true;
					}
				}
			}
		}

		Expression copy= (Expression) rewrite.getASTRewrite().createCopyTarget(initializer);
		if (trackExpression) {
			fTrackedReferences.put(reference, rewrite.getASTRewrite().track(reference.getParent()));
		}
		AST ast= rewrite.getAST();
		if (NecessaryParenthesesChecker.needsParentheses(initializer, reference.getParent(), reference.getLocationInParent())) {
			ParenthesizedExpression parenthesized= ast.newParenthesizedExpression();
			parenthesized.setExpression(copy);
			copy= parenthesized;
		}

		ITypeBinding explicitCast= ASTNodes.getExplicitCast(initializer, reference);
		if (explicitCast != null) {
			CastExpression cast= ast.newCastExpression();
			if (NecessaryParenthesesChecker.needsParentheses(copy, cast, CastExpression.EXPRESSION_PROPERTY)) {
				ParenthesizedExpression parenthesized= ast.newParenthesizedExpression();
				parenthesized.setExpression(copy);
				copy= parenthesized;
			}
			cast.setExpression(copy);
			ImportRewriteContext context= new ContextSensitiveImportRewriteContext(reference, rewrite.getImportRewrite());
			cast.setType(rewrite.getImportRewrite().addImport(explicitCast, ast, context, TypeLocation.CAST));
			copy= cast;

		} else if (initializer instanceof ArrayInitializer && ASTNodes.getDimensions(varDecl) > 0) {
			ArrayType newType= (ArrayType) ASTNodeFactory.newType(ast, varDecl);

			ArrayCreation newArrayCreation= ast.newArrayCreation();
			newArrayCreation.setType(newType);
			newArrayCreation.setInitializer((ArrayInitializer) copy);
			return newArrayCreation;
		}
		return copy;
	}

	private List<Name> getInitializerNames(final Expression initializer) {
		ScopeAnalyzer scopeAnalyzer= new ScopeAnalyzer(fASTRoot);
		VariableDeclaration var= getVariableDeclaration();
		int end = var.getStartPosition();
		final Set<IBinding> declarationsAtInitializer= new HashSet<>(Arrays.asList(scopeAnalyzer.getDeclarationsInScope(end, ScopeAnalyzer.VARIABLES)));
		final List<Name> initializerNames= new ArrayList<>();
		if (initializer != null) {
			initializer.accept(new ASTVisitor() {
				@Override
				public boolean visit(QualifiedName node) {
					IBinding binding= node.resolveBinding();
					if (binding instanceof IVariableBinding) {
						initializerNames.add(node);
					}
					return false;
				}

				@Override
				public boolean visit(SimpleName node) {
					IBinding binding= node.resolveBinding();
					if (binding instanceof IVariableBinding) {
						if (declarationsAtInitializer.contains(binding)) {
							initializerNames.add(node);
						}
					}
					return false;
				}
			});
		}
		return initializerNames;
	}

	private Expression replaceClashingNames(final CompilationUnitRewrite rewrite, final TextEditGroup groupDesc, Expression initializer, final Map<Name, Expression> replacements) {
		final Expression copyOfInitializer= (Expression) ASTNode.copySubtree(fASTRoot.getAST(), initializer);
		final ASTMatcher astMatcher= new ASTMatcher();
		copyOfInitializer.accept(new ASTVisitor() {
			@Override
			public boolean visit(QualifiedName copyOfInitializerNode) {
				return replace(copyOfInitializerNode);
			}

			@Override
			public boolean visit(SimpleName node) {
				return replace(node);
			}

			private boolean replace(Name copyOfInitializerNode) {
				Set<Entry<Name, Expression>> replacementKeySet= replacements.entrySet();
				for (Iterator<Entry<Name, Expression>> replacementIterator= replacementKeySet.iterator(); replacementIterator.hasNext();) {
					Entry<Name, Expression> replacement= replacementIterator.next();
					Name key= replacement.getKey();
					if (key instanceof QualifiedName && astMatcher.match((QualifiedName) key, copyOfInitializerNode) ||
							key instanceof SimpleName && astMatcher.match((SimpleName) key, copyOfInitializerNode)) {
						if (copyOfInitializerNode != copyOfInitializer) {
							rewrite.getASTRewrite().replace(copyOfInitializerNode, replacement.getValue(), groupDesc);
							replacementIterator.remove();
						}
						return false;
					}
				}
				return false;
			}
		});
		if (!replacements.isEmpty()) { // the whole initializer was replaced
			return replacements.values().iterator().next();
		}
		return copyOfInitializer;
	}

	private Map<Name, Expression> getClashingReplacements(SimpleName reference, List<Name> initializerNames) {
		Set<IVariableBinding> newVariables= getNewVariables(reference);
		final Map<Name, Expression> replacements= new HashMap<>();
		for (Name initializerName : initializerNames) {
			if (clashesWithNewVariables(newVariables, initializerName)) {
				List<Expression> alternativeQualifications= getAlternativeQualifications(reference, initializerName);
				for (Expression alternative : alternativeQualifications) {
					if (!clashesWithNewVariables(newVariables, alternative)) {
						replacements.put(initializerName, alternative);
						break;
					}
				}
			}
		}
		return replacements;
	}

	private List<Expression> getAlternativeQualifications(SimpleName reference, Name initializerName) {
		List<Expression> ans= new ArrayList<>();
		if (initializerName instanceof SimpleName) {
			SimpleName simpleNameInitializer= ((SimpleName) initializerName);
			IBinding resolveBinding= simpleNameInitializer.resolveBinding();
			if (resolveBinding instanceof IVariableBinding) {
				IVariableBinding resolvedVariableBinding= (IVariableBinding) resolveBinding;
				boolean isStatic= Modifier.isStatic(resolvedVariableBinding.getModifiers());
				if (isStatic) {
					ans.add(createFullyQualifiedName(simpleNameInitializer, resolvedVariableBinding.getDeclaringClass(), false));
					ans.add(createFullyQualifiedName(simpleNameInitializer, resolvedVariableBinding.getDeclaringClass(), true));
				} else {
					AST ast= fASTRoot.getAST();
					FieldAccess newFieldAccess= ast.newFieldAccess();
					newFieldAccess.setExpression(ast.newThisExpression());
					newFieldAccess.setName((SimpleName) ASTNode.copySubtree(ast, simpleNameInitializer));
					ans.add(newFieldAccess);
				}
			}
		} else if (initializerName instanceof QualifiedName){
			QualifiedName initializerQualifiedName = (QualifiedName) initializerName;
			IBinding resolveBinding= initializerQualifiedName.resolveBinding();
			if (resolveBinding instanceof IVariableBinding resolvedVariableBinding) {
				boolean isStatic= Modifier.isStatic(resolvedVariableBinding.getModifiers());
				if (isStatic) {
					SimpleName simpleName= initializerQualifiedName.getName();
					int i= findCommonDeclaringClassIndex(reference, initializerQualifiedName);
					ITypeBinding initializerQualifiedNameTypeBinding= initializerQualifiedName.getQualifier().resolveTypeBinding();
					ans.add(createFullyQualifiedName(simpleName, initializerQualifiedNameTypeBinding, false));
					ans.add(createFullyQualifiedName(simpleName, initializerQualifiedNameTypeBinding, i, false));
					ans.add(createFullyQualifiedName(simpleName, initializerQualifiedNameTypeBinding, true));
					ans.add(createFullyQualifiedName(simpleName, initializerQualifiedNameTypeBinding, i, true));
				}
			}
		}
		return ans;
	}

	/**
	 * @param reference the reference being inlined
	 * @param initializerQualifiedName the qualified name from the initializer being inlined
	 * @return the number of declaring classes to skip from the fully qualified name of
	 *         initializerQualifiedName in order to get to the declaring class of the passed in
	 *         reference.
	 */
	private int findCommonDeclaringClassIndex(SimpleName reference, QualifiedName initializerQualifiedName) {
		final List<SimpleName> initializerSimpleNames= new ArrayList<>();
		initializerQualifiedName.accept(new ASTVisitor() {
			@Override
			public boolean visit(QualifiedName qualifiedName) {
				initializerSimpleNames.add(0, qualifiedName.getName());
				qualifiedName.getQualifier().accept(this);
				return false;
			}

			@Override
			public boolean visit(SimpleName simpleName) {
				initializerSimpleNames.add(0, simpleName);
				return false;
			}
		});
		TypeDeclaration typeDeclaration= (TypeDeclaration) ASTNodes.getParent(reference, ASTNode.TYPE_DECLARATION);
		ITypeBinding declaringClassBinding= typeDeclaration.resolveBinding();
		int i= 1;
		while (i < initializerSimpleNames.size() && declaringClassBinding != getDeclaringClass(initializerSimpleNames.get(i))) {
			i++;
		}
		return i;
	}

	private ITypeBinding getDeclaringClass(SimpleName simpleName) {
		IBinding resolveBinding= simpleName.resolveBinding();
		if (resolveBinding instanceof ITypeBinding) {
			return ((ITypeBinding) resolveBinding).getDeclaringClass();
		} else if (resolveBinding instanceof IVariableBinding) {
			return ((IVariableBinding) resolveBinding).getDeclaringClass();
		}
		return null;
	}

	private boolean clashesWithNewVariables(Set<IVariableBinding> newVariables, Expression name) {
		if (name instanceof QualifiedName) {
			return clashesWithNewVariables(newVariables, (QualifiedName) name);
		} else if (name instanceof SimpleName) {
			return clashesWithNewVariables(newVariables, (SimpleName) name);
		}
		return false;
	}

	private boolean clashesWithNewVariables(Set<IVariableBinding> newVariables, QualifiedName qualifiedName) {
		return clashesWithNewVariables(newVariables, ASTNodes.getLeftMostSimpleName(qualifiedName));
	}

	private boolean clashesWithNewVariables(Set<IVariableBinding> newVariables, SimpleName leftMostSimpleName) {
		String leftMostSimpleNameIdentifier= leftMostSimpleName.getIdentifier();
		for (IVariableBinding newVariable : newVariables) {
			if (newVariable.getName().equals(leftMostSimpleNameIdentifier)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param reference the reference being inlined
	 * @return A set of bindings representing any variables that have come into scope for the
	 *         variable declaration after it has been inlined to the passed in reference.
	 */
	private Set<IVariableBinding> getNewVariables(SimpleName reference) {
		ScopeAnalyzer scopeAnalyzer= new ScopeAnalyzer(fASTRoot);
		IBinding[] declarationsAtReference= scopeAnalyzer.getDeclarationsInScope(reference.getStartPosition(), ScopeAnalyzer.VARIABLES);
		Set<IVariableBinding> newVariables= new HashSet<>(declarationsAtReference.length);
		for (IBinding newVariable : declarationsAtReference) {
			newVariables.add((IVariableBinding) newVariable);
		}
		VariableDeclaration var= getVariableDeclaration();
		int endPosition= var.getStartPosition() + var.getLength();
		for (IBinding oldVariable : scopeAnalyzer.getDeclarationsInScope(endPosition, ScopeAnalyzer.VARIABLES)) {
			newVariables.remove(oldVariable);
		}
		return newVariables;
	}

	private Name createFullyQualifiedName(SimpleName simpleName, ITypeBinding declaringClass, boolean addPackage) {
		return createFullyQualifiedName(simpleName, declaringClass, 0, addPackage);
	}

	private Name createFullyQualifiedName(SimpleName simpleName, ITypeBinding declaringClass, int numberOfClassesToSkip, boolean addPackage) {
		AST ast= fASTRoot.getAST();
		List<ITypeBinding> declaringClasses = new ArrayList<>();
		while (declaringClass != null) {
			declaringClasses.add(declaringClass);
			declaringClass= declaringClass.getDeclaringClass();
		}
		for (int i= 0; i < numberOfClassesToSkip; i++) {
			if (declaringClasses.size() > 0) {
				declaringClasses.remove(declaringClasses.size() - 1);
			} else {
				break;
			}
		}
		StringBuilder qualifiedName = new StringBuilder();
		if (addPackage && declaringClasses.size() > 0) {
			qualifiedName.append(declaringClasses.get(declaringClasses.size() - 1).getPackage().getName());
			qualifiedName.append('.');
		}
		for (int i= declaringClasses.size() - 1; i >= 0; i--) {
			qualifiedName.append(declaringClasses.get(i).getName());
			qualifiedName.append('.');
		}
		qualifiedName.append(simpleName.getFullyQualifiedName());
		return ast.newName(qualifiedName.toString());
	}

	private String createParameterizedInvocation(Expression invocation, ITypeBinding[] typeArguments, CompilationUnitRewrite cuRewrite) throws JavaModelException {
		ASTRewrite rewrite= ASTRewrite.create(invocation.getAST());
		ListRewrite typeArgsRewrite= Invocations.getInferredTypeArgumentsRewrite(rewrite, invocation);

		for (ITypeBinding typeArgument : typeArguments) {
			Type typeArgumentNode = cuRewrite.getImportRewrite().addImport(typeArgument, cuRewrite.getAST());
			typeArgsRewrite.insertLast(typeArgumentNode, null);
		}

		if (invocation instanceof MethodInvocation) {
			MethodInvocation methodInvocation= (MethodInvocation) invocation;
			Expression expression= methodInvocation.getExpression();
			if (expression == null) {
				IMethodBinding methodBinding= methodInvocation.resolveMethodBinding();
				if (methodBinding != null && Modifier.isStatic(methodBinding.getModifiers())) {
					expression= cuRewrite.getAST().newName(cuRewrite.getImportRewrite().addImport(methodBinding.getDeclaringClass().getTypeDeclaration()));
				} else {
					expression= invocation.getAST().newThisExpression();
				}
				rewrite.set(invocation, MethodInvocation.EXPRESSION_PROPERTY, expression, null);
			}
		}

		IDocument document= new Document(fCu.getBuffer().getContents());
		final RangeMarker marker= new RangeMarker(invocation.getStartPosition(), invocation.getLength());
		TextEdit[] rewriteEdits= rewrite.rewriteAST(document, fCu.getOptions(true)).removeChildren();
		marker.addChildren(rewriteEdits);
		try {
			marker.apply(document, TextEdit.UPDATE_REGIONS);
			String rewrittenInitializer= document.get(marker.getOffset(), marker.getLength());
			IRegion region= document.getLineInformation(document.getLineOfOffset(marker.getOffset()));
			int oldIndent= Strings.computeIndentUnits(document.get(region.getOffset(), region.getLength()), fCu);
			return Strings.changeIndent(rewrittenInitializer, oldIndent, fCu, "", TextUtilities.getDefaultLineDelimiter(document)); //$NON-NLS-1$
		} catch (MalformedTreeException | BadLocationException e) {
			JavaManipulationPlugin.log(e);
		}
		//fallback:
		return fCu.getBuffer().getText(invocation.getStartPosition(), invocation.getLength());
	}

	public SimpleName[] getReferences() {
		if (fReferences != null)
			return fReferences;
		TempOccurrenceAnalyzer analyzer= new TempOccurrenceAnalyzer(getVariableDeclaration(), false);
		analyzer.perform();
		fReferences= analyzer.getReferenceNodes();
		return fReferences;
	}

	private RefactoringStatus initialize(JavaRefactoringArguments arguments) {
		final String selection= arguments.getAttribute(JavaRefactoringDescriptorUtil.ATTRIBUTE_SELECTION);
		if (selection != null) {
			int offset= -1;
			int length= -1;
			final StringTokenizer tokenizer= new StringTokenizer(selection);
			if (tokenizer.hasMoreTokens())
				offset= Integer.parseInt(tokenizer.nextToken());
			if (tokenizer.hasMoreTokens())
				length= Integer.parseInt(tokenizer.nextToken());
			if (offset >= 0 && length >= 0) {
				fSelectionStart= offset;
				fSelectionLength= length;
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_illegal_argument, new Object[] { selection, JavaRefactoringDescriptorUtil.ATTRIBUTE_SELECTION}));
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptorUtil.ATTRIBUTE_SELECTION));
		final String handle= arguments.getAttribute(JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT);
		if (handle != null) {
			final IJavaElement element= JavaRefactoringDescriptorUtil.handleToElement(arguments.getProject(), handle, false);
			if (element == null || !element.exists() || element.getElementType() != IJavaElement.COMPILATION_UNIT)
				return JavaRefactoringDescriptorUtil.createInputFatalStatus(element, getName(), IJavaRefactorings.INLINE_LOCAL_VARIABLE);
			else {
				fCu= (ICompilationUnit) element;
	        	if (checkIfTempSelected().hasFatalError())
					return JavaRefactoringDescriptorUtil.createInputFatalStatus(element, getName(), IJavaRefactorings.INLINE_LOCAL_VARIABLE);
			}
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT));
		return new RefactoringStatus();
	}
}
