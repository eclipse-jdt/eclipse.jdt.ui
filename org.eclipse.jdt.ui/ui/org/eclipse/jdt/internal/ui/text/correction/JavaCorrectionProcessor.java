/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.IProblemAnnotation;
import org.eclipse.jdt.internal.ui.javaeditor.ProblemAnnotationIterator;
import org.eclipse.jdt.internal.ui.text.java.IJavaCompletionProposal;


public class JavaCorrectionProcessor implements IContentAssistProcessor {

	public static boolean hasCorrections(int problemId) {
		switch (problemId) {
			case IProblem.UnterminatedString:
			case IProblem.UnusedImport:
			case IProblem.DuplicateImport:
			case IProblem.CannotImportPackage:
			case IProblem.ConflictingImport:			
			case IProblem.UndefinedMethod:
			case IProblem.UndefinedConstructor:
			case IProblem.ParameterMismatch:
			case IProblem.MethodButWithConstructorName:
			case IProblem.UndefinedField:
			case IProblem.UndefinedName:
			case IProblem.PublicClassMustMatchFileName:
			case IProblem.PackageIsNotExpectedPackage:
			case IProblem.UndefinedType:
			case IProblem.FieldTypeNotFound:
			case IProblem.ArgumentTypeNotFound:
			case IProblem.ReturnTypeNotFound:
			case IProblem.SuperclassNotFound:
			case IProblem.ExceptionTypeNotFound:
			case IProblem.InterfaceNotFound: 
			case IProblem.TypeMismatch:
			case IProblem.UnhandledException:
			case IProblem.VoidMethodReturnsValue:
			case IProblem.ShouldReturnValue:
			case IProblem.MissingReturnType:
			case IProblem.NonExternalizedStringLiteral:
			case IProblem.NonStaticAccessToStaticField:
			case IProblem.NonStaticAccessToStaticMethod:
			case IProblem.StaticMethodRequested:
			case IProblem.NonStaticFieldFromStaticInvocation:
			case IProblem.InstanceMethodDuringConstructorInvocation:
			case IProblem.InstanceFieldDuringConstructorInvocation:			
			case IProblem.NotVisibleMethod:
			case IProblem.NotVisibleConstructor:
			case IProblem.NotVisibleType:
			case IProblem.SuperclassNotVisible:
			case IProblem.InterfaceNotVisible:
			case IProblem.FieldTypeNotVisible:
			case IProblem.ArgumentTypeNotVisible:
			case IProblem.ReturnTypeNotVisible:
			case IProblem.ExceptionTypeNotVisible:
			case IProblem.NotVisibleField:
			case IProblem.ImportNotVisible:		
				return true;
			default:
				return false;
		}
	}
	
	private static class CorrectionsComparator implements Comparator {
		
		private static Collator fgCollator= Collator.getInstance();
		
		public int compare(Object o1, Object o2) {
			IJavaCompletionProposal e1= (IJavaCompletionProposal) o1;
			IJavaCompletionProposal e2= (IJavaCompletionProposal) o2;
			int del= e2.getRelevance() - e1.getRelevance();
			if (del != 0) {
				return del;
			}
			return fgCollator.compare(e1.getDisplayString(), e2.getDisplayString());
		}
	}


	private IEditorPart fEditor;

	/**
	 * Constructor for JavaCorrectionProcessor.
	 */
	public JavaCorrectionProcessor(IEditorPart editor) {
		fEditor= editor;
	}

	/*
	 * @see IContentAssistProcessor#computeCompletionProposals(ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
		
		ICompilationUnit cu= JavaUI.getWorkingCopyManager().getWorkingCopy(fEditor.getEditorInput());
		IAnnotationModel model= JavaUI.getDocumentProvider().getAnnotationModel(fEditor.getEditorInput());

		ArrayList proposals= new ArrayList();
		if (model != null) {
			processProblemAnnotations(cu, model, documentOffset, proposals);
		}
		
		if (proposals.isEmpty()) {
			proposals.add(new NoCorrectionProposal(null, null));
		}
		IJavaCompletionProposal[] res= (IJavaCompletionProposal[]) proposals.toArray(new IJavaCompletionProposal[proposals.size()]);
		Arrays.sort(res, new CorrectionsComparator());
		return res;
	}
	
	private boolean isAtPosition(int offset, Position pos) {
		return (pos != null) && (offset >= pos.getOffset() && offset <= (pos.getOffset() +  pos.getLength()));
	}
	

	private void processProblemAnnotations(ICompilationUnit cu, IAnnotationModel model, int documentOffset, ArrayList proposals) {
		HashSet idsProcessed= new HashSet();
		Iterator iter= new ProblemAnnotationIterator(model, true);
		while (iter.hasNext()) {
			IProblemAnnotation annot= (IProblemAnnotation) iter.next();
			Position pos= model.getPosition((Annotation) annot);
			if (isAtPosition(documentOffset, pos)) {
				if (annot.isProblem()) {
					Integer probId= new Integer(annot.getId());
					if (!idsProcessed.contains(probId)) {
						ProblemPosition pp = new ProblemPosition(pos, annot, cu);
						idsProcessed.add(probId);
						collectCorrections(pp, proposals);
						if (proposals.isEmpty()) {
							//proposals.add(new NoCorrectionProposal(pp, annot.getMessage()));
						}
					}
				} else {
					if (annot instanceof MarkerAnnotation) {
						IMarker marker= ((MarkerAnnotation) annot).getMarker();
						IMarkerResolution[] res= PlatformUI.getWorkbench().getMarkerHelpRegistry().getResolutions(marker);
						for (int i= 0; i < res.length; i++) {
							proposals.add(new MarkerResolutionProposal(res[i], marker));
						}
					}
				}
			}
		}
	}
	
	public static void collectCorrections(ProblemPosition problemPos, ArrayList proposals) {
		try {
			int id= problemPos.getId();
			switch (id) {
				case IProblem.UnterminatedString:
					String quoteLabel= CorrectionMessages.getString("JavaCorrectionProcessor.addquote.description"); //$NON-NLS-1$
					int pos= InsertCorrectionProposal.moveBack(problemPos.getOffset() + problemPos.getLength(), problemPos.getOffset(), "\n\r", problemPos.getCompilationUnit()); //$NON-NLS-1$
					proposals.add(new InsertCorrectionProposal(quoteLabel, problemPos.getCompilationUnit(), pos, "\"", 0)); //$NON-NLS-1$ 
					break;
				case IProblem.UnusedImport:
				case IProblem.DuplicateImport:
				case IProblem.CannotImportPackage:
				case IProblem.ConflictingImport:
					ReorgCorrectionsSubProcessor.removeImportStatementProposals(problemPos, proposals);
					break;
				case IProblem.UndefinedMethod:
					UnresolvedElementsSubProcessor.getMethodProposals(problemPos, false, proposals);
					break;
				case IProblem.UndefinedConstructor:
					UnresolvedElementsSubProcessor.getConstructorProposals(problemPos, proposals);
					break;					
				case IProblem.ParameterMismatch:
					UnresolvedElementsSubProcessor.getMethodProposals(problemPos, true, proposals);
					break;
				case IProblem.MethodButWithConstructorName:	
					ReturnTypeSubProcessor.addMethodWithConstrNameProposals(problemPos, proposals);
					break;
				case IProblem.UndefinedField:
				case IProblem.UndefinedName:
					UnresolvedElementsSubProcessor.getVariableProposals(problemPos, proposals);
					break;					
				case IProblem.PublicClassMustMatchFileName:
					ReorgCorrectionsSubProcessor.getWrongTypeNameProposals(problemPos, proposals);
					break;
				case IProblem.PackageIsNotExpectedPackage:
					ReorgCorrectionsSubProcessor.getWrongPackageDeclNameProposals(problemPos, proposals);
					break;
				case IProblem.UndefinedType:
				case IProblem.FieldTypeNotFound:
				case IProblem.ArgumentTypeNotFound:
				case IProblem.ReturnTypeNotFound:
				case IProblem.SuperclassNotFound:
				case IProblem.ExceptionTypeNotFound:
				case IProblem.InterfaceNotFound: 
					UnresolvedElementsSubProcessor.getTypeProposals(problemPos, proposals);
					break;	
				case IProblem.TypeMismatch:
					LocalCorrectionsSubProcessor.addCastProposals(problemPos, proposals);
					break;
				case IProblem.UnhandledException:
					LocalCorrectionsSubProcessor.addUncaughtExceptionProposals(problemPos, proposals);
					break;
				case IProblem.VoidMethodReturnsValue:
					ReturnTypeSubProcessor.addVoidMethodReturnsProposals(problemPos, proposals);
					break;
				case IProblem.MissingReturnType:
					ReturnTypeSubProcessor.addMissingReturnTypeProposals(problemPos, proposals);
					break;
				case IProblem.ShouldReturnValue:
					ReturnTypeSubProcessor.addMissingReturnStatementProposals(problemPos, proposals);
					break;
				case IProblem.NonExternalizedStringLiteral:
					LocalCorrectionsSubProcessor.addNLSProposals(problemPos, proposals);
					break;
				case IProblem.NonStaticAccessToStaticField:
				case IProblem.NonStaticAccessToStaticMethod:
					LocalCorrectionsSubProcessor.addAccessToStaticProposals(problemPos, proposals);
					break;
				case IProblem.StaticMethodRequested:
				case IProblem.NonStaticFieldFromStaticInvocation:
				case IProblem.InstanceMethodDuringConstructorInvocation:
				case IProblem.InstanceFieldDuringConstructorInvocation:
					LocalCorrectionsSubProcessor.addNonAccessibleMemberProposal(problemPos, proposals, false); 
					break;				
				case IProblem.NotVisibleMethod:
				case IProblem.NotVisibleConstructor:
				case IProblem.NotVisibleType:
				case IProblem.SuperclassNotVisible:
				case IProblem.InterfaceNotVisible:
				case IProblem.FieldTypeNotVisible:
				case IProblem.ArgumentTypeNotVisible:
				case IProblem.ReturnTypeNotVisible:
				case IProblem.ExceptionTypeNotVisible:
				case IProblem.NotVisibleField:
				case IProblem.ImportNotVisible:
					LocalCorrectionsSubProcessor.addNonAccessibleMemberProposal(problemPos, proposals, true); 
					break;
				default:
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