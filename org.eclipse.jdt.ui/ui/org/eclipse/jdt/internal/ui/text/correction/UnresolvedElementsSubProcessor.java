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

import org.eclipse.jface.text.Position;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.dom.Binding2JavaModel;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class UnresolvedElementsSubProcessor {
	
	
	private static SimpleName getVariableNode(ASTNode selectedNode) {
		if (selectedNode instanceof SimpleName) {
			return (SimpleName) selectedNode;
		} else if (selectedNode instanceof QualifiedName) {
			QualifiedName qualifierName= (QualifiedName) selectedNode;
			Name qualifier= qualifierName.getQualifier();
			if (qualifier instanceof SimpleName) {
				SimpleName simpleQualifier= (SimpleName) qualifier;

			}
		} else if (selectedNode instanceof FieldAccess) {
			FieldAccess access= (FieldAccess) selectedNode;
			Expression expression= access.getExpression();
			if (expression == null || expression instanceof ThisExpression) {
				return access.getName();
			}
		}		
		return null;		
	}
	
	
	
	public static void getVariableProposals(ProblemPosition problemPos, ArrayList proposals) throws CoreException {
		
		ICompilationUnit cu= problemPos.getCompilationUnit();
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		ASTNode selectedNode= ASTResolving.findSelectedNode(astRoot, problemPos.getOffset(), problemPos.getLength());

		SimpleName node= null;
		if (selectedNode instanceof SimpleName) {
			node= (SimpleName) selectedNode;
		} else if (selectedNode instanceof QualifiedName) {
			QualifiedName qualifierName= (QualifiedName) selectedNode;
			Name qualifier= qualifierName.getQualifier();
			if (qualifier instanceof SimpleName) {
				SimpleName simpleQualifier= (SimpleName) qualifier;
				Position pos= new Position(simpleQualifier.getStartPosition(), simpleQualifier.getLength());
				ProblemPosition updatedProb= new ProblemPosition(pos, problemPos.getAnnotation(), problemPos.getCompilationUnit());
				getTypeProposals(updatedProb, SimilarElementsRequestor.REF_TYPES, proposals);
			}
		} else if (selectedNode instanceof FieldAccess) {
			FieldAccess access= (FieldAccess) selectedNode;
			Expression expression= access.getExpression();
			if (expression == null || expression instanceof ThisExpression) {
				node= access.getName();
			}
		}				
		if (node == null) {
			return;
		}


		// corrections
		SimilarElement[] elements= SimilarElementsRequestor.findSimilarElement(cu, node, SimilarElementsRequestor.VARIABLES);
		for (int i= 0; i < elements.length; i++) {
			SimilarElement curr= elements[i];
			String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.changevariable.description", curr.getName()); //$NON-NLS-1$
			proposals.add(new ReplaceCorrectionProposal(label, problemPos.getCompilationUnit(), node.getStartPosition(), node.getLength(), curr.getName(), 3));
		}

		// new variables
		BodyDeclaration bodyDeclaration= ASTResolving.findParentBodyDeclaration(node);
		if (bodyDeclaration != null) {
			String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createfield.description", node.getIdentifier()); //$NON-NLS-1$
			proposals.add(new NewVariableCompletionProposal(label, cu, NewVariableCompletionProposal.FIELD, node, 2));
		}
		
		int type= bodyDeclaration.getNodeType();
		if (type == ASTNode.METHOD_DECLARATION) {
			String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createparameter.description", node.getIdentifier()); //$NON-NLS-1$
			proposals.add(new NewVariableCompletionProposal(label, cu, NewVariableCompletionProposal.PARAM, node, 1));
		}
		if (type == ASTNode.METHOD_DECLARATION || type == ASTNode.INITIALIZER) {
			String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createlocal.description", node.getIdentifier()); //$NON-NLS-1$
			proposals.add(new NewVariableCompletionProposal(label, cu, NewVariableCompletionProposal.LOCAL, node, 1));
		}
		
	}
	
	public static void getTypeProposals(ProblemPosition problemPos, int kind, ArrayList proposals) throws CoreException {
		
		ICompilationUnit cu= problemPos.getCompilationUnit();
		
		// corrections
		String typeName= cu.getBuffer().getText(problemPos.getOffset(), problemPos.getLength());
		int bracketIndex= typeName.indexOf('[');
		if (bracketIndex != -1) {
			typeName= typeName.substring(0, bracketIndex);
		}
		
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		
		SimilarElement[] elements= SimilarElementsRequestor.findSimilarElement(cu, problemPos.getOffset(), typeName, kind);
		for (int i= 0; i < elements.length; i++) {
			String curr= elements[i].getName();
			String simpleName= Signature.getSimpleName(curr);
			
			String replaceName= simpleName;
			
			ImportEdit importEdit= new ImportEdit(cu, settings);
						
			IImportDeclaration existingImport= JavaModelUtil.findImport(cu, simpleName);
			if (existingImport != null) {
				if (!existingImport.getElementName().equals(curr)) {
					// import for otehr type already exists -> use qualified
					replaceName= curr;
				}
			} else {
				importEdit.addImport(curr);
			}
						
			importEdit.addImport(curr);
						
			boolean importOnly= replaceName.equals(typeName);
			
			CUCorrectionProposal proposal= new CUCorrectionProposal("", cu, 0); //$NON-NLS-1$
			proposals.add(proposal);
			
			TextEdit root= proposal.getRootTextEdit();
			
			if (!importEdit.isEmpty()) {
				root.add(importEdit); //$NON-NLS-1$
			}
			if (!importOnly) {
				root.add(SimpleTextEdit.createReplace(problemPos.getOffset(), typeName.length(), replaceName)); //$NON-NLS-1$
				proposal.setDisplayName(CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.changetype.description", replaceName)); //$NON-NLS-1$
				proposal.setRelevance(3);
			} else {
				proposal.setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_IMPDECL));
				proposal.setDisplayName(CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.importtype.description", curr)); //$NON-NLS-1$
				proposal.setRelevance(5);
			}
		}
		
		// add type
		String addedCUName= typeName + ".java"; //$NON-NLS-1$
		if (!JavaConventions.validateCompilationUnitName(addedCUName).matches(IStatus.ERROR)) {
			IPackageFragment pack= (IPackageFragment) cu.getParent();
			final ICompilationUnit addedCU= pack.getCompilationUnit(addedCUName);
			if (!addedCU.exists()) {
				if ((kind & SimilarElementsRequestor.CLASSES) != 0) {
					String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createclassusingwizard.description", typeName); //$NON-NLS-1$
                    proposals.add(new NewCUCompletionUsingWizardProposal(label, typeName, cu, true, problemPos, 0));
				}
				if ((kind & SimilarElementsRequestor.INTERFACES) != 0) {
					String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createinterfaceusingwizard.description", typeName); //$NON-NLS-1$
                    proposals.add(new NewCUCompletionUsingWizardProposal(label, typeName, cu, false, problemPos, 0));
				}				
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
			ASTNode typeDecl= ASTResolving.findParentType(invocationNode);
			if (typeDecl instanceof TypeDeclaration) {
				binding= ((TypeDeclaration) typeDecl).resolveBinding();
			} else {
				binding= ((AnonymousClassDeclaration) typeDecl).resolveBinding();
			}
			if (isSuperInvocation && binding != null) {
				binding= binding.getSuperclass();
			}				
		}
		if (binding != null && binding.isFromSource()) {
			ICompilationUnit targetCU= getCompilationUnit(binding, cu, astRoot);
			if (targetCU != null) {			
				String label;
				if (cu.equals(targetCU)) {
					label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createmethod.description", methodName); //$NON-NLS-1$
				} else {
					label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createmethod.other.description", new Object[] { methodName, targetCU.getElementName() } ); //$NON-NLS-1$
				}
				proposals.add(new NewMethodCompletionProposal(label, targetCU, invocationNode, arguments, binding, 1));
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
			ASTNode typeDecl= ASTResolving.findParentType(selectedNode);
			if (typeDecl instanceof TypeDeclaration) {
				ITypeBinding typeBinding= ((TypeDeclaration) typeDecl).resolveBinding();
				if (typeBinding != null) {
					targetBinding= typeBinding.getSuperclass();
					arguments= ((SuperConstructorInvocation) selectedNode).arguments();
				}
			}
		} else if (type == ASTNode.CONSTRUCTOR_INVOCATION) {
			ASTNode typeDecl= ASTResolving.findParentType(selectedNode);
			if (typeDecl instanceof TypeDeclaration) {
				targetBinding= ((TypeDeclaration) typeDecl).resolveBinding();
				arguments= ((ConstructorInvocation) selectedNode).arguments();
			}			
		}
		if (targetBinding != null && targetBinding.isFromSource()) {
			ICompilationUnit targetCU= getCompilationUnit(targetBinding, cu, astRoot);
			if (targetCU != null) {
				String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createconstructor.description", targetBinding.getName()); //$NON-NLS-1$
				proposals.add(new NewMethodCompletionProposal(label, targetCU, selectedNode, arguments, targetBinding, 1));
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
