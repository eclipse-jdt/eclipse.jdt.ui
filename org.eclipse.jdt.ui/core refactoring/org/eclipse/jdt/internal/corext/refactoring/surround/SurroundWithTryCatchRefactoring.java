/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.surround;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.CodeScopeBuilder;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.StatementRewrite;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.SelectionAwareSourceRangeComputer;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Surround a set of statements with a try/catch block.
 * 
 * Special case:
 * 
 * URL url= file.toURL();
 * 
 * In this case the variable declaration statement gets convert into a
 * declaration without initializer. So the body of the try/catch block 
 * only consists of new assignments. In this case we can't move the 
 * selected nodes (e.g. the declaration) into the try block.
 */
public class SurroundWithTryCatchRefactoring extends Refactoring {

	private Selection fSelection;
	private ISurroundWithTryCatchQuery fQuery;
	private SurroundWithTryCatchAnalyzer fAnalyzer;
	private boolean fLeaveDirty;

	private ICompilationUnit fCUnit;
	private CompilationUnit fRootNode;
	private ASTRewrite fRewriter;
	private ImportRewrite fImportRewrite;
	private CodeScopeBuilder.Scope fScope;
	private ASTNode[] fSelectedNodes;

	private SurroundWithTryCatchRefactoring(ICompilationUnit cu, Selection selection, ISurroundWithTryCatchQuery query) {
		fCUnit= cu;
		fSelection= selection;
		fQuery= query;
		fLeaveDirty= false;
	}

	public static SurroundWithTryCatchRefactoring create(ICompilationUnit cu, ITextSelection selection, ISurroundWithTryCatchQuery query) {
		return new SurroundWithTryCatchRefactoring(cu, Selection.createFromStartLength(selection.getOffset(), selection.getLength()), query);
	}
		
	public static SurroundWithTryCatchRefactoring create(ICompilationUnit cu, int offset, int length, ISurroundWithTryCatchQuery query) {
		return new SurroundWithTryCatchRefactoring(cu, Selection.createFromStartLength(offset, length), query);
	}

	public void setLeaveDirty(boolean leaveDirty) {
		fLeaveDirty= leaveDirty;
	}
	
	public boolean stopExecution() {
		if (fAnalyzer == null)
			return true;
		ITypeBinding[] exceptions= fAnalyzer.getExceptions();
		return exceptions == null || exceptions.length == 0;
	}
	
	/* non Java-doc
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("SurroundWithTryCatchRefactoring.name"); //$NON-NLS-1$
	}

	public RefactoringStatus checkActivationBasics(CompilationUnit rootNode) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		fRootNode= rootNode;
			
		fAnalyzer= new SurroundWithTryCatchAnalyzer(fCUnit, fSelection, fQuery);
		fRootNode.accept(fAnalyzer);
		result.merge(fAnalyzer.getStatus());
		return result;
	}


	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		CompilationUnit rootNode= new RefactoringASTParser(AST.JLS3).parse(fCUnit, true, pm);
		return checkActivationBasics(rootNode);
	}

	/*
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		return Checks.validateModifiesFiles(
			ResourceUtil.getFiles(new ICompilationUnit[]{fCUnit}),
			getValidationContext());
	}

	/* non Java-doc
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		final String NN= ""; //$NON-NLS-1$
		if (pm == null) pm= new NullProgressMonitor();
		pm.beginTask(NN, 2);
		// This is cheap since the compilation unit is already open in a editor.
		IPath path= getFile().getFullPath();
		ITextFileBufferManager bufferManager= FileBuffers.getTextFileBufferManager();
		try {
			bufferManager.connect(path, new SubProgressMonitor(pm, 1));
			IDocument document= bufferManager.getTextFileBuffer(path).getDocument();
			final CompilationUnitChange result= new CompilationUnitChange(getName(), fCUnit);
			if (fLeaveDirty)
				result.setSaveMode(TextFileChange.LEAVE_DIRTY);
			MultiTextEdit root= new MultiTextEdit();
			result.setEdit(root);
			fRewriter= ASTRewrite.create(fAnalyzer.getEnclosingBodyDeclaration().getAST());
			fRewriter.setTargetSourceRangeComputer(new SelectionAwareSourceRangeComputer(
				fAnalyzer.getSelectedNodes(), document, fSelection.getOffset(), fSelection.getLength()));
			fImportRewrite= new ImportRewrite(fCUnit);
			
			fScope= CodeScopeBuilder.perform(fAnalyzer.getEnclosingBodyDeclaration(), fSelection).
				findScope(fSelection.getOffset(), fSelection.getLength());
			fScope.setCursor(fSelection.getOffset());
			
			fSelectedNodes= fAnalyzer.getSelectedNodes();
			
			StatementRewrite statementRewrite= new StatementRewrite(fRewriter, fAnalyzer.getSelectedNodes());
			List replacements= createTryCatchStatement(document.getLineDelimiter(0));
			statementRewrite.replace((ASTNode[])replacements.toArray(new ASTNode[replacements.size()]), null);
			
			if (!fImportRewrite.isEmpty()) {
				TextEdit edit= fImportRewrite.createEdit(document);
				root.addChild(edit);
				result.addTextEditGroup(new TextEditGroup(NN, new TextEdit[] {edit} ));
			}
			TextEdit change= fRewriter.rewriteAST(document, null);
			root.addChild(change);
			result.addTextEditGroup(new TextEditGroup(NN, new TextEdit[] {change} ));
			return result;
		} catch (BadLocationException e) {
			throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR,
				e.getMessage(), e));
		} finally {
			bufferManager.disconnect(path, new SubProgressMonitor(pm, 1));
			pm.done();
		}
	}
	
	private AST getAST() {
		return fRootNode.getAST();
	}
	
	private List createTryCatchStatement(String lineDelimiter) throws CoreException {
		List result= new ArrayList(1);
		TryStatement tryStatement= getAST().newTryStatement();
		ITypeBinding[] exceptions= fAnalyzer.getExceptions();
		for (int i= 0; i < exceptions.length; i++) {
			ITypeBinding exception= exceptions[i];
			String type= fImportRewrite.addImport(exception);
			CatchClause catchClause= getAST().newCatchClause();
			tryStatement.catchClauses().add(catchClause);
			SingleVariableDeclaration decl= getAST().newSingleVariableDeclaration();
			String varName= StubUtility.getExceptionVariableName(fCUnit.getJavaProject());
			
			String name= fScope.createName(varName, false);
			decl.setName(getAST().newSimpleName(name));
			decl.setType(ASTNodeFactory.newType(getAST(), type));
			catchClause.setException(decl);
			Statement st= getCatchBody(type, name, lineDelimiter);
			if (st != null) {
				catchClause.getBody().statements().add(st);
			}
		}
		List variableDeclarations= getSpecialVariableDeclarationStatements();
		ListRewrite statements= fRewriter.getListRewrite(tryStatement.getBody(), Block.STATEMENTS_PROPERTY);
		for (int i= 0; i < fSelectedNodes.length; i++) {
			ASTNode node= fSelectedNodes[i];
			if (node instanceof VariableDeclarationStatement && variableDeclarations.contains(node)) {
				VariableDeclarationStatement statement= (VariableDeclarationStatement)node;
				result.add(fRewriter.createCopyTarget(node));
				List fragments= statement.fragments();
				AST ast= getAST();
				for (Iterator iter= fragments.iterator(); iter.hasNext();) {
					VariableDeclarationFragment fragment= (VariableDeclarationFragment)iter.next();
					Expression initializer= fragment.getInitializer();
					if (initializer != null) {
						Assignment assignment= ast.newAssignment();
						assignment.setLeftHandSide((Expression)ASTNode.copySubtree(ast, fragment.getName()));
						assignment.setRightHandSide((Expression)fRewriter.createCopyTarget(initializer));
						statements.insertLast(ast.newExpressionStatement(assignment), null);
						fRewriter.remove(initializer, null);
					}
				}
			} else {
				statements.insertLast(fRewriter.createCopyTarget(node), null);
			}
		}
		result.add(tryStatement);
		return result;
	}
	
	private List getSpecialVariableDeclarationStatements() {
		List result= new ArrayList(3);
		VariableDeclaration[] locals= fAnalyzer.getAffectedLocals();
		for (int i= 0; i < locals.length; i++) {
			ASTNode parent= locals[i].getParent();
			if (parent instanceof VariableDeclarationStatement && !result.contains(parent))
				result.add(parent);
		}
		return result;
		
	}
	
	private Statement getCatchBody(String type, String name, String lineSeparator) throws CoreException {
		String s= StubUtility.getCatchBodyContent(fCUnit, type, name, lineSeparator);
		if (s == null) {
			return null;
		} else {
			return (Statement)fRewriter.createStringPlaceholder(s, ASTNode.RETURN_STATEMENT);
		}
	}
	
	private IFile getFile() {
		return (IFile) JavaModelUtil.toOriginal(fCUnit).getResource();
	}
}
