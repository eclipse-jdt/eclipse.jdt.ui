package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
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
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextBufferChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringAnalyzeUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

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

	public ExtractTempRefactoring(ICompilationUnit cu, int selectionStart, int selectionLength, CodeGenerationSettings settings) {
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
		Expression selected= getSelectedExpression();
		if (selected instanceof MethodInvocation){
			MethodInvocation mi= (MethodInvocation)selected;
			for (int i= 0; i < KNOWN_METHOD_NAME_PREFIXES.length; i++) {
				String proposal= tryTempNamePrefix(KNOWN_METHOD_NAME_PREFIXES[i], mi.getName().getIdentifier());
				if (proposal != null)
					return proposal;
			}
		}
		return fTempName;
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
			pm.beginTask("", 7); //$NON-NLS-1$
			if (fSelectionStart < 0)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractTempRefactoring.select_expression")); //$NON-NLS-1$
			pm.worked(1);
			
			RefactoringStatus result= Checks.validateModifiesFiles(ResourceUtil.getFiles(new ICompilationUnit[]{fCu}));
			if (result.hasFatalError())
				return result;
				
			if (! fCu.isStructureKnown())		
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractTempRefactoring.syntax_error")); //$NON-NLS-1$
		
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
			pm.beginTask("", 8); //$NON-NLS-1$
	
			Expression selectedExpression= getSelectedExpression();
			
			if (selectedExpression == null){
				String message= RefactoringCoreMessages.getString("ExtractTempRefactoring.select_expression");//$NON-NLS-1$
				return CodeRefactoringUtil.checkMethodSyntaxErrors(fSelectionStart, fSelectionLength, fCompilationUnitNode, fCu, message);
			}	
			pm.worked(1);
			
			if(selectionIncludesExtraneousText())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractTempRefactoring.extraneous_text_selected")); //$NON-NLS-1$

			pm.worked(1);
			
			if (isUsedInExplicitConstructorCall())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractTempRefactoring.explicit_constructor")); //$NON-NLS-1$
			pm.worked(1);				
			
			if (getSelectedMethodNode() == null)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractTempRefactoring.expression_in_method"));			 //$NON-NLS-1$
			pm.worked(1);				
			
			if (selectedExpression instanceof Name && selectedExpression.getParent() instanceof ClassInstanceCreation)
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

			return result;
		} finally{
			pm.done();
		}		
	}

	//XXX same as in ExtractConstant
	private static boolean isJustWhitespace(int start, int end, ICompilationUnit cu) throws JavaModelException {
		return 0 == cu.getBuffer().getText(start, end - start).trim().length();
	}
	
	private boolean selectionIncludesExtraneousText() throws JavaModelException {
		Expression selectedExpression= getSelectedExpression();
		if(!isJustWhitespace(fSelectionStart, selectedExpression.getStartPosition(), fCu))
			return true;
		if(!isJustWhitespace(selectedExpression.getStartPosition() + selectedExpression.getLength(), fSelectionStart + fSelectionLength, fCu))				
			return true;
		return false;
	}

	private RefactoringStatus checkExpressionBinding() throws JavaModelException{
		Expression selectedExpression= getSelectedExpression();
		switch(Checks.checkExpressionIsRValue(selectedExpression)) {
			case Checks.NOT_RVALUE_MISC:	return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractTempRefactoring.select_expression"));
			case Checks.NOT_RVALUE_VOID:	return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractTempRefactoring.no_void"));
			case Checks.IS_RVALUE:			return new RefactoringStatus();
			default:						Assert.isTrue(false); return null;
		}
	}
	
	private void initializeAST() throws JavaModelException {
		fCompilationUnitNode= AST.parseCompilationUnit(fCu, true);
	}

	private RefactoringStatus checkExpression() throws JavaModelException {
		Expression selectedExpression= getSelectedExpression();
			
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
		try{
			pm.beginTask(RefactoringCoreMessages.getString("ExtractTempRefactoring.checking_preconditions"), 1); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			
			TextChange change= new TextBufferChange(RefactoringCoreMessages.getString("RenameTempRefactoring.rename"), TextBuffer.create(fCu.getSource())); //$NON-NLS-1$
			change.addTextEdit("", getAllEdits());//$NON-NLS-1$
			String newCuSource= change.getPreviewContent();
			CompilationUnit newCUNode= AST.parseCompilationUnit(newCuSource.toCharArray(), fCu.getElementName(), fCu.getJavaProject());
			result.merge(RefactoringAnalyzeUtil.analyzeIntroducedCompileErrors(newCuSource, newCUNode, fCompilationUnitNode));
			return result;
		} catch (CoreException e){
			throw new JavaModelException(e);	
		} 	
	}
	
	public String getTempSignaturePreview() throws JavaModelException{
		return getTempTypeName() + " " + fTempName; //$NON-NLS-1$
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
			pm.beginTask(RefactoringCoreMessages.getString("ExtractTempRefactoring.preview"), 3);	 //$NON-NLS-1$
			TextChange change= new CompilationUnitChange(RefactoringCoreMessages.getString("ExtractTempRefactoring.extract_temp"), fCu); //$NON-NLS-1$
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
	
	private TextEdit[] getAllEdits() throws CoreException{
		Collection edits= new ArrayList(3);
		edits.add(createTempDeclarationEdit());
		TextEdit importEdit= createImportEditIfNeeded();
		if (importEdit != null)
			edits.add(importEdit);
		TextEdit[] replaceEdits= createReplaceExpressionWithTempEdits(); 	
		for (int i= 0; i < replaceEdits.length; i++) {
			edits.add(replaceEdits[i]);
		}
		return (TextEdit[]) edits.toArray(new TextEdit[edits.size()]);
	}

	private TextEdit createTempDeclarationEdit() throws CoreException {
		if (getSelectedExpression().getParent() instanceof ExpressionStatement)
			return replaceSelectedExpressionWithTempDeclaration();
		else	
			return createAndInsertTempDeclaration();
	}

	private TextEdit createAndInsertTempDeclaration() throws JavaModelException, CoreException {
		ASTNode insertBefore= getNodeToInsertTempDeclarationBefore();		
		int insertOffset= insertBefore.getStartPosition();
		String text= createTempDeclarationSource(getInitializerSource(), true) + getIndent(insertBefore);
		return SimpleTextEdit.createInsert(insertOffset, text); 
	}

	private TextEdit replaceSelectedExpressionWithTempDeclaration() throws CoreException {
		String nodeSource= fCu.getBuffer().getText(getSelectedExpression().getStartPosition(), getSelectedExpression().getLength());
		String text= createTempDeclarationSource(nodeSource, false);
		return SimpleTextEdit.createReplace(getSelectedExpression().getParent().getStartPosition(), getSelectedExpression().getParent().getLength(), text);
	}

	private TextEdit createImportEditIfNeeded() throws JavaModelException {
		ITypeBinding type= getSelectedExpression().resolveTypeBinding();
		if (type.isPrimitive())
			return null;
			
		ImportEdit importEdit= new ImportEdit(fCu, fSettings);
		importEdit.addImport(Bindings.getFullyQualifiedImportName(type));
		if (importEdit.isEmpty())
			return null;
		else	
			return importEdit;
	}

	private TextEdit[] createReplaceExpressionWithTempEdits() throws JavaModelException {
		ASTNode[] nodesToReplace= getNodesToReplace();
		TextEdit[] result= new TextEdit[nodesToReplace.length];
		for (int i= 0; i < nodesToReplace.length; i++) {
			ASTNode astNode= nodesToReplace[i];
			int offset= astNode.getStartPosition();
			int length= astNode.getLength();
			result[i]= SimpleTextEdit.createReplace(offset, length, fTempName);
		}
		return result;
	}

	private void addTempDeclaration(TextChange change) throws CoreException {
		change.addTextEdit(RefactoringCoreMessages.getString("ExtractTempRefactoring.declare_local_variable"), createTempDeclarationEdit()); //$NON-NLS-1$
	}

	private void addImportIfNeeded(TextChange change) throws CoreException {
		TextEdit importEdit= createImportEditIfNeeded();
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
		if ((! fReplaceAllOccurrences) || (getNodesToReplace().length == 1))
			return getInnermostStatementInBlock(getSelectedExpression());
		
		ASTNode[] firstReplaceNodeParents= getParents(getFirstReplacedExpression());
		ASTNode[] commonPath= findDeepestCommonSuperNodePathForReplacedNodes();
		Assert.isTrue(commonPath.length <= firstReplaceNodeParents.length);
		
		ASTNode deepestCommonParent= firstReplaceNodeParents[commonPath.length - 1];
		if (deepestCommonParent instanceof TryStatement)
			return deepestCommonParent;
		
		if (deepestCommonParent instanceof Block)
			return firstReplaceNodeParents[commonPath.length];

		if (deepestCommonParent instanceof IfStatement)
			return deepestCommonParent;

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
	
	//without the trailing indent
	private String createTempDeclarationSource(String initializerSource, boolean addTrailingLineDelimiter) throws CoreException {
		String modifier= fDeclareFinal ? "final ": ""; //$NON-NLS-1$ //$NON-NLS-2$
		ICodeFormatter formatter= ToolFactory.createCodeFormatter();
		String dummyInitializer= "0"; //$NON-NLS-1$
		String semicolon= ";"; //$NON-NLS-1$
		String dummyDeclaration= modifier + getTempTypeName() + " " + fTempName + " = " + dummyInitializer + semicolon; //$NON-NLS-1$ //$NON-NLS-2$
		int[] position= {dummyDeclaration.length() - dummyInitializer.length()  - semicolon.length()};
		StringBuffer formattedDummyDeclaration= new StringBuffer(formatter.format(dummyDeclaration, 0, position, getLineDelimiter()));
		String tail= addTrailingLineDelimiter ? getLineDelimiter(): "";
		return formattedDummyDeclaration.replace(position[0], position[0] + dummyInitializer.length(), initializerSource)
														.append(tail)
														.toString();
	}
	
	private String getTempTypeName() throws JavaModelException {
		Expression expression= getSelectedExpression();
		String name= expression.resolveTypeBinding().getName();
		if (! "".equals(name) || ! (expression instanceof ClassInstanceCreation)) //$NON-NLS-1$
			return name;
			
			
		ClassInstanceCreation cic= (ClassInstanceCreation)expression;
		Assert.isTrue(cic.getAnonymousClassDeclaration() != null);
		return getNameIdentifier(cic.getName());
	}
	
	//recursive
	private static String getNameIdentifier(Name name)  throws JavaModelException {
		if (name.isSimpleName())
			return ((SimpleName)name).getIdentifier();
		if (name.isQualifiedName()){
			QualifiedName qn= (QualifiedName)name;
			return getNameIdentifier(qn.getQualifier()) + "." + qn.getName().getIdentifier();  //$NON-NLS-1$
		}
		Assert.isTrue(false);
		return ""; //$NON-NLS-1$
	}
	
	private String getInitializerSource() throws JavaModelException {
		return removeTrailingSemicolons(fCu.getBuffer().getText(fSelectionStart, fSelectionLength));
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
		if (fReplaceAllOccurrences){
			ASTNode[] allMatches= AstMatchingNodeFinder.findMatchingNodes(getSelectedMethodNode(), getSelectedExpression());
			return retainOnlyReplacableMatches(allMatches);
		} else if (canReplace(getSelectedExpression()))
			return new ASTNode[]{getSelectedExpression()};	
		else	
			return new ASTNode[0];
	}

    private static ASTNode[] retainOnlyReplacableMatches(ASTNode[] allMatches) {
    	List result= new ArrayList(allMatches.length);
    	for (int i= 0; i < allMatches.length; i++) {
            if (canReplace(allMatches[i]))
            	result.add(allMatches[i]);
        }
        return (ASTNode[]) result.toArray(new ASTNode[result.size()]);
    }

    private static boolean canReplace(ASTNode node) {
    	if (node.getParent() instanceof VariableDeclarationFragment){
    		VariableDeclarationFragment vdf= (VariableDeclarationFragment)node.getParent();
    		if (node.equals(vdf.getName()))
    			return false;
    	}
    	if (node.getParent() instanceof ExpressionStatement)
    		return false;	
        return true;
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
		return ResourceUtil.getFile(fCu);
	}
}
