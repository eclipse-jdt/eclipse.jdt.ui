/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.dom.fragments.ASTFragmentFactory;
import org.eclipse.jdt.internal.corext.dom.fragments.IASTFragment;
import org.eclipse.jdt.internal.corext.dom.fragments.IExpressionFragment;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaRefactorings;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextBufferChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringAnalyzeUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

/**
 * Extract Local Variable (from selected expression inside method).
 */
public class ExtractTempRefactoring extends Refactoring {
	
	private static final String[] KNOWN_METHOD_NAME_PREFIXES= {"get", "is"}; //$NON-NLS-2$ //$NON-NLS-1$
			
	private final int fSelectionStart;
	private final int fSelectionLength;
	private final ICompilationUnit fCu;
	private final CodeGenerationSettings fSettings;
			
	private boolean fReplaceAllOccurrences;
	private boolean fDeclareFinal;
	private String fTempName;
	private CompilationUnit fCompilationUnitNode;
	
	private IExpressionFragment fSelectedExpression; //cache

	private ExtractTempRefactoring(ICompilationUnit cu, int selectionStart, int selectionLength, CodeGenerationSettings settings) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		Assert.isTrue(cu.exists());
		Assert.isNotNull(settings);	
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fCu= cu;
		fSettings= settings;
		
		fReplaceAllOccurrences= true; //default
		fDeclareFinal= false; //default
		fTempName= ""; //$NON-NLS-1$
	}

	public static boolean isAvailable(ASTNode[] selectedNodes, ASTNode coveringNode) {
		return Checks.isExtractableExpression(selectedNodes, coveringNode);
	}

	public static ExtractTempRefactoring create(ICompilationUnit cu, int selectionStart, int selectionLength, CodeGenerationSettings settings) {
		return new ExtractTempRefactoring(cu, selectionStart, selectionLength, settings);
	}
	
	public String getName() {
		return RefactoringCoreMessages.getString("ExtractTempRefactoring.name"); //$NON-NLS-1$
	}

	public boolean declareFinal() {
		return fDeclareFinal;
	}

	public void setDeclareFinal(boolean declareFinal) {
		fDeclareFinal= declareFinal;
	}

	public boolean replaceAllOccurrences() {
		return fReplaceAllOccurrences;
	}

	public void setReplaceAllOccurrences(boolean replaceAllOccurrences) {
		fReplaceAllOccurrences= replaceAllOccurrences;
	}

	public String guessTempName() throws JavaModelException{
		IExpressionFragment selected= getSelectedExpression();
		ASTNode associatedNode= selected.getAssociatedNode();
		if (associatedNode instanceof MethodInvocation){
			String candidate= guessTempNameFromMethodInvocation((MethodInvocation)associatedNode);
			if (candidate != null)
				return candidate;
		}
		if (associatedNode instanceof Expression) {
			String candidate= guessTempNameFromExpression((Expression)associatedNode);
			if (candidate != null)
				return candidate;			
		}
		return fTempName;
	}
	
	private static String guessTempNameFromMethodInvocation(MethodInvocation selectedMethodInvocation) {
		for (int i= 0; i < KNOWN_METHOD_NAME_PREFIXES.length; i++) {
			String proposal= tryTempNamePrefix(KNOWN_METHOD_NAME_PREFIXES[i], selectedMethodInvocation.getName().getIdentifier());
			if (proposal != null)
				return proposal;
		}
		return null;
	}
	
	private String guessTempNameFromExpression(Expression selectedExpression) throws JavaModelException {
		ITypeBinding expressionBinding= selectedExpression.resolveTypeBinding();
			
		String packageName= getPackageName(expressionBinding);
		String typeName= getQualifiedName(expressionBinding);
		if (typeName.length() == 0)
			typeName= expressionBinding.getName();
		if (typeName.length() == 0)			
			return fTempName;
		String[] candidates= NamingConventions.suggestLocalVariableNames(fCu.getJavaProject(), packageName, typeName, expressionBinding.getDimensions(), getExcludedVariableNames());
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

	private String[] getExcludedVariableNames() throws JavaModelException {
		IBinding[] bindings= new ScopeAnalyzer(fCompilationUnitNode).getDeclarationsInScope(getSelectedExpression().getStartPosition(), ScopeAnalyzer.VARIABLES);
		String[] names= new String[bindings.length];
		for (int i= 0; i < names.length; i++) {
			names[i]= bindings[i].getName();
		}
		return names;
	}

	private static String getQualifiedName(ITypeBinding typeBinding) {
		if (typeBinding.isAnonymous())
			return getQualifiedName(typeBinding.getSuperclass());
		if (! typeBinding.isArray())
			return typeBinding.getQualifiedName();
		else
			return typeBinding.getElementType().getQualifiedName();
	}

	private static String tryTempNamePrefix(String prefix, String methodName){
		if (! methodName.startsWith(prefix))
			return null;
		if (methodName.length() <= prefix.length())
			return null;
		char firstAfterPrefix= methodName.charAt(prefix.length());
		if (! Character.isUpperCase(firstAfterPrefix))
			return null;
		return Character.toLowerCase(firstAfterPrefix) + methodName.substring(prefix.length() + 1);
	}
	
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 6); //$NON-NLS-1$
			
			RefactoringStatus result= Checks.validateModifiesFiles(ResourceUtil.getFiles(new ICompilationUnit[]{fCu}));
			if (result.hasFatalError())
				return result;
				
			if (! fCu.isStructureKnown())		
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractTempRefactoring.syntax_error")); //$NON-NLS-1$
		
			initializeAST();
		
			return checkSelection(new SubProgressMonitor(pm, 5));
		} finally{
			pm.done();
		}	
	}
	
	private RefactoringStatus checkSelection(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 8); //$NON-NLS-1$
	
			IExpressionFragment selectedExpression= getSelectedExpression();
			
			if (selectedExpression == null){
				String message= RefactoringCoreMessages.getString("ExtractTempRefactoring.select_expression");//$NON-NLS-1$
				return CodeRefactoringUtil.checkMethodSyntaxErrors(fSelectionStart, fSelectionLength, fCompilationUnitNode, message);
			}	
			pm.worked(1);
			
			if (isUsedInExplicitConstructorCall())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractTempRefactoring.explicit_constructor")); //$NON-NLS-1$
			pm.worked(1);				
			
			if (getSelectedMethodNode() == null)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractTempRefactoring.expression_in_method"));			 //$NON-NLS-1$
			pm.worked(1);				
			
			if (selectedExpression.getAssociatedNode() instanceof Name && selectedExpression.getAssociatedNode().getParent() instanceof ClassInstanceCreation)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractTempRefactoring.name_in_new")); //$NON-NLS-1$
			pm.worked(1);				

			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkExpression());
			if (result.hasFatalError())
				return result;
			pm.worked(1);
			
			result.merge(checkExpressionBinding());
			if (result.hasFatalError())
				return result;				
			pm.worked(1);
			
			if (isUsedInForInitializerOrUpdater(getSelectedExpression().getAssociatedExpression()))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractTempRefactoring.for_initializer_updater")); //$NON-NLS-1$
			pm.worked(1);				

			if (isReferringToLocalVariableFromFor(getSelectedExpression().getAssociatedExpression()))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractTempRefactoring.refers_to_for_variable")); //$NON-NLS-1$
			pm.worked(1);
			
			return result;
		} finally{
			pm.done();
		}		
	}

	private RefactoringStatus checkExpressionBinding() throws JavaModelException{
		return checkExpressionFragmentIsRValue();
	}
	
	// !! Same as in ExtractConstantRefactoring
	private RefactoringStatus checkExpressionFragmentIsRValue() 
		throws JavaModelException
	{
		switch(Checks.checkExpressionIsRValue(getSelectedExpression().getAssociatedExpression())) {
			case Checks.NOT_RVALUE_MISC:	return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.getString("ExtractTempRefactoring.select_expression"), null, null, RefactoringStatusCodes.EXPRESSION_NOT_RVALUE); //$NON-NLS-1$
			case Checks.NOT_RVALUE_VOID:	return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.getString("ExtractTempRefactoring.no_void"), null, null, RefactoringStatusCodes.EXPRESSION_NOT_RVALUE_VOID); //$NON-NLS-1$
			case Checks.IS_RVALUE:			return new RefactoringStatus();
			default:						Assert.isTrue(false); return null;
		}		
	}	
	
	private void initializeAST() throws JavaModelException {
		fCompilationUnitNode= AST.parseCompilationUnit(fCu, true);
	}

	private RefactoringStatus checkExpression() throws JavaModelException {
		Expression selectedExpression= getSelectedExpression().getAssociatedExpression();
			
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
	
	public RefactoringStatus checkTempName(String newName) {
		return Checks.checkTempName(newName);
	}
	
	public void setTempName(String newName) {
		fTempName= newName;
	}
	
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		TextBuffer buffer= null;
		try{
			buffer= TextBuffer.acquire((IFile)WorkingCopyUtil.getOriginal(fCu).getResource());
			pm.beginTask(RefactoringCoreMessages.getString("ExtractTempRefactoring.checking_preconditions"), 1); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			
			result.merge(checkMatchingFragments());
			
			TextChange change= new TextBufferChange(RefactoringCoreMessages.getString("RenameTempRefactoring.rename"), TextBuffer.create(fCu.getSource())); //$NON-NLS-1$
			change.addTextEdit("", getAllEdits(buffer));//$NON-NLS-1$
			String newCuSource= change.getPreviewContent();
			CompilationUnit newCUNode= AST.parseCompilationUnit(newCuSource.toCharArray(), fCu.getElementName(), fCu.getJavaProject());
			IProblem[] newProblems= RefactoringAnalyzeUtil.getIntroducedCompileProblems(newCUNode, fCompilationUnitNode);
			for (int i= 0; i < newProblems.length; i++) {
                IProblem problem= newProblems[i];
                if (problem.isError())
                	result.addEntry(JavaRefactorings.createStatusEntry(problem, newCuSource));
            }
			return result;
		} catch (JavaModelException e){
			throw e;
		} catch (CoreException e){
			throw new JavaModelException(e);	
		} finally {
			if (buffer != null)
				TextBuffer.release(buffer);
		}	
	}

	private RefactoringStatus checkMatchingFragments() throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		IASTFragment[] matchingFragments= getMatchingFragments();
		for (int i= 0; i < matchingFragments.length; i++) {
			ASTNode node= matchingFragments[i].getAssociatedNode();
			if (isLeftValue(node) && !isReferringToLocalVariableFromFor((Expression) node)) {
				String msg= RefactoringCoreMessages.getString("ExtractTempRefactoring.assigned_to"); //$NON-NLS-1$
				result.addWarning(msg, JavaStatusContext.create(fCu, node));
			}
		}
		return result;
	}
	
	public String getTempSignaturePreview() throws JavaModelException{
		return getTempTypeName() + " " + fTempName; //$NON-NLS-1$
	}
	
	private boolean isUsedInExplicitConstructorCall() throws JavaModelException {
		Expression selectedExpression= getSelectedExpression().getAssociatedExpression();
		if (ASTNodes.getParent(selectedExpression, ConstructorInvocation.class) != null)
			return true;
		if (ASTNodes.getParent(selectedExpression, SuperConstructorInvocation.class) != null)
			return true;
		return false;	
	}
	
	private static boolean isUsedInForInitializerOrUpdater(Expression expression) {
		ASTNode parent= expression.getParent();
		if (parent instanceof ForStatement) {
			ForStatement forStmt= (ForStatement) parent;
			return forStmt.initializers().contains(expression)
					|| forStmt.updaters().contains(expression);
		}
		return false;	
	}
		
	private static boolean isReferringToLocalVariableFromFor(Expression expression) {
		ASTNode current= expression;
		ASTNode parent= current.getParent();
		while (parent != null && !(parent instanceof BodyDeclaration)) {
			if (parent instanceof ForStatement) {
				ForStatement forStmt = (ForStatement) parent;
				if (forStmt.initializers().contains(current) || forStmt.updaters().contains(current) || forStmt.getExpression() == current) {
					List initializers= forStmt.initializers();
					if (initializers.size() == 1 && initializers.get(0) instanceof VariableDeclarationExpression) {
						List forInitializerVariables= getForInitializedVariables((VariableDeclarationExpression) initializers.get(0));
						ForStatementChecker checker= new ForStatementChecker(forInitializerVariables);
						expression.accept(checker);
						if (checker.isReferringToForVariable())
							return true;
					}
				}
			}
			current= parent;
			parent= current.getParent();
		}
		return false;
	}
	
	//return List<IVariableBinding>
	private static List getForInitializedVariables(VariableDeclarationExpression variableDeclarations) {
		List forInitializerVariables= new ArrayList(1);
		for (Iterator iter= variableDeclarations.fragments().iterator(); iter.hasNext(); ) {
			VariableDeclarationFragment fragment= (VariableDeclarationFragment) iter.next();
			IVariableBinding binding= fragment.resolveBinding();
			if (binding != null)
				forInitializerVariables.add(binding);
		}
		return forInitializerVariables;
	}

	private static final class ForStatementChecker extends ASTVisitor {
		private final Collection fForInitializerVariables;
		private boolean fReferringToForVariable= false;
		
		public ForStatementChecker(Collection forInitializerVariables) {
			Assert.isNotNull(forInitializerVariables);
			fForInitializerVariables= forInitializerVariables;
		}

		public boolean isReferringToForVariable () {
			return fReferringToForVariable;
		}

		public boolean visit(SimpleName node) {
			IBinding binding= node.resolveBinding();
			if (binding != null && fForInitializerVariables.contains(binding)) {
				fReferringToForVariable= true;
			}
			return false;
		}
	}

	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		TextBuffer buffer= null;		
		try{
			buffer= TextBuffer.acquire((IFile)WorkingCopyUtil.getOriginal(fCu).getResource());
			pm.beginTask(RefactoringCoreMessages.getString("ExtractTempRefactoring.preview"), 3);	 //$NON-NLS-1$
			TextChange change= new CompilationUnitChange(RefactoringCoreMessages.getString("ExtractTempRefactoring.extract_temp"), fCu); //$NON-NLS-1$
			addTempDeclaration(change);
			pm.worked(1);
			addImportIfNeeded(change, buffer);
			pm.worked(1);
			addReplaceExpressionWithTemp(change);
			pm.worked(1);
			
			return change;
		} catch (JavaModelException e){
			throw e;
		} catch (CoreException e){
			throw new JavaModelException(e);	
		} finally{
			if (buffer != null)
				TextBuffer.release(buffer);
			pm.done();
		}	
	}
	
	private TextEdit[] getAllEdits(TextBuffer buffer) throws CoreException{
		Collection edits= new ArrayList(3);
		edits.add(createTempDeclarationEdit());
		TextEdit importEdit= createImportEditIfNeeded(buffer);
		if (importEdit != null)
			edits.add(importEdit);
		TextEdit[] replaceEdits= createReplaceExpressionWithTempEdits(); 	
		for (int i= 0; i < replaceEdits.length; i++) {
			edits.add(replaceEdits[i]);
		}
		return (TextEdit[]) edits.toArray(new TextEdit[edits.size()]);
	}

	private TextEdit createTempDeclarationEdit() throws CoreException {
		if (shouldReplaceSelectedExpressionWithTempDeclaration())
			return replaceSelectedExpressionWithTempDeclaration();
		else	
			return createAndInsertTempDeclaration();
	}
	
	private boolean shouldReplaceSelectedExpressionWithTempDeclaration() throws JavaModelException {
		IExpressionFragment selectedFragment= getSelectedExpression();
		return    selectedFragment.getAssociatedNode().getParent() instanceof ExpressionStatement
		        && selectedFragment.matches(ASTFragmentFactory.createFragmentForFullSubtree(selectedFragment.getAssociatedNode()));
	}

	private TextEdit createAndInsertTempDeclaration() throws CoreException {
		ASTNode insertBefore= getNodeToInsertTempDeclarationBefore();		
		int insertOffset= insertBefore.getStartPosition();
		String text= createTempDeclarationSource(getInitializerSource(), true) + getIndent(insertBefore);
		return new InsertEdit(insertOffset, text); 
	}

	private TextEdit replaceSelectedExpressionWithTempDeclaration() throws CoreException {
		/* This is used in cases where the selected expression is the full expression part of an ExpressionStatement,
		 * in which case the expression is not replaced by a variable reference,
		 * but, rather, its parent ExpressionStatement is replaced by the new
		 * local variable declaration.
		 */
		
		String nodeSource= fCu.getBuffer().getText(getSelectedExpression().getStartPosition(), getSelectedExpression().getLength());
		String text= createTempDeclarationSource(nodeSource, false);
		ASTNode parent= getSelectedExpression().getAssociatedNode().getParent();
		Assert.isTrue(parent instanceof ExpressionStatement);
		
		return new ReplaceEdit(parent.getStartPosition(), parent.getLength(), text);
	}

	private TextEdit createImportEditIfNeeded(TextBuffer buffer) throws CoreException {
		ITypeBinding type= getSelectedExpression().getAssociatedExpression().resolveTypeBinding();
		if (type.isPrimitive())
			return null;
		if (type.isArray() && type.getElementType().isPrimitive())	
			return null;
			
		ImportRewrite rewrite= new ImportRewrite(fCu, fSettings);
		rewrite.addImport(type);
		if (rewrite.isEmpty())
			return null;
		else	
			return rewrite.createEdit(buffer);
	}

	private TextEdit[] createReplaceExpressionWithTempEdits() throws JavaModelException {
		IASTFragment[] fragmentsToReplace= getFragmentsToReplace();
		TextEdit[] result= new TextEdit[fragmentsToReplace.length];
		for (int i= 0; i < fragmentsToReplace.length; i++) {
			IASTFragment fragment= fragmentsToReplace[i];
			int offset= fragment.getStartPosition();
			int length= fragment.getLength();
			result[i]= new ReplaceEdit(offset, length, fTempName);
		}
		return result;
	}

	private void addTempDeclaration(TextChange change) throws CoreException {
		change.addTextEdit(RefactoringCoreMessages.getString("ExtractTempRefactoring.declare_local_variable"), createTempDeclarationEdit()); //$NON-NLS-1$
	}

	private void addImportIfNeeded(TextChange change, TextBuffer buffer) throws CoreException {
		TextEdit importEdit= createImportEditIfNeeded(buffer);
		if (importEdit != null)
			change.addTextEdit(RefactoringCoreMessages.getString("ExtractTempRefactoring.update_imports"), importEdit); //$NON-NLS-1$
	}

	private void addReplaceExpressionWithTemp(TextChange change) throws JavaModelException {
		TextEdit[] edits= createReplaceExpressionWithTempEdits();
		for (int i= 0; i < edits.length; i++) {
			change.addTextEdit(RefactoringCoreMessages.getString("ExtractTempRefactoring.replace"), edits[i]); //$NON-NLS-1$		
		}
	}
			
	private ASTNode getNodeToInsertTempDeclarationBefore() throws JavaModelException {
		if ((! fReplaceAllOccurrences) || (getFragmentsToReplace().length == 1))
			return getInnermostStatementInBlock(getSelectedExpression());
		
		ASTNode[] firstReplaceNodeParents= getParents(getFirstReplacedExpression().getAssociatedNode());
		ASTNode[] commonPath= findDeepestCommonSuperNodePathForReplacedNodes();
		Assert.isTrue(commonPath.length <= firstReplaceNodeParents.length);
		
		ASTNode deepestCommonParent= firstReplaceNodeParents[commonPath.length - 1];
		if (deepestCommonParent instanceof TryStatement || deepestCommonParent instanceof IfStatement){
			if (deepestCommonParent.getParent() instanceof Block)
				return deepestCommonParent;
			if (commonPath.length < firstReplaceNodeParents.length)
				return getInnermostStatementInBlock(firstReplaceNodeParents[commonPath.length]);
		}
		
		if (deepestCommonParent instanceof Block)
			return firstReplaceNodeParents[commonPath.length];

		return getInnermostStatementInBlock(getFirstReplacedExpression());
	}
	
	private ASTNode[] findDeepestCommonSuperNodePathForReplacedNodes() throws JavaModelException {
		ASTNode[] matchNodes= getMatchNodes();
		
		ASTNode[][] matchingNodesParents= new ASTNode[matchNodes.length][];
		for (int i= 0; i < matchNodes.length; i++) {
			matchingNodesParents[i]= getParents(matchNodes[i]);
		}
		List l=Arrays.asList(getLongestArrayPrefix(matchingNodesParents));
		return (ASTNode[]) l.toArray(new ASTNode[l.size()]);
	}
	
	private ASTNode[] getMatchNodes() throws JavaModelException {
		IASTFragment[] matches= getFragmentsToReplace();
		ASTNode[] result= new ASTNode[matches.length];
		for(int i= 0; i < matches.length; i++)
			result[i]= matches[i].getAssociatedNode();
		return result;
	}
	
	private static Object[] getLongestArrayPrefix(Object[][] arrays){
		int length= -1;
		for (int i= 0; i < arrays[0].length; i++) {
			if (! allArraysEqual(arrays, i))
				break;
			length++;	
		}
		if (length == -1)
			return new Object[0];
		return getArrayPrefix(arrays[0], length + 1);
	}
	
	private static boolean allArraysEqual(Object[][] arrays, int position){
		Object element= arrays[0][position];
		for (int i= 0; i < arrays.length; i++) {
			Object[] array= arrays[i];
			if (! element.equals(array[position]))
				return false;
		}
		return true;
	}
	
	private static Object[] getArrayPrefix(Object[] array, int prefixLength){
		Assert.isTrue(prefixLength <= array.length);
		Assert.isTrue(prefixLength >= 0);
		Object[] prefix= new Object[prefixLength];
		for (int i= 0; i < prefix.length; i++) {
			prefix[i]= array[i];
		}
		return prefix;
	}
	
	private IExpressionFragment getFirstReplacedExpression() throws JavaModelException {
		if (! fReplaceAllOccurrences)
			return getSelectedExpression();
		IASTFragment[] nodesToReplace= getFragmentsToReplace();
		Comparator comparator= new Comparator(){
			public int compare(Object o1, Object o2){
				return ((IASTFragment)o1).getStartPosition() - ((IASTFragment)o2).getStartPosition();
			}	
		};
		Arrays.sort(nodesToReplace, comparator);
		return (IExpressionFragment)nodesToReplace[0];
	}
	
	//without the trailing indent
	private String createTempDeclarationSource(String initializerSource, boolean addTrailingLineDelimiter) throws CoreException {
		String modifier= fDeclareFinal ? "final ": ""; //$NON-NLS-1$ //$NON-NLS-2$
		String dummyInitializer= "0"; //$NON-NLS-1$
		String semicolon= ";"; //$NON-NLS-1$
		String dummyDeclaration= modifier + getTempTypeName() + " " + fTempName + " = " + dummyInitializer + semicolon; //$NON-NLS-1$ //$NON-NLS-2$
		int[] position= {dummyDeclaration.length() - dummyInitializer.length()  - semicolon.length()};
  String formattedDeclaration= CodeFormatterUtil.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS, dummyDeclaration, 0, position, getLineDelimiter(), null);
		StringBuffer formattedDummyDeclaration= new StringBuffer(formattedDeclaration);
		String tail= addTrailingLineDelimiter ? getLineDelimiter(): ""; //$NON-NLS-1$
		return formattedDummyDeclaration.replace(position[0], position[0] + dummyInitializer.length(), initializerSource)
														.append(tail)
														.toString();
	}
	
	private String getTempTypeName() throws JavaModelException {
		Expression expression= getSelectedExpression().getAssociatedExpression();
		String name= expression.resolveTypeBinding().getName();
		if (! "".equals(name) || ! (expression instanceof ClassInstanceCreation)) //$NON-NLS-1$
			return name;
			
			
		ClassInstanceCreation cic= (ClassInstanceCreation)expression;
		Assert.isTrue(cic.getAnonymousClassDeclaration() != null);
		return ASTNodes.asString(cic.getName());
	}
	
	private String getInitializerSource() throws JavaModelException {
		IExpressionFragment fragment= getSelectedExpression();
		return removeTrailingSemicolons(fCu.getBuffer().getText(fragment.getStartPosition(), fragment.getLength()));
	}
	
	//recursive
	private static String removeTrailingSemicolons(String s){
		String arg= s.trim();
		if (! arg.endsWith(";")) //$NON-NLS-1$
			return arg;
		return removeTrailingSemicolons(arg.substring(0, arg.length() - 1));	
	}

	private String getIndent(ASTNode insertBefore) throws CoreException {
		return CodeFormatterUtil.createIndentString(CodeRefactoringUtil.getIndentationLevel(insertBefore, getFile()));
	}
	
	private String getLineDelimiter() throws CoreException {
		TextBuffer buffer= null;
		try{
			buffer= TextBuffer.acquire(getFile());
			return buffer.getLineDelimiter(buffer.getLineOfOffset(fSelectionStart));
		} finally {
			if (buffer != null)
				TextBuffer.release(buffer);
		}
	}
	
	private Statement getInnermostStatementInBlock(IASTFragment fragment) {
		return getInnermostStatementInBlock(fragment.getAssociatedNode());
	}

	private Statement getInnermostStatementInBlock(ASTNode node) {
		Block block= (Block)ASTNodes.getParent(node, Block.class);
		if (block == null)
			return null;
		for (Iterator iter= block.statements().iterator(); iter.hasNext();) {
			Statement statement= (Statement) iter.next();
			if (ASTNodes.isParent(node, statement))
				return statement;
		}
		return null;
	}
	
	private IASTFragment[] getFragmentsToReplace() throws JavaModelException {
		return retainOnlyReplacableMatches(getMatchingFragments());
	}
	
	private IASTFragment[] getMatchingFragments() throws JavaModelException {
		if (fReplaceAllOccurrences){
			IASTFragment[] allMatches= ASTFragmentFactory.createFragmentForFullSubtree(getSelectedMethodNode())
														 .getSubFragmentsMatching(getSelectedExpression());
			return allMatches;					 
		} else 
			return new IASTFragment[]{getSelectedExpression()};	
	}

    private static IASTFragment[] retainOnlyReplacableMatches(IASTFragment[] allMatches) {
    	List result= new ArrayList(allMatches.length);
    	for (int i= 0; i < allMatches.length; i++) {
            if (canReplace(allMatches[i]))
            	result.add(allMatches[i]);
        }
        return (IASTFragment[]) result.toArray(new IASTFragment[result.size()]);
    }

    private static boolean canReplace(IASTFragment fragment) {
    	ASTNode node= fragment.getAssociatedNode();
    	ASTNode parent= node.getParent();
		if (parent instanceof VariableDeclarationFragment){
    		VariableDeclarationFragment vdf= (VariableDeclarationFragment)parent;
    		if (node.equals(vdf.getName()))
    			return false;
    	}
    	if (isMethodParameter(node))
			return false;	
		if (isThrowableInCatchBlock(node))
			return false;
    	if (parent instanceof ExpressionStatement)
    		return false;	
    	if (isLeftValue(node))
			return false;
		if (isReferringToLocalVariableFromFor((Expression) node))
			return false;
		if (isUsedInForInitializerOrUpdater((Expression) node))
			return false;
        return true;
	}

	private static boolean isThrowableInCatchBlock(ASTNode node) {
		return (node instanceof SimpleName) 
			 && (node.getParent() instanceof SingleVariableDeclaration)
			 && (node.getParent().getParent() instanceof CatchClause);
	}

	private static boolean isMethodParameter(ASTNode node) {
    	return (node instanceof SimpleName) 
    		&& (node.getParent() instanceof SingleVariableDeclaration) 
    		&& (node.getParent().getParent() instanceof MethodDeclaration);
	}

	private static boolean isLeftValue(ASTNode node){
		ASTNode parent= node.getParent();
		if (parent instanceof Assignment){
			Assignment assignment= (Assignment)parent;
			if (assignment.getLeftHandSide() == node)
				return true;
		}
		if (parent instanceof PostfixExpression)
			return true;
		if (parent instanceof PrefixExpression){
			PrefixExpression.Operator op= ((PrefixExpression)parent).getOperator();
			if (op.equals(PrefixExpression.Operator.DECREMENT))
				return true;
			if (op.equals(PrefixExpression.Operator.INCREMENT))
				return true;
			return false;	
		}
		return false;	
    }
		
	private MethodDeclaration getSelectedMethodNode() throws JavaModelException {
		return (MethodDeclaration)ASTNodes.getParent(getSelectedExpression().getAssociatedNode(), MethodDeclaration.class);
	}
	
	private static ASTNode[] getParents(ASTNode node){
		ASTNode current= node;
		List parents= new ArrayList();
		do{
			parents.add(current.getParent());
			current= current.getParent();
		} while(current.getParent() != null);
		Collections.reverse(parents);
		return (ASTNode[]) parents.toArray(new ASTNode[parents.size()]);
	}
	
	private IExpressionFragment getSelectedExpression() throws JavaModelException {
		if (fSelectedExpression != null)
			return fSelectedExpression;
		IASTFragment selectedFragment= ASTFragmentFactory.createFragmentForSourceRange(new SourceRange(fSelectionStart, fSelectionLength), fCompilationUnitNode, fCu);
		
		if (selectedFragment instanceof IExpressionFragment)
			fSelectedExpression= (IExpressionFragment) selectedFragment;

		return fSelectedExpression;
	}

	private IFile getFile() throws JavaModelException {
		return ResourceUtil.getFile(fCu);
	}
}
