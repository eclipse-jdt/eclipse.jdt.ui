/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.nls;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;

public class NLSHintHelper {

	private NLSHintHelper() {
	}

	/**
	 * Returns the accessor binding info or <code>null</code> if this element is not a nls'ed entry
	 */
	public static AccessorClassReference getAccessorClassReference(CompilationUnit astRoot, NLSElement nlsElement) {
		IRegion region= nlsElement.getPosition();
		return getAccessorClassReference(astRoot, region);
	}
	
	/**
	 * Returns the accessor binding info or <code>null</code> if this element is not a nls'ed entry
	 */
	public static AccessorClassReference getAccessorClassReference(CompilationUnit astRoot, IRegion region) {
		ASTNode nlsStringLiteral= NodeFinder.perform(astRoot, region.getOffset(), region.getLength());
		if (nlsStringLiteral == null) {
			return null; // not found
		}
		ASTNode parent= nlsStringLiteral.getParent();
		if (!(parent instanceof MethodInvocation)) {
			return null;
		}
		
		MethodInvocation methodInvocation= (MethodInvocation) parent;
		List args= methodInvocation.arguments();
		if (args.indexOf(nlsStringLiteral) != 0) {
			return null; // must be first argument in lookup method
		}
			
		IMethodBinding methodBinding= methodInvocation.resolveMethodBinding();
		if (methodBinding == null || !Modifier.isStatic(methodBinding.getModifiers())) {
			return null; // only static methods qualify
		}

		ITypeBinding accessorBinding= methodBinding.getDeclaringClass();
		if (isAccessorCandidate(accessorBinding)) {
			return new AccessorClassReference(accessorBinding, new Region(parent.getStartPosition(), parent.getLength()));
		}
		return null;
	}

	private static boolean isAccessorCandidate(ITypeBinding binding) {
		IVariableBinding[] fields= binding.getDeclaredFields();
		for (int i= 0; i < fields.length; i++) {
			String name= fields[i].getName();
			if (name.equals("BUNDLE_NAME") || name.equals("RESOURCE_BUNDLE")) { //$NON-NLS-1$ //$NON-NLS-2$
				return true;
			}
		}
		return false;
	}

	public static IPackageFragment getPackageOfAccessorClass(IJavaProject javaProject, ITypeBinding accessorBinding) throws JavaModelException {
		if (accessorBinding != null) {
			ICompilationUnit unit= Bindings.findCompilationUnit(accessorBinding, javaProject);
			if (unit != null) {
				return (IPackageFragment) unit.getParent();
			}
		}
		return null;
	}

	public static String getResourceBundleName(IJavaProject javaProject, ITypeBinding accessorClassBinding) throws JavaModelException {
		ICompilationUnit unit= Bindings.findCompilationUnit(accessorClassBinding, javaProject);
		if (unit == null) {
			return null;
		}
		ASTParser parser= ASTParser.newParser(AST.JLS3);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		parser.setFocalPosition(0);
		
		CompilationUnit astRoot= (CompilationUnit) parser.createAST(null);
		
		IVariableBinding[] fields= accessorClassBinding.getDeclaredFields();
		for (int i= 0; i < fields.length; i++) {
			String name= fields[i].getName();
			if (name.equals("BUNDLE_NAME") || name.equals("RESOURCE_BUNDLE")) { //$NON-NLS-1$ //$NON-NLS-2$
				VariableDeclarationFragment node= (VariableDeclarationFragment) astRoot.findDeclaringNode(fields[i].getKey());
				if (node != null) {
					Expression initializer= node.getInitializer();
					if (initializer instanceof StringLiteral) {
						return ((StringLiteral) initializer).getLiteralValue();
					} else if (initializer instanceof MethodInvocation) {
						MethodInvocation methInvocation= (MethodInvocation) initializer;
						Expression exp= methInvocation.getExpression();
						if ((exp != null) && (exp instanceof TypeLiteral)) {
							SimpleType simple= (SimpleType) ((TypeLiteral) exp).getType();
							ITypeBinding typeBinding= simple.resolveBinding();
							if (typeBinding != null) {
								return typeBinding.getQualifiedName();
							}
						}
					}
				}
			}
		}
		return null;
	}

	public static IPackageFragment getResourceBundlePackage(IJavaProject javaProject, String packageName, String resourceName) throws JavaModelException {
		IPackageFragmentRoot[] allRoots= javaProject.getAllPackageFragmentRoots();
		for (int i= 0; i < allRoots.length; i++) {
			IPackageFragmentRoot root= allRoots[i];
			if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
				IPackageFragment packageFragment= root.getPackageFragment(packageName);
				if (packageFragment.exists()) {
					Object[] resources= packageFragment.isDefaultPackage() ? root.getNonJavaResources() : packageFragment.getNonJavaResources();
					for (int j= 0; j < resources.length; j++) {
						Object object= resources[j];
						if (object instanceof IFile) {
							IFile file= (IFile) object;
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

	private static IFile getResourceBundleFile(IJavaProject javaProject, ITypeBinding accessorClass) throws JavaModelException {
		String resourceBundle= getResourceBundleName(javaProject, accessorClass);
		if (resourceBundle == null) {
			return null;
		}
		String resourceName= Signature.getSimpleName(resourceBundle) + NLSRefactoring.PROPERTY_FILE_EXT;
		String packName= Signature.getQualifier(resourceBundle);
		
		IPackageFragment bundlePack= getResourceBundlePackage(javaProject, packName, resourceName);
		if (bundlePack == null) {
			return null;
		}
		
		IPath path= bundlePack.getPath().append(resourceName);
		IResource res= ResourcesPlugin.getWorkspace().getRoot().findMember(path);
		if (res instanceof IFile) {
			return (IFile) res;
		}
		return null;
	}
	
	public static Properties getProperties(IJavaProject project, ITypeBinding accessorBinding) {
		Properties props= new Properties();
		try {
			IFile file= NLSHintHelper.getResourceBundleFile(project, accessorBinding);
			if (file != null) {
				InputStream is= file.getContents();
				props.load(is);
				is.close();
			}
		} catch (IOException e) {
			// sorry no property         
		} catch (CoreException e) {
			// sorry no property         
		}
		return props;
	}


}
