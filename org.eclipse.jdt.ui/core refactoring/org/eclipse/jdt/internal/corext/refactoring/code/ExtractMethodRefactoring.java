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
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.ui.CodeGeneration;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTFlattener;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.BodyDeclarationRewrite;
import org.eclipse.jdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.StatementRewrite;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;

/**
 * Extracts a method in a compilation unit based on a text selection range.
 */
public class ExtractMethodRefactoring extends Refactoring {

	private ICompilationUnit fCUnit;
	private ImportRewrite fImportRewriter;
	private int fSelectionStart;
	private int fSelectionLength;
	private AST fAST;
	private ASTRewrite fRewriter;
	private ExtractMethodAnalyzer fAnalyzer;
	private int fVisibility;
	private String fMethodName;
	private boolean fThrowRuntimeExceptions;
	private List fParameterInfos;
	private Set fUsedNames;
	private boolean fGenerateJavadoc;
	private boolean fReplaceDuplicates;
	private SnippetFinder.Match[] fDuplicates;
	// either of type TypeDeclaration or AnonymousClassDeclaration
	private ASTNode fDestination;
	// either of type TypeDeclaration or AnonymousClassDeclaration
	private ASTNode[] fDestinations;

	private static final String EMPTY= ""; //$NON-NLS-1$

	private static class UsedNamesCollector extends ASTVisitor {
		private Set result= new HashSet();
		private Set fIgnore= new HashSet();
		public static Set perform(ASTNode[] nodes) {
			UsedNamesCollector collector= new UsedNamesCollector();
			for (int i= 0; i < nodes.length; i++) {
				nodes[i].accept(collector);
			}
			return collector.result;
		}
		public boolean visit(FieldAccess node) {
			Expression exp= node.getExpression();
			if (exp != null)
				fIgnore.add(node.getName());
			return true;
		}
		public void endVisit(FieldAccess node) {
			fIgnore.remove(node.getName());
		}
		public boolean visit(MethodInvocation node) {
			Expression exp= node.getExpression();
			if (exp != null)
				fIgnore.add(node.getName());
			return true;
		}
		public void endVisit(MethodInvocation node) {
			fIgnore.remove(node.getName());
		}
		public boolean visit(QualifiedName node) {
			fIgnore.add(node.getName());
			return true;
		}
		public void endVisit(QualifiedName node) {
			fIgnore.remove(node.getName());
		}
		public boolean visit(SimpleName node) {
			if (!fIgnore.contains(node))
				result.add(node.getIdentifier());
			return true;
		}
		public boolean visit(TypeDeclaration node) {
			return visitType(node);
		}
		public boolean visit(AnnotationTypeDeclaration node) {
			return visitType(node);
		}
		public boolean visit(EnumDeclaration node) {
			return visitType(node);
		}
		private boolean visitType(AbstractTypeDeclaration node) {
			result.add(node.getName().getIdentifier());
			// don't dive into type declaration since they open a new
			// context.
			return false;
		}
	}
	
	/**
	 * Creates a new extract method refactoring.
	 */
	private ExtractMethodRefactoring(ICompilationUnit cu, int selectionStart, int selectionLength) throws CoreException {
		Assert.isNotNull(cu);
		fCUnit= cu;
		fImportRewriter= new ImportRewrite(cu);
		fMethodName= "extracted"; //$NON-NLS-1$
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fVisibility= -1;
	}
	
	public static boolean isAvailable(ASTNode[] selectedNodes) {
		if (selectedNodes == null || selectedNodes.length == 0)
			return false;
		if (selectedNodes.length == 1) {
			return selectedNodes[0] instanceof Statement || Checks.isExtractableExpression(selectedNodes[0]);
		} else {
			for (int i= 0; i < selectedNodes.length; i++) {
				if (!(selectedNodes[i] instanceof Statement))
					return false;
			}
		}
		return true;
	}
	
	public static ExtractMethodRefactoring create(ICompilationUnit cu, int selectionStart, int selectionLength) throws CoreException {
		return new ExtractMethodRefactoring(cu, selectionStart, selectionLength);
	}
	
	/* (non-Javadoc)
	 * Method declared in IRefactoring
	 */
	 public String getName() {
	 	return RefactoringCoreMessages.getFormattedString("ExtractMethodRefactoring.name", new String[]{fMethodName, fCUnit.getElementName()}); //$NON-NLS-1$
	 }

	/**
	 * Checks if the refactoring can be activated. Activation typically means, if a
	 * corresponding menu entry can be added to the UI.
	 *
	 * @param pm a progress monitor to report progress during activation checking.
	 * @return the refactoring status describing the result of the activation check.	 
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		
		if (fSelectionStart < 0 || fSelectionLength == 0)
			return mergeTextSelectionStatus(result);
		
		result.merge(Checks.validateModifiesFiles(
			ResourceUtil.getFiles(new ICompilationUnit[]{fCUnit}),
			getValidationContext()));
		if (result.hasFatalError())
			return result;
		
		CompilationUnit root= new RefactoringASTParser(AST.JLS3).parse(fCUnit, true, pm);
		fAST= root.getAST();
		root.accept(createVisitor());
		
		result.merge(fAnalyzer.checkActivation());
		if (result.hasFatalError())
			return result;
		if (fVisibility == -1) {
			setVisibility(Modifier.PRIVATE);
		}
		initializeParameterInfos();
		initializeUsedNames();
		initializeDuplicates();
		initializeDestinations();
		return result;
	}
	
	private ASTVisitor createVisitor() throws JavaModelException {
		fAnalyzer= new ExtractMethodAnalyzer(fCUnit, Selection.createFromStartLength(fSelectionStart, fSelectionLength));
		return fAnalyzer;
	}
	
	/**
	 * Sets the method name to be used for the extracted method.
	 *
	 * @param name the new method name.
	 */	
	public void setMethodName(String name) {
		fMethodName= name;
	}
	
	/**
	 * Returns the method name to be used for the extracted method.
	 * @return the method name to be used for the extracted method.
	 */
	public String getMethodName() {
		return fMethodName;
	} 
	
	/**
	 * Sets the visibility of the new method.
	 * 
	 * @param visibility the visibility of the new method. Valid values are
	 *  "public", "protected", "", and "private"
	 */
	public void setVisibility(int visibility) {
		fVisibility= visibility;
	}
	
	/**
	 * Returns the visibility of the new method.
	 * 
	 * @return the visibility of the new method
	 */
	public int getVisibility() {
		return fVisibility;
	}
	
	/**
	 * Returns the parameter infos.
	 * @return a list of parameter infos.
	 */
	public List getParameterInfos() {
		return fParameterInfos;
	}
	
	/**
	 * Sets whether the new method signature throws runtime exceptions.
	 * 
	 * @param throwRuntimeExceptions flag indicating if the new method
	 * 	throws runtime exceptions
	 */
	public void setThrowRuntimeExceptions(boolean throwRuntimeExceptions) {
		fThrowRuntimeExceptions= throwRuntimeExceptions;
	}
	
	/**
	 * Checks if the new method name is a valid method name. This method doesn't
	 * check if a method with the same name already exists in the hierarchy. This
	 * check is done in <code>checkInput</code> since it is expensive.
	 */
	public RefactoringStatus checkMethodName() {
		return Checks.checkMethodName(fMethodName);
	}
	
	public ASTNode[] getDestinations() {
		return fDestinations;
	}
	
	public void setDestination(int index) {
		fDestination= fDestinations[index];
	}
	
	/**
	 * Checks if the parameter names are valid.
	 */
	public RefactoringStatus checkParameterNames() {
		RefactoringStatus result= new RefactoringStatus();
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo parameter= (ParameterInfo)iter.next();
			result.merge(Checks.checkIdentifier(parameter.getNewName()));
			for (Iterator others= fParameterInfos.iterator(); others.hasNext();) {
				ParameterInfo other= (ParameterInfo) others.next();
				if (parameter != other && other.getNewName().equals(parameter.getNewName())) {
					result.addError(RefactoringCoreMessages.getFormattedString(
						"ExtractMethodRefactoring.error.sameParameter", //$NON-NLS-1$
						other.getNewName()));
					return result;
				}
			}
			if (parameter.isRenamed() && fUsedNames.contains(parameter.getNewName())) {
				result.addError(RefactoringCoreMessages.getFormattedString(
					"ExtractMethodRefactoring.error.nameInUse", //$NON-NLS-1$
					parameter.getNewName()));
				return result;
			}
		}
		return result;
	}
	
	/**
	 * Returns the names already in use in the selected statements/expressions.
	 * 
	 * @return names already in use.
	 */
	public Set getUsedNames() {
		return fUsedNames;
	}
	
	/* (non-Javadoc)
	 * Method declared in Refactoring
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		pm.beginTask(RefactoringCoreMessages.getString("ExtractMethodRefactoring.checking_new_name"), 2); //$NON-NLS-1$
		pm.subTask(EMPTY);
		
		RefactoringStatus result= checkMethodName();
		result.merge(checkParameterNames());
		pm.worked(1);
		if (pm.isCanceled())
			throw new OperationCanceledException();

		BodyDeclaration node= fAnalyzer.getEnclosingBodyDeclaration();
		if (node != null) {
			fAnalyzer.checkInput(result, fMethodName, fCUnit.getJavaProject(), fAST);
			pm.worked(1);
		}
		pm.done();
		return result;
	}
	
	/* (non-Javadoc)
	 * Method declared in IRefactoring
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		if (fMethodName == null)
			return null;
		pm.beginTask("", 2); //$NON-NLS-1$
		fAnalyzer.aboutToCreateChange();
		BodyDeclaration declaration= fAnalyzer.getEnclosingBodyDeclaration();
		fRewriter= ASTRewrite.create(declaration.getAST());
		String sourceMethodName= declaration.getNodeType() == ASTNode.METHOD_DECLARATION 
			? ((MethodDeclaration)declaration).getName().getIdentifier()
			: ""; //$NON-NLS-1$
		
		final CompilationUnitChange result= new CompilationUnitChange(
			RefactoringCoreMessages.getFormattedString("ExtractMethodRefactoring.change_name", new String[]{fMethodName, sourceMethodName}),  //$NON-NLS-1$
			fCUnit);
		result.setSaveMode(TextFileChange.KEEP_SAVE_STATE);
	
		MultiTextEdit root= new MultiTextEdit();
		result.setEdit(root);
		// This is cheap since the compilation unit is already open in a editor.
		IPath path= ((IFile)WorkingCopyUtil.getOriginal(fCUnit).getResource()).getFullPath();
		ITextFileBufferManager bufferManager= FileBuffers.getTextFileBufferManager();
		try {
			bufferManager.connect(path, new SubProgressMonitor(pm, 1));
			IDocument document= bufferManager.getTextFileBuffer(path).getDocument();
			
			ASTNode[] selectedNodes= fAnalyzer.getSelectedNodes();
			ASTNodes.expandRange(selectedNodes, document, fSelectionStart, fSelectionLength);
			MethodDeclaration mm= createNewMethod(fMethodName, true, selectedNodes, document.getLineDelimiter(0));

			TextEditGroup insertDesc= new TextEditGroup(RefactoringCoreMessages.getFormattedString("ExtractMethodRefactoring.add_method", fMethodName)); //$NON-NLS-1$
			result.addTextEditGroup(insertDesc);
			
			if (fDestination == fDestinations[0]) {
				ChildListPropertyDescriptor descriptor= (ChildListPropertyDescriptor)declaration.getLocationInParent();
				ListRewrite container= fRewriter.getListRewrite(declaration.getParent(), descriptor);
				container.insertAfter(mm, declaration, insertDesc);
			} else {
				BodyDeclarationRewrite container= BodyDeclarationRewrite.create(fRewriter, fDestination);
				container.insert(mm, insertDesc);
			}
			
			TextEditGroup description= new TextEditGroup(RefactoringCoreMessages.getFormattedString("ExtractMethodRefactoring.substitute_with_call", fMethodName)); //$NON-NLS-1$
			result.addTextEditGroup(description);
			
			new StatementRewrite(fRewriter, selectedNodes).replace(createCallNodes(null), description);
			
			replaceDuplicates(result);
		
			if (!fImportRewriter.isEmpty()) {
				TextEdit edit= fImportRewriter.createEdit(document);
				root.addChild(edit);
				result.addTextEditGroup(new TextEditGroup(
					RefactoringCoreMessages.getString("ExtractMethodRefactoring.organize_imports"), //$NON-NLS-1$
					new TextEdit[] {edit}
				));
			}
			root.addChild(fRewriter.rewriteAST(document, null));
		} catch (BadLocationException e) {
			throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR,
				e.getMessage(), e));
		} finally {
			bufferManager.disconnect(path, new SubProgressMonitor(pm, 1));
			pm.done();
		}
		return result;
	}
	
	/**
	 * Returns the signature of the new method.
	 * 
	 * @return the signature of the extracted method
	 */
	public String getSignature() {
		return getSignature(fMethodName);
	}
	
	/**
	 * Returns the signature of the new method.
	 * 
	 * @param methodName the method name used for the new method
	 * @return the signature of the extracted method
	 */
	public String getSignature(String methodName) {
		MethodDeclaration method= null;
		try {
			method= createNewMethod(methodName, false, null, String.valueOf('\n'));
		} catch (CoreException cannotHappen) {
			// we don't generate a code block and java comments.
			Assert.isTrue(false);
		}
		method.setBody(fAST.newBlock());
		ASTFlattener flattener= new ASTFlattener() {
			public boolean visit(Block node) {
				return false;
			}
		};
		method.accept(flattener);
		return flattener.getResult();		
	}
	
	/**
	 * Returns the number of duplicate code snippets found.
	 * 
	 * @return the number of duplicate code fragments
	 */
	public int getNumberOfDuplicates() {
		if (fDuplicates == null)
			return 0;
		int result=0;
		for (int i= 0; i < fDuplicates.length; i++) {
			if (!fDuplicates[i].isMethodBody())
				result++;
		}
		return result;
	}
	
	public boolean getReplaceDuplicates() {
		return fReplaceDuplicates;
	}

	public void setReplaceDuplicates(boolean replace) {
		fReplaceDuplicates= replace;
	}
	
	public void setGenerateJavadoc(boolean generate) {
		fGenerateJavadoc= generate;
	}
	
	public boolean getGenerateJavadoc() {
		return fGenerateJavadoc;
	}
	
	//---- Helper methods ------------------------------------------------------------------------
	
	private void initializeParameterInfos() {
		IVariableBinding[] arguments= fAnalyzer.getArguments();
		fParameterInfos= new ArrayList(arguments.length);
		ASTNode root= fAnalyzer.getEnclosingBodyDeclaration();
		for (int i= 0; i < arguments.length; i++) {
			IVariableBinding argument= arguments[i];
			if (argument == null)
				continue;
			VariableDeclaration declaration= ASTNodes.findVariableDeclaration(argument, root);
			boolean isVarargs= declaration instanceof SingleVariableDeclaration 
				? ((SingleVariableDeclaration)declaration).isVarargs()
				: false;
			ParameterInfo info= new ParameterInfo(argument, isVarargs, getType(declaration), argument.getName(), i);
			fParameterInfos.add(info);
		}
	}
	
	private void initializeUsedNames() {
		fUsedNames= UsedNamesCollector.perform(fAnalyzer.getSelectedNodes());
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo parameter= (ParameterInfo)iter.next();
			fUsedNames.remove(parameter.getOldName());
		}
	}
	
	private void initializeDuplicates() {
		ASTNode start= fAnalyzer.getEnclosingBodyDeclaration();
		while(!(start instanceof AbstractTypeDeclaration) && !(start instanceof AnonymousClassDeclaration)) {
			start= start.getParent();
		}
		
		fDuplicates= SnippetFinder.perform(start, fAnalyzer.getSelectedNodes());
		fReplaceDuplicates= fDuplicates.length > 0 && ! fAnalyzer.isLiteralNodeSelected();
	}
	
	private void initializeDestinations() {
		List result= new ArrayList();
		BodyDeclaration decl= fAnalyzer.getEnclosingBodyDeclaration();
		ASTNode current= getNextParent(decl);
		result.add(current);
		if (decl instanceof MethodDeclaration) {
			ITypeBinding binding= ASTNodes.getDeclaringType(current);
			ASTNode next= getNextParent(current);
			while (next != null && binding != null && binding.isNested() && !Modifier.isStatic(binding.getDeclaredModifiers())) {
				result.add(next);
				current= next;
				binding= ASTNodes.getDeclaringType(current);
				next= getNextParent(next);
			}
		}
		fDestinations= (ASTNode[])result.toArray(new ASTNode[result.size()]);
		fDestination= fDestinations[0];
	}
	
	private ASTNode getNextParent(ASTNode node) {
		do {
			node= node.getParent();
		} while (node != null && !((node instanceof AbstractTypeDeclaration) || (node instanceof AnonymousClassDeclaration)));
		return node;
	}
		
	private RefactoringStatus mergeTextSelectionStatus(RefactoringStatus status) {
		status.addFatalError(RefactoringCoreMessages.getString("ExtractMethodRefactoring.no_set_of_statements")); //$NON-NLS-1$
		return status;	
	}
	
	private String getType(VariableDeclaration declaration) {
		return ASTNodes.asString(ASTNodeFactory.newType(declaration.getAST(), declaration));
	}
	
	//---- Code generation -----------------------------------------------------------------------
	
	private ASTNode[] createCallNodes(SnippetFinder.Match duplicate) {
		List result= new ArrayList(2);
		
		IVariableBinding[] locals= fAnalyzer.getCallerLocals();
		for (int i= 0; i < locals.length; i++) {
			result.add(createDeclaration(locals[i], null));
		}
		
		MethodInvocation invocation= fAST.newMethodInvocation();
		invocation.setName(fAST.newSimpleName(fMethodName));
		List arguments= invocation.arguments();
		for (int i= 0; i < fParameterInfos.size(); i++) {
			ParameterInfo parameter= ((ParameterInfo)fParameterInfos.get(i));
			arguments.add(ASTNodeFactory.newName(fAST, getMappedName(duplicate, parameter)));
		}		
		
		ASTNode call;		
		int returnKind= fAnalyzer.getReturnKind();
		switch (returnKind) {
			case ExtractMethodAnalyzer.ACCESS_TO_LOCAL:
				IVariableBinding binding= fAnalyzer.getReturnLocal();
				if (binding != null) {
					VariableDeclarationStatement decl= createDeclaration(getMappedBinding(duplicate, binding), invocation);
					call= decl;
				} else {
					Assignment assignment= fAST.newAssignment();
					assignment.setLeftHandSide(ASTNodeFactory.newName(fAST, 
							getMappedBinding(duplicate, fAnalyzer.getReturnValue()).getName()));
					assignment.setRightHandSide(invocation);
					call= assignment;
				}
				break;
			case ExtractMethodAnalyzer.RETURN_STATEMENT_VALUE:
				ReturnStatement rs= fAST.newReturnStatement();
				rs.setExpression(invocation);
				call= rs;
				break;
			default:
				call= invocation;
		}
		
		if (call instanceof Expression && !fAnalyzer.isExpressionSelected()) {
			call= fAST.newExpressionStatement((Expression)call);
		}
		result.add(call);
		
		// We have a void return statement. The code looks like
		// extracted();
		// return;	
		if (returnKind == ExtractMethodAnalyzer.RETURN_STATEMENT_VOID && !fAnalyzer.isLastStatementSelected()) {
			result.add(fAST.newReturnStatement());
		}
		return (ASTNode[])result.toArray(new ASTNode[result.size()]);		
	}
	
	private IVariableBinding getMappedBinding(SnippetFinder.Match duplicate, IVariableBinding org) {
		if (duplicate == null)
			return org;
		return duplicate.getMappedBinding(org);
	}
	
	private String getMappedName(SnippetFinder.Match duplicate, ParameterInfo paramter) {
		if (duplicate == null)
			return paramter.getOldName();
		return duplicate.getMappedName(paramter.getOldBinding()).getIdentifier();
	}
	
	private void replaceDuplicates(CompilationUnitChange result) {
		int numberOf= getNumberOfDuplicates();
		if (numberOf == 0 || !fReplaceDuplicates)
			return;
		String label= null;
		if (numberOf == 1)
			label= RefactoringCoreMessages.getFormattedString("ExtractMethodRefactoring.duplicates.single", fMethodName); //$NON-NLS-1$
		else
			label= RefactoringCoreMessages.getFormattedString("ExtractMethodRefactoring.duplicates.multi", fMethodName); //$NON-NLS-1$
		
		TextEditGroup description= new TextEditGroup(label);
		result.addTextEditGroup(description);
		
		for (int d= 0; d < fDuplicates.length; d++) {
			SnippetFinder.Match duplicate= fDuplicates[d];
			if (!duplicate.isMethodBody()) {
				ASTNode[] callNodes= createCallNodes(duplicate);
				new StatementRewrite(fRewriter, duplicate.getNodes()).replace(callNodes, description);
			}
		}		
	}
	
	private MethodDeclaration createNewMethod(String name, boolean code, ASTNode[] selectedNodes, String lineDelimiter) throws CoreException {
		MethodDeclaration result= fAST.newMethodDeclaration();
		int modifiers= fVisibility;
		if (Modifier.isStatic(fAnalyzer.getEnclosingBodyDeclaration().getModifiers()) || fAnalyzer.getForceStatic()) {
			modifiers|= Modifier.STATIC;
		}
		ITypeBinding[] typeVariables= fAnalyzer.getTypeVariables();
		List typeParameters= result.typeParameters();
		for (int i= 0; i < typeVariables.length; i++) {
			TypeParameter parameter= fAST.newTypeParameter();
			parameter.setName(fAST.newSimpleName(typeVariables[i].getName()));
			typeParameters.add(parameter);
		}
		result.modifiers().addAll(ASTNodeFactory.newModifiers(fAST, modifiers));
		if (fAnalyzer.isExpressionSelected()) {
			String type= fImportRewriter.addImport(ASTNodes.asString(fAnalyzer.getReturnType()));
			result.setReturnType2(ASTNodeFactory.newType(fAST, type));
		} else {
			result.setReturnType2((Type)ASTNode.copySubtree(fAST, fAnalyzer.getReturnType()));
		}
		result.setName(fAST.newSimpleName(name));
		
		List parameters= result.parameters();
		for (int i= 0; i < fParameterInfos.size(); i++) {
			ParameterInfo info= (ParameterInfo)fParameterInfos.get(i);
			VariableDeclaration infoDecl= getVariableDeclaration(info);
			SingleVariableDeclaration parameter= fAST.newSingleVariableDeclaration();
			parameter.modifiers().addAll(ASTNodeFactory.newModifiers(fAST, ASTNodes.getModifiers(infoDecl)));
			parameter.setType(ASTNodeFactory.newType(fAST, infoDecl));
			parameter.setName(fAST.newSimpleName(info.getNewName()));
			parameter.setVarargs(info.isVarargs());
			parameters.add(parameter);
		}
		
		List exceptions= result.thrownExceptions();
		ITypeBinding[] exceptionTypes= fAnalyzer.getExceptions(fThrowRuntimeExceptions, fAST);
		for (int i= 0; i < exceptionTypes.length; i++) {
			ITypeBinding exceptionType= exceptionTypes[i];
			exceptions.add(ASTNodeFactory.newName(fAST, fImportRewriter.addImport(exceptionType)));
		}
		if (code) {
			result.setBody(createMethodBody(selectedNodes));
			if (fGenerateJavadoc) {
				AbstractTypeDeclaration enclosingType= 
					(AbstractTypeDeclaration)ASTNodes.getParent(fAnalyzer.getEnclosingBodyDeclaration(), AbstractTypeDeclaration.class);
				String string= CodeGeneration.getMethodComment(fCUnit, enclosingType.getName().getIdentifier(), result, null, lineDelimiter);
				if (string != null) {
					Javadoc javadoc= (Javadoc)fRewriter.createStringPlaceholder(string, ASTNode.JAVADOC);
					result.setJavadoc(javadoc);
				}
			}
		}
		
		return result;
	}
	
	private Block createMethodBody(ASTNode[] selectedNodes) {
		Block result= fAST.newBlock();
		ListRewrite statements= fRewriter.getListRewrite(result, Block.STATEMENTS_PROPERTY);
		
		// Locals that are not passed as an arguments since the extracted method only
		// writes to them
		IVariableBinding[] methodLocals= fAnalyzer.getMethodLocals();
		for (int i= 0; i < methodLocals.length; i++) {
			if (methodLocals[i] != null) {
				result.statements().add(createDeclaration(methodLocals[i], null));
			}
		}

		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo parameter= (ParameterInfo)iter.next();
			if (parameter.isRenamed()) {
				for (int n= 0; n < selectedNodes.length; n++) {
					SimpleName[] oldNames= LinkedNodeFinder.findByBinding(selectedNodes[n], parameter.getOldBinding());
					for (int i= 0; i < oldNames.length; i++) {
						fRewriter.replace(oldNames[i], fAST.newSimpleName(parameter.getNewName()), null);
					}
				}
			}
		}
		
		boolean extractsExpression= fAnalyzer.isExpressionSelected();
		if (extractsExpression) {
			// if we have an expression then only one node is selected.
			ITypeBinding binding= fAnalyzer.getExpressionBinding();
			if (binding != null && (!binding.isPrimitive() || !"void".equals(binding.getName()))) { //$NON-NLS-1$
				ReturnStatement rs= fAST.newReturnStatement();
				rs.setExpression((Expression)fRewriter.createCopyTarget(selectedNodes[0]));
				statements.insertLast(rs, null);
			} else {
				ExpressionStatement st= fAST.newExpressionStatement((Expression)fRewriter.createCopyTarget(selectedNodes[0]));
				statements.insertLast(st, null);
			}
		} else {
			for (int i= 0; i < selectedNodes.length; i++) {
				statements.insertLast(fRewriter.createCopyTarget(selectedNodes[i]), null);
			}
			IVariableBinding returnValue= fAnalyzer.getReturnValue();
			if (returnValue != null) {
				ReturnStatement rs= fAST.newReturnStatement();
				rs.setExpression(fAST.newSimpleName(getName(returnValue)));
				statements.insertLast(rs, null);				
			}
		}
		return result;
	}
	
	private String getName(IVariableBinding binding) {
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo)iter.next();
			if (Bindings.equals(binding, info.getOldBinding())) {
				return info.getNewName();
			}
		}
		return binding.getName();
	}
	
	private VariableDeclaration getVariableDeclaration(ParameterInfo parameter) {
		return ASTNodes.findVariableDeclaration(parameter.getOldBinding(), fAnalyzer.getEnclosingBodyDeclaration());
	}
	
	private VariableDeclarationStatement createDeclaration(IVariableBinding binding, Expression intilizer) {
		VariableDeclaration original= ASTNodes.findVariableDeclaration(binding, fAnalyzer.getEnclosingBodyDeclaration());
		VariableDeclarationFragment fragment= fAST.newVariableDeclarationFragment();
		fragment.setName((SimpleName)ASTNode.copySubtree(fAST, original.getName()));
		fragment.setInitializer(intilizer);	
		VariableDeclarationStatement result= fAST.newVariableDeclarationStatement(fragment);
		result.modifiers().addAll(ASTNode.copySubtrees(fAST, ASTNodes.getModifiers(original)));
		result.setType(ASTNodeFactory.newType(fAST, original));
		return result;
	}	

	public ICompilationUnit getCompilationUnit() {
		return fCUnit;
	}
}