/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.core.refactoring.types;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.internal.compiler.AbstractSyntaxTreeVisitorAdapter;
import org.eclipse.jdt.internal.compiler.ast.ArrayAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.CastExpression;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
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
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.RefactoringASTAnalyzer;
import org.eclipse.jdt.internal.core.refactoring.SearchResult;
import org.eclipse.jdt.internal.core.util.HackFinder;

/*
 * non java-doc
 * not API
 */
class RenameTypeASTAnalyzer extends RefactoringASTAnalyzer {

	private String fNewName;
	private char[] fNewNameArray;
	private IType fType;
	
	RenameTypeASTAnalyzer(String newName, IType type){
		Assert.isNotNull(newName, "newName");
		Assert.isTrue(type.exists());
		fNewNameArray= newName.toCharArray();
		fNewName= newName;
		fType= type;
	}

	public boolean visit(MessageSend messageSend, BlockScope scope) {
		if (messageSend.receiver != ThisReference.ThisImplicit && sourceRangeOnList(messageSend.receiver) && nameDefinedInScope(fNewName, scope)) {
			addWarning(messageSend);
		}
		return true;
	}

	public boolean visit(QualifiedNameReference qualifiedNameReference, BlockScope scope) {
		if (sourceRangeOnList(qualifiedNameReference) && nameDefinedInScope(fNewName, scope)) {
			addWarning(qualifiedNameReference);
		}
		return true;
	}

	public boolean visit(CastExpression castExpression, BlockScope scope) {
		if (isNewNameHiddenByAnotherType(castExpression.type, scope))
			addWarning(castExpression);
		return true;
	}

	public boolean visit(ClassLiteralAccess classLiteral, BlockScope scope) {
		if (isNewNameHiddenByAnotherType(classLiteral.type, scope))
			addWarning(classLiteral);
		return true;
	}

	public boolean visit(InstanceOfExpression instanceOfExpression, BlockScope scope) {
		if (isNewNameHiddenByAnotherType(instanceOfExpression.type, scope))
			addWarning(instanceOfExpression);
		return true;
	}

	public boolean visit(ArrayAllocationExpression arrayAllocationExpression, BlockScope scope) {
		if (isNewNameHiddenByAnotherType(arrayAllocationExpression.type, scope))
			addWarning(arrayAllocationExpression);
		return true;
	}

	public boolean doVisit(CompilationUnitDeclaration compilationUnitDeclaration, CompilationUnitScope scope) {
		if (typeImported(compilationUnitDeclaration, fNewName))
			addWarning(fNewName + " causes a name conflict in import declarations in \"" + cuFullPath() + "\"");
		if (!typeImported(compilationUnitDeclaration))
			return true;	
		if (typeDeclared(compilationUnitDeclaration, fNewName))
			addWarning(cuFullPath() + " imports " + fType.getFullyQualifiedName() + " and declares a top-level type called " + fNewName);
		return true;
	}

	public boolean visit(MemberTypeDeclaration memberTypeDeclaration, ClassScope scope) {
		analyzeTypeDeclaration(memberTypeDeclaration);
		return true;
	}

	public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
		if (methodDeclaration.isNative() && typeUsedAsParameter(methodDeclaration))
			addWarning(fType.getFullyQualifiedName() + " is used as a parameter type for a native method " 
					+ new String(methodDeclaration.selector) + " in \"" + cuFullPath() 
					+ "\" (line number:"+ getLineNumber(methodDeclaration)+")"); 
		return true;
	}

	public boolean visit(TypeDeclaration typeDeclaration, BlockScope scope) {
		analyzeTypeDeclaration(typeDeclaration);
		return true;
	}

	public boolean visit(TypeDeclaration typeDeclaration, ClassScope scope) {
		analyzeTypeDeclaration(typeDeclaration);
		return true;
	}

	public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
		analyzeTypeDeclaration(typeDeclaration);
		return true;
	}

	public boolean visit(LocalTypeDeclaration localTypeDeclaration, MethodScope scope) {
		analyzeTypeDeclaration(localTypeDeclaration);
		return true;
	}

	public boolean visit(SingleTypeReference singleTypeReference, BlockScope scope) {
		if (referenceConflictsWithImport(singleTypeReference.token, scope))
			addWarning(cuFullPath() + " refers to a type named " + fNewName + " (line number:" + getLineNumber(singleTypeReference) +") and imports (single-type-import) " + fType.getFullyQualifiedName());
		if (isNewNameHiddenByAnotherType(singleTypeReference, scope))
			addWarning(singleTypeReference);
		return true;
	}

	public boolean visit(SingleTypeReference singleTypeReference, ClassScope scope) {
		if (referenceConflictsWithImport(singleTypeReference.token, scope))
			addWarning(cuFullPath() + " refers to a type named " + fNewName + "(line number:" + getLineNumber(singleTypeReference) + ") and imports (single-type-import) " + fType.getFullyQualifiedName());
		if (isNewNameHiddenByAnotherType(singleTypeReference, scope))
			addWarning(singleTypeReference);
		return true;
	}

	public boolean visit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
		if (referenceConflictsWithImport(arrayTypeReference.token, scope))
			addWarning(cuFullPath() + " refers to a type named " + fNewName+ " (line number:" + getLineNumber(arrayTypeReference) + ") and imports (single-type-import) " + fType.getFullyQualifiedName());
		if (sourceRangeOnList(arrayTypeReference.sourceStart, arrayTypeReference.sourceStart + arrayTypeReference.token.length) && localTypeExists(scope, fNewNameArray)) {
			addWarning(arrayTypeReference);
		}
		return true;
	}

	public boolean visit(ArrayTypeReference arrayTypeReference, ClassScope scope) {
		if (referenceConflictsWithImport(arrayTypeReference.token, scope))
			addWarning(cuFullPath() + " refers to a type named " + fNewName + " (line number:" + getLineNumber(arrayTypeReference) + ") and imports (single-type-import) " + fType.getFullyQualifiedName());
		return true;
	}
	//-----------------------------------------------------------
	private void addWarning(AstNode node){
		addWarning("Possible problems in \"" + cuFullPath() + "\" (line number: " + getLineNumber(node) + "). Name " + fNewName + " is already used.");
	}
	
	private boolean isNewNameHiddenByAnotherType(AstNode node, Scope scope) {
		if (! sourceRangeOnList(node)) 
			return false;
		return (localTypeExists(scope, fNewNameArray) || typeExists(scope, fNewNameArray));
	}

	private boolean referenceConflictsWithImport(char[] typeName, Scope scope) {
		return fNewName.equals(new String(typeName)) && cuImportsType(scope, fType);
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
			return false; //???
		return typeImported(((CompilationUnitScope)current).referenceContext, type);
	}
	
	private static boolean typeImported(CompilationUnitDeclaration compilationUnitDeclaration, String simpleTypeName) {
		if (compilationUnitDeclaration.imports == null)
			return false;
		for (int i= 0; i < compilationUnitDeclaration.imports.length; i++){
			if (! compilationUnitDeclaration.imports[i].onDemand){
				char[][] tokens= compilationUnitDeclaration.imports[i].tokens;
				if (new String(tokens[tokens.length -1]).equals(simpleTypeName))
					return true;
			}	
		}
		return false;
	}
	
	private static boolean typeImported(CompilationUnitDeclaration compilationUnitDeclaration, IType type) {
		if (compilationUnitDeclaration.imports == null)
			return false;
		String fullName= type.getFullyQualifiedName();	
		for (int i= 0; i < compilationUnitDeclaration.imports.length; i++){
			if (! compilationUnitDeclaration.imports[i].onDemand
				&& compilationUnitDeclaration.imports[i].toString().equals(fullName))
				return true;
		}
		return false;
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
	
	private static boolean nameDefinedInScope(String newName, ClassScope scope){
		if (scope == null)
			return false;
		if (nameDefinedInType(newName, scope.referenceContext.binding))
			return true;
		if (scope.parent instanceof ClassScope)	
			return nameDefinedInScope(newName, (ClassScope)scope.parent);
		else if (scope.parent instanceof BlockScope)	
			return nameDefinedInScope(newName, (BlockScope)scope.parent);
		else
			return false;
	}
	
	private static boolean nameDefinedInScope(String newName, BlockScope scope){
		if (scope == null)
			return false;
		if (scope.locals != null){
			for (int i= 0; i < scope.locals.length; i++){
				if (scope.locals[i] != null){
					String name= (scope.locals[i].name != null) ? new String(scope.locals[i].name) : "";
					if (new String(name).equals(newName))
						return true;
				}	
			}
		}
		if (scope.parent instanceof BlockScope)
			return nameDefinedInScope(newName, (BlockScope)scope.parent);
		else if (scope.parent instanceof ClassScope)
			return nameDefinedInScope(newName, (ClassScope)scope.parent);
		else 
			return false;
	}
	
	private void analyzeTypeDeclaration(TypeDeclaration typeDeclaration) {
		if (!fType.exists()) //to avoid the exception
			return ;
		try {
			if (fType.isClass()
				&& typeDeclaration.superclass != null
				&& sourceRangeOnList(typeDeclaration.superclass)
				&& new String(typeDeclaration.name).equals(fNewName)
				&& (typeDeclaration.superclass instanceof SingleTypeReference)) {
				addWarning(fType + " has a subclass named " + fNewName);
			}
			if (fType.isInterface()
				&& typeDeclaration.superInterfaces != null
				&& singleTypeReferenceInRange(typeDeclaration.superInterfaces)
				&& nameInConflictWithSuperInterfaces(typeDeclaration, fNewName)) {
				addWarning("Name " + fNewName + " is in conflict with superinterfaces list for type " + new String(typeDeclaration.name));
			}
		} catch (JavaModelException e) {
			HackFinder.fixMeSoon("should we log it or ignore it?");
			//ExceptionHandler.handle(e, "JavaModelException", "Exception during program analysis for refactoring");
		}
	}
	
	private static boolean nameDefinedInType(String newName, SourceTypeBinding sourceTypeBinding){
		if (sourceTypeBinding.fields != null){
			for (int i= 0; i < sourceTypeBinding.fields.length; i++){
				if (newName.equals(new String(sourceTypeBinding.fields[i].name)))
					return true;
			}
		}
		if (sourceTypeBinding.memberTypes != null){
			for (int i= 0; i < sourceTypeBinding.memberTypes.length; i++){
				if (newName.equals(new String(sourceTypeBinding.memberTypes[i].sourceName)))
					return true;
			}
		}
		
		if ((sourceTypeBinding.superclass != null)
			&& (sourceTypeBinding.superclass instanceof SourceTypeBinding)
			&& nameDefinedInType(newName, (SourceTypeBinding)sourceTypeBinding.superclass))
				return true;
				
		if (sourceTypeBinding.superInterfaces != null){
			for (int i= 0; i < sourceTypeBinding.superInterfaces.length; i++){
				if ((sourceTypeBinding.superInterfaces[i] instanceof SourceTypeBinding)
					&& nameDefinedInType(newName, (SourceTypeBinding)sourceTypeBinding.superInterfaces[i]))
						return true;
			}
		}
		
		return false;
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
	private static boolean nameInConflictWithSuperInterfaces(TypeDeclaration typeDeclaration, String name){
		for (int i= 0; i < typeDeclaration.superInterfaces.length; i ++){
			if (typeDeclaration.superInterfaces[i] instanceof SingleTypeReference
				&& new String(typeDeclaration.superInterfaces[i].toStringExpression(0)).equals(name))
					return true;
		}
		return false;
	}
		
	private static boolean typeDeclared(CompilationUnitDeclaration compilationUnitDeclaration, String typeName){
		TypeDeclaration[] types= compilationUnitDeclaration.types;
		if (types == null)
			return false;
		for (int i= 0; i < types.length; i++){
			if (new String(types[i].name).equals(typeName))
				return true;
		}
		return false;
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
