/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
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
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportReferencesCollector;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.LocalVariableIndex;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.FlowContext;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.FlowInfo;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.InOutFlowAnalyzer;

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
		public boolean visit(EnumDeclaration node) {
			return false;
		}		
		public boolean visit(AnnotationTypeDeclaration node) {
			return false;
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
			return visitType(node);
		}
		public void endVisit(TypeDeclaration node) {
			fTypeCounter--;
		}
		public boolean visit(EnumDeclaration node) {
			return visitType(node);
		}
		public void endVisit(EnumDeclaration node) {
			fTypeCounter--;
		}
		public boolean visit(AnnotationTypeDeclaration node) {
			return visitType(node);
		}
		public void endVisit(AnnotationTypeDeclaration node) {
			fTypeCounter--;
		}
		private boolean visitType(AbstractTypeDeclaration node) {
			if (fTypeCounter++ == 0) {
				addNameReference(node.getName());
			}
			return true;
		}
		public boolean visit(AnonymousClassDeclaration node) {
			fTypeCounter++;
			return true;
		}
		public void endVisit(AnonymousClassDeclaration node) {
			fTypeCounter--;
		}
		public boolean visit(FieldAccess node) {
			if (node.getExpression() == null && !isStaticallyImported(node.getName())) {
				fImplicitReceivers.add(node);
			}
			return true;
		}
		public boolean visit(MethodDeclaration node) {
			if (node.isConstructor()) {
				AbstractTypeDeclaration decl= (AbstractTypeDeclaration) ASTNodes.getParent(node, AbstractTypeDeclaration.class);
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
				if (receiver == null && !isStaticallyImported(node.getName()))
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
				addNameReference(node.getName());
			return true;
		}
		public boolean visit(VariableDeclarationFragment node) {
			if (fTypeCounter == 0)
				addNameReference(node.getName());
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
				ITypeBinding type= (ITypeBinding)binding;
				if (type.isTypeVariable()) {
					addTypeVariableReference(type, node);
				}
			} else if (binding instanceof IVariableBinding) {
				IVariableBinding vb= (IVariableBinding)binding;
				if (vb.isField() && ! isStaticallyImported(node)) {
					ASTNode parent= node.getParent();
					if (!(parent instanceof QualifiedName)) {
						StructuralPropertyDescriptor location= node.getLocationInParent();
						if (location != SingleVariableDeclaration.NAME_PROPERTY 
							&& location != VariableDeclarationFragment.NAME_PROPERTY) {
							fImplicitReceivers.add(node);
						}
					}
				}
			}
			return true;
		}
		public boolean visit(ThisExpression node) {
			if (fTypeCounter == 0) {
				fImplicitReceivers.add(node);
			}
			return true;
		}
		private void addNameReference(SimpleName name) {
			fNames.put(name.resolveBinding(), new NameData(name.getIdentifier()));
		}
		private void addTypeVariableReference(ITypeBinding variable, SimpleName name) {
			NameData data= (NameData)fTypeParameterMapping.get(variable);
			if (data == null) {
				data= (NameData)fMethodTypeParameterMapping.get(variable);
			}
			data.addReference(name);
		}
		private boolean isStaticallyImported(Name name) {
			return fStaticsToImport.contains(name);
		}
	}

	private ICompilationUnit fCUnit;
	private MethodDeclaration fDeclaration;
	private Map fParameters;
	private Map fNames;
	private List fImplicitReceivers;
	
	private List/*<Name>*/ fTypesToImport;
	private List/*<Name>*/ fStaticsToImport;
	
	private List/*<NameData>*/ fTypeParameterReferences;
	private Map/*<ITypeBinding, NameData>*/ fTypeParameterMapping;
	
	private List/*<NameData>*/ fMethodTypeParameterReferences;
	private Map/*<ITypeBinding, NameData>*/ fMethodTypeParameterMapping;
	
	private boolean fInterruptedExecutionFlow;

	public SourceAnalyzer(ICompilationUnit unit, MethodDeclaration declaration) {
		super();
		fCUnit= unit;
		fDeclaration= declaration;
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
			List parameters= fDeclaration.parameters();
			fParameters= new HashMap(parameters.size() * 2);
			for (Iterator iter= parameters.iterator(); iter.hasNext();) {
				SingleVariableDeclaration element= (SingleVariableDeclaration) iter.next();
				IVariableBinding binding= element.resolveBinding();
				if (binding == null) {
					result.addFatalError(
						RefactoringCoreMessages.getString("InlineMethodRefactoring.SourceAnalyzer.declaration_has_errors"), //$NON-NLS-1$
						JavaStatusContext.create(fCUnit, fDeclaration));
					return result;
				}
				fParameters.put(binding, element.getProperty(ParameterData.PROPERTY));
			}
			fNames= new HashMap();
			fImplicitReceivers= new ArrayList(2);
			
			fTypeParameterReferences= new ArrayList(0);
			fTypeParameterMapping= new HashMap();
			ITypeBinding declaringType= fDeclaration.resolveBinding().getDeclaringClass();
			if (declaringType == null) {
				result.addFatalError(
					RefactoringCoreMessages.getString("InlineMethodRefactoring.SourceAnalyzer.typedeclaration_has_errors"), //$NON-NLS-1$
					JavaStatusContext.create(fCUnit));
				return result;
			}
			ITypeBinding[] typeParameters= declaringType.getTypeParameters();
			for (int i= 0; i < typeParameters.length; i++) {
				NameData data= new NameData(typeParameters[i].getName());
				fTypeParameterReferences.add(data);
				fTypeParameterMapping.put(typeParameters[i], data);
			}
			
			fMethodTypeParameterReferences= new ArrayList(0);
			fMethodTypeParameterMapping= new HashMap();
			IMethodBinding method= fDeclaration.resolveBinding();
			typeParameters= method.getTypeParameters();
			for (int i= 0; i < typeParameters.length; i++) {
				NameData data= new NameData(typeParameters[i].getName());
				fMethodTypeParameterReferences.add(data);
				fMethodTypeParameterMapping.put(typeParameters[i], data);
			}
			
		}
		return result;
	}

	public void initialize() {
		Block body= fDeclaration.getBody();
		// first collect the static imports. This is neccessary to not mark
		// static imported fields and methods as implicit visible.
		fTypesToImport= new ArrayList();
		fStaticsToImport= new ArrayList();
		ImportReferencesCollector collector= new ImportReferencesCollector(
			fCUnit.getJavaProject(), null, fTypesToImport, fStaticsToImport);
		body.accept(collector);
		
		// Now collect implicit references and name references
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
	
	public List getTypesToImport() {
		return fTypesToImport;
	}
	
	public List getStaticsToImport() {
		return fStaticsToImport;
	}
	
	public List getTypeParameterReferences() {
		return fTypeParameterReferences;
	}
	
	public List getMethodTypeParameterReferences() {
		return fMethodTypeParameterReferences;
	}
	
	private ASTNode[] getStatements() {
		List statements= fDeclaration.getBody().statements();
		return (ASTNode[]) statements.toArray(new ASTNode[statements.size()]);
	}	
}
