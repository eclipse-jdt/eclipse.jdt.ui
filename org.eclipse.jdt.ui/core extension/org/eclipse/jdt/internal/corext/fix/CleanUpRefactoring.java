/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.text.edits.TextEditVisitor;
import org.eclipse.text.edits.UndoEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ltk.core.refactoring.CategorizedTextEditGroup;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.ContentStamp;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringTickProvider;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextEditBasedChangeGroup;
import org.eclipse.ltk.core.refactoring.TextFileChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.dom.ASTBatchParser;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.MultiStateCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.fix.CleanUpOptions;
import org.eclipse.jdt.internal.ui.fix.ICleanUp;
import org.eclipse.jdt.internal.ui.fix.MapCleanUpOptions;
import org.eclipse.jdt.internal.ui.fix.ICleanUp.CleanUpContext;
import org.eclipse.jdt.internal.ui.fix.IMultiFix.MultiFixContext;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.jdt.internal.ui.refactoring.IScheduledRefactoring;

public class CleanUpRefactoring extends Refactoring implements IScheduledRefactoring {
	
	public static class CleanUpTarget {
		
		private final ICompilationUnit fCompilationUnit;

		public CleanUpTarget(ICompilationUnit unit) {
			fCompilationUnit= unit;
		}
		
		public ICompilationUnit getCompilationUnit() {
			return fCompilationUnit;
		}
	}
	
	public static class MultiFixTarget extends CleanUpTarget {
		
		private final IProblemLocation[] fProblems;

		public MultiFixTarget(ICompilationUnit unit, IProblemLocation[] problems) {
			super(unit);
			fProblems= problems;
		}
		
		public IProblemLocation[] getProblems() {
			return fProblems;
		}
	}
	
	public static class CleanUpChange extends CompilationUnitChange {

		private UndoEdit fUndoEdit;

		public CleanUpChange(String name, ICompilationUnit cunit) {
	        super(name, cunit);
        }
		
		/**
		 * {@inheritDoc}
		 */
		protected Change createUndoChange(UndoEdit edit, ContentStamp stampToRestore) {
		    fUndoEdit= edit;
			return super.createUndoChange(edit, stampToRestore);
		}

		public UndoEdit getUndoEdit() {
        	return fUndoEdit;
        }	
	}
	
	private static class FixCalculationException extends RuntimeException {
		
		private static final long serialVersionUID= 3807273310144726165L;
		
		private final CoreException fException;
		
		public FixCalculationException(CoreException exception) {
			fException= exception;
		}
		
		public CoreException getException() {
			return fException;
		}
	}
	
	private static class ParseListElement {
		
		private final CleanUpTarget fTarget;
		private final ICleanUp[] fCleanUpsArray;
		
		public ParseListElement(CleanUpTarget cleanUpTarget, ICleanUp[] cleanUps) {
			fTarget= cleanUpTarget;
			fCleanUpsArray= cleanUps;
		}
		
		public CleanUpTarget getTarget() {
			return fTarget;
		}
		
		public ICleanUp[] getCleanUps() {
			return fCleanUpsArray;
		}
	}
	
	private final class CleanUpRefactoringProgressMonitor extends SubProgressMonitor {
		
		private double fRealWork;
		private int fFlushCount;
		private final int fSize;
		private final int fIndex;
		
		private CleanUpRefactoringProgressMonitor(IProgressMonitor monitor, int ticks, int size, int index) {
			super(monitor, ticks);
			fFlushCount= 0;
			fSize= size;
			fIndex= index;
		}
		
		/**
		 * {@inheritDoc}
		 */
		public void internalWorked(double work) {
			fRealWork+= work;
		}
		
		public void flush() {
			super.internalWorked(fRealWork);
			reset();
			fFlushCount++;
		}
		
		public void reset() {
			fRealWork= 0.0;
		}
		
		public void done() {}
		
		public int getIndex() {
			return fIndex + fFlushCount;
		}
		
		public String getSubTaskMessage(ICompilationUnit source) {
			String typeName= source.getElementName();
			return Messages.format(FixMessages.CleanUpRefactoring_ProcessingCompilationUnit_message, new Object[] {new Integer(getIndex()), new Integer(fSize), typeName});
		}
	}
	
	private static class CleanUpASTRequestor extends ASTRequestor {
		
		private final List/*<ParseListElement>*/fUndoneElements;
		private final Hashtable/*<ICompilationUnit, Change>*/fSolutions;
		private final Hashtable/*<ICompilationUnit, ParseListElement>*/fCompilationUnitParseElementMap;
		private final CleanUpRefactoringProgressMonitor fMonitor;
		
		public CleanUpASTRequestor(List parseList, Hashtable solutions, CleanUpRefactoringProgressMonitor monitor) {
			fSolutions= solutions;
			fMonitor= monitor;
			fUndoneElements= new ArrayList();
			fCompilationUnitParseElementMap= new Hashtable(parseList.size());
			for (Iterator iter= parseList.iterator(); iter.hasNext();) {
				ParseListElement element= (ParseListElement)iter.next();
				fCompilationUnitParseElementMap.put(element.getTarget().getCompilationUnit(), element);
			}
		}
		
		/**
		 * {@inheritDoc}
		 */
		public void acceptAST(ICompilationUnit source, CompilationUnit ast) {
			
			fMonitor.subTask(fMonitor.getSubTaskMessage(source));
			
			ICompilationUnit primary= (ICompilationUnit)source.getPrimaryElement();
			ParseListElement element= (ParseListElement)fCompilationUnitParseElementMap.get(primary);
			CleanUpTarget target= element.getTarget();
			
			CleanUpContext context;
			if (target instanceof MultiFixTarget) {
				context= new MultiFixContext(source, ast, ((MultiFixTarget)target).getProblems());
			} else {
				context= new CleanUpContext(source, ast);
			}
			ICleanUp[] rejectedCleanUps= calculateSolutions(context, element.getCleanUps());
			
			if (rejectedCleanUps.length > 0) {
				fUndoneElements.add(new ParseListElement(target, rejectedCleanUps));
				fMonitor.reset();
			} else {
				fMonitor.flush();
			}
		}
		
		public void acceptSource(ICompilationUnit source) {
			acceptAST(source, null);
		}
		
		public List getUndoneElements() {
			return fUndoneElements;
		}
		
		private ICleanUp[] calculateSolutions(CleanUpContext context, ICleanUp[] cleanUps) {
			List/*<ICleanUp>*/result= new ArrayList();
			CleanUpChange solution;
			try {
				solution= calculateChange(context, cleanUps, result);
			} catch (CoreException e) {
				throw new FixCalculationException(e);
			}
			
			if (solution != null) {
				try {
					integrateSolution(solution, context.getCompilationUnit());
				} catch (JavaModelException e) {
					throw new FixCalculationException(e);
				}
			}
			
			return (ICleanUp[])result.toArray(new ICleanUp[result.size()]);
		}
		
		private void integrateSolution(CleanUpChange solution, ICompilationUnit source) throws JavaModelException {
			ICompilationUnit primary= source.getPrimary();
			
			List changes= (List)fSolutions.get(primary);
			if (changes == null) {
				changes= new ArrayList();
				fSolutions.put(primary, changes);
			}
			changes.add(solution);
		}
	}
	
	private class CleanUpFixpointIterator {
		
		private List/*<ParseListElement>*/fParseList;
		private final Hashtable/*<ICompilationUnit, List<CleanUpChange>>*/fSolutions;
		private final Hashtable/*<ICompilationUnit (primary), ICompilationUnit (working copy)>*/fWorkingCopies;
		private final Map fCleanUpOptions;
		private final int fSize;
		private int fIndex;
		
		public CleanUpFixpointIterator(CleanUpTarget[] targets, ICleanUp[] cleanUps) {
			fSolutions= new Hashtable(targets.length);
			fWorkingCopies= new Hashtable();
			
			fParseList= new ArrayList(targets.length);
			for (int i= 0; i < targets.length; i++) {
				fParseList.add(new ParseListElement(targets[i], cleanUps));
			}
			
			fCleanUpOptions= new Hashtable();
			for (int i= 0; i < cleanUps.length; i++) {
				ICleanUp cleanUp= cleanUps[i];
				Map currentCleanUpOption= cleanUp.getRequirements().getCompilerOptions();
				if (currentCleanUpOption != null)
					fCleanUpOptions.putAll(currentCleanUpOption);
			}
			
			fSize= targets.length;
			fIndex= 1;
		}
		
		public boolean hasNext() {
			return !fParseList.isEmpty();
		}
		
		public void next(IProgressMonitor monitor) throws CoreException {
			List parseList= new ArrayList();
			List sourceList= new ArrayList();
			
			try {
				for (Iterator iter= fParseList.iterator(); iter.hasNext();) {
					ParseListElement element= (ParseListElement)iter.next();
					
					ICompilationUnit compilationUnit= element.getTarget().getCompilationUnit();
					if (fSolutions.containsKey(compilationUnit)) {
						if (fWorkingCopies.containsKey(compilationUnit)) {
							compilationUnit= (ICompilationUnit)fWorkingCopies.get(compilationUnit);
						} else {
							compilationUnit= compilationUnit.getWorkingCopy(new WorkingCopyOwner() {}, null);
							fWorkingCopies.put(compilationUnit.getPrimary(), compilationUnit);
						}
						applyChange(compilationUnit, (List)fSolutions.get(compilationUnit.getPrimary()));
					}
					
					if (requiresAST(element.getCleanUps())) {
						parseList.add(compilationUnit);
					} else {
						sourceList.add(compilationUnit);
					}
				}
				
				CleanUpRefactoringProgressMonitor cuMonitor= new CleanUpRefactoringProgressMonitor(monitor, parseList.size() + sourceList.size(), fSize, fIndex);
				CleanUpASTRequestor requestor= new CleanUpASTRequestor(fParseList, fSolutions, cuMonitor);
				if (parseList.size() > 0) {
					ASTBatchParser parser= new ASTBatchParser() {
						protected ASTParser createParser(IJavaProject project) {
							ASTParser result= ASTParser.newParser(ASTProvider.SHARED_AST_LEVEL);
							result.setResolveBindings(true);
							result.setProject(project);

							Map options= RefactoringASTParser.getCompilerOptions(project);
							options.putAll(fCleanUpOptions);
							result.setCompilerOptions(options);
							return result;
						}
					};
					try {
						ICompilationUnit[] units= (ICompilationUnit[])parseList.toArray(new ICompilationUnit[parseList.size()]);
						parser.createASTs(units, new String[0], requestor, cuMonitor);
					} catch (FixCalculationException e) {
						throw e.getException();
					}
				}
				
				for (Iterator iterator= sourceList.iterator(); iterator.hasNext();) {
					ICompilationUnit cu= (ICompilationUnit)iterator.next();
					
					monitor.worked(1);
					
					requestor.acceptSource(cu);
					
					if (monitor.isCanceled())
						throw new OperationCanceledException();
				}
				
				fParseList= requestor.getUndoneElements();
				fIndex= cuMonitor.getIndex();
			} finally {
			}
		}
		
		public void dispose() {
			for (Iterator iterator= fWorkingCopies.values().iterator(); iterator.hasNext();) {
				ICompilationUnit cu= (ICompilationUnit)iterator.next();
				try {
					cu.discardWorkingCopy();
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				}
			}
			fWorkingCopies.clear();
		}
		
		private boolean requiresAST(ICleanUp[] cleanUps) throws CoreException {
			for (int i= 0; i < cleanUps.length; i++) {
				if (cleanUps[i].getRequirements().requiresAST())
					return true;
			}
			return false;
		}
		
		public Change[] getResult() {
			
			Change[] result= new Change[fSolutions.size()];
			int i=0;
			for (Iterator iterator= fSolutions.entrySet().iterator(); iterator.hasNext();) {
				Map.Entry entry= (Map.Entry)iterator.next();
				
				List changes= (List)entry.getValue();
				ICompilationUnit unit= (ICompilationUnit)entry.getKey();
				
				int saveMode;
				try {
					if (fLeaveFilesDirty || unit.getBuffer().hasUnsavedChanges()) {
						saveMode= TextFileChange.LEAVE_DIRTY;
					} else {
						saveMode= TextFileChange.FORCE_SAVE;
					}
				} catch (JavaModelException e) {
					saveMode= TextFileChange.LEAVE_DIRTY;
					JavaPlugin.log(e);
				}
				
				if (changes.size() == 1) {
					CleanUpChange change= (CleanUpChange)changes.get(0);
					change.setSaveMode(saveMode);
					result[i]= change;
				} else {
					MultiStateCompilationUnitChange mscuc= new MultiStateCompilationUnitChange(getChangeName(unit), unit);
					for (int j= 0; j < changes.size(); j++) {
						mscuc.addChange(createGroupFreeChange((CleanUpChange)changes.get(j)));
					}
					mscuc.setSaveMode(saveMode);
					result[i]= mscuc;
				}
				
				i++;
			}
			
			return result;
		}
		
		private TextChange createGroupFreeChange(CleanUpChange change) {
			CleanUpChange result= new CleanUpChange(change.getName(), change.getCompilationUnit());
			result.setEdit(change.getEdit());
			result.setSaveMode(change.getSaveMode());
	        return result;
        }
		
		private void applyChange(ICompilationUnit compilationUnit, List changes) throws JavaModelException, CoreException {
			IDocument document= new Document(((CompilationUnitChange)changes.get(0)).getCurrentContent(new NullProgressMonitor()));
			for (int i= 0; i < changes.size(); i++) {
				CleanUpChange change= (CleanUpChange)changes.get(i);
				TextEdit edit= change.getEdit().copy();
					
				try {
					edit.apply(document, TextEdit.UPDATE_REGIONS);
				} catch (MalformedTreeException e) {
					JavaPlugin.log(e);
				} catch (BadLocationException e) {
					JavaPlugin.log(e);
				}
			}
			compilationUnit.getBuffer().setContents(document.get());
		}
	}
	
	private static final RefactoringTickProvider CLEAN_UP_REFACTORING_TICK_PROVIDER= new RefactoringTickProvider(0, 1, 0, 0);
	
	private final List/*<ICleanUp>*/fCleanUps;
	private final Hashtable/*<IJavaProject, List<CleanUpTarget>*/fProjects;
	private Change fChange;
	private boolean fLeaveFilesDirty;
	private final String fName;
	
	public CleanUpRefactoring() {
		this(FixMessages.CleanUpRefactoring_Refactoring_name);
	}
	
	public CleanUpRefactoring(String name) {
		fName= name;
		fCleanUps= new ArrayList();
		fProjects= new Hashtable();
	}
	
	public void addCompilationUnit(ICompilationUnit unit) {
		addCleanUpTarget(new CleanUpTarget(unit));
	}
	
	public void addCleanUpTarget(CleanUpTarget target) {
		
		IJavaProject javaProject= target.getCompilationUnit().getJavaProject();
		if (!fProjects.containsKey(javaProject))
			fProjects.put(javaProject, new ArrayList());
		
		List targets= (List)fProjects.get(javaProject);
		targets.add(target);
	}
	
	public CleanUpTarget[] getCleanUpTargets() {
		List result= new ArrayList();
		for (Iterator iter= fProjects.values().iterator(); iter.hasNext();) {
			List projectTargets= (List)iter.next();
			result.addAll(projectTargets);
		}
		return (CleanUpTarget[])result.toArray(new CleanUpTarget[result.size()]);
	}
	
	public int getCleanUpTargetsSize() {
		int result= 0;
		for (Iterator iter= fProjects.values().iterator(); iter.hasNext();) {
			List projectTargets= (List)iter.next();
			result+= projectTargets.size();
		}
		return result;
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
	
	public void setLeaveFilesDirty(boolean leaveFilesDirty) {
		fLeaveFilesDirty= leaveFilesDirty;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#getName()
	 */
	public String getName() {
		return fName;
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
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		if (pm != null) {
			pm.beginTask("", 1); //$NON-NLS-1$
			pm.worked(1);
			pm.done();
		}
		return fChange;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		
		if (pm == null)
			pm= new NullProgressMonitor();
		
		if (fProjects.size() == 0 || fCleanUps.size() == 0) {
			pm.beginTask("", 1); //$NON-NLS-1$
			pm.worked(1);
			pm.done();
			fChange= new NullChange();
			
			return new RefactoringStatus();
		}
		
		int cuCount= getCleanUpTargetsSize();
		
		ICleanUp first= (ICleanUp) fCleanUps.get(0);
		boolean usesProjectSettings= first.getOptions() == null;
		
		RefactoringStatus result= new RefactoringStatus();
		
		ICleanUp[] cleanUps= getCleanUps();
		pm.beginTask("", cuCount * 2 * fCleanUps.size() + 4 * cleanUps.length); //$NON-NLS-1$
		try {
			DynamicValidationStateChange change= new DynamicValidationStateChange(getName());
			change.setSchedulingRule(getSchedulingRule());
			for (Iterator projectIter= fProjects.entrySet().iterator(); projectIter.hasNext();) {
				Map.Entry entry= (Entry) projectIter.next();
				IJavaProject project= (IJavaProject) entry.getKey();
				List targetsList= (List) entry.getValue();
				CleanUpTarget[] targets= (CleanUpTarget[])targetsList.toArray(new CleanUpTarget[targetsList.size()]);
				
				if (usesProjectSettings) {
					result.merge(setProjectOptions(project, cleanUps));
					if (result.hasFatalError())
						return result;
				}
				
				result.merge(checkPreConditions(project, targets, new SubProgressMonitor(pm, 3 * cleanUps.length)));
				if (result.hasFatalError())
					return result;
				
				Change[] changes= cleanUpProject(project, targets, cleanUps, pm);
				
				result.merge(checkPostConditions(new SubProgressMonitor(pm, cleanUps.length)));
				if (result.hasFatalError())
					return result;
				
				for (int i= 0; i < changes.length; i++) {
					change.add(changes[i]);
				}
			}
			fChange= change;
			
			List files= new ArrayList();
			findFilesToBeModified(change, files);
			result.merge(Checks.validateModifiesFiles((IFile[])files.toArray(new IFile[files.size()]), getValidationContext()));
			if (result.hasFatalError())
				return result;
		} finally {
			pm.done();
			if (usesProjectSettings) {
				for (int i= 0; i < cleanUps.length; i++) {
					cleanUps[i].setOptions(null);
				}
			}
		}
		
		return result;
	}
	
	private void findFilesToBeModified(CompositeChange change, List result) throws JavaModelException {
		Change[] children= change.getChildren();
		for (int i= 0; i < children.length; i++) {
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
	
	private Change[] cleanUpProject(IJavaProject project, CleanUpTarget[] targets, ICleanUp[] cleanUps, IProgressMonitor monitor) throws CoreException {
		CleanUpFixpointIterator iter= new CleanUpFixpointIterator(targets, cleanUps);
		
		SubProgressMonitor subMonitor= new SubProgressMonitor(monitor, 2 * targets.length * cleanUps.length);
		subMonitor.beginTask("", targets.length); //$NON-NLS-1$
		subMonitor.subTask(Messages.format(FixMessages.CleanUpRefactoring_Parser_Startup_message, project.getElementName()));
		try {
			while (iter.hasNext()) {
				iter.next(subMonitor);
			}
			
			return iter.getResult();
		} finally {
			iter.dispose();
			subMonitor.done();
		}
	}
	
	private RefactoringStatus setProjectOptions(IJavaProject javaProject, ICleanUp[] cleanUps) throws CoreException {
		Map options= CleanUpPreferenceUtil.loadOptions(new ProjectScope(javaProject.getProject()));
		if (options == null) {
			return RefactoringStatus.createFatalErrorStatus(Messages.format(FixMessages.CleanUpRefactoring_could_not_retrive_profile, javaProject.getElementName()));
		}
		
		CleanUpOptions cleanUpOptions= new MapCleanUpOptions(options);
		for (int j= 0; j < cleanUps.length; j++) {
			cleanUps[j].setOptions(cleanUpOptions);
		}
		
		return new RefactoringStatus();
	}
	
	private RefactoringStatus checkPreConditions(IJavaProject javaProject, CleanUpTarget[] targets, IProgressMonitor monitor) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		
		ICompilationUnit[] compilationUnits= new ICompilationUnit[targets.length];
		for (int i= 0; i < targets.length; i++) {
			compilationUnits[i]= targets[i].getCompilationUnit();
		}
		
		ICleanUp[] cleanUps= getCleanUps();
		monitor.beginTask("", compilationUnits.length * cleanUps.length); //$NON-NLS-1$
		monitor.subTask(Messages.format(FixMessages.CleanUpRefactoring_Initialize_message, javaProject.getElementName()));
		try {
			for (int j= 0; j < cleanUps.length; j++) {
				result.merge(cleanUps[j].checkPreConditions(javaProject, compilationUnits, new SubProgressMonitor(monitor, compilationUnits.length)));
				if (result.hasFatalError())
					return result;
			}
		} finally {
			monitor.done();
		}
		
		return result;
	}
	
	private RefactoringStatus checkPostConditions(SubProgressMonitor monitor) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		
		ICleanUp[] cleanUps= getCleanUps();
		monitor.beginTask("", cleanUps.length); //$NON-NLS-1$
		monitor.subTask(FixMessages.CleanUpRefactoring_checkingPostConditions_message);
		try {
			for (int j= 0; j < cleanUps.length; j++) {
				result.merge(cleanUps[j].checkPostConditions(new SubProgressMonitor(monitor, 1)));
				if (result.hasFatalError())
					return result;
			}
		} finally {
			monitor.done();
		}
		return result;
	}
	
	private static String getChangeName(ICompilationUnit compilationUnit) {
		StringBuffer buf= new StringBuffer();
		JavaElementLabels.getCompilationUnitLabel(compilationUnit, JavaElementLabels.ALL_DEFAULT, buf);
		buf.append(JavaElementLabels.CONCAT_STRING);
		
		StringBuffer buf2= new StringBuffer();
		JavaElementLabels.getPackageFragmentLabel((IPackageFragment)compilationUnit.getParent(), JavaElementLabels.P_QUALIFIED, buf2);
		buf.append(buf2.toString().replace('.', '/'));
		
		return buf.toString();
	}
	
	public static CleanUpChange calculateChange(CleanUpContext context, ICleanUp[] cleanUps, List undoneCleanUps) throws CoreException {
		if (cleanUps.length == 0)
			return null;
		
		CleanUpChange solution= null;
		int i= 0;
		do {
			ICleanUp cleanUp= cleanUps[i];
			IFix fix= cleanUp.createFix(context);
			if (fix != null) {
				CompilationUnitChange current= fix.createChange();
				TextEdit currentEdit= pack(current.getEdit());
				
				if (solution != null) {
					if (intersects(currentEdit, solution.getEdit())) {
						undoneCleanUps.add(cleanUp);
					} else {
						CleanUpChange merge= new CleanUpChange(FixMessages.CleanUpRefactoring_clean_up_multi_chang_name, context.getCompilationUnit());
						merge.setEdit(merge(currentEdit, solution.getEdit()));
						
						copyChangeGroups(merge, solution);
						copyChangeGroups(merge, current);
						
						solution= merge;
					}
				} else {
					solution= new CleanUpChange(current.getName(), context.getCompilationUnit());
					solution.setEdit(currentEdit);
					
					copyChangeGroups(solution, current);
				}
			}
			i++;
		} while (i < cleanUps.length && (context.getAST() == null || !cleanUps[i].getRequirements().requiresFreshAST()));
		
		for (; i < cleanUps.length; i++) {
			undoneCleanUps.add(cleanUps[i]);
		}
		return solution;
	}
	
	private static void copyChangeGroups(CompilationUnitChange target, CompilationUnitChange source) {
		TextEditBasedChangeGroup[] changeGroups= source.getChangeGroups();
		for (int i= 0; i < changeGroups.length; i++) {
			TextEditGroup textEditGroup= changeGroups[i].getTextEditGroup();
			TextEditGroup newGroup;
			if (textEditGroup instanceof CategorizedTextEditGroup) {
				String label= textEditGroup.getName();
				newGroup= new CategorizedTextEditGroup(label, new GroupCategorySet(new GroupCategory(label, label, label)));
			} else {
				newGroup= new TextEditGroup(textEditGroup.getName());
			}
			TextEdit[] textEdits= textEditGroup.getTextEdits();
			for (int j= 0; j < textEdits.length; j++) {
				newGroup.addTextEdit(textEdits[j]);
			}
			target.addTextEditGroup(newGroup);
		}
	}
	
	private static TextEdit pack(TextEdit edit) {
		if (!edit.hasChildren())
			return edit;
		
		final List edits= new ArrayList();
		edit.accept(new TextEditVisitor() {
			public boolean visitNode(TextEdit node) {
				if (node instanceof MultiTextEdit)
					return true;
				
				edits.add(node);
				return false;
			}
		});
		MultiTextEdit result= new MultiTextEdit();
		for (Iterator iterator= edits.iterator(); iterator.hasNext();) {
			TextEdit child= (TextEdit)iterator.next();
			child.getParent().removeChild(child);
			TextChangeCompatibility.insert(result, child);
		}
		return result;
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
				while (children1[i1].getExclusiveEnd() < children2[i2].getOffset()) {
					i1++;
					if (i1 >= children1.length)
						return false;
				}
				while (children2[i2].getExclusiveEnd() < children1[i1].getOffset()) {
					i2++;
					if (i2 >= children2.length)
						return false;
				}
				if (intersects(children1[i1], children2[i2]))
					return true;
				
				if (children1[i1].getExclusiveEnd() < children2[i2].getExclusiveEnd()) {
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
	
	private static TextEdit merge(TextEdit edit1, TextEdit edit2) {
		MultiTextEdit result= new MultiTextEdit();
		if (edit1 instanceof MultiTextEdit && edit2 instanceof MultiTextEdit) {
			MultiTextEdit multiTextEdit1= (MultiTextEdit)edit1;
			TextEdit[] children1= multiTextEdit1.getChildren();
			
			if (children1.length == 0)
				return edit2;
			
			MultiTextEdit multiTextEdit2= (MultiTextEdit)edit2;
			TextEdit[] children2= multiTextEdit2.getChildren();
			
			if (children2.length == 0)
				return edit1;
			
			int i1= 0;
			int i2= 0;
			while (i1 < children1.length && i2 < children2.length) {
				
				while (i1 < children1.length && children1[i1].getExclusiveEnd() < children2[i2].getOffset()) {
					edit1.removeChild(0);
					result.addChild(children1[i1]);
					i1++;
				}
				if (i1 >= children1.length) {
					for (int i= i2; i < children2.length; i++) {
						edit2.removeChild(0);
						result.addChild(children2[i]);
					}
					return result;
				}
				while (i2 < children2.length && children2[i2].getExclusiveEnd() < children1[i1].getOffset()) {
					edit2.removeChild(0);
					result.addChild(children2[i2]);
					i2++;
				}
				if (i2 >= children2.length) {
					for (int i= i1; i < children1.length; i++) {
						edit1.removeChild(0);
						result.addChild(children1[i]);
					}
					return result;
				}
				
				if (!(children1[i1].getExclusiveEnd() < children2[i2].getOffset())) {
					edit1.removeChild(0);
					edit2.removeChild(0);
					result.addChild(merge(children1[i1], children2[i2]));
					i1++;
					i2++;
				}
			}
			
			return result;
		} else if (edit1 instanceof MultiTextEdit) {
			TextEdit[] children= edit1.getChildren();
			
			int i= 0;
			while (children[i].getExclusiveEnd() < edit2.getOffset()) {
				edit1.removeChild(0);
				result.addChild(children[i]);
				i++;
				if (i >= children.length) {
					result.addChild(edit2);
					return result;
				}
			}
			edit1.removeChild(0);
			result.addChild(merge(children[i], edit2));
			i++;
			while (i < children.length) {
				edit1.removeChild(0);
				result.addChild(children[i]);
				i++;
			}
			
			return result;
		} else if (edit2 instanceof MultiTextEdit) {
			TextEdit[] children= edit2.getChildren();
			
			int i= 0;
			while (children[i].getExclusiveEnd() < edit1.getOffset()) {
				edit2.removeChild(0);
				result.addChild(children[i]);
				i++;
				if (i >= children.length) {
					result.addChild(edit1);
					return result;
				}
			}
			edit2.removeChild(0);
			result.addChild(merge(edit1, children[i]));
			i++;
			while (i < children.length) {
				edit2.removeChild(0);
				result.addChild(children[i]);
				i++;
			}
			
			return result;
		} else {
			if (edit1.getExclusiveEnd() < edit2.getOffset()) {
				result.addChild(edit1);
				result.addChild(edit2);
			} else {
				result.addChild(edit2);
				result.addChild(edit1);
			}
			
			return result;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#getRefactoringTickProvider()
	 */
	protected RefactoringTickProvider doGetRefactoringTickProvider() {
		return CLEAN_UP_REFACTORING_TICK_PROVIDER;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public ISchedulingRule getSchedulingRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
	
	public static ICleanUp[] createCleanUps() {
		return JavaPlugin.getDefault().getCleanUpRegistry().getCleanUps();
	}
	
	public static ICleanUp[] createCleanUps(Map settings) {
		ICleanUp[] result= createCleanUps();
		
		for (int i= 0; i < result.length; i++) {
			result[i].setOptions(new MapCleanUpOptions(settings));
		}
		
		return result;
	}
}
