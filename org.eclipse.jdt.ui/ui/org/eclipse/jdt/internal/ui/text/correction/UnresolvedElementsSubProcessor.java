/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Renaud Waldura &lt;renaud+eclipse@waldura.com&gt; - New class/interface with wizard
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.ui.ISharedImages;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeConstants;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.dom.TypeRules;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.text.correction.ChangeMethodSignatureProposal.ChangeDescription;
import org.eclipse.jdt.internal.ui.text.correction.ChangeMethodSignatureProposal.EditDescription;
import org.eclipse.jdt.internal.ui.text.correction.ChangeMethodSignatureProposal.InsertDescription;
import org.eclipse.jdt.internal.ui.text.correction.ChangeMethodSignatureProposal.RemoveDescription;
import org.eclipse.jdt.internal.ui.text.correction.ChangeMethodSignatureProposal.SwapDescription;

public class UnresolvedElementsSubProcessor {
	
	public static void getVariableProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		
		ICompilationUnit cu= context.getCompilationUnit();
		
		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveredNode(astRoot);
		if (selectedNode == null) {
			return;
		}
		
		// type that defines the variable
		ITypeBinding binding= null;
		ITypeBinding declaringTypeBinding= Bindings.getBindingOfParentType(selectedNode);
		if (declaringTypeBinding == null) {
			return;
		}
		

		// possible type kind of the node
		boolean suggestVariablePropasals= true;
		int typeKind= 0;
		
		while (selectedNode instanceof ParenthesizedExpression) {
			selectedNode= ((ParenthesizedExpression) selectedNode).getExpression();
		}
		
		
		Name node= null;
		switch (selectedNode.getNodeType()) {
			case ASTNode.SIMPLE_NAME:
				node= (SimpleName) selectedNode;
				ASTNode parent= node.getParent();
				if (parent instanceof MethodInvocation && node.equals(((MethodInvocation)parent).getExpression())) {
					typeKind= SimilarElementsRequestor.CLASSES;
				} else if (parent instanceof SimpleType) {
					suggestVariablePropasals= false;
					typeKind= SimilarElementsRequestor.REF_TYPES;
				}
				break;		
			case ASTNode.QUALIFIED_NAME:
				QualifiedName qualifierName= (QualifiedName) selectedNode;
				ITypeBinding qualifierBinding= qualifierName.getQualifier().resolveTypeBinding();
				if (qualifierBinding != null) {
					node= qualifierName.getName();
					binding= qualifierBinding;
				} else {
					node= qualifierName.getQualifier();
					typeKind= SimilarElementsRequestor.REF_TYPES;
					suggestVariablePropasals= node.isSimpleName();
				}
				if (selectedNode.getParent() instanceof SimpleType) {
					typeKind= SimilarElementsRequestor.REF_TYPES;
					suggestVariablePropasals= false;
				}
				break;		
			case ASTNode.FIELD_ACCESS:
				FieldAccess access= (FieldAccess) selectedNode;
				Expression expression= access.getExpression();
				if (expression != null) {
					binding= expression.resolveTypeBinding();
					if (binding != null) {
						node= access.getName();
					}
				}
				break;
			case ASTNode.SUPER_FIELD_ACCESS:
				binding= declaringTypeBinding.getSuperclass();
				node= ((SuperFieldAccess) selectedNode).getName();
				break;
			default:	
		}
		
		if (node == null) {
			return;
		}		

		// add type proposals
		if (typeKind != 0) {
			int relevance= Character.isUpperCase(ASTResolving.getSimpleName(node).charAt(0)) ? 3 : 0;
			addSimilarTypeProposals(typeKind, cu, node, relevance + 1, proposals);
			addNewTypeProposals(cu, node, SimilarElementsRequestor.REF_TYPES, relevance, proposals);
		}
		
		if (!suggestVariablePropasals) {
			return;
		}
		
		SimpleName simpleName= node.isSimpleName() ? (SimpleName) node : ((QualifiedName) node).getName();
		boolean isWriteAccess= ASTResolving.isWriteAccess(node);
		
		// similar variables
		addSimilarVariableProposals(cu, astRoot, simpleName, isWriteAccess, proposals);
		
		// new fields
		addNewFieldProposals(cu, astRoot, binding, declaringTypeBinding, simpleName, isWriteAccess, proposals);
		
		// new parameters and local variables
		if (binding == null) {
			addNewVariableProposals(cu, node, simpleName, proposals);
		}
	}
	
	private static void addNewVariableProposals(ICompilationUnit cu, Name node, SimpleName simpleName, Collection proposals) throws CoreException {
		String name= simpleName.getIdentifier();
		BodyDeclaration bodyDeclaration= ASTResolving.findParentBodyDeclaration(node);
		int type= bodyDeclaration.getNodeType();
		if (type == ASTNode.METHOD_DECLARATION) {
			int relevance= StubUtility.hasParameterName(cu.getJavaProject(), name) ? 8 : 5;
			String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createparameter.description", simpleName.getIdentifier()); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
			proposals.add(new NewVariableCompletionProposal(label, cu, NewVariableCompletionProposal.PARAM, simpleName, null, relevance, image));
		}
		if (type == ASTNode.METHOD_DECLARATION || type == ASTNode.INITIALIZER) {
			int relevance= StubUtility.hasLocalVariableName(cu.getJavaProject(), name) ? 10 : 7;
			String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createlocal.description", simpleName.getIdentifier()); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
			proposals.add(new NewVariableCompletionProposal(label, cu, NewVariableCompletionProposal.LOCAL, simpleName, null, relevance, image));
		}
		
		if (node.getParent().getNodeType() == ASTNode.ASSIGNMENT) {
			Assignment assignment= (Assignment) node.getParent();
			if (assignment.getLeftHandSide() == node && assignment.getParent().getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
				ASTNode statement= assignment.getParent();
				ASTRewrite rewrite= new ASTRewrite(statement.getParent());
				rewrite.markAsRemoved(statement);
		
				String label= CorrectionMessages.getString("UnresolvedElementsSubProcessor.removestatement.description"); //$NON-NLS-1$
				Image image= JavaPlugin.getDefault().getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
				ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 4, image);
				proposal.ensureNoModifications();
				proposals.add(proposal);
			}
		}
	}

	private static void addNewFieldProposals(ICompilationUnit cu, CompilationUnit astRoot, ITypeBinding binding, ITypeBinding declaringTypeBinding, SimpleName simpleName, boolean isWriteAccess, Collection proposals) throws JavaModelException {
		// new variables
		ICompilationUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, binding);
		ITypeBinding senderBinding= binding != null ? binding : declaringTypeBinding;
		
		if (!senderBinding.isFromSource() || targetCU == null || !JavaModelUtil.isEditable(targetCU)) {
			return;
		}
			
		ITypeBinding outsideAnonymous= null;
		if (binding == null && senderBinding.isAnonymous()) {
			ASTNode anonymDecl= astRoot.findDeclaringNode(senderBinding);
			if (anonymDecl != null) {
				ITypeBinding bind= Bindings.getBindingOfParentType(anonymDecl.getParent());
				if (!bind.isAnonymous()) {
					outsideAnonymous= bind;
				}
			}
		}
		
		String name= simpleName.getIdentifier();
		int relevance= StubUtility.hasFieldName(cu.getJavaProject(), name) ? 9 : 6;

		String label;
		Image image;
		if (binding == null) {
			label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createfield.description", name); //$NON-NLS-1$
			image= JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PRIVATE);
		} else {
			label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createfield.other.description", new Object[] { name, binding.getName() } ); //$NON-NLS-1$
			image= JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PUBLIC);
		}
		
		proposals.add(new NewVariableCompletionProposal(label, targetCU, NewVariableCompletionProposal.FIELD, simpleName, senderBinding, relevance, image));

		// create field in outer class (if inside anonymous)
		if (outsideAnonymous != null) {
			label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createfield.other.description", new Object[] { name, senderBinding.getName() } ); //$NON-NLS-1$
			image= JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PUBLIC);
			proposals.add(new NewVariableCompletionProposal(label, targetCU, NewVariableCompletionProposal.FIELD, simpleName, outsideAnonymous, relevance + 1, image));
		}
		
		// create constant
		
		if (!isWriteAccess) {
			relevance= StubUtility.hasConstantName(name) ? 9 : 4;
			ITypeBinding target= (outsideAnonymous != null) ? outsideAnonymous : senderBinding;
			if (binding == null) {
				label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createconst.description", name); //$NON-NLS-1$
				image= JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PRIVATE);
			} else {
				label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createconst.other.description", new Object[] { name, binding.getName() } ); //$NON-NLS-1$
				image= JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PUBLIC);
			}
			proposals.add(new NewVariableCompletionProposal(label, targetCU, NewVariableCompletionProposal.CONST_FIELD, simpleName, target, relevance, image));
		}
	}

	private static void addSimilarVariableProposals(ICompilationUnit cu, CompilationUnit astRoot, SimpleName node, boolean isWriteAccess, Collection proposals) {
		IBinding[] varsInScope= (new ScopeAnalyzer(astRoot)).getDeclarationsInScope(node, ScopeAnalyzer.VARIABLES);
		if (varsInScope.length > 0) {
			// avoid corrections like int i= i;
			String otherNameInAssign= null;
			ASTNode parent= node.getParent();
			if (parent.getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
				// node must be initializer
				otherNameInAssign= ((VariableDeclarationFragment) parent).getName().getIdentifier();
			} else if (parent.getNodeType() == ASTNode.ASSIGNMENT) {
				Assignment assignment= (Assignment) parent;
				if (isWriteAccess && assignment.getRightHandSide() instanceof SimpleName) {
					otherNameInAssign= ((SimpleName) assignment.getRightHandSide()).getIdentifier();
				} else if (!isWriteAccess && assignment.getLeftHandSide() instanceof SimpleName) {
					otherNameInAssign= ((SimpleName) assignment.getLeftHandSide()).getIdentifier();
				}
			}
			
			ITypeBinding guessedType= ASTResolving.guessBindingForReference(node);
			if (astRoot.getAST().resolveWellKnownType("java.lang.Object") == guessedType) { //$NON-NLS-1$
				guessedType= null; // too many suggestions
			}
			
			String identifier= node.getIdentifier();
			for (int i= 0; i < varsInScope.length; i++) {
				IVariableBinding curr= (IVariableBinding) varsInScope[i];
				String currName= curr.getName();
				boolean isFinal= Modifier.isFinal(curr.getModifiers());
				if (!currName.equals(otherNameInAssign) && !(isFinal && curr.isField() && isWriteAccess)) {
					int relevance= 0;
					if (NameMatcher.isSimilarName(currName, identifier)) {
						relevance += 3; // variable with a similar name than the unresolved variable
					}
					ITypeBinding varType= curr.getType();
					if (guessedType != null && varType != null) {
						if (!isWriteAccess && TypeRules.canAssign(varType, guessedType)
								|| isWriteAccess && TypeRules.canAssign(guessedType, varType)) {
							relevance += 2; // unresolved variable can be assign to this variable
						}
					}
								
					if (relevance > 0) {
						String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.changevariable.description", currName); //$NON-NLS-1$
						proposals.add(new ReplaceCorrectionProposal(label, cu, node.getStartPosition(), node.getLength(), currName, relevance));
					}
				}
			}			
		}
	}

	public static void getTypeProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();
		
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return;
		}
		int kind= SimilarElementsRequestor.ALL_TYPES;
		
		ASTNode parent= selectedNode.getParent();
		while (parent.getLength() == selectedNode.getLength()) { // get parent of type or variablefragment
			parent= parent.getParent(); 
		}
		switch (parent.getNodeType()) {
			case ASTNode.TYPE_DECLARATION:
				TypeDeclaration typeDeclaration=(TypeDeclaration) parent;
				if (typeDeclaration.superInterfaces().contains(selectedNode)) {					
					kind= SimilarElementsRequestor.INTERFACES;
				} else if (selectedNode.equals(typeDeclaration.getSuperclass())) {
					kind= SimilarElementsRequestor.CLASSES;
				}
				break;
			case ASTNode.METHOD_DECLARATION:
				MethodDeclaration methodDeclaration= (MethodDeclaration) parent;
				if (methodDeclaration.thrownExceptions().contains(selectedNode)) {
					kind= SimilarElementsRequestor.CLASSES;
				} else if (selectedNode.equals(methodDeclaration.getReturnType())) {
					kind= SimilarElementsRequestor.ALL_TYPES | SimilarElementsRequestor.VOIDTYPE;
				}
				break;
			case ASTNode.INSTANCEOF_EXPRESSION:
				kind= SimilarElementsRequestor.REF_TYPES;
				break;
			case ASTNode.THROW_STATEMENT:
				kind= SimilarElementsRequestor.REF_TYPES;
				break;
			case ASTNode.CLASS_INSTANCE_CREATION:
				if (((ClassInstanceCreation) parent).getAnonymousClassDeclaration() == null) {
					kind= SimilarElementsRequestor.CLASSES;
				} else {
					kind= SimilarElementsRequestor.REF_TYPES;
				}
				break;
			case ASTNode.SINGLE_VARIABLE_DECLARATION:
				int superParent= parent.getParent().getNodeType();
				if (superParent == ASTNode.CATCH_CLAUSE) {
					kind= SimilarElementsRequestor.CLASSES;
				}
				break;
			default:
		}		
		
		Name node= null;
		if (selectedNode instanceof SimpleType) {
			node= ((SimpleType) selectedNode).getName();
		} else if (selectedNode instanceof ArrayType) {
			Type elementType= ((ArrayType) selectedNode).getElementType();
			if (elementType.isSimpleType()) {
				node= ((SimpleType) elementType).getName();
			}
		} else if (selectedNode instanceof Name) {
			node= (Name) selectedNode;
		} else {
			return;
		}
		
		// change to simlar type proposals
		addSimilarTypeProposals(kind, cu, node, 3, proposals);
		
		// add type
		addNewTypeProposals(cu, node, kind, 0, proposals);
	}

	private static void addSimilarTypeProposals(int kind, ICompilationUnit cu, Name node, int relevance, Collection proposals) throws CoreException {
		SimilarElement[] elements= SimilarElementsRequestor.findSimilarElement(cu, node, kind);
		
		// try to resolve type in context -> highest severity
		String resolvedTypeName= null;
		ITypeBinding binding= ASTResolving.guessBindingForTypeReference(node);
		if (binding != null) {
			if (binding.isArray()) {
				binding= binding.getElementType();
			}
			resolvedTypeName= binding.getQualifiedName();
			proposals.add(createTypeRefChangeProposal(cu, resolvedTypeName, node, relevance + 2));
		}
		// add all similar elements
		for (int i= 0; i < elements.length; i++) {
			SimilarElement elem= elements[i];
			if ((elem.getKind() & SimilarElementsRequestor.ALL_TYPES) != 0) {
				String fullName= elem.getName();
				if (!fullName.equals(resolvedTypeName)) {
					proposals.add(createTypeRefChangeProposal(cu, fullName, node, relevance));
				}
			}
		}
	}

	private static CUCorrectionProposal createTypeRefChangeProposal(ICompilationUnit cu, String fullName, Name node, int relevance) throws CoreException {
		TextBuffer buffer= null;
		try {
			buffer= TextBuffer.acquire((IFile)WorkingCopyUtil.getOriginal(cu).getResource());
			
			CUCorrectionProposal proposal= new CUCorrectionProposal("", cu, 0); //$NON-NLS-1$
	
			ImportRewrite importRewrite= new ImportRewrite(cu, JavaPreferencesSettings.getCodeGenerationSettings());				
			String simpleName= importRewrite.addImport(fullName);
			
			TextEdit root= proposal.getRootTextEdit();
			
			if (!importRewrite.isEmpty()) {
				root.addChild(importRewrite.createEdit(buffer)); //$NON-NLS-1$
			}
			String packName= Signature.getQualifier(fullName);
			String[] arg= { simpleName, packName };
			if (node.isSimpleName() && simpleName.equals(((SimpleName) node).getIdentifier())) { // import only
				proposal.setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_IMPDECL));
				proposal.setDisplayName(CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.importtype.description", arg)); //$NON-NLS-1$
				proposal.setRelevance(relevance + 20);
			} else {			
				root.addChild(new ReplaceEdit(node.getStartPosition(), node.getLength(), simpleName)); //$NON-NLS-1$
				if (packName.length() == 0) {
					proposal.setDisplayName(CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.changetype.nopack.description", simpleName)); //$NON-NLS-1$
				} else {
					proposal.setDisplayName(CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.changetype.description", arg)); //$NON-NLS-1$
				}
				proposal.setRelevance(relevance);
			}
			return proposal;
		} finally {
			if (buffer != null)
				TextBuffer.release(buffer);
		}
	}

	private static void addNewTypeProposals(ICompilationUnit cu, Name refNode, int kind, int relevance, Collection proposals) throws JavaModelException {
		Name node= refNode;
		do {
			String typeName= ASTResolving.getSimpleName(node);
			Name qualifier= null;
			// only propose to create types for qualifiers when the name starts with upper case
			boolean isPossibleName= Character.isUpperCase(typeName.charAt(0)) || (node == refNode);
			if (isPossibleName) {
				IPackageFragment enclosingPackage= null;
				IType enclosingType= null;
				if (node.isSimpleName()) {
					enclosingPackage= (IPackageFragment) cu.getParent();
					// don't sugest member type, user can select it in wizard
				} else {
					Name qualifierName= ((QualifiedName) node).getQualifier();
					// 24347
					// IBinding binding= qualifierName.resolveBinding(); 
					// if (binding instanceof ITypeBinding) {
					//	enclosingType= Binding2JavaModel.find((ITypeBinding) binding, cu.getJavaProject());
					
					IJavaElement[] res= cu.codeSelect(qualifierName.getStartPosition(), qualifierName.getLength());
					if (res!= null && res.length > 0 && res[0] instanceof IType) {
						enclosingType= (IType) res[0];
					} else {
						qualifier= qualifierName;
						enclosingPackage= JavaModelUtil.getPackageFragmentRoot(cu).getPackageFragment(ASTResolving.getFullName(qualifierName));
					}
				}
				// new top level type
				if (enclosingPackage != null && !enclosingPackage.getCompilationUnit(typeName + ".java").exists()) { //$NON-NLS-1$
					if ((kind & SimilarElementsRequestor.CLASSES) != 0) {
			            proposals.add(new NewCUCompletionUsingWizardProposal(cu, node, true, enclosingPackage, relevance));
					}
					if ((kind & SimilarElementsRequestor.INTERFACES) != 0) {			
			            proposals.add(new NewCUCompletionUsingWizardProposal(cu, node, false, enclosingPackage, relevance));
					}				
				}
				// new member type
				if (enclosingType != null && !enclosingType.isReadOnly() && !enclosingType.getType(typeName).exists()) {
					if ((kind & SimilarElementsRequestor.CLASSES) != 0) {
			            proposals.add(new NewCUCompletionUsingWizardProposal(cu, node, true, enclosingType, relevance));
					}
					if ((kind & SimilarElementsRequestor.INTERFACES) != 0) {			
			            proposals.add(new NewCUCompletionUsingWizardProposal(cu, node, false, enclosingType, relevance));
					}				
				}				
			}
			node= qualifier;
		} while (node != null);
	}
	
	public static void getMethodProposals(IInvocationContext context, IProblemLocation problem, boolean needsNewName, Collection proposals) throws CoreException {

		ICompilationUnit cu= context.getCompilationUnit();
		
		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(astRoot);
		
		if (!(selectedNode instanceof SimpleName)) {
			return;
		}
		SimpleName nameNode= (SimpleName) selectedNode;

		List arguments;
		Expression sender;
		boolean isSuperInvocation;
		
		ASTNode invocationNode= nameNode.getParent();
		if (invocationNode instanceof MethodInvocation) {
			MethodInvocation methodImpl= (MethodInvocation) invocationNode;
			arguments= methodImpl.arguments();
			sender= methodImpl.getExpression();
			isSuperInvocation= false;
		} else if (invocationNode instanceof SuperMethodInvocation) {
			SuperMethodInvocation methodImpl= (SuperMethodInvocation) invocationNode;
			arguments= methodImpl.arguments();
			sender= methodImpl.getQualifier();
			isSuperInvocation= true;
		} else {
			return;
		}
		
		String methodName= nameNode.getIdentifier();
		int nArguments= arguments.size();
			
		// corrections
		IBinding[] bindings= (new ScopeAnalyzer(astRoot)).getDeclarationsInScope(nameNode, ScopeAnalyzer.METHODS);
		
		ArrayList parameterMismatchs= new ArrayList();
		for (int i= 0; i < bindings.length; i++) {
			IMethodBinding binding= (IMethodBinding) bindings[i];
			String curr= binding.getName();
			if (curr.equals(methodName) && needsNewName) {
				parameterMismatchs.add(binding);
			} else if (binding.getParameterTypes().length == nArguments && NameMatcher.isSimilarName(methodName, curr)) {
				String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.changemethod.description", curr); //$NON-NLS-1$
				proposals.add(new ReplaceCorrectionProposal(label, context.getCompilationUnit(), problem.getOffset(), problem.getLength(), curr, 6));
			}
		}
			
		addParameterMissmatchProposals(context, problem, parameterMismatchs, invocationNode, arguments, proposals);
		
		// new method
		ITypeBinding binding= null;
		if (sender != null) {
			binding= sender.resolveTypeBinding();
		} else {
			binding= Bindings.getBindingOfParentType(invocationNode);
			if (isSuperInvocation && binding != null) {
				binding= binding.getSuperclass();
			}				
		}
		if (binding != null && binding.isFromSource()) {
			ICompilationUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, binding);
			if (targetCU != null) {			
				String label;
				Image image;
				String sig= getMethodSignature(methodName, arguments);
				
				if (cu.equals(targetCU)) {
					label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createmethod.description", sig); //$NON-NLS-1$
					image= JavaPluginImages.get(JavaPluginImages.IMG_MISC_PRIVATE);
				} else {
					label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createmethod.other.description", new Object[] { sig, targetCU.getElementName() } ); //$NON-NLS-1$
					image= JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC);
				}
				proposals.add(new NewMethodCompletionProposal(label, targetCU, invocationNode, arguments, binding, 5, image));
				
				if (binding.isAnonymous() && cu.equals(targetCU) && sender == null && Bindings.findMethodInHierarchy(binding, methodName, null) == null) { // no covering method
					ASTNode anonymDecl= astRoot.findDeclaringNode(binding);
					if (anonymDecl != null) {
						binding= Bindings.getBindingOfParentType(anonymDecl.getParent());
						if (!binding.isAnonymous()) {
							String[] args= new String[] { sig, binding.getName() };
							label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createmethod.other.description", args); //$NON-NLS-1$
							image= JavaPluginImages.get(JavaPluginImages.IMG_MISC_PROTECTED);
							proposals.add(new NewMethodCompletionProposal(label, targetCU, invocationNode, arguments, binding, 5, image));
						}
					}
				}
			}
		}
		
		if (!isSuperInvocation) {
			ASTNode parent= invocationNode.getParent();
			while (parent instanceof Expression && parent.getNodeType() != ASTNode.CAST_EXPRESSION) {
				parent= parent.getParent();
			}
			if (!isSuperInvocation && parent instanceof CastExpression) {
				addMissingCastParentsProposal(cu, (CastExpression) parent, sender, nameNode, getArgumentTypes(arguments), proposals);
			}
		}
		
	}

	private static void addMissingCastParentsProposal(ICompilationUnit cu, CastExpression expression, Expression accessExpression, SimpleName accessSelector, ITypeBinding[] paramTypes, Collection proposals) throws CoreException {
		ITypeBinding castType= expression.getType().resolveBinding();
		if (castType == null) {
			return;
		}
		if (paramTypes != null) {
			if (Bindings.findMethodInHierarchy(castType, accessSelector.getIdentifier(), paramTypes) == null) {
				return;
			}
		} else if (Bindings.findFieldInHierarchy(castType, accessSelector.getIdentifier()) == null) {
			return;
		}
		ITypeBinding bindingToCast= accessExpression.resolveTypeBinding();
		if (bindingToCast != null && !TypeRules.canCast(castType, bindingToCast)) {
			return;
		}
		
		IMethodBinding res= Bindings.findMethodInHierarchy(castType, accessSelector.getIdentifier(), paramTypes);
		if (res != null) {
			AST ast= expression.getAST();
			ASTRewrite rewrite= new ASTRewrite(expression.getParent());
			CastExpression newCast= ast.newCastExpression();
			newCast.setType((Type) ASTNode.copySubtree(ast, expression.getType()));
			newCast.setExpression((Expression) rewrite.createCopy(accessExpression));
			ParenthesizedExpression parents= ast.newParenthesizedExpression();
			parents.setExpression(newCast);
			
			ASTNode node= rewrite.createCopy(expression.getExpression());
			rewrite.markAsReplaced(expression, node);
			rewrite.markAsReplaced(accessExpression, parents);

			String label= CorrectionMessages.getString("UnresolvedElementsSubProcessor.missingcastbrackets.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 8, image);
			proposal.ensureNoModifications();
			proposals.add(proposal);				
		}
	}

	private static void addParameterMissmatchProposals(IInvocationContext context, IProblemLocation problem, List similarElements, ASTNode invocationNode, List arguments, Collection proposals) throws CoreException {
		int nSimilarElements= similarElements.size();
		ITypeBinding[] argTypes= getArgumentTypes(arguments);
		if (argTypes == null || nSimilarElements == 0)  {
			return;
		}

		for (int i= 0; i < nSimilarElements; i++) {
			IMethodBinding elem = (IMethodBinding) similarElements.get(i);
			int diff= elem.getParameterTypes().length - argTypes.length;
			if (diff == 0) {
				int nProposals= proposals.size();
				doEqualNumberOfParameters(context, invocationNode, problem, arguments, argTypes, elem, proposals);
				if (nProposals != proposals.size()) {
					return; // only suggest for one method (avoid duplicated proposals)
				}
			} else if (diff > 0) {
				doMoreParameters(context, problem, invocationNode, arguments, argTypes, elem, proposals);
			} else {
				doMoreArguments(context, problem, invocationNode, arguments, argTypes, elem, proposals);
			}
		}
	}
	
	private static void doMoreParameters(IInvocationContext context, IProblemLocation problem, ASTNode invocationNode, List arguments, ITypeBinding[] argTypes, IMethodBinding methodBinding, Collection proposals) throws CoreException {
		ITypeBinding[] paramTypes= methodBinding.getParameterTypes();
		int k= 0, nSkipped= 0;
		int diff= paramTypes.length - argTypes.length;
		int[] indexSkipped= new int[diff];
		for (int i= 0; i < paramTypes.length; i++) {
			if (k < argTypes.length && TypeRules.canAssign(argTypes[k], paramTypes[i])) {
				k++; // match
			} else {
				if (nSkipped >= diff) {
					return; // too different
				} 
				indexSkipped[nSkipped++]= i;
			}
		}
		ITypeBinding declaringType= methodBinding.getDeclaringClass();
		ICompilationUnit cu= context.getCompilationUnit();
		CompilationUnit astRoot= context.getASTRoot();
			
		// add arguments
		{			
			String[] arg= new String[] { getMethodSignature(methodBinding, false) };
			String label;
			if (diff == 1) {
				label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.addargument.description", arg); //$NON-NLS-1$
			} else {
				label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.addarguments.description", arg); //$NON-NLS-1$
			}			
			AddArgumentCorrectionProposal proposal= new AddArgumentCorrectionProposal(label, context.getCompilationUnit(), invocationNode, indexSkipped, paramTypes, 8);
			proposal.setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD));
			proposal.ensureNoModifications();
			proposals.add(proposal);				
		}
		
		// remove parameters
		if (!declaringType.isFromSource()) {
			return;
		}
		
		ICompilationUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, declaringType);
		if (targetCU != null) {
			ChangeDescription[] changeDesc= new ChangeDescription[paramTypes.length];
			ITypeBinding[] changedTypes= new ITypeBinding[diff];
			for (int i= diff - 1; i >= 0; i--) {
				int idx= indexSkipped[i];
				changeDesc[idx]= new RemoveDescription();
				changedTypes[i]= paramTypes[idx];
			}
			String[] arg= new String[] { getMethodSignature(methodBinding, !cu.equals(targetCU)), getTypeNames(changedTypes) };
			String label;
			if (methodBinding.isConstructor()) {
				if (diff == 1) {
					label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.removeparam.constr.description", arg); //$NON-NLS-1$
				} else {
					label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.removeparams.constr.description", arg); //$NON-NLS-1$
				}
			} else {
				if (diff == 1) {
					label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.removeparam.description", arg); //$NON-NLS-1$
				} else {
					label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.removeparams.description", arg); //$NON-NLS-1$
				}					
			}
		
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_REMOVE);
			ChangeMethodSignatureProposal proposal= new ChangeMethodSignatureProposal(label, targetCU, invocationNode, methodBinding, changeDesc, 5, image);
			proposals.add(proposal);
		}
	}
	
	private static String getTypeNames(ITypeBinding[] types) {
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < types.length; i++) {
			if (i > 0) {
				buf.append(", "); //$NON-NLS-1$
			}
			buf.append(types[i].getName());
		}
		return buf.toString();
	}
	
	private static String getArgumentName(ICompilationUnit cu, List arguments, int index) {
		String def= String.valueOf(index + 1);
		
		ASTNode expr= (ASTNode) arguments.get(index);
		if (expr.getLength() > 18) {
			return def;
		}
		try {
			String str= cu.getBuffer().getText(expr.getStartPosition(), expr.getLength());
			for (int i= 0; i < str.length(); i++) {
				if (Strings.isLineDelimiterChar(str.charAt(i))) {
					return def;
				}
			}
			ASTMatcher matcher= new ASTMatcher();
			for (int i= 0; i < arguments.size(); i++) {
				if (i != index && matcher.safeSubtreeMatch(expr, arguments.get(i))) {
					return def;
				}
			}
			return '\'' + str + '\'';
			
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return def;
	}

	private static void doMoreArguments(IInvocationContext context, IProblemLocation problem, ASTNode invocationNode, List arguments, ITypeBinding[] argTypes, IMethodBinding methodBinding, Collection proposals) throws CoreException {
		ITypeBinding[] paramTypes= methodBinding.getParameterTypes();
		int k= 0, nSkipped= 0;
		int diff= argTypes.length - paramTypes.length;
		int[] indexSkipped= new int[diff];
		for (int i= 0; i < argTypes.length; i++) {
			if (k < paramTypes.length && TypeRules.canAssign(argTypes[i], paramTypes[k])) {
				k++; // match
			} else {
				if (nSkipped >= diff) {
					return; // too different
				} 
				indexSkipped[nSkipped++]= i;
			}
		}
		ITypeBinding declaringType= methodBinding.getDeclaringClass();
		ICompilationUnit cu= context.getCompilationUnit();
		CompilationUnit astRoot= context.getASTRoot();
		
	
		// remove arguments
		{
			ASTNode selectedNode= problem.getCoveringNode(astRoot);
			ASTRewrite rewrite= new ASTRewrite(selectedNode.getParent());
			
			for (int i= diff - 1; i >= 0; i--) {
				rewrite.markAsRemoved((Expression) arguments.get(indexSkipped[i]));
			}
			String[] arg= new String[] { getMethodSignature(methodBinding, false) };
			String label;
			if (diff == 1) {
				label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.removeargument.description", arg); //$NON-NLS-1$
			} else {
				label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.removearguments.description", arg); //$NON-NLS-1$
			}			
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_REMOVE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 8, image);
			proposal.ensureNoModifications();
			proposals.add(proposal);				
		}
		
		// add parameters
		if (!declaringType.isFromSource()) {
			return;
		}
		ICompilationUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, declaringType);
		if (targetCU != null) {
			boolean isDifferentCU= !cu.equals(targetCU);
			
			if (isDifferentCU && isImplicitConstructor(methodBinding, targetCU)) {
				return;
			}
			
			ChangeDescription[] changeDesc= new ChangeDescription[argTypes.length];
			ITypeBinding[] changeTypes= new ITypeBinding[diff];
			for (int i= diff - 1; i >= 0; i--) {
				int idx= indexSkipped[i];
				Expression arg= (Expression) arguments.get(idx);
				String name= arg instanceof SimpleName ? ((SimpleName) arg).getIdentifier() : null;
				ITypeBinding newType= Bindings.normalizeTypeBinding(argTypes[idx]);
				if (newType == null) {
					newType= astRoot.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
				}
				changeDesc[idx]= new InsertDescription(newType, name);
				changeTypes[i]= newType;
			}
			String[] arg= new String[] { getMethodSignature(methodBinding, isDifferentCU), getTypeNames(changeTypes) };
			String label;
			if (methodBinding.isConstructor()) {
				if (diff == 1) {
					label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.addparam.constr.description", arg); //$NON-NLS-1$
				} else {
					label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.addparams.constr.description", arg); //$NON-NLS-1$
				}
			} else {
				if (diff == 1) {
					label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.addparam.description", arg); //$NON-NLS-1$
				} else {
					label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.addparams.description", arg); //$NON-NLS-1$
				}
			}	
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD);
			ChangeMethodSignatureProposal proposal= new ChangeMethodSignatureProposal(label, targetCU, invocationNode, methodBinding, changeDesc, 5, image);
			proposals.add(proposal);
		}
	}
	
	private static boolean isImplicitConstructor(IMethodBinding meth, ICompilationUnit targetCU) {
		if (meth.isConstructor() && meth.getParameterTypes().length == 0) {
			IMethodBinding[] bindings= meth.getDeclaringClass().getDeclaredMethods();
			// implicit constructors must be the only constructor
			for (int i= 0; i < bindings.length; i++) {
				IMethodBinding curr= bindings[i];
				if (curr.isConstructor() && curr != meth) {
					return false;
				}
			}
			CompilationUnit unit= AST.parsePartialCompilationUnit(targetCU, 0, true);
			return unit.findDeclaringNode(meth.getKey()) == null;
		}
		return false;		
	}
	
	
	private static String getMethodSignature(IMethodBinding binding, boolean inOtherCU) {
		StringBuffer buf= new StringBuffer();
		if (inOtherCU && !binding.isConstructor()) {
			buf.append(binding.getDeclaringClass().getName()).append('.');
		}
		buf.append(binding.getName());
		return getMethodSignature(buf.toString(), binding.getParameterTypes());
	}
	
	private static String getMethodSignature(String name, List args) {
		ITypeBinding[] params= new ITypeBinding[args.size()];
		for (int i= 0; i < args.size(); i++) {
			Expression expr= (Expression) args.get(i);
			ITypeBinding curr= Bindings.normalizeTypeBinding(expr.resolveTypeBinding());
			if (curr == null) {
				curr= expr.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
			}
			params[i]= curr;
		}
		return getMethodSignature(name, params);
	}
	
	
	private static String getMethodSignature(String name, ITypeBinding[] params) {
		StringBuffer buf= new StringBuffer();
		buf.append(name).append('(');
		for (int i= 0; i < params.length; i++) {
			if (i > 0) {
				buf.append(", "); //$NON-NLS-1$
			}
			buf.append(params[i].getName());
		}
		buf.append(')');
		return buf.toString();
	}	
	

	private static void doEqualNumberOfParameters(IInvocationContext context, ASTNode invocationNode, IProblemLocation problem, List arguments, ITypeBinding[] argTypes, IMethodBinding methodBinding, Collection proposals) throws CoreException {
		ITypeBinding[] paramTypes= methodBinding.getParameterTypes();
		int[] indexOfDiff= new int[paramTypes.length];
		int nDiffs= 0;
		for (int n= 0; n < argTypes.length; n++) {
			if (!TypeRules.canAssign(argTypes[n], paramTypes[n])) {
				indexOfDiff[nDiffs++]= n;
			}
		}
		ITypeBinding declaringType= methodBinding.getDeclaringClass();
		ICompilationUnit cu= context.getCompilationUnit();
		CompilationUnit astRoot= context.getASTRoot();
		
		ASTNode nameNode= problem.getCoveringNode(astRoot);
		if (nameNode == null) {
			return;
		}
		
		if (nDiffs == 0) {
			if (nameNode.getParent() instanceof MethodInvocation) {
				MethodInvocation inv= (MethodInvocation) nameNode.getParent();
				if (inv.getExpression() == null) {
					addQualifierToOuterProposal(context, inv, methodBinding, proposals);
				}
			}
			return;
		}
		
		if (nDiffs == 1) { // one argument missmatching: try to fix
			int idx= indexOfDiff[0];
			Expression nodeToCast= (Expression) arguments.get(idx);
			ITypeBinding castType= paramTypes[idx];
			ITypeBinding binding= nodeToCast.resolveTypeBinding();
			if (binding == null || TypeRules.canCast(castType, binding)) {
				String castTypeName= castType.getQualifiedName();
				ASTRewriteCorrectionProposal proposal= LocalCorrectionsSubProcessor.createCastProposal(context, castTypeName, nodeToCast, 6);
				String[] arg= new String[] { getArgumentName(cu, arguments, idx), castTypeName};
				proposal.setDisplayName(CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.addargumentcast.description", arg)); //$NON-NLS-1$
				proposal.ensureNoModifications();
				proposals.add(proposal);
			}
		}
		if (nDiffs == 2) { // try to swap
			int idx1= indexOfDiff[0];
			int idx2= indexOfDiff[1];
			boolean canSwap= TypeRules.canAssign(argTypes[idx1], paramTypes[idx2]) && TypeRules.canAssign(argTypes[idx2], paramTypes[idx1]);
			 if (canSwap) {
				Expression arg1= (Expression) arguments.get(idx1);
				Expression arg2= (Expression) arguments.get(idx2);
				
				ASTRewrite rewrite= new ASTRewrite(arg1.getParent());
				rewrite.markAsReplaced(arg1, rewrite.createCopy(arg2));
				rewrite.markAsReplaced(arg2, rewrite.createCopy(arg1));
				{
					String[] arg= new String[] { getArgumentName(cu, arguments, idx1), getArgumentName(cu, arguments, idx2) };
					String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.swaparguments.description", arg); //$NON-NLS-1$
					Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
					ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 8, image);
					proposal.ensureNoModifications();
					proposals.add(proposal);					
				}
				
				if (declaringType.isFromSource()) {
					ICompilationUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, declaringType);
					if (targetCU != null) {
						ChangeDescription[] changeDesc= new ChangeDescription[paramTypes.length];
						for (int i= 0; i < nDiffs; i++) {
							changeDesc[idx1]= new SwapDescription(idx2);
						}
						ITypeBinding[] swappedTypes= new ITypeBinding[] { paramTypes[idx1], paramTypes[idx2] };
						String[] args=  new String[] { getMethodSignature(methodBinding, !targetCU.equals(cu)), getTypeNames(swappedTypes) };
						String label;
						if (methodBinding.isConstructor()) {
							label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.swapparams.constr.description", args); //$NON-NLS-1$
						} else {
							label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.swapparams.description", args); //$NON-NLS-1$
						}
						Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
						ChangeMethodSignatureProposal proposal= new ChangeMethodSignatureProposal(label, targetCU, invocationNode, methodBinding, changeDesc, 5, image);
						proposals.add(proposal);
					}
				}
				return;
			}
		}
		
		if (declaringType.isFromSource()) {
			ICompilationUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, declaringType);
			if (targetCU != null) {
				ChangeDescription[] changeDesc= new ChangeDescription[paramTypes.length];
				for (int i= 0; i < nDiffs; i++) {
					int diffIndex= indexOfDiff[i];
					Expression arg= (Expression) arguments.get(diffIndex);
					String name= arg instanceof SimpleName ? ((SimpleName) arg).getIdentifier() : null;					
					changeDesc[diffIndex]= new EditDescription(argTypes[diffIndex], name);
				}
				String[] args=  new String[] { getMethodSignature(methodBinding, !targetCU.equals(cu)), getMethodSignature(methodBinding.getName(), arguments) };
				String label;
				if (methodBinding.isConstructor()) {
					label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.changeparamsignature.constr.description", args); //$NON-NLS-1$
				} else {
					label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.changeparamsignature.description", args); //$NON-NLS-1$
				}
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				ChangeMethodSignatureProposal proposal= new ChangeMethodSignatureProposal(label, targetCU, invocationNode, methodBinding, changeDesc, 7, image);
				proposals.add(proposal);
			}
		}
	}	
	
	private static ITypeBinding[] getArgumentTypes(List arguments) {
		ITypeBinding[] res= new ITypeBinding[arguments.size()];
		for (int i= 0; i < res.length; i++) {
			Expression expression= (Expression) arguments.get(i);
 			ITypeBinding curr= expression.resolveTypeBinding();
			if (curr == null) {
				return null;
			}
			curr= Bindings.normalizeTypeBinding(curr);
			if (curr == null) {
				curr= expression.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
			}
			res[i]= curr;
		}
		return res;
	}
	
	private static void addQualifierToOuterProposal(IInvocationContext context, MethodInvocation invocationNode, IMethodBinding binding, Collection proposals) throws CoreException {
		ITypeBinding declaringType= binding.getDeclaringClass();
		ITypeBinding parentType= Bindings.getBindingOfParentType(invocationNode);
		ITypeBinding currType= parentType;
		
		boolean isInstanceMethod= !Modifier.isStatic(binding.getModifiers());
		
		while (currType != null && !Bindings.isSuperType(declaringType, currType)) {
			if (isInstanceMethod && Modifier.isStatic(currType.getModifiers())) {
				return;
			}
			currType= currType.getDeclaringClass();
		}
		if (currType == null || currType == parentType) {
			return;
		}
		
		ASTRewrite rewrite= new ASTRewrite(invocationNode.getParent());
		AST ast= invocationNode.getAST();
		
		String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.changetoouter.description", currType.getName()); //$NON-NLS-1$	
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 8, image);
		proposal.ensureNoModifications();
		proposals.add(proposal);				
		
		String qualifier=  proposal.addImport(currType);
		Name name= ASTNodeFactory.newName(ast, qualifier);
		
		Expression newExpression;
		if (isInstanceMethod) {
			ThisExpression expr= ast.newThisExpression();
			expr.setQualifier(name);
			newExpression= expr;		
		} else {
			newExpression= name;
		}
		
		rewrite.markAsInsert(invocationNode, ASTNodeConstants.EXPRESSION, newExpression, null);
	}
	

	public static void getConstructorProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();
		
		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(astRoot);
		if (selectedNode == null) {
			return;
		}
		
		ITypeBinding targetBinding= null;
		List arguments= null;
		IMethodBinding recursiveConstructor= null;
		
		int type= selectedNode.getNodeType();
		if (type == ASTNode.CLASS_INSTANCE_CREATION) {
			ClassInstanceCreation creation= (ClassInstanceCreation) selectedNode;
			
			IBinding binding= creation.getName().resolveBinding();
			if (binding instanceof ITypeBinding) {
				targetBinding= (ITypeBinding) binding;
				arguments= creation.arguments();		
			}
		} else if (type == ASTNode.SUPER_CONSTRUCTOR_INVOCATION) {
			ITypeBinding typeBinding= Bindings.getBindingOfParentType(selectedNode);
			if (typeBinding != null && !typeBinding.isAnonymous()) {
				targetBinding= typeBinding.getSuperclass();
				arguments= ((SuperConstructorInvocation) selectedNode).arguments();
			}
		} else if (type == ASTNode.CONSTRUCTOR_INVOCATION) {
			ITypeBinding typeBinding= Bindings.getBindingOfParentType(selectedNode);
			if (typeBinding != null && !typeBinding.isAnonymous()) {
				targetBinding= typeBinding;
				arguments= ((ConstructorInvocation) selectedNode).arguments();
				recursiveConstructor= ASTResolving.findParentMethodDeclaration(selectedNode).resolveBinding();
			}			
		}
		if (targetBinding == null) {
			return;
		}
		IMethodBinding[] methods= targetBinding.getDeclaredMethods();
		ArrayList similarElements= new ArrayList();
		for (int i= 0; i < methods.length; i++) {
			IMethodBinding curr= methods[i];
			if (curr.isConstructor() && recursiveConstructor != curr) {
				similarElements.add(curr); // similar elements can contain a implicit default constructor
			}
		}
		
		addParameterMissmatchProposals(context, problem, similarElements, selectedNode, arguments, proposals);
		
		if (targetBinding.isFromSource()) {
			ICompilationUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, targetBinding);
			if (targetCU != null) {
				String[] args= new String[] { getMethodSignature( targetBinding.getName(), arguments) };
				String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createconstructor.description", args); //$NON-NLS-1$
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC);
				proposals.add(new NewMethodCompletionProposal(label, targetCU, selectedNode, arguments, targetBinding, 5, image));
			}
		}
	}
	
	public static void getAmbiguosTypeReferenceProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		final ICompilationUnit cu= context.getCompilationUnit();
		int offset= problem.getOffset();
		int len= problem.getLength();
		
		IJavaElement[] elements= cu.codeSelect(offset, len);
		for (int i= 0; i < elements.length; i++) {
			IJavaElement curr= elements[i];
			if (curr instanceof IType) {
				String qualifiedTypeName= JavaModelUtil.getFullyQualifiedName((IType) curr);
				String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.importexplicit.description", qualifiedTypeName); //$NON-NLS-1$
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_IMPDECL);
				CUCorrectionProposal proposal= new CUCorrectionProposal(label, cu, 5, image);
				TextBuffer buffer= null;
				try {
					buffer= TextBuffer.acquire((IFile)WorkingCopyUtil.getOriginal(cu).getResource());
					ImportRewrite importRewrite= new ImportRewrite(cu, JavaPreferencesSettings.getCodeGenerationSettings());
					importRewrite.addImport(qualifiedTypeName);
					importRewrite.setFindAmbiguosImports(true);
					proposal.getRootTextEdit().addChild(importRewrite.createEdit(buffer));
					proposals.add(proposal);
				} finally {
					if (buffer != null)
						TextBuffer.release(buffer);
				}			
			}
		}
	}	
	
}
