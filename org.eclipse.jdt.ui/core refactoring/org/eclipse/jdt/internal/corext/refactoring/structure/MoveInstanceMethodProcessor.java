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
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
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
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;

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
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine2;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.util.JavadocUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.BindingLabels;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
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
		 * @param last <code>true</code> if the argument represented by this node is the last one in its declaring method
		 * @return the corresponding node
		 * @throws JavaModelException if an error occurs
		 */
		public ASTNode getArgumentNode(IVariableBinding binding, boolean last) throws JavaModelException;

		/**
		 * Returns a target node for the current target.
		 * 
		 * @return the corresponding node
		 * @throws JavaModelException if an error occurs
		 */
		public ASTNode getTargetNode() throws JavaModelException;
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
				if (variable != null && method != null && !JdtFlags.isStatic(variable) && Bindings.equals(method.getDeclaringClass(), variable.getDeclaringClass())) {
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

		public final boolean visit(final AnnotationTypeDeclaration node) {
			return false;
		}

		public final boolean visit(final AnonymousClassDeclaration node) {
			return false;
		}

		public final boolean visit(final EnumDeclaration node) {
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

	/**
	 * Argument factory which adjusts the visibilities of the argument types.
	 */
	public class VisibilityAdjustingArgumentFactory implements IArgumentFactory {

		/** The visibility adjustments */
		private final Map fAdjustments;

		/** The ast to use for new nodes */
		private final AST fAst;

		/** The compilation unit rewrites */
		private final Map fRewrites;

		/**
		 * Creates a new visibility adjusting argument factory.
		 * 
		 * @param ast the ast to use for new nodes
		 * @param rewrites the compilation unit rewrites
		 * @param adjustments the map of elements to visibility adjustments
		 */
		public VisibilityAdjustingArgumentFactory(final AST ast, final Map rewrites, final Map adjustments) {
			Assert.isNotNull(ast);
			Assert.isNotNull(rewrites);
			Assert.isNotNull(adjustments);
			fAst= ast;
			fRewrites= rewrites;
			fAdjustments= adjustments;
		}

		protected final void adjustTypeVisibility(final ITypeBinding binding) throws JavaModelException {
			Assert.isNotNull(binding);
			final IJavaElement element= binding.getJavaElement();
			if (element instanceof IType) {
				final IType type= (IType) element;
				if (!type.isBinary() && !type.isReadOnly() && !Flags.isPublic(type.getFlags())) {
					boolean same= false;
					final CompilationUnitRewrite rewrite= getCompilationUnitRewrite(fRewrites, type.getCompilationUnit());
					final AbstractTypeDeclaration declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(type, rewrite.getRoot());
					if (declaration != null) {
						final ITypeBinding declaring= declaration.resolveBinding();
						if (declaring != null && Bindings.equals(binding.getPackage(), fTarget.getType().getPackage()))
							same= true;
						final Modifier.ModifierKeyword keyword= same ? null : Modifier.ModifierKeyword.PUBLIC_KEYWORD;
						final String modifier= same ? RefactoringCoreMessages.getString("MemberVisibilityAdjustor.change_visibility_default") : RefactoringCoreMessages.getString("MemberVisibilityAdjustor.change_visibility_public"); //$NON-NLS-1$ //$NON-NLS-2$
						if (MemberVisibilityAdjustor.hasLowerVisibility(binding.getModifiers(), same ? Modifier.NONE : (keyword == null ? Modifier.NONE : keyword.toFlagValue())) && MemberVisibilityAdjustor.needsVisibilityAdjustments(type, keyword, fAdjustments))
							fAdjustments.put(type, new MemberVisibilityAdjustor.OutgoingMemberVisibilityAdjustment(type, keyword, RefactoringStatus.createWarningStatus(RefactoringCoreMessages.getFormattedString("MemberVisibilityAdjustor.change_visibility_type_warning", new String[] { BindingLabels.getFullyQualified(declaration.resolveBinding()), modifier}), JavaStatusContext.create(type.getCompilationUnit(), declaration)))); //$NON-NLS-1$
					}
				}
			}
		}

		public ASTNode getArgumentNode(final IVariableBinding binding, final boolean last) throws JavaModelException {
			Assert.isNotNull(binding);
			adjustTypeVisibility(binding.getType());
			return fAst.newSimpleName(binding.getName());
		}

		public ASTNode getTargetNode() throws JavaModelException {
			return fAst.newThisExpression();
		}
	}

	/** The identifier of this processor */
	public static final String IDENTIFIER= "org.eclipse.jdt.ui.moveInstanceMethodProcessor"; //$NON-NLS-1$

	/**
	 * Returns the bindings of the method arguments of the specified declaration.
	 * 
	 * @param declaration the method declaration
	 * @return the array of method argument variable bindings
	 */
	protected static IVariableBinding[] getArgumentBindings(final MethodDeclaration declaration) {
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
	 * Returns the bindings of the method argument types of the specified declaration.
	 * 
	 * @param declaration the method declaration
	 * @return the array of method argument variable bindings
	 */
	protected static ITypeBinding[] getArgumentTypes(final MethodDeclaration declaration) {
		Assert.isNotNull(declaration);
		final IVariableBinding[] parameters= getArgumentBindings(declaration);
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

	/** Should getter methods be used to resolve visibility issues? */
	private boolean fUseGetters= true;

	/** Should setter methods be used to resolve visibility issues? */
	private boolean fUseSetters= true;

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
		try {
			monitor.beginTask("", methods.length); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.checking")); //$NON-NLS-1$
			IMethod method= null;
			for (int index= 0; index < methods.length; index++) {
				method= methods[index];
				if (method.getElementName().equals(fMethodName))
					status.merge(RefactoringStatus.createErrorStatus(RefactoringCoreMessages.getFormattedString("MoveInstanceMethodProcessor.method.already.exists", new String[] { fMethodName, fTargetType.getElementName()}), JavaStatusContext.create(method))); //$NON-NLS-1$
				monitor.worked(1);
			}
			if (fMethodName.equals(fTargetType.getElementName()))
				status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getFormattedString("MoveInstanceMethodProcessor.method.type.clash", fMethodName), JavaStatusContext.create(fTargetType))); //$NON-NLS-1$
		} finally {
			monitor.done();
		}
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
		VariableDeclaration variable= null;
		final List parameters= declaration.parameters();
		try {
			monitor.beginTask("", parameters.size()); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.checking")); //$NON-NLS-1$
			for (final Iterator iterator= parameters.iterator(); iterator.hasNext();) {
				variable= (VariableDeclaration) iterator.next();
				if (fTargetName.equals(variable.getName().getIdentifier())) {
					status.merge(RefactoringStatus.createErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.target_name_already_used"), JavaStatusContext.create(fMethod))); //$NON-NLS-1$
					break;
				}
				monitor.worked(1);
			}
		} finally {
			monitor.done();
		}
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
			monitor.beginTask("", 4); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.checking")); //$NON-NLS-1$
			status.merge(Checks.checkIfCuBroken(fMethod));
			if (!status.hasError()) {
				checkGenericTarget(new SubProgressMonitor(monitor, 1), status);
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
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.checking")); //$NON-NLS-1$
			final ITypeBinding binding= fTarget.getType();
			if (binding == null || binding.isTypeVariable() || binding.isParameterizedType())
				status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.no_generic_targets"), JavaStatusContext.create(fMethod))); //$NON-NLS-1$
		} finally {
			monitor.done();
		}
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
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.checking")); //$NON-NLS-1$
			final AstNodeFinder finder= new GenericReferenceFinder(declaration);
			declaration.accept(finder);
			if (!finder.getStatus().isOK())
				status.merge(finder.getStatus());
		} finally {
			monitor.done();
		}
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final RefactoringStatus checkInitialConditions(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		Assert.isNotNull(monitor);
		final RefactoringStatus status= new RefactoringStatus();
		try {
			monitor.beginTask("", 4); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.checking")); //$NON-NLS-1$
			status.merge(Checks.checkIfCuBroken(fMethod));
			if (!status.hasError()) {
				checkMethodDeclaration(new SubProgressMonitor(monitor, 1), status);
				if (status.isOK()) {
					final MethodDeclaration declaration= ASTNodeSearchUtil.getMethodDeclarationNode(fMethod, fSourceRewrite.getRoot());
					checkGenericTypes(new SubProgressMonitor(monitor, 1), declaration, status);
					checkMethodBody(new SubProgressMonitor(monitor, 1), declaration, status);
					checkPossibleTargets(new SubProgressMonitor(monitor, 1), declaration, status);
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
		try {
			monitor.beginTask("", 3); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.checking")); //$NON-NLS-1$
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
		} finally {
			monitor.done();
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
		try {
			monitor.beginTask("", 5); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.checking")); //$NON-NLS-1$
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
			if (fMethod.getDeclaringType().isAnnotation())
				status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.no_annotation"), JavaStatusContext.create(fMethod))); //$NON-NLS-1$
			else if (fMethod.getDeclaringType().isInterface())
				status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.no_interface"), JavaStatusContext.create(fMethod))); //$NON-NLS-1$
			monitor.worked(1);
		} finally {
			monitor.done();
		}
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
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.checking")); //$NON-NLS-1$
			if (computeTargetCategories(declaration).length < 1)
				status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.cannot_be_moved"), JavaStatusContext.create(fMethod))); //$NON-NLS-1$
		} finally {
			monitor.done();
		}
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
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.checking")); //$NON-NLS-1$
			final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2(SearchPattern.createPattern(fMethod, IJavaSearchConstants.REFERENCES));
			engine.setStatus(status);
			engine.searchPattern(new SubProgressMonitor(monitor, 1));
			return (SearchResultGroup[]) engine.getResults();
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
		final List names= new ArrayList();
		final MethodDeclaration declaration= ASTNodeSearchUtil.getMethodDeclarationNode(fMethod, fSourceRewrite.getRoot());
		if (declaration != null) {
			final List parameters= declaration.parameters();
			VariableDeclaration variable= null;
			for (int index= 0; index < parameters.size(); index++) {
				variable= (VariableDeclaration) parameters.get(index);
				names.add(variable.getName().getIdentifier());
			}
			final Block body= declaration.getBody();
			if (body != null) {
				final IBinding[] bindings= new ScopeAnalyzer(fSourceRewrite.getRoot()).getDeclarationsAfter(body.getStartPosition(), ScopeAnalyzer.VARIABLES);
				for (int index= 0; index < bindings.length; index++)
					names.add(bindings[index].getName());
			}
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
				IVariableBinding[] bindings= getArgumentBindings(declaration);
				ITypeBinding binding= null;
				for (int index= 0; index < bindings.length; index++) {
					binding= bindings[index].getType();
					if ((binding.isClass() || binding.isEnum()) && binding.isFromSource() && !Bindings.equals(declaring, binding)) {
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
	 * Creates a generic argument list of the refactored moved method
	 * 
	 * @param declaration the method declaration of the method to move
	 * @param arguments the argument list to create
	 * @param factory the argument factory to use
	 * @return <code>true</code> if a target node had to be inserted as first argument, <code>false</code> otherwise
	 * @throws JavaModelException if an error occurs
	 */
	protected boolean createArgumentList(final MethodDeclaration declaration, final List arguments, final IArgumentFactory factory) throws JavaModelException {
		Assert.isNotNull(declaration);
		Assert.isNotNull(arguments);
		Assert.isNotNull(factory);
		final AstNodeFinder finder= new ThisReferenceFinder();
		declaration.accept(finder);
		IVariableBinding binding= null;
		VariableDeclaration variable= null;
		boolean added= false;
		final int size= declaration.parameters().size();
		for (int index= 0; index < size; index++) {
			variable= (VariableDeclaration) declaration.parameters().get(index);
			binding= variable.resolveBinding();
			if (binding != null) {
				if (!Bindings.equals(binding, fTarget))
					arguments.add(factory.getArgumentNode(binding, index == size - 1));
				else if (!finder.getStatus().isOK()) {
					arguments.add(factory.getTargetNode());
					added= true;
				}
			} else
				arguments.add(factory.getArgumentNode(binding, index == size - 1));
		}
		if (!finder.getStatus().isOK() && !added) {
			arguments.add(0, factory.getTargetNode());
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
			monitor.beginTask("", 6); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.creating")); //$NON-NLS-1$
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
		try {
			monitor.beginTask("", 7); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.creating")); //$NON-NLS-1$
			fSourceRewrite.clearASTAndImportRewrites();
			final TextChangeManager manager= new TextChangeManager();
			final CompilationUnitRewrite targetRewrite= fMethod.getCompilationUnit().equals(getTargetType().getCompilationUnit()) ? fSourceRewrite : new CompilationUnitRewrite(getTargetType().getCompilationUnit());
			final MethodDeclaration declaration= ASTNodeSearchUtil.getMethodDeclarationNode(fMethod, fSourceRewrite.getRoot());
			final SearchResultGroup[] references= computeMethodReferences(new SubProgressMonitor(monitor, 1), status);
			final Map rewrites= new HashMap(2);
			rewrites.put(fSourceRewrite.getCu(), fSourceRewrite);
			if (!fSourceRewrite.getCu().equals(targetRewrite.getCu()))
				rewrites.put(targetRewrite.getCu(), targetRewrite);
			final ASTRewrite sourceRewrite= ASTRewrite.create(fSourceRewrite.getRoot().getAST());
			final MemberVisibilityAdjustor adjustor= new MemberVisibilityAdjustor(fTargetType, fMethod);
			adjustor.setStatus(status);
			adjustor.setGetters(fUseGetters);
			adjustor.setSetters(fUseSetters);
			adjustor.setVisibilitySeverity(RefactoringStatus.WARNING);
			adjustor.setFailureSeverity(RefactoringStatus.WARNING);
			adjustor.setRewrites(rewrites);
			adjustor.setRewrite(sourceRewrite, fSourceRewrite.getRoot());
			adjustor.adjustVisibility(new SubProgressMonitor(monitor, 1));
			final boolean target= createMethodCopy(declaration, rewrites, sourceRewrite, adjustor.getAdjustments(), status, new SubProgressMonitor(monitor, 1));
			createMethodJavadocReferences(rewrites, declaration, references, target, status, new SubProgressMonitor(monitor, 1));
			if (!fSourceRewrite.getCu().equals(targetRewrite.getCu()))
				createMethodImports(targetRewrite, new SubProgressMonitor(monitor, 1));
			boolean removable= false;
			if (fInline) {
				removable= createMethodDelegator(rewrites, declaration, references, adjustor.getAdjustments(), target, status, new SubProgressMonitor(monitor, 1));
				if (fRemove && removable) {
					fSourceRewrite.getASTRewrite().remove(declaration, fSourceRewrite.createGroupDescription(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.remove_original_method"))); //$NON-NLS-1$
					if (!fSourceRewrite.getCu().equals(fTargetType.getCompilationUnit()))
						fSourceRewrite.getImportRemover().registerRemovedNode(declaration);
				}
			}
			if (!fRemove || !removable)
				createMethodDelegation(declaration, rewrites, adjustor.getAdjustments(), status, new SubProgressMonitor(monitor, 1));
			adjustor.rewriteVisibility(new SubProgressMonitor(monitor, 1));
			ICompilationUnit unit= null;
			CompilationUnitRewrite rewrite= null;
			for (final Iterator iterator= rewrites.keySet().iterator(); iterator.hasNext();) {
				unit= (ICompilationUnit) iterator.next();
				rewrite= (CompilationUnitRewrite) rewrites.get(unit);
				manager.manage(unit, rewrite.createChange());
			}
			return manager;
		} finally {
			monitor.done();
		}
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
	 * @param rewriter the current compilation unit rewrite
	 * @param declaration the source method declaration
	 * @param match the search match representing the method invocation
	 * @param adjustments the map of elements to visibility adjustments
	 * @param target <code>true</code> if a target node had to be inserted as first argument, <code>false</code> otherwise
	 * @param status the refactoring status
	 * @return <code>true</code> if the inline change could be performed, <code>false</code> otherwise
	 * @throws JavaModelException if a problem occurred while creating the inlined target expression for field targets
	 */
	protected boolean createInlinedMethodInvocation(final CompilationUnitRewrite rewriter, final MethodDeclaration declaration, final SearchMatch match, final Map adjustments, final boolean target, final RefactoringStatus status) throws JavaModelException {
		Assert.isNotNull(rewriter);
		Assert.isNotNull(declaration);
		Assert.isNotNull(match);
		Assert.isNotNull(adjustments);
		Assert.isNotNull(status);
		boolean result= true;
		final ASTRewrite rewrite= rewriter.getASTRewrite();
		final ASTNode node= ASTNodeSearchUtil.findNode(match, rewriter.getRoot());
		final TextEditGroup group= rewriter.createGroupDescription(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.inline_method_invocation")); //$NON-NLS-1$
		if (node instanceof MethodInvocation) {
			final MethodInvocation invocation= (MethodInvocation) node;
			final ListRewrite list= rewrite.getListRewrite(invocation, MethodInvocation.ARGUMENTS_PROPERTY);
			if (fTarget.isField()) {
				Expression expression= null;
				if (invocation.getExpression() != null) {
					expression= createInlinedTargetExpression(rewriter, declaration, invocation.getExpression(), adjustments, status);
					rewrite.set(invocation, MethodInvocation.EXPRESSION_PROPERTY, expression, group);
				} else
					rewrite.set(invocation, MethodInvocation.EXPRESSION_PROPERTY, rewrite.getAST().newSimpleName(fTarget.getName()), group);
				if (target) {
					if (expression == null || !(expression instanceof FieldAccess))
						list.insertFirst(rewrite.getAST().newThisExpression(), null);
					else
						list.insertLast(rewrite.createCopyTarget(invocation.getExpression()), null);
				}
			} else {
				final IVariableBinding[] bindings= getArgumentBindings(declaration);
				if (bindings.length > 0) {
					int index= 0;
					for (; index < bindings.length; index++)
						if (Bindings.equals(bindings[index], fTarget))
							break;
					if (index < bindings.length && invocation.arguments().size() > index) {
						final Expression argument= (Expression) invocation.arguments().get(index);
						if (argument instanceof NullLiteral) {
							status.merge(RefactoringStatus.createErrorStatus(RefactoringCoreMessages.getFormattedString("MoveInstanceMethodProcessor.no_null_argument", BindingLabels.getFullyQualified(declaration.resolveBinding())), JavaStatusContext.create(rewriter.getCu(), invocation))); //$NON-NLS-1$
							result= false;
						} else {
							if (argument instanceof ThisExpression)
								rewrite.remove(invocation.getExpression(), null);
							else
								rewrite.set(invocation, MethodInvocation.EXPRESSION_PROPERTY, rewrite.createCopyTarget(argument), group);
							if (target) {
								if (invocation.getExpression() != null)
									list.replace(argument, rewrite.createCopyTarget(invocation.getExpression()), group);
								else
									list.replace(argument, rewrite.getAST().newThisExpression(), group);
							} else
								list.remove(argument, group);
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
	 * @param rewriter the current compilation unit rewrite
	 * @param declaration the source method declaration
	 * @param original the original method invocation expression
	 * @param adjustments the map of elements to visibility adjustments
	 * @param status the refactoring status
	 * @throws JavaModelException if a problem occurred while retrieving potential getter methods of the target
	 */
	protected Expression createInlinedTargetExpression(final CompilationUnitRewrite rewriter, final MethodDeclaration declaration, final Expression original, final Map adjustments, final RefactoringStatus status) throws JavaModelException {
		Assert.isNotNull(rewriter);
		Assert.isNotNull(declaration);
		Assert.isNotNull(original);
		Assert.isNotNull(adjustments);
		Assert.isNotNull(status);
		Assert.isTrue(fTarget.isField());
		final ASTRewrite rewrite= fSourceRewrite.getASTRewrite();
		final Expression expression= (Expression) ASTNode.copySubtree(rewrite.getAST(), original);
		final IJavaElement element= fTarget.getJavaElement();
		if (element != null && !Modifier.isPublic(fTarget.getModifiers())) {
			final IField field= (IField) fTarget.getJavaElement();
			if (field != null) {
				boolean same= false;
				IMethodBinding binding= declaration.resolveBinding();
				if (binding != null) {
					final ITypeBinding declaring= binding.getDeclaringClass();
					if (declaring != null && Bindings.equals(declaring.getPackage(), fTarget.getType().getPackage()))
						same= true;
				}
				final Modifier.ModifierKeyword keyword= same ? null : Modifier.ModifierKeyword.PUBLIC_KEYWORD;
				final String modifier= same ? RefactoringCoreMessages.getString("MemberVisibilityAdjustor.change_visibility_default") : RefactoringCoreMessages.getString("MemberVisibilityAdjustor.change_visibility_public"); //$NON-NLS-1$//$NON-NLS-2$
				if (fUseGetters) {
					final IMethod getter= GetterSetterUtil.getGetter(field);
					if (getter != null) {
						final MethodDeclaration method= ASTNodeSearchUtil.getMethodDeclarationNode(getter, fSourceRewrite.getRoot());
						if (method != null) {
							binding= method.resolveBinding();
							if (binding != null && MemberVisibilityAdjustor.hasLowerVisibility(getter.getFlags(), same ? Modifier.NONE : (keyword == null ? Modifier.NONE : keyword.toFlagValue())) && MemberVisibilityAdjustor.needsVisibilityAdjustments(getter, keyword, adjustments))
								adjustments.put(getter, new MemberVisibilityAdjustor.OutgoingMemberVisibilityAdjustment(getter, keyword, RefactoringStatus.createWarningStatus(RefactoringCoreMessages.getFormattedString("MemberVisibilityAdjustor.change_visibility_method_warning", new String[] { BindingLabels.getFullyQualified(binding), modifier}), JavaStatusContext.create(getter)))); //$NON-NLS-1$
							final MethodInvocation invocation= rewrite.getAST().newMethodInvocation();
							invocation.setExpression(expression);
							invocation.setName(rewrite.getAST().newSimpleName(getter.getElementName()));
							return invocation;
						}
					}

				}
				if (MemberVisibilityAdjustor.hasLowerVisibility(field.getFlags(), keyword.toFlagValue()) && MemberVisibilityAdjustor.needsVisibilityAdjustments(field, keyword, adjustments))
					adjustments.put(field, new MemberVisibilityAdjustor.OutgoingMemberVisibilityAdjustment(field, keyword, RefactoringStatus.createWarningStatus(RefactoringCoreMessages.getFormattedString("MemberVisibilityAdjustor.change_visibility_field_warning", new String[] { BindingLabels.getFullyQualified(fTarget), modifier}), JavaStatusContext.create(field)))); //$NON-NLS-1$
			}
		}
		final FieldAccess access= rewrite.getAST().newFieldAccess();
		access.setExpression(expression);
		access.setName(rewrite.getAST().newSimpleName(fTarget.getName()));
		return access;
	}

	/**
	 * Creates the method arguments for the target method declaration.
	 * 
	 * @param rewrites the compilation unit rewrites
	 * @param rewrite the source ast rewrite
	 * @param declaration the source method declaration
	 * @param adjustments the map of elements to visibility adjustments
	 * @param status the refactoring status
	 * @return <code>true</code> if a target node had to be inserted as method argument, <code>false</code> otherwise
	 * @throws JavaModelException if an error occurs while accessing the types of the arguments
	 */
	protected boolean createMethodArguments(final Map rewrites, final ASTRewrite rewrite, final MethodDeclaration declaration, final Map adjustments, final RefactoringStatus status) throws JavaModelException {
		Assert.isNotNull(rewrites);
		Assert.isNotNull(declaration);
		Assert.isNotNull(rewrite);
		Assert.isNotNull(adjustments);
		Assert.isNotNull(status);
		final CompilationUnitRewrite rewriter= getCompilationUnitRewrite(rewrites, getTargetType().getCompilationUnit());
		final AST ast= rewriter.getRoot().getAST();
		final AstNodeFinder finder= new AnonymousClassReferenceFinder(declaration);
		declaration.accept(finder);
		final List arguments= new ArrayList(declaration.parameters().size() + 1);
		final boolean result= createArgumentList(declaration, arguments, new VisibilityAdjustingArgumentFactory(ast, rewrites, adjustments) {

			public final ASTNode getArgumentNode(final IVariableBinding binding, final boolean last) throws JavaModelException {
				Assert.isNotNull(binding);
				final SingleVariableDeclaration variable= ast.newSingleVariableDeclaration();
				final ITypeBinding type= binding.getType();
				adjustTypeVisibility(type);
				variable.setName(ast.newSimpleName(binding.getName()));
				variable.modifiers().addAll(ast.newModifiers(binding.getModifiers()));
				final IMethodBinding method= binding.getDeclaringMethod();
				if (last && method != null && method.isVarargs()) {
					variable.setVarargs(true);
					variable.setType(ast.newSimpleType(ast.newSimpleName(type.isArray() ? type.getElementType().getName() : type.getName())));
				} else
					variable.setType(ASTNodeFactory.newType(ast, type, false));
				return variable;
			}

			public final ASTNode getTargetNode() throws JavaModelException {
				final SingleVariableDeclaration variable= ast.newSingleVariableDeclaration();
				final IMethodBinding method= declaration.resolveBinding();
				if (method != null) {
					final ITypeBinding declaring= method.getDeclaringClass();
					if (declaring != null) {
						adjustTypeVisibility(declaring);
						final String type= rewriter.getImportRewrite().addImport(declaring);
						variable.setType(ASTNodeFactory.newType(ast, type));
						variable.setName(ast.newSimpleName(fTargetName));
						if (finder.getResult().size() > 0)
							variable.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD));
					}
				}
				return variable;
			}
		});
		final ListRewrite list= rewrite.getListRewrite(declaration, MethodDeclaration.PARAMETERS_PROPERTY);
		ASTNode node= null;
		for (final Iterator iterator= declaration.parameters().iterator(); iterator.hasNext();) {
			node= (ASTNode) iterator.next();
			list.remove(node, null);
		}
		for (final Iterator iterator= arguments.iterator(); iterator.hasNext();) {
			node= (ASTNode) iterator.next();
			list.insertLast(node, null);
		}
		return result;
	}

	/**
	 * Creates the method body for the target method declaration.
	 * 
	 * @param rewriter the target compilation unit rewrite
	 * @param rewrite the source ast rewrite
	 * @param declaration the source method declaration
	 */
	protected void createMethodBody(final CompilationUnitRewrite rewriter, final ASTRewrite rewrite, final MethodDeclaration declaration) {
		Assert.isNotNull(declaration);
		declaration.getBody().accept(new MethodBodyRewriter(rewriter, rewrite, declaration));
	}

	/**
	 * Creates the method comment for the target method declaration.
	 * 
	 * @param rewrite the source ast rewrite
	 * @param declaration the source method declaration
	 * @throws JavaModelException if the argument references could not be generated
	 */
	protected void createMethodComment(final ASTRewrite rewrite, final MethodDeclaration declaration) throws JavaModelException {
		Assert.isNotNull(rewrite);
		Assert.isNotNull(declaration);
		final Javadoc comment= declaration.getJavadoc();
		if (comment != null) {
			final List tags= new LinkedList(comment.tags());
			final IVariableBinding[] bindings= getArgumentBindings(declaration);
			final Map elements= new HashMap(bindings.length);
			String name= null;
			List fragments= null;
			TagElement element= null;
			TagElement reference= null;
			IVariableBinding binding= null;
			for (int index= 0; index < bindings.length; index++) {
				binding= bindings[index];
				for (final Iterator iterator= comment.tags().iterator(); iterator.hasNext();) {
					element= (TagElement) iterator.next();
					name= element.getTagName();
					fragments= element.fragments();
					if (name != null) {
						if (name.equals(TagElement.TAG_PARAM) && !fragments.isEmpty() && fragments.get(0) instanceof SimpleName) {
							final SimpleName simple= (SimpleName) fragments.get(0);
							if (binding.getName().equals(simple.getIdentifier())) {
								elements.put(binding.getKey(), element);
								tags.remove(element);
							}
						} else if (reference == null)
							reference= element;
					}
				}
			}
			if (bindings.length == 0 && reference == null) {
				for (final Iterator iterator= comment.tags().iterator(); iterator.hasNext();) {
					element= (TagElement) iterator.next();
					name= element.getTagName();
					fragments= element.fragments();
					if (name != null && !name.equals(TagElement.TAG_PARAM))
						reference= element;
				}
			}
			final List arguments= new ArrayList(bindings.length + 1);
			createArgumentList(declaration, arguments, new IArgumentFactory() {

				public final ASTNode getArgumentNode(final IVariableBinding argument, final boolean last) throws JavaModelException {
					Assert.isNotNull(argument);
					if (elements.containsKey(argument.getKey()))
						return rewrite.createCopyTarget((ASTNode) elements.get(argument.getKey()));
					return JavadocUtil.createParamTag(argument.getName(), declaration.getAST(), fMethod.getJavaProject());
				}

				public final ASTNode getTargetNode() throws JavaModelException {
					return JavadocUtil.createParamTag(fTargetName, declaration.getAST(), fMethod.getJavaProject());
				}
			});
			final ListRewrite rewriter= rewrite.getListRewrite(comment, Javadoc.TAGS_PROPERTY);
			ASTNode tag= null;
			for (final Iterator iterator= comment.tags().iterator(); iterator.hasNext();) {
				tag= (ASTNode) iterator.next();
				if (!tags.contains(tag))
					rewriter.remove(tag, null);
			}
			for (final Iterator iterator= arguments.iterator(); iterator.hasNext();) {
				tag= (ASTNode) iterator.next();
				if (reference != null)
					rewriter.insertBefore(tag, reference, null);
				else
					rewriter.insertLast(tag, null);
			}
		}
	}

	/**
	 * Creates the necessary changes to create the delegate method with the original method body.
	 * 
	 * @param declaration the method declaration to use as source
	 * @param rewrites the compilation unit rewrites
	 * @param rewrite the ast rewrite to use for the copy of the method body
	 * @param adjustments the map of elements to visibility adjustments
	 * @param status the refactoring status
	 * @param monitor the progress monitor to display progress
	 * @throws CoreException if no buffer could be created for the source compilation unit
	 * @return <code>true</code> if a target node had to be inserted as first argument, <code>false</code> otherwise
	 */
	protected boolean createMethodCopy(final MethodDeclaration declaration, final Map rewrites, final ASTRewrite rewrite, final Map adjustments, final RefactoringStatus status, final IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(declaration);
		Assert.isNotNull(rewrites);
		Assert.isNotNull(rewrite);
		Assert.isNotNull(adjustments);
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		boolean target= false;
		final IDocument document= new Document(fMethod.getCompilationUnit().getBuffer().getContents());
		final CompilationUnitRewrite rewriter= getCompilationUnitRewrite(rewrites, getTargetType().getCompilationUnit());
		try {
			rewrite.set(declaration, MethodDeclaration.NAME_PROPERTY, rewrite.getAST().newSimpleName(fMethodName), null);
			boolean same= false;
			final IMethodBinding binding= declaration.resolveBinding();
			if (binding != null) {
				final ITypeBinding declaring= binding.getDeclaringClass();
				if (declaring != null && Bindings.equals(declaring.getPackage(), fTarget.getType().getPackage()))
					same= true;
				final Modifier.ModifierKeyword keyword= same ? null : Modifier.ModifierKeyword.PUBLIC_KEYWORD;
				if (MemberVisibilityAdjustor.hasLowerVisibility(binding.getModifiers(), same ? Modifier.NONE : (keyword == null ? Modifier.NONE : keyword.toFlagValue())) && MemberVisibilityAdjustor.needsVisibilityAdjustments(fMethod, keyword, adjustments)) {
					final MemberVisibilityAdjustor.IncomingMemberVisibilityAdjustment adjustment= new MemberVisibilityAdjustor.IncomingMemberVisibilityAdjustment(fMethod, keyword, RefactoringStatus.createStatus(RefactoringStatus.WARNING, RefactoringCoreMessages.getFormattedString("MemberVisibilityAdjustor.change_visibility_method_warning", new String[] { MemberVisibilityAdjustor.getLabel(fMethod), MemberVisibilityAdjustor.getLabel(keyword)}), JavaStatusContext.create(fMethod), null, RefactoringStatusEntry.NO_CODE, null)); //$NON-NLS-1$
					ModifierRewrite.create(rewrite, declaration).setVisibility(keyword == null ? Modifier.NONE : keyword.toFlagValue(), null);
					adjustment.setNeedsRewriting(false);
					adjustments.put(fMethod, adjustment);
				}
			}
			target= createMethodArguments(rewrites, rewrite, declaration, adjustments, status);
			createMethodComment(rewrite, declaration);
			createMethodBody(rewriter, rewrite, declaration);
			final String source= createMethodDeclaration(document, declaration, rewrite);
			final MethodDeclaration targetDeclaration= (MethodDeclaration) rewriter.getASTRewrite().createStringPlaceholder(source, ASTNode.METHOD_DECLARATION);
			final AbstractTypeDeclaration type= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(getTargetType(), rewriter.getRoot());
			rewriter.getASTRewrite().getListRewrite(type, type.getBodyDeclarationsProperty()).insertAt(targetDeclaration, ASTNodes.getInsertionIndex(targetDeclaration, type.bodyDeclarations()), rewriter.createGroupDescription(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.add_moved_method"))); //$NON-NLS-1$
		} catch (MalformedTreeException exception) {
			JavaPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaPlugin.log(exception);
		} finally {
			if (fMethod.getCompilationUnit().equals(getTargetType().getCompilationUnit()))
				rewriter.clearImportRewrites();
		}
		return target;
	}

	/**
	 * Creates the method declaration of the moved method.
	 * 
	 * @param document the document representing the source compilation unit
	 * @param declaration the source method declaration
	 * @param rewrite the ast rewrite to use
	 * @return the string representing the moved method body
	 * @throws BadLocationException if an offset into the document is invalid
	 */
	protected String createMethodDeclaration(final IDocument document, final MethodDeclaration declaration, final ASTRewrite rewrite) throws BadLocationException {
		Assert.isNotNull(document);
		Assert.isNotNull(declaration);
		Assert.isNotNull(rewrite);
		final IRegion range= new Region(declaration.getStartPosition(), declaration.getLength());
		final RangeMarker marker= new RangeMarker(range.getOffset(), range.getLength());
		final TextEdit[] edits= rewrite.rewriteAST(document, fMethod.getJavaProject().getOptions(true)).removeChildren();
		for (int index= 0; index < edits.length; index++)
			marker.addChild(edits[index]);
		final MultiTextEdit result= new MultiTextEdit();
		result.addChild(marker);
		final TextEditProcessor processor= new TextEditProcessor(document, new MultiTextEdit(0, document.getLength()), TextEdit.UPDATE_REGIONS);
		processor.getRoot().addChild(result);
		processor.performEdits();
		final int width= CodeFormatterUtil.getTabWidth(fMethod.getJavaProject());
		final IRegion region= document.getLineInformation(document.getLineOfOffset(marker.getOffset()));
		return Strings.changeIndent(document.get(marker.getOffset(), marker.getLength()), Strings.computeIndent(document.get(region.getOffset(), region.getLength()), width), width, "", StubUtility.getLineDelimiterFor(document)); //$NON-NLS-1$
	}

	/**
	 * Creates the necessary changes to replace the body of the method declaration with an expression to invoke the delegate.
	 * 
	 * @param declaration the method declaration to replace its body
	 * @param rewrites the compilation unit rewrites
	 * @param adjustments the map of elements to visibility adjustments
	 * @param status the refactoring status
	 * @param monitor the progress monitor to display progress
	 * @throws CoreException if the change could not be generated
	 * @return <code>true</code> if a target node had to be inserted as first argument, <code>false</code> otherwise
	 */
	protected boolean createMethodDelegation(final MethodDeclaration declaration, final Map rewrites, final Map adjustments, final RefactoringStatus status, final IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(declaration);
		Assert.isNotNull(monitor);
		final AST ast= fSourceRewrite.getRoot().getAST();
		final ASTRewrite rewrite= fSourceRewrite.getASTRewrite();
		final ImportRemover remover= fSourceRewrite.getImportRemover();
		final MethodInvocation invocation= ast.newMethodInvocation();
		invocation.setName(ast.newSimpleName(fMethodName));
		invocation.setExpression(createTargetAccessExpression(declaration));
		final boolean target= createArgumentList(declaration, invocation.arguments(), new VisibilityAdjustingArgumentFactory(ast, rewrites, adjustments));
		final Block block= ast.newBlock();
		block.statements().add(createMethodInvocation(declaration, invocation));
		if (!fSourceRewrite.getCu().equals(fTargetType.getCompilationUnit()))
			remover.registerRemovedNode(declaration.getBody());
		rewrite.set(declaration, MethodDeclaration.BODY_PROPERTY, block, fSourceRewrite.createGroupDescription(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.replace_body_with_delegation"))); //$NON-NLS-1$
		if (fDeprecated)
			createMethodDeprecation(declaration);
		return target;
	}

	/**
	 * Creates the necessary changes to inline the method invocations to the original method.
	 * 
	 * @param rewrites the map of compilation units to compilation unit rewrites
	 * @param declaration the source method declaration
	 * @param groups the search result groups representing all references to the moved method, including references in comments
	 * @param adjustments the map of elements to visibility adjustments
	 * @param target <code>true</code> if a target node must be inserted as first argument, <code>false</code> otherwise
	 * @param status the refactoring status
	 * @param monitor the progress monitor to use
	 * @return <code>true</code> if all method invocations to the original method declaration could be inlined, <code>false</code> otherwise
	 */
	protected boolean createMethodDelegator(final Map rewrites, final MethodDeclaration declaration, final SearchResultGroup[] groups, final Map adjustments, final boolean target, final RefactoringStatus status, final IProgressMonitor monitor) {
		Assert.isNotNull(rewrites);
		Assert.isNotNull(declaration);
		Assert.isNotNull(groups);
		Assert.isNotNull(adjustments);
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask("", groups.length); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.creating")); //$NON-NLS-1$
			try {
				boolean result= true;
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
					status.merge(RefactoringStatus.createWarningStatus(RefactoringCoreMessages.getFormattedString("MoveInstanceMethodProcessor.inline.overridden", BindingLabels.getFullyQualified(declaration.resolveBinding())), JavaStatusContext.create(fMethod))); //$NON-NLS-1$
					result= false;
				} else {
					monitor.worked(1);
					SearchMatch[] matches= null;
					IJavaElement element= null;
					ICompilationUnit unit= null;
					CompilationUnitRewrite rewrite= null;
					SearchResultGroup group= null;
					for (int index= 0; index < groups.length; index++) {
						group= groups[index];
						element= JavaCore.create(group.getResource());
						if (element instanceof ICompilationUnit) {
							matches= group.getSearchResults();
							unit= (ICompilationUnit) element;
							rewrite= getCompilationUnitRewrite(rewrites, unit);
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
									result= false;
								} else if (!createInlinedMethodInvocation(rewrite, declaration, match, adjustments, target, status))
									result= false;
							}
						} else if (element != null) {
							status.merge(RefactoringStatus.createWarningStatus(RefactoringCoreMessages.getFormattedString("MoveInstanceMethodProcessor.inline.binary.project", element.getJavaProject().getElementName()))); //$NON-NLS-1$
							result= false;
						} else {
							status.merge(RefactoringStatus.createWarningStatus(RefactoringCoreMessages.getFormattedString("MoveInstanceMethodProcessor.inline.binary.resource", group.getResource().getName()))); //$NON-NLS-1$
							result= false;
						}
					}
					monitor.worked(1);
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
	 * @throws JavaModelException if an error occurs
	 */
	protected void createMethodDeprecation(final MethodDeclaration declaration) throws JavaModelException {
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
	 * @param rewriter the target compilation unit rewrite
	 * @param monitor the progress monitor to use
	 * @throws JavaModelException if the types referenced in the method could not be determined
	 */
	protected void createMethodImports(final CompilationUnitRewrite rewriter, final IProgressMonitor monitor) throws JavaModelException {
		Assert.isNotNull(rewriter);
		Assert.isNotNull(monitor);
		monitor.beginTask("", 1); //$NON-NLS-1$
		monitor.setTaskName(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.creating")); //$NON-NLS-1$
		try {
			final IType[] references= ReferenceFinderUtil.getTypesReferencedIn(new IJavaElement[] { fMethod}, monitor);
			final ImportRewrite rewrite= rewriter.getImportRewrite();
			final ImportRemover remover= rewriter.getImportRemover();
			String name= null;
			for (int index= 0; index < references.length; index++) {
				name= JavaModelUtil.getFullyQualifiedName(references[index]);
				rewrite.addImport(name);
				remover.registerAddedImport(name);
			}
		} finally {
			monitor.done();
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
	 * @param rewrite the current compilation unit rewrite
	 * @param declaration the source method declaration
	 * @param match the search match representing the method reference
	 * @param targetNode <code>true</code> if a target node had to be inserted as first argument, <code>false</code> otherwise
	 * @param status the refactoring status
	 */
	protected void createMethodJavadocReference(final CompilationUnitRewrite rewrite, final MethodDeclaration declaration, final SearchMatch match, final boolean targetNode, final RefactoringStatus status) {
		Assert.isNotNull(rewrite);
		Assert.isNotNull(declaration);
		Assert.isNotNull(match);
		Assert.isNotNull(status);
		final ASTNode node= ASTNodeSearchUtil.findNode(match, rewrite.getRoot());
		if (node instanceof MethodRef) {
			final AST ast= node.getAST();
			final MethodRef successor= ast.newMethodRef();

			rewrite.getASTRewrite().replace(node, successor, null);
		}
	}

	/**
	 * Creates the necessary changes to update tag references to the original method.
	 * 
	 * @param rewrites the map of compilation units to compilation unit rewrites
	 * @param declaration the source method declaration
	 * @param groups the search result groups representing all references to the moved method, including references in comments
	 * @param target <code>true</code> if a target node must be inserted as first argument, <code>false</code> otherwise
	 * @param status the refactoring status
	 * @param monitor the progress monitor to use
	 */
	protected void createMethodJavadocReferences(final Map rewrites, final MethodDeclaration declaration, final SearchResultGroup[] groups, final boolean target, final RefactoringStatus status, final IProgressMonitor monitor) {
		Assert.isNotNull(rewrites);
		Assert.isNotNull(declaration);
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask("", groups.length); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("MoveInstanceMethodProcessor.creating")); //$NON-NLS-1$
			try {
				SearchMatch[] matches= null;
				IJavaElement element= null;
				ICompilationUnit unit= null;
				CompilationUnitRewrite rewrite= null;
				SearchResultGroup group= null;
				for (int index= 0; index < groups.length; index++) {
					group= groups[index];
					element= JavaCore.create(group.getResource());
					unit= group.getCompilationUnit();
					if (element instanceof ICompilationUnit) {
						matches= group.getSearchResults();
						unit= (ICompilationUnit) element;
						rewrite= getCompilationUnitRewrite(rewrites, unit);
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
								createMethodJavadocReference(rewrite, declaration, match, target, status);
						}
					} else if (element != null) {
						status.merge(RefactoringStatus.createWarningStatus(RefactoringCoreMessages.getFormattedString("MoveInstanceMethodProcessor.javadoc.binary.project", element.getJavaProject().getElementName()))); //$NON-NLS-1$
					} else {
						status.merge(RefactoringStatus.createWarningStatus(RefactoringCoreMessages.getFormattedString("MoveInstanceMethodProcessor.javadoc.binary.resource", group.getResource().getName()))); //$NON-NLS-1$
					}
					monitor.worked(1);
				}
			} catch (CoreException exception) {
				status.merge(RefactoringStatus.create(exception.getStatus()));
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Creates a comment method reference to the moved method
	 * 
	 * @param declaration the method declaration of the original method
	 * @param ast the ast to create the method reference for
	 * @return the created link tag to reference the method
	 * @throws JavaModelException if an error occurs
	 */
	protected TagElement createMethodReference(final MethodDeclaration declaration, final AST ast) throws JavaModelException {
		Assert.isNotNull(ast);
		Assert.isNotNull(declaration);
		final MethodRef reference= ast.newMethodRef();
		reference.setName(ast.newSimpleName(fMethodName));
		reference.setQualifier(ASTNodeFactory.newName(ast, JavaModelUtil.getFullyQualifiedName(fTargetType)));
		createArgumentList(declaration, reference.parameters(), new IArgumentFactory() {

			public final ASTNode getArgumentNode(final IVariableBinding binding, final boolean last) {
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
			final IVariableBinding[] bindings= getArgumentBindings(declaration);
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
	 * Returns a compilation unit rewrite for the specified compilation unit.
	 * 
	 * @param rewrites the compilation unit rewrite map
	 * @param unit the compilation unit
	 * @return the corresponding compilation unit rewrite
	 */
	protected CompilationUnitRewrite getCompilationUnitRewrite(final Map rewrites, final ICompilationUnit unit) {
		Assert.isNotNull(rewrites);
		Assert.isNotNull(unit);
		CompilationUnitRewrite rewrite= (CompilationUnitRewrite) rewrites.get(unit);
		if (rewrite == null) {
			rewrite= new CompilationUnitRewrite(unit);
			rewrites.put(unit, rewrite);
		}
		return rewrite;
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
		fTargetType= null;
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
	 * Determines whether setter methods should be used to resolve visibility issues.
	 * 
	 * @param use <code>true</code> if setter methods should be used, <code>false</code> otherwise
	 */
	public final void setUseSetters(final boolean use) {
		fUseSetters= use;
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
	 * Should setter methods be used to resolve visibility issues?
	 * 
	 * @return <code>true</code> if setter methods should be used, <code>false</code> otherwise
	 */
	public final boolean shouldUseSetters() {
		return fUseSetters;
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