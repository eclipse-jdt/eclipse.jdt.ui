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
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
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
 * 		Qualify static field access with declaring class
 * 		Change non static access to static using declaring type.
 */
public class CodeStyleFix extends AbstractFix {
	
	private final UnqualifiedFieldAccessInformation[] fUnqualifiedFieldAccesses;
	private final StaticAccessInformation[] fNonStaticAccesses;
	private final UnqualifiedStaticFieldAccessInformation[] fUnqualifiedStatiFieldAccesses;
	private final ControlStatementInformation[] fControlStatementsWithoutBlock;
	private final CompilationUnit fCompilationUnit;
	
	private final static class CodeStyleVisitor extends GenericVisitor {
		
		private final List/*<UnqualifiedFieldAccessInformation>*/ fUnqualifiedAccesses;
		private final List/*<UnqualifiedStaticFieldAccessInformation>*/ fUnqualifiedStaticAccesses;
		private final List fControlStatementsWithoutBlock;
		private final ImportRewrite fImportRewrite;
		private final boolean fFindUnqualifiedAccesses;
		private final boolean fFindUnqualifiedStaticAccesses;
		private final boolean fFindControlStatementsWithoutBlock;
		
		public CodeStyleVisitor(ICompilationUnit compilationUnit, 
				boolean findUnqualifiedAccesses, 
				boolean findUnqualifiedStaticAccesses, 
				boolean findControlStatementsWithoutBlock) throws CoreException {
			
			fFindUnqualifiedAccesses= findUnqualifiedAccesses;
			fFindUnqualifiedStaticAccesses= findUnqualifiedStaticAccesses;
			fFindControlStatementsWithoutBlock= findControlStatementsWithoutBlock;
			fUnqualifiedAccesses= new ArrayList();
			fUnqualifiedStaticAccesses= new ArrayList();
			fImportRewrite= new ImportRewrite(compilationUnit);
			fControlStatementsWithoutBlock= new ArrayList();
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#visit(org.eclipse.jdt.core.dom.DoStatement)
		 */
		public boolean visit(DoStatement node) {
			if (fFindControlStatementsWithoutBlock) {
				ASTNode doBody= node.getBody();
				if (!(doBody instanceof Block)) {
					fControlStatementsWithoutBlock.add(new ControlStatementInformation(DoStatement.BODY_PROPERTY, doBody, node));
				}
			}
			return super.visit(node);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#visit(org.eclipse.jdt.core.dom.ForStatement)
		 */
		public boolean visit(ForStatement node) {
			if (fFindControlStatementsWithoutBlock) {
				ASTNode forBody= node.getBody();
				if (!(forBody instanceof Block)) {
					fControlStatementsWithoutBlock.add(new ControlStatementInformation(ForStatement.BODY_PROPERTY, forBody, node));
				}
			}
			return super.visit(node);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#visit(org.eclipse.jdt.core.dom.IfStatement)
		 */
		public boolean visit(IfStatement statement) {
			if (fFindControlStatementsWithoutBlock) {
				ASTNode then= statement.getThenStatement();
				if (!(then instanceof Block)) {
					fControlStatementsWithoutBlock.add(new ControlStatementInformation(IfStatement.THEN_STATEMENT_PROPERTY, then, statement));
				}
				ASTNode elseStatement= statement.getElseStatement();
				if (elseStatement != null && !(elseStatement instanceof Block) && !(elseStatement instanceof IfStatement)) {
					fControlStatementsWithoutBlock.add(new ControlStatementInformation(IfStatement.ELSE_STATEMENT_PROPERTY, elseStatement, statement));
				}
			}
			return super.visit(statement);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#visit(org.eclipse.jdt.core.dom.WhileStatement)
		 */
		public boolean visit(WhileStatement node) {
			if (fFindControlStatementsWithoutBlock) {
				ASTNode whileBody= node.getBody();
				if (!(whileBody instanceof Block)) {
					fControlStatementsWithoutBlock.add(new ControlStatementInformation(WhileStatement.BODY_PROPERTY, whileBody, node));
				}
			}
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
			if (binding == null || (binding.getKind() != IBinding.VARIABLE) || !((IVariableBinding) binding).isField())
				return;

			if (Modifier.isStatic(binding.getModifiers())) {
				if (fFindUnqualifiedStaticAccesses) {
					Initializer initializer= (Initializer) ASTNodes.getParent(node, Initializer.class);
					//Do not qualify assignments to static final fields in static initializers (would result in compile error)
					if (initializer != null && Modifier.isStatic(initializer.getModifiers())
							&& Modifier.isFinal(binding.getModifiers()) && node.getLocationInParent() == Assignment.LEFT_HAND_SIDE_PROPERTY)
						return;
					
					//Do not qualify static fields if defined inside an anonymous class
					if (((IVariableBinding)binding).getDeclaringClass().isAnonymous())
						return;
	
					fUnqualifiedStaticAccesses.add(new UnqualifiedStaticFieldAccessInformation((IVariableBinding) binding, node));
				}
			} else if (fFindUnqualifiedAccesses){
				String qualifier= getQualifier((IVariableBinding) binding, fImportRewrite, node);
				if (qualifier == null)
					return;

				fUnqualifiedAccesses.add(new UnqualifiedFieldAccessInformation(qualifier, node));
			}
		}

		public UnqualifiedFieldAccessInformation[] getUnqualifiedAccesses() {
			return (UnqualifiedFieldAccessInformation[])fUnqualifiedAccesses.toArray(new UnqualifiedFieldAccessInformation[fUnqualifiedAccesses.size()]);
		}
		
		public UnqualifiedStaticFieldAccessInformation[] getUnqualifiedStaticAccesses() {
			return (UnqualifiedStaticFieldAccessInformation[])fUnqualifiedStaticAccesses.toArray(new UnqualifiedStaticFieldAccessInformation[fUnqualifiedStaticAccesses.size()]);
		}
		
		public ControlStatementInformation[] getControlStatementInformation() {
			return (ControlStatementInformation[])fControlStatementsWithoutBlock.toArray(new ControlStatementInformation[fControlStatementsWithoutBlock.size()]);
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
	
	private final static class UnqualifiedStaticFieldAccessInformation {

		private final IVariableBinding fBinding;
		private final SimpleName fName;
		
		public UnqualifiedStaticFieldAccessInformation(IVariableBinding binding, SimpleName name) {
			fBinding= binding;
			fName= name;
		}

		public IVariableBinding getBinding() {
			return fBinding;
		}

		public SimpleName getName() {
			return fName;
		}
		
	}
	
	private final static class StaticAccessInformation {

		private final ITypeBinding fDeclaringTypeBinding;
		private final IBinding fAccessBinding;
		private final Expression fQualifier;
		private final ASTNode fNode;

		public StaticAccessInformation(ITypeBinding declaringTypeBinding, IBinding accessBinding, Expression qualifier, ASTNode node) {
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
	
	private static final class ControlStatementInformation {

		private final ChildPropertyDescriptor fBodyProperty;
		private final ASTNode fBody;
		private final Statement fStatement;

		public ControlStatementInformation(ChildPropertyDescriptor bodyProperty, ASTNode body, Statement statement) {
			fBodyProperty= bodyProperty;
			fBody= body;
			fStatement= statement;
		}

		public ASTNode getBody() {
			return fBody;
		}

		public ChildPropertyDescriptor getBodyProperty() {
			return fBodyProperty;
		}
		
		public Statement getStatement() {
			return fStatement;
		}

	}
	
	public static CodeStyleFix[] createFixForNonStaticAccess(CompilationUnit compilationUnit, IProblemLocation problem) throws CoreException {
		if (isNonStaticAccess(problem)) {
			StaticAccessInformation nonStaticAccessInformation[]= getNonStaticAccessInformation(compilationUnit, problem);
			if (nonStaticAccessInformation != null) {

				ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
				String label1= Messages.format(FixMessages.CodeStyleFix_ChangeAccessToStatic_description, nonStaticAccessInformation[0].getDeclaringTypeBinding().getName());
				CodeStyleFix fix1= new CodeStyleFix(label1, cu, null, new StaticAccessInformation[] {nonStaticAccessInformation[0]}, null, null, compilationUnit);

				if (nonStaticAccessInformation.length > 1) {
					String label2= Messages.format(FixMessages.CodeStyleFix_ChangeAccessToStaticUsingInstanceType_description, nonStaticAccessInformation[1].getDeclaringTypeBinding().getName());
					CodeStyleFix fix2= new CodeStyleFix(label2, cu, null, new StaticAccessInformation[] {nonStaticAccessInformation[1]}, null, null, compilationUnit);
					return new CodeStyleFix[] {fix1, fix2};
				}
				return new CodeStyleFix[] {fix1};
			}
		}
		return null;
	}

	public static CodeStyleFix createFix(CompilationUnit compilationUnit, IProblemLocation problem, 
			boolean addThisQualifier, 
			boolean changeIndirectStaticAccessToDirect) throws CoreException {
		
		if (changeIndirectStaticAccessToDirect && isIndirectStaticAccess(problem)) {
			StaticAccessInformation nonStaticAccessInformation[]= getNonStaticAccessInformation(compilationUnit, problem);
			if (nonStaticAccessInformation != null) {

				ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
				String label= Messages.format(FixMessages.CodeStyleFix_ChangeStaticAccess_description, nonStaticAccessInformation[0].getDeclaringTypeBinding().getName());
				return new CodeStyleFix(label, cu, null, new StaticAccessInformation[] {nonStaticAccessInformation[0]}, null, null, compilationUnit);
			}
		}
		
		if (addThisQualifier && IProblem.UnqualifiedFieldAccess == problem.getProblemId()) {
			UnqualifiedFieldAccessInformation unqualifiedFieldAccessInformation= getUnqualifiedFieldAccessInformation(compilationUnit, problem);
			if (unqualifiedFieldAccessInformation == null)
				return null;
			
			ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
			String groupName= MessageFormat.format(FixMessages.CodeStyleFix_QualifyWithThis_description, new Object[] {unqualifiedFieldAccessInformation.getName().getIdentifier(), unqualifiedFieldAccessInformation.getQualifier()});
			return new CodeStyleFix(groupName, cu, new UnqualifiedFieldAccessInformation[] {unqualifiedFieldAccessInformation}, null, null, null, compilationUnit);
		}
		
		return null;
	}
	
	public static CodeStyleFix createCleanUp(CompilationUnit compilationUnit, 
			boolean addThisQualifier, 
			boolean changeNonStaticAccessToStatic, 
			boolean qualifyStaticFieldAccess,
			boolean changeIndirectStaticAccessToDirect,
			boolean addBlockToControlStatements) throws CoreException {
		
		if (!addThisQualifier && !changeNonStaticAccessToStatic && !qualifyStaticFieldAccess && !changeIndirectStaticAccessToDirect && !addBlockToControlStatements)
			return null;
		
		IProblem[] problems= compilationUnit.getProblems();
		
		List/*<StaticAccessInformation>*/ nonStaticAccesses= new ArrayList(); 
		for (int i= 0; i < problems.length; i++) {
			IProblemLocation problem= new ProblemLocation(problems[i]);
			boolean isNonStaticAccess= changeNonStaticAccessToStatic && isNonStaticAccess(problem);
			boolean isIndirectStaticAccess= changeIndirectStaticAccessToDirect && isIndirectStaticAccess(problem);
			if (isNonStaticAccess || isIndirectStaticAccess) {
				StaticAccessInformation[] nonStaticAccessInformation= getNonStaticAccessInformation(compilationUnit, problem);
				if (nonStaticAccessInformation != null) {
					nonStaticAccesses.add(nonStaticAccessInformation[0]);
				}
			}
		}

		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
		if (addThisQualifier || qualifyStaticFieldAccess || addBlockToControlStatements) {
			
			CodeStyleVisitor codeStyleVisitor= new CodeStyleVisitor(cu, addThisQualifier, qualifyStaticFieldAccess, addBlockToControlStatements);
			compilationUnit.accept(codeStyleVisitor);
			
			UnqualifiedFieldAccessInformation[] unqualifiedFieldAccessesArray= null;
			UnqualifiedStaticFieldAccessInformation[] unqualifiedStaticFieldAccessesArray= null;
			ControlStatementInformation[] controlStatementInformationArray= null;
				
			if (addThisQualifier) {
				unqualifiedFieldAccessesArray= codeStyleVisitor.getUnqualifiedAccesses();
			}
				
			if (qualifyStaticFieldAccess) {
				unqualifiedStaticFieldAccessesArray= codeStyleVisitor.getUnqualifiedStaticAccesses();
			}
			
			if (addBlockToControlStatements) {
				controlStatementInformationArray= codeStyleVisitor.getControlStatementInformation();
			}
				
			if (nonStaticAccesses.isEmpty() && 
				(!addThisQualifier || unqualifiedFieldAccessesArray.length == 0) &&
				(!qualifyStaticFieldAccess || unqualifiedStaticFieldAccessesArray.length == 0) &&
				(!addBlockToControlStatements || controlStatementInformationArray.length == 0)) {
			
				return null;
			}
			
			StaticAccessInformation[] nonStaticAccessesArray= (StaticAccessInformation[])nonStaticAccesses.toArray(new StaticAccessInformation[nonStaticAccesses.size()]);
			return new CodeStyleFix(FixMessages.CodeStyleFix_AddThisQualifier_description, cu, unqualifiedFieldAccessesArray, nonStaticAccessesArray, unqualifiedStaticFieldAccessesArray, controlStatementInformationArray, compilationUnit);
		} else if (nonStaticAccesses.isEmpty()) {
			return null;
		} else {
			StaticAccessInformation[] nonStaticAccessesArray= (StaticAccessInformation[])nonStaticAccesses.toArray(new StaticAccessInformation[nonStaticAccesses.size()]);
			return new CodeStyleFix(FixMessages.CodeStyleFix_AddThisQualifier_description, cu, null, nonStaticAccessesArray, null, null, compilationUnit);
		}
	}

	private static boolean isIndirectStaticAccess(IProblemLocation problem) {
		return (problem.getProblemId() == IProblem.IndirectAccessToStaticField
				|| problem.getProblemId() == IProblem.IndirectAccessToStaticMethod);
	}
	
	private static boolean isNonStaticAccess(IProblemLocation problem) {
		return (problem.getProblemId() == IProblem.NonStaticAccessToStaticField
				|| problem.getProblemId() == IProblem.NonStaticAccessToStaticMethod);
	}
	
	private static StaticAccessInformation[] getNonStaticAccessInformation(CompilationUnit astRoot, IProblemLocation problem) {
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
        
		if (accessBinding != null) {
			StaticAccessInformation declaring= null;
			ITypeBinding declaringTypeBinding= getDeclaringTypeBinding(accessBinding);
			if (declaringTypeBinding != null) {
				declaringTypeBinding= declaringTypeBinding.getTypeDeclaration(); // use generic to avoid any type arguments
				
				declaring= new StaticAccessInformation(declaringTypeBinding, accessBinding, qualifier, selectedNode);
			}
			StaticAccessInformation instance= null;
			ITypeBinding instanceTypeBinding= Bindings.normalizeTypeBinding(qualifier.resolveTypeBinding());
			if (instanceTypeBinding != null) {
				instanceTypeBinding= instanceTypeBinding.getTypeDeclaration();  // use generic to avoid any type arguments
				if (instanceTypeBinding.getTypeDeclaration() != declaringTypeBinding) {
					instance= new StaticAccessInformation(instanceTypeBinding, accessBinding, qualifier, selectedNode);
				}
			}
			if (declaring != null && instance != null) {
				return new StaticAccessInformation[] {declaring, instance};
			} else {
				return new StaticAccessInformation[] {declaring};
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
	
	private CodeStyleFix(String name, ICompilationUnit compilationUnit, 
			UnqualifiedFieldAccessInformation[] unqualifiedFieldAccesses, 
			StaticAccessInformation[] nonStaticAccesses, 
			UnqualifiedStaticFieldAccessInformation[] unqualifiedStatiFieldAccesses, 
			ControlStatementInformation[] controlStatementsWithoutBlock, CompilationUnit cu) {
		
		super(name, compilationUnit);
		fUnqualifiedFieldAccesses= unqualifiedFieldAccesses;
		fNonStaticAccesses= nonStaticAccesses;
		fUnqualifiedStatiFieldAccesses= unqualifiedStatiFieldAccesses;
		fControlStatementsWithoutBlock= controlStatementsWithoutBlock;
		fCompilationUnit= cu;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.fix.IFix#createChange()
	 */
	public TextChange createChange() throws CoreException {
		AST ast= fCompilationUnit.getAST();

		ASTRewrite rewrite= ASTRewrite.create(ast);
		ImportRewrite imports= new ImportRewrite(getCompilationUnit());
		List groups= new ArrayList();
		if (fUnqualifiedFieldAccesses != null) {
			for (int i= 0; i < fUnqualifiedFieldAccesses.length; i++) {
				UnqualifiedFieldAccessInformation information= fUnqualifiedFieldAccesses[i];
				rewriteASTForThisQualifier(information.getName(), information.getQualifier(), rewrite, groups);
			}
		}
		
		if (fUnqualifiedStatiFieldAccesses != null) {
			for (int i= 0; i < fUnqualifiedStatiFieldAccesses.length; i++) {
				UnqualifiedStaticFieldAccessInformation information= fUnqualifiedStatiFieldAccesses[i];
				rewriteASTForDeclaringClassQualifier(information.getName(), information.getBinding(), imports, rewrite, groups);
			}
		}
		
		if (fNonStaticAccesses != null) {
			for (int i= 0; i < fNonStaticAccesses.length; i++) {
				StaticAccessInformation information= fNonStaticAccesses[i];
				rewriteASTForNonStaticAccess(imports, ast, information.getDeclaringTypeBinding(), information.getQualifier(), rewrite, groups);
			}
		}
		
		if (fControlStatementsWithoutBlock != null) {
			for (int i= 0; i < fControlStatementsWithoutBlock.length; i++) {
				ControlStatementInformation information= fControlStatementsWithoutBlock[i];
				rewriteASTForAddingBlockToControlStatement(ast, information.getStatement(), information.getBodyProperty(), information.getBody(), rewrite, groups);
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
	
	private void rewriteASTForAddingBlockToControlStatement(AST ast, Statement statement, ChildPropertyDescriptor childProperty, ASTNode child, ASTRewrite rewrite, List groups) {
		
		String label;
		if (childProperty == IfStatement.THEN_STATEMENT_PROPERTY) {
			label = FixMessages.CodeStyleFix_ChangeIfToBlock_desription;
		} else if (childProperty == IfStatement.ELSE_STATEMENT_PROPERTY) {
			label = FixMessages.CodeStyleFix_ChangeElseToBlock_description;
		} else {
			label = FixMessages.CodeStyleFix_ChangeControlToBlock_description;
		}
		
		TextEditGroup group= new TextEditGroup(label);
		groups.add(group);
		
		ASTNode childPlaceholder= rewrite.createMoveTarget(child);
		Block replacingBody= ast.newBlock();
		replacingBody.statements().add(childPlaceholder);
		rewrite.set(statement, childProperty, replacingBody, group);
	}

	private void rewriteASTForNonStaticAccess(ImportRewrite imports, AST ast, ITypeBinding declaringTypeBinding, Expression qualifier, ASTRewrite rewrite, List groups) {
		String typeName= importClass(declaringTypeBinding, qualifier, imports);
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
	
	private void rewriteASTForDeclaringClassQualifier(SimpleName name, IVariableBinding binding, ImportRewrite imports, ASTRewrite rewrite, List groups) {
		ITypeBinding declaringClass= binding.getDeclaringClass();
		String qualifier= importClass(declaringClass, name, imports);
		TextEditGroup group= new TextEditGroup(Messages.format(FixMessages.CodeStyleFix_QualifyWithThis_description, new Object[] {name.getIdentifier(), qualifier}));
		groups.add(group);
		rewrite.replace(name, rewrite.createStringPlaceholder(qualifier  + '.' + name.getIdentifier(), ASTNode.SIMPLE_NAME), group);
	}

	private String importClass(final ITypeBinding toImport, ASTNode accessor, ImportRewrite imports) {
		final String qualifier= imports.addImport(toImport);
		if (doesDeclare(toImport, getWrittenIn(accessor))) {
			//The import is not required but is added by the ImportRewrite
			imports.removeImport(toImport);
			return qualifier;
		}
		if (qualifier.indexOf('.') != -1)
			return qualifier;
		
		final boolean[] isSilentImported= new boolean[1];
		isSilentImported[0]= false;
		final boolean[] isImported= new boolean[1];
		isImported[0]= false;
		fCompilationUnit.accept(new GenericVisitor() {

			public boolean visit(ImportDeclaration node) {
				if (node.resolveBinding() == toImport) 
					isImported[0]= true;
				return false;
			}

			protected boolean visitNode(ASTNode node) {
				if (isSilentImported[0] || isImported[0])
					return false;
				
				return super.visitNode(node);
			}

			public boolean visit(SimpleName name) {
				if (name.getIdentifier().equals(qualifier) && name.resolveBinding() != toImport) {
					isSilentImported[0]= true;
				}
				return false;
			}
			
		});
		if (isSilentImported[0] && !isImported[0]) {
			imports.removeImport(toImport);
			return getQualifiedName(toImport);
		} 
		return qualifier;
	}
	
	public String getQualifiedName(ITypeBinding binding) {
		
		if (binding.isPrimitive() || binding.isTypeVariable()) {
			return binding.getName();
		}
		
		ITypeBinding normalizedBinding= Bindings.normalizeTypeBinding(binding);
		if (normalizedBinding == null) {
			return "invalid"; //$NON-NLS-1$
		}
		if (normalizedBinding.isWildcardType()) {
			StringBuffer res= new StringBuffer("?"); //$NON-NLS-1$
			ITypeBinding bound= normalizedBinding.getBound();
			if (bound != null && !bound.isWildcardType() && !bound.isCapture()) { // bug 95942
				if (normalizedBinding.isUpperbound()) {
					res.append(" extends "); //$NON-NLS-1$
				} else {
					res.append(" super "); //$NON-NLS-1$
				}
				res.append(getQualifiedName(bound));
			}
			return res.toString();
		}
		
		if (normalizedBinding.isArray()) {
			StringBuffer res= new StringBuffer(getQualifiedName(normalizedBinding.getElementType()));
			for (int i= normalizedBinding.getDimensions(); i > 0; i--) {
				res.append("[]"); //$NON-NLS-1$
			}
			return res.toString();
		}
		
		return Bindings.getRawQualifiedName(normalizedBinding);
	}
	
	private ITypeBinding getWrittenIn(ASTNode node) {
		TypeDeclaration current= (TypeDeclaration)ASTNodes.getParent(node, TypeDeclaration.class);
		TypeDeclaration importingClass= null;
		do {
			importingClass= current;
			current= (TypeDeclaration)ASTNodes.getParent(current, TypeDeclaration.class);
		} while (current != null);
		return importingClass.resolveBinding();
	}
	
	private boolean doesDeclare(ITypeBinding declaredClass, ITypeBinding declaringClass) {
		ITypeBinding curr= declaredClass.getDeclaringClass();
		while (curr != null && declaringClass != curr) {
			curr= curr.getDeclaringClass();
		}
		return curr != null;
	}
}
