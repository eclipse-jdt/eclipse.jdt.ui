/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation
 *          (report 36180: Callers/Callees view)
 *   Red Hat Inc. - refactored to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.callhierarchy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

class CalleeAnalyzerVisitor extends HierarchicalASTVisitor {
    private final CallSearchResultCollector fSearchResults;
    private final IMember fMember;
    private final CompilationUnit fCompilationUnit;
    private final IProgressMonitor fProgressMonitor;
    private int fMethodEndPosition;
    private int fMethodStartPosition;
	private CallLocation fCalledAt;

    CalleeAnalyzerVisitor(CallLocation calledAt, IMember member, CompilationUnit compilationUnit, IProgressMonitor progressMonitor) {
		fSearchResults = new CallSearchResultCollector();
        this.fCalledAt= calledAt;
        this.fMember = member;
        this.fCompilationUnit= compilationUnit;
        this.fProgressMonitor = progressMonitor;

        try {
            ISourceRange sourceRange = member.getSourceRange();
            this.fMethodStartPosition = sourceRange.getOffset();
            this.fMethodEndPosition = fMethodStartPosition + sourceRange.getLength();
        } catch (JavaModelException jme) {
            JavaManipulationPlugin.log(jme);
        }
    }

    /**
     * @return a map from handle identifier ({@link String}) to {@link MethodCall}
     */
    public Map<String, MethodCall> getCallees() {
        return fSearchResults.getCallers();
    }

    @Override
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
    @Override
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
     * @see HierarchicalASTVisitor#visit(org.eclipse.jdt.core.dom.AbstractTypeDeclaration)
     */
    @Override
	public boolean visit(AbstractTypeDeclaration node) {
    	progressMonitorWorked(1);
    	if (!isFurtherTraversalNecessary(node)) {
    		return false;
    	}

    	if (isNodeWithinMethod(node)) {
    		List<BodyDeclaration> bodyDeclarations= node.bodyDeclarations();
    		for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
				if (bodyDeclaration instanceof MethodDeclaration) {
					MethodDeclaration child= (MethodDeclaration) bodyDeclaration;
					if (child.isConstructor()) {
						addMethodCall(child.resolveBinding(), child.getName());
					}
				}
			}
    		return false;
    	}

    	return true;
    }

    /**
     * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MethodDeclaration)
     */
    @Override
	public boolean visit(MethodDeclaration node) {
        progressMonitorWorked(1);
        if(Modifier.isAbstract(node.getModifiers()) ||
        		((node.getParent() instanceof TypeDeclaration) && ((TypeDeclaration)node.getParent()).isInterface())) {
    		addMethodCall(node.resolveBinding(), node);
        }
        return isFurtherTraversalNecessary(node);
    }

    /**
     * Find all method invocations from the called method. Since we only traverse into
     * the AST on the wanted method declaration, this method should not hit on more
     * method invocations than those in the wanted method.
     *
     * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MethodInvocation)
     */
    @Override
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
    @Override
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
     * @param node node to visit
	 * @return whether children should be visited
     *
     * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MethodInvocation)
     */
    @Override
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
    @Override
	public boolean visit(AnonymousClassDeclaration node) {
        return isNodeEnclosingMethod(node);
    }


    /**
	 * Adds the specified method binding to the search results.
	 *
	 * @param calledMethodBinding the called method binding
	 * @param node the AST node
	 */
    protected void addMethodCall(IMethodBinding calledMethodBinding, ASTNode node) {
        try {
            if (calledMethodBinding != null) {
                fProgressMonitor.worked(1);

                ITypeBinding calledTypeBinding = calledMethodBinding.getDeclaringClass();
                IType calledType = null;

                if (!calledTypeBinding.isAnonymous()) {
                    calledType = (IType) calledTypeBinding.getJavaElement();
                } else {
                    if (!"java.lang.Object".equals(calledTypeBinding.getSuperclass().getQualifiedName())) { //$NON-NLS-1$
                        calledType= (IType) calledTypeBinding.getSuperclass().getJavaElement();
                    } else {
                        calledType = (IType) calledTypeBinding.getInterfaces()[0].getJavaElement();
                    }
                }

                IMethod calledMethod = findIncludingSupertypes(calledMethodBinding,
                        calledType, fProgressMonitor);

                List<IMember> referencedMembers= new ArrayList<>();
                boolean implementationResults = false;
                if (calledMethod == null) {
                    if (calledMethodBinding.isConstructor() && calledMethodBinding.getParameterTypes().length == 0) {
                    	referencedMembers.add(calledType);
                    }
                } else {
                	// only find implementations if we are handling a method declaration.
                    if (node instanceof MethodDeclaration && (calledType.isInterface() || Flags.isAbstract(calledType.getFlags()))) {
                        Collection<IJavaElement> implementingMethods= CallHierarchyCore.getDefault().getImplementingMethods(calledMethod);
                        implementationResults = true;
                        for (IJavaElement element : implementingMethods) {
							if(element instanceof IMethod) {
								if(!isIgnoredBySearchScope((IMethod) element)) {
									referencedMembers.add((IMethod) element);
								}
							} else if(element instanceof IMember) {
								referencedMembers.add((IMember) element);
							}
						}
                    } else {
                    	referencedMembers.add(calledMethod);
                    }
                }

                Optional<CallLocation> calledAt= Optional.ofNullable(fCalledAt);
                Integer ignore = Integer.valueOf(-1);
				final int position= implementationResults ? calledAt.map(CallLocation::getStart).orElse(ignore).intValue() : node.getStartPosition();
				final int number= implementationResults ? calledAt.map(CallLocation::getLineNumber).orElse(ignore).intValue() : fCompilationUnit.getLineNumber(position);
				final int length = implementationResults ? calledAt.map(c -> Integer.valueOf(c.getEnd() - position)).orElse(ignore).intValue() : node.getLength();
				final IMember member = implementationResults ? calledAt.map(CallLocation::getMember).orElse(fMember) : fMember;
				final boolean potential = implementationResults;
				referencedMembers.forEach(m -> {
					fSearchResults.addMember(member, m, position, position + length, number < 1 ? 1 : number, potential);
				});
            }
        } catch (JavaModelException jme) {
            JavaManipulationPlugin.log(jme);
        }
    }

    private static IMethod findIncludingSupertypes(IMethodBinding method, IType type, IProgressMonitor pm) throws JavaModelException {
		IMethod inThisType= Bindings.findMethod(method, type);
		if (inThisType != null)
			return inThisType;
		for (IType superType : JavaModelUtil.getAllSuperTypes(type, pm)) {
			IMethod m= Bindings.findMethod(method, superType);
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
        return CallHierarchyCore.getDefault().getSearchScope();
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

    private void progressMonitorWorked(int work) {
        if (fProgressMonitor != null) {
            fProgressMonitor.worked(work);
            if (fProgressMonitor.isCanceled()) {
                throw new OperationCanceledException();
            }
        }
    }
}
