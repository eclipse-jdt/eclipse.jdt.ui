/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation with code moved from other classes
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IModuleBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ModuleDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ProvidesDirective;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.UsesDirective;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.manipulation.CodeGeneration;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.util.Messages;

public abstract class JavadocTagsBaseSubProcessor<T> {
	protected JavadocTagsBaseSubProcessor() {
	}
	public void addMissingJavadocTagProposals(IInvocationContextCore context, IProblemLocationCore problem, Collection<T> proposals) {
		ASTNode node= problem.getCoveringNode(context.getASTRoot());
		addMissingJavadocTagProposals(context, node, proposals);
	}

	/*
	 * This entry point is requested by jdt.ls
	 */
	public void addMissingJavadocTagProposals(IInvocationContextCore context, ASTNode node, Collection<T> proposals) {
		ASTNode parentDeclaration= null;
		if (node == null) {
			return;
		}
		node= ASTNodes.getNormalizedNode(node);
		String label;

		StructuralPropertyDescriptor location= node.getLocationInParent();
		if (location == ModuleDeclaration.MODULE_DIRECTIVES_PROPERTY) {
			if (node instanceof UsesDirective) {
				label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_usestag_description;
			} else if (node instanceof ProvidesDirective) {
				label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_providestag_description;
			} else {
				return;
			}
			ModuleDeclaration moduleDecl= (ModuleDeclaration) node.getParent();
			T proposal= addMissingModuleJavadocTagProposal(label, context.getCompilationUnit(), moduleDecl, node, IProposalRelevance.ADD_MISSING_TAG);
			if (proposal != null)
				proposals.add(proposal);

			String label2= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_allmissing_description;
			T addAllMissing= addAllMissingModuleJavadocTagsProposal(label2, context.getCompilationUnit(), moduleDecl, node, IProposalRelevance.ADD_ALL_MISSING_TAGS);
			if (addAllMissing != null)
				proposals.add(addAllMissing);
		} else {
			parentDeclaration= ASTResolving.findParentBodyDeclaration(node);
			if (parentDeclaration == null) {
				return;
			}
			Javadoc javadoc= ((BodyDeclaration) parentDeclaration).getJavadoc();
			if (javadoc == null) {
				return;
			}

			if (location == SingleVariableDeclaration.NAME_PROPERTY) {
				label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_paramtag_description;
				StructuralPropertyDescriptor propDesc= node.getParent().getLocationInParent();
				if (propDesc != MethodDeclaration.PARAMETERS_PROPERTY
						&& propDesc != RecordDeclaration.RECORD_COMPONENTS_PROPERTY) {
					return; // paranoia checks
				}
			} else if (location == TypeParameter.NAME_PROPERTY) {
				label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_paramtag_description;
				StructuralPropertyDescriptor parentLocation= node.getParent().getLocationInParent();
				if (parentLocation != MethodDeclaration.TYPE_PARAMETERS_PROPERTY
						&& parentLocation != TypeDeclaration.TYPE_PARAMETERS_PROPERTY
						&& parentLocation != RecordDeclaration.TYPE_PARAMETERS_PROPERTY) {
					return; // paranoia checks
				}
			} else if (location == MethodDeclaration.RETURN_TYPE2_PROPERTY) {
				label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_returntag_description;
			} else if (location == MethodDeclaration.THROWN_EXCEPTION_TYPES_PROPERTY) {
				label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_throwstag_description;
			} else {
				return;
			}
			T proposal= addMissingJavadocTagProposal(label, context.getCompilationUnit(), parentDeclaration, node, IProposalRelevance.ADD_MISSING_TAG);
			if (proposal != null)
				proposals.add(proposal);

			String label2= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_allmissing_description;
			T addAllMissing= addAllMissingJavadocTagsProposal(label2, context.getCompilationUnit(), parentDeclaration, IProposalRelevance.ADD_ALL_MISSING_TAGS);
			if (addAllMissing != null)
				proposals.add(addAllMissing);
		}
	}

	public void addUnusedAndUndocumentedParameterOrExceptionProposals(IInvocationContextCore context, IProblemLocationCore problem, Collection<T> proposals) {
		ICompilationUnit cu= context.getCompilationUnit();
		IJavaProject project= cu.getJavaProject();

		if (!JavaCore.ENABLED.equals(project.getOption(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, true))) {
			return;
		}

		int problemId= problem.getProblemId();
		boolean isUnusedTypeParam= problemId == IProblem.UnusedTypeParameter;
		boolean isUnusedParam= problemId == IProblem.ArgumentIsNeverUsed || isUnusedTypeParam;
		String key= isUnusedParam ? JavaCore.COMPILER_PB_UNUSED_PARAMETER_INCLUDE_DOC_COMMENT_REFERENCE : JavaCore.COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_INCLUDE_DOC_COMMENT_REFERENCE;

		if (!JavaCore.ENABLED.equals(project.getOption(key, true))) {
			return;
		}

		ASTNode node= problem.getCoveringNode(context.getASTRoot());
		if (node == null) {
			return;
		}

		BodyDeclaration bodyDecl= ASTResolving.findParentBodyDeclaration(node);
		if (bodyDecl == null || ASTResolving.getParentMethodOrTypeBinding(bodyDecl) == null) {
			return;
		}

		String label;
		if (isUnusedTypeParam) {
			label= CorrectionMessages.JavadocTagsSubProcessor_document_type_parameter_description;
		} else if (isUnusedParam) {
			label= CorrectionMessages.JavadocTagsSubProcessor_document_parameter_description;
		} else {
			node= ASTNodes.getNormalizedNode(node);
			label= CorrectionMessages.JavadocTagsSubProcessor_document_exception_description;
		}
		T proposal= addMissingJavadocTagProposal(label, context.getCompilationUnit(), bodyDecl, node, IProposalRelevance.DOCUMENT_UNUSED_ITEM);
		if (proposal != null)
			proposals.add(proposal);
	}

	public void addMissingJavadocCommentProposals(IInvocationContextCore context, IProblemLocationCore problem, Collection<T> proposals) throws CoreException {
		ASTNode node= problem.getCoveringNode(context.getASTRoot());
		if (node == null) {
			return;
		}
		ICompilationUnit cu= context.getCompilationUnit();
		if (node instanceof ModuleDeclaration) {
			ModuleDeclaration declaration= (ModuleDeclaration) node;
			IModuleBinding binding= declaration.resolveBinding();
			if (binding == null) {
				return;
			}
			List<String> usesNames= new ArrayList<>();
			for (ITypeBinding use : binding.getUses()) {
				usesNames.add(use.getName());
			}
			List<String> providesNames= new ArrayList<>();
			for (ITypeBinding provide : binding.getServices()) {
				providesNames.add(provide.getName());
			}
			String comment= CodeGeneration.getModuleComment(cu, declaration.getName().getFullyQualifiedName(), providesNames.toArray(new String[0]), usesNames.toArray(new String[0]),
					String.valueOf('\n'));
			if (comment != null) {
				String label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_method_description;
				T p = addJavadocCommentProposal(label, cu, IProposalRelevance.ADD_JAVADOC_MODULE, declaration.getStartPosition(), comment);
				if (p != null)
					proposals.add(p);
			}
		} else {
			BodyDeclaration declaration= ASTResolving.findParentBodyDeclaration(node);
			if (declaration == null) {
				return;
			}
			ITypeBinding binding= Bindings.getBindingOfParentType(declaration);
			if (binding == null) {
				return;
			}

			if (declaration instanceof MethodDeclaration) {
				MethodDeclaration methodDecl= (MethodDeclaration) declaration;
				IMethodBinding methodBinding= methodDecl.resolveBinding();
				IMethodBinding overridden= null;
				if (methodBinding != null) {
					overridden= Bindings.findOverriddenMethod(methodBinding, true);
				}

				String string= CodeGeneration.getMethodComment(cu, binding.getName(), methodDecl, overridden, String.valueOf('\n'));
				if (string != null) {
					String label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_method_description;
					T p = addJavadocCommentProposal(label, cu, IProposalRelevance.ADD_JAVADOC_METHOD, declaration.getStartPosition(), string);
					if (p != null)
						proposals.add(p);
				}
			} else if (declaration instanceof AbstractTypeDeclaration) {
				String typeQualifiedName= Bindings.getTypeQualifiedName(binding);
				String[] typeParamNames, params;
				if (declaration instanceof TypeDeclaration) {
					List<TypeParameter> typeParams= ((TypeDeclaration) declaration).typeParameters();
					typeParamNames= new String[typeParams.size()];
					for (int i= 0; i < typeParamNames.length; i++) {
						typeParamNames[i]= (typeParams.get(i)).getName().getIdentifier();
					}
					params= new String[0];
				} else if (declaration instanceof RecordDeclaration) {
					List<SingleVariableDeclaration> recComps= ((RecordDeclaration) declaration).recordComponents();
					params= new String[recComps.size()];
					for (int i= 0; i < params.length; i++) {
						params[i]= (recComps.get(i)).getName().getIdentifier();
					}
					List<TypeParameter> typeParams= ((RecordDeclaration) declaration).typeParameters();
					typeParamNames= new String[typeParams.size()];
					for (int i= 0; i < typeParamNames.length; i++) {
						typeParamNames[i]= (typeParams.get(i)).getName().getIdentifier();
					}
				} else {
					typeParamNames= new String[0];
					params= new String[0];
				}
				String string= CodeGeneration.getTypeComment(cu, typeQualifiedName, typeParamNames, params, String.valueOf('\n'));
				if (string != null) {
					String label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_type_description;
					T p = addJavadocCommentProposal(label, cu, IProposalRelevance.ADD_JAVADOC_TYPE, declaration.getStartPosition(), string);
					if (p != null)
						proposals.add(p);
				}
			} else if (declaration instanceof FieldDeclaration) {
				String comment= "/**\n *\n */\n"; //$NON-NLS-1$
				List<VariableDeclarationFragment> fragments= ((FieldDeclaration) declaration).fragments();
				if (fragments != null && fragments.size() > 0) {
					VariableDeclaration decl= fragments.get(0);
					String fieldName= decl.getName().getIdentifier();
					String typeName= binding.getName();
					comment= CodeGeneration.getFieldComment(cu, typeName, fieldName, String.valueOf('\n'));
				}
				if (comment != null) {
					String label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_field_description;
					T p = addJavadocCommentProposal(label, cu, IProposalRelevance.ADD_JAVADOC_FIELD, declaration.getStartPosition(), comment);
					if (p != null)
						proposals.add(p);
				}
			} else if (declaration instanceof EnumConstantDeclaration) {
				EnumConstantDeclaration enumDecl= (EnumConstantDeclaration) declaration;
				String id= enumDecl.getName().getIdentifier();
				String comment= CodeGeneration.getFieldComment(cu, binding.getName(), id, String.valueOf('\n'));
				String label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_enumconst_description;
				T p = addJavadocCommentProposal(label, cu, IProposalRelevance.ADD_JAVADOC_ENUM, declaration.getStartPosition(), comment);
				if (p != null)
					proposals.add(p);
			}
		}
	}

	protected abstract T addJavadocCommentProposal(String label, ICompilationUnit cu, int addJavadocModule, int startPosition, String comment);

	public void addRemoveJavadocTagProposals(IInvocationContextCore context, IProblemLocationCore problem, Collection<T> proposals) {
		ASTNode node= problem.getCoveringNode(context.getASTRoot());
		while (node != null && !(node instanceof TagElement)) {
			node= node.getParent();
		}
		if (node == null) {
			return;
		}
		ASTRewrite rewrite= ASTRewrite.create(node.getAST());
		rewrite.remove(node, null);

		String label= CorrectionMessages.JavadocTagsSubProcessor_removetag_description;
		T p = createRemoveJavadocTagProposals(label, context.getCompilationUnit(), rewrite, IProposalRelevance.REMOVE_TAG);
		if (p != null)
			proposals.add(p);
	}


	protected abstract T createRemoveJavadocTagProposals(String label, ICompilationUnit compilationUnit, ASTRewrite rewrite, int removeTag);

	public void addRemoveDuplicateModuleJavadocTagProposals(IInvocationContextCore context, IProblemLocationCore problem, Collection<T> proposals) {
		ASTNode node= problem.getCoveringNode(context.getASTRoot());
		if (node instanceof ModuleDeclaration) {
			node= findModuleJavadocTag((ModuleDeclaration) node, problem);
			if (node == null) {
				return;
			}
			CompilationUnit cu= (CompilationUnit) ((Javadoc) node.getParent()).getAlternateRoot();
			if (cu == null) {
				return;
			}
			int line= cu.getLineNumber(problem.getOffset());
			IJavaElement javaElement= cu.getJavaElement();
			if (javaElement == null) {
				return;
			}
			String lineDelimiter= StubUtility.getLineDelimiterUsed(javaElement);
			int start= cu.getPosition(line, 0) - lineDelimiter.length();
			int column= cu.getColumnNumber(node.getStartPosition());
			int length= node.getLength() + column + lineDelimiter.length();
			String label= Messages.format(CorrectionMessages.JavadocTagsSubProcessor_removeduplicatetag_description, ((TextElement) ((TagElement) node).fragments().get(0)).getText().trim());
			T proposal= createRemoveDuplicateModuleJavadocTagProposal(label, context.getCompilationUnit(), start, length,
					"", IProposalRelevance.REMOVE_TAG); //$NON-NLS-1$
			if (proposal != null)
				proposals.add(proposal);
		}
	}

	protected abstract T createRemoveDuplicateModuleJavadocTagProposal(String label, ICompilationUnit compilationUnit, int start, int length, String string, int removeTag);

	private ASTNode findModuleJavadocTag(ModuleDeclaration decl, IProblemLocationCore problem) {
		ASTNode result= null;
		CompilationUnit cu= (CompilationUnit) decl.getParent();
		int problemLocationStart= problem.getOffset();
		Name moduleName= decl.getName();
		List<Comment> comments= cu.getCommentList();

		for (Comment comment : comments) {
			if (comment instanceof Javadoc
					&& comment.getStartPosition() + comment.getLength() < moduleName.getStartPosition()) {
				Javadoc javadoc= (Javadoc) comment;
				List<TagElement> tags= javadoc.tags();

				for (TagElement tag : tags) {
					if (problemLocationStart > tag.getStartPosition()
							&& problemLocationStart < tag.getStartPosition() + tag.getLength()) {
						result= tag;
						break;
					}
				}
			}
		}
		return result;
	}

	public void addInvalidQualificationProposals(IInvocationContextCore context, IProblemLocationCore problem, Collection<T> proposals) {
		ASTNode node= problem.getCoveringNode(context.getASTRoot());
		if (!(node instanceof Name)) {
			return;
		}
		Name name= (Name) node;
		IBinding binding= name.resolveBinding();
		if (!(binding instanceof ITypeBinding)) {
			return;
		}
		ITypeBinding typeBinding= (ITypeBinding) binding;

		AST ast= node.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		rewrite.replace(name, ast.newName(typeBinding.getQualifiedName()), null);

		String label= CorrectionMessages.JavadocTagsSubProcessor_qualifylinktoinner_description;
		T proposal= createInvalidQualificationProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.QUALIFY_INNER_TYPE_NAME);
		if (proposal != null)
			proposals.add(proposal);
	}

	protected abstract T addAllMissingJavadocTagsProposal(String label, ICompilationUnit compilationUnit, ASTNode parentDeclaration, int relevance);
	protected abstract T addMissingJavadocTagProposal(String label, ICompilationUnit compilationUnit, ASTNode parentDeclaration, ASTNode node, int relevance);
	protected abstract T addAllMissingModuleJavadocTagsProposal(String label, ICompilationUnit compilationUnit, ModuleDeclaration moduleDecl, ASTNode node, int relevance);
	protected abstract T addMissingModuleJavadocTagProposal(String label, ICompilationUnit compilationUnit, ModuleDeclaration moduleDecl, ASTNode node, int relevance);
	protected abstract T createInvalidQualificationProposal(String label, ICompilationUnit compilationUnit, ASTRewrite rewrite, int relevance);

}
