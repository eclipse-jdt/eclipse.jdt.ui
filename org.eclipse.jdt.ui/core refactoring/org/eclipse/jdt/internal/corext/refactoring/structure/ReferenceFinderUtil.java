package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultCollector;

public class ReferenceFinderUtil {
	
	//no instances
	private ReferenceFinderUtil(){
	}

	//----- referenced types -
	
	public static IType[] getTypesReferencedIn(IJavaElement[] elements, IProgressMonitor pm) throws JavaModelException {
		SearchResult[] results= getTypeReferencesIn(elements, pm);
		List referencedTypes= extractElements(results, IJavaElement.TYPE);
		return (IType[]) referencedTypes.toArray(new IType[referencedTypes.size()]);	
	}
	
	private static SearchResult[] getTypeReferencesIn(IJavaElement[] elements, IProgressMonitor pm) throws JavaModelException {
		List referencedFields= new ArrayList();
		pm.beginTask("", elements.length); //$NON-NLS-1$
		for (int i = 0; i < elements.length; i++) {
			referencedFields.addAll(getTypeReferencesIn(elements[i], new SubProgressMonitor(pm, 1)));
		}
		pm.done();
		return (SearchResult[]) referencedFields.toArray(new SearchResult[referencedFields.size()]);
	}
	
	private static List getTypeReferencesIn(IJavaElement element, IProgressMonitor pm) throws JavaModelException {
		SearchResultCollector collector= new SearchResultCollector(pm);
		new SearchEngine().searchDeclarationsOfReferencedTypes(ResourcesPlugin.getWorkspace(), element, collector);
		return collector.getResults();
	}
	
	//----- referenced fields ----
	
	public static IField[] getFieldsReferencedIn(IJavaElement[] elements, IProgressMonitor pm) throws JavaModelException {
		SearchResult[] results= getFieldReferencesIn(elements, pm);
		List referencedFields= extractElements(results, IJavaElement.FIELD);
		return (IField[]) referencedFields.toArray(new IField[referencedFields.size()]);
	}

	private static SearchResult[] getFieldReferencesIn(IJavaElement[] elements, IProgressMonitor pm) throws JavaModelException {
		List referencedFields= new ArrayList();
		pm.beginTask("", elements.length); //$NON-NLS-1$
		for (int i = 0; i < elements.length; i++) {
			referencedFields.addAll(getFieldReferencesIn(elements[i], new SubProgressMonitor(pm, 1)));
		}
		pm.done();
		return (SearchResult[]) referencedFields.toArray(new SearchResult[referencedFields.size()]);
	}
	
	private static List getFieldReferencesIn(IJavaElement element, IProgressMonitor pm) throws JavaModelException {
		SearchResultCollector collector= new SearchResultCollector(pm);
		new SearchEngine().searchDeclarationsOfAccessedFields(ResourcesPlugin.getWorkspace(), element, collector);
		return collector.getResults();
	}
	
	//----- referenced methods ----
	
	public static IMethod[] getMethodsReferencedIn(IJavaElement[] elements, IProgressMonitor pm) throws JavaModelException {
		SearchResult[] results= getMethodReferencesIn(elements, pm);
		List referencedMethods= extractElements(results, IJavaElement.METHOD);
		return (IMethod[]) referencedMethods.toArray(new IMethod[referencedMethods.size()]);
	}
	
	private static SearchResult[] getMethodReferencesIn(IJavaElement[] elements, IProgressMonitor pm) throws JavaModelException {
		List referencedMethods= new ArrayList();
		pm.beginTask("", elements.length); //$NON-NLS-1$
		for (int i = 0; i < elements.length; i++) {
			referencedMethods.addAll(getMethodReferencesIn(elements[i], new SubProgressMonitor(pm, 1)));
		}
		pm.done();
		return (SearchResult[]) referencedMethods.toArray(new SearchResult[referencedMethods.size()]);
	}
	
	private static List getMethodReferencesIn(IJavaElement element, IProgressMonitor pm) throws JavaModelException {
		SearchResultCollector collector= new SearchResultCollector(pm);
		new SearchEngine().searchDeclarationsOfSentMessages(ResourcesPlugin.getWorkspace(), element, collector);
		return collector.getResults();
	}
	

	/// private helpers 	
	private static List extractElements(SearchResult[] searchResults, int elementType){
		List elements= new ArrayList();
		for (int i= 0; i < searchResults.length; i++) {
			IJavaElement el= searchResults[i].getEnclosingElement();
			if (el.exists() && el.getElementType() == elementType)
				elements.add(el);
		}
		return elements;
	}	
}
