/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Body moved from org.eclipse.jdt.internal.ui.text.correction.proposals.TypeChangeCorrectionProposal
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.core.manipulation.BindingLabelProviderCore;
import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.DimensionRewrite;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.dom.TypeAnnotationRewrite;
import org.eclipse.jdt.internal.corext.fix.TypeParametersFixCore;
import org.eclipse.jdt.internal.corext.refactoring.structure.ImportRemover;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.TypeFilter;

import org.eclipse.jdt.internal.ui.text.CompletionTimeoutProgressMonitor;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.text.correction.JavadocTagsSubProcessorCore;

public class TypeChangeCorrectionProposalCore extends LinkedCorrectionProposalCore {
	private final IBinding fBinding;
	private final ASTNode fNode;
	private final CompilationUnit fAstRoot;
	private final ICompilationUnit fCompilationUnit;
	private ITypeBinding fNewType;
	private ITypeBinding[] fTypeProposals;
	private final TypeLocation fTypeLocation;
	private final boolean fIsNewTypeVar;
	private static String VAR_TYPE= "var"; //$NON-NLS-1$
	private static String CONSTRUCTOR= "constructor"; //$NON-NLS-1$
	private static final int SKIP_NEW_KEYWORD = 4;

	public TypeChangeCorrectionProposalCore(ICompilationUnit targetCU, IBinding binding, CompilationUnit astRoot, ITypeBinding newType, boolean offerSuperTypeProposals, int relevance) {
		this(targetCU, binding, astRoot, newType, false, offerSuperTypeProposals, relevance);
	}

	//This needs to be used to convert a given type to var type.
	public TypeChangeCorrectionProposalCore(ICompilationUnit targetCU, IBinding binding, CompilationUnit astRoot, ITypeBinding oldType, int relevance) {
		this(targetCU, binding, astRoot, oldType, true, false, relevance);
	}

	public TypeChangeCorrectionProposalCore(ICompilationUnit targetCU, IBinding binding, CompilationUnit astRoot, ITypeBinding newType, boolean isNewTypeVar, boolean offerSuperTypeProposals,
			int relevance) {
		super("", targetCU, null, relevance); //$NON-NLS-1$

		Assert.isTrue(binding != null && (binding.getKind() == IBinding.METHOD || binding.getKind() == IBinding.VARIABLE) && Bindings.isDeclarationBinding(binding));

		fBinding= binding; // must be generic method or (generic) variable
		fAstRoot= astRoot;
		fCompilationUnit = targetCU;
		fIsNewTypeVar= isNewTypeVar;
		fNode = null;

		if (offerSuperTypeProposals) {
			fTypeProposals= ASTResolving.getRelaxingTypes(astRoot.getAST(), newType);
			sortTypes(fTypeProposals);
			fNewType= fTypeProposals[0];
		} else {
			if (!fIsNewTypeVar) {
				fNewType= newType;
			} else {
				fNewType= null;
			}
			fTypeProposals= null;
		}

		String typeName;
		if (isNewTypeVar) {
			typeName= VAR_TYPE;
		} else {
			// Bug 540927 - if type contains nested capture, we need to get the name
			// that the type will eventually be resolved to
			if (containsNestedCapture(fNewType, false)) {
				typeName= getNewTypeString();
			} else {
				typeName= BindingLabelProviderCore.getBindingLabel(fNewType, JavaElementLabelsCore.ALL_DEFAULT);
			}
		}
		if (binding.getKind() == IBinding.VARIABLE) {
			IVariableBinding varBinding= (IVariableBinding) binding;
			String[] args= { BasicElementLabels.getJavaElementName(varBinding.getName()),  BasicElementLabels.getJavaElementName(typeName)};
			if (varBinding.isField()) {
				fTypeLocation= TypeLocation.FIELD;
				setDisplayName(Messages.format(CorrectionMessages.TypeChangeCompletionProposal_field_name, args));
			} else if (astRoot.findDeclaringNode(binding) instanceof SingleVariableDeclaration) {
				fTypeLocation= TypeLocation.PARAMETER;
				setDisplayName(Messages.format(CorrectionMessages.TypeChangeCompletionProposal_param_name, args));
			} else {
				fTypeLocation= TypeLocation.LOCAL_VARIABLE;
				setDisplayName(Messages.format(CorrectionMessages.TypeChangeCompletionProposal_variable_name, args));
			}
		} else {
			String[] args= { binding.getName(), typeName };
			fTypeLocation= TypeLocation.RETURN_TYPE;
			setDisplayName(Messages.format(CorrectionMessages.TypeChangeCompletionProposal_method_name, args));
		}
	}

	//This needs to be used to convert a given constructor invocation
	public TypeChangeCorrectionProposalCore(ICompilationUnit targetCU, ASTNode nodeToChange, CompilationUnit astRoot, ITypeBinding variableTypeBinding, int relevance) {
		super("", targetCU, null, relevance); //$NON-NLS-1$

		Assert.isTrue(nodeToChange instanceof ClassInstanceCreation);

		fBinding= variableTypeBinding;
		fAstRoot= astRoot;
		fCompilationUnit = targetCU;
		fIsNewTypeVar= false;
		fNode = nodeToChange;
		fTypeLocation= TypeLocation.NEW;
		fTypeProposals = null;
		fNewType = null;

		IBinding nodeBinding = Bindings.resolveExpressionBinding((Expression)nodeToChange, false);
		String[] arg = new String[1];
		if (nodeBinding != null) {
			arg[0] = nodeBinding.getName();
		} else {
			arg[0] = CONSTRUCTOR;
		}

		setDisplayName(Messages.format(CorrectionMessages.TypeChangeCompletionProposal_constructor_name, arg));
	}

	private boolean containsNestedCapture(ITypeBinding binding, boolean isNested) {
		if (binding == null || binding.isPrimitive() || binding.isTypeVariable()) {
			return false;
		}
		if (binding.isCapture()) {
			if (isNested) {
				return true;
			}
			return containsNestedCapture(binding.getWildcard(), true);
		}
		if (binding.isWildcardType()) {
			return containsNestedCapture(binding.getBound(), true);
		}
		if (binding.isArray()) {
			return containsNestedCapture(binding.getElementType(), true);
		}
		for (ITypeBinding typeArgument : binding.getTypeArguments()) {
			if (containsNestedCapture(typeArgument, true)) {
				return true;
			}
		}
		return false;
	}

	private String getNewTypeString() {
		ASTNode boundNode= fAstRoot.findDeclaringNode(fBinding);
		ASTNode declNode= null;
		CompilationUnit newRoot= fAstRoot;
		if (boundNode != null) {
			declNode= boundNode; // is same CU
		} else {
			newRoot= ASTResolving.createQuickFixAST(getCompilationUnit(), null);
			declNode= newRoot.findDeclaringNode(fBinding.getKey());
		}
		if (declNode != null) {
			ImportRewrite imports= createImportRewrite(newRoot);
			ImportRewriteContext context= new ContextSensitiveImportRewriteContext(newRoot, declNode.getStartPosition(), imports);

			return imports.addImport(fNewType, context);
		}
		return BindingLabelProviderCore.getBindingLabel(fNewType, JavaElementLabelsCore.ALL_DEFAULT);
	}

	@Override
	protected ASTRewrite getRewrite() throws CoreException {
		ASTNode boundNode;
		if (fNode != null) {
			boundNode = fNode;
			ITypeBinding[] newTypes = getNewConstructorProposals();
			if(newTypes == null) {
				return null;
			}
			fTypeProposals = new ITypeBinding[newTypes.length-1];
			fNewType= newTypes[0];
			System.arraycopy(newTypes, 1, fTypeProposals, 0, fTypeProposals.length);
		} else {
			boundNode = fAstRoot.findDeclaringNode(fBinding);
		}
		ASTNode declNode= null;
		CompilationUnit newRoot= fAstRoot;
		if (boundNode != null) {
			declNode= boundNode; // is same CU
		} else {
			newRoot= ASTResolving.createQuickFixAST(getCompilationUnit(), null);
			declNode= newRoot.findDeclaringNode(fBinding.getKey());
		}
		if (declNode != null) {
			AST ast= declNode.getAST();
			ASTRewrite rewrite= ASTRewrite.create(ast);
			ImportRewrite imports= createImportRewrite(newRoot);

			ImportRewriteContext context= new ContextSensitiveImportRewriteContext(newRoot, declNode.getStartPosition(), imports);
			ImportRemover remover= new ImportRemover(getCompilationUnit().getJavaProject(), newRoot);
			Type type;
			if (fIsNewTypeVar) {
				type= ast.newSimpleType(ast.newName(VAR_TYPE));
			} else {
				type= imports.addImport(fNewType, ast, context, fTypeLocation);
			}

			if (declNode instanceof MethodDeclaration) {
				MethodDeclaration methodDecl= (MethodDeclaration) declNode;
				Type origReturnType= methodDecl.getReturnType2();
				rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE2_PROPERTY, type, null);
				DimensionRewrite.removeAllChildren(methodDecl, MethodDeclaration.EXTRA_DIMENSIONS2_PROPERTY, rewrite, null);
				TypeAnnotationRewrite.removePureTypeAnnotations(methodDecl, MethodDeclaration.MODIFIERS2_PROPERTY, rewrite, null);
				// add javadoc tag
				Javadoc javadoc= methodDecl.getJavadoc();
				if (javadoc != null && origReturnType != null && origReturnType.isPrimitiveType()
						&& ((PrimitiveType) origReturnType).getPrimitiveTypeCode() == PrimitiveType.VOID) {

					TagElement returnTag= JavadocTagsSubProcessorCore.findTag(javadoc, TagElement.TAG_RETURN, null);
					if (returnTag == null) {
						returnTag= ast.newTagElement();
						returnTag.setTagName(TagElement.TAG_RETURN);
						TextElement commentStart= ast.newTextElement();
						returnTag.fragments().add(commentStart);
						addLinkedPosition(rewrite.track(commentStart), false, "comment_start"); //$NON-NLS-1$

						ListRewrite tagsRewriter= rewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);
						JavadocTagsSubProcessorCore.insertTag(tagsRewriter, returnTag, null);
					}
				}

			} else if (declNode instanceof AnnotationTypeMemberDeclaration) {
				AnnotationTypeMemberDeclaration methodDecl= (AnnotationTypeMemberDeclaration) declNode;
				rewrite.set(methodDecl, AnnotationTypeMemberDeclaration.TYPE_PROPERTY, type, null);
			} else if (declNode instanceof VariableDeclarationFragment) {
				ASTNode parent= declNode.getParent();
				if (parent instanceof FieldDeclaration) {
					FieldDeclaration fieldDecl= (FieldDeclaration) parent;
					if (fieldDecl.fragments().size() > 1 && (fieldDecl.getParent() instanceof AbstractTypeDeclaration)) { // split
						VariableDeclarationFragment placeholder= (VariableDeclarationFragment) rewrite.createMoveTarget(declNode);
						FieldDeclaration newField= ast.newFieldDeclaration(placeholder);
						newField.setType(type);
						AbstractTypeDeclaration typeDecl= (AbstractTypeDeclaration) fieldDecl.getParent();

						ListRewrite listRewrite= rewrite.getListRewrite(typeDecl, typeDecl.getBodyDeclarationsProperty());
						if (fieldDecl.fragments().indexOf(declNode) == 0) { // if it as the first in the list-> insert before
							listRewrite.insertBefore(newField, parent, null);
						} else {
							listRewrite.insertAfter(newField, parent, null);
						}
					} else {
						rewrite.set(fieldDecl, FieldDeclaration.TYPE_PROPERTY, type, null);
						DimensionRewrite.removeAllChildren(declNode, VariableDeclarationFragment.EXTRA_DIMENSIONS2_PROPERTY, rewrite, null);
						TypeAnnotationRewrite.removePureTypeAnnotations(fieldDecl, FieldDeclaration.MODIFIERS2_PROPERTY, rewrite, null);
					}
				} else if (parent instanceof VariableDeclarationStatement) {
					VariableDeclarationStatement varDecl= (VariableDeclarationStatement) parent;
					if (varDecl.fragments().size() > 1 && (varDecl.getParent() instanceof Block)) { // split
						VariableDeclarationFragment placeholder= (VariableDeclarationFragment) rewrite.createMoveTarget(declNode);
						VariableDeclarationStatement newStat= ast.newVariableDeclarationStatement(placeholder);
						newStat.setType(type);

						ListRewrite listRewrite= rewrite.getListRewrite(varDecl.getParent(), Block.STATEMENTS_PROPERTY);
						if (varDecl.fragments().indexOf(declNode) == 0) { // if it as the first in the list-> insert before
							listRewrite.insertBefore(newStat, parent, null);
						} else {
							listRewrite.insertAfter(newStat, parent, null);
						}
						if (fIsNewTypeVar) {
							handledInferredParametrizedType(newStat, declNode, ast, rewrite, imports, context);
						}
					} else {
						Type oldType= (Type) rewrite.get(varDecl, VariableDeclarationStatement.TYPE_PROPERTY);
						rewrite.set(varDecl, VariableDeclarationStatement.TYPE_PROPERTY, type, null);
						DimensionRewrite.removeAllChildren(declNode, VariableDeclarationFragment.EXTRA_DIMENSIONS2_PROPERTY, rewrite, null);
						if (fIsNewTypeVar) {
							handledInferredParametrizedType(parent, declNode, ast, rewrite, imports, context);
							TypeAnnotationRewrite.removePureTypeAnnotations(parent, VariableDeclarationStatement.MODIFIERS2_PROPERTY, rewrite, null);
							if (oldType != null) {
								remover.registerRemovedNode(oldType);
							}
						}
					}
				} else if (parent instanceof VariableDeclarationExpression) {
					VariableDeclarationExpression varDecl= (VariableDeclarationExpression) parent;
					Type oldType= (Type) rewrite.get(varDecl, VariableDeclarationExpression.TYPE_PROPERTY);
					rewrite.set(varDecl, VariableDeclarationExpression.TYPE_PROPERTY, type, null);
					DimensionRewrite.removeAllChildren(declNode, VariableDeclarationFragment.EXTRA_DIMENSIONS2_PROPERTY, rewrite, null);
					if (fIsNewTypeVar) {
						handledInferredParametrizedType(parent, declNode, ast, rewrite, imports, context);
						TypeAnnotationRewrite.removePureTypeAnnotations(parent, VariableDeclarationExpression.MODIFIERS2_PROPERTY, rewrite, null);
						if (oldType != null) {
							remover.registerRemovedNode(oldType);
						}
					}
				}
			} else if (declNode instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration variableDeclaration= (SingleVariableDeclaration) declNode;
				Type oldType= (Type) rewrite.get(variableDeclaration, SingleVariableDeclaration.TYPE_PROPERTY);
				rewrite.set(variableDeclaration, SingleVariableDeclaration.TYPE_PROPERTY, type, null);
				DimensionRewrite.removeAllChildren(declNode, SingleVariableDeclaration.EXTRA_DIMENSIONS2_PROPERTY, rewrite, null);
				TypeAnnotationRewrite.removePureTypeAnnotations(declNode, SingleVariableDeclaration.MODIFIERS2_PROPERTY, rewrite, null);
				if (fIsNewTypeVar) {
					if (oldType != null) {
						remover.registerRemovedNode(oldType);
					}
				}
			} else if (declNode instanceof ClassInstanceCreation) {
				ClassInstanceCreation constructor = (ClassInstanceCreation) declNode;
				rewrite.set(constructor, ClassInstanceCreation.TYPE_PROPERTY, type, null);
			}

			// set up linked mode
			final String KEY_TYPE= "type"; //$NON-NLS-1$
			addLinkedPosition(rewrite.track(type), true, KEY_TYPE);
			if (fTypeProposals != null) {
				for (ITypeBinding fTypeProposal : fTypeProposals) {
					addLinkedPositionProposal(KEY_TYPE, fTypeProposal);
				}
			}
			if (fIsNewTypeVar) {
				remover.applyRemoves(imports);
			}
			return rewrite;
		}
		return null;
	}

	private ITypeBinding[] getNewConstructorProposals() {
		HashSet<CompletionProposal> completionProposals = new HashSet<>();
		CompletionRequestor requestor = new CompletionRequestor(false) {
			@Override
			public void accept(CompletionProposal proposal) {
				completionProposals.add(proposal);
			}
		};

		requestor.setAllowsRequiredProposals(CompletionProposal.CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF, true);

		final ExecutorService executor= Executors.newSingleThreadExecutor();
		try {
			Future<?> future= executor.submit(() -> {
				try {
					fCompilationUnit.codeComplete(fNode.getStartPosition() + SKIP_NEW_KEYWORD, requestor, new CompletionTimeoutProgressMonitor());
				} catch (JavaModelException e) {
					// do nothing
				}
			});
			future.get(1, TimeUnit.SECONDS);
		} catch (final Exception e) {
			executor.shutdownNow();
		}

		IJavaProject project= fCompilationUnit.getJavaProject();
		List<String> typeSuggestions = new ArrayList<>();
		for (CompletionProposal p: completionProposals) {
			String typeName;
			try {
				typeName = p.getDeclarationSignature() != null ? new String(Signature.toCharArray(Signature.getTypeErasure(p.getDeclarationSignature()))) : null;
			} catch (IllegalArgumentException e) {
				typeName = p.getSignature() != null ? new String(Signature.toCharArray(Signature.getTypeErasure(p.getSignature()))) : null;
			}
			if (typeName != null && !TypeFilter.isFiltered(typeName) && !typeSuggestions.contains(typeName)) {
				typeSuggestions.add(typeName);
			}
		}
		IJavaElement[] typeElements= typeSuggestions.stream().map(b -> {
			try {
				return project.findType(b);
			} catch (JavaModelException e) {
				return null;
			}
		})
				.filter(e -> {
					try {
						return e != null && !Modifier.isAbstract(e.getFlags());
					} catch (JavaModelException e1) {
						return false;
					}
				})
				.toArray(IJavaElement[]::new);
		ASTParser parser = ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setProject(project);
		ITypeBinding[] newBindings = Arrays.stream(parser.createBindings(typeElements, new NullProgressMonitor())).map(b -> (ITypeBinding)b)
				.filter(b -> Bindings.isSuperType(((ITypeBinding) fBinding).getTypeDeclaration(), b, false))
				.toArray(ITypeBinding[]::new);
		if (newBindings.length > 0 ) {
			return newBindings;
		}
		return null;
	}

	private void sortTypes(ITypeBinding[] typeProposals) {
		ITypeBinding oldType;
		if (fBinding instanceof IMethodBinding) {
			oldType= ((IMethodBinding) fBinding).getReturnType();
		} else {
			oldType= ((IVariableBinding) fBinding).getType();
		}
		if (! oldType.isParameterizedType())
			return;

		final ITypeBinding oldTypeDeclaration= oldType.getTypeDeclaration();
		Arrays.sort(typeProposals, new Comparator<ITypeBinding>() {
			@Override
			public int compare(ITypeBinding o1, ITypeBinding o2) {
				return rank(o2) - rank(o1);
			}

			private int rank(ITypeBinding type) {
				if (type.getTypeDeclaration().equals(oldTypeDeclaration))
					return 1;
				return 0;
			}
		});
	}

	private void handledInferredParametrizedType(ASTNode node, ASTNode declaringNode, AST ast, ASTRewrite rewrite, ImportRewrite importRewrite, ImportRewriteContext context) {
		if (ast == null || rewrite == null || importRewrite == null || context == null) {
			return;
		}
		ASTNode processNode= null;
		List<VariableDeclarationFragment> fragments= null;
		if (node instanceof VariableDeclarationStatement) {
			fragments= ((VariableDeclarationStatement) node).fragments();
		} else if (node instanceof VariableDeclarationExpression) {
			fragments= ((VariableDeclarationExpression) node).fragments();
		}
		if (fragments != null && fragments.size() == 1) {
			VariableDeclarationFragment varFrag= fragments.get(0);
			processNode= varFrag.getInitializer();
			if (processNode == null && declaringNode instanceof VariableDeclarationFragment) {
				processNode= ((VariableDeclarationFragment) declaringNode).getInitializer();
			}
		}
		ParameterizedType createdType= null;
		if (processNode instanceof ClassInstanceCreation) {
			ClassInstanceCreation creation= (ClassInstanceCreation) processNode;
			Type type= creation.getType();
			if (type instanceof ParameterizedType) {
				createdType= (ParameterizedType) type;
			}
		}
		if (createdType == null) {
			return;
		}

		final ArrayList<ASTNode> changedNodes= new ArrayList<>();
		node.accept(new TypeParametersFixCore.InsertTypeArgumentsVisitor(changedNodes));
		if (changedNodes.isEmpty()) {
			return;
		}

		ITypeBinding binding= createdType.resolveBinding();
		if (binding != null) {
			ListRewrite argumentsRewrite= rewrite.getListRewrite(createdType, ParameterizedType.TYPE_ARGUMENTS_PROPERTY);
			for (ITypeBinding typeArgument : binding.getTypeArguments()) {
				Type argumentNode= importRewrite.addImport(typeArgument, ast, context, TypeLocation.TYPE_ARGUMENT);
				argumentsRewrite.insertLast(argumentNode, null);
			}
		}
	}

}