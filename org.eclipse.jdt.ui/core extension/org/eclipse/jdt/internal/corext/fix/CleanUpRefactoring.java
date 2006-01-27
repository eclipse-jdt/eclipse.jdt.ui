/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.MoveSourceEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.text.edits.TextEditVisitor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringTickProvider;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextEditBasedChangeGroup;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.composite.MultiStateCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.fix.ICleanUp;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;

public class CleanUpRefactoring extends Refactoring {

	private static final int BATCH_SIZE= 40;

	private class FixCalculationException extends RuntimeException {

		private static final long serialVersionUID= 3807273310144726165L;
		
		private final CoreException fException;

		public FixCalculationException(CoreException exception) {
			fException= exception;
		}

		public CoreException getException() {
			return fException;
		}
		
	}
	
	private class ParseListElement {

		private final ICompilationUnit fUnit;
		private List fCleanUpsToGo;
		private ICleanUp[] fCleanUpsArray;

		public ParseListElement(ICompilationUnit unit) {
			fUnit= unit;
			fCleanUpsToGo= new ArrayList();
			fCleanUpsArray= new ICleanUp[0];
		}
		
		public ParseListElement(ICompilationUnit unit, ICleanUp[] cleanUps) {
			fUnit= unit;
			fCleanUpsArray= cleanUps;
			fCleanUpsToGo= null;
		}

		public void addCleanUp(ICleanUp multiFix) {
			if (fCleanUpsToGo == null) {
				fCleanUpsToGo= Arrays.asList(fCleanUpsArray);
			}
			fCleanUpsToGo.add(multiFix);
			fCleanUpsArray= null;
		}

		public ICompilationUnit getCompilationUnit() {
			return fUnit;
		}

		public ICleanUp[] getCleanUps() {
			if (fCleanUpsArray == null) {
				fCleanUpsArray= (ICleanUp[])fCleanUpsToGo.toArray(new ICleanUp[fCleanUpsToGo.size()]);
			}
			return fCleanUpsArray;
		}
	}
	
	
	private final class SolutionGenerator extends ASTRequestor {

		private final IProgressMonitor fMonitor;
		private final List fResult;
		private final Hashtable fSolutions;
		private final int fSize;
		private final Iterator fToParseIter;
		private ParseListElement fCurElement;
		private int fIndex;

		private SolutionGenerator(List/*<ParseListElement>*/ toSolve, int startIndex, int totalSize, Hashtable solutions, IProgressMonitor monitor) {
			fMonitor= monitor;
			fResult= new ArrayList();
			fSolutions= solutions;
			fSize= totalSize;
			fIndex= startIndex + 1;
			
			fToParseIter= toSolve.iterator();
			fCurElement= (ParseListElement)fToParseIter.next();
			fMonitor.subTask(getSubTaskMessage(fCurElement, fIndex, fSize));
		}

		public List getResult() {
			return fResult;
		}

		public void acceptAST(ICompilationUnit source, CompilationUnit ast) {
			ParseListElement tuple= calculateSolution(fSolutions, ast, fCurElement.getCleanUps());
			if (tuple != null) {
				fResult.add(tuple);
			}
			if (fToParseIter.hasNext()) {
				fIndex++;
				fCurElement= (ParseListElement)fToParseIter.next();
				fMonitor.subTask(getSubTaskMessage(fCurElement, fIndex, fSize));
			}
		}
		
		private String getSubTaskMessage(ParseListElement element, int index, int size) {
			return Messages.format(FixMessages.CleanUpRefactoring_ProcessingCompilationUnit_message, new Object[] {getTypeName(element.getCompilationUnit()), new Integer(index), new Integer(size)});
		}
		
		private ParseListElement calculateSolution(Hashtable solutions, CompilationUnit ast, ICleanUp[] cleanUps) {
			TextChange solution= null;
			ParseListElement result= null;
			for (int i= 0; i < cleanUps.length; i++) {
				ICleanUp cleanUp= cleanUps[i];
				try {
					IFix fix= cleanUp.createFix(ast);
					if (fix != null) {
						TextChange current= fix.createChange();
						if (solution != null) {
							if (intersects(current.getEdit(),solution.getEdit())) {
								if (result == null) {
									result= new ParseListElement((ICompilationUnit)ast.getJavaElement().getPrimaryElement());
								}
								result.addCleanUp(cleanUp);
							} else {
								mergeTextChanges(current, solution);
								solution= current;
							}
						} else {
							solution= current;
						}
					}
				} catch (CoreException e) {
					throw new FixCalculationException(e);
				}
			}
			
			if (solution != null) {
				if (solutions.containsKey(ast.getJavaElement().getPrimaryElement())) {
					MultiStateCompilationUnitChange oldChange= (MultiStateCompilationUnitChange)solutions.get(ast.getJavaElement().getPrimaryElement());
					oldChange.addChange(solution);
				} else {
					solutions.put(ast.getJavaElement(), solution);
				}
			}
			
			return result;
		}
	}


	private static final RefactoringTickProvider CLEAN_UP_REFACTORING_TICK_PROVIDER= new RefactoringTickProvider(0, 0, 1, 0);
	
	private List/*<ICleanUp>*/ fCleanUps;
	private Hashtable/*<IJavaProject, List<ICompilationUnit>*/ fProjects;
	
	public CleanUpRefactoring() {
		fCleanUps= new ArrayList();
		fProjects= new Hashtable();
	}
	
	public void addCompilationUnit(ICompilationUnit unit) {
		IJavaProject javaProject= unit.getJavaProject();
		if (!fProjects.containsKey(javaProject))
			fProjects.put(javaProject, new ArrayList());
		
		List cus= (List)fProjects.get(javaProject);
		cus.add(unit);
	}
	
	public void clearCompilationUnits() {
		fProjects.clear();
	}
	
	public boolean hasCompilationUnits() {
		return !fProjects.isEmpty();
	}
	
	public ICompilationUnit[] getCompilationUnits() {
		List cus= new ArrayList();
		for (Iterator iter= fProjects.values().iterator(); iter.hasNext();) {
			List pcus= (List)iter.next();
			cus.addAll(pcus);
		}
		return (ICompilationUnit[])cus.toArray(new ICompilationUnit[cus.size()]);
	}
	
	public void addCleanUp(ICleanUp fix) {
		fCleanUps.add(fix);
	}
	
	public void clearCleanUps() {
		fCleanUps.clear();
	}
	
	public boolean hasCleanUps() {
		return !fCleanUps.isEmpty();
	}
	
	public ICleanUp[] getCleanUps() {
		return (ICleanUp[])fCleanUps.toArray(new ICleanUp[fCleanUps.size()]);
	}
	
	public IJavaProject[] getProjects() {
		return (IJavaProject[])fProjects.keySet().toArray(new IJavaProject[fProjects.keySet().size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#getName()
	 */
	public String getName() {
		return FixMessages.CleanUpRefactoring_Refactoring_name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		if (pm != null) {
			pm.beginTask("", 1); //$NON-NLS-1$
			pm.worked(1);
			pm.done();
		}
		return new RefactoringStatus();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		if (pm != null) {
			pm.beginTask("", 1); //$NON-NLS-1$
			pm.worked(1);
			pm.done();
		}
		return new RefactoringStatus();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		
		if (pm == null)
			pm= new NullProgressMonitor();
		
		if (fProjects.size() == 0 || fCleanUps.size() == 0) {
			pm.beginTask("", 1); //$NON-NLS-1$
			pm.worked(1);
			pm.done();
			return new NullChange();
		}
		
		int cuCount= 0;
		for (Iterator iter= fProjects.keySet().iterator(); iter.hasNext();) {
			IJavaProject project= (IJavaProject)iter.next();
			cuCount+= ((List)fProjects.get(project)).size();
		}
		
		pm.beginTask("", cuCount * 3 * fCleanUps.size()); //$NON-NLS-1$

		try {
			CompositeChange result= new CompositeChange(getName());
			for (Iterator projectIter= fProjects.keySet().iterator(); projectIter.hasNext();) {
				IJavaProject project= (IJavaProject)projectIter.next();
				
				List compilationUnits= (List)fProjects.get(project);
				ICompilationUnit[] cus= (ICompilationUnit[])compilationUnits.toArray(new ICompilationUnit[compilationUnits.size()]);
				
				ICleanUp[] cleanUps= (ICleanUp[])fCleanUps.toArray(new ICleanUp[fCleanUps.size()]);
				
				cleanUpProject(project, cus, cleanUps, result, pm);
			}
			return result;
			
		} finally {
			pm.done();
		}
	}

	private void cleanUpProject(IJavaProject project, ICompilationUnit[] compilationUnits, ICleanUp[] cleanUps, CompositeChange result, IProgressMonitor monitor) throws CoreException {
		initCleanUps(project, compilationUnits, new SubProgressMonitor(monitor, compilationUnits.length * cleanUps.length));
		
		List toGo= new ArrayList();
		for (int i= 0; i < compilationUnits.length; i++) {
			toGo.add(new ParseListElement(compilationUnits[i], cleanUps));
		}
		Hashtable resultingFixes= new Hashtable();
		Map cleanUpOptions= getCleanUpOptions();
		
		int start= 0;
		int end= 0;
		while (end < toGo.size()) {
			end= Math.min(start + BATCH_SIZE, toGo.size());
			List toParse= toGo.subList(start, end);
			
			ASTParser parser= createParser(cleanUpOptions, project);
			List redoList= parse(resultingFixes, start, toParse, new SubProgressMonitor(monitor, toParse.size() * 2 * cleanUps.length), parser, toGo.size());
			toGo.addAll(redoList);
			
			start= end;
		}
		for (Iterator iter= resultingFixes.values().iterator(); iter.hasNext();) {
			Change element= (Change)iter.next();
			result.add(element);
		}

		endCleanUps();
	}

	private void initCleanUps(IJavaProject javaProject, ICompilationUnit[] compilationUnits, IProgressMonitor monitor) throws CoreException {
		ICleanUp[] cleanUps= getCleanUps();
		monitor.beginTask(Messages.format(FixMessages.CleanUpRefactoring_Initialize_message, javaProject.getElementName()), compilationUnits.length * cleanUps.length);
		try {
			for (int j= 0; j < cleanUps.length; j++) {
				cleanUps[j].beginCleanUp(javaProject, compilationUnits, new SubProgressMonitor(monitor, compilationUnits.length));
			}
		} finally {
			monitor.done();
		}
	}
	
	private void endCleanUps() throws CoreException {
		ICleanUp[] cleanUps= getCleanUps();
		for (int j= 0; j < cleanUps.length; j++) {
			cleanUps[j].endCleanUp();
		}
	}

	/**
	 * @return Options for all multi-fixes in <code>fMultiFixes</code>
	 */
	private Map getCleanUpOptions() {
		Map cleanUpOptions= new Hashtable();
		for (Iterator iter= fCleanUps.iterator(); iter.hasNext();) {
			ICleanUp cleanUp= (ICleanUp)iter.next();
			Map currentCleanUpOption= cleanUp.getRequiredOptions();
			if (currentCleanUpOption != null)
				cleanUpOptions.putAll(currentCleanUpOption);
		}
		return cleanUpOptions;
	}

	private List/*<ParseListElement>*/ parse(final Hashtable solutions, final int start, List/*<ParseListElement*/ toParse, final IProgressMonitor monitor, ASTParser parser, final int size) throws CoreException {

		final ICompilationUnit[] compilationUnits= new ICompilationUnit[toParse.size()];
		final List workingCopys= new ArrayList();
		try {
			int i= 0;
			for (Iterator iter= toParse.iterator(); iter.hasNext();) {
				ParseListElement element= (ParseListElement) iter.next();

				ICompilationUnit compilationUnit= element.getCompilationUnit();
				if (solutions.containsKey(compilationUnit)) {

					ICompilationUnit workingCopy= createWorkingCopy(solutions, compilationUnit);

					compilationUnits[i]= workingCopy;
					workingCopys.add(workingCopy);
				} else {
					compilationUnits[i]= compilationUnit;
				}
				i++;
			}

			SolutionGenerator solutionGenerator= new SolutionGenerator(toParse, start, size, solutions, monitor);
			try {
				parser.createASTs(compilationUnits, new String[0], solutionGenerator, monitor);
			} catch (FixCalculationException e) {
				throw e.getException();
			}
			return solutionGenerator.getResult();
		} finally { 
			for (Iterator iter= workingCopys.iterator(); iter.hasNext();) {
				ICompilationUnit cu= (ICompilationUnit)iter.next();
				cu.discardWorkingCopy();
			}
			monitor.done();
		}
	}

	private ICompilationUnit createWorkingCopy(final Hashtable solutions, ICompilationUnit compilationUnit) throws JavaModelException, CoreException {
		Change oldChange= (Change)solutions.get(compilationUnit);
		
		MultiStateCompilationUnitChange mscuc;
		if (!(oldChange instanceof MultiStateCompilationUnitChange)) {
			mscuc= new MultiStateCompilationUnitChange(getChangeName(compilationUnit), compilationUnit);
			mscuc.setKeepPreviewEdits(true);
			mscuc.addChange((TextChange)oldChange);
			solutions.remove(compilationUnit);
			solutions.put(compilationUnit, mscuc);
		} else {
			mscuc= (MultiStateCompilationUnitChange)oldChange;
		}

		ICompilationUnit workingCopy= compilationUnit.getWorkingCopy(new WorkingCopyOwner() {}, null, null);
		
		IBuffer buffer= workingCopy.getBuffer();
		buffer.setContents(mscuc.getPreviewContent(null));
		return workingCopy;
	}

	private String getChangeName(ICompilationUnit compilationUnit) {
		StringBuffer buf= new StringBuffer();
		JavaElementLabels.getCompilationUnitLabel(compilationUnit, JavaElementLabels.ALL_DEFAULT, buf);
		buf.append(JavaElementLabels.CONCAT_STRING);
		
		StringBuffer buf2= new StringBuffer();
		JavaElementLabels.getPackageFragmentLabel((IPackageFragment) compilationUnit.getParent(), JavaElementLabels.P_QUALIFIED, buf2);
		buf.append(buf2.toString().replace('.', '/'));
		
		return buf.toString();
	}

	private ASTParser createParser(Map cleanUpOptions, IJavaProject javaProject) {
		ASTParser parser= ASTParser.newParser(ASTProvider.AST_LEVEL);
		parser.setResolveBindings(true);
		parser.setProject(javaProject);
				
		Map options= RefactoringASTParser.getCompilerOptions(javaProject);
		options.putAll(cleanUpOptions);
		parser.setCompilerOptions(options);
		return parser;
	}

	private String getTypeName(final ICompilationUnit unit) {
		return JavaCore.removeJavaLikeExtension(unit.getElementName());
	}

	private static boolean intersects(TextEdit edit1, TextEdit edit2) {
		if (edit1 instanceof MultiTextEdit && edit2 instanceof MultiTextEdit) {
			MultiTextEdit multiTextEdit1= (MultiTextEdit)edit1;
			TextEdit[] children1= multiTextEdit1.getChildren();
			
			MultiTextEdit multiTextEdit2= (MultiTextEdit)edit2;
			TextEdit[] children2= multiTextEdit2.getChildren();
			
			int i1= 0;
			int i2= 0;
			while (i1 < children1.length && i2 < children2.length) {
				while (i1 + 1 < children1.length && children1[i1 + 1].getOffset() < children2[i2].getOffset()) {
					i1++;
				}
				while (i2 + 1 < children2.length && children2[i2 + 1].getOffset() < children1[i1].getOffset()) {
					i2++;
				}
				if (intersects(children1[i1], children2[i2]))
					return true;
				
				if (children1[i1].getOffset() < children2[i2].getOffset()) {
					i1++;
				} else {
					i2++;
				}
			}
			
			return false;
			
		} else if (edit1 instanceof MultiTextEdit) {
			MultiTextEdit multiTextEdit1= (MultiTextEdit)edit1;
			TextEdit[] children= multiTextEdit1.getChildren();
			for (int i= 0; i < children.length; i++) {
				TextEdit child= children[i];
				if (intersects(child, edit2))
					return true;
			}
			return false;
			
		} else if (edit2 instanceof MultiTextEdit) {
			MultiTextEdit multiTextEdit2= (MultiTextEdit)edit2;
			TextEdit[] children= multiTextEdit2.getChildren();
			for (int i= 0; i < children.length; i++) {
				TextEdit child= children[i];
				if (intersects(child, edit1))
					return true;
			}
			return false;
			
		} else {
			int start1= edit1.getOffset();
			int end1= start1 + edit1.getLength();
			int start2= edit2.getOffset();
			int end2= start2 + edit2.getLength();
			
			if (start1 > end2)
				return false;
			
			if (start2 > end1)
				return false;
			
			return true;
		}
	}

	private static void mergeTextChanges(TextChange target, TextChange source) {
		final List edits= new ArrayList();
		source.getEdit().accept(new TextEditVisitor() {
			public boolean visitNode(TextEdit edit) {
				if (edit instanceof MoveSourceEdit)
					return false;
				
				if (edit instanceof MultiTextEdit)
					return true;
					
				edits.add(edit);
				return super.visitNode(edit);
			}
			
		});
		if (edits.isEmpty())
			return;
		
		final List removedEdits= new ArrayList();
		target.getEdit().accept(new TextEditVisitor() {
			public boolean visit(DeleteEdit deleteEdit) {
				int start= deleteEdit.getOffset();
				int end= start + deleteEdit.getLength();
				
				List toRemove= new ArrayList();
				for (Iterator iter= edits.iterator(); iter.hasNext();) {
					TextEdit edit= (TextEdit)iter.next();
					int offset= edit.getOffset();
					if (offset >= start && offset <= end) {
						toRemove.add(edit);
					}
				}
				
				if (!toRemove.isEmpty()) {
					edits.removeAll(toRemove);
					removedEdits.addAll(toRemove);
				}
				
				return super.visit(deleteEdit);
			}
		});
		for (Iterator iter= edits.iterator(); iter.hasNext();) {
			TextEdit edit= (TextEdit)iter.next();
			edit.getParent().removeChild(edit);
			TextChangeCompatibility.insert(target.getEdit(), edit);
		}
		TextEditBasedChangeGroup[] changeGroups= source.getChangeGroups();
		for (int i= 0; i < changeGroups.length; i++) {
			TextEditGroup textEditGroup= changeGroups[i].getTextEditGroup();
			TextEditGroup newGroup= new TextEditGroup(textEditGroup.getName());
			TextEdit[] textEdits= textEditGroup.getTextEdits();
			for (int j= 0; j < textEdits.length; j++) {
				if (!removedEdits.contains(textEdits[j]))
					newGroup.addTextEdit(textEdits[j]);
			}
			target.addTextEditGroup(newGroup);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#getRefactoringTickProvider()
	 */
	protected RefactoringTickProvider doGetRefactoringTickProvider() {
		return CLEAN_UP_REFACTORING_TICK_PROVIDER;
	}
}