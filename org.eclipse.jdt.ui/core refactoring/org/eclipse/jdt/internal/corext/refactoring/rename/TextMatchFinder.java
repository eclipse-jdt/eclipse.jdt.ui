package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.corext.codemanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.tagging.ITextUpdatingRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;

class TextMatchFinder {
	
	private Map fJavaDocMatches; //ICompilationUnit -> Set of Integer
	private Map fCommentMatches; //ICompilationUnit -> Set of Integer
	private Map fStringMatches;//ICompilationUnit -> Set of Integer
	
	private IJavaSearchScope fScope;
	private RefactoringScanner fScanner;
	
	private TextMatchFinder(IJavaSearchScope scope, RefactoringScanner scanner, Map javaDocMatches, Map commentMatches, Map stringMatches){
		Assert.isNotNull(scope);
		Assert.isNotNull(scanner);
		fCommentMatches= commentMatches;
		fJavaDocMatches= javaDocMatches;
		fStringMatches= 	stringMatches;
		fScope= scope;
		fScanner= scanner;
	}

	static void findTextMatches(IProgressMonitor pm, IJavaSearchScope scope, ITextUpdatingRefactoring refactoring, TextChangeManager manager) throws JavaModelException{
		try{
			if (! isSearchingNeeded(refactoring))
				return;
			RefactoringScanner scanner = createRefactoringScanner(refactoring);
			Map javaDocMatches= new HashMap();
			Map commentsMatches= new HashMap();
			Map stringMatches= new HashMap();
			findTextMatches(pm, scope, scanner, javaDocMatches, commentsMatches, stringMatches);
			int patternLength= scanner.getPattern().length();
			String newName= refactoring.getNewName();
			addMatches(manager, newName, patternLength, javaDocMatches, "text reference update in JavaDoc");
			addMatches(manager, newName, patternLength, commentsMatches, "text reference update in a comment");
			addMatches(manager, newName, patternLength, stringMatches, "text reference update in a string literal");
		} catch (CoreException e){
			throw new JavaModelException(e);
		}
	}
	
	private static void addMatches(TextChangeManager manager, String newText, int patternLength, Map matches, String matchName) throws CoreException{
		for(Iterator iter= matches.keySet().iterator(); iter.hasNext();){
			Object key= iter.next();
			if (! (key instanceof ICompilationUnit))
				continue;
			ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists((ICompilationUnit)key);
			Set results= (Set)matches.get(cu);
			for (Iterator resultIter= results.iterator(); resultIter.hasNext();){
				int match= ((Integer)resultIter.next()).intValue();
				manager.get(cu).addTextEdit(matchName, SimpleTextEdit.createReplace(match, patternLength, newText));
			}
		}
	}

	private static void findTextMatches(IProgressMonitor pm, IJavaSearchScope scope, RefactoringScanner scanner, Map javaDocMatches, Map commentMatches, Map stringMatches) throws JavaModelException{
		new TextMatchFinder( scope, scanner, javaDocMatches, commentMatches, stringMatches).findTextMatches(pm);
	}	
	
	private static boolean isSearchingNeeded(ITextUpdatingRefactoring refactoring){
		return refactoring.getUpdateComments() || refactoring.getUpdateJavaDoc() || refactoring.getUpdateStrings();
	}
	
	private static RefactoringScanner createRefactoringScanner(ITextUpdatingRefactoring refactoring) {
		RefactoringScanner scanner= new RefactoringScanner();
		scanner.setAnalyzeComments(refactoring.getUpdateComments());
		scanner.setAnalyzeJavaDoc(refactoring.getUpdateJavaDoc());
		scanner.setAnalyzeStrings(refactoring.getUpdateStrings());
		scanner.setPattern(refactoring.getCurrentName());
		return scanner;
	}
	
	private void findTextMatches(IProgressMonitor pm) throws JavaModelException{	
		try{
			IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			pm.beginTask("", projects.length);
			
			Set enclosingProjectSet = createEnclosingProjectSet();
			
			for (int i =0 ; i < projects.length; i++){
				if (pm.isCanceled())
					throw new OperationCanceledException();
				if (enclosingProjectSet.contains(projects[i].getFullPath()))
					addTextMatches(projects[i], new SubProgressMonitor(pm, 1));
				else	
					pm.worked(1);
			}
		} finally{
			pm.done();
		}		
	}

	private Set createEnclosingProjectSet() {
		IPath[] enclosingProjects= fScope.enclosingProjectsAndJars();
		Set enclosingProjectSet= new HashSet();
		enclosingProjectSet.addAll(Arrays.asList(enclosingProjects));	
		return enclosingProjectSet;
	}
	
	private void addTextMatches(IResource resource, IProgressMonitor pm) throws JavaModelException{
		try{
			if (resource instanceof IFile){
				IJavaElement element= JavaCore.create(resource);
				pm.beginTask("", 1);
				if (!(element instanceof ICompilationUnit))
					return;
				if (! fScope.encloses((ICompilationUnit)element))
					return;
				addTextMatches((ICompilationUnit)element);
			}
			if (resource instanceof IContainer){
				IContainer container= (IContainer)resource;
				IResource[] members= container.members();
				pm.beginTask("", members.length);
				pm.subTask("searching for text matches in:" + container.getFullPath());
				for (int i = 0; i < members.length; i++) {
					if (pm.isCanceled())
						throw new OperationCanceledException();
					
					addTextMatches(members[i], new SubProgressMonitor(pm, 1));
				}	
			}
		} catch (CoreException e){
			throw new JavaModelException(e);	
		} finally{
			pm.done();
		}	
	}
	
	private void addTextMatches(ICompilationUnit cu) throws JavaModelException{
		Set javaDocResults= new HashSet();
		Set commentResults= new HashSet();
		Set stringResults= new HashSet();
		fScanner.scan(cu, javaDocResults, commentResults, stringResults);
		ICompilationUnit wc= WorkingCopyUtil.getWorkingCopyIfExists(cu);
		fJavaDocMatches.put(wc, javaDocResults);
		fCommentMatches.put(wc, commentResults);
		fStringMatches.put(wc, stringResults);
	}	
}

