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
import java.util.Collection;
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
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jface.text.Document;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.Corext;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.fragments.ASTFragmentFactory;
import org.eclipse.jdt.internal.corext.dom.fragments.IASTFragment;
import org.eclipse.jdt.internal.corext.dom.fragments.IExpressionFragment;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaRefactorings;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringAnalyzeUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;

public class ExtractConstantRefactoring extends Refactoring {

	private static final char UNDERSCORE= '_';
	public static final String PUBLIC= 	JdtFlags.VISIBILITY_STRING_PUBLIC;
	public static final String PROTECTED= JdtFlags.VISIBILITY_STRING_PROTECTED;
	public static final String PACKAGE= 	JdtFlags.VISIBILITY_STRING_PACKAGE;
	public static final String PRIVATE= 	JdtFlags.VISIBILITY_STRING_PRIVATE;

	private static final String MODIFIER= "static final"; //$NON-NLS-1$
	private static final String[] KNOWN_METHOD_NAME_PREFIXES= {"get", "is"}; //$NON-NLS-2$ //$NON-NLS-1$

	private final int fSelectionStart;
	private final int fSelectionLength;
	private final ICompilationUnit fCu;

	private IExpressionFragment fSelectedExpression;
	private boolean fReplaceAllOccurrences= true; //default value
	private boolean fQualifyReferencesWithDeclaringClassName= false;	//default value

	private String fAccessModifier= PRIVATE; //default value
	private String fConstantName= ""; //$NON-NLS-1$;
	private CompilationUnit fCompilationUnitNode;

	private boolean fSelectionAllStaticFinal;
	private boolean fAllStaticFinalCheckPerformed= false;
	
	private List fBodyDeclarations;
	
	//Constant Declaration Location
	private BodyDeclaration fToInsertAfter;
	private boolean fInsertFirst;

	private ExtractConstantRefactoring(ICompilationUnit cu, int selectionStart, int selectionLength, CodeGenerationSettings settings) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		Assert.isTrue(cu.exists());
		Assert.isNotNull(settings);
		
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		
		fCu= cu;
	}
	
	public static boolean isAvailable(ASTNode[] selectedNodes, ASTNode coveringNode) {
		return Checks.isExtractableExpression(selectedNodes, coveringNode);
	}

	public static ExtractConstantRefactoring create(ICompilationUnit cu, int selectionStart, int selectionLength, CodeGenerationSettings settings) {
		return new ExtractConstantRefactoring(cu, selectionStart, selectionLength, settings);
	}
	
	public String getName() {
		return RefactoringCoreMessages.getString("ExtractConstantRefactoring.name"); //$NON-NLS-1$
	}

	public boolean replaceAllOccurrences() {
		return fReplaceAllOccurrences;
	}

	public void setReplaceAllOccurrences(boolean replaceAllOccurrences) {
		fReplaceAllOccurrences= replaceAllOccurrences;
	}
	
	public void setAccessModifier(String am) {
		Assert.isTrue(
			am == PRIVATE || am == PROTECTED || am == PACKAGE || am == PUBLIC
		);
		fAccessModifier= am;
	}
	
	public String getAccessModifier() {
		return fAccessModifier;	
	}

	public boolean qualifyReferencesWithDeclaringClassName() {
		return fQualifyReferencesWithDeclaringClassName;
	}
	
	public void setQualifyReferencesWithDeclaringClassName(boolean qualify) {
		fQualifyReferencesWithDeclaringClassName= qualify;
	}
	
	//XXX similar to code in ExtractTemp
	public String guessConstantName() throws JavaModelException {
		IExpressionFragment selectedFragment= getSelectedExpression();
		if (selectedFragment == null)
			return fConstantName;
		Expression selected= selectedFragment.getAssociatedExpression();
		if (selected instanceof MethodInvocation){
			String candidate= guessContantName((MethodInvocation) selected);
			if (candidate != null)
				return candidate;
		} else if (selected instanceof StringLiteral) {
			String candidate= guessContantName((StringLiteral) selected);
			if (candidate != null)
				return candidate;
		}
		return fConstantName;
	}
	
	private static String guessContantName(StringLiteral literal) {
		return convertNonLettersToUnderscores(literal.getLiteralValue()).toUpperCase();
	}

	private static String convertNonLettersToUnderscores(String string) {
		if (string.length() == 0)
			return ""; //$NON-NLS-1$
		
		StringBuffer result= new StringBuffer(string.length());
		if (Character.isJavaIdentifierStart(string.charAt(0)))
			result.append(string.charAt(0));
		else
			result.append(UNDERSCORE);
		
		for (int i= 1; i < string.length(); i++) {
			if (Character.isJavaIdentifierPart(string.charAt(i)))
				result.append(string.charAt(i));
			else
				result.append(UNDERSCORE);
		}
		return result.toString();
	}

	private static String guessContantName(MethodInvocation selectedNode) {
		for (int i= 0; i < KNOWN_METHOD_NAME_PREFIXES.length; i++) {
			String proposal= tryTempNamePrefix(KNOWN_METHOD_NAME_PREFIXES[i], selectedNode.getName().getIdentifier());
			if (proposal != null)
				return proposal;
		}
		return null;	
	}
	
	private static String tryTempNamePrefix(String prefix, String methodName){
		if (isPrefixOk(prefix, methodName))
			return splitByUpperCaseChars(methodName.substring(prefix.length())).toUpperCase();
		else	
			return null;
	}

	//XXX similar to code in ExtractTemp
	private static boolean isPrefixOk(String prefix, String methodName){
		if (! methodName.startsWith(prefix))
			return false;
		else if (methodName.length() <= prefix.length())
			return false;
		else if (! Character.isUpperCase(methodName.charAt(prefix.length())))
			return false;
		else 
			return true;
	}
	
    private static String splitByUpperCaseChars(String string) {
    	StringBuffer buff= new StringBuffer(string.length());
    	for (int i= 0, n= string.length(); i < n; i++) {
			char c= string.charAt(i);
			if (i != 0 && Character.isUpperCase(c))
				buff.append(UNDERSCORE);
			buff.append(c);	
        }
    	return buff.toString();
    }

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask("", 8); //$NON-NLS-1$
	
			RefactoringStatus result= Checks.validateModifiesFiles(ResourceUtil.getFiles(new ICompilationUnit[] { fCu }));
			if (result.hasFatalError())
				return result;
			pm.worked(1);
	
			if (!fCu.isStructureKnown())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractConstantRefactoring.syntax_error")); //$NON-NLS-1$
			pm.worked(1);
	
			initializeAST(new SubProgressMonitor(pm, 1));
	
			result.merge(checkSelection(new SubProgressMonitor(pm, 5)));
			if ((! result.hasFatalError()) && isLiteralNodeSelected())
				fReplaceAllOccurrences= false;
			return result;
		} finally {
			pm.done();
		}
	}
	
	public boolean selectionAllStaticFinal() {
		Assert.isTrue(fAllStaticFinalCheckPerformed);
		return fSelectionAllStaticFinal;
	}

	private void checkAllStaticFinal() 
		throws JavaModelException
	{
		fSelectionAllStaticFinal= ConstantChecks.isStaticFinalConstant(getSelectedExpression());
		fAllStaticFinalCheckPerformed= true;
	}

	private String getModifier() {
		return getAccessModifier() + " " + MODIFIER;	 //$NON-NLS-1$
	}
	
	private RefactoringStatus checkSelection(IProgressMonitor pm) throws JavaModelException {
		try {
			pm.beginTask("", 2); //$NON-NLS-1$
			
			IExpressionFragment selectedExpression= getSelectedExpression();
			
			if (selectedExpression == null) {
				String message= RefactoringCoreMessages.getString("ExtractConstantRefactoring.select_expression"); //$NON-NLS-1$
				return CodeRefactoringUtil.checkMethodSyntaxErrors(fSelectionStart, fSelectionLength, fCompilationUnitNode, message);
			}
			pm.worked(1);
			
			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkExpression());
			if (result.hasFatalError())
				return result;
			pm.worked(1);
			
			return result;
		} finally {
			pm.done();
		}
	}

	private RefactoringStatus checkExpressionBinding() throws JavaModelException {
		return checkExpressionFragmentIsRValue();
	}
	
	private RefactoringStatus checkExpressionFragmentIsRValue() throws JavaModelException {
		/* Moved this functionality to Checks, to allow sharing with
		   ExtractTempRefactoring, others */
		switch(Checks.checkExpressionIsRValue(getSelectedExpression().getAssociatedExpression())) {
			case Checks.NOT_RVALUE_MISC:
				return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.getString("ExtractConstantRefactoring.select_expression"), null, Corext.getPluginId(), RefactoringStatusCodes.EXPRESSION_NOT_RVALUE, null); //$NON-NLS-1$
			case Checks.NOT_RVALUE_VOID:
				return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.getString("ExtractConstantRefactoring.no_void"), null, Corext.getPluginId(), RefactoringStatusCodes.EXPRESSION_NOT_RVALUE_VOID, null); //$NON-NLS-1$
			case Checks.IS_RVALUE:
				return new RefactoringStatus();
			default:
				Assert.isTrue(false); return null;
		}		
	}

	//	 !!! -- same as in ExtractTempRefactoring
	private boolean isLiteralNodeSelected() throws JavaModelException {
		IExpressionFragment fragment= getSelectedExpression();
		if (fragment == null)
			return false;
		Expression expression= fragment.getAssociatedExpression();
		if (expression == null)
			return false;
		switch (expression.getNodeType()) {
			case ASTNode.BOOLEAN_LITERAL :
			case ASTNode.CHARACTER_LITERAL :
			case ASTNode.NULL_LITERAL :
			case ASTNode.NUMBER_LITERAL :
				return true;
			
			default :
				return false;
		}
	}

	private void initializeAST(IProgressMonitor pm) {
		fCompilationUnitNode= new RefactoringASTParser(AST.JLS2).parse(fCu, true, pm);
	}

	private RefactoringStatus checkExpression() throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkExpressionBinding());
		if(result.hasFatalError())
			return result;
		checkAllStaticFinal();

		IExpressionFragment selectedExpression= getSelectedExpression();
		if (selectedExpression.getAssociatedExpression() instanceof NullLiteral)
			result.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractConstantRefactoring.null_literals"))); //$NON-NLS-1$
		else if (!ConstantChecks.isLoadTimeConstant(selectedExpression))
			result.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractConstantRefactoring.not_load_time_constant"))); //$NON-NLS-1$
		
		return result;
	}

	public void setConstantName(String newName) {
		Assert.isNotNull(newName);
		fConstantName= newName;
	}

	public String getConstantName() {
		return fConstantName;
	}

	/**
	 * This method performs checks on the constant name which are
	 * quick enough to be performed every time the ui input component
	 * contents are changed.
	 */
	public RefactoringStatus checkConstantNameOnChange() 
		throws JavaModelException
	{
		if(fieldExistsInThisType())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getFormattedString("ExtractConstantRefactoring.field_exists", getConstantName())); //$NON-NLS-1$
		return Checks.checkConstantName(getConstantName());
	}
	
	private boolean fieldExistsInThisType() 
		throws JavaModelException
	{
		return getContainingType().getField(getConstantName()).exists();
	}

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		pm.beginTask(RefactoringCoreMessages.getString("ExtractConstantRefactoring.checking_preconditions"), 1); //$NON-NLS-1$
		
		/* Note: some checks are performed on change of input widget
		 * values. (e.g. see ExtractConstantRefactoring.checkConstantNameOnChange())
		 */ 
		
		//TODO: possibly add more checking for name conflicts that might
		//      lead to a change in behaviour
		
		RefactoringStatus result= checkCompilation();
		
		pm.done();
		return result;
	}
	
	private RefactoringStatus checkCompilation() throws JavaModelException {
		TextBuffer buffer= null;
		try{
			RefactoringStatus result= new RefactoringStatus();
			
			buffer= TextBuffer.acquire((IFile)WorkingCopyUtil.getOriginal(fCu).getResource());
			TextEdit[] edits= getAllEdits(buffer);
			TextChange change= new DocumentChange(RefactoringCoreMessages.getString("ExtractConstantRefactoring.rename"), new Document(fCu.getSource())); //$NON-NLS-1$
			TextChangeCompatibility.addTextEdit(change, "", edits); //$NON-NLS-1$

			String newCuSource= change.getPreviewContent();
			ASTParser p= ASTParser.newParser(AST.JLS2);
			p.setSource(newCuSource.toCharArray());
			p.setUnitName(fCu.getElementName());
			p.setProject(fCu.getJavaProject());
			CompilationUnit newCUNode= (CompilationUnit) p.createAST(null);
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

	// !! similar to ExtractTempRefactoring equivalent
	public String getConstantSignaturePreview() throws JavaModelException {
		return getModifier() + " " + getConstantTypeName() + " " + fConstantName; //$NON-NLS-2$//$NON-NLS-1$
	}

	public Change createChange(IProgressMonitor pm) throws CoreException {
		TextBuffer buffer= null;
		try {
			buffer= TextBuffer.acquire((IFile)WorkingCopyUtil.getOriginal(fCu).getResource());
			pm.beginTask(RefactoringCoreMessages.getString("ExtractConstantRefactoring.preview"), 3); //$NON-NLS-1$
			TextChange result= new CompilationUnitChange(RefactoringCoreMessages.getString("ExtractConstantRefactoring.extract_constant"), fCu); //$NON-NLS-1$
			addConstantDeclaration(result);
			pm.worked(1);
			addImportIfNeeded(result, buffer);
			pm.worked(1);
			addReplaceExpressionWithConstant(result);
			pm.worked(1);

			return result;
		} finally {
			if (buffer != null)
				TextBuffer.release(buffer);
			pm.done();
		}
	}

	private TextEdit[] getAllEdits(TextBuffer buffer) throws CoreException {
		Collection edits= new ArrayList(3);
		edits.add(createConstantDeclarationEdit());
		TextEdit importEdit= createImportEditIfNeeded(buffer);
		if (importEdit != null)
			edits.add(importEdit);
		TextEdit[] replaceEdits= createReplaceExpressionWithConstantEdits();
		for (int i= 0; i < replaceEdits.length; i++) {
			edits.add(replaceEdits[i]);
		}
		return (TextEdit[]) edits.toArray(new TextEdit[edits.size()]);
	}

	// !!! analogue in ExtractTempRefactoring
	private TextEdit createConstantDeclarationEdit() throws CoreException {
		if(insertFirst())
			return createInsertDeclarationFirstEdit();
		else
			return createInsertDeclarationAfterEdit(getNodeToInsertConstantDeclarationAfter());
	}
	
	private TextEdit createInsertDeclarationFirstEdit() throws JavaModelException, CoreException {
		BodyDeclaration first = (BodyDeclaration) getBodyDeclarations().next();
		int insertOffset = first.getStartPosition();
		String text= createConstantDeclarationSource() + getLineDelimiter() + getIndent(first);
		return new InsertEdit(insertOffset, text);		
	}
	
	private TextEdit createInsertDeclarationAfterEdit(BodyDeclaration toInsertAfter) throws JavaModelException, CoreException {
		Assert.isNotNull(toInsertAfter);

		if(isOtherDeclOrClassEndOnSameLine(toInsertAfter))
			return createInsertDeclarationOnSameLineEdit(toInsertAfter);
		else
			return createInsertDeclarationOnNextLineEdit(toInsertAfter);			
	}
		
	private boolean isOtherDeclOrClassEndOnSameLine(BodyDeclaration toInsertAfter) throws JavaModelException {
		int indexToInsertAtOrAfter= toInsertAfter.getStartPosition() + toInsertAfter.getLength();
		
		BodyDeclaration nextBodyDecl= getNextBodyDeclaration(toInsertAfter);
		if(nextBodyDecl != null) {
			if(isOnSameLine(indexToInsertAtOrAfter, nextBodyDecl.getStartPosition()))
				return true;
		} else {
			TypeDeclaration typeDecl= getContainingTypeDeclarationNode();
			int typeDeclClose= typeDecl.getStartPosition() + typeDecl.getLength() - 1;
			if(isOnSameLine(indexToInsertAtOrAfter, typeDeclClose))
				return true;
		}
		return false;
	}
	
	private boolean isOnSameLine(int firstOffset, int secondOffset) {
		return lineNumber(firstOffset) == lineNumber(secondOffset);
	}
	
	private int lineNumber(int offset) {
		return fCompilationUnitNode.lineNumber(offset);	
	}
	
	private BodyDeclaration getNextBodyDeclaration(BodyDeclaration bodyDeclaration) throws JavaModelException {
		Assert.isNotNull(bodyDeclaration);
		
		for(Iterator it= getBodyDeclarations(); it.hasNext();)
			if(bodyDeclaration.equals(it.next()))
				if(it.hasNext())
					return (BodyDeclaration) it.next();	
		return null;	
	}
	
	private TextEdit createInsertDeclarationOnNextLineEdit(BodyDeclaration toInsertAfter) throws JavaModelException, CoreException {
		Assert.isNotNull(toInsertAfter);
			
		int insertOffset= getStartOfFollowingLine(toInsertAfter);
		String text= getIndent(toInsertAfter) + createConstantDeclarationSource() + getLineDelimiter();
		return new InsertEdit(insertOffset, text);		
	}
	
	private int getStartOfFollowingLine(BodyDeclaration declaration) {
		CompilationUnit cu= fCompilationUnitNode;
		
		int declEnd= getNodeInclusiveEnd(declaration);
		int declLine= cu.lineNumber(declEnd);
		
		int i= declEnd;
		while(cu.lineNumber(i) == declLine)
			i++;
			
		return i;
	}
	
	private int getNodeInclusiveEnd(ASTNode node) {
		return getNodeExclusiveEnd(node) - 1;
	}
	
	private int getNodeExclusiveEnd(ASTNode node) {
		return node.getStartPosition() + node.getLength();	
	}
	
	private TextEdit createInsertDeclarationOnSameLineEdit(BodyDeclaration toInsertAfter) throws JavaModelException, CoreException {
		Assert.isNotNull(toInsertAfter);

		int insertOffset= getNodeExclusiveEnd(toInsertAfter);
		String text= " " + createConstantDeclarationSource(); //$NON-NLS-1$
		return new InsertEdit(insertOffset, text);
	}
		
	// !! almost identical to version in ExtractTempRefactoring
	private TextEdit createImportEditIfNeeded(TextBuffer buffer) throws CoreException {
		ITypeBinding type= getSelectedExpression().getAssociatedExpression().resolveTypeBinding();
		if (type.isPrimitive())
			return null;
		if (type.isArray() && type.getElementType().isPrimitive())
			return null;

		ImportRewrite rewrite= new ImportRewrite(fCu);
		rewrite.addImport(type);
		if (rewrite.isEmpty())
			return null;
		else
			return rewrite.createEdit(buffer.getDocument());
	}

	// !!! very similar to (same as) equivalent in ExtractTempRefactoring
	private TextEdit[] createReplaceExpressionWithConstantEdits() throws JavaModelException {
		IASTFragment[] fragmentsToReplace= getFragmentsToReplace();
		TextEdit[] result= new TextEdit[fragmentsToReplace.length];
		for (int i= 0; i < fragmentsToReplace.length; i++) {
			IASTFragment fragment= fragmentsToReplace[i];
			int offset= fragment.getStartPosition();
			int length= fragment.getLength();
			result[i]= createReplaceEdit(offset, length);
		}
		return result;
	}
	
	private TextEdit createReplaceEdit(int offset, int length) throws JavaModelException {
		String constantReference= getConstantNameForReference();
		return new ReplaceEdit(offset, length, constantReference);	
	}
	
	private String getConstantNameForReference() throws JavaModelException {
		return getReferenceQualifier() + fConstantName;
	}
	
	private String getReferenceQualifier() throws JavaModelException {
		if(qualifyReferencesWithDeclaringClassName())
			return getContainingTypeBinding().getName() + "."; //$NON-NLS-1$
		else
			return ""; //$NON-NLS-1$
	}

	// !!!
	private void addConstantDeclaration(TextChange change) throws CoreException {
		TextChangeCompatibility.addTextEdit(change, RefactoringCoreMessages.getString("ExtractConstantRefactoring.declare_constant"), createConstantDeclarationEdit()); //$NON-NLS-1$
	}

	// !!! very similar to equivalent in ExtractTempRefactoring
	private void addImportIfNeeded(TextChange change, TextBuffer buffer) throws CoreException {
		TextEdit importEdit= createImportEditIfNeeded(buffer);
		if (importEdit != null)
			TextChangeCompatibility.addTextEdit(change, RefactoringCoreMessages.getString("ExtractConstantRefactoring.update_imports"), importEdit); //$NON-NLS-1$
	}

	// !!! very similar to equivalent in ExtractTempRefactoring
	private void addReplaceExpressionWithConstant(TextChange change) throws JavaModelException {
		TextEdit[] edits= createReplaceExpressionWithConstantEdits();
		for (int i= 0; i < edits.length; i++) {
			TextChangeCompatibility.addTextEdit(change, RefactoringCoreMessages.getString("ExtractConstantRefactoring.replace"), edits[i]); //$NON-NLS-1$
		}
	}

	private void computeConstantDeclarationLocation() 
		throws JavaModelException
	{
		if(isDeclarationLocationComputed())
			return;

		BodyDeclaration lastStaticDependency= null;
		Iterator decls= getBodyDeclarations();
		
		Assert.isTrue(decls.hasNext()); /* Admissable selected expressions must occur
		                                   within a body declaration.  Thus, the 
		                                   class/interface in which such an expression occurs
		                                   must have at least one body declaration */
		
		while (decls.hasNext()) {
			BodyDeclaration decl= (BodyDeclaration) decls.next();
			
			int modifiers;
			if (decl instanceof FieldDeclaration)
				modifiers= ((FieldDeclaration) decl).getModifiers();
			else if (decl instanceof Initializer)
				modifiers= ((Initializer) decl).getModifiers();
			else {
				continue; /* this declaration is not a field declaration
				              or initializer, so the placement of the constant
				              declaration relative to it does not matter */
			}
			
			if (Modifier.isStatic(modifiers) && depends(getSelectedExpression(), decl))
				lastStaticDependency= decl;
		}
		
		if(lastStaticDependency == null)
			fInsertFirst= true;
		else
			fToInsertAfter= lastStaticDependency;
	}
	
	/** bd is a static field declaration or static initializer */
	private static boolean depends(IExpressionFragment selected, BodyDeclaration bd) {
		/* We currently consider selected to depend on bd only if db includes a declaration
		 * of a static field on which selected depends.
		 * 
		 * A more accurate strategy might be to also check if bd contains (or is) a
		 * static initializer containing code which changes the value of a static field on 
		 * which selected depends.  However, if a static is written to multiple times within
		 * during class initialization, it is difficult to predict which value should be used.
		 * This would depend on which value is used by expressions instances for which the new 
		 * constant will be substituted, and there may be many of these; in each, the
		 * static field in question may have taken on a different value (if some of these uses
		 * occur within static initializers).
		 */
		
		if(bd instanceof FieldDeclaration) {
			FieldDeclaration fieldDecl = (FieldDeclaration) bd;
			for(Iterator fragments = fieldDecl.fragments().iterator(); fragments.hasNext();) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragments.next();
				SimpleName staticFieldName = fragment.getName();
				if(selected.getSubFragmentsMatching(ASTFragmentFactory.createFragmentForFullSubtree(staticFieldName)).length != 0)
					return true;
			}
		}
		return false;
	}

	private boolean isDeclarationLocationComputed() {
		return fInsertFirst == true || fToInsertAfter != null;	
	}
	
	private boolean insertFirst() 
		throws JavaModelException
	{
		if(!isDeclarationLocationComputed())
			computeConstantDeclarationLocation();
		return fInsertFirst;
	}
	
	private BodyDeclaration getNodeToInsertConstantDeclarationAfter() 
		throws JavaModelException
	{
		if(!isDeclarationLocationComputed())
			computeConstantDeclarationLocation();
		return fToInsertAfter;
	}
	
	private Iterator getBodyDeclarations() 
		throws JavaModelException
	{
		if(fBodyDeclarations == null)
			fBodyDeclarations= getContainingTypeDeclarationNode().bodyDeclarations();
		return fBodyDeclarations.iterator();
	}

	private String createConstantDeclarationSource() throws JavaModelException, CoreException {
		return createConstantDeclarationSource(getInitializerSource());		
	}

	// !!! similar to one in ExtractTempRefactoring
	//*without the trailing indent*
	private String createConstantDeclarationSource(String initializerSource) throws CoreException {
		String dummyInitializer= "0"; //$NON-NLS-1$
		String semicolon= ";"; //$NON-NLS-1$
		String dummyDeclaration= getModifier() + " " + getConstantTypeName() + " " + fConstantName + " = " + dummyInitializer + semicolon; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		int[] position= { dummyDeclaration.length() - dummyInitializer.length() - semicolon.length()};
		String formattedDeclaration= CodeFormatterUtil.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS, dummyDeclaration, 0, position, getLineDelimiter(), fCu.getJavaProject());
		StringBuffer formattedDummyDeclaration= new StringBuffer(formattedDeclaration);
		return formattedDummyDeclaration.replace(position[0], position[0] + dummyInitializer.length(), initializerSource).toString();
	}

	// !!! Almost duplicates getTempTypeName() in ExtractTempRefactoring
	private String getConstantTypeName() throws JavaModelException {
		IExpressionFragment selection= getSelectedExpression();
		Expression expression= selection.getAssociatedExpression();
		String name= expression.resolveTypeBinding().getName();
		if (!"".equals(name) || !(expression instanceof ClassInstanceCreation)) //$NON-NLS-1$
			return name;

		ClassInstanceCreation cic= (ClassInstanceCreation) expression;
		Assert.isNotNull(cic.getAnonymousClassDeclaration());
		
		return ASTNodes.asString(cic.getName());
	}

	private String getInitializerSource() throws JavaModelException {
		IExpressionFragment fragment= getSelectedExpression();
		return removeTrailingSemicolons(fCu.getBuffer().getText(fragment.getStartPosition(), fragment.getLength()));
	}

	// !!! same as method in ExtractTempRefactoring
	//recursive
	private static String removeTrailingSemicolons(String s) {
		String arg= s.trim();
		if (!arg.endsWith(";")) //$NON-NLS-1$
			return arg;
		return removeTrailingSemicolons(arg.substring(0, arg.length() - 1));
	}

	// !!! same as method in ExtractTempRefactoring
	private String getIndent(ASTNode insertAfter) throws CoreException {
		TextBuffer buffer= null;
		try {
			buffer= TextBuffer.acquire(getFile());
			int startLine= buffer.getLineOfOffset(insertAfter.getStartPosition());
			return CodeFormatterUtil.createIndentString(buffer.getLineIndent(startLine, CodeFormatterUtil.getTabWidth()));
		} finally {
			if (buffer != null)
				TextBuffer.release(buffer);
		}
	}

	private String getLineDelimiter() throws CoreException {
		TextBuffer buffer= null;
		try {
			buffer= TextBuffer.acquire(getFile());
			return buffer.getLineDelimiter(buffer.getLineOfOffset(fSelectionStart));
		} finally {
			if (buffer != null)
				TextBuffer.release(buffer);
		}
	}

	private static boolean isStaticFieldOrStaticInitializer(BodyDeclaration node) {
		if(node instanceof MethodDeclaration || node instanceof TypeDeclaration)
			return false;
		
		int modifiers;
		if(node instanceof FieldDeclaration) {
			modifiers = ((FieldDeclaration) node).getModifiers();
		} else if(node instanceof Initializer) {
			modifiers = ((Initializer) node).getModifiers();
		} else {
			Assert.isTrue(false);
			return false;
		}
		
		if(!Modifier.isStatic(modifiers))
			return false;
		
		return true;
	}
	/**
	 *   Elements returned by next() are BodyDeclaration
	 *   instances.
	 */
	private Iterator getReplacementScope() throws JavaModelException {
		boolean declPredecessorReached= false;
		
		Collection scope= new ArrayList();
		for(Iterator bodyDeclarations = getBodyDeclarations(); bodyDeclarations.hasNext();) {
		    BodyDeclaration bodyDeclaration= (BodyDeclaration) bodyDeclarations.next();
		    
		    if(bodyDeclaration == getNodeToInsertConstantDeclarationAfter())
		    	declPredecessorReached= true;
		    
		    if(insertFirst() || declPredecessorReached || !isStaticFieldOrStaticInitializer(bodyDeclaration))
		    	scope.add(bodyDeclaration);
		}
		return scope.iterator();
	}

	private IASTFragment[] getFragmentsToReplace() throws JavaModelException {
		List toReplace = new ArrayList();
		if (fReplaceAllOccurrences) {
			Iterator replacementScope = getReplacementScope();
			while(replacementScope.hasNext()) {
				BodyDeclaration bodyDecl = (BodyDeclaration) replacementScope.next();
				IASTFragment[] allMatches= ASTFragmentFactory.createFragmentForFullSubtree(bodyDecl).getSubFragmentsMatching(getSelectedExpression());
				IASTFragment[] replaceableMatches = retainOnlyReplacableMatches(allMatches);
				for(int i = 0; i < replaceableMatches.length; i++)
					toReplace.add(replaceableMatches[i]);
			}
		} else if (canReplace(getSelectedExpression()))
			toReplace.add(getSelectedExpression());
		return (IASTFragment[]) toReplace.toArray(new IASTFragment[toReplace.size()]);
	}

	// !! - like one in ExtractTempRefactoring
	private static IASTFragment[] retainOnlyReplacableMatches(IASTFragment[] allMatches) {
		List result= new ArrayList(allMatches.length);
		for (int i= 0; i < allMatches.length; i++) {
			if (canReplace(allMatches[i]))
				result.add(allMatches[i]);
		}
		return (IASTFragment[]) result.toArray(new IASTFragment[result.size()]);
	}

	// !! - like one in ExtractTempRefactoring
	private static boolean canReplace(IASTFragment fragment) {
		ASTNode node= fragment.getAssociatedNode();
		if (node.getParent() instanceof VariableDeclarationFragment) {
			VariableDeclarationFragment vdf= (VariableDeclarationFragment) node.getParent();
			if (node.equals(vdf.getName()))
				return false;
		}
		if (node.getParent() instanceof ExpressionStatement)
			return false;
		return true;
	}

	private IExpressionFragment getSelectedExpression() throws JavaModelException {
		if(fSelectedExpression != null)
			return fSelectedExpression;
		
		IASTFragment selectedFragment= ASTFragmentFactory.createFragmentForSourceRange(new SourceRange(fSelectionStart, fSelectionLength), fCompilationUnitNode, fCu);
		
		if (selectedFragment instanceof IExpressionFragment)
			return fSelectedExpression= (IExpressionFragment) selectedFragment;
		else
			return null;
	}

	//returns non-null
	private TypeDeclaration getContainingTypeDeclarationNode() throws JavaModelException {
		TypeDeclaration result= (TypeDeclaration) ASTNodes.getParent(getSelectedExpression().getAssociatedNode(), TypeDeclaration.class);  
		Assert.isNotNull(result);
		return result;
	}

	private ITypeBinding getContainingTypeBinding() throws JavaModelException {
		ITypeBinding result= getContainingTypeDeclarationNode().resolveBinding();
		Assert.isNotNull(result);
		return result;
	}

	private IType getContainingType() throws JavaModelException {
		IType type= Bindings.findType(getContainingTypeBinding(), fCu.getJavaProject());
		Assert.isNotNull(type);

		return type;
	}

	// !!! - from ExtractTempRefactoring
	private IFile getFile() {
		return ResourceUtil.getFile(fCu);
	}
}
