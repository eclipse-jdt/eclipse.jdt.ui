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

package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.ui.CodeGeneration;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.OldASTRewrite;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.ListRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class NewMethodCompletionProposal extends LinkedCorrectionProposal {

	private static final String KEY_NAME= "name"; //$NON-NLS-1$
	private static final String KEY_TYPE= "type"; //$NON-NLS-1$

	private ASTNode fNode; // MethodInvocation, ConstructorInvocation, SuperConstructorInvocation, ClassInstanceCreation, SuperMethodInvocation
	private List fArguments;
	private ITypeBinding fSenderBinding;
		
	public NewMethodCompletionProposal(String label, ICompilationUnit targetCU, ASTNode invocationNode,  List arguments, ITypeBinding binding, int relevance, Image image) {
		super(label, targetCU, null, relevance, image);
		
		fNode= invocationNode;
		fArguments= arguments;
		fSenderBinding= binding;
	}
			
	protected OldASTRewrite getRewrite() throws CoreException {
		
		CompilationUnit astRoot= ASTResolving.findParentCompilationUnit(fNode);
		ASTNode typeDecl= astRoot.findDeclaringNode(fSenderBinding);
		ASTNode newTypeDecl= null;
		boolean isInDifferentCU;
		if (typeDecl != null) {
			isInDifferentCU= false;
			newTypeDecl= typeDecl;
		} else {
			isInDifferentCU= true;
			astRoot= AST.parseCompilationUnit(getCompilationUnit(), true);
			newTypeDecl= astRoot.findDeclaringNode(fSenderBinding.getKey());
		}
		if (newTypeDecl != null) {
			OldASTRewrite rewrite= new OldASTRewrite(astRoot);
			
			ChildListPropertyDescriptor property= fSenderBinding.isAnonymous() ? AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY : TypeDeclaration.BODY_DECLARATIONS_PROPERTY;
			List members= (List) newTypeDecl.getStructuralProperty(property);
			
			MethodDeclaration newStub= getStub(rewrite, newTypeDecl);
			
			int insertIndex;
			if (isConstructor()) {
				insertIndex= findConstructorInsertIndex(members);
			} else if (!isInDifferentCU) {
				insertIndex= findMethodInsertIndex(members, fNode.getStartPosition());
			} else {
				insertIndex= members.size();
			}
			ListRewrite listRewriter= rewrite.getListRewrite(newTypeDecl, property);
			listRewriter.insertAt(newStub, insertIndex, null);

			if (!isInDifferentCU) {
				Name invocationName= getInvocationName();
				if (invocationName != null) {
					markAsLinked(rewrite, invocationName, true, KEY_NAME);
				}				
			}
			markAsLinked(rewrite, newStub.getName(), false, KEY_NAME);
			if (!newStub.isConstructor()) {
				markAsLinked(rewrite, newStub.getReturnType(), false, KEY_TYPE);
			}			
			
			return rewrite;
		}
		return null;
	}
	
	private boolean isConstructor() {
		return fNode.getNodeType() != ASTNode.METHOD_INVOCATION && fNode.getNodeType() != ASTNode.SUPER_METHOD_INVOCATION;
	}
	
	private MethodDeclaration getStub(OldASTRewrite rewrite, ASTNode targetTypeDecl) throws CoreException {
		AST ast= targetTypeDecl.getAST();
		MethodDeclaration decl= ast.newMethodDeclaration();
		
		decl.setConstructor(isConstructor());
		decl.setModifiers(evaluateModifiers(targetTypeDecl));
		decl.setName(ast.newSimpleName(getMethodName()));
		
		List arguments= fArguments;
		List params= decl.parameters();
		
		int nArguments= arguments.size();
		ArrayList takenNames= new ArrayList(nArguments);
		IVariableBinding[] declaredFields= fSenderBinding.getDeclaredFields();
		for (int i= 0; i < declaredFields.length; i++) { // avoid to take parameter names that are equal to field names
			takenNames.add(declaredFields[i].getName());
		}
		
		for (int i= 0; i < arguments.size(); i++) {
			Expression elem= (Expression) arguments.get(i);
			SingleVariableDeclaration param= ast.newSingleVariableDeclaration();

			// argument type
			String argTypeKey= "arg_type_" + i; //$NON-NLS-1$
			Type type= evaluateParameterTypes(ast, elem, argTypeKey);
			param.setType(type);

			// argument name
			String argNameKey= "arg_name_" + i; //$NON-NLS-1$
			String name= evaluateParameterNames(takenNames, elem, type, argNameKey);
			param.setName(ast.newSimpleName(name));

			params.add(param);
			
			markAsLinked(rewrite, param.getType(), false, argTypeKey);
			markAsLinked(rewrite, param.getName(), false, argNameKey);
		}
		
		Block body= null;

		String bodyStatement= ""; //$NON-NLS-1$
		if (!isConstructor()) {
			Type returnType= evaluateMethodType(ast);
			if (returnType == null) {
				decl.setReturnType(ast.newPrimitiveType(PrimitiveType.VOID));
			} else {
				decl.setReturnType(returnType);
			}
			if (!fSenderBinding.isInterface() && returnType != null) {
				ReturnStatement returnStatement= ast.newReturnStatement();
				returnStatement.setExpression(ASTNodeFactory.newDefaultExpression(ast, returnType, 0));
				bodyStatement= ASTNodes.asFormattedString(returnStatement, 0, String.valueOf('\n'));
			}
		}
		if (!fSenderBinding.isInterface()) {
			body= ast.newBlock();
			String placeHolder= CodeGeneration.getMethodBodyContent(getCompilationUnit(), fSenderBinding.getName(), getMethodName(), isConstructor(), bodyStatement, String.valueOf('\n')); 	
			if (placeHolder != null) {
				ASTNode todoNode= rewrite.createPlaceholder(placeHolder, ASTNode.RETURN_STATEMENT);
				body.statements().add(todoNode);
			}
		}
		decl.setBody(body);

		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		if (settings.createComments && !fSenderBinding.isAnonymous()) {
			String string= CodeGeneration.getMethodComment(getCompilationUnit(), fSenderBinding.getName(), decl, null, String.valueOf('\n'));
			if (string != null) {
				Javadoc javadoc= (Javadoc) rewrite.createPlaceholder(string, ASTNode.JAVADOC);
				decl.setJavadoc(javadoc);
			}
		}
		return decl;
	}
			
	private int findMethodInsertIndex(List decls, int currPos) {
		int nDecls= decls.size();
		for (int i= 0; i < nDecls; i++) {
			ASTNode curr= (ASTNode) decls.get(i);
			if (curr instanceof MethodDeclaration && currPos < curr.getStartPosition() + curr.getLength()) {
				return i + 1;
			}
		}
		return nDecls;
	}
	
	private int findConstructorInsertIndex(List decls) {
		int nDecls= decls.size();
		int lastMethod= 0;
		for (int i= nDecls - 1; i >= 0; i--) {
			ASTNode curr= (ASTNode) decls.get(i);
			if (curr instanceof MethodDeclaration) {
				if (((MethodDeclaration) curr).isConstructor()) {
					return i + 1;
				}
				lastMethod= i;
			}
		}
		return lastMethod;
	}
	
	private Name getInvocationName() {
		if (fNode instanceof MethodInvocation) {
			return ((MethodInvocation)fNode).getName();
		} else if (fNode instanceof SuperMethodInvocation) {
			return ((SuperMethodInvocation)fNode).getName();
		} else if (fNode instanceof ClassInstanceCreation) {
			return ((ClassInstanceCreation)fNode).getName();
		}		
		return null;
	}
	
	
	private String getMethodName() {
		if (fNode instanceof MethodInvocation) {
			return ((MethodInvocation)fNode).getName().getIdentifier();
		} else if (fNode instanceof SuperMethodInvocation) {
			return ((SuperMethodInvocation)fNode).getName().getIdentifier();
		} else {
			return fSenderBinding.getName(); // name of the class
		}
	}
	
	private int evaluateModifiers(ASTNode targetTypeDecl) {
		if (fSenderBinding.isInterface()) {
			// for interface members copy the modifiers from an existing field
			MethodDeclaration[] methodDecls= ((TypeDeclaration) targetTypeDecl).getMethods();
			if (methodDecls.length > 0) {
				return methodDecls[0].getModifiers();
			}
			return 0;
		}
		if (fNode instanceof MethodInvocation) {
			int modifiers= 0;
			Expression expression= ((MethodInvocation)fNode).getExpression();
			if (expression != null) {
				if (expression instanceof Name && ((Name) expression).resolveBinding().getKind() == IBinding.TYPE) {
					modifiers |= Modifier.STATIC;
				}
			} else if (ASTResolving.isInStaticContext(fNode)) {
				modifiers |= Modifier.STATIC;
			}
			ASTNode node= ASTResolving.findParentType(fNode);
			if (targetTypeDecl.equals(node)) {
				modifiers |= Modifier.PRIVATE;
			} else if (node instanceof AnonymousClassDeclaration && ASTNodes.isParent(node, targetTypeDecl)) {
				modifiers |= Modifier.PROTECTED;
			} else {
				modifiers |= Modifier.PUBLIC;
			}
			return modifiers;
		}
		return Modifier.PUBLIC;
	}
	
	private Type evaluateMethodType(AST ast) throws CoreException {
		ITypeBinding binding= ASTResolving.guessBindingForReference(fNode);
		if (binding != null) {
			String typeName= getImportRewrite().addImport(binding);
			return ASTNodeFactory.newType(ast, typeName);			
		} else {
			ASTNode parent= fNode.getParent();
			if (parent instanceof ExpressionStatement) {
				return null;
			}
			Type type= ASTResolving.guessTypeForReference(ast, fNode);
			if (type != null) {
				return type;
			}
			return ast.newSimpleType(ast.newSimpleName("Object")); //$NON-NLS-1$
		}
	}
	
	private Type evaluateParameterTypes(AST ast, Expression elem, String key) throws CoreException {
		ITypeBinding binding= Bindings.normalizeTypeBinding(elem.resolveTypeBinding());
		if (binding != null) {
			ITypeBinding[] typeProposals= ASTResolving.getRelaxingTypes(ast, binding);
			for (int i= 0; i < typeProposals.length; i++) {
				addLinkedModeProposal(key, typeProposals[i]);
			}		
			String typeName= getImportRewrite().addImport(binding);
			return ASTNodeFactory.newType(ast, typeName);
		}
		return ast.newSimpleType(ast.newSimpleName("Object")); //$NON-NLS-1$
	}
	
	private String evaluateParameterNames(ArrayList takenNames, Expression argNode, Type type, String key) {
		IJavaProject project= getCompilationUnit().getJavaProject();
		String[] excludedNames= (String[]) takenNames.toArray(new String[takenNames.size()]);
		String favourite= null;
		if (argNode instanceof SimpleName) {
			SimpleName name= (SimpleName) argNode;
			favourite= StubUtility.suggestArgumentName(project, name.getIdentifier(), excludedNames);
		}
		
		int dim= 0;
		if (type.isArrayType()) {
			ArrayType arrayType= (ArrayType) type;
			dim= arrayType.getDimensions();
			type= arrayType.getElementType();
		}
		String typeName= ASTNodes.asString(type);
		String packName= Signature.getQualifier(typeName);
		
		String[] names= NamingConventions.suggestArgumentNames(project, packName, typeName, dim, excludedNames);
		if (favourite == null) {
			favourite= names[0];
		}
		for (int i= 0; i < names.length; i++) {
			addLinkedModeProposal(key, names[i]);
		}
		
		takenNames.add(favourite);
		return favourite;
	}	

}
