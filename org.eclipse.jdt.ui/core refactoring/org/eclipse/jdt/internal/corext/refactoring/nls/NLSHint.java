/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.nls;

import java.util.List;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ASTCreator;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;

/**
 * calculates hints for the nls-refactoring out of a compilation unit.
 * - package fragments of the accessor class and the resource bundle
 * - accessor class name, resource bundle name
 */
public class NLSHint {

    private String fMessageClass;

    private IPackageFragment fPackage;

    private String fResourceBundle;

    private IPackageFragment fResourceBundlePackage;

    public NLSHint(NLSLine[] nlsLines, ICompilationUnit cu) {
        IPackageFragment defaultPackage = (IPackageFragment) cu.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
        fPackage = defaultPackage;
        fResourceBundlePackage = defaultPackage;
        
        findMessageClassHint(nlsLines, cu);
        findPropertyHint(cu);
    }

    public String getMessageClass() {
        return fMessageClass;
    }

    public IPackageFragment getMessageClassPackage() {
        return fPackage;
    }

    public String getResourceBundle() {
        return fResourceBundle;
    }

    public IPackageFragment getResourceBundlePackage() {
        return fResourceBundlePackage;
    }

    private void findPropertyHint(ICompilationUnit cu) {
        if (fPackage != null) {
            try {
                IJavaProject javaProject = cu.getJavaProject();
                IType messageClass = javaProject.findType(fPackage.getElementName() + '.' + fMessageClass); 
                if (messageClass != null) {
                    ASTNode ast = ASTCreator.createAST(messageClass.getCompilationUnit(), null);
                    ResourceBundleNameFinder resourceBundleNameFinder = new ResourceBundleNameFinder(NLSRefactoring.BUNDLE_NAME);
                    ast.accept(resourceBundleNameFinder);
                    String resourceBundleName = resourceBundleNameFinder.getResourceBundleName() + NLSRefactoring.PROPERTY_FILE_EXT;
                    fResourceBundle = getResourceNameHelper(resourceBundleName);
                    fResourceBundlePackage = findPackageFragmentOfResource(javaProject, resourceBundleName);
                }
            } catch (JavaModelException e) {
                // ...no hint possible
            }
        }
    }

    private class ResourceBundleNameFinder extends ASTVisitor {

        private String fResourceBundleName;
        private String fFieldName;

        public ResourceBundleNameFinder(String fieldName) {
            fResourceBundleName = null;            
            fFieldName = fieldName;
        }

        public boolean visit(FieldDeclaration node) {
            List fragments = node.fragments();
            if (fragments.size() == 1) {
                VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) fragments.get(0);
                String id = variableDeclarationFragment.getName().getIdentifier();
                if (id.equals(fFieldName)) {
                    Expression initializer = variableDeclarationFragment.getInitializer();                    
                    if (initializer != null) {
                        if(initializer instanceof StringLiteral) {
                            fResourceBundleName = ((StringLiteral) initializer).getLiteralValue();
                        } else if (initializer instanceof MethodInvocation) {
                            MethodInvocation methInvocation = (MethodInvocation) initializer;
                            Expression exp = methInvocation.getExpression();
                            if ((exp != null) && (exp instanceof TypeLiteral)) {                                
                                SimpleType simple = (SimpleType) ((TypeLiteral) exp).getType();
                                ITypeBinding typeBinding = simple.resolveBinding();
                                fResourceBundleName = typeBinding.getQualifiedName();                                                                
                            }                                                        
                        }
                    }
                }
            }
            return true;
        }

        public String getResourceBundleName() {
            return fResourceBundleName;
        }
    }
    
    private IPackageFragment findPackageFragmentOfResource(IJavaProject javaProject, String fullyQualifiedResource) throws JavaModelException {
        String packageString = getPackagePartHelper(fullyQualifiedResource);
        String resourceName = getResourceNameHelper(fullyQualifiedResource);
        IPackageFragmentRoot[] allRoots = javaProject.getAllPackageFragmentRoots();
        for (int i = 0; i < allRoots.length; i++) {
            IPackageFragmentRoot root = allRoots[i];
            if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
                IPackageFragment packageFragment = root.getPackageFragment(packageString);
                if (packageFragment.exists()) {
                    Object[] resources = packageFragment.getNonJavaResources();
                    for (int j = 0; j < resources.length; j++) {
                        Object object = resources[j];
                        if (object instanceof IFile) {
                            IFile file = (IFile) object;
                            if (file.getName().equals(resourceName)) { 
                                return packageFragment;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private String getResourceNameHelper(String fullyQualifiedResource) {
        int propertyDot = fullyQualifiedResource.lastIndexOf('.');
        int lastPackageDot = fullyQualifiedResource.lastIndexOf('.', propertyDot - 1);
        if (lastPackageDot == -1) {
            return fullyQualifiedResource;
        } else {
            return fullyQualifiedResource.substring(lastPackageDot + 1);
        }
    }

    private String getPackagePartHelper(String fullyQualifiedResource) {
        int propertyDot = fullyQualifiedResource.lastIndexOf('.');
        int lastPackageDot = fullyQualifiedResource.lastIndexOf('.', propertyDot - 1);
        if (lastPackageDot == -1) {
            return ""; //$NON-NLS-1$
        } else {
            return fullyQualifiedResource.substring(0, lastPackageDot);
        }
    }
    
	private void findMessageClassHint(NLSLine lines[], ICompilationUnit cu) {
	    ASTNode ast = ASTCreator.createAST(cu, null);
	    for (int i = 0; i < lines.length; i++) {
	        NLSLine nlsLine = lines[i];
	        NLSElement nlsElements[] = nlsLine.getElements();
	        for (int j = 0; j < nlsElements.length; j++) {
	            NLSElement nlsElement = nlsElements[j];
	            if ((nlsElement != null) && nlsElement.hasTag()) {	              
	                ITypeBinding binding = getAccessorClass(nlsElement, ast);
	                if (binding != null) {
		                fMessageClass = binding.getName();
		                try {
		                    fPackage = getPackageOfAccessorClass(binding, cu);
		                } catch (JavaModelException e) {
		                      // nothing todo...no hint
		                }
		                return; // we only need one hint
	                }
	            }
	        }
		}
	}
    
    private ITypeBinding getAccessorClass(NLSElement nlsElement, ASTNode ast) {
        TextRegion region = nlsElement.getPosition();
        ASTNode node = NodeFinder.perform(ast, region.getOffset(), region.getLength());
        if ((node != null) && (node.getParent() instanceof MethodInvocation)) {        	
            MethodInvocation methodInvocation = (MethodInvocation) node.getParent();
            if (methodInvocation.arguments().indexOf(node) == 0) {
	            IMethodBinding binding = methodInvocation.resolveMethodBinding();
	            if (binding != null && Modifier.isStatic(binding.getModifiers())) {
	                return binding.getDeclaringClass();
	            }
            }
        }
        return null;
    }
    
    private IPackageFragment getPackageOfAccessorClass(ITypeBinding accessorBinding, ICompilationUnit cu) throws JavaModelException {
        return (IPackageFragment) Bindings.findCompilationUnit(accessorBinding, cu.getJavaProject()).getParent();
    }
}
