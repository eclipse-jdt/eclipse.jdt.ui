/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import java.util.Map;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.eclipse.ui.IMarkerResolutionRelevance;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;

import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerUtilities;

import org.eclipse.jdt.core.CorrectionEngine;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring.MultiFixTarget;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jdt.ui.text.java.CompletionProposalComparator;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.fix.IMultiFix;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaMarkerAnnotation;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CreatePackageInfoWithDefaultNullnessProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;


public class CorrectionMarkerResolutionGenerator implements IMarkerResolutionGenerator2 {

	public static class CorrectionMarkerResolution extends WorkbenchMarkerResolution implements IMarkerResolutionRelevance {

		private static final IMarker[] NO_MARKERS= new IMarker[0];

		private ICompilationUnit fCompilationUnit;
		private int fOffset;
		private int fLength;
		private IJavaCompletionProposal fProposal;
		private final IMarker fMarker;

		/**
		 * Constructor for CorrectionMarkerResolution.
		 * @param cu the compilation unit
		 * @param offset the offset
		 * @param length the length
		 * @param proposal the proposal for the given marker
		 * @param marker the marker to fix
		 */
		public CorrectionMarkerResolution(ICompilationUnit cu, int offset, int length, IJavaCompletionProposal proposal, IMarker marker) {
			fCompilationUnit= cu;
			fOffset= offset;
			fLength= length;
			fProposal= proposal;
			fMarker= marker;
		}

		@Override
		public String getLabel() {
			return fProposal.getDisplayString();
		}

		@Override
		public void run(IMarker marker) {
			try {
				IEditorPart part= EditorUtility.isOpenInEditor(fCompilationUnit);
				if (part == null) {
					part= JavaUI.openInEditor(fCompilationUnit, true, false);
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


		@Override
		public void run(IMarker[] markers, IProgressMonitor monitor) {
			if (markers.length == 1) {
				run(markers[0]);
				return;
			}

			if (!(fProposal instanceof FixCorrectionProposal) && !(fProposal instanceof CreatePackageInfoWithDefaultNullnessProposal))
				return;

			if (monitor == null)
				monitor= new NullProgressMonitor();

			try {
				MultiFixTarget[] problems= getCleanUpTargets(markers);

				if (fProposal instanceof CreatePackageInfoWithDefaultNullnessProposal) {
					((CreatePackageInfoWithDefaultNullnessProposal) fProposal).resolve(problems, monitor);
					return;
				}

				((FixCorrectionProposal)fProposal).resolve(problems, monitor);

				IEditorPart part= EditorUtility.isOpenInEditor(fCompilationUnit);
				if (part instanceof ITextEditor) {
					((ITextEditor) part).selectAndReveal(fOffset, fLength);
					part.setFocus();
				}
			} catch (CoreException e) {
				JavaPlugin.log(e);
			} finally {
				monitor.done();
			}
		}

		public static MultiFixTarget[] getCleanUpTargets(IMarker[] markers) {
			Hashtable<ICompilationUnit, List<IProblemLocation>> problemLocations= new Hashtable<>();
			for (IMarker marker : markers) {
				ICompilationUnit cu= getCompilationUnit(marker);

				if (cu != null) {
					IEditorInput input= EditorUtility.getEditorInput(cu);
					IProblemLocation location= findProblemLocation(input, marker);
					if (location != null) {
						List<IProblemLocation> l= problemLocations.get(cu.getPrimary());
						if (l == null) {
							l= new ArrayList<>();
							problemLocations.put(cu.getPrimary(), l);
						}
						l.add(location);
					}
				}
			}

			MultiFixTarget[] result= new MultiFixTarget[problemLocations.size()];
			int i= 0;
			for (Map.Entry<ICompilationUnit, List<IProblemLocation>> entry : problemLocations.entrySet()) {
				ICompilationUnit cu= entry.getKey();
				List<IProblemLocation> locations= entry.getValue();
				result[i]= new MultiFixTarget(cu, locations.toArray(new IProblemLocation[locations.size()]));
				i++;
			}

			return result;
		}

		@Override
		public String getDescription() {
			return fProposal.getAdditionalProposalInfo();
		}

		@Override
		public Image getImage() {
			return fProposal.getImage();
		}

		@Override
		public int getRelevanceForResolution() {
			if (fProposal != null)
				return fProposal.getRelevance();
			return 0;
		}

		@Override
		public IMarker[] findOtherMarkers(IMarker[] markers) {
			if (fProposal instanceof CreatePackageInfoWithDefaultNullnessProposal) {
				CreatePackageInfoWithDefaultNullnessProposal createPackageInfoWithDefaultNullnessProposal= (CreatePackageInfoWithDefaultNullnessProposal) fProposal;
				final List<IMarker> result= new ArrayList<>();
				for (IMarker marker : markers) {
					int id= marker.getAttribute(IJavaModelMarker.ID, -1);
					if(JavaModelUtil.PACKAGE_INFO_JAVA.equals(marker.getResource().getName())) {
						// if marker is on package-info.java, no need to create it (another quickfix offers just to add @NonNullByDefault)
						continue;
					}
					if (createPackageInfoWithDefaultNullnessProposal.fProblemId == id)
						result.add(marker);
				}
				if (result.isEmpty())
					return NO_MARKERS;

				return result.toArray(new IMarker[result.size()]);
			}
			if (!(fProposal instanceof FixCorrectionProposal))
				return NO_MARKERS;

			FixCorrectionProposal fix= (FixCorrectionProposal)fProposal;
			final ICleanUp cleanUp= fix.getCleanUp();
			if (!(cleanUp instanceof IMultiFix))
				return NO_MARKERS;

			IMultiFix multiFix= (IMultiFix) cleanUp;

			final Hashtable<IFile, List<IMarker>> fileMarkerTable= getMarkersForFiles(markers);
			if (fileMarkerTable.isEmpty())
				return NO_MARKERS;

			final List<IMarker> result= new ArrayList<>();

			for (Map.Entry<IFile, List<IMarker>> entry : fileMarkerTable.entrySet()) {
				IFile file= entry.getKey();
				List<IMarker> fileMarkers= entry.getValue();

				IJavaElement element= JavaCore.create(file);
				if (element instanceof ICompilationUnit) {
					ICompilationUnit unit= (ICompilationUnit) element;

					for (int i= 0, size= fileMarkers.size(); i < size; i++) {
						IMarker marker= fileMarkers.get(i);
						IProblemLocation problem= createFromMarker(marker, unit);
						if (problem != null && multiFix.canFix(unit, problem)) {
							result.add(marker);
						}
					}
				}
			}

			if (result.isEmpty())
				return NO_MARKERS;

			return result.toArray(new IMarker[result.size()]);
		}

		/**
		 * Returns the markers with the same type as fMarker.getType for each IFile.
		 * @param markers the markers
		 * @return mapping files to markers
		 */
		private Hashtable<IFile, List<IMarker>> getMarkersForFiles(IMarker[] markers) {
			final Hashtable<IFile, List<IMarker>> result= new Hashtable<>();

			String markerType;
			try {
				markerType= fMarker.getType();
			} catch (CoreException e1) {
				JavaPlugin.log(e1);
				return result;
			}

			for (IMarker marker : markers) {
				if (!marker.equals(fMarker)) {
					String currMarkerType= null;
					try {
						currMarkerType= marker.getType();
					} catch (CoreException e1) {
						JavaPlugin.log(e1);
					}

					if (currMarkerType != null && currMarkerType.equals(markerType)) {
						IResource res= marker.getResource();
						if (res instanceof IFile && res.isAccessible()) {
							List<IMarker> markerList= result.get(res);
							if (markerList == null) {
								markerList= new ArrayList<>();
								result.put((IFile) res, markerList);
							}
							markerList.add(marker);
						}
					}
				}
			}
			return result;
		}
	}

	private static final IMarkerResolution[] NO_RESOLUTIONS= new IMarkerResolution[0];


	/**
	 * Constructor for CorrectionMarkerResolutionGenerator.
	 */
	public CorrectionMarkerResolutionGenerator() {
		super();
	}

	@Override
	public boolean hasResolutions(IMarker marker) {
		return internalHasResolutions(marker);
	}

	@Override
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

		ICompilationUnit cu= getCompilationUnit(marker);
		if (cu != null) {
			IEditorInput input= EditorUtility.getEditorInput(cu);
			if (input != null) {
				IProblemLocation location= findProblemLocation(input, marker);
				if (location != null) {

					IInvocationContext context= new AssistContext(cu,  location.getOffset(), location.getLength());
					if (!hasProblem(context.getASTRoot().getProblems(), location) && !(marker.getResource() instanceof IFolder))
						return NO_RESOLUTIONS;

					ArrayList<IJavaCompletionProposal> proposals= new ArrayList<>();
					JavaCorrectionProcessor.collectCorrections(context, new IProblemLocation[] { location }, proposals);
					Collections.sort(proposals, new CompletionProposalComparator());

					int nProposals= proposals.size();
					IMarkerResolution[] resolutions= new IMarkerResolution[nProposals];
					for (int i= 0; i < nProposals; i++) {
						resolutions[i]= new CorrectionMarkerResolution(context.getCompilationUnit(), location.getOffset(), location.getLength(), proposals.get(i), marker);
					}
					return resolutions;
				}
			}
		}
		return NO_RESOLUTIONS;
	}

	private static boolean hasProblem(IProblem[] problems, IProblemLocation location) {
		for (IProblem problem : problems) {
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
		if (res instanceof IFolder && res.isAccessible()) {
			IJavaElement element= JavaCore.create((IFolder) res);
			if (element instanceof IPackageFragment) {
				IPackageFragment packageFragment= (IPackageFragment) element;
				IJavaProject javaProject= packageFragment.getJavaProject();
				try {
					IPackageFragment[] packageFragments= javaProject.getPackageFragments();
					for (IPackageFragment p : packageFragments) {
						if (p.getElementName().equals(packageFragment.getElementName())) {
							ICompilationUnit compilationUnit= p.getCompilationUnit(org.eclipse.jdt.internal.corext.util.JavaModelUtil.PACKAGE_INFO_JAVA);
							if (compilationUnit.exists()) {
								return compilationUnit;
							}
						}
					}
					// no existing package-info.java found, return any writeable compilation unit in packageFragment as placeholder
					// (there must be one, or the error wouldn't exist.)
					for (ICompilationUnit compilationUnit : packageFragment.getCompilationUnits()) {
						if (compilationUnit.exists()) {
							return compilationUnit;
						}
					}
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				}
			}
		}
		return null;
	}

	public static IProblemLocation findProblemLocation(IEditorInput input, IMarker marker) {
		if (marker.getResource() instanceof IFolder) {
			ICompilationUnit cu= getCompilationUnit(marker);
			return createFromMarker(marker, cu);
		}
		IAnnotationModel model= JavaPlugin.getDefault().getCompilationUnitDocumentProvider().getAnnotationModel(input);
		if (model != null) { // open in editor
			Iterator<Annotation> iter= model.getAnnotationIterator();
			while (iter.hasNext()) {
				Annotation curr= iter.next();
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
