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
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
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
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaRefactorings;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.ltk.core.refactoring.participants.ValidateEditChecker;

public class RenameLocalVariableProcessor extends JavaRenameProcessor implements INameUpdating, IReferenceUpdating {

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
	
	private final ILocalVariable fLocalVariable;
	private final ICompilationUnit fCu;
	
	//the following fields are set or modified after the construction
	private boolean fUpdateReferences;
	private String fCurrentName;
	private String fNewName;
	private CompilationUnit fCompilationUnitNode;
	private VariableDeclaration fTempDeclarationNode;
	private TextChange fChange;

	public static final String IDENTIFIER= "org.eclipse.jdt.ui.renameTypeParameterProcessor"; //$NON-NLS-1$
	
	public RenameLocalVariableProcessor(ILocalVariable localVariable) {
		Assert.isNotNull(localVariable);
		fLocalVariable= localVariable;
		fUpdateReferences= true;
		fCu= (ICompilationUnit) localVariable.getAncestor(IJavaElement.COMPILATION_UNIT);
		fNewName= ""; //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.rename.JavaRenameProcessor#needsSavedEditors()
	 */
	public boolean needsSavedEditors() {
		return false;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.rename.JavaRenameProcessor#loadDerivedParticipants(org.eclipse.ltk.core.refactoring.RefactoringStatus, java.util.List, java.lang.String[], org.eclipse.ltk.core.refactoring.participants.SharableParticipants)
	 */
	protected final void loadDerivedParticipants(final RefactoringStatus status, final List result, final String[] natures, final SharableParticipants shared) throws CoreException {
		// Do nothing
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.rename.JavaRenameProcessor#getAffectedProjectNatures()
	 */
	protected final String[] getAffectedProjectNatures() throws CoreException {
		return JavaProcessors.computeAffectedNatures(fLocalVariable);
	}
	
	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getElements()
	 */
	public Object[] getElements() {
		return new Object[] { fLocalVariable };
	}
	
	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getIdentifier()
	 */
	public String getIdentifier() {
		return IDENTIFIER;
	}
	
	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getProcessorName()
	 */
	public String getProcessorName() {
		return RefactoringCoreMessages.getString("RenameTempRefactoring.rename"); //$NON-NLS-1$
	}
	
	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#isApplicable()
	 */
	public boolean isApplicable() throws CoreException {
		return Checks.isAvailable(fLocalVariable);
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdating#canEnableUpdateReferences()
	 */
	public boolean canEnableUpdateReferences() {
		return true;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.rename.JavaRenameProcessor#getUpdateReferences()
	 */
	public boolean getUpdateReferences() {
		return fUpdateReferences;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdating#setUpdateReferences(boolean)
	 */
	public void setUpdateReferences(boolean updateReferences) {
		fUpdateReferences= updateReferences;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating#getCurrentElementName()
	 */
	public String getCurrentElementName() {
		return fCurrentName;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating#getNewElementName()
	 */
	public String getNewElementName() {
		return fNewName;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating#setNewElementName(java.lang.String)
	 */
	public void setNewElementName(String newName) {
		Assert.isNotNull(newName);
		fNewName= newName;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating#getNewElement()
	 */
	public Object getNewElement(){
		return null; //cannot create an ILocalVariable
	}
	
	
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

	private void initAST() throws JavaModelException {
		fCompilationUnitNode= new RefactoringASTParser(AST.JLS3).parse(fCu, true);
		ISourceRange sourceRange= fLocalVariable.getNameRange();
		ASTNode name= NodeFinder.perform(fCompilationUnitNode, sourceRange);
		if (name == null)
			return;
		if (name.getParent() instanceof VariableDeclaration)
			fTempDeclarationNode= (VariableDeclaration) name.getParent();
	}
	
	private void initNames(){
		fCurrentName= fTempDeclarationNode.getName().getIdentifier();
	}
	
	
	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor, org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws CoreException, OperationCanceledException {
		try {
			pm.beginTask("", 1);	 //$NON-NLS-1$

			ValidateEditChecker checker= (ValidateEditChecker) context.getChecker(ValidateEditChecker.class);
			checker.addFile(ResourceUtil.getFile(fCu));
			
			RefactoringStatus result= checkNewElementName(fNewName);
			if (result.hasFatalError())
				return result;
			result.merge(analyzeAST());
			return result;
		} finally {
			pm.done();
		}	
	}
		
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating#checkNewElementName(java.lang.String)
	 */
	public RefactoringStatus checkNewElementName(String newName) throws JavaModelException {
		RefactoringStatus result= Checks.checkFieldName(newName);
		if (! Checks.startsWithLowerCase(newName))
			result.addWarning(RefactoringCoreMessages.getString("RenameTempRefactoring.lowercase")); //$NON-NLS-1$
		return result;		
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
		String newCuSource= fChange.getPreviewContent(new NullProgressMonitor());
		ASTParser p= ASTParser.newParser(AST.JLS3);
		p.setSource(newCuSource.toCharArray());
		p.setUnitName(fCu.getElementName());
		p.setProject(fCu.getJavaProject());
		p.setCompilerOptions(RefactoringASTParser.getCompilerOptions(fCu));
		CompilationUnit newCUNode= (CompilationUnit) p.createAST(null);

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
    
	
	/* non java-doc
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		pm.done();
		return fChange;
	}

}
