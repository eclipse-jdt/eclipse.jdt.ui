/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.surround;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.ASTNodeCodeBlock;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeBlock;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeBlockEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.CompositeCodeBlock;
import org.eclipse.jdt.internal.corext.codemanipulation.DeleteNodeEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.TryCatchBlock;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.CodeScopeBuilder;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

public class SurroundWithTryCatchRefactoring extends Refactoring {

	private Selection fSelection;
	private CodeGenerationSettings fSettings;
	private ISurroundWithTryCatchQuery fQuery;
	private SurroundWithTryCatchAnalyzer fAnalyzer;
	private boolean fSaveChanges;

	private ICompilationUnit fCUnit;
	private AST fTargetAST;

	public SurroundWithTryCatchRefactoring(ICompilationUnit cu, ITextSelection selection, CodeGenerationSettings settings, ISurroundWithTryCatchQuery query) {
		fCUnit= cu;
		fSelection= Selection.createFromStartLength(selection.getOffset(), selection.getLength());
		fSettings= settings;
		fQuery= query;
	}
	
	public SurroundWithTryCatchRefactoring(ICompilationUnit cu, int offset, int length, CodeGenerationSettings settings, ISurroundWithTryCatchQuery query) {
		fCUnit= cu;
		fSelection= Selection.createFromStartLength(offset, length);
		fSettings= settings;
		fQuery= query;
	}

	public void setSaveChanges(boolean saveChanges) {
		fSaveChanges= saveChanges;
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

	public RefactoringStatus checkActivationBasics(CompilationUnit rootNode, IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
			
		fAnalyzer= new SurroundWithTryCatchAnalyzer(fCUnit, fSelection, fQuery);
		rootNode.accept(fAnalyzer);
		result.merge(fAnalyzer.getStatus());
		return result;
	}


	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		result.merge(Checks.validateModifiesFiles(ResourceUtil.getFiles(new ICompilationUnit[]{fCUnit})));
		if (result.hasFatalError())
			return result;
		
		CompilationUnit rootNode= AST.parseCompilationUnit(fCUnit, true);
		return checkActivationBasics(rootNode, pm);
	}

	/*
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		return new RefactoringStatus();
	}

	/* non Java-doc
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		TextBuffer buffer= null;
		try {
			fTargetAST= new AST();
			CompilationUnitChange result= new CompilationUnitChange(getName(), fCUnit);
			result.setSave(fSaveChanges);
			// We already have a text buffer since we start a surround with from the editor. So it is fast
			// to acquire it here.
			buffer= TextBuffer.acquire(getFile());
			addEdits(result, buffer);
			return result;
		} catch (JavaModelException e) {
			throw e;
		} catch (CoreException e) {
			throw new JavaModelException(e);
		} finally {
			if (buffer != null)
				TextBuffer.release(buffer);
			fTargetAST= null;
		}
	}
	
	private void addEdits(TextChange change, TextBuffer buffer) throws CoreException {
		final String NN= ""; //$NON-NLS-1$
		final ITypeBinding[] exceptions= fAnalyzer.getExceptions();
		final VariableDeclaration[] locals= fAnalyzer.getAffectedLocals();
		final int tabWidth= CodeFormatterUtil.getTabWidth();
		
		addImportEdit(change, exceptions);
		
		final int selectionStart= fSelection.getOffset();
		TextBuffer.Block block= buffer.getBlockContent(selectionStart, fSelection.getLength(), tabWidth);
		TextBufferEditor editor= new TextBufferEditor(TextBuffer.create(block.content));
		int delta= selectionStart + block.offsetDelta;
		
		List handleDeclarationStatements= new ArrayList(3);
		ASTNodeCodeBlock newLocals= new ASTNodeCodeBlock();
		for (int i= 0; i < locals.length; i++) {
			VariableDeclaration local= locals[i];
			if (local instanceof SingleVariableDeclaration) {
				editor.add(handleLocal((SingleVariableDeclaration)local, newLocals, delta));
			} else if (local.getParent() instanceof VariableDeclarationStatement) {
				VariableDeclarationStatement ds= (VariableDeclarationStatement)local.getParent();
				if (!handleDeclarationStatements.contains(ds)) {
					editor.add(handleLocal(ds, buffer, newLocals, delta));
					handleDeclarationStatements.add(ds);
				}
			} else {
				Assert.isTrue(false, "Operation doesn't work for expressions. So should never happen"); //$NON-NLS-1$
			}
		}
		CodeScopeBuilder.Scope root= CodeScopeBuilder.perform(fAnalyzer.getEnclosingBodyDeclaration(), fSelection);
		CodeScopeBuilder.Scope scope= root.findScope(fSelection.getOffset(), fSelection.getLength());
		scope.setCursor(fSelection.getOffset());
		editor.performEdits(null);
		CompositeCodeBlock codeBlock= new CompositeCodeBlock();
		codeBlock.add(newLocals);
		codeBlock.add(new TryCatchBlock(exceptions, fCUnit.getJavaProject(), scope, new CodeBlock(editor.getTextBuffer())));
		change.addTextEdit(NN, CodeBlockEdit.createReplace(selectionStart, fSelection.getLength(), codeBlock));
	}
	
	private void addImportEdit(TextChange change, ITypeBinding[] exceptions) throws JavaModelException {
		ImportEdit edit= new ImportEdit(fCUnit, fSettings);
		for (int i= 0; i < exceptions.length; i++) {
			edit.addImport(Bindings.getFullyQualifiedImportName(exceptions[i]));
		}
		if (!edit.isEmpty())
			change.addTextEdit("", edit); //$NON-NLS-1$
	}
	
	private TextEdit handleLocal(SingleVariableDeclaration node, ASTNodeCodeBlock newLocals, int delta) {
		SingleVariableDeclaration copy= (SingleVariableDeclaration)ASTNode.copySubtree(fTargetAST, node);
		copy.setInitializer(null);
		newLocals.add(copy);
		
		int start= node.getStartPosition();
		if (node.getInitializer() != null) {
			int end= node.getName().getStartPosition();
			return SimpleTextEdit.createDelete(start - delta, end - start);
		} else {
			int length= node.getLength();
			return new DeleteNodeEdit(start - delta, length, true, ASTNodes.getDelimiterToken(node));
		}
	}

	private TextEdit handleLocal(VariableDeclarationStatement node, TextBuffer buffer, ASTNodeCodeBlock newLocals, int delta) throws CoreException {
		List fragments= node.fragments();
		createNewDeclaration(node, newLocals);
		if (allFragmentsUninitialized(fragments)) {
			return new DeleteNodeEdit(node.getStartPosition() - delta, node.getLength(), true, ASTNodes.getDelimiterToken(node));
		} else {
			int size= fragments.size();
			ASTNodeCodeBlock block= new ASTNodeCodeBlock();
			for (int i= 0; i < size; i++) {
				VariableDeclarationFragment fragment= (VariableDeclarationFragment)fragments.get(i);
				if (fragment.getInitializer() != null) {
					Assignment ass= fTargetAST.newAssignment();
					ass.setLeftHandSide((Expression)ASTNode.copySubtree(fTargetAST, fragment.getName()));
					ass.setOperator(Assignment.Operator.ASSIGN);
					ass.setRightHandSide((Expression)ASTNode.copySubtree(fTargetAST, fragment.getInitializer()));
					block.add(fTargetAST.newExpressionStatement(ass));
				}
			}
			return CodeBlockEdit.createReplace(node.getStartPosition() - delta, node.getLength(), block);
		}
	}
	
	private void createNewDeclaration(VariableDeclarationStatement node, ASTNodeCodeBlock newLocals) {
		VariableDeclarationStatement copy= (VariableDeclarationStatement)ASTNode.copySubtree(fTargetAST, node);
		for (Iterator iter= copy.fragments().iterator(); iter.hasNext();) {
			((VariableDeclarationFragment)iter.next()).setInitializer(null);
		}
		newLocals.add(copy);
	}
	
	private boolean allFragmentsUninitialized(List fragments) {
		for (Iterator iter= fragments.iterator(); iter.hasNext();) {
			VariableDeclarationFragment fragment= (VariableDeclarationFragment) iter.next();
			if (fragment.getInitializer() != null)
				return false;
		}
		return true;
	}
	
	private IFile getFile() throws JavaModelException {
		if (fCUnit.isWorkingCopy())
			return (IFile)fCUnit.getOriginalElement().getResource();
		else
			return (IFile)fCUnit.getResource();
	}
}
