package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.changes.MoveCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameCompilationUnitChange;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.ProblemPosition;

public class JavaCorrectionProcessor implements IContentAssistProcessor {

	private CompilationUnitEditor fCompilationUnitEditor;

	/**
	 * Constructor for JavaCorrectionProcessor.
	 */
	public JavaCorrectionProcessor(CompilationUnitEditor editor) {
		fCompilationUnitEditor= editor;
	}

	/*
	 * @see IContentAssistProcessor#computeCompletionProposals(ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
		List problemPositions= fCompilationUnitEditor.getProblemPositions();
		if (problemPositions == null)
			return null;
			
		for (Iterator e = problemPositions.iterator(); e.hasNext();) {
			ProblemPosition pp = (ProblemPosition) e.next();
			if (pp.overlapsWith(documentOffset, 1)) {
				return getCorrections(pp);
			}
		}

		// to do
		return null;
	}
	
	private ICompletionProposal[] getCorrections(ProblemPosition problemPos) {

		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
		ICompilationUnit cu= manager.getWorkingCopy(fCompilationUnitEditor.getEditorInput());

		ArrayList proposals= new ArrayList();
					
		try {
			int id= problemPos.getProblem().getID();
			switch (id) {
				case IProblem.UnterminatedString:
					proposals.add(new InsertCharacterCorrectionProposal(cu, problemPos, "Insert missing quote", "\"", false));
					break;
				case IProblem.UnterminatedComment:
					proposals.add(new InsertCharacterCorrectionProposal(cu, problemPos, "Terminate Comment", "*/", false));
					break;
				case IProblem.UndefinedMethod:
					UnknownMethodEvaluator.getProposals(cu, problemPos, proposals);
					break;
				case IProblem.PublicClassMustMatchFileName:
					handleWrongTypeName(cu, problemPos, proposals);
					break;
				case IProblem.PackageIsNotExpectedPackage:
					handleWrongPackageDeclName(cu, problemPos, proposals);
					break;					
				default:
					proposals.add(new NoCorrectionProposal(problemPos));
			}
		} catch(CoreException e) {
			JavaPlugin.log(e);
		}
		return (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);

	}
	
	private void handleWrongTypeName(ICompilationUnit cu, ProblemPosition problemPos, ArrayList proposals) throws CoreException {
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
	
	private void handleWrongPackageDeclName(ICompilationUnit cu, ProblemPosition problemPos, ArrayList proposals) throws CoreException {
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
			final MoveCompilationUnitChange change= new MoveCompilationUnitChange(cu, root.getPackageFragment(newPack));
			label= "Move to package '" + newPack + "'";
			// rename cu
			proposals.add(new ChangeCorrectionProposal(label, problemPos) {
				protected Change getChange() throws CoreException {
					return change;
				}
			});			
			
		}
	}	

	/*
	 * @see IContentAssistProcessor#computeContextInformation(ITextViewer, int)
	 */
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
		return null;
	}

	/*
	 * @see IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
	 */
	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	/*
	 * @see IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	/*
	 * @see IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

	/*
	 * @see IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		return null;
	}

}