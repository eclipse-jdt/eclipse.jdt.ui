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
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.dom.Binding2JavaModel;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

public class UnresolvedElementsSubProcessor {
	
	public static void getVariableProposals(ProblemPosition problemPos, ArrayList proposals) throws CoreException {
		
		ICompilationUnit cu= problemPos.getCompilationUnit();
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		ASTNode selectedNode= ASTResolving.findSelectedNode(astRoot, problemPos.getOffset(), problemPos.getLength());
		if (selectedNode == null) {
			return;
		}
		
		// type that defines the variable, null if local
		ITypeBinding binding= null;

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
				proposals.add(new ReplaceCorrectionProposal(label, problemPos.getCompilationUnit(), node.getStartPosition(), node.getLength(), curr.getName(), 3));
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
		ICompilationUnit targetCU= getCompilationUnit(binding, cu, astRoot);
		ITypeBinding senderBinding= binding != null ? binding : ASTResolving.getBindingOfParentType(node);
		String label;
		Image image;
		if (cu.equals(targetCU)) {
			label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createfield.description", simpleName.getIdentifier()); //$NON-NLS-1$
			image= JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PRIVATE);
		} else {
			label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createfield.other.description", new Object[] { simpleName.getIdentifier(), targetCU.getElementName() } ); //$NON-NLS-1$
			image= JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PUBLIC);
		}
		proposals.add(new NewVariableCompletionProposal(label, targetCU, NewVariableCompletionProposal.FIELD, simpleName, senderBinding, 2, image));
		
		if (binding == null) {
			BodyDeclaration bodyDeclaration= ASTResolving.findParentBodyDeclaration(node);
			int type= bodyDeclaration.getNodeType();
			if (type == ASTNode.METHOD_DECLARATION) {
				label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createparameter.description", simpleName.getIdentifier()); //$NON-NLS-1$
				image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
				proposals.add(new NewVariableCompletionProposal(label, cu, NewVariableCompletionProposal.PARAM, simpleName, null, 1, image));
			}
			if (type == ASTNode.METHOD_DECLARATION || type == ASTNode.INITIALIZER) {
				label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createlocal.description", simpleName.getIdentifier()); //$NON-NLS-1$
				image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
				proposals.add(new NewVariableCompletionProposal(label, cu, NewVariableCompletionProposal.LOCAL, simpleName, null, 1, image));
			}
		}
		
	}
	
	public static void getTypeProposals(ProblemPosition problemPos, ArrayList proposals) throws CoreException {
		ICompilationUnit cu= problemPos.getCompilationUnit();
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		ASTNode selectedNode= ASTResolving.findSelectedNode(astRoot, problemPos.getOffset(), problemPos.getLength());
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

	private static void addSimilarTypeProposals(SimilarElement[] elements, ICompilationUnit cu, Name node, int relevance, ArrayList proposals) throws JavaModelException {
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
			proposal.setRelevance(relevance + 2);
		} else {			
			root.add(SimpleTextEdit.createReplace(node.getStartPosition(), node.getLength(), simpleName)); //$NON-NLS-1$
			proposal.setDisplayName(CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.changetype.description", simpleName)); //$NON-NLS-1$
			proposal.setRelevance(relevance);
		}
		return proposal;
	}

	private static void addNewTypeProposals(ICompilationUnit cu, Name refNode, int kind, int relevance, ArrayList proposals) throws JavaModelException {
		Name node= refNode;
		do {
			String typeName= ASTResolving.getSimpleName(node);
			Name qualifier= null;
			// only propose to create types for qualifiers when the name starts with upper case
			boolean isPossibleName= Character.isUpperCase(typeName.charAt(0)) || (node == refNode);
			if (isPossibleName) {
				String addedCUName= typeName + ".java"; //$NON-NLS-1$
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
				if (enclosingPackage != null && !enclosingPackage.getCompilationUnit(typeName + ".java").exists()) {
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
	
	public static void getMethodProposals(ProblemPosition problemPos, boolean needsNewName, ArrayList proposals) throws CoreException {

		ICompilationUnit cu= problemPos.getCompilationUnit();
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		ASTNode selectedNode= ASTResolving.findCoveringNode(astRoot, problemPos.getOffset(), problemPos.getLength());
		
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
		for (int i= 0; i < elements.length; i++) {
			String curr= elements[i].getName();
			if (curr.equals(methodName) && needsNewName) {
				continue;
			}
			
			String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.changemethod.description", curr); //$NON-NLS-1$
			proposals.add(new ReplaceCorrectionProposal(label, problemPos, curr, 2));
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
			ICompilationUnit targetCU= getCompilationUnit(binding, cu, astRoot);
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
			}
		}
	}
	

	public static void getConstructorProposals(ProblemPosition problemPos, ArrayList proposals) throws CoreException {
		ICompilationUnit cu= problemPos.getCompilationUnit();
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		ASTNode selectedNode= ASTResolving.findCoveringNode(astRoot, problemPos.getOffset(), problemPos.getLength());
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
			ICompilationUnit targetCU= getCompilationUnit(targetBinding, cu, astRoot);
			if (targetCU != null) {
				String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createconstructor.description", targetBinding.getName()); //$NON-NLS-1$
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC);
				proposals.add(new NewMethodCompletionProposal(label, targetCU, selectedNode, arguments, targetBinding, 1, image));
			}
		}
	}
	
	private static ICompilationUnit getCompilationUnit(ITypeBinding binding, ICompilationUnit cu, CompilationUnit astRoot) throws JavaModelException {
		if (binding != null && astRoot.findDeclaringNode(binding) == null) {
			ICompilationUnit targetCU= Binding2JavaModel.findCompilationUnit(binding, cu.getJavaProject());
			if (targetCU != null) {
				return JavaModelUtil.toWorkingCopy(targetCU);
			}
			return null;
		}
		return cu;
	}

}
