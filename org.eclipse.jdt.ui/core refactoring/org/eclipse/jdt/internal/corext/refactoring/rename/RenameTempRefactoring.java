/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Message;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusEntry.Context;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextBufferChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdatingRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;

public class RenameTempRefactoring extends Refactoring implements IRenameRefactoring, IReferenceUpdatingRefactoring{
	
	private final int fSelectionStart;
	private final int fSelectionLength;
	private final ICompilationUnit fCu;
	
	//the following fields are set or modified after the construction
	private boolean fUpdateReferences;
	private String fCurrentName;
	private String fNewName;
	private CompilationUnit fCompilationUnitNode;
	private VariableDeclaration fTempDeclarationNode;
	
	public RenameTempRefactoring(ICompilationUnit cu, int selectionStart, int selectionLength) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		Assert.isTrue(cu.exists());
		fUpdateReferences= true;
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fCu= cu;
		fNewName= "";//the only thing we can set
	}
	
	public Object getNewElement(){
		return null; //?????
	}
	
	/* non java-doc
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return "Rename local variable";
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
	public void setNewName(String newName) {
		Assert.isNotNull(newName);
		fNewName= newName;
	}

	/*
	 * @see IRenameRefactoring#getNewName()
	 */
	public String getNewName() {
		return fNewName;
	}

	/*
	 * @see IRenameRefactoring#getCurrentName()
	 */
	public String getCurrentName() {
		return fCurrentName;
	}

	//--- preconditions 
		
	/* non java-doc
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm)	throws JavaModelException {
		initAST();
		if (fTempDeclarationNode == null)
			return RefactoringStatus.createFatalErrorStatus("A local variable declaration or reference must be selected to activate this refactoring");
		if (fTempDeclarationNode.getParent() instanceof MethodDeclaration)
			return RefactoringStatus.createFatalErrorStatus("Currently. to rename a method parameter you should use 'Modify Parameters' refactoring");
			
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
	public RefactoringStatus checkNewName(String newName) throws JavaModelException {
		RefactoringStatus result= Checks.checkFieldName(newName);
		if (! Checks.startsWithLowerCase(newName))
			result.addWarning("By convention, all names of local variables start with lowercase letters.");
		return result;		
	}
		
	/* non java-doc
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm)	throws JavaModelException {
		try{
			pm.beginTask("", 1);	
			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkNewName(fNewName));
			if (result.hasFatalError())
				return result;
			result.merge(analyzeAST());
			return result;
		} finally{
			pm.done();
		}	
	}
		
	private RefactoringStatus analyzeAST() throws JavaModelException{
		try {
			RefactoringStatus result= new RefactoringStatus();
						
			TextEdit[] edits= getAllRenameEdits();
			TextChange change= new TextBufferChange("Rename Local Variable", TextBuffer.create(fCu.getSource()));
			change.setTrackPositionChanges(true);
		
			ICompilationUnit wc= getWorkingCopyWithNewContent(edits, change);
			CompilationUnit newCUNode= AST.parseCompilationUnit(wc, true);

			MethodDeclaration method= getDeclaringMethod(edits, change, newCUNode);			
			Message[] messages= ASTNodes.getMessages(method, ASTNodes.INCLUDE_ALL_PARENTS);
			for (int i= 0; i < messages.length; i++) {
				Context context= JavaSourceContext.create(wc, new SourceRange(messages[i].getSourcePosition(),0));
				result.addError(messages[i].getMessage(), context);
			}
			
			if (result.hasError())
				return result;
				
			ProblemNameNodeFinder nameVisitor= new ProblemNameNodeFinder(getRanges(edits, change), getFullDeclarationBindingKey(edits));
			method.accept(nameVisitor);
			result.merge(reportProblemNodes(wc, nameVisitor.getProblemNodes()));

			return result;
		} catch(CoreException e) {
			throw new JavaModelException(e);
		}
	}
	
	private RefactoringStatus reportProblemNodes(ICompilationUnit modifiedWorkingCopy, SimpleName[] problemNodes){
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < problemNodes.length; i++) {
			//FIX ME incorrect - needs backlinking see http://bugs.eclipse.org/bugs/show_bug.cgi?id=11646
			//see also http://bugs.eclipse.org/bugs/show_bug.cgi?id=12035
			Context context= JavaSourceContext.create(modifiedWorkingCopy, new SourceRange(0,0));
			result.addError("Name collision with name " + problemNodes[i].getIdentifier(), context);
		}
		return result;
	}

	private ICompilationUnit getWorkingCopyWithNewContent(TextEdit[] edits, TextChange change) throws JavaModelException {
		for (int i= 0; i < edits.length; i++) {
			change.addTextEdit("", edits[i]);
		}
		ICompilationUnit wc= getNewWorkingCopy(fCu);
		Assert.isTrue(! fCu.equals(wc));
		wc.getBuffer().setContents(change.getPreviewTextBuffer().getContent());
		return wc;
	}

	private static ICompilationUnit getNewWorkingCopy(ICompilationUnit cu) throws JavaModelException{
		return (ICompilationUnit)(getOriginal(cu).getWorkingCopy());
	}
	
	private static ICompilationUnit getOriginal(ICompilationUnit cu){
		if (! cu.isWorkingCopy())
			return cu;
		else
			return (ICompilationUnit)cu.getOriginalElement();	
	}
	
	private static MethodDeclaration getDeclaringMethod(TextEdit[] edits, TextChange change, CompilationUnit newCUNode) {
		ASTNode decl= getNameNode(change.getNewTextRange(findDeclarationEdit(edits)), newCUNode);
		return ((MethodDeclaration)ASTNodes.getParent(decl, MethodDeclaration.class));
	}
	
	private static TextEdit findDeclarationEdit(TextEdit[] edits){
		Arrays.sort(edits, new Comparator(){
			public int compare(Object o1, Object o2){
				return ((TextEdit)o1).getTextRange().getOffset() - ((TextEdit)o2).getTextRange().getOffset();
			}
		});
		return edits[0];
	}
	
	private static TextRange[] getRanges(TextEdit[] edits, TextChange change){
		TextRange[] result= new TextRange[edits.length];
		for (int i= 0; i < edits.length; i++) {
			if (change == null){
				result[i]= edits[i].getTextRange();
			} else{
				result[i]= change.getNewTextRange(edits[i]);
			}	
		}
		return result;
	}
		
	private static SimpleName getNameNode(TextRange range, CompilationUnit cuNode) {
		Selection sel= Selection.createFromStartLength(range.getOffset(), range.getLength());
		SelectionAnalyzer analyzer= new SelectionAnalyzer(sel, true);
		cuNode.accept(analyzer);
		return getSimpleName(analyzer.getFirstSelectedNode());
	}
	
	private static SimpleName getSimpleName(ASTNode node){
		if (node instanceof SimpleName)
			return (SimpleName)node;
		if (node instanceof VariableDeclaration)
			return ((VariableDeclaration)node).getName();
		return null;	
	}

	private String getFullDeclarationBindingKey(TextEdit[] edits) {
		Name declarationNameNode= getNameNode(findDeclarationEdit(edits).getTextRange(), fCompilationUnitNode);			
		return getFullBindingKey((VariableDeclaration)declarationNameNode.getParent());
	}		

	static String getFullBindingKey(VariableDeclaration decl){
		StringBuffer buff= new StringBuffer();
		buff.append(decl.resolveBinding().getVariableId());
		buff.append('/');
		
		AnonymousClassDeclaration acd= (AnonymousClassDeclaration)ASTNodes.getParent(decl, AnonymousClassDeclaration.class);
		if (acd != null && acd.resolveBinding() != null){
			if (acd.resolveBinding().getKey() != null)
				buff.append(acd.resolveBinding().getKey());
			else
				buff.append("AnonymousClassDeclaration");	
			buff.append('/');	
		}	
		
		TypeDeclaration td= (TypeDeclaration)ASTNodes.getParent(decl, TypeDeclaration.class);
		if (td != null && td.resolveBinding() != null){
			if (td.resolveBinding().getKey() != null)
				buff.append(td.resolveBinding().getKey());
			else
				buff.append("TypeDeclaration");	
			buff.append('/');	
		}
		
		MethodDeclaration md= (MethodDeclaration)ASTNodes.getParent(decl, MethodDeclaration.class);
		if (md != null && md.resolveBinding() != null){
			if (md.resolveBinding().getKey() != null)
				buff.append(md.resolveBinding().getKey());
			else
				buff.append("MethodDeclaration");	
		}
		return buff.toString();
	}

	private TextEdit[] getAllRenameEdits() throws JavaModelException {
		Integer[] renamingOffsets= getOccurrenceOffsets();
		Assert.isTrue(renamingOffsets.length > 0); //should be enforced by preconditions
		TextEdit[] result= new TextEdit[renamingOffsets.length];
		int length= fCurrentName.length();
		for(int i= 0; i < renamingOffsets.length; i++){
			int offset= renamingOffsets[i].intValue();
			result[i]= SimpleTextEdit.createReplace(offset, length, fNewName);
		}
		return result;
	}
	
	private Integer[] getOccurrenceOffsets() throws JavaModelException{
		return TempOccurrenceFinder.findTempOccurrenceOffsets(fCompilationUnitNode, fTempDeclarationNode, fUpdateReferences, true);
	}
	
	//--- changes 
	
	/* non java-doc
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 2);
			pm.worked(1);
			
			TextChange change= new CompilationUnitChange("Rename Local Variable", fCu);
			
			String changeName= "Rename local variable:'" + fCurrentName + "' to: '" + fNewName + "'.";
			TextEdit[] edits= getAllRenameEdits();
			for (int i= 0; i < edits.length; i++) {
				change.addTextEdit(changeName, edits[i]);
			}
			
			return change;
		} catch (CoreException e){
			throw new JavaModelException(e);
		} finally{
			pm.done();
		}	
	}

	///-------------------private static classes 
	
	private static class ProblemNameNodeFinder extends ASTVisitor{
		
		private Collection fRanges;
		private Collection fProblemNodes;
		private String fKey;
		
		ProblemNameNodeFinder(TextRange[] ranges, String key){
			Assert.isNotNull(ranges);
			Assert.isNotNull(key);
			fRanges= new HashSet(Arrays.asList(ranges));
			fProblemNodes= new ArrayList(0);
			fKey= key;
		}
		
		private SimpleName[] getProblemNodes(){
			return (SimpleName[]) fProblemNodes.toArray(new SimpleName[fProblemNodes.size()]);
		}
		
		private static VariableDeclaration getVariableDeclaration(Name node){
			IBinding binding= node.resolveBinding();
			if (binding == null && node.getParent() instanceof VariableDeclaration)
				return (VariableDeclaration)node.getParent();
			
			if (binding != null && binding.getKind() == IBinding.VARIABLE){
				CompilationUnit cu= (CompilationUnit)ASTNodes.getParent(node, CompilationUnit.class);
				return ASTNodes.findVariableDeclaration(((IVariableBinding)binding), cu);
			}	
			return null;
		}
		
		//----- visit methods 
		
		public boolean visit(SimpleName node) {
			VariableDeclaration decl= getVariableDeclaration(node);
			if (decl == null)
				return super.visit(node);
			boolean keysEqual= fKey.equals(RenameTempRefactoring.getFullBindingKey(decl));
			boolean rangeInSet= fRanges.contains(TextRange.createFromStartAndLength(node.getStartPosition(), node.getLength()));

			if (keysEqual && ! rangeInSet)
				fProblemNodes.add(node);
				
			if (! keysEqual && rangeInSet)	
				fProblemNodes.add(node);
				
			return super.visit(node);
		}
	}
}