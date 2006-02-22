/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

/**
 * A fix which fixes code style issues.
 */
public class CodeStyleFix extends AbstractFix {
	
	private final static class CodeStyleVisitor extends GenericVisitor {
		
		private final List/*<IFixRewriteOperation>*/ fResult;
		private final ImportRewrite fImportRewrite;
		private final boolean fFindUnqualifiedAccesses;
		private final boolean fFindUnqualifiedStaticAccesses;
		private final boolean fFindUnqualifiedMethodAccesses;
		private final boolean fFindUnqualifiedStaticMethodAccesses;
		
		public CodeStyleVisitor(CompilationUnit compilationUnit, 
				boolean findUnqualifiedAccesses, 
				boolean findUnqualifiedStaticAccesses,
				boolean findUnqualifiedMethodAccesses,
				boolean findUnqualifiedStaticMethodAccesses,
				List resultingCollection) throws CoreException {
			
			fFindUnqualifiedAccesses= findUnqualifiedAccesses;
			fFindUnqualifiedStaticAccesses= findUnqualifiedStaticAccesses;
			fFindUnqualifiedMethodAccesses= findUnqualifiedMethodAccesses;
			fFindUnqualifiedStaticMethodAccesses= findUnqualifiedStaticMethodAccesses;
			fImportRewrite= StubUtility.createImportRewrite(compilationUnit, true);
			fResult= resultingCollection;
		}
		
		/**
		 * {@inheritDoc}
		 */
		public boolean visit(TypeDeclaration node) {
			if (!fFindUnqualifiedStaticAccesses && !fFindUnqualifiedStaticMethodAccesses && node.isInterface())
				return false;
			
			return super.visit(node);
		}

		public boolean visit(QualifiedName node) {
			if (fFindUnqualifiedAccesses || fFindUnqualifiedStaticAccesses) {
				ASTNode simpleName= node;
				while (simpleName instanceof QualifiedName) {
					simpleName= ((QualifiedName) simpleName).getQualifier();
				}
				if (simpleName instanceof SimpleName) {
					handleSimpleName((SimpleName)simpleName);
				}
			}
			return false;
		}

		public boolean visit(SimpleName node) {
			if (fFindUnqualifiedAccesses || fFindUnqualifiedStaticAccesses) {
				handleSimpleName(node);
			}
			return false;
		}
		
		/**
		 * {@inheritDoc}
		 */
		public boolean visit(MethodInvocation node) {
			if (!fFindUnqualifiedMethodAccesses && !fFindUnqualifiedStaticMethodAccesses)
				return true;
			
			if (node.getExpression() != null)
				return false;
			
			IBinding binding= node.getName().resolveBinding();
			if (!(binding instanceof IMethodBinding))
				return false;
			
			handleMethod(node.getName(), (IMethodBinding)binding);
			return false;
		}

		private void handleSimpleName(SimpleName node) {
			ASTNode firstExpression= node.getParent();
			if (firstExpression instanceof FieldAccess) {
				while (firstExpression instanceof FieldAccess) {
					firstExpression= ((FieldAccess)firstExpression).getExpression();
				}
				if (!(firstExpression instanceof SimpleName))
					return;
				
				node= (SimpleName)firstExpression;
			} else if (firstExpression instanceof SuperFieldAccess)
				return;
			
			StructuralPropertyDescriptor parentDescription= node.getLocationInParent();
			if (parentDescription == VariableDeclarationFragment.NAME_PROPERTY || parentDescription == SwitchCase.EXPRESSION_PROPERTY)
				return;
			
			IBinding binding= node.resolveBinding();
			if (!(binding instanceof IVariableBinding))
				return;
			
			handleVariable(node, (IVariableBinding) binding);
		}

		private void handleVariable(SimpleName node, IVariableBinding varbinding) {
			if (!varbinding.isField())
				return;

			ITypeBinding declaringClass= varbinding.getDeclaringClass();
			if (Modifier.isStatic(varbinding.getModifiers())) {
				if (fFindUnqualifiedStaticAccesses) {
					Initializer initializer= (Initializer) ASTNodes.getParent(node, Initializer.class);
					//Do not qualify assignments to static final fields in static initializers (would result in compile error)
					StructuralPropertyDescriptor parentDescription= node.getLocationInParent();
					if (initializer != null && Modifier.isStatic(initializer.getModifiers())
							&& Modifier.isFinal(varbinding.getModifiers()) && parentDescription == Assignment.LEFT_HAND_SIDE_PROPERTY)
						return;
						
					//Do not qualify static fields if defined inside an anonymous class
					if (declaringClass.isAnonymous())
						return;

					fResult.add(new AddStaticQualifierOperation(declaringClass, node));
				}
			} else if (fFindUnqualifiedAccesses){
				String qualifier= getNonStaticQualifier(declaringClass, fImportRewrite, node);
				if (qualifier == null)
					return;

				fResult.add(new AddThisQualifierOperation(qualifier, node));
			}
		}		

		private void handleMethod(SimpleName node, IMethodBinding binding) {
			ITypeBinding declaringClass= binding.getDeclaringClass();
			if (Modifier.isStatic(binding.getModifiers())) {
				if (fFindUnqualifiedStaticMethodAccesses) {
					//Do not qualify static fields if defined inside an anonymous class
					if (declaringClass.isAnonymous())
						return;

					fResult.add(new AddStaticQualifierOperation(declaringClass, node));
				}
			} else {
				if (fFindUnqualifiedMethodAccesses) {
					String qualifier= getNonStaticQualifier(declaringClass, fImportRewrite, node);
					if (qualifier == null)
						return;

					fResult.add(new AddThisQualifierOperation(qualifier, node));
				}
			}
		}
	}

	private final static class AddThisQualifierOperation implements IFixRewriteOperation {

		private final String fQualifier;
		private final SimpleName fName;

		public AddThisQualifierOperation(String qualifier, SimpleName name) {
			fQualifier= qualifier;
			fName= name;
		}
		
		public String getDescription() {
			return Messages.format(FixMessages.CodeStyleFix_QualifyWithThis_description, new Object[] {fName.getIdentifier(), fQualifier});
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.fix.AbstractFix.IFixRewriteOperation#rewriteAST(org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite, java.util.List)
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			String groupName= Messages.format(FixMessages.CodeStyleFix_QualifyWithThis_description, new Object[] {fName.getIdentifier(), fQualifier});
			TextEditGroup group= new TextEditGroup(groupName);
			textEditGroups.add(group);
			rewrite.replace(fName, rewrite.createStringPlaceholder(fQualifier  + '.' + fName.getIdentifier(), ASTNode.SIMPLE_NAME), group);
		}		
	}
	
	private final static class AddStaticQualifierOperation extends AbstractFixRewriteOperation {

		private final SimpleName fName;
		private final ITypeBinding fDeclaringClass;
		
		public AddStaticQualifierOperation(ITypeBinding declaringClass, SimpleName name) {
			super();
			fDeclaringClass= declaringClass;
			fName= name;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.fix.AbstractFix.IFixRewriteOperation#rewriteAST(org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite, java.util.List)
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			CompilationUnit compilationUnit= cuRewrite.getRoot();
			Type qualifier= importType(fDeclaringClass, fName, cuRewrite.getImportRewrite(), compilationUnit);
			TextEditGroup group= new TextEditGroup(Messages.format(FixMessages.CodeStyleFix_QualifyWithThis_description, new Object[] {fName.getIdentifier(), ASTNodes.asString(qualifier)}));
			textEditGroups.add(group);
			rewrite.replace(fName, compilationUnit.getAST().newQualifiedType(qualifier, (SimpleName)rewrite.createMoveTarget(fName)), group);
		}
	}
	
	private final static class ToStaticAccessOperation extends AbstractFixRewriteOperation {

		private final ITypeBinding fDeclaringTypeBinding;
		private final Expression fQualifier;

		public ToStaticAccessOperation(ITypeBinding declaringTypeBinding, Expression qualifier) {
			super();
			fDeclaringTypeBinding= declaringTypeBinding;
			fQualifier= qualifier;
		}
		
		public String getAccessorName() {
			return fDeclaringTypeBinding.getName();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.fix.AbstractFix.IFixRewriteOperation#rewriteAST(org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite, java.util.List)
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
			Type type= importType(fDeclaringTypeBinding, fQualifier, cuRewrite.getImportRewrite(), cuRewrite.getRoot());
			TextEditGroup group= new TextEditGroup(Messages.format(FixMessages.CodeStyleFix_ChangeAccessToStatic_description, fDeclaringTypeBinding.getName()));
			textEditGroups.add(group);
			cuRewrite.getASTRewrite().replace(fQualifier, type, group);
		}
	}
	
	public static CodeStyleFix[] createNonStaticAccessFixes(CompilationUnit compilationUnit, IProblemLocation problem) throws CoreException {
		if (!isNonStaticAccess(problem))
			return null;
		
		ToStaticAccessOperation operations[]= createNonStaticAccessResolveOperations(compilationUnit, problem);
		if (operations == null)
			return null;

		String label1= Messages.format(FixMessages.CodeStyleFix_ChangeAccessToStatic_description, operations[0].getAccessorName());
		CodeStyleFix fix1= new CodeStyleFix(label1, compilationUnit, new IFixRewriteOperation[] {operations[0]});

		if (operations.length > 1) {
			String label2= Messages.format(FixMessages.CodeStyleFix_ChangeAccessToStaticUsingInstanceType_description, operations[1].getAccessorName());
			CodeStyleFix fix2= new CodeStyleFix(label2, compilationUnit, new IFixRewriteOperation[] {operations[1]});
			return new CodeStyleFix[] {fix1, fix2};
		}
		return new CodeStyleFix[] {fix1};
	}
	
	public static CodeStyleFix createAddFieldQualifierFix(CompilationUnit compilationUnit, IProblemLocation problem) throws CoreException {
		if (IProblem.UnqualifiedFieldAccess != problem.getProblemId())
			return null;
		
		AddThisQualifierOperation operation= getUnqualifiedFieldAccessResolveOperation(compilationUnit, problem);
		if (operation == null)
			return null;

		String groupName= operation.getDescription();
		return new CodeStyleFix(groupName, compilationUnit, new IFixRewriteOperation[] {operation});
	}
	
	public static CodeStyleFix createIndirectAccessToStaticFix(CompilationUnit compilationUnit, IProblemLocation problem) throws CoreException {
		if (!isIndirectStaticAccess(problem))
			return null;
		
		ToStaticAccessOperation operations[]= createNonStaticAccessResolveOperations(compilationUnit, problem);
		if (operations == null)
			return null;

		String label= Messages.format(FixMessages.CodeStyleFix_ChangeStaticAccess_description, operations[0].getAccessorName());
		return new CodeStyleFix(label, compilationUnit, new IFixRewriteOperation[] {operations[0]});
	}
	
	public static CodeStyleFix createCleanUp(CompilationUnit compilationUnit, 
			boolean addThisQualifier,
			boolean changeNonStaticAccessToStatic, 
			boolean qualifyStaticFieldAccess,
			boolean changeIndirectStaticAccessToDirect,
			boolean qualifyMethodAccess,
			boolean qualifyStaticMethodAccess) throws CoreException {
		
		if (!addThisQualifier && !changeNonStaticAccessToStatic && !qualifyStaticFieldAccess && !changeIndirectStaticAccessToDirect && !qualifyMethodAccess && !qualifyStaticMethodAccess)
			return null;

		List/*<IFixRewriteOperation>*/ operations= new ArrayList(); 
		if (addThisQualifier || qualifyStaticFieldAccess || qualifyMethodAccess || qualifyStaticMethodAccess) {
			CodeStyleVisitor codeStyleVisitor= new CodeStyleVisitor(compilationUnit, addThisQualifier, qualifyStaticFieldAccess, qualifyMethodAccess, qualifyStaticMethodAccess, operations);
			compilationUnit.accept(codeStyleVisitor);
		}
		
		if (changeNonStaticAccessToStatic || changeIndirectStaticAccessToDirect) {
			IProblem[] problems= compilationUnit.getProblems();
			for (int i= 0; i < problems.length; i++) {
				IProblemLocation problem= new ProblemLocation(problems[i]);
				boolean isNonStaticAccess= changeNonStaticAccessToStatic && isNonStaticAccess(problem);
				boolean isIndirectStaticAccess= changeIndirectStaticAccessToDirect && isIndirectStaticAccess(problem);
				if (isNonStaticAccess || isIndirectStaticAccess) {
					ToStaticAccessOperation[] nonStaticAccessInformation= createNonStaticAccessResolveOperations(compilationUnit, problem);
					if (nonStaticAccessInformation != null) {
						operations.add(nonStaticAccessInformation[0]);
					}
				}
			}
		}

		if (operations.isEmpty())
			return null;
		
		IFixRewriteOperation[] operationsArray= (IFixRewriteOperation[])operations.toArray(new IFixRewriteOperation[operations.size()]);
		return new CodeStyleFix("", compilationUnit, operationsArray); //$NON-NLS-1$
	}
	
	public static CodeStyleFix createCleanUp(CompilationUnit compilationUnit, IProblemLocation[] problems, 
			boolean addThisQualifier, 
			boolean changeNonStaticAccessToStatic,
			boolean changeIndirectStaticAccessToDirect) throws CoreException {
		
		if (!addThisQualifier && !changeNonStaticAccessToStatic && !changeIndirectStaticAccessToDirect)
			return null;
				
		List/*<IFixRewriteOperation>*/ operations= new ArrayList(); 
		if (addThisQualifier) {
			for (int i= 0; i < problems.length; i++) {
				IProblemLocation problem= problems[i];
				if (problem.getProblemId() == IProblem.UnqualifiedFieldAccess) {
					AddThisQualifierOperation operation= getUnqualifiedFieldAccessResolveOperation(compilationUnit, problem);
					if (operation != null)
						operations.add(operation);
				}
			}
		}

		if (changeNonStaticAccessToStatic || changeIndirectStaticAccessToDirect) {
			for (int i= 0; i < problems.length; i++) {
				IProblemLocation problem= problems[i];
				boolean isNonStaticAccess= changeNonStaticAccessToStatic && isNonStaticAccess(problem);
				boolean isIndirectStaticAccess= changeIndirectStaticAccessToDirect && isIndirectStaticAccess(problem);
				if (isNonStaticAccess || isIndirectStaticAccess) {
					ToStaticAccessOperation[] nonStaticAccessInformation= createNonStaticAccessResolveOperations(compilationUnit, problem);
					if (nonStaticAccessInformation != null) {
						operations.add(nonStaticAccessInformation[0]);
					}
				}
			}
		}

		if (operations.isEmpty())
			return null;
		
		IFixRewriteOperation[] operationsArray= (IFixRewriteOperation[])operations.toArray(new IFixRewriteOperation[operations.size()]);
		return new CodeStyleFix("", compilationUnit, operationsArray); //$NON-NLS-1$
	}

	private static boolean isIndirectStaticAccess(IProblemLocation problem) {
		return (problem.getProblemId() == IProblem.IndirectAccessToStaticField
				|| problem.getProblemId() == IProblem.IndirectAccessToStaticMethod);
	}
	
	private static boolean isNonStaticAccess(IProblemLocation problem) {
		return (problem.getProblemId() == IProblem.NonStaticAccessToStaticField
				|| problem.getProblemId() == IProblem.NonStaticAccessToStaticMethod);
	}
	
	private static ToStaticAccessOperation[] createNonStaticAccessResolveOperations(CompilationUnit astRoot, IProblemLocation problem) {
		ASTNode selectedNode= problem.getCoveringNode(astRoot);
		if (selectedNode == null) {
			return null;
		}

		Expression qualifier= null;
		IBinding accessBinding= null;

        if (selectedNode instanceof QualifiedName) {
        	QualifiedName name= (QualifiedName) selectedNode;
        	qualifier= name.getQualifier();
        	accessBinding= name.resolveBinding();
        } else if (selectedNode instanceof SimpleName) {
        	ASTNode parent= selectedNode.getParent();
        	if (parent instanceof FieldAccess) {
        		FieldAccess fieldAccess= (FieldAccess) parent;
        		qualifier= fieldAccess.getExpression();
        		accessBinding= fieldAccess.getName().resolveBinding();
        	} else if (parent instanceof QualifiedName) {
        		QualifiedName qualifiedName= (QualifiedName) parent;
        		qualifier= qualifiedName.getQualifier();
        		accessBinding= qualifiedName.getName().resolveBinding();
        	}
        } else if (selectedNode instanceof MethodInvocation) {
        	MethodInvocation methodInvocation= (MethodInvocation) selectedNode;
        	qualifier= methodInvocation.getExpression();
        	accessBinding= methodInvocation.getName().resolveBinding();
        } else if (selectedNode instanceof FieldAccess) {
			FieldAccess fieldAccess= (FieldAccess) selectedNode;
			qualifier= fieldAccess.getExpression();
			accessBinding= fieldAccess.getName().resolveBinding();
		}
        
		if (accessBinding != null && qualifier != null) {
			ToStaticAccessOperation declaring= null;
			ITypeBinding declaringTypeBinding= getDeclaringTypeBinding(accessBinding);
			if (declaringTypeBinding != null) {
				declaringTypeBinding= declaringTypeBinding.getTypeDeclaration(); // use generic to avoid any type arguments
				
				declaring= new ToStaticAccessOperation(declaringTypeBinding, qualifier);
			}
			ToStaticAccessOperation instance= null;
			ITypeBinding instanceTypeBinding= Bindings.normalizeTypeBinding(qualifier.resolveTypeBinding());
			if (instanceTypeBinding != null) {
				instanceTypeBinding= instanceTypeBinding.getTypeDeclaration();  // use generic to avoid any type arguments
				if (instanceTypeBinding.getTypeDeclaration() != declaringTypeBinding) {
					instance= new ToStaticAccessOperation(instanceTypeBinding, qualifier);
				}
			}
			if (declaring != null && instance != null) {
				return new ToStaticAccessOperation[] {declaring, instance};
			} else {
				return new ToStaticAccessOperation[] {declaring};
			}
		}
		return null;
	}
	
	private static ITypeBinding getDeclaringTypeBinding(IBinding accessBinding) {
		if (accessBinding instanceof IMethodBinding) {
			return ((IMethodBinding) accessBinding).getDeclaringClass();
		} else if (accessBinding instanceof IVariableBinding) {
			return ((IVariableBinding) accessBinding).getDeclaringClass();
		}
		return null;
	}
		
	private static AddThisQualifierOperation getUnqualifiedFieldAccessResolveOperation(CompilationUnit compilationUnit, IProblemLocation problem) throws CoreException {
		SimpleName name= getName(compilationUnit, problem);
		if (name == null)
			return null;
		
		IBinding binding= name.resolveBinding();
		if (binding == null || binding.getKind() != IBinding.VARIABLE)
			return null;
		
		ImportRewrite imports= StubUtility.createImportRewrite(compilationUnit, true);
		
		String replacement= getQualifier((IVariableBinding)binding, imports, name);
		if (replacement == null)
			return null;
		
		return new AddThisQualifierOperation(replacement, name);
	}
	
	private static String getQualifier(IVariableBinding binding, ImportRewrite imports, SimpleName name) {
		ITypeBinding declaringClass= binding.getDeclaringClass();
		String qualifier;
		if (Modifier.isStatic(binding.getModifiers())) {
			qualifier= imports.addImport(declaringClass);
		} else {
			qualifier= getNonStaticQualifier(declaringClass, imports, name);
		}

		return qualifier;
	}
	
	private static String getNonStaticQualifier(ITypeBinding declaringClass, ImportRewrite imports, SimpleName name) {
		ITypeBinding parentType= Bindings.getBindingOfParentType(name);
		ITypeBinding currType= parentType;
		while (currType != null && !Bindings.isSuperType(declaringClass, currType)) {
			currType= currType.getDeclaringClass();
		}
		if (currType != parentType) {
			if (currType == null)
				return null;
			
			if (currType.isAnonymous())
				//If we access a field of a super class of an anonymous class
				//then we can only qualify with 'this' but not with outer.this
				//see bug 115277
				return null;
			
			String outer= imports.addImport(currType);
			return outer + ".this"; //$NON-NLS-1$
		} else {
			return "this"; //$NON-NLS-1$
		}
	}
	
	private static SimpleName getName(CompilationUnit compilationUnit, IProblemLocation problem) {
		ASTNode selectedNode= problem.getCoveringNode(compilationUnit);
		
		while (selectedNode instanceof QualifiedName) {
			selectedNode= ((QualifiedName) selectedNode).getQualifier();
		}
		if (!(selectedNode instanceof SimpleName)) {
			return null;
		}
		return (SimpleName) selectedNode;
	}

	private CodeStyleFix(String name, CompilationUnit compilationUnit, IFixRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
}
