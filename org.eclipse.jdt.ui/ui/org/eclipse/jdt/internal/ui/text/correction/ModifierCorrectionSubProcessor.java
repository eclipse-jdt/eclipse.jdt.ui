package org.eclipse.jdt.internal.ui.text.correction;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
  */
public class ModifierCorrectionSubProcessor {

	public static void addNonAccessibleMemberProposal(ICorrectionContext context, List proposals, boolean visibilityChange) throws JavaModelException {
		ICompilationUnit cu= context.getCompilationUnit();

		ASTNode selectedNode= context.getCoveringNode();
		if (selectedNode == null) {
			return;
		}
		
		IBinding binding=null;
		switch (selectedNode.getNodeType()) {
			case ASTNode.SIMPLE_NAME:
				binding= ((SimpleName) selectedNode).resolveBinding();
				break;
			case ASTNode.QUALIFIED_NAME:
				binding= ((QualifiedName) selectedNode).resolveBinding();
				break;
			case ASTNode.SIMPLE_TYPE:
				binding= ((SimpleType) selectedNode).resolveBinding();
				break;
			case ASTNode.METHOD_INVOCATION:
				binding= ((MethodInvocation) selectedNode).getName().resolveBinding();
				break;
			case ASTNode.SUPER_METHOD_INVOCATION:
				binding= ((SuperMethodInvocation) selectedNode).getName().resolveBinding();
				break;								
			case ASTNode.SUPER_FIELD_ACCESS:
				binding= ((SuperFieldAccess) selectedNode).getName().resolveBinding();
				break;				
			case ASTNode.CLASS_INSTANCE_CREATION:
				binding= ((ClassInstanceCreation) selectedNode).resolveConstructorBinding();
				break;
			case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
				binding= ((SuperConstructorInvocation) selectedNode).resolveConstructorBinding();
				break;							
			default:
				return;
		}
		ITypeBinding typeBinding= null;
		String name;
		if (binding instanceof IMethodBinding) {
			typeBinding= ((IMethodBinding) binding).getDeclaringClass();
			name= binding.getName() + "()";
		} else if (binding instanceof IVariableBinding) {
			typeBinding= ((IVariableBinding) binding).getDeclaringClass();
			name= binding.getName();
		} else if (binding instanceof ITypeBinding) {
			typeBinding= (ITypeBinding) binding;
			name= binding.getName();
		} else {
			return;
		}
		if (typeBinding.isFromSource()) {
			int includedModifiers= 0;
			int excludedModifiers= 0;
			String label;
			if (visibilityChange) {
				excludedModifiers= Modifier.PRIVATE | Modifier.PROTECTED | Modifier.PUBLIC;
				includedModifiers= getNeededVisibility(selectedNode, typeBinding);
				label= CorrectionMessages.getFormattedString("ModifierCorrectionSubProcessor.changevisibility.description", new String[] { name, getVisibilityString(includedModifiers) });
			} else {				
				label= CorrectionMessages.getFormattedString("ModifierCorrectionSubProcessor.changemodifiertostatic.description", name);
				includedModifiers= Modifier.STATIC;
			}
			ICompilationUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, context.getASTRoot(), typeBinding);
			if (targetCU != null) {
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				proposals.add(new ModifierChangeCompletionProposal(label, targetCU, binding, selectedNode, includedModifiers, excludedModifiers, 0, image));
			}
		}
	}
		
	private static String getVisibilityString(int code) {
		if (Modifier.isPublic(code)) {
			return "public";
		}else if (Modifier.isProtected(code)) {
			return "protected";
		}
		return "default";
	}
	
	
	private static int getNeededVisibility(ASTNode currNode, ITypeBinding targetType) {
		ITypeBinding currNodeBinding= ASTResolving.getBindingOfParentType(currNode);
		if (currNodeBinding == null) { // import
			return Modifier.PUBLIC;
		}
		
		ITypeBinding curr= currNodeBinding;
		while (curr != null) {
			if (curr.getKey().equals(targetType.getKey())) {
				return Modifier.PROTECTED;
			}
			curr= curr.getSuperclass();
		}
		if (currNodeBinding.getPackage().getKey().equals(targetType.getPackage().getKey())) {
			return 0;
		}
		return Modifier.PUBLIC;
	}

	public static void addAbstractMethodProposals(ICorrectionContext context, List proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();

		CompilationUnit astRoot= context.getASTRoot();

		ASTNode selectedNode= context.getCoveringNode();
		if (selectedNode == null) {
			return;
		}
		MethodDeclaration decl;
		if (selectedNode instanceof SimpleName) {
			decl= (MethodDeclaration) selectedNode.getParent();
		} else if (selectedNode instanceof MethodDeclaration) {
			decl= (MethodDeclaration) selectedNode;
		} else {
			return;
		}
	
		ASTNode parentType= ASTResolving.findParentType(decl);
		boolean parentIsAbstract= (parentType instanceof TypeDeclaration) && Modifier.isAbstract(((TypeDeclaration) parentType).getModifiers());
		
		int endPos= decl.getStartPosition() + decl.getLength() - 1;
		IBuffer buffer= cu.getBuffer();
		boolean hasNoBody= buffer.getLength() > endPos && buffer.getChar(endPos) == ';';
		
		if (context.getProblemId() == IProblem.AbstractMethodInAbstractClass || parentIsAbstract) {
			ASTRewrite rewrite= new ASTRewrite(decl.getParent());
			
			AST ast= astRoot.getAST();
			MethodDeclaration modifiedNode= ast.newMethodDeclaration();
			modifiedNode.setConstructor(decl.isConstructor());
			modifiedNode.setExtraDimensions(decl.getExtraDimensions());
			modifiedNode.setModifiers(decl.getModifiers() & ~Modifier.ABSTRACT);
			rewrite.markAsModified(decl, modifiedNode);

			if (hasNoBody) {
				Block newBody= ast.newBlock();
				rewrite.markAsInserted(newBody);
				decl.setBody(newBody);
				Expression expr= ASTResolving.getInitExpression(decl.getReturnType(), decl.getExtraDimensions());
				if (expr != null) {
					ReturnStatement returnStatement= ast.newReturnStatement();
					returnStatement.setExpression(expr);
					newBody.statements().add(returnStatement);
				}
			}
	
			String label= CorrectionMessages.getString("ModifierCorrectionSubProcessor.removeabstract.description");
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 1, image);
			proposal.ensureNoModifications();
			proposals.add(proposal);
		}
				
		if (context.getProblemId() == IProblem.AbstractMethodInAbstractClass && (parentType instanceof TypeDeclaration)) {
			ASTRewriteCorrectionProposal proposal= getMakeTypeStaticProposal(cu, (TypeDeclaration) parentType);
			proposals.add(proposal);
		}		
		
	}
	
	public static ASTRewriteCorrectionProposal getMakeTypeStaticProposal(ICompilationUnit cu, TypeDeclaration typeDeclaration) throws CoreException {
		ASTRewrite rewrite= new ASTRewrite(typeDeclaration.getParent());
		
		AST ast= typeDeclaration.getAST();
		TypeDeclaration modifiedNode= ast.newTypeDeclaration();
		modifiedNode.setInterface(typeDeclaration.isInterface());
		modifiedNode.setModifiers(typeDeclaration.getModifiers() | Modifier.ABSTRACT);
		rewrite.markAsModified(typeDeclaration, modifiedNode);

		String label= CorrectionMessages.getFormattedString("ModifierCorrectionSubProcessor.addabstract.description", typeDeclaration.getName().getIdentifier());
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 3, image);
		proposal.ensureNoModifications();
		return proposal;
	}
	

}
