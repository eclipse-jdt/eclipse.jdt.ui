package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.TryStatement;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextUtil;

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
	private Map fAlreadyUsedNameMap; //String -> ISourceRange
	private CompilationUnit fCompilationUnitNode;
	
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
		
		fReplaceAllOccurrences= true; //default
		fDeclareFinal= false; //default
		fTempName= "";
	}

	public String getName() {
		return "Extract Local Variable";
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
		try{
			pm.beginTask("", 7);
			if (fSelectionStart < 0)
				return RefactoringStatus.createFatalErrorStatus("An expression must be selected to activate this refactoring");
			pm.worked(1);
			
			RefactoringStatus result= Checks.validateModifiesFiles(ResourceUtil.getFiles(new ICompilationUnit[]{fCu}));
			if (result.hasFatalError())
				return result;
				
			if (! fCu.isStructureKnown())		
				return RefactoringStatus.createFatalErrorStatus("This file has syntax errors - please fix them first");
		
			initializeAST();
		
			return checkSelection(new SubProgressMonitor(pm, 5));
		} catch (CoreException e){	
			throw new JavaModelException(e);
		} finally{
			pm.done();
		}	
	}
	
	private RefactoringStatus checkSelection(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 8);
	
			if (getSelectedExpression() == null)
				return RefactoringStatus.createFatalErrorStatus("An expression must be selected to activate this refactoring.");

			pm.worked(1);
			
			if (isUsedInExplicitConstructorCall())
				return RefactoringStatus.createFatalErrorStatus("Code from explicit constructor calls cannot be extracted to a variable.");
			pm.worked(1);				
			
			if (getSelectedMethodNode() == null)
				return RefactoringStatus.createFatalErrorStatus("An expression used in a method must be selected to activate this refactoring.");			
			pm.worked(1);				
			
			if (getSelectedExpression().getParent() instanceof ExpressionStatement)
				return RefactoringStatus.createFatalErrorStatus("Cannot extract expressions used as statements.");
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

			initializeTempNames(); 
			pm.worked(1);
			return result;
		} finally{
			pm.done();
		}		
	}

	private RefactoringStatus checkExpressionBinding() throws JavaModelException{
		Expression expression= getSelectedExpression();
		ITypeBinding tb= expression.resolveTypeBinding();
		if (tb == null)
			return RefactoringStatus.createFatalErrorStatus("This expression cannot currenty be extracted.");
		
		if (tb.getName().equals("void"))
			return RefactoringStatus.createFatalErrorStatus("Cannot extract an expression of type 'void'.");
		
		return null;	
	}
	
	private void initializeTempNames() throws JavaModelException {
		fAlreadyUsedNameMap= TempNameUtil.getLocalNameMap(getSelectedMethodNode());
	}

	private void initializeAST() throws JavaModelException {
		fCompilationUnitNode= AST.parseCompilationUnit(fCu, true);
	}

	private RefactoringStatus checkExpression() throws JavaModelException {
		Expression selectedExpression= getSelectedExpression();
		if (selectedExpression instanceof NullLiteral) {
			return RefactoringStatus.createFatalErrorStatus("Cannot extract single null literals.");
		} else if (selectedExpression instanceof ArrayInitializer) {
			return RefactoringStatus.createFatalErrorStatus("Operation not applicable to an array initializer.");
		} else if (selectedExpression instanceof Assignment) {
			if (selectedExpression.getParent() instanceof Expression)
				return RefactoringStatus.createFatalErrorStatus("Cannot extract assignment that is part of another expression.");
			else
				return null;	
		} else if (selectedExpression instanceof ConditionalExpression) {
			return RefactoringStatus.createFatalErrorStatus("Currently no support to extract a single conditional expression.");
		} else
			return null;
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
	
	public String getTempSignaturePreview() throws JavaModelException{
		return getTempTypeName() + " " + fTempName;
	}
	
	private boolean isUsedInExplicitConstructorCall() throws JavaModelException{
		Expression selectedExpression= getSelectedExpression();
		if (ASTNodes.getParent(selectedExpression, ConstructorInvocation.class) != null)
			return true;
		if (ASTNodes.getParent(selectedExpression, SuperConstructorInvocation.class) != null)
			return true;
		return false;	
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
		ITypeBinding type= getSelectedExpression().resolveTypeBinding();
		if (type.isPrimitive())
			return;
			
		ImportEdit importEdit= new ImportEdit(fCu, fSettings);
		importEdit.addImport(Bindings.getFullyQualifiedImportName(type));
		if (!importEdit.isEmpty())
			change.addTextEdit("Update imports", importEdit);
	}
	
	private void addTempDeclaration(TextChange change) throws CoreException {
		ASTNode insertBefore= getNodeToInsertTempDeclarationBefore();		
		int insertOffset= insertBefore.getStartPosition();
		String text= createTempDeclarationSource() + getIndent(insertBefore);
		change.addTextEdit("Declare local variable", SimpleTextEdit.createInsert(insertOffset, text));
	}

	private ASTNode getNodeToInsertTempDeclarationBefore() throws JavaModelException {
		if ((! fReplaceAllOccurrences) || (getNodesToReplace().length == 1))
			return getInnermostStatementInBlock(getSelectedExpression());
		
		ASTNode[] firstReplaceNodeParents= getParents(getFirstReplacedExpression());
		ASTNode[] commonPath= findDeepestCommonSuperNodePathForReplacedNodes();
		Assert.isTrue(commonPath.length <= firstReplaceNodeParents.length);
		
		if (firstReplaceNodeParents[commonPath.length - 1] instanceof TryStatement)
			return firstReplaceNodeParents[commonPath.length - 1];
		
		if (firstReplaceNodeParents[commonPath.length - 1] instanceof Block)
			return firstReplaceNodeParents[commonPath.length];

		return getInnermostStatementInBlock(getFirstReplacedExpression());
	}
	
	private ASTNode[] findDeepestCommonSuperNodePathForReplacedNodes() throws JavaModelException {
		ASTNode[] matchingNodes= getNodesToReplace();
		ASTNode[][] matchingNodesParents= new ASTNode[matchingNodes.length][];
		for (int i= 0; i < matchingNodes.length; i++) {
			matchingNodesParents[i]= getParents(matchingNodes[i]);
		}
		List l=Arrays.asList(getLongestArrayPrefix(matchingNodesParents));
		return (ASTNode[]) l.toArray(new ASTNode[l.size()]);
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
	
	private Expression getFirstReplacedExpression() throws JavaModelException {
		if (! fReplaceAllOccurrences)
			return getSelectedExpression();
		ASTNode[] nodesToReplace= getNodesToReplace();
		Comparator comparator= new Comparator(){
			public int compare(Object o1, Object o2){
				return ((ASTNode)o1).getStartPosition() - ((ASTNode)o2).getStartPosition();
			}	
		};
		Arrays.sort(nodesToReplace, comparator);
		return (Expression)nodesToReplace[0];
	}
	
	private void addReplaceExpressionWithTemp(TextChange change) throws JavaModelException {
		ASTNode[] nodesToReplace= getNodesToReplace();
		for (int i= 0; i < nodesToReplace.length; i++) {
			ASTNode astNode= nodesToReplace[i];
			int offset= astNode.getStartPosition();
			int length= astNode.getLength();
			change.addTextEdit("Replace expression with a local variable reference", SimpleTextEdit.createReplace(offset, length, fTempName));
		}
	}

	//without the trailing indent
	private String createTempDeclarationSource() throws CoreException {
		String modifier= fDeclareFinal ? "final ": "";
		return modifier + getTempTypeName() + " " + fTempName + getAssignmentString() + getInitializerSource() + ";" + getLineDelimiter();
	}

	private String getTempTypeName() throws JavaModelException {
		Expression expression= getSelectedExpression();
		String name= expression.resolveTypeBinding().getName();
		if (! "".equals(name) || ! (expression instanceof ClassInstanceCreation))
			return name;
			
		ClassInstanceCreation cic= (ClassInstanceCreation)expression;
		if (cic.getAnonymousClassDeclaration() != null)
			return getNameIdentifier(cic.getName());
		else
			return ""; //fallback
	}
	
	//recursive
	private static String getNameIdentifier(Name name)  throws JavaModelException {
		if (name.isSimpleName())
			return ((SimpleName)name).getIdentifier();
		if (name.isQualifiedName()){
			QualifiedName qn= (QualifiedName)name;
			return getNameIdentifier(qn.getQualifier()) + "." + qn.getName().getIdentifier(); 
		}
		Assert.isTrue(false);
		return "";
	}
	
	private String getInitializerSource() throws JavaModelException {
		return removeTrailingSemicolons(fCu.getBuffer().getText(fSelectionStart, fSelectionLength));
	}
	
	//recursive
	private static String removeTrailingSemicolons(String s){
		String arg= s.trim();
		if (! arg.endsWith(";"))
			return arg;
		return removeTrailingSemicolons(arg.substring(0, arg.length() - 1));	
	}

	private String getIndent(ASTNode insertBefore) throws CoreException {
		TextBuffer buffer= null;
		try{
			buffer= TextBuffer.acquire(getFile());
			int startLine= buffer.getLineOfOffset(insertBefore.getStartPosition());
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
	
	private ASTNode[] getNodesToReplace() throws JavaModelException {
		if (fReplaceAllOccurrences)
			return  AstMatchingNodeFinder.findMatchingNodes(getSelectedMethodNode(), getSelectedExpression());
		else 
			return new ASTNode[]{getSelectedExpression()};	
	}
		
	private MethodDeclaration getSelectedMethodNode() throws JavaModelException {
		return (MethodDeclaration)ASTNodes.getParent(getSelectedExpression(), MethodDeclaration.class);
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
	
	private Expression getSelectedExpression() throws JavaModelException {
		SelectionAnalyzer analyzer= new SelectionAnalyzer(Selection.createFromStartLength(fSelectionStart, fSelectionLength), true);
		fCompilationUnitNode.accept(analyzer);
		ASTNode selectedNode= analyzer.getFirstSelectedNode();
		if (selectedNode instanceof Expression)
			return (Expression)selectedNode;
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
			return " = ";
	}

	private boolean isCompactingAssignment() {
		return fIsCompactingAssignments;
	}
	
	private int getTabSize() {
		return fTabSize;
	}	
}
