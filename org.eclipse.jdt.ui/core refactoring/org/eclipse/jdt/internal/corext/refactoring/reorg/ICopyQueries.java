package org.eclipse.jdt.internal.corext.refactoring.reorg;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;

public interface ICopyQueries {
	public	INewNameQuery createNewCompilationUnitNameQuery(ICompilationUnit cu);
	public	INewNameQuery createNewResourceNameQuery(IResource res);
	public	INewNameQuery createNewPackageNameQuery(IPackageFragment pack);
	public	INewNameQuery createNullQuery();
	public	INewNameQuery createStaticQuery(String newName);
}
