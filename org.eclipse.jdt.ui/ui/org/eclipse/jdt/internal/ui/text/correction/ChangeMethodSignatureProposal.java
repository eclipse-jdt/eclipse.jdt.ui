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
import org.eclipse.jdt.core.NamingConventions;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;

public class ChangeMethodSignatureProposal extends LinkedCorrectionProposal {
	
	public static interface ChangeDescription {
	}
	
	public static class SwapDescription implements ChangeDescription {
		int index;
		public SwapDescription(int index) {
			this.index= index;
		}
	}
	
	public static class RemoveDescription implements ChangeDescription {
	}
	
	private static class ModifyDescription implements ChangeDescription {
		String name;
		ITypeBinding type;
		SingleVariableDeclaration resultingNode;
		
		public ModifyDescription(ITypeBinding type, String name) {
			this.type= type;
			this.name= name;
		}		
	}
	
	public static class EditDescription extends ModifyDescription {
		public EditDescription(ITypeBinding type, String name) {
			super(type, name);
		}
	}
	
	public static class InsertDescription extends ModifyDescription {
		public InsertDescription(ITypeBinding type, String name) {
			super(type, name);
		}
	}	
		
	private ASTNode fInvocationNode;
	private IMethodBinding fSenderBinding;
	private ChangeDescription[] fParameterChanges;
	private ChangeDescription[] fExceptionChanges;
		
	public ChangeMethodSignatureProposal(String label, ICompilationUnit targetCU, ASTNode invocationNode, IMethodBinding binding, ChangeDescription[] paramChanges, ChangeDescription[] exceptionChanges, int relevance, Image image) {
		super(label, targetCU, null, relevance, image);
		
		fInvocationNode= invocationNode;
		fSenderBinding= binding;
		fParameterChanges= paramChanges;
		fExceptionChanges= exceptionChanges;
	}
	
	protected ASTRewrite getRewrite() throws CoreException {
		CompilationUnit astRoot= (CompilationUnit) fInvocationNode.getRoot();
		ASTNode methodDecl= astRoot.findDeclaringNode(fSenderBinding);
		ASTNode newMethodDecl= null;
		boolean isInDifferentCU;
		if (methodDecl != null) {
			isInDifferentCU= false;
			newMethodDecl= methodDecl;
		} else {
			isInDifferentCU= true;
			ASTParser astParser= ASTParser.newParser(AST.JLS2);
			astParser.setSource(getCompilationUnit());
			astParser.setResolveBindings(true);
			astRoot= (CompilationUnit) astParser.createAST(null);
			newMethodDecl= astRoot.findDeclaringNode(fSenderBinding.getKey());
		}
		if (newMethodDecl instanceof MethodDeclaration) {
			MethodDeclaration decl= (MethodDeclaration) newMethodDecl;
			
			ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
			if (fParameterChanges != null) {
				modifyParameters(rewrite, decl, isInDifferentCU);
			}
			if (fExceptionChanges != null) {
				modifyExceptions(rewrite, decl);
			}
			return rewrite;
		}
		return null;
	}
	
	private void modifyParameters(ASTRewrite rewrite, MethodDeclaration methodDecl, boolean isInDifferentCU) throws CoreException {
		AST ast= methodDecl.getAST();

		ArrayList usedNames= new ArrayList();
		boolean hasCreatedVariables= false;
		
		IVariableBinding[] declaredFields= fSenderBinding.getDeclaringClass().getDeclaredFields();
		for (int i= 0; i < declaredFields.length; i++) { // avoid to take parameter names that are equal to field names
			usedNames.add(declaredFields[i].getName());
		}
		
		ImportRewrite imports= getImportRewrite();
		ListRewrite listRewrite= rewrite.getListRewrite(methodDecl, MethodDeclaration.PARAMETERS_PROPERTY);
		
		List parameters= methodDecl.parameters(); // old parameters
		int k= 0; // index over the oldParameters
		
		for (int i= 0; i < fParameterChanges.length; i++) {
			ChangeDescription curr= fParameterChanges[i];
			
			if (curr == null) {
				SingleVariableDeclaration oldParam= (SingleVariableDeclaration) parameters.get(k);
				usedNames.add(oldParam.getName().getIdentifier());
				k++;
			} else if (curr instanceof InsertDescription) {
				InsertDescription desc= (InsertDescription) curr;
				SingleVariableDeclaration newNode= ast.newSingleVariableDeclaration();
				String type= imports.addImport(desc.type);
				newNode.setType(ASTNodeFactory.newType(ast, type));
				
				// remember to set name later
				desc.resultingNode= newNode;
				hasCreatedVariables= true;
				
				listRewrite.insertAt(newNode, i, null);
					
			} else if (curr instanceof RemoveDescription) {
				listRewrite.remove((ASTNode) parameters.get(k), null);
				k++;
			} else if (curr instanceof EditDescription) {
				EditDescription desc= (EditDescription) curr;

				SingleVariableDeclaration newNode= ast.newSingleVariableDeclaration();
				String type= imports.addImport(desc.type);
				newNode.setType(ASTNodeFactory.newType(ast, type));
				
				// remember to set name later
				desc.resultingNode= newNode;
				hasCreatedVariables= true;
				
				listRewrite.replace((ASTNode) parameters.get(k), newNode, null);
				
				k++;
			} else if (curr instanceof SwapDescription) {
				SingleVariableDeclaration decl1= (SingleVariableDeclaration) parameters.get(k);
				SingleVariableDeclaration decl2= (SingleVariableDeclaration) parameters.get(((SwapDescription) curr).index);
				
				rewrite.replace(decl1, rewrite.createCopyTarget(decl2), null);
				rewrite.replace(decl2, rewrite.createCopyTarget(decl1), null);
				
				usedNames.add(decl1.getName().getIdentifier());
				k++;	
			}
		}
		if (!hasCreatedVariables) {
			return;
		}
		
		if (methodDecl.getBody() != null) {
			// avoid take a name of a local variable inside
			CompilationUnit root= (CompilationUnit) methodDecl.getRoot();
			IBinding[] bindings= (new ScopeAnalyzer(root)).getDeclarationsAfter(methodDecl.getBody().getStartPosition(), ScopeAnalyzer.VARIABLES);
			for (int i= 0; i < bindings.length; i++) {
				usedNames.add(bindings[i].getName());
			}
		}
		
		fixupNames(rewrite, usedNames, isInDifferentCU);
	}

	private void fixupNames(ASTRewrite rewrite, ArrayList usedNames, boolean isInDifferentCU) {
		AST ast= rewrite.getAST();
		// set names for new parameters
		for (int i= 0; i < fParameterChanges.length; i++) {
			ChangeDescription curr= fParameterChanges[i];
			if (curr instanceof ModifyDescription) {
				ModifyDescription desc= (ModifyDescription) curr;
				SingleVariableDeclaration var= desc.resultingNode;
				String suggestedName= desc.name;

				String typeKey= "param_type_" + i; //$NON-NLS-1$
				String nameKey= "param_name_" + i; //$NON-NLS-1$

				// collect name suggestions
				String favourite= null;
				String[] excludedNames= (String[]) usedNames.toArray(new String[usedNames.size()]);
				if (suggestedName != null) {
					favourite= StubUtility.suggestArgumentName(getCompilationUnit().getJavaProject(), suggestedName, excludedNames);
					addLinkedPositionProposal(nameKey, favourite, null);
				}
				Type type= var.getType();
				int dim= 0;
				if (type.isArrayType()) {
					dim= ((ArrayType) type).getDimensions();
					type= ((ArrayType) type).getElementType();
				}
				String[] suggestedNames=  NamingConventions.suggestArgumentNames(getCompilationUnit().getJavaProject(), "", ASTNodes.asString(type), dim, excludedNames); //$NON-NLS-1$
				for (int k= 0; k < suggestedNames.length; k++) {
					addLinkedPositionProposal(nameKey, suggestedNames[k], null);
				}
				if (favourite == null) {
					favourite= suggestedNames[0];
				}

				var.setName(ast.newSimpleName(favourite));
				usedNames.add(favourite);

				// collect type suggestions
				ITypeBinding[] bindings= ASTResolving.getRelaxingTypes(ast, desc.type);
				for (int k= 0; k < bindings.length; k++) {
					addLinkedPositionProposal(typeKey, bindings[k]);
				}
			
				addLinkedPosition(rewrite.track(var.getType()), false, typeKey);
				addLinkedPosition(rewrite.track(var.getName()), false, nameKey);
			}
		}
	}
	
	private void modifyExceptions(ASTRewrite rewrite, MethodDeclaration methodDecl) throws CoreException {
		AST ast= methodDecl.getAST();
		
		ImportRewrite imports= getImportRewrite();
		ListRewrite listRewrite= rewrite.getListRewrite(methodDecl, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY);
		
		List exceptions= methodDecl.thrownExceptions(); // old exceptions
		int k= 0; // index over the old exceptions
		
		for (int i= 0; i < fExceptionChanges.length; i++) {
			ChangeDescription curr= fExceptionChanges[i];
			
			if (curr == null) {
				k++;
			} else if (curr instanceof InsertDescription) {
				InsertDescription desc= (InsertDescription) curr;
				String type= imports.addImport(desc.type);
				ASTNode newNode= ASTNodeFactory.newName(ast, type);

				listRewrite.insertAt(newNode, i, null);
			} else if (curr instanceof RemoveDescription) {
				listRewrite.remove((ASTNode) exceptions.get(k), null);
				k++;
			} else if (curr instanceof EditDescription) {
				EditDescription desc= (EditDescription) curr;

				String type= imports.addImport(desc.type);
				ASTNode newNode= ASTNodeFactory.newName(ast, type);
				
				listRewrite.replace((ASTNode) exceptions.get(k), newNode, null);
				k++;
			} else if (curr instanceof SwapDescription) {
				ASTNode decl1= (ASTNode) exceptions.get(k);
				ASTNode decl2= (ASTNode) exceptions.get(((SwapDescription) curr).index);
				
				rewrite.replace(decl1, rewrite.createCopyTarget(decl2), null);
				rewrite.replace(decl2, rewrite.createCopyTarget(decl1), null);
				
				k++;	
			}
		}
	}

	
}
