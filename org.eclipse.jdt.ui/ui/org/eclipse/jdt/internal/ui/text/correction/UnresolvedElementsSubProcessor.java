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
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.dom.Binding2JavaModel;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class UnresolvedElementsSubProcessor {
		
	private static ProblemPosition createProblemPosition(ASTNode node, ProblemPosition problemPos) {
		return new ProblemPosition(node.getStartPosition(), node.getLength(), problemPos.getId(), problemPos.getArguments(), problemPos.getCompilationUnit());
	}
	
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


		// corrections
		SimilarElement[] elements= SimilarElementsRequestor.findSimilarElement(cu, node, similarNodeKind);
		for (int i= 0; i < elements.length; i++) {
			SimilarElement curr= elements[i];
			if ((curr.getKind() & SimilarElementsRequestor.VARIABLES) != 0) {
				String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.changevariable.description", curr.getName()); //$NON-NLS-1$
				proposals.add(new ReplaceCorrectionProposal(label, problemPos.getCompilationUnit(), node.getStartPosition(), node.getLength(), curr.getName(), 3));
			}
		}
		// add type proposals
		if ((similarNodeKind & SimilarElementsRequestor.ALL_TYPES) != 0) {
			addSimilarTypeProposals(elements, cu, node, proposals);
			addNewTypeProposals(cu, node, SimilarElementsRequestor.REF_TYPES, proposals);
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
	
	public static void getTypeProposals(ProblemPosition problemPos, int kind, ArrayList proposals) throws CoreException {
		ICompilationUnit cu= problemPos.getCompilationUnit();
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		ASTNode selectedNode= ASTResolving.findSelectedNode(astRoot, problemPos.getOffset(), problemPos.getLength());
		
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
		addSimilarTypeProposals(elements, cu, node, proposals);
		
		// add type
		addNewTypeProposals(cu, node, kind, proposals);
	}

	private static void addSimilarTypeProposals(SimilarElement[] elements, ICompilationUnit cu, Name node, ArrayList proposals) throws JavaModelException {
		// try to resolve type in context -> highest severity
		String resolvedTypeName= null;
		ITypeBinding binding= ASTResolving.guessBindingForTypeReference(node, false);
		if (binding != null) {
			if (binding.isArray()) {
				binding= binding.getElementType();
			}
			resolvedTypeName= Bindings.getFullyQualifiedName(binding);
			proposals.add(createTypeRefChangeProposal(cu, resolvedTypeName, node, 5));
		}
		// add all similar elements
		for (int i= 0; i < elements.length; i++) {
			SimilarElement elem= elements[i];
			if ((elem.getKind() & SimilarElementsRequestor.ALL_TYPES) != 0) {
				String fullName= elem.getName();
				if (!fullName.equals(resolvedTypeName)) {
					proposals.add(createTypeRefChangeProposal(cu, fullName, node, 3));
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
			proposal.setRelevance(relevance + 1);
		} else {			
			root.add(SimpleTextEdit.createReplace(node.getStartPosition(), node.getLength(), simpleName)); //$NON-NLS-1$
			proposal.setDisplayName(CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.changetype.description", simpleName)); //$NON-NLS-1$
			proposal.setRelevance(relevance);
		}
		return proposal;
	}

	private static void addNewTypeProposals(ICompilationUnit cu, Name node, int kind, ArrayList proposals) {
		String typeName= ASTResolving.getSimpleName(node);
		while (true) {
			String addedCUName= typeName + ".java"; //$NON-NLS-1$
			if (!JavaConventions.validateCompilationUnitName(addedCUName).matches(IStatus.ERROR)) {
				int severity= 0;
				ICompilationUnit addedCU;
				if (node.isSimpleName()) {
					addedCU= ((IPackageFragment) cu.getParent()).getCompilationUnit(addedCUName);
				} else {
					IPackageFragmentRoot root= (IPackageFragmentRoot) cu.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
					String packName= ASTResolving.getQualifier(node);
					addedCU= root.getPackageFragment(packName).getCompilationUnit(addedCUName);
					typeName= JavaModelUtil.concatenateName(packName, typeName);
					if (Character.isLowerCase(packName.charAt(0))) {
						severity++;
					}					
				}
				if (Character.isUpperCase(addedCUName.charAt(0))) {
					severity++;
				}
				if (!addedCU.exists()) {
					if ((kind & SimilarElementsRequestor.CLASSES) != 0) {
						String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createclassusingwizard.description", typeName); //$NON-NLS-1$
			            proposals.add(new NewCUCompletionUsingWizardProposal(label, cu, node, true, severity));
					}
					if ((kind & SimilarElementsRequestor.INTERFACES) != 0) {
						String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createinterfaceusingwizard.description", typeName); //$NON-NLS-1$
			            proposals.add(new NewCUCompletionUsingWizardProposal(label, cu, node, false, severity));
					}				
				}
			}
			if (node.isSimpleName()) {
				return;
			}
			node= ((QualifiedName) node).getQualifier();
			typeName= ASTResolving.getSimpleName(node);
			if (Character.isLowerCase(typeName.charAt(kind))) {
				return;
			}
		}
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
