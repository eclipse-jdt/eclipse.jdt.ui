/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation 
 *          (report 36180: Callers/Callees view)
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.callhierarchy;

import java.util.Collection;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;

class CalleeAnalyzerVisitor extends ASTVisitor {
    private CallSearchResultCollector fSearchResults;
    private IMethod fMethod;
    private CompilationUnit fCompilationUnit;
    private IProgressMonitor fProgressMonitor;
    private int fMethodEndPosition;
    private int fMethodStartPosition;

    CalleeAnalyzerVisitor(IMethod method, CompilationUnit compilationUnit, IProgressMonitor progressMonitor) {
        fSearchResults = new CallSearchResultCollector();
        this.fMethod = method;
        this.fCompilationUnit= compilationUnit;
        this.fProgressMonitor = progressMonitor;

        try {
            ISourceRange sourceRange = method.getSourceRange();
            this.fMethodStartPosition = sourceRange.getOffset();
            this.fMethodEndPosition = fMethodStartPosition + sourceRange.getLength();
        } catch (JavaModelException jme) {
            JavaPlugin.log(jme);
        }
    }

    /**
     * Method getCallees.
     *
     * @return CallerElement
     */
    public Map getCallees() {
        return fSearchResults.getCallers();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.ClassInstanceCreation)
     */
    public boolean visit(ClassInstanceCreation node) {
        progressMonitorWorked(1);
        if (!isFurtherTraversalNecessary(node)) {
            return false;
        }

        if (isNodeWithinMethod(node)) {
            addMethodCall(node.resolveConstructorBinding(), node);
        }

        return true;
    }

    /**
     * Find all constructor invocations (<code>this(...)</code>) from the called method.
     * Since we only traverse into the AST on the wanted method declaration, this method
     * should not hit on more constructor invocations than those in the wanted method.
     *
     * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.ConstructorInvocation)
     */
    public boolean visit(ConstructorInvocation node) {
        progressMonitorWorked(1);
        if (!isFurtherTraversalNecessary(node)) {
            return false;
        }

        if (isNodeWithinMethod(node)) {
            addMethodCall(node.resolveConstructorBinding(), node);
        }

        return true;
    }

    /**
     * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MethodDeclaration)
     */
    public boolean visit(MethodDeclaration node) {
        progressMonitorWorked(1);
        return isFurtherTraversalNecessary(node);
    }

    /**
     * Find all method invocations from the called method. Since we only traverse into
     * the AST on the wanted method declaration, this method should not hit on more
     * method invocations than those in the wanted method.
     *
     * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MethodInvocation)
     */
    public boolean visit(MethodInvocation node) {
        progressMonitorWorked(1);
        if (!isFurtherTraversalNecessary(node)) {
            return false;
        }

        if (isNodeWithinMethod(node)) {
            addMethodCall(node.resolveMethodBinding(), node);
        }

        return true;
    }

    /**
     * Find invocations of the supertype's constructor from the called method
     * (=constructor). Since we only traverse into the AST on the wanted method
     * declaration, this method should not hit on more method invocations than those in
     * the wanted method.
     *
     * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.SuperConstructorInvocation)
     */
    public boolean visit(SuperConstructorInvocation node) {
        progressMonitorWorked(1);
        if (!isFurtherTraversalNecessary(node)) {
            return false;
        }

        if (isNodeWithinMethod(node)) {
            addMethodCall(node.resolveConstructorBinding(), node);
        }
        
        return true;
    }

    /**
     * Find all method invocations from the called method. Since we only traverse into
     * the AST on the wanted method declaration, this method should not hit on more
     * method invocations than those in the wanted method.
     *
     * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MethodInvocation)
     */
    public boolean visit(SuperMethodInvocation node) {
        progressMonitorWorked(1);
        if (!isFurtherTraversalNecessary(node)) {
            return false;
        }

        if (isNodeWithinMethod(node)) {
            addMethodCall(node.resolveMethodBinding(), node);
        }
        
        return true;
    }
    
    /**
     * When an anonymous class declaration is reached, the traversal should not go further since it's not
     * supposed to consider calls inside the anonymous inner class as calls from the outer method.
     * 
     * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.AnonymousClassDeclaration)
     */
    public boolean visit(AnonymousClassDeclaration node) {
        return isNodeEnclosingMethod(node);
    }


    /**
     * Adds the specified method binding to the search results.
     *
     * @param calledMethodBinding
     * @param node
     */
    protected void addMethodCall(IMethodBinding calledMethodBinding, ASTNode node) {
        try {
            if (calledMethodBinding != null) {
                fProgressMonitor.worked(1);

                ITypeBinding calledTypeBinding = calledMethodBinding.getDeclaringClass();
                IType calledType = null;

                if (!calledTypeBinding.isAnonymous()) {
                    calledType = Bindings.findType(calledTypeBinding,
                            fMethod.getJavaProject());
                } else {
                    if (!"java.lang.Object".equals(calledTypeBinding.getSuperclass().getQualifiedName())) { //$NON-NLS-1$
                        calledType= Bindings.findType(calledTypeBinding.getSuperclass(),
                        fMethod.getJavaProject());
                    } else {
                        calledType = Bindings.findType(calledTypeBinding.getInterfaces()[0],
                                fMethod.getJavaProject());
                    }
                }

                IMethod calledMethod = findIncludingSupertypes(calledMethodBinding,
                        calledType, fProgressMonitor);

                IMember referencedMember= null;
                if (calledMethod == null) {
                    if (calledMethodBinding.isConstructor() && calledMethodBinding.getParameterTypes().length == 0) {
                        referencedMember= calledType;
                    }
                } else { 
                    if (calledType.isInterface()) {
                        calledMethod = findImplementingMethods(calledMethod);
                    }

                    if (!isIgnoredBySearchScope(calledMethod)) {
                        referencedMember= calledMethod;
                    }
                }
                
                fSearchResults.addMember(fMethod, referencedMember,
                    node.getStartPosition(),
                    node.getStartPosition() + node.getLength(), fCompilationUnit.lineNumber(node.getStartPosition()));
            }
        } catch (JavaModelException jme) {
            JavaPlugin.log(jme);
        }
    }
    
	private static IMethod findIncludingSupertypes(IMethodBinding method, IType type, IProgressMonitor pm) throws JavaModelException {
		IMethod inThisType= Bindings.findMethod(method, type);
		if (inThisType != null)
			return inThisType;
		IType[] superTypes= JavaModelUtil.getAllSuperTypes(type, pm);
		for (int i= 0; i < superTypes.length; i++) {
			IMethod m= Bindings.findMethod(method, superTypes[i]);
			if (m != null)
				return m;
		}
		return null;
	}

    private boolean isIgnoredBySearchScope(IMethod enclosingElement) {
        if (enclosingElement != null) {
            return !getSearchScope().encloses(enclosingElement);
        } else {
            return false;
        }
    }

    private IJavaSearchScope getSearchScope() {
        return CallHierarchy.getDefault().getSearchScope();
    }

    private boolean isNodeWithinMethod(ASTNode node) {
        int nodeStartPosition = node.getStartPosition();
        int nodeEndPosition = nodeStartPosition + node.getLength();

        if (nodeStartPosition < fMethodStartPosition) {
            return false;
        }

        if (nodeEndPosition > fMethodEndPosition) {
            return false;
        }

        return true;
    }

    private boolean isNodeEnclosingMethod(ASTNode node) {
        int nodeStartPosition = node.getStartPosition();
        int nodeEndPosition = nodeStartPosition + node.getLength();

        if (nodeStartPosition < fMethodStartPosition && nodeEndPosition > fMethodEndPosition) {
            // Is the method completely enclosed by the node?
            return true;
        }
        return false;
    }
    
    private boolean isFurtherTraversalNecessary(ASTNode node) {
        return isNodeWithinMethod(node) || isNodeEnclosingMethod(node);
    }

    private IMethod findImplementingMethods(IMethod calledMethod) {
        Collection implementingMethods = CallHierarchy.getDefault()
                                                        .getImplementingMethods(calledMethod);

        if ((implementingMethods.size() == 0) || (implementingMethods.size() > 1)) {
            return calledMethod;
        } else {
            return (IMethod) implementingMethods.iterator().next();
        }
    }
    
    private void progressMonitorWorked(int work) {
        if (fProgressMonitor != null) {
            fProgressMonitor.worked(work);
            if (fProgressMonitor.isCanceled()) {
                throw new OperationCanceledException();
            }
        }
    }
}
