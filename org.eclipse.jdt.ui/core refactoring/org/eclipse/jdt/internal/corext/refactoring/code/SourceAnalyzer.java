/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dmitry Stalnov (dstalnov@fusionone.com) - contributed fixes for:
 * 	     o bug "Inline refactoring showed bogus error" (see bugzilla
 *         https://bugs.eclipse.org/bugs/show_bug.cgi?id=42753)
 *       o Allow 'this' constructor to be inlined  
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38093)
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.LocalVariableIndex;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.FlowContext;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.FlowInfo;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.InOutFlowAnalyzer;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

class SourceAnalyzer  {
	
	public static class NameData {
		private String fName;
		private List fReferences;
		public NameData(String n) {
			fName= n;
			fReferences= new ArrayList(2);
		}
		public String getName() {
			return fName;
		}
		public void addReference(SimpleName ref) {
			fReferences.add(ref);
		}
		public List references() {
			return fReferences;
		}
	}

	private class ActivationAnalyzer extends ASTVisitor {
		public RefactoringStatus status= new RefactoringStatus();
		private ASTNode fLastNode= getLastNode();
		private IMethodBinding fBinding= getBinding();
		public boolean visit(ReturnStatement node) {
			if (node != fLastNode) {
				fInterruptedExecutionFlow= true;
			}
			return true;
		}
		public boolean visit(TypeDeclaration node) {
			return false;
		}
		public boolean visit(AnonymousClassDeclaration node) {
			return false;
		}
		public boolean visit(MethodInvocation node) {
			if (fBinding != null && fBinding == node.getName().resolveBinding() && !status.hasFatalError()) {
				status.addFatalError(RefactoringCoreMessages.getString("InlineMethodRefactoring.SourceAnalyzer.recursive_call")); //$NON-NLS-1$
				return false;
			}
			return true;
		}
		public boolean visit(SimpleName node) {
			IBinding binding= node.resolveBinding();
			if (binding == null && !status.hasFatalError()) {
				// fixes bug #42753
				if (!ASTNodes.isLabel(node)) {
					status.addFatalError(
						RefactoringCoreMessages.getString("InlineMethodRefactoring.SourceAnalyzer.declaration_has_errors"), //$NON-NLS-1$
						JavaStatusContext.create(fCUnit, fDeclaration));
					return false;
				}
			}
			return true;
		}
		public boolean visit(ThisExpression node) {
			if (node.getQualifier() != null) {
				status.addFatalError(
					RefactoringCoreMessages.getString("InlineMethodRefactoring.SourceAnalyzer.qualified_this_expressions"), //$NON-NLS-1$
					JavaStatusContext.create(fCUnit, node));
				return false;
			}
			return true;
		}
		private ASTNode getLastNode() {
			List statements= fDeclaration.getBody().statements();
			if (statements.size() == 0)
				return null;
			return (ASTNode)statements.get(statements.size() - 1);
		}
		private IMethodBinding getBinding() {
			return fDeclaration.resolveBinding();
		}
	}
	
	private class UpdateCollector extends ASTVisitor {
		private int fTypeCounter;
		public boolean visit(TypeDeclaration node) {
			if (fTypeCounter++ == 0) {
				addNameData(node.getName());
			}
			Name superclass= node.getSuperclass();
			if (superclass != null) {
				ITypeBinding superBinding= ASTNodes.getTypeBinding(superclass);
				if (superBinding != null)
					fTypes.add(superclass);
			}
			List interfaces= node.superInterfaces();
			for (Iterator iter= interfaces.iterator(); iter.hasNext();) {
				Name element= (Name)iter.next();
				ITypeBinding binding= ASTNodes.getTypeBinding(element);
				if (binding != null)
					fTypes.add(element);
			}
			return true;
		}
		public void endVisit(TypeDeclaration node) {
			fTypeCounter--;
		}
		public boolean visit(AnonymousClassDeclaration node) {
			fTypeCounter++;
			return true;
		}
		public void endVisit(AnonymousClassDeclaration node) {
			fTypeCounter--;
		}
		public boolean visit(MethodDeclaration node) {
			if (node.isConstructor()) {
				TypeDeclaration decl= (TypeDeclaration) ASTNodes.getParent(node, ASTNode.TYPE_DECLARATION);
				NameData name= (NameData)fNames.get(decl.getName().resolveBinding());
				if (name != null) {
					name.addReference(node.getName());
				}
			}
			return true;
		}
		public boolean visit(MethodInvocation node) {
			if (fTypeCounter == 0) {
				Expression receiver= node.getExpression();
				if (receiver == null)
					fImplicitReceivers.add(node);
			}
			return true;
		}
		public boolean visit(ClassInstanceCreation node) {
			if (fTypeCounter == 0) {
				Expression receiver= node.getExpression();
				if (receiver == null) {
					if (node.resolveTypeBinding().isLocal())
						fImplicitReceivers.add(node);
				}
			}
			return true;
		}
		public boolean visit(SingleVariableDeclaration node) {
			if (fTypeCounter == 0)
				addNameData(node.getName());
			return true;
		}
		public boolean visit(VariableDeclarationFragment node) {
			if (fTypeCounter == 0)
				addNameData(node.getName());
			return true;
		}
		public boolean visit(SimpleName node) {
			IBinding binding= node.resolveBinding();
			ParameterData data= (ParameterData)fParameters.get(binding);
			if (data != null)
				data.addReference(node);
				
			NameData name= (NameData)fNames.get(binding);
			if (name != null)
				name.addReference(node);
			if (binding instanceof ITypeBinding) {
				ITypeBinding tb= (ITypeBinding)binding;
				Name qName= node;
				QualifiedName parent;
				while ((parent= (QualifiedName)ASTNodes.getParent(qName, ASTNode.QUALIFIED_NAME)) != null &&
						parent.getQualifier() != qName) {
					qName= parent; 
				}
				String typeName= null;
				if (tb.isArray())
					typeName= tb.getElementType().getQualifiedName();
				else
					typeName= tb.getQualifiedName();
				if (!ASTNodes.asString(qName).equals(typeName))
					fTypes.add(qName);
			}
			return true;
		}
		public boolean visit(ThisExpression node) {
			if (fTypeCounter == 0) {
				fImplicitReceivers.add(node);
			}
			return true;
		}
		private void addNameData(SimpleName name) {
			fNames.put(name.resolveBinding(), new NameData(name.getIdentifier()));
		}
	}

	private ICompilationUnit fCUnit;
	private MethodDeclaration fDeclaration;
	private Map fParameters;
	private Map fNames;
	private List fImplicitReceivers;
	private List fTypes;
	private boolean fInterruptedExecutionFlow;

	public SourceAnalyzer(ICompilationUnit unit, MethodDeclaration declaration) {
		super();
		fCUnit= unit;
		fDeclaration= declaration;
		List parameters= fDeclaration.parameters();
		fParameters= new HashMap(parameters.size() * 2);
		for (Iterator iter= parameters.iterator(); iter.hasNext();) {
			SingleVariableDeclaration element= (SingleVariableDeclaration) iter.next();
			fParameters.put(element.resolveBinding(), element.getProperty(ParameterData.PROPERTY));
		}
		fNames= new HashMap();
		fImplicitReceivers= new ArrayList(2);
		fTypes= new ArrayList(2);
	}
	
	public boolean isExecutionFlowInterrupted() {
		return fInterruptedExecutionFlow;
	}
	
	public RefactoringStatus checkActivation() throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		if (!fCUnit.isStructureKnown()) {
			result.addFatalError(		
				RefactoringCoreMessages.getString("InlineMethodRefactoring.SourceAnalyzer.syntax_errors"), //$NON-NLS-1$
				JavaStatusContext.create(fCUnit));		
			return result;
		}
		IProblem[] problems= ASTNodes.getProblems(fDeclaration, ASTNodes.NODE_ONLY, ASTNodes.ERROR);
		if (problems.length > 0) {
			result.addFatalError(		
				RefactoringCoreMessages.getString("InlineMethodRefactoring.SourceAnalyzer.declaration_has_errors"), //$NON-NLS-1$
				JavaStatusContext.create(fCUnit, fDeclaration));		
			return result;
		}
		if (fDeclaration.getBody() == null) {
			result.addFatalError(
				RefactoringCoreMessages.getString("InlineMethodRefactoring.SourceAnalyzer.abstract_methods"),  //$NON-NLS-1$
				JavaStatusContext.create(fCUnit, fDeclaration));
				return result;
		}
		ActivationAnalyzer analyzer= new ActivationAnalyzer();
		fDeclaration.accept(analyzer);
		result.merge(analyzer.status);
		if (!result.hasFatalError()) {
			
		}
		return result;
	}

	public void analyzeParameters() {
		Block body= fDeclaration.getBody();
		body.accept(new UpdateCollector());
		
		int numberOfLocals= LocalVariableIndex.perform(fDeclaration);
		FlowContext context= new FlowContext(0, numberOfLocals + 1);
		context.setConsiderAccessMode(true);
		context.setComputeMode(FlowContext.MERGE);
		InOutFlowAnalyzer flowAnalyzer= new InOutFlowAnalyzer(context);
		FlowInfo info= flowAnalyzer.perform(getStatements());
		
		for (Iterator iter= fDeclaration.parameters().iterator(); iter.hasNext();) {
			SingleVariableDeclaration element= (SingleVariableDeclaration) iter.next();
			IVariableBinding binding= element.resolveBinding();
			ParameterData data= (ParameterData)element.getProperty(ParameterData.PROPERTY);
			data.setAccessMode(info.getAccessMode(context, binding));
		}		
	}
	
	public Collection getUsedNames() {
		return fNames.values();
	}
	
	public List getImplicitReceivers() {
		return fImplicitReceivers;
	}
	
	public List getUsedTypes() {
		return fTypes;
	}
	private ASTNode[] getStatements() {
		List statements= fDeclaration.getBody().statements();
		return (ASTNode[]) statements.toArray(new ASTNode[statements.size()]);
	}	
}
