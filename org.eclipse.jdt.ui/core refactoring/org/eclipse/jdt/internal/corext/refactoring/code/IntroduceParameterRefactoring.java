/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Felix Pahl (fpahl@web.de) - contributed fix for:
 *       o introduce parameter throws NPE if there are compiler errors
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=48325)
 *******************************************************************************/

package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.Corext;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.dom.fragments.ASTFragmentFactory;
import org.eclipse.jdt.internal.corext.dom.fragments.IASTFragment;
import org.eclipse.jdt.internal.corext.dom.fragments.IExpressionFragment;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveStaticMembersProcessor.ASTData;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class IntroduceParameterRefactoring extends Refactoring {
	
	private ICompilationUnit fSourceCU;
	private int fSelectionStart;
	private int fSelectionLength;
	private final CodeGenerationSettings fSettings;

	private String fParameterName;

	private ASTData fSource;
	private Expression fSelectedExpression;
	private MethodDeclaration fMethodDeclaration;
	private String[] fExcludedParameterNames;
	private CompositeChange fChange;
	private ICompilationUnit[] fAffectedCUs;
	
	
	private IntroduceParameterRefactoring(ICompilationUnit cu, int selectionStart, int selectionLength, CodeGenerationSettings settings) {
		Assert.isTrue(cu != null && cu.exists());
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		Assert.isNotNull(settings);
		fSourceCU= cu;
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fSettings= settings;
		
		fParameterName= ""; //$NON-NLS-1$
	}
	
	public static boolean isAvailable(ASTNode[] selectedNodes, ASTNode coveringNode) {
		return Checks.isExtractableExpression(selectedNodes, coveringNode);
	}

	public static IntroduceParameterRefactoring create(ICompilationUnit cu, int selectionStart, int selectionLength, CodeGenerationSettings settings) {
		return new IntroduceParameterRefactoring(cu, selectionStart, selectionLength, settings);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("IntroduceParameterRefactoring.name"); //$NON-NLS-1$
	}

	//--- checkActivation
		
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask("", 7); //$NON-NLS-1$
			
			if (! fSourceCU.isStructureKnown())		
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("IntroduceParameterRefactoring.syntax_error")); //$NON-NLS-1$
			pm.worked(1);
			
			fSource= new ASTData(fSourceCU, true);
			initializeSelectedExpression();
			pm.worked(1);
		
			RefactoringStatus result= checkSelection(new SubProgressMonitor(pm, 5));
			if (result.hasFatalError())
				return result;

			initializeExcludedParameterNames();
			return result;
		} finally {
			pm.done();
		}	
	}

	private void initializeSelectedExpression() throws JavaModelException {
		IASTFragment fragment= ASTFragmentFactory.createFragmentForSourceRange(
				new SourceRange(fSelectionStart, fSelectionLength), fSource.root, fSource.unit);
		
		if (fragment instanceof IExpressionFragment) {
			//TODO: doesn't handle selection of partial Expressions
			Expression expression= ((IExpressionFragment) fragment).getAssociatedExpression();
			if (fragment.getStartPosition() == expression.getStartPosition()
					&& fragment.getLength() == expression.getLength())
				fSelectedExpression= expression;
		}
	}
	
	private RefactoringStatus checkSelection(IProgressMonitor pm) {
		if (fSelectedExpression == null){
			String message= RefactoringCoreMessages.getString("IntroduceParameterRefactoring.select");//$NON-NLS-1$
			return CodeRefactoringUtil.checkMethodSyntaxErrors(fSelectionStart, fSelectionLength, fSource.root, message);
		}	
		
		fMethodDeclaration= (MethodDeclaration) ASTNodes.getParent(fSelectedExpression, MethodDeclaration.class);
		if (fMethodDeclaration == null)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("IntroduceParameterRefactoring.expression_in_method")); //$NON-NLS-1$
		if (fMethodDeclaration.resolveBinding() == null)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("IntroduceParameterRefactoring.no_binding")); //$NON-NLS-1$
		//TODO: check for rippleMethods -> find matching fragments, consider callers of all rippleMethods
		
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkExpression());
		if (result.hasFatalError())
			return result;
		
		result.merge(checkExpressionBinding());
		if (result.hasFatalError())
			return result;				
		
//			if (isUsedInForInitializerOrUpdater(getSelectedExpression().getAssociatedExpression()))
//				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractTempRefactoring.for_initializer_updater")); //$NON-NLS-1$
//			pm.worked(1);				
//
//			if (isReferringToLocalVariableFromFor(getSelectedExpression().getAssociatedExpression()))
//				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractTempRefactoring.refers_to_for_variable")); //$NON-NLS-1$
//			pm.worked(1);
		
		return result;		
	}

	private RefactoringStatus checkExpression() {
		//TODO: adjust error messages (or generalize for all refactorings on expression-selections?)
		Expression selectedExpression= fSelectedExpression;
		
		if (selectedExpression instanceof Name && selectedExpression.getParent() instanceof ClassInstanceCreation)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractTempRefactoring.name_in_new")); //$NON-NLS-1$
			//TODO: let's just take the CIC automatically (no ambiguity -> no problem -> no dialog ;-)
		
		if (selectedExpression instanceof NullLiteral) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractTempRefactoring.null_literals")); //$NON-NLS-1$
		} else if (selectedExpression instanceof ArrayInitializer) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractTempRefactoring.array_initializer")); //$NON-NLS-1$
		} else if (selectedExpression instanceof Assignment) {
			if (selectedExpression.getParent() instanceof Expression)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractTempRefactoring.assignment")); //$NON-NLS-1$
			else
				return null;
		
		} else if (selectedExpression instanceof ConditionalExpression) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractTempRefactoring.single_conditional_expression")); //$NON-NLS-1$
		} else if (selectedExpression instanceof SimpleName){
			if ((((SimpleName)selectedExpression)).isDeclaration())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractTempRefactoring.names_in_declarations")); //$NON-NLS-1$
		} 
		
		return null;
	}

	private RefactoringStatus checkExpressionBinding() {
		return checkExpressionFragmentIsRValue();
	}
	
	// !! +/- same as in ExtractConstantRefactoring & ExtractTempRefactoring
	private RefactoringStatus checkExpressionFragmentIsRValue() {
		switch(Checks.checkExpressionIsRValue(fSelectedExpression)) {
			case Checks.NOT_RVALUE_MISC:
				return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.getString("IntroduceParameterRefactoring.select"), null, Corext.getPluginId(), RefactoringStatusCodes.EXPRESSION_NOT_RVALUE, null); //$NON-NLS-1$
			case Checks.NOT_RVALUE_VOID:
				return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.getString("IntroduceParameterRefactoring.no_void"), null, Corext.getPluginId(), RefactoringStatusCodes.EXPRESSION_NOT_RVALUE_VOID, null); //$NON-NLS-1$
			case Checks.IS_RVALUE:
				return new RefactoringStatus();
			default:
				Assert.isTrue(false); return null;
		}		
	}	

	
//--- Input setting/validation

	public void setParameterName(String name) {
		Assert.isNotNull(name);
		fParameterName= name;
	}
	
	/** must only be called <i>after</i> checkActivation() */
	public String guessedParameterName() {
		String candidate= guessParameterNameFromExpression(fSelectedExpression);
		if (candidate != null)
			return candidate;
		return fParameterName;
	}

	private String guessParameterNameFromExpression(Expression selectedExpression) {
		ITypeBinding expressionBinding= selectedExpression.resolveTypeBinding();
			
		String packageName= getPackageName(expressionBinding);
		String typeName= getQualifiedName(expressionBinding);
		if (typeName.length() == 0)
			typeName= expressionBinding.getName();
		if (typeName.length() == 0)			
			return fParameterName;
		String[] candidates= NamingConventions.suggestArgumentNames(fSourceCU.getJavaProject(),
				packageName, typeName, expressionBinding.getDimensions(), fExcludedParameterNames);
		if (candidates.length > 0)
			return candidates[0];
		return null;
	}
	
	private static String getPackageName(ITypeBinding typeBinding) {
		if (typeBinding.getPackage() != null)
			return typeBinding.getPackage().getName();
		else
			return ""; //$NON-NLS-1$
	}

	private static String getQualifiedName(ITypeBinding typeBinding) {
		if (typeBinding.isAnonymous())
			return getQualifiedName(typeBinding.getSuperclass());
		if (! typeBinding.isArray())
			return typeBinding.getQualifiedName();
		else
			return typeBinding.getElementType().getQualifiedName();
	}

	private void initializeExcludedParameterNames() {
		IBinding[] bindings= new ScopeAnalyzer(fSource.root).getDeclarationsInScope(
				fSelectedExpression.getStartPosition(), ScopeAnalyzer.VARIABLES);
		fExcludedParameterNames= new String[bindings.length];
		for (int i= 0; i < fExcludedParameterNames.length; i++) {
			fExcludedParameterNames[i]= bindings[i].getName();
		}
	}
	
	public RefactoringStatus validateInput() {
		RefactoringStatus status= checkExcludedParameterNames();
		if (! status.isOK())
			return status;
		else
			return Checks.checkTempName(fParameterName);
	}
	
	private RefactoringStatus checkExcludedParameterNames() {
		for (int i= 0; i < fExcludedParameterNames.length; i++) {
			if (fParameterName.equals(fExcludedParameterNames[i]))
			return RefactoringStatus.createErrorStatus(RefactoringCoreMessages.getString("IntroduceParameterRefactoring.duplicate_name")); //$NON-NLS-1$
		}
		return new RefactoringStatus();
	}
	
//--- checkInput
	
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		pm.beginTask(RefactoringCoreMessages.getString("IntroduceParameterRefactoring.preview"), 5); //$NON-NLS-1$
		RefactoringStatus result= checkExcludedParameterNames();
		if (result.hasFatalError())
			return result;
		
		// TODO: check for name clashes in ripple methods, ...
		
		fChange= new DynamicValidationStateChange(RefactoringCoreMessages.getString("IntroduceParameterRefactoring.introduce_parameter")); //$NON-NLS-1$
		fSource.reset(fSettings);
		changeSource();
		pm.worked(1);
		
		result.merge(changeReferences(new SubProgressMonitor(pm, 3)));
		
		fChange.add(fSource.createChange()); //ASTData#createChange() should add a GroupDescription "Update imports"

		HashSet cus= new HashSet();
		cus.add(fSource.unit);
		cus.addAll(Arrays.asList(fAffectedCUs));
		result.merge(
			Checks.validateModifiesFiles(ResourceUtil.getFiles(
				(ICompilationUnit[])cus.toArray(new ICompilationUnit[cus.size()]))));
		if (result.hasFatalError())
			return result;

		pm.worked(1);

		return result;
	}
	
	private void changeSource() {
		AST ast= fSource.root.getAST();

		//replace selected expression
		ASTNode newExpression= ast.newSimpleName(fParameterName);
		fSource.rewriter.replace(fSelectedExpression, newExpression,
				fSource.createGroupDescription(RefactoringCoreMessages.getString("IntroduceParameterRefactoring.replace"))); //$NON-NLS-1$
		
		//add parameter
		SingleVariableDeclaration param= ast.newSingleVariableDeclaration();
		param.setName(ast.newSimpleName(fParameterName));
		String type= fSource.imports.addImport(fSelectedExpression.resolveTypeBinding());
		param.setType((Type) fSource.rewriter.createStringPlaceholder(type, ASTNode.SIMPLE_TYPE));
		fMethodDeclaration.parameters().add(param);
		fSource.rewriter.markAsInserted(param,
				fSource.createGroupDescription(RefactoringCoreMessages.getString("IntroduceParameterRefactoring.add_parameter"))); //$NON-NLS-1$
	}
	
	private RefactoringStatus changeReferences(SubProgressMonitor pm) throws CoreException {
		pm.beginTask("", 2); //$NON-NLS-1$
		fAffectedCUs= findAffectedCompilationUnits(new SubProgressMonitor(pm, 1));
		IMethodBinding method= fMethodDeclaration.resolveBinding();
		SubProgressMonitor sub= new SubProgressMonitor(pm, 1);
		sub.beginTask("", fAffectedCUs.length); //$NON-NLS-1$
		for (int i= 0; i < fAffectedCUs.length; i++) {
			ASTData ast= getASTData(fAffectedCUs[i]);
			ReferenceAnalyzer analyzer= new ReferenceAnalyzer(ast, method, fSelectedExpression);
			ast.root.accept(analyzer);
			if (ast != fSource)
				fChange.add(ast.createChange());
			sub.worked(1);
		}
		return new RefactoringStatus();
	}
	
	private static class ReferenceAnalyzer extends ASTVisitor {
		private ASTData fAst;
		private IMethodBinding fMethodBinding;
		private Expression fExpression;

		public ReferenceAnalyzer(ASTData astData, IMethodBinding methodBinding, Expression expression) {
			fExpression= expression;
			fAst= astData;
			fMethodBinding= methodBinding;
		}
		
		public boolean visit(MethodInvocation node) {
			if (Bindings.equals(fMethodBinding, node.resolveMethodBinding())) {
				Expression argument= (Expression) ASTNode.copySubtree(fAst.root.getAST(), fExpression);
				node.arguments().add(argument);
				fAst.rewriter.markAsInserted(argument,
						fAst.createGroupDescription(RefactoringCoreMessages.getString("IntroduceParameterRefactoring.add_argument"))); //$NON-NLS-1$
			}
			return super.visit(node);
		}
	}
	
	private ASTData getASTData(ICompilationUnit unit) throws CoreException {
		if (fSource.unit.equals(unit))
			return fSource;
		return new ASTData(unit, true, fSettings);
	}
	
	private ICompilationUnit[] findAffectedCompilationUnits(IProgressMonitor pm) throws CoreException {
		IMethod method= Bindings.findMethod(fMethodDeclaration.resolveBinding(), fSourceCU.getJavaProject());
		Assert.isTrue(method != null);
		ICompilationUnit[] result= RefactoringSearchEngine.findAffectedCompilationUnits(
			SearchPattern.createPattern(method, IJavaSearchConstants.REFERENCES), RefactoringScopeFactory.create(method),
			pm);
		return result;
	}
	
	public Change createChange(IProgressMonitor pm) throws CoreException {
		pm.done();
		return fChange;
	}
}
