/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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
 *     Chris West (Faux) <eclipse@goeswhere.com> - [clean up] "Use modifier 'final' where possible" can introduce compile errors - https://bugs.eclipse.org/bugs/show_bug.cgi?id=272532
 *     Red Hat Inc. - created VariableDeclarationFixCore from VariableDeclarationFix
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.dom.VariableDeclarationRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

public class VariableDeclarationFixCore extends CompilationUnitRewriteOperationsFixCore {

	public static class WrittenNamesFinder extends GenericVisitor {

		private final HashMap<IBinding, List<SimpleName>> fResult;

		public WrittenNamesFinder(HashMap<IBinding, List<SimpleName>> result) {
			fResult= result;
		}

		@Override
		public boolean visit(SimpleName node) {
			if (node.getParent() instanceof VariableDeclarationFragment)
				return super.visit(node);
			if (node.getParent() instanceof SingleVariableDeclaration)
				return super.visit(node);

			IBinding binding= node.resolveBinding();
			if (!(binding instanceof IVariableBinding))
				return super.visit(node);

			binding= ((IVariableBinding)binding).getVariableDeclaration();
			if (ASTResolving.isWriteAccess(node)) {
				List<SimpleName> list;
				if (fResult.containsKey(binding)) {
					list= fResult.get(binding);
				} else {
					list= new ArrayList<>();
				}
				list.add(node);
				fResult.put(binding, list);
			}

			return super.visit(node);
		}
	}

	public static class ReturnFinder extends ASTVisitor {
		boolean foundOne;
		@Override
		public boolean visit(ReturnStatement node) {
			foundOne= true;
			return super.visit(node);
		}
	}

	public static class VariableDeclarationFinder extends GenericVisitor {

		private final List<ModifierChangeOperation> fResult;
		private final HashMap<IBinding, List<SimpleName>> fWrittenVariables;
		private final boolean fAddFinalFields;
		private final boolean fAddFinalParameters;
		private final boolean fAddFinalLocals;

		public VariableDeclarationFinder(boolean addFinalFields,
				boolean addFinalParameters,
				boolean addFinalLocals,
				final List<ModifierChangeOperation> result, final HashMap<IBinding, List<SimpleName>> writtenNames) {

			super();
			fAddFinalFields= addFinalFields;
			fAddFinalParameters= addFinalParameters;
			fAddFinalLocals= addFinalLocals;
			fResult= result;
			fWrittenVariables= writtenNames;
		}

		@Override
		public boolean visit(FieldDeclaration node) {
			if (fAddFinalFields)
				handleFragments(node.fragments(), node);

			List<VariableDeclarationFragment> fragments= node.fragments();
			for (VariableDeclarationFragment fragment : fragments) {
				Expression initializer= fragment.getInitializer();
				if (initializer != null) {
					initializer.accept(this);
				}
			}

			return false;
		}

		@Override
		public boolean visit(VariableDeclarationStatement node) {
			if (fAddFinalLocals)
				handleFragments(node.fragments(), node);

			List<VariableDeclarationFragment> fragments= node.fragments();
			for (VariableDeclarationFragment fragment : fragments) {
				Expression initializer= fragment.getInitializer();
				if (initializer != null) {
					initializer.accept(this);
				}
			}

			return false;
		}

		@Override
		public boolean visit(VariableDeclarationExpression node) {
			if (fAddFinalLocals && node.fragments().size() == 1) {
				SimpleName name= ((VariableDeclarationFragment)node.fragments().get(0)).getName();

				IBinding binding= name.resolveBinding();
				if (binding == null)
					return false;

				if (fWrittenVariables.containsKey(binding))
					return false;

				ModifierChangeOperation op= createAddFinalOperation(name, node);
				if (op == null)
					return false;

				fResult.add(op);
				return false;
			}
			return false;
		}

		private boolean handleFragments(List<VariableDeclarationFragment> list, ASTNode declaration) {
			List<VariableDeclarationFragment> toChange= new ArrayList<>();

			for (VariableDeclarationFragment fragment : list) {
				SimpleName name= fragment.getName();
				IBinding resolveBinding= name.resolveBinding();
				if (canAddFinal(resolveBinding, declaration)) {
					IVariableBinding varbinding= (IVariableBinding)resolveBinding;
					if (varbinding.isField()) {
						if (fieldCanBeFinal(fragment, varbinding))
							toChange.add(fragment);
					} else {
						if (!fWrittenVariables.containsKey(resolveBinding))
							toChange.add(fragment);
					}
				}
			}

			if (toChange.size() == 0)
				return false;

			ModifierChangeOperation op= new ModifierChangeOperation(declaration, toChange, Modifier.FINAL, Modifier.NONE);
			fResult.add(op);
			return false;
		}

		private boolean fieldCanBeFinal(VariableDeclarationFragment fragment, IVariableBinding binding) {
			if (Modifier.isStatic(((FieldDeclaration)fragment.getParent()).getModifiers()))
				return false;

			if (!fWrittenVariables.containsKey(binding)) {
				//variable is not written
				if (fragment.getInitializer() == null) {//variable is not initialized
					return false;
				} else {
					return true;
				}
			}

			if (fragment.getInitializer() != null)//variable is initialized and written
				return false;

			ITypeBinding declaringClass= binding.getDeclaringClass();
			if (declaringClass == null)
				return false;

			List<SimpleName> writes= fWrittenVariables.get(binding);
			if (!isWrittenInTypeConstructors(writes, declaringClass))
				return false;

			HashSet<IMethodBinding> writingConstructorBindings= new HashSet<>();
			ArrayList<MethodDeclaration> writingConstructors= new ArrayList<>();
			for (SimpleName name : writes) {
	            MethodDeclaration constructor= getWritingConstructor(name);
	            if (writingConstructors.contains(constructor))//variable is written twice or more in constructor
	            	return false;

	            if (canReturn(constructor))
	            	return false;

	            writingConstructors.add(constructor);
	            IMethodBinding constructorBinding= constructor.resolveBinding();
	            if (constructorBinding == null)
	            	return false;

				writingConstructorBindings.add(constructorBinding);
            }

			for (MethodDeclaration constructor : writingConstructors) {
	            if (callsWritingConstructor(constructor, writingConstructorBindings))//writing constructor calls other writing constructor
	            	return false;
            }

			MethodDeclaration constructor= writingConstructors.get(0);
			TypeDeclaration typeDecl= ASTNodes.getParent(constructor, TypeDeclaration.class);
			if (typeDecl == null)
				return false;

			for (MethodDeclaration method : typeDecl.getMethods()) {
	            if (method.isConstructor()) {
	            	IMethodBinding methodBinding= method.resolveBinding();
	            	if (methodBinding == null)
	            		return false;

	            	if (!writingConstructorBindings.contains(methodBinding)) {
	            		if (!callsWritingConstructor(method, writingConstructorBindings))//non writing constructor does not call a writing constructor
	            			return false;
	            	}
	            }
            }

	        return true;
        }

		private boolean canReturn(MethodDeclaration constructor) {
			final ReturnFinder retFinder= new ReturnFinder();
			constructor.accept(retFinder);
			return retFinder.foundOne;
		}

		private boolean callsWritingConstructor(MethodDeclaration methodDeclaration, HashSet<IMethodBinding> writingConstructorBindings) {
			HashSet<MethodDeclaration> visitedMethodDeclarations= new HashSet<>();
			visitedMethodDeclarations.add(methodDeclaration);
			return callsWritingConstructor(methodDeclaration, writingConstructorBindings, visitedMethodDeclarations);
		}

		private boolean callsWritingConstructor(MethodDeclaration methodDeclaration, HashSet<IMethodBinding> writingConstructorBindings, Set<MethodDeclaration> visitedMethodDeclarations) {
			Block body= methodDeclaration.getBody();
			if (body == null)
				return false;

			List<Statement> statements= body.statements();
			if (statements.size() == 0)
				return false;

			Statement statement= statements.get(0);
			if (!(statement instanceof ConstructorInvocation))
				return false;

			ConstructorInvocation invocation= (ConstructorInvocation)statement;
			IMethodBinding constructorBinding= invocation.resolveConstructorBinding();
			if (constructorBinding == null)
				return false;

			if (writingConstructorBindings.contains(constructorBinding)) {
				return true;
			} else {
				ASTNode declaration= ASTNodes.findDeclaration(constructorBinding, methodDeclaration.getParent());
				if (!(declaration instanceof MethodDeclaration))
					return false;

				if (visitedMethodDeclarations.contains(declaration)) {
					return false;
				}
				visitedMethodDeclarations.add(methodDeclaration);
				return callsWritingConstructor((MethodDeclaration)declaration, writingConstructorBindings, visitedMethodDeclarations);
			}
		}

		private boolean isWrittenInTypeConstructors(List<SimpleName> writes, ITypeBinding declaringClass) {

			for (SimpleName name : writes) {
	            MethodDeclaration methodDeclaration= getWritingConstructor(name);
	            if (methodDeclaration == null)
	            	return false;

	            if (!methodDeclaration.isConstructor())
	            	return false;

	            IMethodBinding constructor= methodDeclaration.resolveBinding();
	            if (constructor == null)
	            	return false;

	            ITypeBinding declaringClass2= constructor.getDeclaringClass();
	            if (!declaringClass.equals(declaringClass2))
	            	return false;
            }

	        return true;
        }

		private MethodDeclaration getWritingConstructor(SimpleName name) {
			Assignment assignement= ASTNodes.getParent(name, Assignment.class);
			if (assignement == null)
				return null;

			ASTNode expression= assignement.getParent();
			if (!(expression instanceof ExpressionStatement))
				return null;

			ASTNode block= expression.getParent();
			if (!(block instanceof Block))
				return null;

			ASTNode methodDeclaration= block.getParent();
			if (!(methodDeclaration instanceof MethodDeclaration))
				return null;

	        return (MethodDeclaration)methodDeclaration;
        }

		@Override
		public boolean visit(VariableDeclarationFragment node) {
			SimpleName name= node.getName();

			IBinding binding= name.resolveBinding();
			if (binding == null)
				return true;

			if (fWrittenVariables.containsKey(binding))
				return true;

			ModifierChangeOperation op= createAddFinalOperation(name, node);
			if (op == null)
				return true;

			fResult.add(op);
			return true;
		}

		@Override
		public boolean visit(SingleVariableDeclaration node) {
			SimpleName name= node.getName();

			IBinding binding= name.resolveBinding();
			if (!(binding instanceof IVariableBinding))
				return false;

			IVariableBinding varBinding= (IVariableBinding)binding;
			if (fWrittenVariables.containsKey(varBinding))
				return false;

			if (fAddFinalParameters && fAddFinalLocals) {

				ModifierChangeOperation op= createAddFinalOperation(name, node);
				if (op == null)
					return false;

				fResult.add(op);
				return false;
			} else if (fAddFinalParameters) {
				if (!varBinding.isParameter())
					return false;

				ModifierChangeOperation op= createAddFinalOperation(name, node);
				if (op == null)
					return false;

				fResult.add(op);
				return false;
			} else if (fAddFinalLocals) {
				if (varBinding.isParameter())
					return false;

				ModifierChangeOperation op= createAddFinalOperation(name, node);
				if (op == null)
					return false;

				fResult.add(op);
				return false;
			}
			return false;
		}
	}

	public static class ModifierChangeOperation extends CompilationUnitRewriteOperation {

		private final ASTNode fDeclaration;
		private final List<VariableDeclarationFragment> fToChange;
		private final int fIncludedModifiers;
		private final int fExcludedModifiers;

		public ModifierChangeOperation(ASTNode declaration, List<VariableDeclarationFragment> toChange, int includedModifiers, int excludedModifiers) {
			fDeclaration= declaration;
			fToChange= toChange;
			fIncludedModifiers= includedModifiers;
			fExcludedModifiers= excludedModifiers;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore model) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();

			TextEditGroup group= createTextEditGroup(FixMessages.VariableDeclarationFix_changeModifierOfUnknownToFinal_description, cuRewrite);

			if (fDeclaration instanceof VariableDeclarationStatement) {
				VariableDeclarationFragment[] toChange= fToChange.toArray(new VariableDeclarationFragment[fToChange.size()]);
				VariableDeclarationRewrite.rewriteModifiers((VariableDeclarationStatement)fDeclaration, toChange, fIncludedModifiers, fExcludedModifiers, rewrite, group);
			} else if (fDeclaration instanceof FieldDeclaration) {
				VariableDeclarationFragment[] toChange= fToChange.toArray(new VariableDeclarationFragment[fToChange.size()]);
				VariableDeclarationRewrite.rewriteModifiers((FieldDeclaration)fDeclaration, toChange, fIncludedModifiers, fExcludedModifiers, rewrite, group);
			} else if (fDeclaration instanceof SingleVariableDeclaration) {
				VariableDeclarationRewrite.rewriteModifiers((SingleVariableDeclaration)fDeclaration, fIncludedModifiers, fExcludedModifiers, rewrite, group);
			} else if (fDeclaration instanceof VariableDeclarationExpression) {
				VariableDeclarationRewrite.rewriteModifiers((VariableDeclarationExpression)fDeclaration, fIncludedModifiers, fExcludedModifiers, rewrite, group);
			}
		}
	}

	public static VariableDeclarationFixCore createChangeModifierToFinalFix(final CompilationUnit compilationUnit, ASTNode[] selectedNodes) {
		HashMap<IBinding, List<SimpleName>> writtenNames= new HashMap<>();
		WrittenNamesFinder finder= new WrittenNamesFinder(writtenNames);
		compilationUnit.accept(finder);
		List<ModifierChangeOperation> ops= new ArrayList<>();
		VariableDeclarationFinder visitor= new VariableDeclarationFinder(true, true, true, ops, writtenNames);
		if (selectedNodes.length == 1) {
			selectedNodes[0].accept(visitor);
		} else {
			for (ASTNode selectedNode : selectedNodes) {
				selectedNode.accept(visitor);
			}
		}
		if (ops.size() == 0)
			return null;

		CompilationUnitRewriteOperation[] result= ops.toArray(new CompilationUnitRewriteOperation[ops.size()]);
		String label;
		if (result.length == 1) {
			label= FixMessages.VariableDeclarationFix_changeModifierOfUnknownToFinal_description;
		} else {
			label= FixMessages.VariableDeclarationFix_ChangeMidifiersToFinalWherPossible_description;
		}
		return new VariableDeclarationFixCore(label, compilationUnit, result);
	}

	public static ICleanUpFixCore createCleanUp(CompilationUnit compilationUnit,
			boolean addFinalFields, boolean addFinalParameters, boolean addFinalLocals) {

		if (!addFinalFields && !addFinalParameters && !addFinalLocals)
			return null;

		HashMap<IBinding, List<SimpleName>> writtenNames= new HashMap<>();
		WrittenNamesFinder finder= new WrittenNamesFinder(writtenNames);
		compilationUnit.accept(finder);

		List<ModifierChangeOperation> operations= new ArrayList<>();
		VariableDeclarationFinder visitor= new VariableDeclarationFinder(addFinalFields, addFinalParameters, addFinalLocals, operations, writtenNames);
		compilationUnit.accept(visitor);

		if (operations.isEmpty())
			return null;

		return new VariableDeclarationFixCore(FixMessages.VariableDeclarationFix_add_final_change_name, compilationUnit, operations.toArray(new CompilationUnitRewriteOperation[operations.size()]));
	}

	private static ModifierChangeOperation createAddFinalOperation(SimpleName name, ASTNode decl) {
		if (decl == null)
			return null;

		IBinding binding= name.resolveBinding();
		if (!canAddFinal(binding, decl))
			return null;

		if (decl instanceof SingleVariableDeclaration
				|| decl instanceof VariableDeclarationExpression) {
			return new ModifierChangeOperation(decl, new ArrayList<VariableDeclarationFragment>(), Modifier.FINAL, Modifier.NONE);
		} else if (decl instanceof VariableDeclarationFragment){
			VariableDeclarationFragment frag= (VariableDeclarationFragment)decl;
			decl= decl.getParent();
			if (decl instanceof FieldDeclaration || decl instanceof VariableDeclarationStatement) {
				List<VariableDeclarationFragment> list= new ArrayList<>();
				list.add(frag);
				return new ModifierChangeOperation(decl, list, Modifier.FINAL, Modifier.NONE);
			} else if (decl instanceof VariableDeclarationExpression) {
				return new ModifierChangeOperation(decl, new ArrayList<VariableDeclarationFragment>(), Modifier.FINAL, Modifier.NONE);
			}
		}

		return null;
	}

	public static boolean canAddFinal(IBinding binding, ASTNode declNode) {
		if (!(binding instanceof IVariableBinding))
			return false;

		IVariableBinding varbinding= (IVariableBinding)binding;
		int modifiers= varbinding.getModifiers();
		if (Modifier.isFinal(modifiers) || Modifier.isVolatile(modifiers) || Modifier.isTransient(modifiers))
			return false;

		VariableDeclarationExpression parent= ASTNodes.getParent(declNode, VariableDeclarationExpression.class);
		if (parent != null && parent.fragments().size() > 1)
			return false;

		if (varbinding.isField() && !Modifier.isPrivate(modifiers))
			return false;

		if (varbinding.isRecordComponent())
			return false;

		if (varbinding.isParameter()) {
			ASTNode varDecl= declNode.getParent();
			if (varDecl instanceof MethodDeclaration) {
				MethodDeclaration declaration= (MethodDeclaration)varDecl;
				if (declaration.getBody() == null)
					return false;
			}
		}

		return true;
	}

	protected VariableDeclarationFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}

}
