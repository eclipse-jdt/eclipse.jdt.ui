/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
  *     Renaud Waldura &lt;renaud+eclipse@waldura.com&gt; - New class/interface with wizard
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class UnresolvedElementsSubProcessor {
	
	public static void getVariableProposals(ICorrectionContext context, List proposals) throws CoreException {
		
		ICompilationUnit cu= context.getCompilationUnit();
		
		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= context.getCoveredNode();
		if (selectedNode == null) {
			return;
		}
		
		// type that defines the variable, null if local
		ITypeBinding binding= null;
		ITypeBinding declaringTypeBinding= ASTResolving.getBindingOfParentType(selectedNode);
		if (declaringTypeBinding == null) {
			return;
		}
		

		// possible kind of the node
		int similarNodeKind= SimilarElementsRequestor.VARIABLES;
		
		Name node= null;
		if (selectedNode instanceof SimpleName) {
			node= (SimpleName) selectedNode;
			ASTNode parent= node.getParent();
			if (parent instanceof MethodInvocation && node.equals(((MethodInvocation)parent).getExpression())) {
				similarNodeKind |= SimilarElementsRequestor.CLASSES;
			}
		} else if (selectedNode instanceof QualifiedName) {
			QualifiedName qualifierName= (QualifiedName) selectedNode;
			ITypeBinding qualifierBinding= qualifierName.getQualifier().resolveTypeBinding();
			if (qualifierBinding != null) {
				node= qualifierName.getName();
				binding= qualifierBinding;
			} else {
				node= qualifierName.getQualifier();
				if (node.isSimpleName()) {
					similarNodeKind |= SimilarElementsRequestor.REF_TYPES;
				} else {
					similarNodeKind= SimilarElementsRequestor.REF_TYPES;
				}
			}
		} else if (selectedNode instanceof FieldAccess) {
			FieldAccess access= (FieldAccess) selectedNode;
			Expression expression= access.getExpression();
			if (expression != null) {
				binding= expression.resolveTypeBinding();
				if (binding != null) {
					node= access.getName();
				}
			}
		} else if (selectedNode instanceof SuperFieldAccess) {
			binding= declaringTypeBinding.getSuperclass();	
		} else if (selectedNode instanceof SimpleType) {
			similarNodeKind= SimilarElementsRequestor.REF_TYPES;
			node= ((SimpleType) selectedNode).getName();
		}
		if (node == null) {
			return;
		}
		
		// avoid corrections like int i= i;
		String assignedName= null;
		ASTNode parent= node.getParent();
		if (parent.getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
			assignedName= ((VariableDeclarationFragment) parent).getName().getIdentifier();
		}

		// corrections
		SimilarElement[] elements= SimilarElementsRequestor.findSimilarElement(cu, node, similarNodeKind);
		for (int i= 0; i < elements.length; i++) {
			SimilarElement curr= elements[i];
			if ((curr.getKind() & SimilarElementsRequestor.VARIABLES) != 0 && !curr.getName().equals(assignedName)) {
				String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.changevariable.description", curr.getName()); //$NON-NLS-1$
				proposals.add(new ReplaceCorrectionProposal(label, cu, node.getStartPosition(), node.getLength(), curr.getName(), 3));
			}
		}
		// add type proposals
		if ((similarNodeKind & SimilarElementsRequestor.ALL_TYPES) != 0) {
			int relevance= Character.isUpperCase(ASTResolving.getSimpleName(node).charAt(0)) ? 3 : 0;
			addSimilarTypeProposals(elements, cu, node, relevance + 1, proposals);
			addNewTypeProposals(cu, node, SimilarElementsRequestor.REF_TYPES, relevance, proposals);
		}
		
		if ((similarNodeKind & SimilarElementsRequestor.VARIABLES) == 0) {
			return;
		}
		
		SimpleName simpleName= node.isSimpleName() ? (SimpleName) node : ((QualifiedName) node).getName();

		// new variables
		ICompilationUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, binding);
		ITypeBinding senderBinding= binding != null ? binding : declaringTypeBinding;

		if (senderBinding.isFromSource() && targetCU != null && JavaModelUtil.isEditable(targetCU)) {
			String label;
			Image image;
			if (binding == null) {
				label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createfield.description", simpleName.getIdentifier()); //$NON-NLS-1$
				image= JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PRIVATE);
			} else {
				label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createfield.other.description", new Object[] { simpleName.getIdentifier(), binding.getName() } ); //$NON-NLS-1$
				image= JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PUBLIC);
			}
			proposals.add(new NewVariableCompletionProposal(label, targetCU, NewVariableCompletionProposal.FIELD, simpleName, senderBinding, 2, image));
			if (binding == null && senderBinding.isAnonymous()) {
				ASTNode anonymDecl= astRoot.findDeclaringNode(senderBinding);
				if (anonymDecl != null) {
					senderBinding= ASTResolving.getBindingOfParentType(anonymDecl.getParent());
					if (!senderBinding.isAnonymous()) {
						label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createfield.other.description", new Object[] { simpleName.getIdentifier(), senderBinding.getName() } ); //$NON-NLS-1$
						image= JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PUBLIC);
						proposals.add(new NewVariableCompletionProposal(label, targetCU, NewVariableCompletionProposal.FIELD, simpleName, senderBinding, 2, image));
					}
				}
			}
		}
		if (binding == null) {
			BodyDeclaration bodyDeclaration= ASTResolving.findParentBodyDeclaration(node);
			int type= bodyDeclaration.getNodeType();
			if (type == ASTNode.METHOD_DECLARATION) {
				String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createparameter.description", simpleName.getIdentifier()); //$NON-NLS-1$
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
				proposals.add(new NewVariableCompletionProposal(label, cu, NewVariableCompletionProposal.PARAM, simpleName, null, 1, image));
			}
			if (type == ASTNode.METHOD_DECLARATION || type == ASTNode.INITIALIZER) {
				String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createlocal.description", simpleName.getIdentifier()); //$NON-NLS-1$
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
				proposals.add(new NewVariableCompletionProposal(label, cu, NewVariableCompletionProposal.LOCAL, simpleName, null, 3, image));
			}
		}
		
	}
	
	public static void getTypeProposals(ICorrectionContext context, List proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();
		
		ASTNode selectedNode= context.getCoveringNode();
		if (selectedNode == null) {
			return;
		}
		int kind= SimilarElementsRequestor.ALL_TYPES;
		
		ASTNode parent= selectedNode.getParent();
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
					kind= SimilarElementsRequestor.REF_TYPES | SimilarElementsRequestor.VOIDTYPE;
				}
				break;
			case ASTNode.INSTANCEOF_EXPRESSION:
				kind= SimilarElementsRequestor.REF_TYPES;
				break;
			case ASTNode.THROW_STATEMENT:
			case ASTNode.CLASS_INSTANCE_CREATION:
				kind= SimilarElementsRequestor.CLASSES;
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
		
		SimilarElement[] elements= SimilarElementsRequestor.findSimilarElement(cu, node, kind);
		addSimilarTypeProposals(elements, cu, node, 3, proposals);
		
		// add type
		addNewTypeProposals(cu, node, kind, 0, proposals);
	}

	private static void addSimilarTypeProposals(SimilarElement[] elements, ICompilationUnit cu, Name node, int relevance, List proposals) throws JavaModelException {
		// try to resolve type in context -> highest severity
		String resolvedTypeName= null;
		ITypeBinding binding= ASTResolving.guessBindingForTypeReference(node);
		if (binding != null) {
			if (binding.isArray()) {
				binding= binding.getElementType();
			}
			resolvedTypeName= Bindings.getFullyQualifiedName(binding);
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

	private static CUCorrectionProposal createTypeRefChangeProposal(ICompilationUnit cu, String fullName, Name node, int relevance) throws JavaModelException {
		CUCorrectionProposal proposal= new CUCorrectionProposal("", cu, 0); //$NON-NLS-1$

		ImportEdit importEdit= new ImportEdit(cu, JavaPreferencesSettings.getCodeGenerationSettings());				
		String simpleName= importEdit.addImport(fullName);
		
		TextEdit root= proposal.getRootTextEdit();
		
		if (!importEdit.isEmpty()) {
			root.add(importEdit); //$NON-NLS-1$
		}
		if (node.isSimpleName() && simpleName.equals(((SimpleName) node).getIdentifier())) { // import only
			proposal.setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_IMPDECL));
			proposal.setDisplayName(CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.importtype.description", fullName)); //$NON-NLS-1$
			proposal.setRelevance(relevance + 20);
		} else {			
			root.add(SimpleTextEdit.createReplace(node.getStartPosition(), node.getLength(), simpleName)); //$NON-NLS-1$
			proposal.setDisplayName(CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.changetype.description", simpleName)); //$NON-NLS-1$
			proposal.setRelevance(relevance);
		}
		return proposal;
	}

	private static void addNewTypeProposals(ICompilationUnit cu, Name refNode, int kind, int relevance, List proposals) throws JavaModelException {
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
	
	public static void getMethodProposals(ICorrectionContext context, boolean needsNewName, List proposals) throws CoreException {

		ICompilationUnit cu= context.getCompilationUnit();
		
		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= context.getCoveringNode();
		
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
			
		// corrections
		SimilarElement[] elements= SimilarElementsRequestor.findSimilarElement(cu, nameNode, SimilarElementsRequestor.METHODS);
		
		ArrayList parameterMismatchs= new ArrayList();
		
		for (int i= 0; i < elements.length; i++) {
			String curr= elements[i].getName();
			if (curr.equals(methodName) && needsNewName) {
				//parameterMismatchs.add(elements[i]);
				continue;
			}
			
			String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.changemethod.description", curr); //$NON-NLS-1$
			proposals.add(new ReplaceCorrectionProposal(label, context.getCompilationUnit(), context.getOffset(), context.getLength(), curr, 2));
		}
		
		if (parameterMismatchs.size() == 1) {
			SimilarElement elem= (SimilarElement) parameterMismatchs.get(0);
			String[] paramTypes= elem.getParameterTypes();
			ITypeBinding[] argTypes= getArgumentTypes(arguments);
			if (paramTypes != null && argTypes != null) {
			}
		}
		
		// new method
		ITypeBinding binding= null;
		if (sender != null) {
			binding= sender.resolveTypeBinding();
		} else {
			binding= ASTResolving.getBindingOfParentType(invocationNode);
			if (isSuperInvocation && binding != null) {
				binding= binding.getSuperclass();
			}				
		}
		if (binding != null && binding.isFromSource()) {
			ICompilationUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, binding);
			if (targetCU != null) {			
				String label;
				Image image;
				if (cu.equals(targetCU)) {
					label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createmethod.description", methodName); //$NON-NLS-1$
					image= JavaPluginImages.get(JavaPluginImages.IMG_MISC_PRIVATE);
				} else {
					label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createmethod.other.description", new Object[] { methodName, targetCU.getElementName() } ); //$NON-NLS-1$
					image= JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC);
				}
				proposals.add(new NewMethodCompletionProposal(label, targetCU, invocationNode, arguments, binding, 1, image));
				
				if (binding.isAnonymous() && cu.equals(targetCU)) {
					ASTNode anonymDecl= astRoot.findDeclaringNode(binding);
					if (anonymDecl != null) {
						binding= ASTResolving.getBindingOfParentType(anonymDecl.getParent());
						if (!binding.isAnonymous()) {
							String[] args= new String[] { methodName, binding.getName() };
							label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createmethod.other.description", args); //$NON-NLS-1$
							image= JavaPluginImages.get(JavaPluginImages.IMG_MISC_PROTECTED);
							proposals.add(new NewMethodCompletionProposal(label, targetCU, invocationNode, arguments, binding, 1, image));
						}
					}
				}
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
			curr= ASTResolving.normalizeTypeBinding(curr);
			if (curr == null) {
				curr= expression.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
			}
			res[i]= curr;
		}
		return res;
	}

	public static void getConstructorProposals(ICorrectionContext context, List proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();
		
		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= context.getCoveringNode();
		if (selectedNode == null) {
			return;
		}
		
		ITypeBinding targetBinding= null;
		List arguments= null;
		
		int type= selectedNode.getNodeType();
		if (type == ASTNode.CLASS_INSTANCE_CREATION) {
			ClassInstanceCreation creation= (ClassInstanceCreation) selectedNode;
			
			IBinding binding= creation.getName().resolveBinding();
			if (binding instanceof ITypeBinding) {
				targetBinding= (ITypeBinding) binding;
				arguments= creation.arguments();		
			}
		} else if (type == ASTNode.SUPER_CONSTRUCTOR_INVOCATION) {
			ITypeBinding typeBinding= ASTResolving.getBindingOfParentType(selectedNode);
			if (typeBinding != null && !typeBinding.isAnonymous()) {
				targetBinding= typeBinding.getSuperclass();
				arguments= ((SuperConstructorInvocation) selectedNode).arguments();
			}
		} else if (type == ASTNode.CONSTRUCTOR_INVOCATION) {
			ITypeBinding typeBinding= ASTResolving.getBindingOfParentType(selectedNode);
			if (typeBinding != null && !typeBinding.isAnonymous()) {
				targetBinding= typeBinding;
				arguments= ((ConstructorInvocation) selectedNode).arguments();
			}			
		}
		if (targetBinding != null && targetBinding.isFromSource()) {
			ICompilationUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, targetBinding);
			if (targetCU != null) {
				String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createconstructor.description", targetBinding.getName()); //$NON-NLS-1$
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC);
				proposals.add(new NewMethodCompletionProposal(label, targetCU, selectedNode, arguments, targetBinding, 1, image));
			}
		}
	}
	
	public static void getAmbiguosTypeReferenceProposals(ICorrectionContext context, List proposals) throws CoreException {
		final ICompilationUnit cu= context.getCompilationUnit();
		int offset= context.getOffset();
		int len= context.getLength();
		
		IJavaElement[] elements= cu.codeSelect(offset, len);
		for (int i= 0; i < elements.length; i++) {
			IJavaElement curr= elements[i];
			if (curr instanceof IType) {
				String qualifiedTypeName= JavaModelUtil.getFullyQualifiedName((IType) curr);
				String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.importexplicit.description", qualifiedTypeName); //$NON-NLS-1$
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_IMPDECL);
				CUCorrectionProposal proposal= new CUCorrectionProposal(label, cu, 1, image);
				ImportEdit importEdit= new ImportEdit(cu, JavaPreferencesSettings.getCodeGenerationSettings());
				importEdit.addImport(qualifiedTypeName);
				importEdit.setFindAmbiguosImports(true);
				proposal.getRootTextEdit().add(importEdit);
				proposals.add(proposal);			
			}
		}
	}	
	
}
