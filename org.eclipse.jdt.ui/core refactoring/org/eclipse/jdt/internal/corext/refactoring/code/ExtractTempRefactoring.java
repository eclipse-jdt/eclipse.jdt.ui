package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import sun.security.action.GetPropertyAction;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.DebugUtils;
import org.eclipse.jdt.internal.corext.refactoring.ExtendedBuffer;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.util.AST;
import org.eclipse.jdt.internal.corext.refactoring.util.ASTUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.NewSelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.util.Selection;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextUtil;
import org.eclipse.jdt.internal.corext.util.Bindings;

public class ExtractTempRefactoring extends Refactoring {
	
	private final int fSelectionStart;
	private final int fSelectionLength;
	private final ICompilationUnit fCu;
	private final int fTabSize;
	private final boolean fIsCompactingAssignments;
	private final CodeGenerationSettings fSettings;
			
	private boolean fReplaceAllOccurrences;
	private boolean fDeclareFinal;
	private String fTempName;
	private Map fAlreadyUsedNameMap;
	private AST fAST;
	
	public ExtractTempRefactoring(ICompilationUnit cu, int selectionStart, int selectionLength, CodeGenerationSettings settings, int tabSize, boolean compactAssignments) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		Assert.isTrue(cu.exists());
		Assert.isTrue(tabSize >= 0);
		Assert.isNotNull(settings);	
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fCu= cu;
		fSettings= settings;
		fTabSize= tabSize;
		fIsCompactingAssignments= compactAssignments;
		fAlreadyUsedNameMap= new HashMap(0);
		
		fReplaceAllOccurrences= false;
		fDeclareFinal= false;
	}

	public String getName() {
		return "Extract Temp";
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

	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		if (fSelectionStart < 0)
			return RefactoringStatus.createFatalErrorStatus("An expression must be selected to activate this refactoring");
		
		if (!fCu.isStructureKnown())
			return RefactoringStatus.createFatalErrorStatus("Syntax errors");
			
		if (!(fCu instanceof CompilationUnit))
			return RefactoringStatus.createFatalErrorStatus("Internal Error");
	
		initializeAST();
		
		return checkSelection();
	}
	
	private RefactoringStatus checkSelection() throws JavaModelException {
		if (fAST.hasProblems()){
			RefactoringStatus compileErrors= Checks.checkCompileErrors(fAST, fCu);
			if (compileErrors.hasFatalError())
				return compileErrors;
		}
		
		if (analyzeSelection().getSelectedNodes() == null || analyzeSelection().getSelectedNodes().length != 1)
			return RefactoringStatus.createFatalErrorStatus("An expression must be selected to activate this refactoring.");
			
		if (getSelectedExpression() == null)
			return RefactoringStatus.createFatalErrorStatus("An expression must be selected to activate this refactoring.");
		
		if (analyzeSelection().getExpressionTypeBinding() == null)
			return RefactoringStatus.createFatalErrorStatus("An expression must be selected to activate this refactoring.");
		
		initializeTempNames(); 
		return new RefactoringStatus();
	}

	private void initializeTempNames() throws JavaModelException {
		fAlreadyUsedNameMap= TempNameUtil.getLocalNameMap(getSelectedMethodNode().scope);
	}

	private void initializeAST() throws JavaModelException {
		fAST= new AST(fCu);
	}
	
	public RefactoringStatus checkTempName(String newName) {
		RefactoringStatus result= Checks.checkFieldName(newName);
		if (! Checks.startsWithLowerCase(newName))
			result.addWarning("By convention, all names of local variables start with lowercase letters.");
		if (fAlreadyUsedNameMap.containsKey(newName))
			result.addError("Name '" + newName + "' is already used.", JavaSourceContext.create(fCu, (ISourceRange)fAlreadyUsedNameMap.get(newName)));
		return result;		
	}
	
	public void setTempName(String newName) {
		fTempName= newName;
	}
	
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("Checking preconditions", 1);
			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkTempName(fTempName));
			return result;
		} finally{
			pm.done();
		}	
	}

	public IChange createChange(IProgressMonitor pm) throws JavaModelException {		
		try{
			pm.beginTask("Preparing preview", 3);	
			TextChange change= new CompilationUnitChange("Extract Temp", fCu);
			addTempDeclaration(change);
			pm.worked(1);
			addImportIfNeeded(change);
			pm.worked(1);
			addReplaceExpressionWithTemp(change);
			pm.worked(1);
			return change;
		} catch (CoreException e){
			throw new JavaModelException(e);	
		} finally{
			pm.done();
		}	
	}

	private void addImportIfNeeded(TextChange change) throws CoreException {
		TypeBinding type= analyzeSelection().getExpressionTypeBinding();
		if (type.isBaseType())
			return;
			
		ImportEdit importEdit= new ImportEdit(fCu, fSettings);
		importEdit.addImport(Bindings.makeFullyQualifiedName(type.qualifiedPackageName(), type.qualifiedSourceName()));
		if (!importEdit.isEmpty())
			change.addTextEdit("add import", importEdit);
	}
	
	private void addTempDeclaration(TextChange change) throws CoreException {
		AstNode insertBefore= getNodeToInsertTempDeclarationBefore();		
		int insertOffset= ASTUtil.getSourceStart(insertBefore);
		String text= createTempDeclarationSource() + getIndent(insertBefore);
		change.addTextEdit("Declare local variable", SimpleTextEdit.createInsert(insertOffset, text));
	}

	private AstNode getNodeToInsertTempDeclarationBefore() throws JavaModelException {
		if ((! fReplaceAllOccurrences) || (getNodesToReplace().length == 1))
			return getInnermostStatement(getSelectedExpression());
		
		AstNode[] firstReplaceNodeParents= findParents(getFirstReplacedExpression());
		AstNode[] commonPath= findDeepestCommonSuperNodePathForReplacedNodes();
		if (isBlock(firstReplaceNodeParents[commonPath.length -1]))
			return firstReplaceNodeParents[commonPath.length];
		else	
			return firstReplaceNodeParents[commonPath.length - 1];
	}
	
	private static boolean isBlock(AstNode node){
		if (node instanceof Block)
			return true;
		if (node instanceof AbstractMethodDeclaration)
			return true;
		return false;	
	}
	
	private AstNode[] findDeepestCommonSuperNodePathForReplacedNodes() throws JavaModelException {
		AstNode[] matchingNodes= getNodesToReplace();
		AstNode[][] matchingNodesParents= new AstNode[matchingNodes.length][];
		for (int i= 0; i < matchingNodes.length; i++) {
			matchingNodesParents[i]= findParents(matchingNodes[i]);
		}
		List l=Arrays.asList(getLongestArrayPrefix(matchingNodesParents));
		return (AstNode[]) l.toArray(new AstNode[l.size()]);
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
	
	private AstNode[] findParents(AstNode astNode)  throws JavaModelException {
		int start= ASTUtil.getSourceStart(astNode);
		int end= ASTUtil.getSourceEnd(astNode);
		int length= end - start +1;
		NewSelectionAnalyzer selAnalyzer= new NewSelectionAnalyzer(new ExtendedBuffer(fCu.getBuffer()), Selection.createFromStartLength(start, length));
		fAST.accept(selAnalyzer);
		return selAnalyzer.getParents();
	}
	
	private Expression getFirstReplacedExpression() throws JavaModelException {
		if (! fReplaceAllOccurrences)
			return getSelectedExpression();
		AstNode[] nodesToReplace= getNodesToReplace();
		Comparator comparator= new Comparator(){
			public int compare(Object o1, Object o2){
				return ASTUtil.getSourceStart((AstNode)o1) - ASTUtil.getSourceStart((AstNode)o2);
			}	
		};
		Arrays.sort(nodesToReplace, comparator);
		return (Expression)nodesToReplace[0];
	}
	
	private void addReplaceExpressionWithTemp(TextChange change) throws JavaModelException {
		AstNode[] nodesToReplace= getNodesToReplace();
		for (int i= 0; i < nodesToReplace.length; i++) {
			AstNode astNode= nodesToReplace[i];
			int offset= ASTUtil.getSourceStart(astNode);
			int end= ASTUtil.getSourceEnd(astNode);
			int length= end - offset + 1;
			change.addTextEdit("Replace expression with a local variable reference", SimpleTextEdit.createReplace(offset, length, fTempName));
		}
	}

	//without the trailing indent
	private String createTempDeclarationSource() throws CoreException {
		TypeBinding tb= analyzeSelection().getExpressionTypeBinding();
		String modifier= fDeclareFinal ? "final ": "";
		String typeName= new String(tb.sourceName());
		String initializer= fCu.getSource().substring(fSelectionStart, fSelectionStart + fSelectionLength);
		String text= modifier + typeName + " " + fTempName + getAssignmentString() + initializer+ ";" + getLineDelimiter();
		return text;
	}

	private String getIndent(AstNode insertBefore) throws CoreException {
		TextBuffer buffer= null;
		try{
			buffer= TextBuffer.acquire(getFile());
			int startLine= buffer.getLineOfOffset(ASTUtil.getSourceStart(insertBefore));
			return TextUtil.createIndentString(buffer.getLineIndent(startLine, getTabSize()));	
		} finally {
			if (buffer != null)
				TextBuffer.release(buffer);
		}
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
	
	private Statement getInnermostStatement(Expression expression) throws JavaModelException {
		Selection selection= Selection.createFromStartEnd(ASTUtil.getSourceStart(expression), ASTUtil.getSourceEnd(expression));
		NewSelectionAnalyzer selAnalyzer= new NewSelectionAnalyzer(new ExtendedBuffer(fCu.getBuffer()), selection);
		fAST.accept(selAnalyzer);
		AstNode[] parents= selAnalyzer.getParents();
		if (parents.length < 2)
			return null;
		for (int i= parents.length - 2 ; i >= 0 ; i--) {
			if (isBlock(parents[i]))
				return (Statement)parents[i + 1];
		}	
		return null;		
	}	
	
	private AstNode[] getNodesToReplace() throws JavaModelException {
		Expression expression= getSelectedExpression();
	
		if (fReplaceAllOccurrences)
			return  AstMatchingNodeFinder.findMatchingNodes(getSelectedMethodNode(), expression);
		else 
			return new AstNode[]{expression};	
	}
	
	private AbstractMethodDeclaration getSelectedMethodNode() throws JavaModelException {
		NewSelectionAnalyzer selAnalyzer= analyzeSelection();
		
		AstNode[] parents= selAnalyzer.getParents();
		for (int i= parents.length -1 ; i >= 0 ; i--) {
			AstNode astNode= parents[i];
			if (astNode instanceof AbstractMethodDeclaration)
				return (AbstractMethodDeclaration)astNode;
		}
		return null;
	}

	private NewSelectionAnalyzer analyzeSelection() throws JavaModelException {
		NewSelectionAnalyzer selAnalyzer= new NewSelectionAnalyzer(new ExtendedBuffer(fCu.getBuffer()), Selection.createFromStartLength(fSelectionStart, fSelectionLength));
		fAST.accept(selAnalyzer);
		return selAnalyzer;
	}
	
	private Expression getSelectedExpression() throws JavaModelException {
		NewSelectionAnalyzer selAnalyzer= analyzeSelection();
		AstNode[] selected= selAnalyzer.getSelectedNodes();
		if (selected[0] instanceof Expression)
			return (Expression)selected[0];
		else 
			return null;	
	}	
	
	private IFile getFile() throws JavaModelException {
		if (fCu.isWorkingCopy())
			return (IFile)fCu.getOriginalElement().getCorrespondingResource();
		else
			return (IFile)fCu.getCorrespondingResource();
	}

	private String getAssignmentString() {
		if (isCompactingAssignment())
			return "= ";
		else
			return "=";
	}

	private boolean isCompactingAssignment() {
		return fIsCompactingAssignments;
	}
	
	private int getTabSize() {
		return fTabSize;
	}	
}
