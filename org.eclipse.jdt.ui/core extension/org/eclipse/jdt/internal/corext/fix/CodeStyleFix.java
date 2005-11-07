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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

/**
 * A fix which fixes code style issues.
 * Supports:
 * 		Qualify field with this: f -> this.f if f is a field.
 * 		Change non static access to static using declaring type.
 */
public class CodeStyleFix extends AbstractFix {
	
	private final UnqualifiedFieldAccessInformation[] fUnqualifiedFieldAccesses;
	private final NonStaticAccessInformation[] fNonStaticAccesses;
	private final AST fAst;
	
	private final static class FieldAccessFinder extends GenericVisitor {
		
		private List/*<UnqualifiedFieldAccessInformation>*/ fUnqualifiedAccesses;
		private ImportRewrite fImportRewrite;
		
		public FieldAccessFinder(ICompilationUnit compilationUnit) throws CoreException {
			fUnqualifiedAccesses= new ArrayList();
			fImportRewrite= new ImportRewrite(compilationUnit);
		}
		
		public boolean visit(QualifiedName node) {
			ASTNode simpleName= node;
			while (simpleName instanceof QualifiedName) {
				simpleName= ((QualifiedName) simpleName).getQualifier();
			}
			if (simpleName instanceof SimpleName) {
				handleSimpleName((SimpleName)simpleName);
			}
			return false;
		}

		public boolean visit(SimpleName node) {
			handleSimpleName(node);
			return false;
		}

		private void handleSimpleName(SimpleName node) {
			if (node.getParent() instanceof FieldAccess) {
				ASTNode firstExpression= node.getParent();
				while (firstExpression instanceof FieldAccess) {
					firstExpression= ((FieldAccess)firstExpression).getExpression();
				}
				if (firstExpression instanceof ThisExpression)
					return;
				
				if (firstExpression instanceof SimpleName) {
					node= (SimpleName)firstExpression;
				} else {
					return;
				}
			}			
			if (node.getLocationInParent() == VariableDeclarationFragment.NAME_PROPERTY)
				return;
			
			if (node.getLocationInParent() == SwitchCase.EXPRESSION_PROPERTY)
				return;
			
			IBinding binding= node.resolveBinding();
			if (binding == null || (binding.getKind() != IBinding.VARIABLE) || !((IVariableBinding) binding).isField()
				|| Modifier.isStatic(binding.getModifiers()))//Don't qualify accesses to static fields, maybe we want to add a separate clean up for this?
				return;
			
			String qualifier= getQualifier((IVariableBinding)binding, fImportRewrite, node);
			if (qualifier == null)
				return;
			
			fUnqualifiedAccesses.add(new UnqualifiedFieldAccessInformation(qualifier, node));
		}

		public UnqualifiedFieldAccessInformation[] getUnqualifiedAccesses() {
			return (UnqualifiedFieldAccessInformation[])fUnqualifiedAccesses.toArray(new UnqualifiedFieldAccessInformation[fUnqualifiedAccesses.size()]);
		}
	}

	private final static class UnqualifiedFieldAccessInformation {

		private final String fQualifier;
		private final SimpleName fName;

		public UnqualifiedFieldAccessInformation(String qualifier, SimpleName name) {
			fQualifier= qualifier;
			fName= name;
		}

		public SimpleName getName() {
			return fName;
		}

		public String getQualifier() {
			return fQualifier;
		}
		
	}
	
	private final static class NonStaticAccessInformation {

		private final ITypeBinding fDeclaringTypeBinding;
		private final IBinding fAccessBinding;
		private final Expression fQualifier;
		private final ASTNode fNode;

		public NonStaticAccessInformation(ITypeBinding declaringTypeBinding, IBinding accessBinding, Expression qualifier, ASTNode node) {
			fDeclaringTypeBinding= declaringTypeBinding;
			fAccessBinding= accessBinding;
			fQualifier= qualifier;
			fNode= node;
		}

		public IBinding getAccessBinding() {
			return fAccessBinding;
		}

		public ITypeBinding getDeclaringTypeBinding() {
			return fDeclaringTypeBinding;
		}

		public Expression getQualifier() {
			return fQualifier;
		}

		public ASTNode getNode() {
			return fNode;
		}
		
	}

	public static CodeStyleFix createFix(CompilationUnit compilationUnit, IProblemLocation problem, boolean addThisQualifier, boolean changeNonStaticAccessToStatic) throws CoreException {
		if (changeNonStaticAccessToStatic && isNonStaticAccess(problem)) {
			NonStaticAccessInformation nonStaticAccessInformation= getNonStaticAccessInformation(compilationUnit, problem);
			if (nonStaticAccessInformation != null) {

				ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
				String label= Messages.format(FixMessages.CodeStyleFix_ChangeAccessToStatic_description, nonStaticAccessInformation.getDeclaringTypeBinding().getName());
				return new CodeStyleFix(label, cu, null, new NonStaticAccessInformation[] {nonStaticAccessInformation}, compilationUnit.getAST());
			}
		}
		
		if (addThisQualifier && IProblem.UnqualifiedFieldAccess == problem.getProblemId()) {
			UnqualifiedFieldAccessInformation unqualifiedFieldAccessInformation= getUnqualifiedFieldAccessInformation(compilationUnit, problem);
			if (unqualifiedFieldAccessInformation == null)
				return null;
			
			ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
			String groupName= MessageFormat.format(FixMessages.CodeStyleFix_QualifyWithThis_description, new Object[] {unqualifiedFieldAccessInformation.getName().getIdentifier(), unqualifiedFieldAccessInformation.getQualifier()});
			return new CodeStyleFix(groupName, cu, new UnqualifiedFieldAccessInformation[] {unqualifiedFieldAccessInformation}, null, compilationUnit.getAST());
		}
		
		return null;
	}
	
	public static CodeStyleFix createCleanUp(CompilationUnit compilationUnit, boolean addThisQualifier, boolean changeNonStaticAccessToStatic) throws CoreException {
		IProblem[] problems= compilationUnit.getProblems();
		
		List/*<NonStaticAccessInformation>*/ nonStaticAccesses= new ArrayList(); 
		for (int i= 0; i < problems.length; i++) {
			IProblemLocation problem= new ProblemLocation(problems[i]);
			NonStaticAccessInformation nonStaticAccessInformation= null;
			if (changeNonStaticAccessToStatic && isNonStaticAccess(problem)) {
				nonStaticAccessInformation= getNonStaticAccessInformation(compilationUnit, problem);
				if (nonStaticAccessInformation != null)
					nonStaticAccesses.add(nonStaticAccessInformation);
			}
		}
		
		if (!addThisQualifier && nonStaticAccesses.size() == 0)
			return null;

		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
		UnqualifiedFieldAccessInformation[] unqualifiedFieldAccessesArray= null;
		if (addThisQualifier) {
			FieldAccessFinder fieldAccessFinder= new FieldAccessFinder(cu);
			compilationUnit.accept(fieldAccessFinder);
			unqualifiedFieldAccessesArray= fieldAccessFinder.getUnqualifiedAccesses();
			
			if (unqualifiedFieldAccessesArray.length == 0 && nonStaticAccesses.size() == 0)
				return null;
		}
		
		NonStaticAccessInformation[] nonStaticAccessesArray= (NonStaticAccessInformation[])nonStaticAccesses.toArray(new NonStaticAccessInformation[nonStaticAccesses.size()]);
		return new CodeStyleFix(FixMessages.CodeStyleFix_AddThisQualifier_description, cu, unqualifiedFieldAccessesArray, nonStaticAccessesArray, compilationUnit.getAST());
	}

	private static boolean isNonStaticAccess(IProblemLocation problem) {
		return (problem.getProblemId() == IProblem.NonStaticAccessToStaticField ||
		 problem.getProblemId() == IProblem.NonStaticAccessToStaticMethod);
	}
	
	private static NonStaticAccessInformation getNonStaticAccessInformation(CompilationUnit astRoot, IProblemLocation problem) {
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
        
		ITypeBinding declaringTypeBinding= null;
		if (accessBinding != null) {
			declaringTypeBinding= getDeclaringTypeBinding(accessBinding);
			if (declaringTypeBinding != null) {
				declaringTypeBinding= declaringTypeBinding.getTypeDeclaration(); // use generic to avoid any type arguments
				
				return new NonStaticAccessInformation(declaringTypeBinding, accessBinding, qualifier, selectedNode);
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
		
	private static UnqualifiedFieldAccessInformation getUnqualifiedFieldAccessInformation(CompilationUnit compilationUnit, IProblemLocation problem) throws CoreException {
		SimpleName name= getName(compilationUnit, problem);
		if (name == null)
			return null;
		
		IBinding binding= name.resolveBinding();
		if (binding == null || binding.getKind() != IBinding.VARIABLE)
			return null;
		
		ImportRewrite imports= new ImportRewrite((ICompilationUnit)compilationUnit.getJavaElement());
		
		String replacement= getQualifier((IVariableBinding)binding, imports, name);
		if (replacement == null)
			return null;
		
		return new UnqualifiedFieldAccessInformation(replacement, name);
	}
	
	private static String getQualifier(IVariableBinding binding, ImportRewrite imports, SimpleName name) {
		ITypeBinding declaringClass= binding.getDeclaringClass();
		String qualifier;
		if (Modifier.isStatic(binding.getModifiers())) {
			qualifier= imports.addImport(declaringClass);
		} else {
			ITypeBinding parentType= Bindings.getBindingOfParentType(name);
			ITypeBinding currType= parentType;
			while (currType != null && !Bindings.isSuperType(declaringClass, currType)) {
				currType= currType.getDeclaringClass();
			}
			if (currType != parentType) {
				if (currType.isAnonymous())
					//If we access a field of a super class of an anonymous class
					//then we can only qualify with 'this' but not with outer.this
					//see bug 115277
					return null;
				
				String outer= imports.addImport(currType);
				qualifier= outer + ".this"; //$NON-NLS-1$
			} else {
				qualifier= "this"; //$NON-NLS-1$
			}
		}

		return qualifier;
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
	
	public CodeStyleFix(String name, ICompilationUnit compilationUnit, UnqualifiedFieldAccessInformation[] unqualifiedFieldAccesses, NonStaticAccessInformation[] nonStaticAccesses, AST ast) {
		super(name, compilationUnit);
		fUnqualifiedFieldAccesses= unqualifiedFieldAccesses;
		fNonStaticAccesses= nonStaticAccesses;
		fAst= ast;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.fix.IFix#createChange()
	 */
	public TextChange createChange() throws CoreException {
		AST ast= fAst;

		ASTRewrite rewrite= ASTRewrite.create(ast);
		ImportRewrite imports= new ImportRewrite(getCompilationUnit());
		List groups= new ArrayList();
		if (fUnqualifiedFieldAccesses != null) {
			for (int i= 0; i < fUnqualifiedFieldAccesses.length; i++) {
				UnqualifiedFieldAccessInformation information= fUnqualifiedFieldAccesses[i];
				rewriteASTForThisQualifier(information.getName(), information.getQualifier(), rewrite, groups);
			}
		}
		
		if (fNonStaticAccesses != null) {
			for (int i= 0; i < fNonStaticAccesses.length; i++) {
				NonStaticAccessInformation information= fNonStaticAccesses[i];
				rewriteASTForNonStaticAccess(imports, ast, information.getDeclaringTypeBinding(), information.getQualifier(), rewrite, groups);
			}
		}
		
		TextEdit edit= applyEdits(getCompilationUnit(), rewrite, imports);
		
		CompilationUnitChange result= new CompilationUnitChange("", getCompilationUnit()); //$NON-NLS-1$
		result.setEdit(edit);
			
		for (Iterator iter= groups.iterator(); iter.hasNext();) {
			TextEditGroup group= (TextEditGroup)iter.next();
			result.addTextEditGroup(group);
		}
			
		return result;
	}
	
	private void rewriteASTForNonStaticAccess(ImportRewrite imports, AST ast, ITypeBinding declaringTypeBinding, Expression qualifier, ASTRewrite rewrite, List groups) {
		String typeName= imports.addImport(declaringTypeBinding);
		TextEditGroup group= new TextEditGroup(Messages.format(FixMessages.CodeStyleFix_ChangeAccessToStatic_description, declaringTypeBinding.getName()));
		groups.add(group);
		rewrite.replace(qualifier, ASTNodeFactory.newName(ast, typeName), group);
	}

	private void rewriteASTForThisQualifier(SimpleName name, String qualifier, ASTRewrite rewrite, List editGroups) {
		String groupName= MessageFormat.format(FixMessages.CodeStyleFix_QualifyWithThis_description, new Object[] {name.getIdentifier(), qualifier});
		TextEditGroup group= new TextEditGroup(groupName);
		editGroups.add(group);
		rewrite.replace(name, rewrite.createStringPlaceholder(qualifier  + '.' + name.getIdentifier(), ASTNode.SIMPLE_NAME), group);
	}

}
