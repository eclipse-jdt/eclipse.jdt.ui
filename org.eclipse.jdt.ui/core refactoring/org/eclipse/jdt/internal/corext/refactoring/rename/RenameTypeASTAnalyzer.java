/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.rename;


import java.util.Iterator;
import java.util.List;


import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;


import org.eclipse.jdt.internal.compiler.AbstractSyntaxTreeVisitorAdapter;
import org.eclipse.jdt.internal.compiler.ast.AnonymousLocalTypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ArrayAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.CastExpression;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.InstanceOfExpression;
import org.eclipse.jdt.internal.compiler.ast.LocalTypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MemberTypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.util.CharOperation;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;

/*
 * non java-doc
 * not API
 */
class RenameTypeASTAnalyzer extends RenameRefactoringASTAnalyzer {


	private String fNewName;
	private char[] fNewNameArray;
	private IType fType;
	
	RenameTypeASTAnalyzer(String newName, IType type){
		Assert.isNotNull(newName, "newName"); //$NON-NLS-1$
		Assert.isTrue(type.exists());
		fNewNameArray= newName.toCharArray();
		fNewName= newName;
		fType= type;
	}


	public boolean visit(MessageSend messageSend, BlockScope scope) {
		if (messageSend.receiver != ThisReference.ThisImplicit && sourceRangeOnList(messageSend.receiver) && nameDefinedInScope(fNewNameArray, scope)) {
			addError(messageSend);
		}
		return true;
	}


	public boolean visit(QualifiedNameReference qualifiedNameReference, BlockScope scope) {
		if (sourceRangeOnList(qualifiedNameReference) && nameDefinedInScope(fNewNameArray, scope)) {
			addError(qualifiedNameReference);
		}
		return true;
	}


	public boolean visit(CastExpression castExpression, BlockScope scope) {
		if (isNewNameHiddenByAnotherType(castExpression.type, scope))
			addError(castExpression);
		return true;
	}


	public boolean visit(ClassLiteralAccess classLiteral, BlockScope scope) {
		if (isNewNameHiddenByAnotherType(classLiteral.type, scope))
			addError(classLiteral);
		return true;
	}


	public boolean visit(InstanceOfExpression instanceOfExpression, BlockScope scope) {
		if (isNewNameHiddenByAnotherType(instanceOfExpression.type, scope))
			addError(instanceOfExpression);
		return true;
	}


	public boolean visit(ArrayAllocationExpression arrayAllocationExpression, BlockScope scope) {
		if (isNewNameHiddenByAnotherType(arrayAllocationExpression.type, scope))
			addError(arrayAllocationExpression);
		return true;
	}


	public boolean doVisit(CompilationUnitDeclaration compilationUnitDeclaration, CompilationUnitScope scope) {
		ImportReference imp= typeImported(compilationUnitDeclaration, fNewNameArray);
		if (imp != null)
			addError(RefactoringCoreMessages.getFormattedString("RenameTypeASTAnalyzer.import_conflict",  //$NON-NLS-1$
																new Object[]{fNewName, cuFullPath()}),
							imp.sourceStart, imp.sourceEnd);
	
		if (!typeImported(compilationUnitDeclaration))
			return true;
		
		TypeDeclaration type= typeDeclared(compilationUnitDeclaration, fNewNameArray);
		if (type != null)
			addError(RefactoringCoreMessages.getFormattedString("RenameTypeASTAnalyzer.conflict_with_declared_type",  //$NON-NLS-1$
																new String[]{cuFullPath(), JavaModelUtil.getFullyQualifiedName(fType), fNewName}),
							type.sourceStart, type.sourceEnd);
		return true;
	}


	public boolean visit(MemberTypeDeclaration memberTypeDeclaration, ClassScope scope) {
		analyzeTypeDeclaration(memberTypeDeclaration);
		return true;
	}


	public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
		if (methodDeclaration.isNative() && typeUsedAsParameter(methodDeclaration))
			addWarning(RefactoringCoreMessages.getFormattedString("RenameTypeASTAnalyzer.native_param",  //$NON-NLS-1$
																	new Object[]{JavaModelUtil.getFullyQualifiedName(fType), 
																				new String(methodDeclaration.selector),
																				cuFullPath(),
																				new Integer(getLineNumber(methodDeclaration))}),
									methodDeclaration.sourceStart, methodDeclaration.sourceEnd);
		return true;
	}

	public boolean visit(LocalTypeDeclaration typeDeclaration, BlockScope scope){
		analyzeTypeDeclaration(typeDeclaration);
		return true;
	} 

	public boolean visit(AnonymousLocalTypeDeclaration anonymousTypeDeclaration, BlockScope scope){
		analyzeTypeDeclaration(anonymousTypeDeclaration);
		return true;
	} 
	
	public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
		analyzeTypeDeclaration(typeDeclaration);
		return true;
	}

	public boolean visit(SingleTypeReference singleTypeReference, BlockScope scope) {
		if (referenceConflictsWithImport(singleTypeReference.token, scope))
			addError(RefactoringCoreMessages.getFormattedString("RenameTypeASTAnalyzer.refers_and_imports",  //$NON-NLS-1$
																new Object[]{cuFullPath(), 
																			fNewName,
																			new Integer(getLineNumber(singleTypeReference)),
																			JavaModelUtil.getFullyQualifiedName(fType)}),
								singleTypeReference.sourceStart, singleTypeReference.sourceEnd);
		if (isNewNameHiddenByAnotherType(singleTypeReference, scope))
			addError(singleTypeReference);
		return true;
	}


	public boolean visit(SingleTypeReference singleTypeReference, ClassScope scope) {
		if (referenceConflictsWithImport(singleTypeReference.token, scope))
			addError(RefactoringCoreMessages.getFormattedString("RenameTypeASTAnalyzer.refers_and_imports",  //$NON-NLS-1$
																new Object[]{cuFullPath(), 
																			fNewName,
																			new Integer(getLineNumber(singleTypeReference)),
																			JavaModelUtil.getFullyQualifiedName(fType)}),
							singleTypeReference.sourceStart, singleTypeReference.sourceEnd);
			
		if (isNewNameHiddenByAnotherType(singleTypeReference, scope))
			addError(singleTypeReference);
		return true;
	}


	public boolean visit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
		if (referenceConflictsWithImport(arrayTypeReference.token, scope))
			addError(RefactoringCoreMessages.getFormattedString("RenameTypeASTAnalyzer.refers_and_imports",  //$NON-NLS-1$
																new Object[]{cuFullPath(), 
																			fNewName,
																			new Integer(getLineNumber(arrayTypeReference)),
																			JavaModelUtil.getFullyQualifiedName(fType)}),
							arrayTypeReference.sourceStart, arrayTypeReference.sourceEnd);
			
		if (sourceRangeOnList(arrayTypeReference.sourceStart, arrayTypeReference.sourceStart + arrayTypeReference.token.length) && localTypeExists(scope, fNewNameArray)) {
			addError(arrayTypeReference);
		}
		return true;
	}


	public boolean visit(ArrayTypeReference arrayTypeReference, ClassScope scope) {
		if (referenceConflictsWithImport(arrayTypeReference.token, scope))
			addError(RefactoringCoreMessages.getFormattedString("RenameTypeASTAnalyzer.refers_and_imports",  //$NON-NLS-1$
																new Object[]{cuFullPath(), 
																			fNewName,
																			new Integer(getLineNumber(arrayTypeReference)),
																			JavaModelUtil.getFullyQualifiedName(fType)}),
							arrayTypeReference.sourceStart, arrayTypeReference.sourceEnd);
			
		return true;
	}
	//-----------------------------------------------------------
	private void addError(AstNode node){
		addError(RefactoringCoreMessages.getFormattedString("RenameTypeASTAnalyzer.name_visible",  //$NON-NLS-1$
															new Object[]{fNewName, 									cuFullPath(),
																		new Integer(getLineNumber(node))}), node.sourceStart, node.sourceEnd);
	}
	
	private boolean isNewNameHiddenByAnotherType(AstNode node, Scope scope) {
		if (! sourceRangeOnList(node)) 
			return false;
		return (localTypeExists(scope, fNewNameArray) || typeExists(scope, fNewNameArray));
	}


	private boolean referenceConflictsWithImport(char[] typeName, Scope scope) {
		return CharOperation.equals(fNewNameArray, typeName) && cuImportsType(scope, fType);
	}
	
	private static final boolean typeExists(Scope scope, char[] name){
		/*
		 * should maybe do sth with unresolved bindings
		 */
		TypeBinding binding= scope.getType(name);
		return (binding != null && !(binding instanceof ProblemReferenceBinding));
	}
	
	private static final boolean localTypeExists(Scope scope, char[] name){
		Scope current= scope;
		while ((current != null) && ! (current instanceof CompilationUnitScope)){
			if ((current instanceof BlockScope) && ((BlockScope)current).findLocalType(name) != null)
				return true;
			current= current.parent;	
		}
		return false;
	}
	
	private static boolean cuImportsType(Scope scope, IType type){
		Scope current= scope;
		while (current != null && !(current instanceof CompilationUnitScope))
			current= current.parent;
		if (current == null)
			return false; //XXX: Revisit
		return typeImported(((CompilationUnitScope)current).referenceContext, type) != null;
	}
	
	private static ImportReference typeImported(CompilationUnitDeclaration compilationUnitDeclaration, char[] simpleTypeName) {
		if (compilationUnitDeclaration.imports == null)
			return null;
		for (int i= 0; i < compilationUnitDeclaration.imports.length; i++){
			if (! compilationUnitDeclaration.imports[i].onDemand){
				char[][] tokens= compilationUnitDeclaration.imports[i].tokens;
				if (CharOperation.equals(tokens[tokens.length -1], simpleTypeName))
					return compilationUnitDeclaration.imports[i];
			}	
		}
		return null;
	}
	
	private static ImportReference typeImported(CompilationUnitDeclaration compilationUnitDeclaration, IType type) {
		if (compilationUnitDeclaration.imports == null)
			return null;
		String fullName= JavaModelUtil.getFullyQualifiedName(type);	
		for (int i= 0; i < compilationUnitDeclaration.imports.length; i++){
			if (! compilationUnitDeclaration.imports[i].onDemand
				&& compilationUnitDeclaration.imports[i].toString().equals(fullName))
				return compilationUnitDeclaration.imports[i];
		}
		return null;
	}
	
	private boolean typeImported(CompilationUnitDeclaration compilationUnitDeclaration) {
		if (compilationUnitDeclaration.imports == null)
			return false;
		for (int i= 0; i < compilationUnitDeclaration.imports.length; i++){
			if (! compilationUnitDeclaration.imports[i].onDemand
				&& sourceRangeOnList(compilationUnitDeclaration.imports[i]))
				return true;
		}
		return false;
	}
	
	/*
	 * overriden
	 */
	protected boolean nameDefinedInScope(char[] newName, CompilationUnitScope scope){
		return false;
	}
	
	private void analyzeTypeDeclaration(TypeDeclaration typeDeclaration) {
		if (!fType.exists()) //to avoid the exception
			return ;
		try {
			if (fType.isClass()
				&& typeDeclaration.superclass != null
				&& sourceRangeOnList(typeDeclaration.superclass)
				&& CharOperation.equals(typeDeclaration.name, fNewNameArray)
				&& (typeDeclaration.superclass instanceof SingleTypeReference)) {
				addError(RefactoringCoreMessages.getFormattedString("RenameTypeASTAnalyzer.subclass",  //$NON-NLS-1$
																new String[]{fType.getElementName(), fNewName}),
								typeDeclaration.sourceStart, typeDeclaration.sourceEnd);
			}
			if (fType.isInterface()
				&& typeDeclaration.superInterfaces != null
				&& singleTypeReferenceInRange(typeDeclaration.superInterfaces)
				&& nameInConflictWithSuperInterfaces(typeDeclaration, fNewNameArray)) {
				addError(RefactoringCoreMessages.getFormattedString("RenameTypeASTAnalyzer.superinterface_conflict", //$NON-NLS-1$
																	new String[]{fNewName, new String(typeDeclaration.name)}),
								typeDeclaration.sourceStart, typeDeclaration.sourceEnd);
			}
		} catch (JavaModelException e) {
			//ignore it
		}
	}
	
	/*
	 * returns true if any reference is included on the list
	 * checks if it's a single type reference
	 */
	private boolean singleTypeReferenceInRange(TypeReference[] typeReference){
		for (int i= 0; i < typeReference.length; i++){
			if (sourceRangeOnList(typeReference[i])){
				if (typeReference[i] instanceof SingleTypeReference)
					return true;
				else
					return false;	
			}
		}
		return false;
	}
	private static boolean nameInConflictWithSuperInterfaces(TypeDeclaration typeDeclaration, char[] name){
		for (int i= 0; i < typeDeclaration.superInterfaces.length; i ++){
			if (typeDeclaration.superInterfaces[i] instanceof SingleTypeReference
				&& new String(name).equals(typeDeclaration.superInterfaces[i].toStringExpression(0)))
					return true;
		}
		return false;
	}
		
	private static TypeDeclaration typeDeclared(CompilationUnitDeclaration compilationUnitDeclaration, char[] typeName){
		TypeDeclaration[] types= compilationUnitDeclaration.types;
		if (types == null)
			return null;
		for (int i= 0; i < types.length; i++){
			if (CharOperation.equals(types[i].name, typeName))
				return types[i];
		}
		return null;
	}
	
	private boolean typeUsedAsParameter(MethodDeclaration methodDeclaration){
		if (methodDeclaration.arguments == null)
			return false;
		for (int i= 0; i < methodDeclaration.arguments.length; i ++){
			if (sourceRangeOnList(methodDeclaration.arguments[i].type))
				return true;
		}
		return false;
	}
}
