/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     jens.lukowski@gmx.de - contributed code to convert prefix and postfix 
 *       expressions into a combination of setter and getter calls.
 *     Dmitry Stalnov (dstalnov@fusionone.com) - contributed fix for
 *       bug Encapuslate field can fail when two variables in one variable declaration (see
 *       https://bugs.eclipse.org/bugs/show_bug.cgi?id=51540).
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.sef;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.Message;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.dom.OldASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

import org.eclipse.jdt.ui.CodeGeneration;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;

/**
 * Encapsulates a field into gettter and setter calls.
 */
public class SelfEncapsulateFieldRefactoring extends Refactoring {

	private IField fField;
	private TextChangeManager fChangeManager;
	
	private CompilationUnit fRoot;
	private VariableDeclarationFragment fFieldDeclaration;
	private OldASTRewrite fRewriter;

	private int fVisibility;
	private String fGetterName;
	private String fSetterName;
	private String fArgName;
	private boolean fSetterMustReturnValue;
	private int fInsertionIndex;	// -1 represents as first method.
	private boolean fEncapsulateDeclaringClass;
	private boolean fGenerateJavadoc;
	
	private List fUsedReadNames;
	private List fUsedModifyNames;
	
	private static final String NO_NAME= ""; //$NON-NLS-1$
	
	private SelfEncapsulateFieldRefactoring(IField field) throws JavaModelException {
		Assert.isNotNull(field);
		fField= field;
		fChangeManager= new TextChangeManager();
		fGetterName= GetterSetterUtil.getGetterName(field, null);
		fSetterName= GetterSetterUtil.getSetterName(field, null);
		fEncapsulateDeclaringClass= true;
		fArgName= NamingConventions.removePrefixAndSuffixForFieldName(field.getJavaProject(), field.getElementName(), field.getFlags());
		checkArgName();
	}
	
	public static boolean isAvailable(IField field) throws JavaModelException {
		return Checks.isAvailable(field);
	}
	
	public static SelfEncapsulateFieldRefactoring create(IField field) throws JavaModelException {
		if (Checks.checkAvailability(field).hasFatalError())
			return null;
		return new SelfEncapsulateFieldRefactoring(field);
	}
	
	//---- Setter and Getter methdos ----------------------------------------------------------
	
	public IField getField() {
		return fField;
	}

	public String getGetterName() {
		return fGetterName;
	}
		
	public void setGetterName(String name) {
		fGetterName= name;
		Assert.isNotNull(fGetterName);
	}

	public String getSetterName() {
		return fSetterName;
	}
	
	public void setSetterName(String name) {
		fSetterName= name;
		Assert.isNotNull(fSetterName);
	}
	
	public void setInsertionIndex(int index) {
		fInsertionIndex= index;
	}
	
	public int getVisibility() {
		return fVisibility;
	}
	
	public void setVisibility(int visibility) {
		fVisibility= visibility;
	}
	
	public void setEncapsulateDeclaringClass(boolean encapsulateDeclaringClass) {
		fEncapsulateDeclaringClass= encapsulateDeclaringClass;
	}

	public boolean getEncapsulateDeclaringClass() {
		return fEncapsulateDeclaringClass;
	}
	
	public boolean getGenerateJavadoc() {
		return fGenerateJavadoc;
	}
	
	public void setGenerateJavadoc(boolean value) {
		fGenerateJavadoc= value;
	}

	//----activation checking ----------------------------------------------------------
	
	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		fVisibility= (fField.getFlags() & (Flags.AccPublic | Flags.AccProtected | Flags.AccPrivate));
		RefactoringStatus result=  new RefactoringStatus();
		
		fRoot= new RefactoringASTParser(AST.JLS2).parse(fField.getCompilationUnit(), true, pm);
		ISourceRange sourceRange= fField.getNameRange();
		ASTNode node= NodeFinder.perform(fRoot, sourceRange.getOffset(), sourceRange.getLength());
		if (node == null) {
			return mappingErrorFound(result, node);
		}
		fFieldDeclaration= (VariableDeclarationFragment)ASTNodes.getParent(node, VariableDeclarationFragment.class);
		if (fFieldDeclaration == null) {
			return mappingErrorFound(result, node);
		}
		if (fFieldDeclaration.resolveBinding() == null) {
			if (!processCompilerError(result, node))
				result.addFatalError(RefactoringCoreMessages.getString("SelfEncapsulateField.type_not_resolveable")); //$NON-NLS-1$
			return result;
		}
		result.merge(Checks.validateModifiesFiles(new IFile[]{ResourceUtil.getFile(fField.getCompilationUnit())}));
		if (result.hasFatalError())
			return result;
		computeUsedNames();
		fRewriter= new OldASTRewrite(fRoot);
		return result;
	}

	private RefactoringStatus mappingErrorFound(RefactoringStatus result, ASTNode node) {
		if (node != null && (node.getFlags() & ASTNode.MALFORMED) != 0 && processCompilerError(result, node))
			return result;
		result.addFatalError(getMappingErrorMessage());
		return result;
	}

	private boolean processCompilerError(RefactoringStatus result, ASTNode node) {
		Message[] messages= ASTNodes.getMessages(node, ASTNodes.INCLUDE_ALL_PARENTS);
		if (messages.length == 0)
			return false;
		result.addFatalError(RefactoringCoreMessages.getFormattedString(
			"SelfEncapsulateField.compiler_errors_field",  //$NON-NLS-1$
			new String[] { fField.getElementName(), messages[0].getMessage()}));
		return true;
	}

	private String getMappingErrorMessage() {
		return RefactoringCoreMessages.getFormattedString(
			"SelfEncapsulateField.cannot_analyze_selected_field", //$NON-NLS-1$
			new String[] {fField.getElementName()});
	}

	//---- Input checking ----------------------------------------------------------
	
	public RefactoringStatus checkMethodNames() {
		RefactoringStatus result= new RefactoringStatus();
		IType declaringType= fField.getDeclaringType();
		checkName(result, fGetterName, fUsedReadNames, declaringType);
		checkName(result, fSetterName, fUsedModifyNames, declaringType);
		return result;
	}
	
	private static void checkName(RefactoringStatus status, String name, List usedNames, IType type) {
		if ("".equals(name)) { //$NON-NLS-1$
			status.addFatalError(RefactoringCoreMessages.getString("Checks.Choose_name")); //$NON-NLS-1$
			return;
	    }
		status.merge(Checks.checkMethodName(name));
		for (Iterator iter= usedNames.iterator(); iter.hasNext(); ) {
			IMethodBinding method= (IMethodBinding)iter.next();
			String selector= method.getName();
			if (selector.equals(name))
				status.addFatalError(RefactoringCoreMessages.getFormattedString(
					"SelfEncapsulateField.method_exists", //$NON-NLS-1$
					new String[] {Bindings.asString(method), type.getElementName()}));
		}
	}	
	
	/*
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		fChangeManager.clear();
		pm.beginTask(NO_NAME, 11);
		pm.setTaskName(RefactoringCoreMessages.getString("SelfEncapsulateField.checking_preconditions")); //$NON-NLS-1$
		result.merge(checkMethodNames());
		pm.worked(1);
		if (result.hasFatalError())
			return result;
		pm.setTaskName(RefactoringCoreMessages.getString("SelfEncapsulateField.searching_for_cunits")); //$NON-NLS-1$
		ICompilationUnit[] affectedCUs= RefactoringSearchEngine.findAffectedCompilationUnits(
			SearchPattern.createPattern(fField, IJavaSearchConstants.REFERENCES),
			RefactoringScopeFactory.create(fField),
			new SubProgressMonitor(pm, 5),
			result);
		
		checkInHierarchy(result);
		if (result.hasFatalError())
			return result;
			
		pm.setTaskName(RefactoringCoreMessages.getString("SelfEncapsulateField.analyzing"));	 //$NON-NLS-1$
		IProgressMonitor sub= new SubProgressMonitor(pm, 5);
		sub.beginTask(NO_NAME, affectedCUs.length);
		IVariableBinding fieldIdentifier= fFieldDeclaration.resolveBinding();
		ITypeBinding declaringClass= 
			((TypeDeclaration)ASTNodes.getParent(fFieldDeclaration, TypeDeclaration.class)).resolveBinding();
		List ownerDescriptions= new ArrayList();
		ICompilationUnit owner= fField.getCompilationUnit();
		for (int i= 0; i < affectedCUs.length; i++) {
			ICompilationUnit unit= affectedCUs[i];
			sub.subTask(unit.getElementName());
			CompilationUnit root= null;
			OldASTRewrite rewriter= null;
			List descriptions;
			if (owner.equals(unit)) {
				root= fRoot;
				rewriter= fRewriter;
				descriptions= ownerDescriptions;
			} else {
				root= new RefactoringASTParser(AST.JLS2).parse(unit, true);
				rewriter= new OldASTRewrite(root);
				descriptions= new ArrayList();
			}
			checkCompileErrors(result, root, unit);
			AccessAnalyzer analyzer= new AccessAnalyzer(this, unit, fieldIdentifier, declaringClass, rewriter);
			root.accept(analyzer);
			result.merge(analyzer.getStatus());
			if (!fSetterMustReturnValue) 
				fSetterMustReturnValue= analyzer.getSetterMustReturnValue();
			if (result.hasFatalError()) {
				fChangeManager.clear();
				return result;
			}
			descriptions.addAll(analyzer.getGroupDescriptions());
			if (!owner.equals(unit))
				createEdits(unit, rewriter, descriptions);
			sub.worked(1);
			if (pm.isCanceled())
				throw new OperationCanceledException();
		}
		
		TextBuffer buffer= TextBuffer.acquire(getFile(owner));
		try {
			ownerDescriptions.addAll(addGetterSetterChanges(fRoot, fRewriter, buffer.getLineDelimiter()));
			createEdits(owner, fRewriter, ownerDescriptions, buffer);
		} finally {
			TextBuffer.release(buffer);
		}
		sub.done();
		result.merge(validateModifiesFiles());
		return result;
	}

	private void createEdits(ICompilationUnit unit, OldASTRewrite rewriter, List groups) throws CoreException {
		TextBuffer buffer= TextBuffer.acquire(getFile(unit));
		try {
			createEdits(unit, rewriter, groups, buffer);
		} finally {
			TextBuffer.release(buffer);
		}
	}

	private void createEdits(ICompilationUnit unit, OldASTRewrite rewriter, List groups, TextBuffer buffer) {
		TextChange change= fChangeManager.get(unit);
		MultiTextEdit root= new MultiTextEdit(); 
		rewriter.rewriteNode(buffer, root);
		change.setEdit(root);
		for (Iterator iter= groups.iterator(); iter.hasNext();) {
			change.addTextEditGroup((TextEditGroup)iter.next());
		}
	}

	/*
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		final DynamicValidationStateChange result= new DynamicValidationStateChange(getName());
		TextChange[] changes= fChangeManager.getAllChanges();
		pm.beginTask(NO_NAME, changes.length);
		pm.setTaskName(RefactoringCoreMessages.getString("SelfEncapsulateField.create_changes")); //$NON-NLS-1$
		for (int i= 0; i < changes.length; i++) {
			result.add(changes[i]);
			pm.worked(1);
		}
		pm.done();
		return result;
	}

	/*
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("SelfEncapsulateField.name"); //$NON-NLS-1$
	}
	
	//---- Helper methods -------------------------------------------------------------
	
	private void checkCompileErrors(RefactoringStatus result, CompilationUnit root, ICompilationUnit element) {
		Message[] messages= root.getMessages();
		if (messages.length != 0) {
			result.addError(RefactoringCoreMessages.getFormattedString(
				"SelfEncapsulateField.compiler_errors_update", //$NON-NLS-1$
				element.getElementName()), JavaStatusContext.create(element));
		}
	}
	
	private void checkInHierarchy(RefactoringStatus status) {
		TypeDeclaration declaration= (TypeDeclaration)ASTNodes.getParent(fFieldDeclaration, ASTNode.TYPE_DECLARATION);
		ITypeBinding type= declaration.resolveBinding();
		if (type != null) {
			ITypeBinding fieldType= fFieldDeclaration.resolveBinding().getType();
			status.merge(Checks.checkMethodInHierarchy(type, fGetterName, fieldType, new ITypeBinding[0], fField.getJavaProject()));
			status.merge(Checks.checkMethodInHierarchy(type, fSetterName, fFieldDeclaration.getAST().resolveWellKnownType("void"), //$NON-NLS-1$
				new ITypeBinding[] {fieldType}, 
				fField.getJavaProject()));
		}
	}
	
	private void computeUsedNames() {
		fUsedReadNames= new ArrayList(0);
		fUsedModifyNames= new ArrayList(0);
		IVariableBinding binding= fFieldDeclaration.resolveBinding();
		ITypeBinding type= binding.getType();
		IMethodBinding[] methods= binding.getDeclaringClass().getDeclaredMethods();
		for (int i= 0; i < methods.length; i++) {
			IMethodBinding method= methods[i];
			ITypeBinding[] parameters= methods[i].getParameterTypes();
			if (parameters == null || parameters.length == 0) {
				fUsedReadNames.add(method);
			} else if (parameters.length == 1 && parameters[0] == type) {
				fUsedModifyNames.add(method);
			} 
		}
	}

	private List addGetterSetterChanges(CompilationUnit root, OldASTRewrite rewriter, String lineDelimiter) throws CoreException {
		List result= new ArrayList(2);
		AST ast= root.getAST();
		FieldDeclaration decl= (FieldDeclaration)ASTNodes.getParent(fFieldDeclaration, ASTNode.FIELD_DECLARATION);
		if (!JdtFlags.isPrivate(fField)) {
			result.add(makeDeclarationPrivate(rewriter, decl));
		}
		
		int position= 0;
		int numberOfMethods= 0;
		List members= ASTNodes.getBodyDeclarations(decl.getParent());
		for (Iterator iter= members.iterator(); iter.hasNext();) {
			BodyDeclaration element= (BodyDeclaration)iter.next();
			if (element.getNodeType() == ASTNode.METHOD_DECLARATION) {
				if (fInsertionIndex == -1) {
					break;
				} else if (fInsertionIndex == numberOfMethods) {
					position++;
					break;
				}
				numberOfMethods++;	
			}
			position++;
		}
		TextEditGroup description;
		if (!JdtFlags.isFinal(fField)) {
			description= new TextEditGroup(RefactoringCoreMessages.getString("SelfEncapsulateField.add_setter")); //$NON-NLS-1$
			result.add(description);
			members.add(position++, createSetterMethod(ast, rewriter, description, lineDelimiter));
		}
		description= new TextEditGroup(RefactoringCoreMessages.getString("SelfEncapsulateField.add_getter")); //$NON-NLS-1$
		result.add(description);
		members.add(position, createGetterMethod(ast, rewriter, description, lineDelimiter));
		return result;
	}

	private TextEditGroup makeDeclarationPrivate(OldASTRewrite rewriter, FieldDeclaration decl) {
		AST ast= rewriter.getAST();
		TextEditGroup description= new TextEditGroup(RefactoringCoreMessages
			.getString("SelfEncapsulateField.change_visibility")); //$NON-NLS-1$
		
		if (decl.fragments().size() > 1) {
			rewriter.remove(fFieldDeclaration, description);
			List bodyDeclarations= ASTNodes.getBodyDeclarations(decl.getParent());
			int index= bodyDeclarations.indexOf(decl);
			VariableDeclarationFragment newField= (VariableDeclarationFragment)rewriter.createCopyTarget(fFieldDeclaration);
			decl= ast.newFieldDeclaration(newField);
			decl.setModifiers(Modifier.PRIVATE);
			bodyDeclarations.add(index + 1, decl);
			rewriter.markAsInserted(decl, description);
		} else {
			int newModifiers= ASTNodes.changeVisibility(decl.getModifiers(), Modifier.PRIVATE);
			rewriter.set(decl, FieldDeclaration.MODIFIERS_PROPERTY, new Integer(newModifiers), description);
		}
		return description;
	}

	private MethodDeclaration createSetterMethod(AST ast, OldASTRewrite rewriter, TextEditGroup description, String lineDelimiter) throws CoreException {
		FieldDeclaration field= (FieldDeclaration)ASTNodes.getParent(fFieldDeclaration, FieldDeclaration.class);
		Type type= field.getType();
		MethodDeclaration result= ast.newMethodDeclaration();
		rewriter.markAsInserted(result, description);
		result.setName(ast.newSimpleName(fSetterName));
		result.setModifiers(createModifiers());
		if (fSetterMustReturnValue) {
			result.setReturnType((Type)rewriter.createCopyTarget(type));
		}
		SingleVariableDeclaration param= ast.newSingleVariableDeclaration();
		result.parameters().add(param);
		param.setName(ast.newSimpleName(fArgName));
		param.setType((Type)rewriter.createCopyTarget(type));
		
		Block block= ast.newBlock();
		result.setBody(block);
		Assignment ass= ast.newAssignment();
		ass.setLeftHandSide(createFieldAccess(ast));
		ass.setRightHandSide(ast.newSimpleName(fArgName));
		if (fSetterMustReturnValue) {
			ReturnStatement rs= ast.newReturnStatement();
			rs.setExpression(ass);
			block.statements().add(rs);
		} else {
			block.statements().add(ast.newExpressionStatement(ass));
		}
		
		if (fGenerateJavadoc) {
			String string= CodeGeneration.getSetterComment(
				fField.getCompilationUnit() , getTypeName(field.getParent()), fSetterName, 
				fField.getElementName(), ASTNodes.asString(type), fArgName, 
				NamingConventions.removePrefixAndSuffixForFieldName(fField.getJavaProject(), fField.getElementName(), fField.getFlags()),
				lineDelimiter);
			if (string != null) {
				Javadoc javadoc= (Javadoc)fRewriter.createStringPlaceholder(string, ASTNode.JAVADOC);
				result.setJavadoc(javadoc);
			}
		}
		return result;
	}
	
	private MethodDeclaration createGetterMethod(AST ast, OldASTRewrite rewriter, TextEditGroup description, String lineDelimiter) throws CoreException {
		FieldDeclaration field= (FieldDeclaration)ASTNodes.getParent(fFieldDeclaration, FieldDeclaration.class);
		Type type= field.getType();
		MethodDeclaration result= ast.newMethodDeclaration();
		rewriter.markAsInserted(result, description);
		result.setName(ast.newSimpleName(fGetterName));
		result.setModifiers(createModifiers());
		result.setReturnType((Type)rewriter.createCopyTarget(type));
		
		Block block= ast.newBlock();
		result.setBody(block);
		ReturnStatement rs= ast.newReturnStatement();
		rs.setExpression(ast.newSimpleName(fField.getElementName()));
		block.statements().add(rs);
		if (fGenerateJavadoc) {
			String string= CodeGeneration.getGetterComment(
				fField.getCompilationUnit() , getTypeName(field.getParent()), fGetterName,
				fField.getElementName(), ASTNodes.asString(type), 
				NamingConventions.removePrefixAndSuffixForFieldName(fField.getJavaProject(), fField.getElementName(), fField.getFlags()),
				lineDelimiter);
			if (string != null) {
				Javadoc javadoc= (Javadoc)fRewriter.createStringPlaceholder(string, ASTNode.JAVADOC);
				result.setJavadoc(javadoc);
			}
		}
		return result;
	}

	private int createModifiers() throws JavaModelException {
		int result= 0;
		if (Flags.isPublic(fVisibility)) 
			result |= Modifier.PUBLIC;
		else if (Flags.isProtected(fVisibility)) 
			result |= Modifier.PROTECTED;
		else if (Flags.isPrivate(fVisibility))
			result |= Modifier.PRIVATE;
		if (JdtFlags.isStatic(fField)) 
			result |= Modifier.STATIC; 
		return result;
	}
	
	private Expression createFieldAccess(AST ast) throws JavaModelException {
		String fieldName= fField.getElementName();
		if (fArgName.equals(fieldName)) {
			if (JdtFlags.isStatic(fField)) {
				return ast.newQualifiedName(
					ast.newSimpleName(fField.getDeclaringType().getElementName()), 
					ast.newSimpleName(fieldName));
			} else {
				FieldAccess result= ast.newFieldAccess();
				result.setExpression(ast.newThisExpression());
				result.setName(ast.newSimpleName(fieldName));
				return result;
			}
		} else {
			return ast.newSimpleName(fieldName);
		}
	}
	
	private void checkArgName() {
		String fieldName= fField.getElementName();
		boolean isStatic= true;
		try {
			isStatic= JdtFlags.isStatic(fField);
		} catch(JavaModelException e) {
		}
		if (isStatic && fArgName.equals(fieldName) && fieldName.equals(fField.getDeclaringType().getElementName()))
			fArgName= "_" + fieldName; //$NON-NLS-1$
	}
	
	private static IFile getFile(ICompilationUnit cu) {
		return (IFile)WorkingCopyUtil.getOriginal(cu).getResource();
	}
	
	private RefactoringStatus validateModifiesFiles(){
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}			

	private IFile[] getAllFilesToModify(){
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}
	
	private String getTypeName(ASTNode type) {
		if (type instanceof AbstractTypeDeclaration) {
			return ((AbstractTypeDeclaration)type).getName().getIdentifier();
		} else if (type instanceof AnonymousClassDeclaration) {
			ClassInstanceCreation node= (ClassInstanceCreation)ASTNodes.getParent(type, ClassInstanceCreation.class);
			return ASTNodes.asString(node.getName());
		}
		Assert.isTrue(false, "Should not happen"); //$NON-NLS-1$
		return null;
	}
}

