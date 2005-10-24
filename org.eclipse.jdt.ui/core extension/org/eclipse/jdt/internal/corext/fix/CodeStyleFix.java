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
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix wich fixes code style isuess.
 * Supports:
 * 		Qualifie field with this: f -> this.f if f is a field.
 * 		Change non static access to static using declaring type.
 */
public class CodeStyleFix extends AbstractFix {
	
	public static final String ADD_THIS_QUALIFIER= "Add 'this' qualifier";
	public static final String QUALIFY_0_WITH_THIS= "Qualify ''{0}'' with ''this''";
	
	private final TupleForUnqualifiedAccess[] fBindingTuples;
	private final TupleForNonStaticAccess[] fTupleDirects;

	public static class TupleForUnqualifiedAccess {

		private final IBinding fBinding;
		private final SimpleName fName;

		public TupleForUnqualifiedAccess(IBinding binding, SimpleName name) {
			fBinding= binding;
			fName= name;
		}

		public SimpleName getName() {
			return fName;
		}

		public IBinding getBinding() {
			return fBinding;
		}
		
	}
	
	public static class TupleForNonStaticAccess {

		private final ITypeBinding fDeclaringTypeBinding;
		private final IBinding fAccessBinding;
		private final Expression fQualifier;

		public TupleForNonStaticAccess(ITypeBinding declaringTypeBinding, IBinding accessBinding, Expression qualifier) {
			fDeclaringTypeBinding= declaringTypeBinding;
			fAccessBinding= accessBinding;
			fQualifier= qualifier;

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
		
	}

	public static CodeStyleFix createFix(CompilationUnit compilationUnit, IProblemLocation problem, boolean addThisQualifier, boolean changeNonStaticAccessToStatic) {
		if (changeNonStaticAccessToStatic && isNonStaticAccess(problem)) {
			TupleForNonStaticAccess tupleDirect= getTupleForNonStaticAccess(compilationUnit, problem);
			if (tupleDirect != null) {

				ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
				String label= Messages.format("Change access to static using ''{0}'' (declaring type)", tupleDirect.getDeclaringTypeBinding().getName());
				return new CodeStyleFix(label, cu, null, new TupleForNonStaticAccess[] {tupleDirect});
			}
		}
		
		if (addThisQualifier && IProblem.UnqualifiedFieldAccess == problem.getProblemId()) {
			TupleForUnqualifiedAccess tuple= getBindingTuple(compilationUnit, problem);
			if (tuple == null)
				return null;
			
			ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
			String groupName= MessageFormat.format(QUALIFY_0_WITH_THIS, new Object[] {tuple.getName().getFullyQualifiedName()});
			return new CodeStyleFix(groupName, cu, new TupleForUnqualifiedAccess[] {tuple}, null);
		}
		
		return null;
	}

	public static boolean isNonStaticAccess(IProblemLocation problem) {
		return (problem.getProblemId() == IProblem.NonStaticAccessToStaticField ||
		 problem.getProblemId() == IProblem.NonStaticAccessToStaticMethod);
	}
	
	public static TupleForNonStaticAccess getTupleForNonStaticAccess(CompilationUnit astRoot, IProblemLocation problem) {
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
				
				return new TupleForNonStaticAccess(declaringTypeBinding, accessBinding, qualifier);
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
		
	public static TupleForUnqualifiedAccess getBindingTuple(CompilationUnit compilationUnit, IProblemLocation problem) {
		SimpleName name= getName(compilationUnit, problem);
		if (name == null)
			return null;
		
		IBinding binding= name.resolveBinding();
		if (binding == null || binding.getKind() != IBinding.VARIABLE)
			return null;
		
		return new TupleForUnqualifiedAccess(binding, name);
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
	
	public CodeStyleFix(String name, ICompilationUnit compilationUnit, TupleForUnqualifiedAccess[] bindingTuples, TupleForNonStaticAccess[] tupleDirects) {
		super(name, compilationUnit);
		fBindingTuples= bindingTuples;
		fTupleDirects= tupleDirects;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.fix.IFix#createChange()
	 */
	public TextChange createChange() throws CoreException {
		AST ast= null;
		if (fBindingTuples != null && fBindingTuples.length > 0) {
			ast= fBindingTuples[0].getName().getAST();
		} else if (fTupleDirects != null && fTupleDirects.length > 0) {
			ast= fTupleDirects[0].getQualifier().getAST();
		} else {
			return null;
		}

		ASTRewrite rewrite= ASTRewrite.create(ast);
		ImportRewrite imports= new ImportRewrite(getCompilationUnit());
		List groups= new ArrayList();
		if (fBindingTuples != null) {
			for (int i= 0; i < fBindingTuples.length; i++) {
				TupleForUnqualifiedAccess tuple= fBindingTuples[i];
				rewriteASTForThisQualifier(imports, tuple.getName(), tuple.getBinding(), rewrite, groups);
			}
		}
		
		if (fTupleDirects != null) {
			for (int i= 0; i < fTupleDirects.length; i++) {
				TupleForNonStaticAccess tuple= fTupleDirects[i];
				rewriteASTForNonStaticAccess(imports, ast, tuple.getDeclaringTypeBinding(), tuple.getQualifier(), rewrite, groups);
			}
		}
		
		TextEdit edit= applyEdits(getCompilationUnit(), rewrite, imports);
		
		CompilationUnitChange result= new CompilationUnitChange(ADD_THIS_QUALIFIER, getCompilationUnit());
		result.setEdit(edit);
			
			//Commented out: Lead to no more handle error if applayed to large projects
//			for (Iterator iter= groups.iterator(); iter.hasNext();) {
//				TextEditGroup group= (TextEditGroup)iter.next();
//				result.addTextEditGroup(group);
//			}
			
		return result;
	}
	
	private void rewriteASTForNonStaticAccess(ImportRewrite imports, AST ast, ITypeBinding declaringTypeBinding, Expression qualifier, ASTRewrite rewrite, List groups) {
		String typeName= imports.addImport(declaringTypeBinding);
		rewrite.replace(qualifier, ASTNodeFactory.newName(ast, typeName), null);
	}

	private void rewriteASTForThisQualifier(ImportRewrite imports, SimpleName name, IBinding binding, ASTRewrite rewrite, List editGroups) {
		ITypeBinding declaringClass= ((IVariableBinding) binding).getDeclaringClass();
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
				String outer= imports.addImport(currType);
				qualifier= outer + ".this"; //$NON-NLS-1$
			} else {
				qualifier= "this"; //$NON-NLS-1$
			}
		}

		String replacement= qualifier + '.' + name.getIdentifier();
		String groupName= MessageFormat.format(QUALIFY_0_WITH_THIS, new Object[] {name.getFullyQualifiedName()});
		TextEditGroup group= new TextEditGroup(groupName);
		editGroups.add(group);
		rewrite.replace(name, rewrite.createStringPlaceholder(replacement, ASTNode.SIMPLE_NAME), group);
	}

}
