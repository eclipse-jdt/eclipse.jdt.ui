package org.eclipse.jdt.internal.corext.refactoring.structure;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Binding2JavaModel;
import org.eclipse.jdt.internal.corext.refactoring.util.JdtFlags;

class VisibilityChecker {
	private VisibilityChecker() {
	}
	
	static boolean isVisibleFrom(IMember member, ASTNode accessingNode, ICompilationUnit nodeCu) throws JavaModelException{
		if (member.getDeclaringType() != null && ! isVisibleFrom(member.getDeclaringType(), accessingNode, nodeCu))
			return false;
		if (JdtFlags.isPublic(member))
			return true;
		boolean samePackage= isInSamePackage(member, nodeCu);	
		if (JdtFlags.isProtected(member))
			return samePackage || isInSubtype(member, accessingNode, nodeCu.getJavaProject());
		if (JdtFlags.isPackageVisible(member))	
			return samePackage;
		return isInSameType(member, accessingNode, nodeCu.getJavaProject());	
	}
	
	private static boolean isInSamePackage(IMember member, ICompilationUnit nodeCu){
		return getPackage(member).equals(getPackage(nodeCu));
	}
	
	private static IPackageFragment getPackage(IMember member){
		return (IPackageFragment)member.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
	}
	
	private static IPackageFragment getPackage(ICompilationUnit cu){
		return (IPackageFragment)cu.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
	}
	
	private static boolean isInSameType(IMember member, ASTNode node, IJavaProject project) throws JavaModelException{
		IType memberType= member.getDeclaringType();
		IType nodeType= getDeclaringType(node, project);
		if (memberType == null || nodeType == null)
			return false; 
		do{
			if (memberType.equals(nodeType))
				return true;
			memberType= memberType.getDeclaringType();
		} while (memberType != null);
		return false; 
	}
	
	private static IType getDeclaringType(ASTNode node, IJavaProject project) throws JavaModelException{
		ITypeBinding tb= getDeclaringTypeBinding(node);
		if (tb == null)
			return null;
		return Binding2JavaModel.find(tb, project);
	}
	
	private static ITypeBinding getDeclaringTypeBinding(ASTNode node){
		TypeDeclaration td= (TypeDeclaration)ASTNodes.getParent(node, TypeDeclaration.class);
		if (td == null)
			return null;
		return td.resolveBinding();
	}
	private static boolean isInSubtype(IMember member, ASTNode node, IJavaProject project) throws JavaModelException{
		IType memberType= member.getDeclaringType();
		ITypeBinding tb= getDeclaringTypeBinding(node);
		
		while(tb != null){
			if (memberType.equals(Binding2JavaModel.find(tb, project)))
				return true;
			tb= tb.getSuperclass();
		}
		return false;
	}
}
