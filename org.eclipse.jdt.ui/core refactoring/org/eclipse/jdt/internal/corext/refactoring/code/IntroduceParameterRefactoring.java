/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.MethodRefParameter;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.Corext;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.dom.fragments.ASTFragmentFactory;
import org.eclipse.jdt.internal.corext.dom.fragments.IASTFragment;
import org.eclipse.jdt.internal.corext.dom.fragments.IExpressionFragment;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine2;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.JavadocUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.util.SearchUtils;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class IntroduceParameterRefactoring extends Refactoring {
	
	private static final String[] KNOWN_METHOD_NAME_PREFIXES= {"get", "is"}; //$NON-NLS-2$ //$NON-NLS-1$
	
	private ICompilationUnit fSourceCU;
	private int fSelectionStart;
	private int fSelectionLength;

	private String fParameterName;

	private CompilationUnitRewrite fSource;
	private Expression fSelectedExpression;
	private MethodDeclaration fMethodDeclaration;
	private String[] fExcludedParameterNames;
	private CompositeChange fChange;
	private ICompilationUnit[] fAffectedCUs;
	
	
	private IntroduceParameterRefactoring(ICompilationUnit cu, int selectionStart, int selectionLength) {
		Assert.isTrue(cu != null && cu.exists());
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		fSourceCU= cu;
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		
		fParameterName= ""; //$NON-NLS-1$
	}
	
	public static boolean isAvailable(ASTNode[] selectedNodes, ASTNode coveringNode) {
		return Checks.isExtractableExpression(selectedNodes, coveringNode);
	}

	public static IntroduceParameterRefactoring create(ICompilationUnit cu, int selectionStart, int selectionLength) {
		return new IntroduceParameterRefactoring(cu, selectionStart, selectionLength);
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
			
			fSource= new CompilationUnitRewrite(fSourceCU);
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
				new SourceRange(fSelectionStart, fSelectionLength), fSource.getRoot(), fSource.getCu());
		
		if (! (fragment instanceof IExpressionFragment))
			return;
		
		//TODO: doesn't handle selection of partial Expressions
		Expression expression= ((IExpressionFragment) fragment).getAssociatedExpression();
		if (fragment.getStartPosition() != expression.getStartPosition()
				|| fragment.getLength() != expression.getLength())
			return;
		
		if (Checks.isInsideJavadoc(expression))
			return;
		//TODO: exclude invalid selections
		fSelectedExpression= expression;
	}
	
	private RefactoringStatus checkSelection(IProgressMonitor pm) {
		if (fSelectedExpression == null){
			String message= RefactoringCoreMessages.getString("IntroduceParameterRefactoring.select");//$NON-NLS-1$
			return CodeRefactoringUtil.checkMethodSyntaxErrors(fSelectionStart, fSelectionLength, fSource.getRoot(), message);
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
			if (selectedExpression.getParent() instanceof QualifiedName && selectedExpression.getLocationInParent() == QualifiedName.NAME_PROPERTY
					|| selectedExpression.getParent() instanceof FieldAccess && selectedExpression.getLocationInParent() == FieldAccess.NAME_PROPERTY)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractTempRefactoring.select_expression"));//$NON-NLS-1$;
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
	
	/** 
	 * must only be called <i>after</i> checkActivation() 
	 * @return guessed parameter name
	 */
	public String guessedParameterName() {
		String[] proposals= guessParameterNames();
		if (proposals.length == 0)
			return fParameterName;
		else
			return proposals[0];
	}
	
// --- TODO: copied from ExtractTempRefactoring - should extract ------------------------------
	
	/**
	 * Must only be called <i>after</i> checkActivation().
	 * The first proposal should be used as "best guess" (if it exists).
	 * @return proposed variable names (may be empty, but not null).
	 */
	public String[] guessParameterNames() {
		LinkedHashSet proposals= new LinkedHashSet(); //retain ordering, but prevent duplicates
		String[] excludedVariableNames= getExcludedVariableNames();
		if (fSelectedExpression instanceof MethodInvocation){
			proposals.addAll(guessTempNamesFromMethodInvocation((MethodInvocation) fSelectedExpression, excludedVariableNames));
		}
		proposals.addAll(guessTempNamesFromExpression(fSelectedExpression, excludedVariableNames));
		return (String[]) proposals.toArray(new String[proposals.size()]);
	}
	
	private List/*<String>*/ guessTempNamesFromMethodInvocation(MethodInvocation selectedMethodInvocation, String[] excludedVariableNames) {
		String methodName= selectedMethodInvocation.getName().getIdentifier();
		for (int i= 0; i < KNOWN_METHOD_NAME_PREFIXES.length; i++) {
			String prefix= KNOWN_METHOD_NAME_PREFIXES[i];
			if (! methodName.startsWith(prefix))
				continue; //not this prefix
			if (methodName.length() == prefix.length())
				return Collections.EMPTY_LIST; // prefix alone -> don't take method name
			char firstAfterPrefix= methodName.charAt(prefix.length());
			if (! Character.isUpperCase(firstAfterPrefix))
				continue; //not uppercase after prefix
			//found matching prefix
			String proposal= Character.toLowerCase(firstAfterPrefix) + methodName.substring(prefix.length() + 1);
			methodName= proposal;
			break;
		}
		String[] proposals= StubUtility.getLocalNameSuggestions(fSourceCU.getJavaProject(), methodName, 0, excludedVariableNames);
		return Arrays.asList(proposals);
	}
	
	private List/*<String>*/ guessTempNamesFromExpression(Expression selectedExpression, String[] excluded) {
		ITypeBinding expressionBinding= selectedExpression.resolveTypeBinding();
		String typeName= getQualifiedName(expressionBinding);
		if (typeName.length() == 0)
			typeName= expressionBinding.getName();
		if (typeName.length() == 0)			
			return Collections.EMPTY_LIST;
		String[] proposals= StubUtility.getLocalNameSuggestions(fSourceCU.getJavaProject(), typeName, expressionBinding.getDimensions(), excluded);
		return Arrays.asList(proposals);
	}
	
	private String[] getExcludedVariableNames() {
		IBinding[] bindings= new ScopeAnalyzer(fSource.getRoot()).getDeclarationsInScope(fSelectedExpression.getStartPosition(), ScopeAnalyzer.VARIABLES);
		String[] names= new String[bindings.length];
		for (int i= 0; i < names.length; i++) {
			names[i]= bindings[i].getName();
		}
		return names;
	}
	
// ----------------------------------------------------------------------
	
	private static String getQualifiedName(ITypeBinding typeBinding) {
		if (typeBinding.isAnonymous())
			return getQualifiedName(typeBinding.getSuperclass());
		if (! typeBinding.isArray())
			return typeBinding.getQualifiedName();
		else
			return typeBinding.getElementType().getQualifiedName();
	}

	private void initializeExcludedParameterNames() {
		IBinding[] bindings= new ScopeAnalyzer(fSource.getRoot()).getDeclarationsInScope(
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
		fSource.clearASTAndImportRewrites();
		changeSource();
		pm.worked(1);
		
		result.merge(changeReferences(new SubProgressMonitor(pm, 3)));
		
		fChange.add(fSource.createChange());

		HashSet cus= new HashSet();
		cus.add(fSource.getCu());
		cus.addAll(Arrays.asList(fAffectedCUs));
		result.merge(
			Checks.validateModifiesFiles(ResourceUtil.getFiles(
				(ICompilationUnit[])cus.toArray(new ICompilationUnit[cus.size()])),
				getValidationContext()));
		if (result.hasFatalError())
			return result;

		pm.worked(1);

		return result;
	}
	
	private void changeSource() {
		//TODO (47547): for constructors, must update implicit super(..) calls in some subclasses' constructors 
		replaceSelectedExpression();		
		addParameter();
	}
	
	private void replaceSelectedExpression() {
		ASTNode newExpression= fSource.getRoot().getAST().newSimpleName(fParameterName);
		String description= RefactoringCoreMessages.getString("IntroduceParameterRefactoring.replace"); //$NON-NLS-1$
		fSource.getASTRewrite().replace(fSelectedExpression, newExpression, fSource.createGroupDescription(description));
	}

	private void addParameter() {
		AST ast= fSource.getRoot().getAST();
		ASTRewrite astRewrite= fSource.getASTRewrite();
		
		SingleVariableDeclaration param= ast.newSingleVariableDeclaration();
		param.setName(ast.newSimpleName(fParameterName));
		String type= fSource.getImportRewrite().addImport(fSelectedExpression.resolveTypeBinding());
		param.setType((Type) astRewrite.createStringPlaceholder(type, ASTNode.SIMPLE_TYPE));
		ListRewrite parameters= astRewrite.getListRewrite(fMethodDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		String description= RefactoringCoreMessages.getString("IntroduceParameterRefactoring.add_parameter"); //$NON-NLS-1$
		TextEditGroup groupDescription= fSource.createGroupDescription(description);
		parameters.insertLast(param, groupDescription);
		
		JavadocUtil.addParamJavadoc(fParameterName, fMethodDeclaration, fSource.getASTRewrite(),
				fSource.getCu().getJavaProject(), groupDescription);
	}

	private RefactoringStatus changeReferences(SubProgressMonitor pm) throws CoreException {
		pm.beginTask("", 2); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();
		fAffectedCUs= findAffectedCompilationUnits(new SubProgressMonitor(pm, 1), result);
		IMethodBinding method= fMethodDeclaration.resolveBinding();
		SubProgressMonitor sub= new SubProgressMonitor(pm, 1);
		sub.beginTask("", fAffectedCUs.length); //$NON-NLS-1$
		for (int i= 0; i < fAffectedCUs.length; i++) {
			CompilationUnitRewrite ast= getCURewrite(fAffectedCUs[i]);
			ReferenceAnalyzer analyzer= new ReferenceAnalyzer(ast, method, fSelectedExpression);
			ast.getRoot().accept(analyzer);
			if (ast != fSource)
				fChange.add(ast.createChange());
			sub.worked(1);
			if (sub.isCanceled())
				throw new OperationCanceledException();
		}
		return result;
	}
	
	private class ReferenceAnalyzer extends ASTVisitor {
		private CompilationUnitRewrite fCURewrite;
		private IMethodBinding fMethodBinding;
		private Expression fExpression;

		public ReferenceAnalyzer(CompilationUnitRewrite cuRewrite, IMethodBinding methodBinding, Expression expression) {
			super(true);
			fExpression= expression;
			fCURewrite= cuRewrite;
			fMethodBinding= methodBinding;
		}
		
		private void addArgument(ASTRewrite astRewrite, ListRewrite argumentListRewrite) {
			Expression argument;
			if (fExpression.getAST() == astRewrite.getAST()) {
				argument= (Expression) astRewrite.createCopyTarget(fExpression);
			} else {
				try {
					String expression= fSource.getCu().getBuffer().getText(fExpression.getStartPosition(), fExpression.getLength());
					argument= (Expression) astRewrite.createStringPlaceholder(expression, ASTNode.SIMPLE_NAME);
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
					argument= (Expression) ASTNode.copySubtree(fCURewrite.getRoot().getAST(), fExpression);
				}
			}
			String description= RefactoringCoreMessages.getString("IntroduceParameterRefactoring.add_argument"); //$NON-NLS-1$
			argumentListRewrite.insertLast(argument, fCURewrite.createGroupDescription(description));
		}

		public boolean visit(MethodInvocation node) {
			if (Bindings.equals(fMethodBinding, node.resolveMethodBinding())) {
				ASTRewrite astRewrite= fCURewrite.getASTRewrite();
				ListRewrite listRewrite= astRewrite.getListRewrite(node, MethodInvocation.ARGUMENTS_PROPERTY);
				addArgument(astRewrite, listRewrite);
			}
			return super.visit(node);
		}
		
		public boolean visit(ClassInstanceCreation node) {
			if (Bindings.equals(fMethodBinding, node.resolveConstructorBinding())) {
				ASTRewrite astRewrite= fCURewrite.getASTRewrite();
				ListRewrite listRewrite= astRewrite.getListRewrite(node, ClassInstanceCreation.ARGUMENTS_PROPERTY);
				addArgument(astRewrite, listRewrite);
			}
			return super.visit(node);
		}
		
		public boolean visit(ConstructorInvocation node) {
			if (Bindings.equals(fMethodBinding, node.resolveConstructorBinding())) {
				ASTRewrite astRewrite= fCURewrite.getASTRewrite();
				ListRewrite listRewrite= astRewrite.getListRewrite(node, ConstructorInvocation.ARGUMENTS_PROPERTY);
				addArgument(astRewrite, listRewrite);
			}
			return super.visit(node);
		}
		
		public boolean visit(SuperMethodInvocation node) {
			if (Bindings.equals(fMethodBinding, node.resolveMethodBinding())) {
				ASTRewrite astRewrite= fCURewrite.getASTRewrite();
				ListRewrite listRewrite= astRewrite.getListRewrite(node, SuperMethodInvocation.ARGUMENTS_PROPERTY);
				addArgument(astRewrite, listRewrite);
			}
			return super.visit(node);
		}
		
		public boolean visit(SuperConstructorInvocation node) {
			if (Bindings.equals(fMethodBinding, node.resolveConstructorBinding())) {
				ASTRewrite astRewrite= fCURewrite.getASTRewrite();
				ListRewrite listRewrite= astRewrite.getListRewrite(node, SuperConstructorInvocation.ARGUMENTS_PROPERTY);
				addArgument(astRewrite, listRewrite);
			}
			return super.visit(node);
		}
		
		public boolean visit(MethodRef node) {
			if (Bindings.equals(fMethodBinding, node.resolveBinding())) {
				ASTRewrite astRewrite= fCURewrite.getASTRewrite();
				ListRewrite listRewrite= astRewrite.getListRewrite(node, MethodRef.PARAMETERS_PROPERTY);
				
				List parameters= listRewrite.getOriginalList();
				MethodRefParameter newParam= astRewrite.getAST().newMethodRefParameter();
				// only add name iff first parameter already has a name:
				if (parameters.size() > 0)
					if (((MethodRefParameter) parameters.get(0)).getName() != null)
						newParam.setName(astRewrite.getAST().newSimpleName(fParameterName));
				
				String type= fCURewrite.getImportRewrite().addImport(fSelectedExpression.resolveTypeBinding());
				newParam.setType((Type) astRewrite.createStringPlaceholder(type, ASTNode.SIMPLE_TYPE));
				String description= RefactoringCoreMessages.getString("IntroduceParameterRefactoring.add_javadoc_parameter"); //$NON-NLS-1$
				listRewrite.insertLast(newParam, fCURewrite.createGroupDescription(description));
			}
			return false;
		}
	}
	
	private CompilationUnitRewrite getCURewrite(ICompilationUnit unit) {
		if (fSource.getCu().equals(unit))
			return fSource;
		return new CompilationUnitRewrite(unit);
	}

	private ICompilationUnit[] findAffectedCompilationUnits(IProgressMonitor pm, RefactoringStatus status) throws CoreException {
		final IMethod method= (IMethod) fSourceCU.getElementAt(fMethodDeclaration.getName().getStartPosition());
		final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2(SearchPattern.createPattern(method, IJavaSearchConstants.REFERENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE));
		engine.setFiltering(true, true);
		engine.setScope(RefactoringScopeFactory.create(method));
		engine.setStatus(status);
		engine.searchPattern(new SubProgressMonitor(pm, 1));
		return engine.getCompilationUnits();
	}

	public Change createChange(IProgressMonitor pm) throws CoreException {
		pm.done();
		return fChange;
	}
}
