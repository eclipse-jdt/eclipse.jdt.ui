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
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import org.eclipse.jface.text.Region;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaRefactorings;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;

public class RenameTempRefactoring extends Refactoring implements INameUpdating, IReferenceUpdating {
	private static class ProblemNodeFinder {
	
		private ProblemNodeFinder() {
			//static
		}
		
		public static SimpleName[] getProblemNodes(ASTNode methodNode, TextEdit[] edits, TextChange change, String key) {
			NameNodeVisitor visitor= new NameNodeVisitor(edits, change, key);
			methodNode.accept(visitor);
			return visitor.getProblemNodes();
		}
		
		private static class NameNodeVisitor extends ASTVisitor {
	
			private Collection fRanges;
			private Collection fProblemNodes;
			private String fKey;
	
			public NameNodeVisitor(TextEdit[] edits, TextChange change, String key) {
				Assert.isNotNull(edits);
				Assert.isNotNull(key);
				fRanges= new HashSet(Arrays.asList(RefactoringAnalyzeUtil.getNewRanges(edits, change)));
				fProblemNodes= new ArrayList(0);
				fKey= key;
			}
	
			public SimpleName[] getProblemNodes() {
				return (SimpleName[]) fProblemNodes.toArray(new SimpleName[fProblemNodes.size()]);
			}
	
			private static VariableDeclaration getVariableDeclaration(Name node) {
				IBinding binding= node.resolveBinding();
				if (binding == null && node.getParent() instanceof VariableDeclaration)
					return (VariableDeclaration) node.getParent();
	
				if (binding != null && binding.getKind() == IBinding.VARIABLE) {
					CompilationUnit cu= (CompilationUnit) ASTNodes.getParent(node, CompilationUnit.class);
					return ASTNodes.findVariableDeclaration(((IVariableBinding) binding), cu);
				}
				return null;
			}
	
			//----- visit methods 
	
			public boolean visit(SimpleName node) {
				VariableDeclaration decl= getVariableDeclaration(node);
				if (decl == null)
					return super.visit(node);
				boolean keysEqual= fKey.equals(RefactoringAnalyzeUtil.getFullBindingKey(decl));
				boolean rangeInSet= fRanges.contains(new Region(node.getStartPosition(), node.getLength()));
	
				if (keysEqual && !rangeInSet)
					fProblemNodes.add(node);
	
				if (!keysEqual && rangeInSet)
					fProblemNodes.add(node);
	
				return super.visit(node);
			}
		}
	}
	
	private final int fSelectionStart;
	private final int fSelectionLength;
	private final ICompilationUnit fCu;
	
	//the following fields are set or modified after the construction
	private boolean fUpdateReferences;
	private String fCurrentName;
	private String fNewName;
	private CompilationUnit fCompilationUnitNode;
	private VariableDeclaration fTempDeclarationNode;
	private TextChange fChange;
	
	private RenameTempRefactoring(ICompilationUnit cu, int selectionStart, int selectionLength) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		Assert.isTrue(cu.exists());
		fUpdateReferences= true;
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fCu= cu;
		fNewName= "";//the only thing we can set //$NON-NLS-1$
	}

	public static boolean isAvailable(IJavaElement element) {
		return element != null && element.getElementType() == IJavaElement.LOCAL_VARIABLE;
	}

	public static RenameTempRefactoring create(ICompilationUnit cu, int selectionStart, int selectionLength) {
		return new RenameTempRefactoring(cu, selectionStart, selectionLength);
	}
	
	public Object getNewElement(){
		return null; //?????
	}
	
	/* non java-doc
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("RenameTempRefactoring.rename"); //$NON-NLS-1$
	}

	/*
	 * @see IReferenceUpdatingRefactoring#canEnableUpdateReferences()
	 */
	public boolean canEnableUpdateReferences() {
		return true;
	}

	/*
	 * @see IReferenceUpdatingRefactoring#getUpdateReferences()
	 */
	public boolean getUpdateReferences() {
		return fUpdateReferences;
	}

	/*
	 * @see IReferenceUpdatingRefactoring#setUpdateReferences()
	 */
	public void setUpdateReferences(boolean updateReferences) {
		fUpdateReferences= updateReferences;
	}
	
	/*
	 * @see IRenameRefactoring#setNewName
	 */
	public void setNewElementName(String newName) {
		Assert.isNotNull(newName);
		fNewName= newName;
	}

	/*
	 * @see IRenameRefactoring#getNewName()
	 */
	public String getNewElementName() {
		return fNewName;
	}

	/*
	 * @see IRenameRefactoring#getCurrentName()
	 */
	public String getCurrentElementName() {
		return fCurrentName;
	}

		//--- preconditions 
			
	/* non java-doc
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		initAST();
		if (fTempDeclarationNode == null || fTempDeclarationNode.resolveBinding() == null)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("RenameTempRefactoring.must_select_local")); //$NON-NLS-1$
		if (! Checks.isDeclaredIn(fTempDeclarationNode, MethodDeclaration.class) 
		 && ! Checks.isDeclaredIn(fTempDeclarationNode, Initializer.class))
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("RenameTempRefactoring.only_in_methods_and_initializers")); //$NON-NLS-1$
				
		initNames();			
		return new RefactoringStatus();
	}

	private void initAST(){
		fCompilationUnitNode= AST.parseCompilationUnit(fCu, true);
		fTempDeclarationNode= TempDeclarationFinder.findTempDeclaration(fCompilationUnitNode, fSelectionStart, fSelectionLength);
	}
	
	private void initNames(){
		fCurrentName= fTempDeclarationNode.getName().getIdentifier();
	}
	
	/*
	 * @see IRenameRefactoring#checkNewName()
	 */
	public RefactoringStatus checkNewElementName(String newName) throws JavaModelException {
		RefactoringStatus result= Checks.checkFieldName(newName);
		if (! Checks.startsWithLowerCase(newName))
			result.addWarning(RefactoringCoreMessages.getString("RenameTempRefactoring.lowercase")); //$NON-NLS-1$
		return result;		
	}
		
	/* non java-doc
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)	throws CoreException {
		try {
			pm.beginTask("", 1);	 //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();			
			result.merge(Checks.validateModifiesFiles(ResourceUtil.getFiles(new ICompilationUnit[]{fCu})));
			if (result.hasFatalError())
				return result;
			
			result.merge(checkNewElementName(fNewName));
			if (result.hasFatalError())
				return result;
			result.merge(analyzeAST());
			return result;
		} finally {
			pm.done();
		}	
	}
		
	private RefactoringStatus analyzeAST() throws CoreException{
		TextEdit declarationEdit= createRenameEdit(fTempDeclarationNode.getName().getStartPosition());
		TextEdit[] allRenameEdits= getAllRenameEdits(declarationEdit);
		fChange= new CompilationUnitChange(RefactoringCoreMessages.getString("RenameTempRefactoring.rename"), fCu); //$NON-NLS-1$
		MultiTextEdit rootEdit= new MultiTextEdit();
		fChange.setEdit(rootEdit);
		fChange.setKeepPreviewEdits(true);

		String changeName= RefactoringCoreMessages.getFormattedString("RenameTempRefactoring.changeName", new String[]{fCurrentName, fNewName}); //$NON-NLS-1$
		for (int i= 0; i < allRenameEdits.length; i++) {
			rootEdit.addChild(allRenameEdits[i]);
			fChange.addTextEditGroup(new TextEditGroup(changeName, allRenameEdits[i]));
		}
		String newCuSource= fChange.getPreviewContent();
		CompilationUnit newCUNode= AST.parseCompilationUnit(newCuSource.toCharArray(), fCu.getElementName(), fCu.getJavaProject());

		RefactoringStatus result= new RefactoringStatus();
		result.merge(analyzeCompileErrors(newCuSource, newCUNode));
		if (result.hasError())
			return result;
		
		String fullKey= RefactoringAnalyzeUtil.getFullBindingKey(fTempDeclarationNode);	
		ASTNode enclosing= getEnclosingBlockOrMethod(declarationEdit, fChange, newCUNode);
		SimpleName[] problemNodes= ProblemNodeFinder.getProblemNodes(enclosing, allRenameEdits, fChange, fullKey);
		result.merge(RefactoringAnalyzeUtil.reportProblemNodes(newCuSource, problemNodes));
		return result;
	}

	private TextEdit[] getAllRenameEdits(TextEdit declarationEdit) {
		if (! fUpdateReferences)
			return new TextEdit[] { declarationEdit };
		
		TempOccurrenceAnalyzer fTempAnalyzer= new TempOccurrenceAnalyzer(fTempDeclarationNode, true);
		fTempAnalyzer.perform();
		int[] referenceOffsets= fTempAnalyzer.getReferenceAndJavadocOffsets();

		TextEdit[] allRenameEdits= new TextEdit[referenceOffsets.length + 1];
		for (int i= 0; i < referenceOffsets.length; i++)
			allRenameEdits[i]= createRenameEdit(referenceOffsets[i]);
		allRenameEdits[referenceOffsets.length]= declarationEdit;
		return allRenameEdits;
	}

	private TextEdit createRenameEdit(int offset) {
		return new ReplaceEdit(offset, fCurrentName.length(), fNewName);
	}
	
	private ASTNode getEnclosingBlockOrMethod(TextEdit declarationEdit, TextChange change, CompilationUnit newCUNode) {
		ASTNode enclosing= RefactoringAnalyzeUtil.getBlock(declarationEdit, change, newCUNode);
		if (enclosing == null)	
			enclosing= RefactoringAnalyzeUtil.getMethodDeclaration(declarationEdit, change, newCUNode);
		return enclosing;
	}
	
    private RefactoringStatus analyzeCompileErrors(String newCuSource, CompilationUnit newCUNode) {
    	RefactoringStatus result= new RefactoringStatus();
    	IProblem[] newProblems= RefactoringAnalyzeUtil.getIntroducedCompileProblems(newCUNode, fCompilationUnitNode);
    	for (int i= 0; i < newProblems.length; i++) {
            IProblem problem= newProblems[i];
            if (problem.isError())
            	result.addEntry(JavaRefactorings.createStatusEntry(problem, newCuSource));
        }
        return result;
    }
    
	//--- changes 
	
	/* non java-doc
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		pm.done();
		return fChange;
	}
}
