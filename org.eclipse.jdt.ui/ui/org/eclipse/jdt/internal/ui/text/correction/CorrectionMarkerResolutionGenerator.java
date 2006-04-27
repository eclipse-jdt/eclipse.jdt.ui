/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.dialogs.ErrorDialog;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerUtilities;

import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.CorrectionEngine;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.MultiStateCompilationUnitChange;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.CompletionProposalComparator;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.fix.ICleanUp;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaMarkerAnnotation;

/**
  */
public class CorrectionMarkerResolutionGenerator implements IMarkerResolutionGenerator, IMarkerResolutionGenerator2 {

	public static class CorrectionMarkerResolution extends WorkbenchMarkerResolution {

		private static final IMarker[] NO_MARKERS= new IMarker[0];

		private ICompilationUnit fCompilationUnit;
		private int fOffset;
		private int fLength;
		private IJavaCompletionProposal fProposal;
		private final IMarker fMarker;

		/**
		 * Constructor for CorrectionMarkerResolution.
		 * @param marker 
		 */
		public CorrectionMarkerResolution(ICompilationUnit cu, int offset, int length, IJavaCompletionProposal proposal, IMarker marker) {
			fCompilationUnit= cu;
			fOffset= offset;
			fLength= length;
			fProposal= proposal;
			fMarker= marker;
		}

		/* (non-Javadoc)
		 * @see IMarkerResolution#getLabel()
		 */
		public String getLabel() {
			return fProposal.getDisplayString();
		}

		/* (non-Javadoc)
		 * @see IMarkerResolution#run(IMarker)
		 */
		public void run(IMarker marker) {
			try {
				IEditorPart part= EditorUtility.isOpenInEditor(fCompilationUnit);
				if (part == null) {
					part= EditorUtility.openInEditor(fCompilationUnit);
					if (part instanceof ITextEditor) {
						((ITextEditor) part).selectAndReveal(fOffset, fLength);
					}
				}
				if (part != null) {
					IEditorInput input= part.getEditorInput();
					IDocument doc= JavaPlugin.getDefault().getCompilationUnitDocumentProvider().getDocument(input);					
					fProposal.apply(doc);
				}
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
		
		public void run(IMarker[] markers, IProgressMonitor monitor) {
			if (markers.length == 1) {
				run(markers[0]);
				return;
			}
			IProgressMonitor pm= monitor;
			if (pm == null)
				pm= new NullProgressMonitor();
			
			try {
				if (fProposal instanceof FixCorrectionProposal) {
					ICleanUp cleanUp= ((FixCorrectionProposal)fProposal).getCleanUp();
					if (cleanUp != null) {
						Hashtable/*<ICompilationUnit, List<IProblemLocation>*/ problemLocations= new Hashtable();
						for (int i= 0; i < markers.length; i++) {
							IMarker marker= markers[i];
							ICompilationUnit cu= getCompilationUnit(marker);
							
							if (cu != null) {
								try {
									IEditorInput input= EditorUtility.getEditorInput(cu);
									IProblemLocation location= findProblemLocation(input, marker);
									if (location != null) {
										if (!problemLocations.containsKey(cu.getPrimary())) {
											problemLocations.put(cu.getPrimary(), new ArrayList());
										}
										List l= (List)problemLocations.get(cu.getPrimary());
										l.add(location);
									}
								} catch (JavaModelException e) {
									JavaPlugin.log(e);
								}
							}
						}
						if (problemLocations.size() > 0) {
									
							Set cus= problemLocations.keySet();
							Hashtable projects= new Hashtable();
							for (Iterator iter= cus.iterator(); iter.hasNext();) {
								ICompilationUnit cu= (ICompilationUnit)iter.next();
								IJavaProject project= cu.getJavaProject();
								if (!projects.containsKey(project)) {
									projects.put(project, new ArrayList());
								}
								((List)projects.get(project)).add(cu);
							}
							
							pm.beginTask("", problemLocations.size() * 2 + 2 + projects.keySet().size()); //$NON-NLS-1$
							
							String name= ""; //$NON-NLS-1$
							String[] descriptions= cleanUp.getDescriptions();
							if (descriptions != null && descriptions.length == 1) {
								name= descriptions[0];
							}
							CompositeChange allChanges= new CompositeChange(name);
							
							for (Iterator projectIter= projects.keySet().iterator(); projectIter.hasNext();) {
								IJavaProject project= (IJavaProject)projectIter.next();
								List compilationUnitsList= (List)projects.get(project);
								ICompilationUnit[] compilationUnits= (ICompilationUnit[])compilationUnitsList.toArray(new ICompilationUnit[compilationUnitsList.size()]);
								
								try {
									cleanUpProject(project, compilationUnits, cleanUp, problemLocations, allChanges, pm);
								} catch (CoreException e) {
									JavaPlugin.log(e);
								} finally {
									pm.worked(1);
								}
							}

							if (pm.isCanceled())
								return;
							
							allChanges.initializeValidationData(new SubProgressMonitor(pm, 1));
							
							if (!validChanges(allChanges))
								return;
							
							PerformChangeOperation op= new PerformChangeOperation(allChanges);
							op.setUndoManager(RefactoringCore.getUndoManager(), allChanges.getName());
							try {
								op.run(new SubProgressMonitor(pm, 1));
							} catch (CoreException e1) {
								JavaPlugin.log(e1);
							} finally {
								pm.worked(1);
							}
							IEditorPart part= EditorUtility.isOpenInEditor(fCompilationUnit);
							if (part instanceof ITextEditor) {
								((ITextEditor) part).selectAndReveal(fOffset, fLength);
								part.setFocus();
							}
						}
					}
				} 
			} finally {
				pm.done();
			}
		}

		private boolean validChanges(CompositeChange change) {
			RefactoringStatus result= new RefactoringStatus();
			List files= new ArrayList();
			try {
				findFilesToBeModified(change, files);
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
				return false;
			}
			result.merge(Checks.validateModifiesFiles((IFile[])files.toArray(new IFile[files.size()]), JavaPlugin.getActiveWorkbenchShell().getShell()));
			if (result.hasFatalError()) {
				RefactoringStatusEntry[] entries= result.getEntries();
				IStatus status;
				if (entries.length > 1) {
					status= new MultiStatus(JavaUI.ID_PLUGIN, 0, result.getMessageMatchingSeverity(RefactoringStatus.ERROR), null);
					for (int i= 0; i < entries.length; i++) {
						((MultiStatus)status).add(new Status(entries[i].getSeverity(), JavaUI.ID_PLUGIN, 0, entries[i].getMessage(), null));
					}
				} else {
					RefactoringStatusEntry entry= entries[0];
					status= new Status(entry.getSeverity(), JavaUI.ID_PLUGIN, 0, entry.getMessage(), null);
				}
				ErrorDialog.openError(JavaPlugin.getActiveWorkbenchShell().getShell(), CorrectionMessages.CorrectionMarkerResolutionGenerator__multiFixErrorDialog_Titel, CorrectionMessages.CorrectionMarkerResolutionGenerator_multiFixErrorDialog_description, status);
				return false;
			}
			return true;
		}
		
		private void findFilesToBeModified(CompositeChange change, List result) throws JavaModelException {
			Change[] children= change.getChildren();
			for (int i=0;i < children.length;i++) {
				Change child= children[i];
				if (child instanceof CompositeChange) {
					findFilesToBeModified((CompositeChange)child, result);
				} else if (child instanceof MultiStateCompilationUnitChange) {
					result.add(((MultiStateCompilationUnitChange)child).getCompilationUnit().getCorrespondingResource());
				} else if (child instanceof CompilationUnitChange) {
					result.add(((CompilationUnitChange)child).getCompilationUnit().getCorrespondingResource());
				}
			}
		}

		private void cleanUpProject(IJavaProject project, ICompilationUnit[] compilationUnits, ICleanUp cleanUp, Hashtable problemLocations, CompositeChange result, IProgressMonitor monitor) throws CoreException {
			cleanUp.beginCleanUp(project, compilationUnits, new SubProgressMonitor(monitor, 1));
			for (int i= 0; i < compilationUnits.length; i++) {
				ICompilationUnit cu= compilationUnits[i];
				CompilationUnit root= JavaPlugin.getDefault().getASTProvider().getAST(cu, ASTProvider.WAIT_YES, new SubProgressMonitor(monitor, 1));
				List locationList= (List)problemLocations.get(cu);
				IProblemLocation[] locations= (IProblemLocation[])locationList.toArray(new IProblemLocation[locationList.size()]);

				IFix fix= cleanUp.createFix(root, locations);
				
				if (monitor.isCanceled())
					return;
				
				if (fix != null) {
					TextChange change= fix.createChange();
					
					if (monitor.isCanceled())
						return;
					
					result.add(change);
					monitor.worked(1);
				}

			}
			cleanUp.endCleanUp();	
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.IMarkerResolution2#getDescription()
		 */
		public String getDescription() {
			return fProposal.getAdditionalProposalInfo();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.IMarkerResolution2#getImage()
		 */
		public Image getImage() {
			return fProposal.getImage();
		}
		
		/**
		 * {@inheritDoc}
		 */
		public IMarker[] findOtherMarkers(IMarker[] markers) {
			if (fProposal instanceof FixCorrectionProposal) {
				FixCorrectionProposal fix= (FixCorrectionProposal)fProposal;
				ICleanUp cleanUp= fix.getCleanUp();
				if (cleanUp != null) {
					List result= new ArrayList();
					for (int i= 0; i < markers.length; i++) {
						IMarker marker= markers[i];
						if (!marker.equals(fMarker)) {
							ICompilationUnit cu= getCompilationUnit(marker);
							if (cu != null) {
								try {
									IEditorInput input= EditorUtility.getEditorInput(cu);
									IProblemLocation location= findProblemLocation(input, marker);
									if (location != null) {
										IInvocationContext context= new AssistContext(cu,  location.getOffset(), location.getLength());
										CompilationUnit root= context.getASTRoot();
										if (cleanUp.canFix(root, location)) {
											result.add(marker);
										}
									}
								} catch (CoreException e) {
									JavaPlugin.log(e);
								}
							}
						}
					}
					return (IMarker[])result.toArray(new IMarker[result.size()]);
				}
			}
			return NO_MARKERS;
		}
	}

	private static final IMarkerResolution[] NO_RESOLUTIONS= new IMarkerResolution[0];


	/**
	 * Constructor for CorrectionMarkerResolutionGenerator.
	 */
	public CorrectionMarkerResolutionGenerator() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolutionGenerator2#hasResolutions(org.eclipse.core.resources.IMarker)
	 */
	public boolean hasResolutions(IMarker marker) {
		return internalHasResolutions(marker);
	}

	/* (non-Javadoc)
	 * @see IMarkerResolutionGenerator#getResolutions(IMarker)
	 */
	public IMarkerResolution[] getResolutions(IMarker marker) {
		return internalGetResolutions(marker);
	}
	
	private static boolean internalHasResolutions(IMarker marker) {
		int id= marker.getAttribute(IJavaModelMarker.ID, -1);
		ICompilationUnit cu= getCompilationUnit(marker);
		return cu != null && JavaCorrectionProcessor.hasCorrections(cu, id, MarkerUtilities.getMarkerType(marker));
	}
	
	private static IMarkerResolution[] internalGetResolutions(IMarker marker) {
		if (!internalHasResolutions(marker)) {
			return NO_RESOLUTIONS;
		}

		try {
			ICompilationUnit cu= getCompilationUnit(marker);
			if (cu != null) {
				IEditorInput input= EditorUtility.getEditorInput(cu);
				if (input != null) {
					IProblemLocation location= findProblemLocation(input, marker);
					if (location != null) {

						IInvocationContext context= new AssistContext(cu,  location.getOffset(), location.getLength());
						if (!hasProblem (context.getASTRoot().getProblems(), location)) 
							return NO_RESOLUTIONS;
						
						ArrayList proposals= new ArrayList();
						JavaCorrectionProcessor.collectCorrections(context, new IProblemLocation[] { location }, proposals);
						Collections.sort(proposals, new CompletionProposalComparator());

						int nProposals= proposals.size();
						IMarkerResolution[] resolutions= new IMarkerResolution[nProposals];
						for (int i= 0; i < nProposals; i++) {
							resolutions[i]= new CorrectionMarkerResolution(context.getCompilationUnit(), location.getOffset(), location.getLength(), (IJavaCompletionProposal) proposals.get(i), marker);
						}
						return resolutions;
					}
				}
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return NO_RESOLUTIONS;
	}

	private static boolean hasProblem(IProblem[] problems, IProblemLocation location) {
		for (int i= 0; i < problems.length; i++) {
			IProblem problem= problems[i];
			if (problem.getID() == location.getProblemId() && problem.getSourceStart() == location.getOffset())
				return true;
		}
		return false;
	}

	private static ICompilationUnit getCompilationUnit(IMarker marker) {
		IResource res= marker.getResource();
		if (res instanceof IFile && res.isAccessible()) {
			IJavaElement element= JavaCore.create((IFile) res);
			if (element instanceof ICompilationUnit)
				return (ICompilationUnit) element;
		}
		return null;
	}

	private static IProblemLocation findProblemLocation(IEditorInput input, IMarker marker) {
		IAnnotationModel model= JavaPlugin.getDefault().getCompilationUnitDocumentProvider().getAnnotationModel(input);
		if (model != null) { // open in editor
			Iterator iter= model.getAnnotationIterator();
			while (iter.hasNext()) {
				Object curr= iter.next();
				if (curr instanceof JavaMarkerAnnotation) {
					JavaMarkerAnnotation annot= (JavaMarkerAnnotation) curr;
					if (marker.equals(annot.getMarker())) {
						Position pos= model.getPosition(annot);
						if (pos != null) {
							return new ProblemLocation(pos.getOffset(), pos.getLength(), annot);
						}
					}
				}
			}
		} else { // not open in editor
			ICompilationUnit cu= getCompilationUnit(marker);
			return createFromMarker(marker, cu);
		}
		return null;
	}

	private static IProblemLocation createFromMarker(IMarker marker, ICompilationUnit cu) {
		try {
			int id= marker.getAttribute(IJavaModelMarker.ID, -1);
			int start= marker.getAttribute(IMarker.CHAR_START, -1);
			int end= marker.getAttribute(IMarker.CHAR_END, -1);
			int severity= marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
			String[] arguments= CorrectionEngine.getProblemArguments(marker);
			String markerType= marker.getType();
			if (cu != null && id != -1 && start != -1 && end != -1 && arguments != null) {
				boolean isError= (severity == IMarker.SEVERITY_ERROR);
				return new ProblemLocation(start, end - start, id, arguments, isError, markerType);
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
		return null;
	}
	

}
