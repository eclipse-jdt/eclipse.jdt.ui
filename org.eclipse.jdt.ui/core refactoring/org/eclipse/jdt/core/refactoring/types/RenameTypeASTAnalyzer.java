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
import org.eclipse.jdt.internal.core.refactoring.SearchResult;
import org.eclipse.jdt.internal.core.util.HackFinder;

/*
 * non java-doc
 * not API
 */
class RenameTypeASTAnalyzer extends AbstractSyntaxTreeVisitorAdapter {

	private List fSearchResults;
	private String fNewName;
	private char[] fNewNameArray;
	private IType fType;
	private RefactoringStatus fResult;
	private CompilationUnit fCu;

	RefactoringStatus analyze(List searchResults, String newName, ICompilationUnit cu, IType type) throws JavaModelException{
		Assert.isNotNull(searchResults, "searchResults");
		Assert.isNotNull(searchResults, "newName");
		Assert.isNotNull(type, "type");
		Assert.isTrue(type.exists());
		
		fNewNameArray= newName.toCharArray();
		fNewName= newName;
		fSearchResults= searchResults;
		fType= type;
		fResult= new RefactoringStatus();
		fCu= (CompilationUnit)cu;
		fCu.accept(this);
		return fResult;
	}

	public boolean visit(MessageSend messageSend, BlockScope scope) {
		if (messageSend.receiver != ThisReference.ThisImplicit && sourceRangeOnList(messageSend.receiver) && nameDefinedInScope(fNewName, scope)) {
			addWarning ("Name " + fNewName + " is already used in scope (in " + cuFullPath() + ")");
		}
		return true;
	}

	public boolean visit(QualifiedNameReference qualifiedNameReference, BlockScope scope) {
		if (sourceRangeOnList(qualifiedNameReference) && nameDefinedInScope(fNewName, scope)) {
			addWarning("Name " + fNewName + " is already used in scope (in " + cuFullPath() + ")");
		}
		return true;
	}

	public boolean visit(CastExpression castExpression, BlockScope scope) {
		if (isNewNameHiddenByAnotherType(castExpression.type, scope))
			addWarning("Name " + fNewName + " is already used in for a local type (in " + cuFullPath() + "), which affects a cast expression");
		return true;
	}

	public boolean visit(ClassLiteralAccess classLiteral, BlockScope scope) {
		if (isNewNameHiddenByAnotherType(classLiteral.type, scope))
			addWarning("Name " + fNewName + " is already used in for a local type (in " + cuFullPath() + "), which affects a class literal");
		return true;
	}

	public boolean visit(InstanceOfExpression instanceOfExpression, BlockScope scope) {
		if (isNewNameHiddenByAnotherType(instanceOfExpression.type, scope))
			addWarning("Name " + fNewName + " is already used in for a local type (in " + cuFullPath() + "), which affects an instanceof expression");
		return true;
	}

	public boolean visit(ArrayAllocationExpression arrayAllocationExpression, BlockScope scope) {
		if (isNewNameHiddenByAnotherType(arrayAllocationExpression.type, scope))
			addWarning("Name " + fNewName + " is already used in for a local type (in " + cuFullPath() + "), which affects an array allocation expression");
		return true;
	}

	public boolean visit(CompilationUnitDeclaration compilationUnitDeclaration, CompilationUnitScope scope) {
		if (typeImported(compilationUnitDeclaration, fNewName))
			addWarning(fNewName + " causes a name conflict in import declarations in " + cuFullPath());
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
			addWarning(fType.getFullyQualifiedName() + " is used as a parameter type for a native method " + new String(methodDeclaration.selector) + " in " + cuFullPath()); 
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
			addWarning(cuFullPath() + " refers to a type named " + fNewName + " and imports (single-type-import) " + fType.getFullyQualifiedName());
		if (isNewNameHiddenByAnotherType(singleTypeReference, scope))
			addWarning("Name " + fNewName + " is already used in for a local type (in " + cuFullPath() + "), which affects a type reference");
		return true;
	}

	public boolean visit(SingleTypeReference singleTypeReference, ClassScope scope) {
		if (referenceConflictsWithImport(singleTypeReference.token, scope))
			addWarning(cuFullPath() + " refers to a type named " + fNewName + " and imports (single-type-import) " + fType.getFullyQualifiedName());
		if (isNewNameHiddenByAnotherType(singleTypeReference, scope))
			addWarning("Name " + fNewName + " is already used in for a local type (in " + cuFullPath() + "), which affects a type reference");
		return true;
	}

	public boolean visit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
		if (referenceConflictsWithImport(arrayTypeReference.token, scope))
			addWarning(cuFullPath() + " refers to a type named " + fNewName + " and imports (single-type-import) " + fType.getFullyQualifiedName());
		if (sourceRangeOnList(arrayTypeReference.sourceStart, arrayTypeReference.sourceStart + arrayTypeReference.token.length) && localTypeExists(scope, fNewNameArray)) {
			addWarning("Name " + fNewName + " is already used in for a local type (in " + cuFullPath() + "), which affects a type reference");
		}
		return true;
	}

	public boolean visit(ArrayTypeReference arrayTypeReference, ClassScope scope) {
		if (referenceConflictsWithImport(arrayTypeReference.token, scope))
			addWarning(cuFullPath() + " refers to a type named " + fNewName + " and imports (single-type-import) " + fType.getFullyQualifiedName());
		return true;
	}
	//-----------------------------------------------------------
	
	private void addWarning(String msg){
		fResult.addError(msg);
	}
	
	private void addError(String msg){
		fResult.addFatalError(msg);
	}
	
	private String cuFullPath(){
		return RenameTypeRefactoring.getFullPath(fCu);
	}
	
	private boolean isNewNameHiddenByAnotherType(AstNode node, Scope scope) {
		if (! sourceRangeOnList(node)) 
			return false;
		return (localTypeExists(scope, fNewNameArray) || typeExists(scope, fNewNameArray));
	}

	private boolean referenceConflictsWithImport(char[] typeName, Scope scope) {
		return fNewName.equals(new String(typeName)) && cuImportsType(scope, fType);
	}
	
	private boolean sourceRangeOnList(int start, int end){
		//	DebugUtils.dump("start:" + start + " end:" + end);
		Iterator iter= fSearchResults.iterator();
		while (iter.hasNext()){
			SearchResult searchResult= (SearchResult)iter.next();
			//	DebugUtils.dump("[" + searchResult.getStart() + ", " + searchResult.getEnd() + "]");
			if (start == searchResult.getStart() && end == searchResult.getEnd())
				return true;
		}
		return false;
	}
	
	private boolean sourceRangeOnList(AstNode astNode){
		return sourceRangeOnList(astNode.sourceStart, astNode.sourceEnd + 1);
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
				fResult.addError(fType + " has a subclass named " + fNewName);
			}
			if (fType.isInterface()
				&& typeDeclaration.superInterfaces != null
				&& singleTypeReferenceInRange(typeDeclaration.superInterfaces)
				&& nameInConflictWithSuperInterfaces(typeDeclaration, fNewName)) {
				fResult.addError("Name " + fNewName + " is in conflict with superinterfaces list for type " + new String(typeDeclaration.name));
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
	
	private boolean sourceRangeOnList(QualifiedNameReference qualifiedNameReference){
		int start= qualifiedNameReference.sourceStart;
		return sourceRangeOnList(start, start + qualifiedNameReference.tokens[0].length);
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
