/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.RangeMarker;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.text.edits.TextEditProcessor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.MethodRefParameter;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.ui.CodeGeneration;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CollectingSearchRequestor;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.SearchUtils;
import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.BindingLabels;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveProcessor;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

/**
 * Refactoring processor to move instance methods.
 */
public final class MoveInstanceMethodProcessor extends MoveProcessor {

	/**
	 * AST visitor to find references to parameters occurring in anonymous classes of a method body.
	 */
	public final class AnonymousClassReferenceFinder extends AstNodeFinder {

		/** The anonymous class nesting counter */
		protected int fAnonymousClass= 0;

		/** The declaring type of the method declaration */
		protected final ITypeBinding fDeclaringType;

		/**
		 * Creates a new anonymous class reference finder.
		 * 
		 * @param declaration the method declaration to search for references
		 */
		public AnonymousClassReferenceFinder(final MethodDeclaration declaration) {
			fDeclaringType= declaration.resolveBinding().getDeclaringClass();
		}

		public final void endVisit(final AnonymousClassDeclaration node) {
			Assert.isNotNull(node);
			if (fAnonymousClass > 0)
				fAnonymousClass--;
			super.endVisit(node);
		}

		public final boolean visit(final AnonymousClassDeclaration node) {
			Assert.isNotNull(node);
			fAnonymousClass++;
			return super.visit(node);
		}

		public final boolean visit(final MethodInvocation node) {
			Assert.isNotNull(node);
			if (fAnonymousClass > 0) {
				final IMethodBinding binding= node.resolveMethodBinding();
				if (binding != null) {
					if (node.getExpression() == null && !Modifier.isStatic(binding.getModifiers()))
						fResult.add(node.getName());
				}
			}
			return true;
		}

		public boolean visit(final SimpleName node) {
			Assert.isNotNull(node);
			if (fAnonymousClass > 0) {
				if (!(node.getParent() instanceof FieldAccess)) {
					final IBinding binding= node.resolveBinding();
					if (binding instanceof IVariableBinding) {
						final IVariableBinding variable= (IVariableBinding) binding;
						final ITypeBinding declaring= variable.getDeclaringClass();
						if (declaring != null && Bindings.equals(declaring, fDeclaringType))
							fResult.add(node);
					}
				}
			}
			return false;
		}
	}

	/**
	 * Partial implementation of an ast node finder.
	 */
	protected static class AstNodeFinder extends ASTVisitor {

		/** The found ast nodes */
		protected final Set fResult= new HashSet();

		/** The status of the find operation */
		protected final RefactoringStatus fStatus= new RefactoringStatus();

		/**
		 * Returns the result set.
		 * 
		 * @return the result set
		 */
		public final Set getResult() {
			return fResult;
		}

		/**
		 * Returns the status of the find operation.
		 * 
		 * @return the status of the operation
		 */
		public final RefactoringStatus getStatus() {
			return fStatus;
		}
	}

	/**
	 * AST visitor to find 'this' references to enclosing instances.
	 */
	public final class EnclosingInstanceReferenceFinder extends AstNodeFinder {

		/** The list of enclosing types */
		private final List fEnclosingTypes= new ArrayList(3);

		/**
		 * Creates a new enclosing instance reference finder.
		 * 
		 * @param binding the declaring type
		 */
		public EnclosingInstanceReferenceFinder(final ITypeBinding binding) {
			Assert.isNotNull(binding);
			ITypeBinding declaring= binding.getDeclaringClass();
			while (declaring != null) {
				fEnclosingTypes.add(declaring);
				declaring= declaring.getDeclaringClass();
			}
		}

		public final boolean visit(final SimpleName node) {
			Assert.isNotNull(node);
			final IBinding binding= node.resolveBinding();
			ITypeBinding declaring= null;
			if (binding instanceof IVariableBinding) {
				final IVariableBinding variable= (IVariableBinding) binding;
				declaring= variable.getDeclaringClass();
			} else if (binding instanceof IMethodBinding) {
				final IMethodBinding method= (IMethodBinding) binding;
				declaring= method.getDeclaringClass();
			}
			if (declaring != null) {
				ITypeBinding enclosing= null;
				for (final Iterator iterator= fEnclosingTypes.iterator(); iterator.hasNext();) {
					enclosing= (ITypeBinding) iterator.next();
					if (Bindings.equals(enclosing, declaring)) {
						fStatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.refers_enclosing_instances"), JavaStatusContext.create(fMethod.getCompilationUnit(), node))); //$NON-NLS-1$
						fResult.add(node);
						break;
					}
				}
			}
			return false;
		}

		public final boolean visit(final ThisExpression node) {
			Assert.isNotNull(node);
			if (node.getQualifier() != null) {
				fStatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.refers_enclosing_instances"), JavaStatusContext.create(fMethod.getCompilationUnit(), node))); //$NON-NLS-1$
				fResult.add(node);
			}
			return false;
		}
	}

	/**
	 * AST visitor to find references to type variables or generic types.
	 */
	public final class GenericReferenceFinder extends AstNodeFinder {

		/** The type parameter binding keys */
		protected final Set fBindings= new HashSet();

		/**
		 * Creates a new generic reference finder.
		 * 
		 * @param declaration the method declaration
		 */
		public GenericReferenceFinder(final MethodDeclaration declaration) {
			Assert.isNotNull(declaration);
			ITypeBinding binding= null;
			TypeParameter parameter= null;
			for (final Iterator iterator= declaration.typeParameters().iterator(); iterator.hasNext();) {
				parameter= (TypeParameter) iterator.next();
				binding= (ITypeBinding) parameter.resolveBinding();
				if (binding != null)
					fBindings.add(binding.getKey());
			}
		}

		/*
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.SimpleName)
		 */
		public final boolean visit(final SimpleName node) {
			Assert.isNotNull(node);
			final IBinding binding= node.resolveBinding();
			if (binding instanceof ITypeBinding) {
				final ITypeBinding type= (ITypeBinding) binding;
				if (!fBindings.contains(type.getKey()) && type.isTypeVariable()) {
					fResult.add(node);
					fStatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.no_type_variables"), JavaStatusContext.create(fMethod.getCompilationUnit(), node))); //$NON-NLS-1$ //$NON-NLS-2$
					return false;
				}
			}
			return true;
		}
	}

	/**
	 * Factory for method argument declaration or expression nodes.
	 */
	protected static interface IArgumentFactory {

		/**
		 * Returns a argument node for the specified variable binding.
		 * 
		 * @param binding the binding to create a argument node for
		 * @return the corresponding node
		 */
		public ASTNode getArgumentNode(IVariableBinding binding);

		/**
		 * Returns a target node for the current target.
		 * 
		 * @return the corresponding node
		 */
		public ASTNode getTargetNode();
	}

	/**
	 * AST visitor to rewrite the body of the moved method.
	 */
	public final class MethodBodyRewriter extends ASTVisitor {

		/** The anonymous class nesting counter */
		protected int fAnonymousClass= 0;

		/** The method declaration to rewrite */
		protected final MethodDeclaration fDeclaration;

		/** The set of handled method invocations */
		protected final Set fMethodDeclarations= new HashSet();

		/** The source ast rewrite to use */
		protected final ASTRewrite fRewrite;

		/** The refactoring status */
		protected final RefactoringStatus fStatus= new RefactoringStatus();

		/** The target compilation unit rewrite to use */
		protected final CompilationUnitRewrite fTargetRewrite;

		/**
		 * Creates a new method body rewriter.
		 * 
		 * @param targetRewrite the target compilation unit rewrite to use
		 * @param rewrite the source ast rewrite to use
		 * @param sourceDeclaration the source method declaration
		 */
		public MethodBodyRewriter(final CompilationUnitRewrite targetRewrite, final ASTRewrite rewrite, final MethodDeclaration sourceDeclaration) {
			Assert.isNotNull(targetRewrite);
			Assert.isNotNull(rewrite);
			Assert.isNotNull(sourceDeclaration);
			fTargetRewrite= targetRewrite;
			fRewrite= rewrite;
			fDeclaration= sourceDeclaration;
		}

		public final void endVisit(final AnonymousClassDeclaration node) {
			Assert.isNotNull(node);
			if (fAnonymousClass > 0)
				fAnonymousClass--;
			super.endVisit(node);
		}

		public final boolean visit(final AnonymousClassDeclaration node) {
			Assert.isNotNull(node);
			fAnonymousClass++;
			return super.visit(node);
		}

		public final boolean visit(final ClassInstanceCreation node) {
			Assert.isNotNull(node);
			if (node.getParent() instanceof ClassInstanceCreation) {
				final AnonymousClassDeclaration declaration= node.getAnonymousClassDeclaration();
				if (declaration != null)
					visit(declaration);
				return false;
			}
			return super.visit(node);
		}

		public final boolean visit(final FieldAccess node) {
			Assert.isNotNull(node);
			final Expression expression= node.getExpression();
			final IVariableBinding variable= node.resolveFieldBinding();
			final AST ast= fRewrite.getAST();
			if (expression instanceof ThisExpression) {
				if (Bindings.equals(fTarget, variable)) {
					if (fAnonymousClass > 0) {
						final ThisExpression target= ast.newThisExpression();
						target.setQualifier(ast.newSimpleName(fTargetType.getElementName()));
						fRewrite.replace(node, target, null);
					} else
						fRewrite.replace(node, ast.newThisExpression(), null);
					return false;
				}
			}
			if (expression instanceof FieldAccess) {
				final FieldAccess access= (FieldAccess) expression;
				final IBinding binding= access.getName().resolveBinding();
				if ((access.getExpression() instanceof ThisExpression) && Bindings.equals(fTarget, binding)) {
					fRewrite.replace(node, ast.newSimpleName(node.getName().getIdentifier()), null);
					return false;
				}
			} else if (expression != null) {
				final IMethodBinding method= fDeclaration.resolveBinding();
				if (variable != null && method != null && !JdtFlags.isStatic(variable) && Bindings.equals(variable.getDeclaringClass(), method.getDeclaringClass())) {
					fRewrite.replace(expression, ast.newSimpleName(fTargetName), null);
					return false;
				}
			}
			return true;
		}

		public final void visit(final List nodes) {
			Assert.isNotNull(nodes);
			ASTNode node= null;
			for (final Iterator iterator= nodes.iterator(); iterator.hasNext();) {
				node= (ASTNode) iterator.next();
				node.accept(this);
			}
		}

		public final boolean visit(final MethodInvocation node) {
			Assert.isNotNull(node);
			final Expression expression= node.getExpression();
			final IMethodBinding method= node.resolveMethodBinding();
			if (method != null) {
				final ASTRewrite rewrite= fRewrite;
				if (expression == null) {
					final AST ast= node.getAST();
					if (!JdtFlags.isStatic(method))
						rewrite.set(node, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(fTargetName), null);
					else
						rewrite.set(node, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleType(ast.newSimpleName(fMethod.getDeclaringType().getElementName())), null);
					return true;
				} else {
					if (expression instanceof FieldAccess) {
						final FieldAccess access= (FieldAccess) expression;
						if (Bindings.equals(access.resolveFieldBinding(), fTarget)) {
							rewrite.remove(expression, null);
							visit(node.arguments());
							return false;
						}
					} else if (expression instanceof Name) {
						final Name name= (Name) expression;
						if (Bindings.equals(name.resolveBinding(), fTarget)) {
							rewrite.remove(expression, null);
							visit(node.arguments());
							return false;
						}
					}
				}
			}
			return true;
		}

		public final boolean visit(final QualifiedName node) {
			Assert.isNotNull(node);
			IBinding binding= node.resolveBinding();
			if (binding instanceof ITypeBinding) {
				final ITypeBinding type= (ITypeBinding) binding;
				if (type.isClass() && type.getDeclaringClass() != null) {
					final String name= fTargetRewrite.getImportRewrite().addImport(type);
					if (name != null && name.length() > 0) {
						fRewrite.replace(node, ASTNodeFactory.newName(node.getAST(), name), null);
						return false;
					}
				}
			}
			binding= node.getQualifier().resolveBinding();
			if (Bindings.equals(binding, fTarget)) {
				fRewrite.replace(node, fRewrite.createCopyTarget(node.getName()), null);
				return false;
			}
			return true;
		}

		public final boolean visit(final SimpleName node) {
			Assert.isNotNull(node);
			final AST ast= node.getAST();
			final ASTRewrite rewrite= fRewrite;
			final IBinding binding= node.resolveBinding();
			if (binding instanceof ITypeBinding) {
				final ITypeBinding type= (ITypeBinding) binding;
				if (type.isClass() && type.getDeclaringClass() != null) {
					final String name= fTargetRewrite.getImportRewrite().addImport(type);
					if (name != null && name.length() > 0) {
						fRewrite.replace(node, ASTNodeFactory.newName(ast, name), null);
						return false;
					}
				}
			}
			if (Bindings.equals(binding, fTarget))
				if (fAnonymousClass > 0) {
					final ThisExpression target= ast.newThisExpression();
					target.setQualifier(ast.newSimpleName(fTargetType.getElementName()));
					fRewrite.replace(node, target, null);
				} else
					rewrite.replace(node, ast.newThisExpression(), null);
			else if (binding instanceof IVariableBinding) {
				final IVariableBinding variable= (IVariableBinding) binding;
				final IMethodBinding method= fDeclaration.resolveBinding();
				final ITypeBinding declaring= variable.getDeclaringClass();
				if (variable != null && method != null && Bindings.equals(method.getDeclaringClass(), declaring)) {
					if (JdtFlags.isStatic(variable))
						rewrite.replace(node, ast.newQualifiedName(ASTNodeFactory.newName(ast, fTargetRewrite.getImportRewrite().addImport(declaring)), ast.newSimpleName(node.getFullyQualifiedName())), null);
					else {
						final FieldAccess access= ast.newFieldAccess();
						access.setExpression(ast.newSimpleName(fTargetName));
						access.setName(ast.newSimpleName(node.getFullyQualifiedName()));
						rewrite.replace(node, access, null);
					}
				}
			}
			return false;
		}

		public final boolean visit(final ThisExpression node) {
			Assert.isNotNull(node);
			fRewrite.replace(node, node.getAST().newSimpleName(fTargetName), null);
			return false;
		}
	}

	/**
	 * AST visitor to find read-only fields of the declaring class of 'this'.
	 */
	public static class ReadyOnlyFieldFinder extends ASTVisitor {

		/**
		 * Returns the field binding associated with this expression.
		 * 
		 * @param expression the expression to get the field binding for
		 * @return the field binding, if the expression denotes a field access or a field name, <code>null</code> otherwise
		 */
		protected static IVariableBinding getFieldBinding(final Expression expression) {
			Assert.isNotNull(expression);
			if (expression instanceof FieldAccess)
				return (IVariableBinding) ((FieldAccess) expression).getName().resolveBinding();
			if (expression instanceof Name) {
				final IBinding binding= ((Name) expression).resolveBinding();
				if (binding instanceof IVariableBinding) {
					final IVariableBinding variable= (IVariableBinding) binding;
					if (variable.isField())
						return variable;
				}
			}
			return null;
		}

		/**
		 * Is the specified name a qualified entity, e.g. preceded by 'this', 'super' or part of a method invocation?
		 * 
		 * @param name the name to check
		 * @return <code>true</code> if this entity is qualified, <code>false</code> otherwise
		 */
		protected static boolean isQualifiedEntity(final Name name) {
			Assert.isNotNull(name);
			final ASTNode parent= name.getParent();
			if ((parent instanceof QualifiedName && ((QualifiedName) parent).getName().equals(name)) || (parent instanceof FieldAccess && ((FieldAccess) parent).getName().equals(name)) || (parent instanceof SuperFieldAccess))
				return true;
			else if (parent instanceof MethodInvocation) {
				final MethodInvocation invocation= (MethodInvocation) parent;
				return invocation.getExpression() != null && invocation.getName().equals(name);
			}
			return false;
		}

		/** The list of found bindings */
		protected final List fBindings= new LinkedList();

		/** The keys of the found bindings */
		protected final Set fFound= new HashSet();

		/** The keys of the written bindings */
		protected final Set fWritten= new HashSet();

		/**
		 * Creates a new read only field finder.
		 * 
		 * @param binding The declaring class of the method declaring to find fields for
		 */
		public ReadyOnlyFieldFinder(final ITypeBinding binding) {
			Assert.isNotNull(binding);
			final IVariableBinding[] bindings= binding.getDeclaredFields();
			IVariableBinding variable= null;
			for (int index= 0; index < bindings.length; index++) {
				variable= bindings[index];
				if (!variable.isSynthetic() && !fFound.contains(variable.getKey())) {
					fFound.add(variable.getKey());
					fBindings.add(variable);
				}
			}
		}

		/**
		 * Returns all fields of the declaring class plus the ones references in the visited method declaration.
		 * 
		 * @return all fields of the declaring class plus the references ones
		 */
		public final IVariableBinding[] getDeclaredFields() {
			final IVariableBinding[] result= new IVariableBinding[fBindings.size()];
			fBindings.toArray(result);
			return result;
		}

		/**
		 * Returns all fields of the declaring class which are not written by the visited method declaration.
		 * 
		 * @return all fields which are not written
		 */
		public final IVariableBinding[] getReadOnlyFields() {
			IVariableBinding binding= null;
			final List list= new LinkedList(fBindings);
			for (final Iterator iterator= list.iterator(); iterator.hasNext();) {
				binding= (IVariableBinding) iterator.next();
				if (fWritten.contains(binding.getKey()))
					iterator.remove();
			}
			final IVariableBinding[] result= new IVariableBinding[list.size()];
			list.toArray(result);
			return result;
		}

		public final boolean visit(final Assignment node) {
			Assert.isNotNull(node);
			final IVariableBinding binding= getFieldBinding(node.getLeftHandSide());
			if (binding != null)
				fWritten.add(binding.getKey());
			return true;
		}

		public final boolean visit(final FieldAccess node) {
			Assert.isNotNull(node);
			if (node.getExpression() instanceof ThisExpression) {
				final IVariableBinding binding= (IVariableBinding) node.getName().resolveBinding();
				if (binding != null) {
					final String key= binding.getKey();
					if (!fFound.contains(key)) {
						fFound.add(key);
						fBindings.add(binding);
					}
				}
			}
			return true;
		}

		public final boolean visit(final PostfixExpression node) {
			final IVariableBinding binding= getFieldBinding(node.getOperand());
			if (binding != null)
				fWritten.add(binding.getKey());
			return true;
		}

		public final boolean visit(final PrefixExpression node) {
			final IVariableBinding binding= getFieldBinding(node.getOperand());
			if (binding != null)
				fWritten.add(binding.getKey());
			return false;
		}

		public final boolean visit(final SimpleName node) {
			Assert.isNotNull(node);
			final IBinding binding= node.resolveBinding();
			if (binding != null)
				if (isFieldAccess(node) && !isQualifiedEntity(node)) {
					final IVariableBinding variable= (IVariableBinding) binding;
					final String key= variable.getKey();
					if (!fFound.contains(key)) {
						fFound.add(key);
						fBindings.add(variable);
					}
				}
			return false;
		}
	}

	/**
	 * AST visitor to find recursive calls to the method.
	 */
	public final class RecursiveCallFinder extends AstNodeFinder {

		/** The method binding */
		protected final IMethodBinding fBinding;

		/**
		 * Creates a new recursive call finder.
		 * 
		 * @param declaration the method declaration
		 */
		public RecursiveCallFinder(final MethodDeclaration declaration) {
			Assert.isNotNull(declaration);
			fBinding= declaration.resolveBinding();
		}

		public final boolean visit(final MethodInvocation node) {
			Assert.isNotNull(node);
			final Expression expression= node.getExpression();
			final IMethodBinding binding= node.resolveMethodBinding();
			if (binding == null || (Bindings.equals(binding, fBinding) && (expression == null || expression instanceof ThisExpression))) {
				fStatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.potentially_recursive"), JavaStatusContext.create(fMethod.getCompilationUnit(), node))); //$NON-NLS-1$
				fResult.add(node);
				return false;
			}
			return true;
		}
	}

	/**
	 * AST visitor to find 'super' references.
	 */
	public final class SuperReferenceFinder extends AstNodeFinder {

		public final boolean visit(final AnonymousClassDeclaration node) {
			return false;
		}

		public final boolean visit(final SuperFieldAccess node) {
			Assert.isNotNull(node);
			fStatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.uses_super"), JavaStatusContext.create(fMethod.getCompilationUnit(), node))); //$NON-NLS-1$
			fResult.add(node);
			return false;
		}

		public final boolean visit(final SuperMethodInvocation node) {
			Assert.isNotNull(node);
			fStatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.uses_super"), JavaStatusContext.create(fMethod.getCompilationUnit(), node))); //$NON-NLS-1$
			fResult.add(node);
			return false;
		}

		public final boolean visit(final TypeDeclaration node) {
			return false;
		}
	}

	/**
	 * AST visitor to find references to 'this'.
	 */
	public final class ThisReferenceFinder extends AstNodeFinder {

		public final boolean visit(final MethodInvocation node) {
			Assert.isNotNull(node);
			final IMethodBinding binding= node.resolveMethodBinding();
			if (binding != null && !JdtFlags.isStatic(binding) && node.getExpression() == null) {
				fResult.add(node);
				fStatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.this_reference"), JavaStatusContext.create(fMethod.getCompilationUnit(), node))); //$NON-NLS-1$
			}
			return true;
		}

		public final boolean visit(final SimpleName node) {
			Assert.isNotNull(node);
			if (isFieldAccess(node) && !Bindings.equals(node.resolveBinding(), fTarget)) {
				fResult.add(node);
				fStatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.this_reference"), JavaStatusContext.create(fMethod.getCompilationUnit(), node))); //$NON-NLS-1$
			}
			return false;
		}

		public final boolean visit(final ThisExpression node) {
			Assert.isNotNull(node);
			fResult.add(node);
			fStatus.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.this_reference"), JavaStatusContext.create(fMethod.getCompilationUnit(), node))); //$NON-NLS-1$
			return false;
		}
	}

	/** The identifier of this processor */
	public static final String IDENTIFIER= "org.eclipse.jdt.ui.moveInstanceMethodProcessor"; //$NON-NLS-1$

	/**
	 * Returns the bindings of the method parameters of the specified declaration.
	 * 
	 * @param declaration the method declaration
	 * @return the array of method parameter variable bindings
	 */
	protected static IVariableBinding[] getParameterBindings(final MethodDeclaration declaration) {
		Assert.isNotNull(declaration);
		final List parameters= new ArrayList(declaration.parameters().size());
		VariableDeclaration variable= null;
		IVariableBinding binding= null;
		for (final Iterator iterator= declaration.parameters().iterator(); iterator.hasNext();) {
			variable= (VariableDeclaration) iterator.next();
			binding= variable.resolveBinding();
			if (binding == null)
				return new IVariableBinding[0];
			parameters.add(binding);
		}
		final IVariableBinding[] result= new IVariableBinding[parameters.size()];
		parameters.toArray(result);
		return result;
	}

	/**
	 * Returns the bindings of the method parameters types of the specified declaration.
	 * 
	 * @param declaration the method declaration
	 * @return the array of method parameter variable bindings
	 */
	protected static ITypeBinding[] getParameterTypes(final MethodDeclaration declaration) {
		Assert.isNotNull(declaration);
		final IVariableBinding[] parameters= getParameterBindings(declaration);
		final List types= new ArrayList(parameters.length);
		IVariableBinding binding= null;
		ITypeBinding type= null;
		for (int index= 0; index < parameters.length; index++) {
			binding= parameters[index];
			type= binding.getType();
			if (type != null)
				types.add(type);
		}
		final ITypeBinding[] result= new ITypeBinding[types.size()];
		types.toArray(result);
		return result;
	}

	/**
	 * Is the specified name a field access?
	 * 
	 * @param name the name to check
	 * @return <code>true</code> if this name is a field access, <code>false</code> otherwise
	 */
	protected static boolean isFieldAccess(final SimpleName name) {
		Assert.isNotNull(name);
		final IBinding binding= name.resolveBinding();
		if (!(binding instanceof IVariableBinding))
			return false;
		final IVariableBinding variable= (IVariableBinding) binding;
		if (!variable.isField())
			return false;
		return !Modifier.isStatic(variable.getModifiers());
	}

	/** The candidate targets */
	private IVariableBinding[] fCandidateTargets= new IVariableBinding[0];

	/** The text change manager */
	private TextChangeManager fChangeManager= null;

	/** Should the delegator be deprecated? */
	private boolean fDeprecated= false;

	/** Should the delegator be inlined? */
	private boolean fInline= false;

	/** The method to move */
	private final IMethod fMethod;

	/** The name of the new method to generate */
	private String fMethodName;

	/** The possible targets */
	private IVariableBinding[] fPossibleTargets= new IVariableBinding[0];

	/** Should the delegator be removed after inlining? */
	private boolean fRemove= false;

	/** The code generation settings to apply */
	private final CodeGenerationSettings fSettings;

	/** The source compilation unit rewrite */
	private final CompilationUnitRewrite fSourceRewrite;

	/** The new target */
	private IVariableBinding fTarget= null;

	/** The name of the new target */
	private String fTargetName;

	/** The target type */
	private IType fTargetType= null;

	/** Has the target visibility already been adjusted? */
	private boolean fTargetVisibilityAdjusted= false;

	/** Should getter methods be used to resolve visibility issues? */
	private boolean fUseGetters= true;

	/**
	 * Creates a new move instance method processor.
	 * 
	 * @param method the method to move
	 * @param settings the code generation settings to apply
	 */
	public MoveInstanceMethodProcessor(final IMethod method, final CodeGenerationSettings settings) {
		Assert.isNotNull(method);
		Assert.isNotNull(settings);
		fMethod= method;
		fSettings= settings;
		fSourceRewrite= new CompilationUnitRewrite(fMethod.getCompilationUnit());
		fMethodName= method.getElementName();
		fTargetName= suggestTargetName();
	}

	/**
	 * Checks whether a method with the proposed name already exists in the target type.
	 * 
	 * @param monitor the progress monitor to display progress
	 * @param status the status of the condition checking
	 * @throws JavaModelException if the declared methods of the target type could not be retrieved
	 */
	protected void checkConflictingMethod(final IProgressMonitor monitor, final RefactoringStatus status) throws JavaModelException {
		Assert.isNotNull(monitor);
		Assert.isNotNull(status);
		final IMethod[] methods= fTargetType.getMethods();
		IMethod method= null;
		for (int index= 0; index < methods.length; index++) {
			method= methods[index];
			if (method.getElementName().equals(fMethodName))
				status.merge(RefactoringStatus.createErrorStatus(RefactoringCoreMessages.getFormattedString("MoveInstanceMethodProcessor.method.already.exists", new String[] { fMethodName, fTargetType.getElementName()}), JavaStatusContext.create(method))); //$NON-NLS-1$
		}
		if (fMethodName.equals(fTargetType.getElementName()))
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getFormattedString("MoveInstanceMethodProcessor.method.type.clash", fMethodName), JavaStatusContext.create(fTargetType))); //$NON-NLS-1$
	}

	/**
	 * Checks whether the new target name conflicts with an already existing method parameter.
	 * 
	 * @param monitor the progress monitor to display progress
	 * @param status the status of the condition checking
	 * @throws JavaModelException if the method declaration of the method to move could not be found
	 */
	protected void checkConflictingTarget(final IProgressMonitor monitor, final RefactoringStatus status) throws JavaModelException {
		Assert.isNotNull(monitor);
		Assert.isNotNull(status);
		final MethodDeclaration declaration= ASTNodeSearchUtil.getMethodDeclarationNode(fMethod, fSourceRewrite.getRoot());
		monitor.worked(1);
		VariableDeclaration variable= null;
		for (final Iterator iterator= declaration.parameters().iterator(); iterator.hasNext();) {
			variable= (VariableDeclaration) iterator.next();
			if (fTargetName.equals(variable.getName().getIdentifier())) {
				status.merge(RefactoringStatus.createErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.target_name_already_used"), JavaStatusContext.create(fMethod))); //$NON-NLS-1$
				break;
			}
		}
		monitor.worked(1);
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor, org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext)
	 */
	public final RefactoringStatus checkFinalConditions(final IProgressMonitor monitor, final CheckConditionsContext context) throws CoreException, OperationCanceledException {
		Assert.isNotNull(monitor);
		Assert.isNotNull(context);
		Assert.isNotNull(fTarget);
		final RefactoringStatus status= new RefactoringStatus();
		fChangeManager= new TextChangeManager();
		try {
			monitor.beginTask("", 6); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.checking")); //$NON-NLS-1$
			status.merge(Checks.checkIfCuBroken(fMethod));
			monitor.worked(1);
			if (!status.hasError()) {
				checkGenericTarget(monitor, status);
				if (status.isOK()) {
					final IType type= getTargetType();
					if (type != null) {
						if (type.isBinary() || type.isReadOnly() || !fMethod.exists() || fMethod.isBinary() || fMethod.isReadOnly())
							status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.no_binary"), JavaStatusContext.create(fMethod))); //$NON-NLS-1$
						else {
							status.merge(Checks.checkIfCuBroken(type));
							if (!status.hasError()) {
								if (!type.exists() || type.isBinary() || type.isReadOnly())
									status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.no_binary"), JavaStatusContext.create(fMethod))); //$NON-NLS-1$
								checkConflictingTarget(new SubProgressMonitor(monitor, 1), status);
								checkConflictingMethod(new SubProgressMonitor(monitor, 1), status);
								status.merge(Checks.validateModifiesFiles(computeModifiedFiles(fMethod.getCompilationUnit(), type.getCompilationUnit()), null));
								monitor.worked(1);
								if (!status.hasFatalError())
									fChangeManager= createChangeManager(status, new SubProgressMonitor(monitor, 1));
							}
						}
					} else
						status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.no_resolved_target"), JavaStatusContext.create(fMethod))); //$NON-NLS-1$
				}
				monitor.worked(1);
			}
		} finally {
			monitor.done();
		}
		return status;
	}

	/**
	 * Checks whether the target is a type variable or a generic type.
	 * 
	 * @param monitor the progress monitor to display progress
	 * @param status the refactoring status
	 */
	protected void checkGenericTarget(final IProgressMonitor monitor, final RefactoringStatus status) {
		Assert.isNotNull(monitor);
		Assert.isNotNull(status);
		final ITypeBinding binding= fTarget.getType();
		if (binding == null || binding.isTypeVariable() || binding.isParameterizedType())
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.no_generic_targets"), JavaStatusContext.create(fMethod))); //$NON-NLS-1$
	}

	/**
	 * Checks whether the method has references to type variables or generic types.
	 * 
	 * @param monitor the progress monitor to display progress
	 * @param declaration the method declaration to check for generic types
	 * @param status the status of the condition checking
	 */
	protected void checkGenericTypes(final IProgressMonitor monitor, final MethodDeclaration declaration, final RefactoringStatus status) {
		Assert.isNotNull(monitor);
		Assert.isNotNull(declaration);
		Assert.isNotNull(status);
		final AstNodeFinder finder= new GenericReferenceFinder(declaration);
		declaration.accept(finder);
		monitor.worked(1);
		if (!finder.getStatus().isOK())
			status.merge(finder.getStatus());
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final RefactoringStatus checkInitialConditions(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		Assert.isNotNull(monitor);
		final RefactoringStatus status= new RefactoringStatus();
		try {
			monitor.beginTask("", 10); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.checking")); //$NON-NLS-1$
			status.merge(Checks.checkIfCuBroken(fMethod));
			if (!status.hasError()) {
				checkMethodDeclaration(monitor, status);
				if (status.isOK()) {
					final MethodDeclaration declaration= ASTNodeSearchUtil.getMethodDeclarationNode(fMethod, fSourceRewrite.getRoot());
					checkGenericTypes(monitor, declaration, status);
					checkMethodBody(monitor, declaration, status);
					checkPossibleTargets(monitor, declaration, status);
				}
			}
		} finally {
			monitor.done();
		}
		return status;
	}

	/**
	 * Checks whether the instance method body is compatible with this refactoring.
	 * 
	 * @param monitor the progress monitor to display progress
	 * @param declaration the method declaration whose body to check
	 * @param status the status of the condition checking
	 */
	protected void checkMethodBody(final IProgressMonitor monitor, final MethodDeclaration declaration, final RefactoringStatus status) {
		Assert.isNotNull(monitor);
		Assert.isNotNull(declaration);
		Assert.isNotNull(status);
		AstNodeFinder finder= new SuperReferenceFinder();
		declaration.accept(finder);
		if (!finder.getStatus().isOK())
			status.merge(finder.getStatus());
		monitor.worked(1);
		finder= null;
		final IMethodBinding binding= declaration.resolveBinding();
		if (binding != null) {
			final ITypeBinding declaring= binding.getDeclaringClass();
			if (declaring != null)
				finder= new EnclosingInstanceReferenceFinder(declaring);
		}
		if (finder != null) {
			declaration.accept(finder);
			if (!finder.getStatus().isOK())
				status.merge(finder.getStatus());
			monitor.worked(1);
			finder= new RecursiveCallFinder(declaration);
			declaration.accept(finder);
			if (!finder.getStatus().isOK())
				status.merge(finder.getStatus());
			monitor.worked(1);
		}
	}

	/**
	 * Checks whether the instance method declaration is compatible with this refactoring.
	 * 
	 * @param monitor the progress monitor to display progress
	 * @param status the status of the condition checking
	 * @throws JavaModelException if the method does not exist
	 */
	protected void checkMethodDeclaration(final IProgressMonitor monitor, final RefactoringStatus status) throws JavaModelException {
		Assert.isNotNull(monitor);
		Assert.isNotNull(status);
		final int flags= fMethod.getFlags();
		if (Flags.isStatic(flags))
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.no_static_methods"), JavaStatusContext.create(fMethod))); //$NON-NLS-1$
		else if (Flags.isAbstract(flags))
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.single_implementation"), JavaStatusContext.create(fMethod))); //$NON-NLS-1$
		monitor.worked(1);
		if (Flags.isNative(flags))
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.no_native_methods"), JavaStatusContext.create(fMethod))); //$NON-NLS-1$
		monitor.worked(1);
		if (Flags.isSynchronized(flags))
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.no_synchronized_methods"), JavaStatusContext.create(fMethod))); //$NON-NLS-1$
		monitor.worked(1);
		if (fMethod.isConstructor())
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.no_constructors"), JavaStatusContext.create(fMethod))); //$NON-NLS-1$
		monitor.worked(1);
		if (fMethod.getDeclaringType().isInterface())
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.no_interface"), JavaStatusContext.create(fMethod))); //$NON-NLS-1$
		monitor.worked(1);
	}

	/**
	 * Checks whether the method has possible targets to be moved to
	 * 
	 * @param monitor the progress monitor to display progress
	 * @param declaration the method declaration to check
	 * @param status the status of the condition checking
	 */
	protected void checkPossibleTargets(final IProgressMonitor monitor, final MethodDeclaration declaration, final RefactoringStatus status) {
		Assert.isNotNull(monitor);
		Assert.isNotNull(declaration);
		Assert.isNotNull(status);
		if (computeTargetCategories(declaration).length < 1)
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.cannot_be_moved"), JavaStatusContext.create(fMethod))); //$NON-NLS-1$
		monitor.worked(1);
	}

	/**
	 * Searches for references to the original method.
	 * 
	 * @param monitor the progress monitor to use
	 * @param status the refactoring status to use
	 * @return the array of search result groups
	 * @throws CoreException if an error occurred during search
	 */
	protected SearchResultGroup[] computeMethodReferences(final IProgressMonitor monitor, final RefactoringStatus status) throws CoreException {
		Assert.isNotNull(monitor);
		Assert.isNotNull(status);
		try {
			monitor.beginTask("", 2); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.checking")); //$NON-NLS-1$
			CollectingSearchRequestor requestor= new CollectingSearchRequestor();
			try {
				new SearchEngine().search(SearchPattern.createPattern(fMethod, IJavaSearchConstants.REFERENCES), SearchUtils.getDefaultSearchParticipants(), SearchEngine.createWorkspaceScope(), requestor, new SubProgressMonitor(monitor, 1));
			} catch (CoreException exception) {
				throw new JavaModelException(exception);
			}
			final Map grouped= new HashMap();
			SearchMatch match= null;
			for (final Iterator iterator= requestor.getResults().iterator(); iterator.hasNext();) {
				match= (SearchMatch) iterator.next();
				if (!grouped.containsKey(match.getResource()))
					grouped.put(match.getResource(), new ArrayList(1));
				((List) grouped.get(match.getResource())).add(match);
			}
			IResource resource= null;
			IJavaElement element= null;
			for (final Iterator iterator= grouped.keySet().iterator(); iterator.hasNext();) {
				resource= (IResource) iterator.next();
				element= JavaCore.create(resource);
				if (element == null)
					iterator.remove();
			}
			final SearchResultGroup[] result= new SearchResultGroup[grouped.keySet().size()];
			int index= 0;
			List matches= null;
			for (final Iterator iterator= grouped.keySet().iterator(); iterator.hasNext();) {
				resource= (IResource) iterator.next();
				matches= (List) grouped.get(resource);
				result[index]= new SearchResultGroup(resource, ((SearchMatch[]) matches.toArray(new SearchMatch[matches.size()])));
				index++;
			}
			return result;
		} finally {
			monitor.done();
		}
	}

	/**
	 * Computes the files that are being modified by this refactoring.
	 * 
	 * @param source the source compilation unit
	 * @param target the target compilation unit
	 * @return the modified files
	 */
	protected IFile[] computeModifiedFiles(final ICompilationUnit source, final ICompilationUnit target) {
		Assert.isNotNull(source);
		Assert.isNotNull(target);
		if (source.equals(target))
			return ResourceUtil.getFiles(new ICompilationUnit[] { source});
		return ResourceUtil.getFiles(new ICompilationUnit[] { source, target});
	}

	/**
	 * Returns the reserved identifiers in the method to move.
	 * 
	 * @return the reserved identifiers
	 * @throws JavaModelException if the method declaration could not be found
	 */
	protected String[] computeReservedIdentifiers() throws JavaModelException {
		final CompilationUnit root= fSourceRewrite.getRoot();
		final MethodDeclaration declaration= ASTNodeSearchUtil.getMethodDeclarationNode(fMethod, root);
		final List names= new ArrayList();
		final List parameters= declaration.parameters();
		VariableDeclaration variable= null;
		for (int index= 0; index < parameters.size(); index++) {
			variable= (VariableDeclaration) parameters.get(index);
			names.add(variable.getName().getIdentifier());
		}
		final Block body= declaration.getBody();
		if (body != null) {
			final IBinding[] bindings= new ScopeAnalyzer(root).getDeclarationsAfter(body.getStartPosition(), ScopeAnalyzer.VARIABLES);
			for (int index= 0; index < bindings.length; index++)
				names.add(bindings[index].getName());
		}
		final String[] result= new String[names.size()];
		names.toArray(result);
		return result;
	}

	/**
	 * Computes the target categories for the method to move.
	 * 
	 * @param declaration the method declaration
	 * @return the possible targets as variable bindings of read-only fields and parameters
	 */
	protected IVariableBinding[] computeTargetCategories(final MethodDeclaration declaration) {
		Assert.isNotNull(declaration);
		if (fPossibleTargets.length == 0 || fCandidateTargets.length == 0) {
			final List possibleTargets= new ArrayList(16);
			final List candidateTargets= new ArrayList(16);
			final IMethodBinding method= declaration.resolveBinding();
			if (method != null) {
				final ITypeBinding declaring= method.getDeclaringClass();
				IVariableBinding[] bindings= getParameterBindings(declaration);
				ITypeBinding binding= null;
				for (int index= 0; index < bindings.length; index++) {
					binding= bindings[index].getType();
					if (binding.isClass() && binding.isFromSource() && !Bindings.equals(declaring, binding)) {
						possibleTargets.add(bindings[index]);
						candidateTargets.add(bindings[index]);
					}
				}
				final ReadyOnlyFieldFinder visitor= new ReadyOnlyFieldFinder(declaring);
				declaration.accept(visitor);
				bindings= visitor.getReadOnlyFields();
				for (int index= 0; index < bindings.length; index++) {
					binding= bindings[index].getType();
					if (binding.isClass() && binding.isFromSource() && !Bindings.equals(declaring, binding))
						possibleTargets.add(bindings[index]);
				}
				bindings= visitor.getDeclaredFields();
				for (int index= 0; index < bindings.length; index++) {
					binding= bindings[index].getType();
					if (binding.isClass() && binding.isFromSource() && !Bindings.equals(declaring, binding))
						candidateTargets.add(bindings[index]);
				}
			}
			fPossibleTargets= new IVariableBinding[possibleTargets.size()];
			possibleTargets.toArray(fPossibleTargets);
			fCandidateTargets= new IVariableBinding[candidateTargets.size()];
			candidateTargets.toArray(fCandidateTargets);
		}
		return fPossibleTargets;
	}

	/**
	 * Creates a argument node list for the methods being refactored.
	 * 
	 * @param declaration the original method declaration of the method to move
	 * @param nodes the argument node list to create
	 * @param factory the method argument node factory to use
	 * @return <code>true</code> if a target node had to be inserted as first argument, <code>false</code> otherwise
	 */
	protected boolean createArgumentNodeList(final MethodDeclaration declaration, final List nodes, final IArgumentFactory factory) {
		Assert.isNotNull(declaration);
		Assert.isNotNull(nodes);
		Assert.isNotNull(factory);
		final AstNodeFinder finder= new ThisReferenceFinder();
		declaration.accept(finder);
		IVariableBinding binding= null;
		VariableDeclaration variable= null;
		boolean added= false;
		for (final Iterator iterator= declaration.parameters().iterator(); iterator.hasNext();) {
			variable= (VariableDeclaration) iterator.next();
			binding= variable.resolveBinding();
			if (binding != null) {
				if (!Bindings.equals(binding, fTarget))
					nodes.add(factory.getArgumentNode(binding));
				else if (!finder.getStatus().isOK()) {
					nodes.add(factory.getTargetNode());
					added= true;
				}
			} else
				nodes.add(factory.getArgumentNode(binding));
		}
		if (!finder.getStatus().isOK() && !added) {
			nodes.add(0, factory.getTargetNode());
			added= true;
		}
		return added;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final Change createChange(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			final TextChange[] changes= fChangeManager.getAllChanges();
			if (changes.length == 1)
				return changes[0];
			return new DynamicValidationStateChange(RefactoringCoreMessages.getString("MoveInstanceMethodRefactoring.name"), changes); //$NON-NLS-1$
		} finally {
			monitor.done();
		}
	}

	/**
	 * Creates the text change manager for this processor.
	 * 
	 * @param status the refactoring status
	 * @param monitor the progress monitor to display progress
	 * @return the created text change manager
	 * @throws JavaModelException if the method declaration could not be found
	 * @throws CoreException if the changes could not be generated
	 */
	protected TextChangeManager createChangeManager(final RefactoringStatus status, final IProgressMonitor monitor) throws JavaModelException, CoreException {
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		fSourceRewrite.clearASTAndImportRewrites();
		final TextChangeManager manager= new TextChangeManager();
		final CompilationUnitRewrite targetRewrite= fMethod.getCompilationUnit().equals(getTargetType().getCompilationUnit()) ? fSourceRewrite : new CompilationUnitRewrite(getTargetType().getCompilationUnit());
		final MethodDeclaration declaration= ASTNodeSearchUtil.getMethodDeclarationNode(fMethod, fSourceRewrite.getRoot());
		final SearchResultGroup[] references= computeMethodReferences(new SubProgressMonitor(monitor, 1), status);
		final boolean result= createMethodCopy(declaration, targetRewrite, monitor);
		createMethodJavadocReferences(manager, declaration, targetRewrite, references, result, status, monitor);
		if (!fSourceRewrite.getCu().equals(targetRewrite.getCu()))
			createMethodImports(targetRewrite, monitor);
		boolean removable= false;
		if (fInline) {
			removable= createMethodDelegator(manager, declaration, targetRewrite, references, result, status, monitor);
			if (fRemove && removable) {
				fSourceRewrite.getASTRewrite().remove(declaration, fSourceRewrite.createGroupDescription(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.remove_original_method"))); //$NON-NLS-1$
				fSourceRewrite.getImportRemover().registerRemovedNode(declaration);
			}
		}
		if (!fRemove || !removable)
			createMethodDelegation(declaration, monitor);
		manager.manage(fSourceRewrite.getCu(), fSourceRewrite.createChange());
		if (!fSourceRewrite.getCu().equals(targetRewrite.getCu()))
			manager.manage(targetRewrite.getCu(), targetRewrite.createChange());
		return manager;
	}

	/**
	 * Creates a new expression statement for the method invocation.
	 * 
	 * @param invocation the method invocation
	 * @return the corresponding statement
	 */
	protected ExpressionStatement createExpressionStatement(final MethodInvocation invocation) {
		Assert.isNotNull(invocation);
		return invocation.getAST().newExpressionStatement(invocation);
	}

	/**
	 * Creates the necessary change to inline a method invocation represented by a search match.
	 * 
	 * @param unitRewrite the current compilation unit rewrite
	 * @param sourceDeclaration the source method declaration
	 * @param match the search match representing the method invocation
	 * @param targetNode <code>true</code> if a target node had to be inserted as first argument, <code>false</code> otherwise
	 * @param status the refactoring status
	 * @return <code>true</code> if the inline change could be performed, <code>false</code> otherwise
	 * @throws JavaModelException if a problem occurred while creating the inlined target expression for field targets
	 */
	protected boolean createInlinedMethodInvocation(final CompilationUnitRewrite unitRewrite, final MethodDeclaration sourceDeclaration, final SearchMatch match, final boolean targetNode, final RefactoringStatus status) throws JavaModelException {
		Assert.isNotNull(unitRewrite);
		Assert.isNotNull(sourceDeclaration);
		Assert.isNotNull(match);
		Assert.isNotNull(status);
		boolean result= true;
		final ASTRewrite rewrite= unitRewrite.getASTRewrite();
		final ASTNode node= ASTNodeSearchUtil.findNode(match, unitRewrite.getRoot());
		final TextEditGroup group= unitRewrite.createGroupDescription(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.inline_method_invocation")); //$NON-NLS-1$
		if (node instanceof MethodInvocation) {
			final MethodInvocation invocation= (MethodInvocation) node;
			final ListRewrite rewriter= rewrite.getListRewrite(invocation, MethodInvocation.ARGUMENTS_PROPERTY);
			if (fTarget.isField()) {
				Expression target= null;
				if (invocation.getExpression() != null) {
					target= createInlinedTargetExpression(unitRewrite, sourceDeclaration, invocation.getExpression(), status);
					rewrite.set(invocation, MethodInvocation.EXPRESSION_PROPERTY, target, group);
				} else
					rewrite.set(invocation, MethodInvocation.EXPRESSION_PROPERTY, rewrite.getAST().newSimpleName(fTarget.getName()), group);
				if (targetNode) {
					if (target == null || !(target instanceof FieldAccess))
						rewriter.insertLast(rewrite.getAST().newThisExpression(), null);
					else
						rewriter.insertLast(rewrite.getAST().newSimpleName(fTargetName), null);
				}
			} else {
				final IVariableBinding[] bindings= getParameterBindings(sourceDeclaration);
				if (bindings.length > 0) {
					int index= 0;
					for (; index < bindings.length; index++)
						if (Bindings.equals(bindings[index], fTarget))
							break;
					if (index < bindings.length && invocation.arguments().size() > index) {
						final Expression argument= (Expression) invocation.arguments().get(index);
						if (argument instanceof NullLiteral) {
							status.merge(RefactoringStatus.createErrorStatus(RefactoringCoreMessages.getFormattedString("MoveInstanceMethodProcessor.no_null_argument", BindingLabels.getFullyQualified(sourceDeclaration.resolveBinding())), JavaStatusContext.create(unitRewrite.getCu(), invocation))); //$NON-NLS-1$
							result= false;
						} else {
							if (argument instanceof ThisExpression)
								rewrite.remove(invocation.getExpression(), null);
							else
								rewrite.set(invocation, MethodInvocation.EXPRESSION_PROPERTY, rewrite.createCopyTarget(argument), group);
							if (targetNode) {
								if (invocation.getExpression() != null)
									rewriter.replace(argument, rewrite.createCopyTarget(invocation.getExpression()), group);
								else
									rewriter.replace(argument, rewrite.getAST().newThisExpression(), group);
							} else
								rewriter.remove(argument, group);
						}
					}
				}
			}
			if (result)
				rewrite.set(invocation, MethodInvocation.NAME_PROPERTY, rewrite.getAST().newSimpleName(fMethodName), group);
		}
		return result;
	}

	/**
	 * Creates the target field expression for the inline method invocation.
	 * 
	 * @param unitRewrite the current compilation unit rewrite
	 * @param sourceDeclaration the source method declaration
	 * @param original the original method invocation expression
	 * @param status the refactoring status
	 * @throws JavaModelException if a problem occurred while retrieving potential getter methods of the target
	 */
	protected Expression createInlinedTargetExpression(final CompilationUnitRewrite unitRewrite, final MethodDeclaration sourceDeclaration, final Expression original, final RefactoringStatus status) throws JavaModelException {
		Assert.isNotNull(unitRewrite);
		Assert.isNotNull(original);
		Assert.isTrue(fTarget.isField());
		final ASTRewrite rewrite= fSourceRewrite.getASTRewrite();
		final AST ast= rewrite.getAST();
		final Expression expression= (Expression) ASTNode.copySubtree(ast, original);
		if (!fTargetVisibilityAdjusted && !Modifier.isPublic(fTarget.getModifiers())) {
			boolean samePackage= false;
			final IMethodBinding method= sourceDeclaration.resolveBinding();
			if (method != null) {
				final ITypeBinding sourceType= method.getDeclaringClass();
				if (sourceType != null) {
					final ITypeBinding targetType= fTarget.getType();
					if (targetType != null) {
						if (Bindings.equals(sourceType.getPackage(), targetType.getPackage()))
							samePackage= true;
					}
				}
			}
			final int visibility= samePackage ? 0 : Modifier.PUBLIC;
			final String modifier= samePackage ? RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.change_visibility_default") : RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.change_visibility_public"); //$NON-NLS-1$//$NON-NLS-2$
			final TextEditGroup group= fSourceRewrite.createGroupDescription(RefactoringCoreMessages.getFormattedString("MoveInstanceMethodProcessor.change_visibility", modifier)); //$NON-NLS-1$
			if (fUseGetters) {
				final IField field= (IField) fTarget.getJavaElement();
				if (field != null) {
					final IMethod getter= GetterSetterUtil.getGetter(field);
					if (getter != null) {
						final MethodDeclaration declaration= ASTNodeSearchUtil.getMethodDeclarationNode(getter, fSourceRewrite.getRoot());
						if (declaration != null) {
							final IMethodBinding binding= declaration.resolveBinding();
							final int flags= getter.getFlags();
							if (binding != null && samePackage ? Flags.isPrivate(flags) : ((Flags.isProtected(flags) || Flags.isPrivate(flags)) && !Flags.isPublic(flags))) {
								fTargetVisibilityAdjusted= true;
								ModifierRewrite.create(rewrite, declaration).setVisibility(visibility, group);
								status.merge(RefactoringStatus.createWarningStatus(RefactoringCoreMessages.getFormattedString("MoveInstanceMethodProcessor.change_visibility_method_warning", new String[] { BindingLabels.getFullyQualified(binding), modifier}), JavaStatusContext.create(getter))); //$NON-NLS-1$
							}
							final MethodInvocation invocation= ast.newMethodInvocation();
							invocation.setExpression(expression);
							invocation.setName(ast.newSimpleName(getter.getElementName()));
							return invocation;
						}
					}
				}
			}
			final ASTNode target= fSourceRewrite.getRoot().findDeclaringNode(fTarget);
			if (target instanceof VariableDeclarationFragment) {
				final VariableDeclarationFragment fragment= (VariableDeclarationFragment) target;
				final FieldDeclaration declaration= (FieldDeclaration) fragment.getParent();
				if (declaration.fragments().size() <= 1)
					ModifierRewrite.create(rewrite, declaration).setVisibility(visibility, group);
				else {
					final Type newType= (Type) ASTNode.copySubtree(ast, declaration.getType());
					final VariableDeclarationFragment newFragment= ast.newVariableDeclarationFragment();
					newFragment.setName(ast.newSimpleName(fTarget.getName()));
					final FieldDeclaration newDeclaration= ast.newFieldDeclaration(newFragment);
					newDeclaration.setType(newType);
					rewrite.getListRewrite(declaration.getParent(), TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertAfter(newDeclaration, declaration, null);
					rewrite.getListRewrite(declaration, FieldDeclaration.FRAGMENTS_PROPERTY).remove(fragment, group);
				}
				fTargetVisibilityAdjusted= true;
				status.merge(RefactoringStatus.createWarningStatus(RefactoringCoreMessages.getFormattedString("MoveInstanceMethodProcessor.change_visibility_field_warning", new String[] { BindingLabels.getFullyQualified(fTarget), modifier}), JavaStatusContext.create(fSourceRewrite.getCu(), declaration))); //$NON-NLS-1$
			}
		}
		final FieldAccess access= ast.newFieldAccess();
		access.setExpression(expression);
		access.setName(ast.newSimpleName(fTarget.getName()));
		return access;
	}

	/**
	 * Creates the method body for the target method declaration.
	 * 
	 * @param targetRewrite the target compilation unit rewrite
	 * @param sourceRewrite the source ast rewrite
	 * @param sourceDeclaration the source method declaration
	 */
	protected void createMethodBody(final CompilationUnitRewrite targetRewrite, final ASTRewrite sourceRewrite, final MethodDeclaration sourceDeclaration) {
		Assert.isNotNull(sourceDeclaration);
		sourceDeclaration.getBody().accept(new MethodBodyRewriter(targetRewrite, sourceRewrite, sourceDeclaration));
	}

	/**
	 * Creates the method comment for the target method declaration.
	 * 
	 * @param targetRewrite the target compilation unit rewrite
	 * @param sourceRewriter the source ast rewrite
	 * @param sourceDeclaration the source method declaration
	 * @param parameters the parameter list of the target method
	 * @throws CoreException if the comment template could not be retrieved
	 * @throws JavaModelException if the method properties could not be retrieved
	 */
	protected void createMethodComment(final CompilationUnitRewrite targetRewrite, final ASTRewrite sourceRewriter, final MethodDeclaration sourceDeclaration, final List parameters) throws CoreException, JavaModelException {
		Assert.isNotNull(targetRewrite);
		Assert.isNotNull(sourceRewriter);
		Assert.isNotNull(sourceDeclaration);
		Assert.isNotNull(parameters);
		final Javadoc comment= sourceDeclaration.getJavadoc();
		if (comment != null) {
			final AST ast= sourceRewriter.getAST();
			final Map existing= new HashMap(parameters.size());
			final ListRewrite rewrite= sourceRewriter.getListRewrite(comment, Javadoc.TAGS_PROPERTY);
			boolean found= true;
			String name= null;
			TagElement tag= null;
			TagElement first= null;
			TextElement text= null;
			for (final Iterator tags= comment.tags().iterator(); tags.hasNext();) {
				found= false;
				tag= (TagElement) tags.next();
				name= tag.getTagName();
				if (name != null && name.equals(TagElement.TAG_PARAM)) {
					final List fragments= tag.fragments();
					if (fragments.size() > 0) {
						final ASTNode fragment= (ASTNode) fragments.get(0);
						if (fragment instanceof TextElement) {
							VariableDeclaration declaration= null;
							for (final Iterator iterator= parameters.iterator(); iterator.hasNext();) {
								declaration= (VariableDeclaration) iterator.next();
								text= (TextElement) fragment;
								if (declaration.getName().getIdentifier().equals(text.getText()))
									found= true;
							}
						}
					}
					if (!found)
						rewrite.remove(tag, null);
					else
						existing.put(text.getText(), tag);
				} else
					first= tag;
			}
			tag= first;
			TagElement element= null;
			VariableDeclaration declaration= null;
			for (final Iterator iterator= parameters.iterator(); iterator.hasNext();) {
				declaration= (VariableDeclaration) iterator.next();
				name= declaration.getName().getIdentifier();
				if (existing.containsKey(name))
					tag= (TagElement) existing.get(name);
				else {
					element= ast.newTagElement();
					element.setTagName(TagElement.TAG_PARAM);
					text= ast.newTextElement();
					text.setText(name);
					element.fragments().add(text);
					if (tag != null)
						rewrite.insertAfter(element, tag, null);
					else
						rewrite.insertFirst(element, null);
				}
			}
		} else if (fSettings.createComments) {
			final List list= new ArrayList();
			VariableDeclaration declaration= null;
			for (final Iterator iterator= parameters.iterator(); iterator.hasNext();) {
				declaration= (VariableDeclaration) iterator.next();
				list.add(declaration.getName().getIdentifier());
			}
			final String[] names= new String[list.size()];
			list.toArray(names);
			final String text= CodeGeneration.getMethodComment(targetRewrite.getCu(), getTargetType().getTypeQualifiedName('.'), fMethodName, names, fMethod.getExceptionTypes(), fMethod.getReturnType(), null, StubUtility.getLineDelimiterUsed(getTargetType()));
			if (text != null && text.length() > 0) {
				final Javadoc doc= (Javadoc) sourceRewriter.createStringPlaceholder(text, ASTNode.JAVADOC);
				sourceRewriter.set(sourceDeclaration, MethodDeclaration.JAVADOC_PROPERTY, doc, null);
			}
		}
	}

	/**
	 * Creates the necessary changes to create the delegate method with the original method body.
	 * 
	 * @param sourceDeclaration the method declaration to use as source
	 * @param targetRewrite the target compilation unit rewrite
	 * @param monitor the progress monitor to display progress
	 * @throws CoreException if no buffer could be created for the source compilation unit
	 * @return <code>true</code> if a target node had to be inserted as first argument, <code>false</code> otherwise
	 */
	protected boolean createMethodCopy(final MethodDeclaration sourceDeclaration, final CompilationUnitRewrite targetRewrite, final IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(sourceDeclaration);
		Assert.isNotNull(monitor);
		boolean result= false;
		final IDocument document= new Document(fMethod.getCompilationUnit().getBuffer().getContents());
		try {
			ASTRewrite rewrite= ASTRewrite.create(fSourceRewrite.getRoot().getAST());
			final List parameters= new ArrayList(sourceDeclaration.parameters().size());
			rewrite.set(sourceDeclaration, MethodDeclaration.NAME_PROPERTY, rewrite.getAST().newSimpleName(fMethodName), null);
			result= createMethodParameters(targetRewrite, rewrite, sourceDeclaration, parameters);
			createMethodComment(targetRewrite, rewrite, sourceDeclaration, parameters);
			createMethodBody(targetRewrite, rewrite, sourceDeclaration);
			final String content= createMethodDeclaration(document, sourceDeclaration, rewrite);
			rewrite= targetRewrite.getASTRewrite();
			final MethodDeclaration targetDeclaration= (MethodDeclaration) rewrite.createStringPlaceholder(content, ASTNode.METHOD_DECLARATION);
			final TypeDeclaration type= ASTNodeSearchUtil.getTypeDeclarationNode(getTargetType(), targetRewrite.getRoot());
			rewrite.getListRewrite(type, TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertAt(targetDeclaration, ASTNodes.getInsertionIndex(targetDeclaration, type.bodyDeclarations()), targetRewrite.createGroupDescription(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.add_moved_method"))); //$NON-NLS-1$
		} catch (MalformedTreeException exception) {
			JavaPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaPlugin.log(exception);
		} finally {
			if (fMethod.getCompilationUnit().equals(getTargetType().getCompilationUnit()))
				targetRewrite.clearImportRewrites();
		}
		return result;
	}

	/**
	 * Creates the method declaration of the moved method.
	 * 
	 * @param document the document representing the source compilation unit
	 * @param sourceDeclaration the source method declaration
	 * @param rewrite the ast rewrite to use
	 * @return the string representing the moved method body
	 * @throws BadLocationException if an offset into the document is invalid
	 */
	protected String createMethodDeclaration(final IDocument document, final MethodDeclaration sourceDeclaration, final ASTRewrite rewrite) throws BadLocationException {
		Assert.isNotNull(document);
		Assert.isNotNull(sourceDeclaration);
		Assert.isNotNull(rewrite);
		final IRegion range= new Region(sourceDeclaration.getStartPosition(), sourceDeclaration.getLength());
		final RangeMarker marker= new RangeMarker(range.getOffset(), range.getLength());
		final TextEdit[] edits= rewrite.rewriteAST(document, fMethod.getJavaProject().getOptions(true)).removeChildren();
		for (int index= 0; index < edits.length; index++)
			marker.addChild(edits[index]);
		final MultiTextEdit result= new MultiTextEdit();
		result.addChild(marker);
		final TextEditProcessor processor= new TextEditProcessor(document, new MultiTextEdit(0, document.getLength()), TextEdit.UPDATE_REGIONS);
		processor.getRoot().addChild(result);
		processor.performEdits();
		final int width= CodeFormatterUtil.getTabWidth();
		final IRegion region= document.getLineInformation(document.getLineOfOffset(marker.getOffset()));
		return Strings.changeIndent(document.get(marker.getOffset(), marker.getLength()), Strings.computeIndent(document.get(region.getOffset(), region.getLength()), width), width, "", StubUtility.getLineDelimiterFor(document)); //$NON-NLS-1$
	}

	/**
	 * Creates the necessary changes to replace the body of the method declaration with an expression to invoke the delegate.
	 * 
	 * @param declaration the method declaration to replace its body
	 * @param monitor the progress monitor to display progress
	 * @throws CoreException if the change could not be generated
	 * @return <code>true</code> if a target node had to be inserted as first argument, <code>false</code> otherwise
	 */
	protected boolean createMethodDelegation(final MethodDeclaration declaration, final IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(declaration);
		Assert.isNotNull(monitor);
		final AST ast= fSourceRewrite.getRoot().getAST();
		final ASTRewrite rewrite= fSourceRewrite.getASTRewrite();
		final ImportRemover remover= fSourceRewrite.getImportRemover();
		final MethodInvocation invocation= ast.newMethodInvocation();
		invocation.setName(ast.newSimpleName(fMethodName));
		invocation.setExpression(createTargetAccessExpression(declaration));
		final boolean result= createArgumentNodeList(declaration, invocation.arguments(), new IArgumentFactory() {

			public final ASTNode getArgumentNode(final IVariableBinding binding) {
				Assert.isNotNull(binding);
				return ast.newSimpleName(binding.getName());
			}

			public final ASTNode getTargetNode() {
				return ast.newThisExpression();
			}
		});
		final Block block= ast.newBlock();
		block.statements().add(createMethodInvocation(declaration, invocation));
		remover.registerRemovedNode(declaration.getBody());
		rewrite.set(declaration, MethodDeclaration.BODY_PROPERTY, block, fSourceRewrite.createGroupDescription(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.replace_body_with_delegation"))); //$NON-NLS-1$
		if (fDeprecated)
			createMethodDeprecation(declaration);
		return result;
	}

	/**
	 * Creates the necessary changes to inline the method invocations to the original method.
	 * 
	 * @param manager text change manager
	 * @param sourceDeclaration the source method declaration
	 * @param targetRewrite the target compilation unit rewrite
	 * @param groups the search result groups representing all references to the moved method, including references in comments
	 * @param targetNode <code>true</code> if a target node must be inserted as first argument, <code>false</code> otherwise
	 * @param status the refactoring status
	 * @param monitor the progress monitor to use
	 * @return <code>true</code> if all method invocations to the original method declaration could be inlined, <code>false</code> otherwise
	 */
	protected boolean createMethodDelegator(final TextChangeManager manager, final MethodDeclaration sourceDeclaration, final CompilationUnitRewrite targetRewrite, final SearchResultGroup[] groups, final boolean targetNode, final RefactoringStatus status, final IProgressMonitor monitor) {
		Assert.isNotNull(manager);
		Assert.isNotNull(sourceDeclaration);
		Assert.isNotNull(targetRewrite);
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask("", 3); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.checking")); //$NON-NLS-1$
			try {
				boolean result= true;
				boolean readonly= false;
				boolean found= false;
				final ITypeHierarchy hierarchy= fMethod.getDeclaringType().newTypeHierarchy(new SubProgressMonitor(monitor, 1));
				IType type= null;
				IMethod method= null;
				IType[] types= hierarchy.getAllSubtypes(fMethod.getDeclaringType());
				for (int index= 0; index < types.length && !found; index++) {
					type= types[index];
					method= JavaModelUtil.findMethod(fMethod.getElementName(), fMethod.getParameterTypes(), false, type);
					if (method != null)
						found= true;
				}
				types= hierarchy.getAllSupertypes(fMethod.getDeclaringType());
				for (int index= 0; index < types.length && !found; index++) {
					type= types[index];
					method= JavaModelUtil.findMethod(fMethod.getElementName(), fMethod.getParameterTypes(), false, type);
					if (method != null)
						found= true;
				}
				if (found) {
					status.merge(RefactoringStatus.createWarningStatus(RefactoringCoreMessages.getFormattedString("MoveInstanceMethodProcessor.inline.overridden", BindingLabels.getFullyQualified(sourceDeclaration.resolveBinding())), JavaStatusContext.create(fMethod))); //$NON-NLS-1$
					result= false;
				} else {
					monitor.worked(1);
					SearchMatch match= null;
					SearchMatch[] matches= null;
					ICompilationUnit unit= null;
					CompilationUnitRewrite rewrite= null;
					SearchResultGroup group= null;
					for (int index= 0; index < groups.length; index++) {
						group= groups[index];
						unit= group.getCompilationUnit();
						if (unit != null) {
							types= unit.getAllTypes();
							for (int offset= 0; offset < types.length; offset++) {
								if (types[offset].isReadOnly()) {
									readonly= true;
									break;
								}
							}
						} else
							readonly= true;
						if (!readonly) {
							matches= group.getSearchResults();
							if (fSourceRewrite.getCu().equals(unit))
								rewrite= fSourceRewrite;
							else if (targetRewrite.getCu().equals(unit))
								rewrite= targetRewrite;
							else
								rewrite= new CompilationUnitRewrite(unit);
							for (int offset= 0; offset < matches.length; offset++) {
								match= matches[offset];
								if (match.getAccuracy() == SearchMatch.A_INACCURATE) {
									final SearchMatch context= match;
									status.merge(RefactoringStatus.createWarningStatus(RefactoringCoreMessages.getFormattedString("MoveInstanceMethodProcessor.inline.inaccurate", unit.getCorrespondingResource().getName()), JavaStatusContext.create(unit, new ISourceRange() { //$NON-NLS-1$

												public final int getLength() {
													return context.getLength();
												}

												public final int getOffset() {
													return context.getOffset();
												}
											})));
									result= false;
								} else if (!createInlinedMethodInvocation(rewrite, sourceDeclaration, match, targetNode, status))
									result= false;
							}
							if (!fSourceRewrite.getCu().equals(unit) && !targetRewrite.getCu().equals(unit))
								manager.manage(unit, rewrite.createChange());
						} else {
							final IJavaElement element= JavaCore.create(group.getResource());
							if (element != null && element.exists())
								status.merge(RefactoringStatus.createWarningStatus(RefactoringCoreMessages.getFormattedString("MoveInstanceMethodProcessor.inline.binary.project", element.getJavaProject().getElementName()))); //$NON-NLS-1$
							else
								status.merge(RefactoringStatus.createWarningStatus(RefactoringCoreMessages.getFormattedString("MoveInstanceMethodProcessor.inline.binary.resource", group.getResource().getName()))); //$NON-NLS-1$
							result= false;
						}
					}
				}
				return result;
			} catch (CoreException exception) {
				status.merge(RefactoringStatus.create(exception.getStatus()));
				return false;
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Creates the method deprecation message for the original method.
	 * 
	 * @param declaration the method declaration to create the message for
	 */
	protected void createMethodDeprecation(final MethodDeclaration declaration) {
		Assert.isNotNull(declaration);
		final AST ast= fSourceRewrite.getRoot().getAST();
		final ASTRewrite rewrite= fSourceRewrite.getASTRewrite();
		final String[] tokens= Strings.splitByToken(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.deprecate_delegator_message"), " "); //$NON-NLS-1$ //$NON-NLS-2$
		final List fragments= new ArrayList(tokens.length);
		String element= null;
		for (int index= 0; index < tokens.length; index++) {
			element= tokens[index];
			if (element != null && element.length() > 0) {
				if (element.equals("{0}")) //$NON-NLS-1$
					fragments.add(createMethodReference(declaration, ast));
				else {
					final TextElement text= ast.newTextElement();
					text.setText(element);
					fragments.add(text);
				}
			}
		}
		final TagElement tag= ast.newTagElement();
		tag.setTagName(TagElement.TAG_DEPRECATED);
		tag.fragments().addAll(fragments);
		Javadoc comment= declaration.getJavadoc();
		if (comment == null) {
			comment= ast.newJavadoc();
			comment.tags().add(tag);
			rewrite.set(declaration, MethodDeclaration.JAVADOC_PROPERTY, comment, fSourceRewrite.createGroupDescription(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.deprecate_delegator_method"))); //$NON-NLS-1$
		} else
			rewrite.getListRewrite(comment, Javadoc.TAGS_PROPERTY).insertLast(tag, fSourceRewrite.createGroupDescription(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.deprecate_delegator_method"))); //$NON-NLS-1$
	}

	/**
	 * Creates the necessary imports for the copied method in the target compilation unit.
	 * 
	 * @param targetRewrite the target compilation unit rewrite
	 * @param monitor the progress monitor to use
	 * @throws JavaModelException if the types referenced in the method could not be determined
	 */
	protected void createMethodImports(final CompilationUnitRewrite targetRewrite, final IProgressMonitor monitor) throws JavaModelException {
		Assert.isNotNull(targetRewrite);
		Assert.isNotNull(monitor);
		final IType[] references= ReferenceFinderUtil.getTypesReferencedIn(new IJavaElement[] { fMethod}, monitor);
		final ImportRewrite rewrite= targetRewrite.getImportRewrite();
		final ImportRemover remover= targetRewrite.getImportRemover();
		String name= null;
		for (int index= 0; index < references.length; index++) {
			name= JavaModelUtil.getFullyQualifiedName(references[index]);
			rewrite.addImport(name);
			remover.registerAddedImport(name);
		}
	}

	/**
	 * Creates the corresponding statement for the method invocation, based on the return type.
	 * 
	 * @param declaration the method declaration where the invocation statement is inserted
	 * @param invocation the method invocation being encapsulated by the resulting statement
	 * @return the corresponding statement
	 */
	protected Statement createMethodInvocation(final MethodDeclaration declaration, final MethodInvocation invocation) {
		Assert.isNotNull(declaration);
		Assert.isNotNull(invocation);
		Statement statement= null;
		final Type type= declaration.getReturnType2();
		if (type == null)
			statement= createExpressionStatement(invocation);
		else {
			if (type instanceof PrimitiveType) {
				final PrimitiveType primitive= (PrimitiveType) type;
				if (primitive.getPrimitiveTypeCode().equals(PrimitiveType.VOID))
					statement= createExpressionStatement(invocation);
				else
					statement= createReturnStatement(invocation);
			} else
				statement= createReturnStatement(invocation);
		}
		return statement;
	}

	/**
	 * Creates the necessary change to updated a comment reference represented by a search match.
	 * 
	 * @param unitRewrite the current compilation unit rewrite
	 * @param sourceDeclaration the source method declaration
	 * @param match the search match representing the method reference
	 * @param targetNode <code>true</code> if a target node had to be inserted as first argument, <code>false</code> otherwise
	 * @param status the refactoring status
	 */
	protected void createMethodJavadocReference(final CompilationUnitRewrite unitRewrite, final MethodDeclaration sourceDeclaration, final SearchMatch match, final boolean targetNode, final RefactoringStatus status) {
		Assert.isNotNull(unitRewrite);
		Assert.isNotNull(sourceDeclaration);
		Assert.isNotNull(match);
		Assert.isNotNull(status);
		final ASTNode node= ASTNodeSearchUtil.findNode(match, unitRewrite.getRoot());
		if (node instanceof MethodRef) {
			final AST ast= node.getAST();
			final MethodRef successor= ast.newMethodRef();

			unitRewrite.getASTRewrite().replace(node, successor, null);
		}
	}

	/**
	 * Creates the necessary changes to update tag references to the original method.
	 * 
	 * @param manager text change manager
	 * @param sourceDeclaration the source method declaration
	 * @param targetRewrite the target compilation unit rewrite
	 * @param groups the search result groups representing all references to the moved method, including references in comments
	 * @param targetNode <code>true</code> if a target node must be inserted as first argument, <code>false</code> otherwise
	 * @param status the refactoring status
	 * @param monitor the progress monitor to use
	 */
	protected void createMethodJavadocReferences(final TextChangeManager manager, final MethodDeclaration sourceDeclaration, final CompilationUnitRewrite targetRewrite, final SearchResultGroup[] groups, final boolean targetNode, final RefactoringStatus status, final IProgressMonitor monitor) {
		Assert.isNotNull(manager);
		Assert.isNotNull(sourceDeclaration);
		Assert.isNotNull(targetRewrite);
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask("", 3); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.checking")); //$NON-NLS-1$
			try {
				SearchMatch[] matches= null;
				ICompilationUnit unit= null;
				CompilationUnitRewrite rewrite= null;
				SearchResultGroup group= null;
				for (int index= 0; index < groups.length; index++) {
					group= groups[index];
					unit= group.getCompilationUnit();
					if (unit != null) {
						matches= group.getSearchResults();
						if (fSourceRewrite.getCu().equals(unit))
							rewrite= fSourceRewrite;
						else if (targetRewrite.getCu().equals(unit))
							rewrite= targetRewrite;
						else
							rewrite= new CompilationUnitRewrite(unit);
						SearchMatch match= null;
						for (int offset= 0; offset < matches.length; offset++) {
							match= matches[offset];
							if (match.getAccuracy() == SearchMatch.A_INACCURATE) {
								final SearchMatch context= match;
								status.merge(RefactoringStatus.createWarningStatus(RefactoringCoreMessages.getFormattedString("MoveInstanceMethodProcessor.inline.inaccurate", unit.getCorrespondingResource().getName()), JavaStatusContext.create(unit, new ISourceRange() { //$NON-NLS-1$

											public final int getLength() {
												return context.getLength();
											}

											public final int getOffset() {
												return context.getOffset();
											}
										})));
							} else
								createMethodJavadocReference(rewrite, sourceDeclaration, match, targetNode, status);
						}
						if (!fSourceRewrite.getCu().equals(unit) && !targetRewrite.getCu().equals(unit))
							manager.manage(unit, rewrite.createChange());
					} else {
						final IJavaElement element= JavaCore.create(group.getResource());
						if (element != null && element.exists())
							status.merge(RefactoringStatus.createWarningStatus(RefactoringCoreMessages.getFormattedString("MoveInstanceMethodProcessor.javadoc.binary.project", element.getJavaProject().getElementName()))); //$NON-NLS-1$
						else
							status.merge(RefactoringStatus.createWarningStatus(RefactoringCoreMessages.getFormattedString("MoveInstanceMethodProcessor.javadoc.binary.resource", group.getResource().getName()))); //$NON-NLS-1$
					}
				}
			} catch (CoreException exception) {
				status.merge(RefactoringStatus.create(exception.getStatus()));
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Creates the method parameters for the target method declaration.
	 * 
	 * @param targetRewrite the compilation unit target rewrite
	 * @param sourceRewriter the source ast rewrite
	 * @param sourceDeclaration the source method declaration
	 * @param parameters the parameter list to create
	 * @return <code>true</code> if a target node had to be inserted as first argument, <code>false</code> otherwise
	 */
	protected boolean createMethodParameters(final CompilationUnitRewrite targetRewrite, final ASTRewrite sourceRewriter, final MethodDeclaration sourceDeclaration, final List parameters) {
		Assert.isNotNull(targetRewrite);
		Assert.isNotNull(sourceDeclaration);
		Assert.isNotNull(sourceRewriter);
		Assert.isNotNull(parameters);
		final AST ast= targetRewrite.getRoot().getAST();
		final AstNodeFinder finder= new AnonymousClassReferenceFinder(sourceDeclaration);
		sourceDeclaration.accept(finder);
		final boolean result= createArgumentNodeList(sourceDeclaration, parameters, new IArgumentFactory() {

			public final ASTNode getArgumentNode(final IVariableBinding binding) {
				Assert.isNotNull(binding);
				final SingleVariableDeclaration declaration= ast.newSingleVariableDeclaration();
				declaration.setType(ASTNodeFactory.newType(ast, binding.getType(), false));
				declaration.setName(ast.newSimpleName(binding.getName()));
				return declaration;
			}

			public final ASTNode getTargetNode() {
				final SingleVariableDeclaration declaration= ast.newSingleVariableDeclaration();
				final IMethodBinding method= sourceDeclaration.resolveBinding();
				if (method != null) {
					final ITypeBinding declaring= method.getDeclaringClass();
					if (declaring != null) {
						final String type= targetRewrite.getImportRewrite().addImport(declaring);
						declaration.setType(ASTNodeFactory.newType(ast, type));
						declaration.setName(ast.newSimpleName(fTargetName));
						if (finder.getResult().size() > 0)
							declaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD));
					}
				}
				return declaration;
			}
		});
		final ListRewrite list= sourceRewriter.getListRewrite(sourceDeclaration, MethodDeclaration.PARAMETERS_PROPERTY);
		ASTNode node= null;
		for (final Iterator iterator= sourceDeclaration.parameters().iterator(); iterator.hasNext();) {
			node= (ASTNode) iterator.next();
			list.remove(node, null);
		}
		for (final Iterator iterator= parameters.iterator(); iterator.hasNext();) {
			node= (ASTNode) iterator.next();
			list.insertLast(node, null);
		}
		return result;
	}

	/**
	 * Creates a comment method reference to the moved method
	 * 
	 * @param declaration the method declaration of the original method
	 * @param ast the ast to create the method reference for
	 * @return the created link tag to reference the method
	 */
	protected TagElement createMethodReference(final MethodDeclaration declaration, final AST ast) {
		Assert.isNotNull(ast);
		Assert.isNotNull(declaration);
		final MethodRef reference= ast.newMethodRef();
		reference.setName(ast.newSimpleName(fMethodName));
		reference.setQualifier(ASTNodeFactory.newName(ast, JavaModelUtil.getFullyQualifiedName(fTargetType)));
		createArgumentNodeList(declaration, reference.parameters(), new IArgumentFactory() {

			public final ASTNode getArgumentNode(final IVariableBinding binding) {
				Assert.isNotNull(binding);
				final MethodRefParameter parameter= ast.newMethodRefParameter();
				parameter.setType(ASTNodeFactory.newType(ast, binding.getType(), true));
				return parameter;
			}

			public final ASTNode getTargetNode() {
				final MethodRefParameter parameter= ast.newMethodRefParameter();
				final IMethodBinding method= declaration.resolveBinding();
				if (method != null) {
					final ITypeBinding declaring= method.getDeclaringClass();
					if (declaring != null)
						parameter.setType(ASTNodeFactory.newType(ast, Bindings.getFullyQualifiedName(declaring)));
				}
				return parameter;
			}
		});
		final TagElement element= ast.newTagElement();
		element.setTagName(TagElement.TAG_LINK);
		element.fragments().add(reference);
		return element;
	}

	/**
	 * Creates a new return statement for the method invocation.
	 * 
	 * @param invocation the method invocation to create a return statement for
	 * @return the corresponding statement
	 */
	protected ReturnStatement createReturnStatement(final MethodInvocation invocation) {
		Assert.isNotNull(invocation);
		final ReturnStatement statement= invocation.getAST().newReturnStatement();
		statement.setExpression(invocation);
		return statement;
	}

	/**
	 * Creates the expression to access the new target.
	 * 
	 * @param declaration the method declaration where to access the target
	 * @return the corresponding expression
	 */
	protected Expression createTargetAccessExpression(final MethodDeclaration declaration) {
		Assert.isNotNull(declaration);
		Expression expression= null;
		final AST ast= declaration.getAST();
		final ITypeBinding type= fTarget.getDeclaringClass();
		if (type != null) {
			boolean shadows= false;
			final IVariableBinding[] bindings= getParameterBindings(declaration);
			IVariableBinding variable= null;
			for (int index= 0; index < bindings.length; index++) {
				variable= bindings[index];
				if (fMethod.getDeclaringType().getField(variable.getName()).exists()) {
					shadows= true;
					break;
				}
			}
			if (fSettings.useKeywordThis || shadows) {
				final FieldAccess access= ast.newFieldAccess();
				access.setName(ast.newSimpleName(fTarget.getName()));
				access.setExpression(ast.newThisExpression());
				expression= access;
			} else
				expression= ast.newSimpleName(fTarget.getName());
		} else
			expression= ast.newSimpleName(fTarget.getName());
		return expression;
	}

	/**
	 * Returns the candidate targets for the method to move.
	 * 
	 * @return the candidate targets as variable bindings of fields and parameters
	 */
	public final IVariableBinding[] getCandidateTargets() {
		Assert.isNotNull(fCandidateTargets);
		return fCandidateTargets;
	}

	/**
	 * Should the delegator be deprecated?
	 * 
	 * @return <code>true</code> if the delegator should be deprecated, <code>false</code> otherwise
	 */
	public final boolean getDeprecated() {
		return fDeprecated;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getElements()
	 */
	public final Object[] getElements() {
		return new Object[] { fMethod};
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getIdentifier()
	 */
	public final String getIdentifier() {
		return IDENTIFIER;
	}

	/**
	 * Returns the method to be moved.
	 * 
	 * @return the method to be moved
	 */
	public final IMethod getMethod() {
		return fMethod;
	}

	/**
	 * Returns the new method name.
	 * 
	 * @return the name of the new method
	 */
	public final String getMethodName() {
		return fMethodName;
	}

	/**
	 * Returns the possible targets for the method to move.
	 * 
	 * @return the possible targets as variable bindings of read-only fields and parameters
	 */
	public final IVariableBinding[] getPossibleTargets() {
		Assert.isNotNull(fPossibleTargets);
		return fPossibleTargets;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getProcessorName()
	 */
	public final String getProcessorName() {
		return RefactoringCoreMessages.getFormattedString("MoveInstanceMethodProcessor.name", new String[] { fMethod.getElementName()}); //$NON-NLS-1$
	}

	/**
	 * Returns the new target name.
	 * 
	 * @return the name of the new target
	 */
	public final String getTargetName() {
		return fTargetName;
	}

	/**
	 * Returns the type of the new target.
	 * 
	 * @return the type of the new target
	 * @throws JavaModelException if the type does not exist
	 */
	protected IType getTargetType() throws JavaModelException {
		Assert.isNotNull(fTarget);
		if (fTargetType == null)
			fTargetType= Bindings.findType(fTarget.getType(), fMethod.getJavaProject());
		return fTargetType;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#isApplicable()
	 */
	public final boolean isApplicable() throws CoreException {
		return fMethod.exists() && !fMethod.isConstructor() && !fMethod.isBinary() && !fMethod.isReadOnly() && fMethod.getCompilationUnit() != null && !JdtFlags.isStatic(fMethod) && !fMethod.getDeclaringType().isLocal();
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#loadParticipants(org.eclipse.ltk.core.refactoring.RefactoringStatus, org.eclipse.ltk.core.refactoring.participants.SharableParticipants)
	 */
	public final RefactoringParticipant[] loadParticipants(final RefactoringStatus status, final SharableParticipants participants) throws CoreException {
		return new RefactoringParticipant[0];
	}

	/**
	 * Determines whether the delegator has to be deprecated.
	 * 
	 * @param deprecate <code>true</code> if the delegator has to be deprecated, <code>false</code> otherwise
	 */
	public final void setDeprecated(final boolean deprecate) {
		fDeprecated= deprecate;
	}

	/**
	 * Determines whether the delegator has to be inlined.
	 * 
	 * @param inline <code>true</code> to inline the delegator, <code>false</code> otherwise
	 */
	public final void setInlineDelegator(final boolean inline) {
		fInline= inline;
	}

	/**
	 * Sets the new method name.
	 * 
	 * @param name the name to set
	 * @return the status of the operation
	 */
	public final RefactoringStatus setMethodName(final String name) {
		Assert.isNotNull(name);
		RefactoringStatus status= Checks.checkMethodName(name);
		if (status.hasFatalError())
			return status;
		fMethodName= name;
		return status;
	}

	/**
	 * Determines whether the delegator has to be removed after inlining. Note that the option to inline the delegator has to be enabled if this method is called with the argument <code>true</code>.
	 * 
	 * @param remove <code>true</code> if it should be removed, <code>false</code> otherwise
	 */
	public final void setRemoveDelegator(final boolean remove) {
		Assert.isTrue(!remove || fInline);
		fRemove= remove;
	}

	/**
	 * Sets the new target.
	 * 
	 * @param target the target to set
	 */
	public final void setTarget(final IVariableBinding target) {
		Assert.isNotNull(target);
		fTarget= target;
	}

	/**
	 * Sets the new target name.
	 * 
	 * @param name the name to set
	 * @return the status of the operation
	 */
	public final RefactoringStatus setTargetName(final String name) {
		Assert.isNotNull(name);
		final RefactoringStatus status= Checks.checkTempName(name);
		if (status.hasFatalError())
			return status;
		fTargetName= name;
		return status;
	}

	/**
	 * Determines whether getter methods should be used to resolve visibility issues.
	 * 
	 * @param use <code>true</code> if getter methods should be used, <code>false</code> otherwise
	 */
	public final void setUseGetters(final boolean use) {
		fUseGetters= use;
	}

	/**
	 * Should getter methods be used to resolve visibility issues?
	 * 
	 * @return <code>true</code> if getter methods should be used, <code>false</code> otherwise
	 */
	public final boolean shouldUseGetters() {
		return fUseGetters;
	}

	/**
	 * Returns a best guess for the name of the new target.
	 * 
	 * @return a best guess for the name
	 */
	protected String suggestTargetName() {
		final IType type= fMethod.getDeclaringType();
		final String name= type.getFullyQualifiedName();
		try {
			final String[] candidates= NamingConventions.suggestArgumentNames(fMethod.getJavaProject(), type.getPackageFragment().getElementName(), name, 0, computeReservedIdentifiers()); //$NON-NLS-1$
			if (candidates.length > 0) {
				if (candidates[0].indexOf('$') < 0)
					return candidates[0];
			}
		} catch (JavaModelException exception) {
			JavaPlugin.log(exception);
		}
		return ""; //$NON-NLS-1$
	}
}