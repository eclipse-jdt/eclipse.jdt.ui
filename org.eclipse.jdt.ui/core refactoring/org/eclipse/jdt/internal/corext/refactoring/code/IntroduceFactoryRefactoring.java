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

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.core.JavaModelStatus;
import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.ValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ASTCreator;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;

/**
 * Refactoring class that permits the substitution of a factory method
 * for direct calls to a given constructor.
 * @author rfuhrer
 */
public class IntroduceFactoryRefactoring extends Refactoring {
	/**
	 * The <code>CodeGenerationSettings</code> governing this refactoring instance,
	 * typically passed in through the constructor.
	 */
	private CodeGenerationSettings	fCodeGenSettings;

	/**
	 * The handle for the compilation unit holding the selection that was
	 * passed into this refactoring.
	 */
	private ICompilationUnit fCUHandle;

	/**
	 * The AST for the compilation unit holding the selection that was
	 * passed into this refactoring.
	 */
	private CompilationUnit fCU;

	/**
	 * Handle for compilation unit in which the factory method/class/interface will be
	 * generated.
	 */
	private ICompilationUnit fFactoryUnitHandle;

	/**
	 * The start of the original textual selection in effect when this refactoring
	 * was initiated. If the refactoring was initiated from a structured selection
	 * (e.g. from the outline view), then this refers to the textual selection that
	 * corresponds to the structured selection item.
	 */
	private int fSelectionStart;

	/**
	 * The length of the original textual selection in effect when this refactoring
	 * was initiated. If the refactoring was initiated from a structured selection
	 * (e.g. from the outline view), then this refers to the textual selection that
	 * corresponds to the structured selection item.
	 */
	private int fSelectionLength;

	/**
	 * The AST node corresponding to the user's textual selection.
	 */
	private ASTNode fSelectedNode;

	/**
	 * The method binding for the selected constructor.
	 */
	private IMethodBinding fCtorBinding;

	/**
	 * <code>TypeDeclaration</code> for class containing the constructor to be
	 * encapsulated.
	 */
	private TypeDeclaration fCtorOwningClass;

	/**
	 * The name to be given to the generated factory method.
	 */
	private String fNewMethodName= null;

	/**
	 * An array of <code>SearchResultGroup</code>'s of all call sites
	 * that refer to the constructor signature in question.
	 */
	private SearchResultGroup[] fAllCallsTo;

	/**
	 * The class that will own the factory method/class/interface.
	 */
	private TypeDeclaration fFactoryOwningClass;

	/**
	 * The newly-generated factory method.
	 */
	private MethodDeclaration fFactoryMethod= null;

	/**
	 * An array containing the names of the constructor's formal arguments,
	 * if available, otherwise "arg1" ... "argN".
	 */
	private String[] fFormalArgNames= null;

	/**
	 * An array of <code>ITypeBinding</code>'s that describes the types of
	 * the constructor arguments, in order.
	 */
	private ITypeBinding[] fArgTypes;

	/**
	 * If true, change the visibility of the constructor to protected to better
	 * encapsulate it.
	 */
	private boolean fProtectConstructor= true;

	/**
	 * An <code>ImportRewrite</code> that manages imports needed to satisfy
	 * newly-introduced type references in the <code>ICompilationUnit</code>
	 * currently being rewritten during <code>createChange()</code>.
	 */
	private ImportRewrite fImportRewriter;

	/**
	 * True iff there are call sites for the constructor to be encapsulated
	 * located in binary classes.
	 */
	private boolean fCallSitesInBinaryUnits;

	/**
	 * <code>CompilationUnit</code> in which the factory is to be created.
	 */
	private CompilationUnit fFactoryCU;

	/**
	 * 
	 */
	private int fConstructorVisibility= Modifier.PRIVATE;

	public static boolean isAvailable(IMethod method) throws JavaModelException {
		return Checks.isAvailable(method) && method.isConstructor();
	}

	public static IntroduceFactoryRefactoring create(ICompilationUnit cu,
													 int selectionStart, int selectionLength,
													 CodeGenerationSettings settings) {
		return new IntroduceFactoryRefactoring(cu, selectionStart, selectionLength, settings);
	}

	/**
	 * Creates a new <code>IntroduceFactoryRefactoring</code> with the given selection
	 * on the given compilation unit.
	 * @param cu the <code>ICompilationUnit</code> in which the user selection was made
	 * @param selectionStart the start of the textual selection in <code>cu</code>
	 * @param selectionLength the length of the textual selection in <code>cu</code>
	 * @param settings
	 */
	private IntroduceFactoryRefactoring(ICompilationUnit cu, int selectionStart, int selectionLength,
							CodeGenerationSettings settings) {
		super();
		Assert.isTrue(selectionStart  >= 0);
		Assert.isTrue(selectionLength >= 0);
		Assert.isTrue(cu.exists());
		Assert.isNotNull(settings);

		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;

		fCUHandle= cu;
		fCU= ASTCreator.createAST(fCUHandle, null);

		fCodeGenSettings= settings;
	}

	/**
	 * Finds and returns the <code>ASTNode</code> for the given source text
	 * selection, if it is an entire constructor call or the class name portion
	 * of a constructor call or constructor declaration, or null otherwise.
	 * @param unit The compilation unit in which the selection was made 
	 * @param offset The textual offset of the start of the selection
	 * @param length The length of the selection in characters
	 * @return ClassInstanceCreation or MethodDeclaration
	 */
	private ASTNode getTargetNode(ICompilationUnit unit, int offset, int length) {
		ASTNode		node= NodeFinder.perform(fCU, offset, length);

		// First, find the proper node to inspect (may be the parent or child
		// of the selected node, e.g., when the selected node is the name of
		// the constructor to call, such that the parent is really the ctor
		// invocation expression.
		if (node.getNodeType() == ASTNode.SIMPLE_NAME)
			node= node.getParent();
		else if (node.getNodeType() == ASTNode.EXPRESSION_STATEMENT)
			node= ((ExpressionStatement) node).getExpression();

		if (node.getNodeType() == ASTNode.QUALIFIED_NAME)
			node= node.getParent();

		if (node.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION ||
			(node.getNodeType() == ASTNode.METHOD_DECLARATION && ((MethodDeclaration) node).isConstructor()))
			return node;
		else
			return null;
	}

	/**
	 * Determines what kind of AST node was selected, and returns an error status
	 * if the kind of node is inappropriate for this refactoring.
	 * @param pm
	 * @return a RefactoringStatus indicating whether the selection is valid
	 * @throws JavaModelException
	 */
	private RefactoringStatus checkSelection(IProgressMonitor pm) throws JavaModelException {
		try {
			pm.beginTask(RefactoringCoreMessages.getString("IntroduceFactory.examiningSelection"), 2); //$NON-NLS-1$
	
			fSelectedNode= getTargetNode(fCUHandle, fSelectionStart, fSelectionLength);
	
			if (fSelectedNode == null)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("IntroduceFactory.notAConstructorInvocation")); //$NON-NLS-1$
	
			// getTargetNode() must return either a ClassInstanceCreation or a
			// constructor MethodDeclaration; nothing else.
			if (fSelectedNode instanceof ClassInstanceCreation)
				fCtorBinding= ((ClassInstanceCreation) fSelectedNode).resolveConstructorBinding();
			else if (fSelectedNode instanceof MethodDeclaration)
				fCtorBinding= ((MethodDeclaration) fSelectedNode).resolveBinding();
	
			if (fCtorBinding == null)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("IntroduceFactory.unableToResolveConstructorBinding")); //$NON-NLS-1$
	
			if (fNewMethodName == null)
				fNewMethodName= "create" + fCtorBinding.getName();//$NON-NLS-1$
	
			pm.worked(1);
	
			// We don't handle constructors of nested types at the moment
			if (fCtorBinding.getDeclaringClass().isNested())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("IntroduceFactory.unsupportedNestedTypes")); //$NON-NLS-1$
	
			ITypeBinding	ctorType= fCtorBinding.getDeclaringClass();
			IType			ctorOwningType= Bindings.findType(ctorType, fCUHandle.getJavaProject());
	
			if (ctorOwningType.isBinary())
				// Can't modify binary CU; don't know what CU to put factory method
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("IntroduceFactory.constructorInBinaryClass")); //$NON-NLS-1$
	
			// Put the generated factory method inside the type that owns the constructor
			fFactoryUnitHandle= ctorOwningType.getCompilationUnit();
			fFactoryCU= getASTFor(fFactoryUnitHandle);
	
			Name	ctorOwnerName= (Name) NodeFinder.perform(fFactoryCU, ctorOwningType.getNameRange());
	
			fCtorOwningClass= (TypeDeclaration) ASTNodes.getParent(ctorOwnerName, TypeDeclaration.class);
			fFactoryOwningClass= fCtorOwningClass;
	
			pm.worked(1);
	
			return new RefactoringStatus();
		} finally {
			pm.done();
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkActivation(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.getString("IntroduceFactory.checkingActivation"), 1); //$NON-NLS-1$
	
			if (!fCUHandle.isStructureKnown())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("IntroduceFactory.syntaxError")); //$NON-NLS-1$
	
			return checkSelection(new SubProgressMonitor(pm, 1));
		} finally {
			pm.done();
		}
	}

	/**
	 * Returns the set of compilation units that will be affected by this
	 * particular invocation of this refactoring. This in general includes
	 * the class containing the constructor in question, as well as all
	 * call sites to the constructor.
	 * @return ICompilationUnit[]
	 */
	private ICompilationUnit[] collectAffectedUnits(SearchResultGroup[] searchHits) {
		Collection	result= new ArrayList();
		boolean hitInFactoryClass= false;

		for(int i=0; i < searchHits.length; i++) {
			SearchResultGroup	rg=  searchHits[i];
			ICompilationUnit	icu= rg.getCompilationUnit();

			result.add(icu);
			if (icu.equals(fFactoryUnitHandle))
				hitInFactoryClass= true;
		}
		if (!hitInFactoryClass)
			result.add(fFactoryUnitHandle);
		return (ICompilationUnit[]) result.toArray(new ICompilationUnit[result.size()]);
	}

	/**
	 * Returns an <code>ISearchPattern</code> that finds all calls to the constructor
	 * identified by the argument <code>methodBinding</code>.
	 */
	private ISearchPattern createSearchPattern(IMethodBinding methodBinding) throws JavaModelException {
		Assert.isNotNull(methodBinding,
				RefactoringCoreMessages.getString("IntroduceFactory.noBindingForSelectedConstructor")); //$NON-NLS-1$

		// Find the IMethod corresponding to the given method binding and use that
		// as the basis for the search pattern.
		ICompilationUnit unit= ASTCreator.getCu(fSelectedNode);
		IJavaProject javaProject= (IJavaProject) unit.getAncestor(IJavaElement.JAVA_PROJECT);
		IMethod method= Bindings.findMethod(methodBinding, javaProject);

		if (method != null)
			return SearchEngine.createSearchPattern(method, IJavaSearchConstants.REFERENCES);
		else { // perhaps a synthetic method? (but apparently not always... hmmm...)
			// Can't find an IMethod for this method, so build a string pattern instead
			StringBuffer	buf= new StringBuffer();

			buf.append(methodBinding.getDeclaringClass().getQualifiedName())
			   .append("(");//$NON-NLS-1$
			for(int i=0; i < fArgTypes.length; i++) {
				if (i != 0)
					buf.append(","); //$NON-NLS-1$
				buf.append(fArgTypes[i].getQualifiedName());
			}
			buf.append(")"); //$NON-NLS-1$
			return SearchEngine.createSearchPattern(buf.toString(), IJavaSearchConstants.CONSTRUCTOR, IJavaSearchConstants.REFERENCES, true);
		}
	}

	/**
	 * Returns an array of <code>SearchResultGroup</code>'s like the argument,
	 * but omitting those groups that have no corresponding compilation unit
	 * (i.e. are binary and therefore can't be modified).
	 */
	private SearchResultGroup[] excludeBinaryUnits(SearchResultGroup[] groups) {
		Collection/*<SearchResultGroup>*/	result= new ArrayList();

		for (int i = 0; i < groups.length; i++) {
			SearchResultGroup	rg=   groups[i];
			ICompilationUnit	unit= rg.getCompilationUnit();

			if (unit != null)	// Ignore hits within a binary unit
				result.add(rg);
			else
				fCallSitesInBinaryUnits= true;
		}
		return (SearchResultGroup[]) result.toArray(new SearchResultGroup[result.size()]);
	}

	/**
	 * Search for all calls to the given <code>IMethodBinding</code> in the project
	 * that contains the compilation unit <code>fCUHandle</code>.
	 * @param methodBinding
	 * @param pm
	 * @return an array of <code>SearchResultGroup</code>'s that identify the search matches
	 * @throws JavaModelException
	 */
	private SearchResultGroup[] searchForCallsTo(IMethodBinding methodBinding, IProgressMonitor pm) throws JavaModelException {
		ISearchPattern		pattern=  createSearchPattern(methodBinding);
		IJavaProject		javaProj= (IJavaProject) fCUHandle.getAncestor(IJavaElement.JAVA_PROJECT);
		IJavaSearchScope	scope=	  SearchEngine.createJavaSearchScope(new IJavaElement[] { javaProj }, false);
		SearchResultGroup[]	groups=	  RefactoringSearchEngine.search(pm, scope, pattern);

		return groups;
	}

	/**
	 * Returns an array of <code>SearchResultGroup</code>'s containing all method
	 * calls in the Java project that invoke the constructor identified by the given
	 * <code>IMethodBinding</code>
	 * @param ctorBinding an <code>IMethodBinding</code> identifying a particular
	 * constructor signature to search for
	 * @param pm an <code>IProgressMonitor</code> to use during this potentially
	 * lengthy operation
	 * @return an array of <code>SearchResultGroup</code>'s identifying all
	 * calls to the given constructor signature
	 */
	private SearchResultGroup[] findAllCallsTo(IMethodBinding ctorBinding, IProgressMonitor pm) throws JavaModelException {
		SearchResultGroup[] groups= excludeBinaryUnits(searchForCallsTo(ctorBinding, pm));

		return groups;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.getString("IntroduceFactory.checking_preconditions"), 1); //$NON-NLS-1$

			fArgTypes= fCtorBinding.getParameterTypes();
			fAllCallsTo= findAllCallsTo(fCtorBinding, pm);
			fFormalArgNames= findCtorArgNames();
 
			ICompilationUnit[]	affectedFiles= collectAffectedUnits(fAllCallsTo);
			RefactoringStatus	result= Checks.validateModifiesFiles(ResourceUtil.getFiles(affectedFiles));

			if (fCallSitesInBinaryUnits)
				result.merge(RefactoringStatus.createWarningStatus(RefactoringCoreMessages.getString("IntroduceFactory.callSitesInBinaryClass"))); //$NON-NLS-1$

			return result;
		} finally {
			pm.done();
		}
	}

	/**
	 * Returns an array containing the argument names for the constructor
	 * identified by <code>fCtorBinding</code>, if available, or default
	 * names if unavailable (e.g. if the constructor resides in a binary unit).
	 */
	private String[] findCtorArgNames() {
		int			numArgs= fCtorBinding.getParameterTypes().length;
		String[]	names= new String[numArgs];

		CompilationUnit		ctorUnit= (CompilationUnit) ASTNodes.getParent(fCtorOwningClass, CompilationUnit.class);
		MethodDeclaration	ctorDecl= (MethodDeclaration) ctorUnit.findDeclaringNode(fCtorBinding.getKey());

		if (ctorDecl != null) {
			List	formalArgs= ctorDecl.parameters();
			int		i= 0;

			for(Iterator iter= formalArgs.iterator(); iter.hasNext(); i++) {
				SingleVariableDeclaration	svd= (SingleVariableDeclaration) iter.next();

				names[i]= svd.getName().getIdentifier();
			}
			return names;
		}

		// Have no way of getting the formal argument names; just fake it.
		for(int i=0; i < numArgs; i++)
			names[i]= "arg" + (i+1); //$NON-NLS-1$

		return names;
	}

	/**
	 * Creates and returns a new MethodDeclaration that represents the factory
	 * method to be used in place of direct calls to the constructor in question.
	 * @param ast An AST used as a factory for various AST nodes
	 * @param ctorBinding binding for the constructor being wrapped
	 * @param unitRewriter the ASTRewrite to be used
	 * @return
	 */
	private MethodDeclaration createFactoryMethod(AST ast, IMethodBinding ctorBinding, ASTRewrite unitRewriter) {
		MethodDeclaration		newMethod= ast.newMethodDeclaration();
		SimpleName				newMethodName= ast.newSimpleName(fNewMethodName);
		ClassInstanceCreation	newCtorCall= ast.newClassInstanceCreation();
		ReturnStatement			ret= ast.newReturnStatement();
		Block		body= ast.newBlock();
		List		stmts= body.statements();
		String		retTypeName= ctorBinding.getName();

		createFactoryMethodSignature(ast, newMethod);

		newMethod.setName(newMethodName);
		newMethod.setBody(body);
		newMethod.setReturnType(ast.newSimpleType(ast.newSimpleName(retTypeName)));
		newMethod.setModifiers(Modifier.STATIC | Modifier.PUBLIC);

		newCtorCall.setName(ast.newSimpleName(retTypeName));

		createFactoryMethodConstructorArgs(ast, newCtorCall);

		ret.setExpression(newCtorCall);
		stmts.add(ret);
		unitRewriter.markAsInserted(ret);

		return newMethod;
	}

	/**
	 * Creates and adds the necessary argument declarations to the given factory method.<br>
	 * An argument is needed for each original constructor argument for which the
	 * evaluation of the actual arguments across all calls was not able to be
	 * pushed inside the factory method (e.g. arguments with side-effects, references
	 * to fields if the factory method is to be static or reside in a factory class,
	 * or arguments that varied across the set of constructor calls).<br>
	 * <code>fArgTypes</code> identifies such arguments by a <code>null</code> value.
	 * @param ast utility object used to create AST nodes
	 * @param newMethod the <code>MethodDeclaration</code> for the factory method
	 */
	private void createFactoryMethodSignature(AST ast, MethodDeclaration newMethod) {
		List	argTypes= newMethod.parameters();

		for(int i=0; i < fArgTypes.length; i++) {
			SingleVariableDeclaration	argDecl= ast.newSingleVariableDeclaration();

			argDecl.setName(ast.newSimpleName(fFormalArgNames[i]));
			argDecl.setType(ASTNodeFactory.newType(ast, fArgTypes[i], true));
			argTypes.add(argDecl);
		}

		ITypeBinding[]	ctorExcepts= fCtorBinding.getExceptionTypes();
		List			exceptions= newMethod.thrownExceptions();

		for(int i=0; i < ctorExcepts.length; i++) {
			String	excName= fImportRewriter.addImport(ctorExcepts[i]);

			exceptions.add(ASTNodeFactory.newName(ast, excName));
		}
	}

	/**
	 * Create the list of actual arguments to the constructor call that is
	 * encapsulated inside the factory method, and associate the arguments
	 * with the given constructor call object.
	 * @param ast utility object used to create AST nodes
	 * @param newCtorCall the newly-generated constructor call to be wrapped inside
	 * the factory method
	 */
	private void createFactoryMethodConstructorArgs(AST ast, ClassInstanceCreation newCtorCall) {
		List	argList= newCtorCall.arguments();

		for(int i=0; i < fArgTypes.length; i++) {
			ASTNode	ctorArg= ast.newSimpleName(fFormalArgNames[i]);

			argList.add(ctorArg);
		}
	}

	/**
	 * Creates and returns a new MethodInvocation node to represent a call to
	 * the factory method that replaces a direct constructor call.<br>
	 * The original constructor call is marked as replaced by the new method
	 * call with the ASTRewrite instance fCtorCallRewriter.
	 * @param ast utility object used to create AST nodes
	 * @param ctorCall the ClassInstanceCreation to be marked as replaced
	 */
	private MethodInvocation createFactoryMethodCall(AST ast, ClassInstanceCreation ctorCall,
													 ASTRewrite unitRewriter, TextEditGroup gd) {
		MethodInvocation	factoryMethodCall= ast.newMethodInvocation();

		List	actualFactoryArgs= factoryMethodCall.arguments();
		List	actualCtorArgs= ctorCall.arguments();
		String	rawMethodName= fNewMethodName;
		String	methodName;

		// Need to use a qualified name for the factory method if we're not
		// in the context of the class holding the factory.
		TypeDeclaration	callOwner= (TypeDeclaration) ASTNodes.getParent(ctorCall, TypeDeclaration.class);
		ITypeBinding callOwnerBinding= callOwner.resolveBinding();

		if (callOwnerBinding == null ||
			!Bindings.equals(callOwner.resolveBinding(), fFactoryOwningClass.resolveBinding())) {
			String	qualifier= fImportRewriter.addImport(fFactoryOwningClass.resolveBinding());

			methodName= qualifier + "." + rawMethodName; //$NON-NLS-1$
		} else
			methodName= rawMethodName;

		factoryMethodCall.setName(ast.newSimpleName(methodName));

		for(int i=0; i < fArgTypes.length; i++) {
			Expression	actualCtorArg= (Expression) actualCtorArgs.get(i);
			ASTNode		movedArg= unitRewriter.createMove(actualCtorArg);

			actualFactoryArgs.add(movedArg);
//			unitRewriter.createMove(actualCtorArg);
//			ASTNode		rewrittenArg= rewriteArgument(actualCtorArg);
//			actualFactoryArgs.add(rewrittenArg);
		}

		unitRewriter.markAsReplaced(ctorCall, factoryMethodCall, gd);

		return factoryMethodCall;
	}

	/**
	 * Returns true iff the given <code>ICompilationUnit</code> is the unit
	 * containing the original constructor.
	 * @param unit
	 */
	private boolean isConstructorUnit(ICompilationUnit unit) {
		return unit.equals(ASTCreator.getCu(fCtorOwningClass));
	}

	/**
	 * Returns true iff we should actually change the original constructor's
	 * visibility to <code>protected</code>. This takes into account the user-
	 * requested mode and whether the constructor's compilation unit is in
	 * source form.
	 */
	private boolean shouldProtectConstructor() {
		return fProtectConstructor && fCtorOwningClass != null;
	}

	static final int VISIBILITY_MASK= (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE);

	/**
	 * Creates and adds the necessary change to make the constructor method protected.
	 * Returns false iff the constructor didn't exist (i.e. was implicit)
	 * @param unit
	 * @param unitChange
	 * @param root
	 * @param buffer
	 */
	private boolean protectConstructor(CompilationUnit unitAST, ASTRewrite unitRewriter, TextEditGroup declGD) {
		MethodDeclaration constructor= (MethodDeclaration) unitAST.findDeclaringNode(fCtorBinding.getKey());

		// No need to rewrite the modifiers if the visibility is what we already want it to be.
		if (constructor == null || (constructor.getModifiers() & VISIBILITY_MASK) == fConstructorVisibility)
			return false;
		unitRewriter.set(
			constructor, MethodDeclaration.MODIFIERS_PROPERTY,
			new Integer(ASTNodes.changeVisibility(constructor.getModifiers(), fConstructorVisibility)),
			declGD);
		return true;
	}

	/**
	 * Add all changes necessary on the <code>ICompilationUnit</code> in the given
	 * <code>SearchResultGroup</code> to implement the refactoring transformation
	 * to the given <code>CompilationUnitChange</code>.
	 * @param rg the <code>SearchResultGroup</code> for which changes should be created
	 * @param unitChange the CompilationUnitChange object for the compilation unit in question
	 * @throws CoreException
	 */
	private boolean addAllChangesFor(SearchResultGroup rg, ICompilationUnit	unitHandle, CompilationUnitChange unitChange) throws CoreException {
//		ICompilationUnit	unitHandle= rg.getCompilationUnit();
		Assert.isTrue(rg == null || rg.getCompilationUnit() == unitHandle);
		CompilationUnit		unit= getASTFor(unitHandle);
		ASTRewrite			unitRewriter= new ASTRewrite(unit);
		TextBuffer			buffer= null;
		MultiTextEdit		root= new MultiTextEdit();
		boolean				someChange= false;

		try {
			unitChange.setEdit(root);
			buffer= TextBuffer.acquire((IFile) unitHandle.getResource());
			fImportRewriter= new ImportRewrite(unitHandle, fCodeGenSettings);

			// First create the factory method
			if (unitHandle.equals(fFactoryUnitHandle)) {
				TextEditGroup	factoryGD= new TextEditGroup(RefactoringCoreMessages.getString("IntroduceFactory.addFactoryMethod")); //$NON-NLS-1$

				createFactoryChange(unitRewriter, unit, factoryGD);
				unitChange.addTextEditGroup(factoryGD);
				someChange= true;
			}

			// Now rewrite all the constructor calls to use the factory method
			if (rg != null)
				if (replaceConstructorCalls(rg, unit, unitRewriter, unitChange))
					someChange= true;

			// Finally, make the constructor private, if requested.
			if (shouldProtectConstructor() && isConstructorUnit(unitHandle)) {
				TextEditGroup	declGD= new TextEditGroup(RefactoringCoreMessages.getString("IntroduceFactory.protectConstructor")); //$NON-NLS-1$

				if (protectConstructor(unit, unitRewriter, declGD)) {
					unitChange.addTextEditGroup(declGD);
					someChange= true;
				}
			}

			if (someChange) {
				unitRewriter.rewriteNode(buffer, root);
				root.addChild(fImportRewriter.createEdit(buffer.getDocument()));
			}
		} finally {
			if (unitRewriter != null)
				unitRewriter.removeModifications();
			if (buffer != null)
				TextBuffer.release(buffer);
		}
		return someChange;
	}

	/**
	 * Returns an AST for the given compilation unit handle.<br>
	 * If this is the unit containing the selection or the unit in which the factory
	 * is to reside, checks the appropriate field (<code>fCU</code> or <code>fFactoryCU</code>,
	 * respectively) and initializes the field with a new AST only if not already done.
	 */
	private CompilationUnit getASTFor(ICompilationUnit unitHandle) {
		if (unitHandle.equals(fCUHandle)) { // is this the unit containing the selection?
			if (fCU == null) {
				fCU= ASTCreator.createAST(unitHandle, null);
				if (fCU.equals(fFactoryUnitHandle)) // if selection unit and factory unit are the same...
					fFactoryCU= fCU; // ...make sure the factory unit gets initialized
			}
			return fCU;
		} else if (unitHandle.equals(fFactoryUnitHandle)) { // is this the "factory unit"?
			if (fFactoryCU == null)
				fFactoryCU= ASTCreator.createAST(unitHandle, null);
			return fFactoryCU;
		} else
			return ASTCreator.createAST(unitHandle, null);
	}

	/**
	 * Use the given <code>ASTRewrite</code> to replace direct calls to the constructor
	 * with calls to the newly-created factory method.
	 * @param rg the <code>SearchResultGroup</code> indicating all of the constructor references
	 * @param unit the <code>CompilationUnit</code> to be rewritten
	 * @param unitRewriter
	 * @param gd the <code>GroupDescription</code> to associate with the changes
	 * @throws JavaModelException
	 * @return true iff at least one constructor call site was rewritten.
	 */
	private boolean replaceConstructorCalls(SearchResultGroup rg, CompilationUnit unit,
											ASTRewrite unitRewriter, CompilationUnitChange unitChange)
	throws JavaModelException {
		Assert.isTrue(ASTCreator.getCu(unit).equals(rg.getCompilationUnit()));
		SearchResult[]	hits= rg.getSearchResults();
		AST	ctorCallAST= unit.getAST();
		boolean someCallPatched= false;

		for(int i=0; i < hits.length; i++) {
			ClassInstanceCreation	creation= getCtorCallAt(hits[i].getStart(), hits[i].getEnd(), unit);

			if (creation != null) {
				TextEditGroup gd= new TextEditGroup(RefactoringCoreMessages.getString("IntroduceFactory.replaceCalls")); //$NON-NLS-1$

				createFactoryMethodCall(ctorCallAST, creation, unitRewriter, gd);
				unitChange.addTextEditGroup(gd);
				someCallPatched= true;
			}
		}
		return someCallPatched;
	}

	/**
	 * Look "in the vicinity" of the given range to find the <code>ClassInstanceCreation</code>
	 * node that this search hit identified. Necessary because the <code>SearchEngine</code>
	 * doesn't always cough up text extents that <code>NodeFinder.perform()</code> agrees with.
	 * @param start
	 * @param end
	 * @param unitAST
	 * @return may return null if this is really a constructor->constructor call (e.g. "this(...)")
	 */
	private ClassInstanceCreation getCtorCallAt(int start, int end, CompilationUnit unitAST) throws JavaModelException {
		ICompilationUnit	unitHandle= ASTCreator.getCu(unitAST);
		int			length= end - start;
		ASTNode		node= NodeFinder.perform(unitAST, start, length);

		if (node == null)
			throw new JavaModelException(new JavaModelStatus(IStatus.ERROR,
				RefactoringCoreMessages.getFormattedString("IntroduceFactory.noASTNodeForConstructorSearchHit", //$NON-NLS-1$
					new Object[] {	Integer.toString(start), Integer.toString(end),
									unitHandle.getSource().substring(start, end),
									unitHandle.getElementName() })));

		if (node instanceof ClassInstanceCreation) {
			return (ClassInstanceCreation) node;
		} else if (node instanceof VariableDeclaration) {
			Expression	init= ((VariableDeclaration) node).getInitializer();

			if (init instanceof ClassInstanceCreation) {
				return (ClassInstanceCreation) init;
			} else if (init != null)
				throw new JavaModelException(new JavaModelStatus(IStatus.ERROR,
					RefactoringCoreMessages.getFormattedString("IntroduceFactory.unexpectedInitializerNodeType", //$NON-NLS-1$
										new Object[] { init.toString(), unitHandle.getElementName() })));
			else
				throw new JavaModelException(new JavaModelStatus(IStatus.ERROR,
					RefactoringCoreMessages.getFormattedString("IntroduceFactory.noConstructorCallNodeInsideFoundVarbleDecl", //$NON-NLS-1$
										new Object[] { node.toString() })));
		} else if (node instanceof ConstructorInvocation) {
			// This is a call we can bypass; it's from one constructor flavor
			// to another flavor on the same class.
			return null;
		} else if (node instanceof ExpressionStatement) {
			Expression	expr= ((ExpressionStatement) node).getExpression();

			if (expr instanceof ClassInstanceCreation)
				return (ClassInstanceCreation) expr;
			else
				throw new JavaModelException(new JavaModelStatus(IStatus.ERROR,
					RefactoringCoreMessages.getFormattedString("IntroduceFactory.unexpectedASTNodeTypeForConstructorSearchHit", //$NON-NLS-1$
						new Object[] { expr.toString(), unitHandle.getElementName() })));
		} else if (node instanceof SimpleName && node.getParent() instanceof MethodDeclaration) {
			// We seem to have been given a hit for an implicit call to the base-class constructor.
			// Do nothing with this (implicit) call, but have to make sure we make the derived class
			// doesn't lose access to the base-class constructor (so make it 'protected', not 'private').
			fConstructorVisibility= Modifier.PROTECTED;
			return null;
		} else
			throw new JavaModelException(new JavaModelStatus(IStatus.ERROR,
				RefactoringCoreMessages.getFormattedString("IntroduceFactory.unexpectedASTNodeTypeForConstructorSearchHit", //$NON-NLS-1$
					new Object[] {	node.getClass().getName() + "('" + node.toString() + "')", unitHandle.getElementName() }))); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Perform the AST rewriting necessary on the given <code>CompilationUnit</code>
	 * to create the factory method. The method will reside on the type identified by
	 * <code>fFactoryOwningClass</code>.
	 * @param unitRewriter
	 * @param unit
	 * @param gd the <code>GroupDescription</code> to associate with the changes made
	 */
	private void createFactoryChange(ASTRewrite unitRewriter, CompilationUnit unit, TextEditGroup gd) {
		// ================================================================================
		// First add the factory itself (method, class, and interface as needed/directed by user)
		AST				ast= unit.getAST();

		fFactoryMethod= createFactoryMethod(ast, fCtorBinding, unitRewriter);

		TypeDeclaration	factoryOwner= (TypeDeclaration) unit.findDeclaringNode(fFactoryOwningClass.resolveBinding().getKey());
		TypeDeclaration	methodOwner= factoryOwner;

		fImportRewriter.addImport(fCtorOwningClass.resolveBinding());

		List	methodDeclList= methodOwner.bodyDeclarations(); // where to put factory method

		int	idx= ASTNodes.getInsertionIndex(fFactoryMethod, methodDeclList);

		if (idx < 0) idx= 0; // Guard against bug in getInsertionIndex()
		methodDeclList.add(idx, fFactoryMethod);
		unitRewriter.markAsInserted(fFactoryMethod, gd);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		pm.beginTask(RefactoringCoreMessages.getString("IntroduceFactory.createChanges"), fAllCallsTo.length); //$NON-NLS-1$
		final ValidationStateChange result= new ValidationStateChange(RefactoringCoreMessages.getString("IntroduceFactory.topLevelChangeLabel") + fCtorBinding.getName()); //$NON-NLS-1$

		try {
			boolean hitInFactoryClass= false;
			boolean hitInCtorClass= false;
			for(int i=0; i < fAllCallsTo.length; i++) {
				SearchResultGroup		rg= fAllCallsTo[i];
				ICompilationUnit		unitHandle= rg.getCompilationUnit();
				CompilationUnitChange	cuChange= new CompilationUnitChange(getName(), unitHandle);

				if (addAllChangesFor(rg, unitHandle, cuChange))
					result.add(cuChange);

				if (unitHandle.equals(fFactoryUnitHandle))
					hitInFactoryClass= true;
				if (unitHandle.equals(ASTCreator.getCu(fCtorOwningClass)))
					hitInCtorClass= true;

				pm.worked(1);
			}
			if (!hitInFactoryClass) { // Handle factory class if no search hits there
				CompilationUnitChange cuChange= new CompilationUnitChange(getName(), fFactoryUnitHandle);
				addAllChangesFor(null, fFactoryUnitHandle, cuChange);
				result.add(cuChange);
			}
			if (!hitInCtorClass && !fFactoryUnitHandle.equals(ASTCreator.getCu(fCtorOwningClass))) { // Handle constructor-owning class if no search hits there
				CompilationUnitChange cuChange= new CompilationUnitChange(getName(), ASTCreator.getCu(fCtorOwningClass));
				addAllChangesFor(null, ASTCreator.getCu(fCtorOwningClass), cuChange);
				result.add(cuChange);
			}
			return result;
		} finally {
			pm.done();
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("IntroduceFactory.name");//$NON-NLS-1$
	}

	/**
	 * Returns the name to be used for the generated factory method.
	 */
	public String getNewMethodName() {
		return fNewMethodName;
	}

	/**
	 * Sets the name to be used for the generated factory method.<br>
	 * Returns a <code>RefactoringStatus</code> that indicates whether the
	 * given name is valid for the new factory method.
	 * @param newMethodName the name to be used for the generated factory method
	 */
	public RefactoringStatus setNewMethodName(String newMethodName) {
		Assert.isNotNull(newMethodName);
		fNewMethodName = newMethodName;

		RefactoringStatus	stat= Checks.checkMethodName(newMethodName);

		stat.merge(isUniqueMethodName(newMethodName));

		return stat;
	}

	/**
	 * Returns a <code>RefactoringStatus</code> that identifies whether the
	 * the name <code>newMethodName</code> is available to use as the name of
	 * the new factory method within the factory-owner class (either a to-be-
	 * created factory class or the constructor-owning class, depending on the
	 * user options).
	 * @param methodName
	 */
	private RefactoringStatus isUniqueMethodName(String methodName) {
		boolean	conflict= hasMethod(fFactoryOwningClass, methodName);

		return conflict ? RefactoringStatus.createErrorStatus(RefactoringCoreMessages.getString("IntroduceFactory.duplicateMethodName") + methodName) : new RefactoringStatus(); //$NON-NLS-1$
	}

	/**
	 * Returns true iff the given <code>TypeDeclaration</code> has a method with
	 * the given name.
	 * @param type
	 * @param name
	 */
	private boolean hasMethod(TypeDeclaration type, String name) {
		List	decls= type.bodyDeclarations();

		for (Iterator iter = decls.iterator(); iter.hasNext();) {
			BodyDeclaration decl = (BodyDeclaration) iter.next();
			if (decl instanceof MethodDeclaration) {
				if (((MethodDeclaration) decl).getName().getIdentifier().equals(name))
					return true;
			}
		}
		return false;
	}

	/**
	 * Returns true iff the selected constructor can be protected.
	 * @return
	 */
	public boolean canProtectConstructor() {
		return !fCtorBinding.isSynthetic() && fFactoryCU.findDeclaringNode(fCtorBinding.getKey()) != null;
	}

	/**
	 * If the argument is true, change the visibility of the constructor to
	 * <code>protected</code>, thereby encapsulating it.
	 * @param protectConstructor
	 */
	public void setProtectConstructor(boolean protectConstructor) {
		fProtectConstructor = protectConstructor;
	}

	/**
	 * Returns the project on behalf of which this refactoring was invoked.
	 */
	public IJavaProject getProject() {
		return fCUHandle.getJavaProject();
	}

	/**
	 * Sets the class on which the generated factory method is to be placed.
	 * @param fullyQualifiedTypeName an <code>IType</code> referring to an existing class
	 */
	public RefactoringStatus setFactoryClass(String fullyQualifiedTypeName) {
		IType factoryType;

		try {
			factoryType= getProject().findType(fullyQualifiedTypeName);

			if (factoryType == null)
				return RefactoringStatus.createErrorStatus(RefactoringCoreMessages.getFormattedString("IntroduceFactory.noSuchClass", fullyQualifiedTypeName)); //$NON-NLS-1$

			if (factoryType.isInterface())
				return RefactoringStatus.createErrorStatus(RefactoringCoreMessages.getString("IntroduceFactory.cantPutFactoryMethodOnInterface")); //$NON-NLS-1$
		} catch (JavaModelException e1) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("IntroduceFactory.cantCheckForInterface")); //$NON-NLS-1$
		}

		ICompilationUnit	factoryUnitHandle= factoryType.getCompilationUnit();

		if (factoryType.isBinary())
			return RefactoringStatus.createErrorStatus(RefactoringCoreMessages.getString("IntroduceFactory.cantPutFactoryInBinaryClass")); //$NON-NLS-1$
		else {
			try {
				if (!fFactoryUnitHandle.equals(factoryUnitHandle)) {
					fFactoryCU= getASTFor(factoryUnitHandle);
					fFactoryUnitHandle= factoryUnitHandle;
				}
				fFactoryOwningClass= ASTNodeSearchUtil.getTypeDeclarationNode(factoryType, fFactoryCU);

				String factoryPkg= factoryType.getPackageFragment().getElementName();
				String ctorPkg= fCtorOwningClass.resolveBinding().getPackage().getName();

				if (!factoryPkg.equals(ctorPkg))
					fConstructorVisibility= Modifier.PUBLIC;
				else if (fFactoryOwningClass != fCtorOwningClass)
					fConstructorVisibility= 0; // No such thing as Modifier.PACKAGE...


				if (fFactoryOwningClass != fCtorOwningClass)
					fConstructorVisibility= 0; // No such thing as Modifier.PACKAGE...

			} catch (JavaModelException e) {
				e.printStackTrace();
			}
			return new RefactoringStatus();
		}
	}

	/**
	 * Returns the name of the class on which the generated factory method is
	 * to be placed.
	 */
	public String getFactoryClassName() {
		return fFactoryOwningClass.resolveBinding().getQualifiedName();
	}
}
