
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.changes.CreatePackageChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.MoveCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameCompilationUnitChange;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.javaeditor.ProblemPosition;

public class ReorgEvaluator {
	
	public static void getWrongTypeNameProposals(ICompilationUnit cu, ProblemPosition problemPos, ArrayList proposals) throws CoreException {
		IProblem problem= problemPos.getProblem();
		String[] args= problem.getArguments();
		if (args.length == 2) {
			// rename type
			Path path= new Path(args[0]);
			String newName= path.removeFileExtension().lastSegment();
			String label= "Rename type to '" + newName + "'";
			proposals.add(new ReplaceCorrectionProposal(cu, problemPos, label, newName));
			
			String newCUName= args[1] + ".java";
			final RenameCompilationUnitChange change= new RenameCompilationUnitChange(cu, newCUName);
			label= "Rename complation unit to '" + newCUName + "'";
			// rename cu
			proposals.add(new ChangeCorrectionProposal(label, problemPos) {
				protected Change getChange() throws CoreException {
					return change;
				}
			});
		}
	}
	
	public static void getWrongPackageDeclNameProposals(ICompilationUnit cu, ProblemPosition problemPos, ArrayList proposals) throws CoreException {
		IProblem problem= problemPos.getProblem();
		String[] args= problem.getArguments();
		if (args.length == 1) {
			// rename pack decl
			String newName= args[0];
			String label= "Rename to '" + newName + "'";
			proposals.add(new ReplaceCorrectionProposal(cu, problemPos, label, newName));
			
			// move to pack
			IPackageDeclaration[] packDecls= cu.getPackageDeclarations();
			String newPack= packDecls.length > 0 ? packDecls[0].getElementName() : "";
						
			IPackageFragmentRoot root= JavaModelUtil.getPackageFragmentRoot(cu);
			IPackageFragment pack= root.getPackageFragment(newPack);
			
			final CompositeChange composite= new CompositeChange();
			composite.add(new CreatePackageChange(pack));
			composite.add(new MoveCompilationUnitChange(cu, pack));
			
			label= "Move to package '" + newPack + "'";
			// rename cu
			proposals.add(new ChangeCorrectionProposal(label, problemPos) {
				protected Change getChange() throws CoreException {
					return composite;
				}
			});
		}
	}	
	

}
