/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.core.refactoring.methods;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.Refactoring;
import org.eclipse.jdt.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.internal.compiler.AbstractSyntaxTreeVisitorAdapter;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.util.CharOperation;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.RefactoringASTAnalyzer;
import org.eclipse.jdt.internal.core.refactoring.SearchResult;
import org.eclipse.jdt.internal.core.util.HackFinder;

/*
 * non java-doc
 * not API
 */
class RenameMethodASTAnalyzer extends RefactoringASTAnalyzer {

	private String fNewName;
	private char[] fNewNameArray;
	private IMethod fMethod;
	private int fParamCount;
	
	RenameMethodASTAnalyzer(String newName, IMethod method) throws JavaModelException {
		Assert.isTrue(method.exists());

		fNewNameArray= newName.toCharArray();
		fNewName= newName;
		fMethod= method;
		fParamCount= method.getParameterNames().length;
	}

	public boolean visit(MessageSend messageSend, BlockScope scope) {
		int start= (int) (messageSend.nameSourcePosition >>> 32);
		int end= 1 + messageSend.sourceEnd; //?? why 1 ?
		if (! sourceRangeOnList(start, end))
			return true;
		if (nameDefinedInScope(scope))
			addError("Possible problems in \"" + cuFullPath() + "\" (line number:" + getLineNumber(messageSend) + "). Name " + fNewName + " is already visible.");
		return true;
	}

	//-----------------------------------------------------------

	private boolean nameDefinedInTypes(MethodBinding[] methods) {
		if (methods != null) {
			for (int i= 0; i < methods.length; i++) {
				HackFinder.fixMeSoon("should perform real visibility analysis");
				if (methods[i].isPrivate())
					continue;
				int methodParamCount= methods[i].parameters == null ? 0 : methods[i].parameters.length;
				if ((fParamCount == methodParamCount) 
				&& CharOperation.equals(fNewNameArray, methods[i].selector))
					return true;
			}
		}
		return false;
	}

	private boolean nameDefinedInTypes(TypeBinding binding) {
		HackFinder.fixMeSoon("other types of bindings?");
		if (binding instanceof SourceTypeBinding) {
			SourceTypeBinding sourceBinding= (SourceTypeBinding) binding;
			if (nameDefinedInTypes(sourceBinding.methods()))
				return true;
			if (sourceBinding.superclass() != null && nameDefinedInTypes(sourceBinding.superclass()))
				return true;
		} else
			if (binding instanceof BinaryTypeBinding) {
				HackFinder.fixMeSoon("is BinaryTypeBinding ok?");
				BinaryTypeBinding binaryBinding= (BinaryTypeBinding) binding;
				if (nameDefinedInTypes(binaryBinding.methods()))
					return true;
				if (binaryBinding.superclass() != null && nameDefinedInTypes(binaryBinding.superclass()))
					return true;
			}
		return false;
	}

	private boolean nameDefinedInType(TypeDeclaration typeDeclaration) {
		if (typeDeclaration == null || typeDeclaration.methods == null)
			return false;
		for (int i= 0; i < typeDeclaration.methods.length; i++) {
			AbstractMethodDeclaration method= typeDeclaration.methods[i];
			int methodParamCount= method.arguments == null ? 0 : method.arguments.length;
			if ((fParamCount == methodParamCount) 
				&& CharOperation.equals(fNewNameArray, method.selector)){
				return true;
			}
		}
		HackFinder.fixMeSoon("should check access modifiers");
		if (typeDeclaration.superclass != null && nameDefinedInTypes(typeDeclaration.superclass.binding))
			return true;
		return false;
	}

	private boolean nameDefinedInScope(ClassScope classScope) {
		if (classScope == null)
			return false;
		else if (nameDefinedInType(classScope.referenceContext))
			return true;
		else if (classScope.parent instanceof ClassScope)
			return nameDefinedInScope((ClassScope) classScope.parent);
		else if (classScope.parent instanceof BlockScope)
			return nameDefinedInScope((BlockScope) classScope.parent);
		else
			return false;
	}

	private boolean nameDefinedInScope(BlockScope blockScope) {
		if (blockScope == null)
			return false;
		else if (blockScope.parent instanceof BlockScope)
			return nameDefinedInScope((BlockScope) blockScope.parent);
		else if (blockScope.parent instanceof ClassScope)
			return nameDefinedInScope((ClassScope) blockScope.parent);
		else
			return false;
	}
}

