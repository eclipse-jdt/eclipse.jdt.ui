package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.texteditor.IDocumentProvider;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.IProblemAnnotation;
import org.eclipse.jdt.internal.ui.javaeditor.ProblemAnnotationIterator;


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
		
		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
		ICompilationUnit cu= manager.getWorkingCopy(fCompilationUnitEditor.getEditorInput());
		
		IDocumentProvider provider= JavaPlugin.getDefault().getCompilationUnitDocumentProvider();
		IAnnotationModel model= provider.getAnnotationModel(fCompilationUnitEditor.getEditorInput());

		ArrayList proposals= new ArrayList();
		HashSet idsProcessed= new HashSet();
			
		for (Iterator iter= new ProblemAnnotationIterator(model); iter.hasNext();) {
			IProblemAnnotation annot= (IProblemAnnotation) iter.next();
			Position pos= model.getPosition((Annotation) annot);
			
			ProblemPosition pp = new ProblemPosition(pos, annot, cu);
			if (pp.overlapsWith(documentOffset, 1)) {
				Integer probId= new Integer(annot.getId());
				if (!idsProcessed.contains(probId)) {
					idsProcessed.add(probId);
					collectCorrections(pp, proposals);
				}
			}
		}
		if (proposals.isEmpty()) {
			return null;
		}
		return (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);
	}
	
	private void collectCorrections(ProblemPosition problemPos, ArrayList proposals) {
		try {
			int id= problemPos.getId();
			switch (id) {
				case IProblem.UnterminatedString:
					proposals.add(new InsertCharacterCorrectionProposal(problemPos, "Insert missing quote", "\"", false));
					break;
				case IProblem.UnterminatedComment:
					proposals.add(new InsertCharacterCorrectionProposal(problemPos, "Terminate Comment", "*/", false));
					break;
				case IProblem.UndefinedMethod:
					UnknownMethodEvaluator.getProposals(problemPos, proposals);
					break;
				case IProblem.UndefinedName:
				case IProblem.UndefinedField:
					UnknownVariableEvaluator.getProposals(problemPos, proposals);
					break;					
				case IProblem.PublicClassMustMatchFileName:
					ReorgEvaluator.getWrongTypeNameProposals(problemPos, proposals);
					break;
				case IProblem.PackageIsNotExpectedPackage:
					ReorgEvaluator.getWrongPackageDeclNameProposals(problemPos, proposals);
					break;
				case IProblem.UndefinedType:
				case IProblem.FieldTypeNotFound:
				case IProblem.ArgumentTypeNotFound:
				case IProblem.ReturnTypeNotFound:
					UnknownTypeEvaluator.getTypeProposals(problemPos, UnknownTypeEvaluator.TYPE, proposals);
					break;
				case IProblem.SuperclassNotFound:
				case IProblem.ExceptionTypeNotFound:
					UnknownTypeEvaluator.getTypeProposals(problemPos, UnknownTypeEvaluator.CLASS, proposals);
					break;				
				case IProblem.InterfaceNotFound: 
					UnknownTypeEvaluator.getTypeProposals(problemPos, UnknownTypeEvaluator.INTERFACE, proposals);
					break;	
				default:
					proposals.add(new NoCorrectionProposal(problemPos));
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
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