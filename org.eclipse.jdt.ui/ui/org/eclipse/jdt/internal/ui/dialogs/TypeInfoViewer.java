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
 package org.eclipse.jdt.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.ProgressMonitorWrapper;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.TypeFilter;
import org.eclipse.jdt.internal.corext.util.TypeInfo;
import org.eclipse.jdt.internal.corext.util.TypeInfoFactory;
import org.eclipse.jdt.internal.corext.util.TypeInfoHistory;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.util.StringMatcher;

/**
 * A viewer to present type queried form the type history and form the
 * search engine. All viewer updating takes place in the UI thread.
 * Therefore no synchronization of the methods is necessary.
 * 
 * @since 3.1
 */
public class TypeInfoViewer {
	
	private static class TypeInfoFilter {
		private String fText;
		
		private int fFlags;
		
		private StringMatcher fPackageMatcher;
		private String fPackagePattern;
		
		private String fNamePattern;
		private StringMatcher fNameMatcher;

		private String fCamelCasePattern;
		private StringMatcher fCamelCaseTailMatcher;
		private StringMatcher fExactNameMatcher;
		
		private static final char END_SYMBOL= '<';
		private static final char ANY_STRING= '*';
		
		public TypeInfoFilter(String text) {
			fText= text;
			int index= text.lastIndexOf("."); //$NON-NLS-1$
			if (index == -1) {
				createNamePattern(text);
			} else {
				fPackagePattern= text.substring(0, index);
				fPackageMatcher= new StringMatcher(fPackagePattern, true, false);
				
				createNamePattern(text.substring(index + 1));
			}
			fNameMatcher= new StringMatcher(fNamePattern, true, false);
			if (fCamelCasePattern != null) {
				fExactNameMatcher= new StringMatcher(createTextPatternFromText(fText), true, false);
			}
		}
		public String getText() {
			return fText;
		}
		public boolean isSubFilter(String text) {
			return fText.startsWith(text);
		}
		public boolean isCamcelCasePattern() {
			return fCamelCasePattern != null;
		}
		public String getPackagePattern() {
			return fPackagePattern;
		}
		public String getNamePattern() {
			return fNamePattern;
		}
		public int getSearchFlags() {
			int result= 0;
			if (fCamelCasePattern != null) {
				result= result | SearchPattern.R_CASE_SENSITIVE;
			}
			if (fNamePattern.indexOf("*") != -1) { //$NON-NLS-1$
				result= result | SearchPattern.R_PATTERN_MATCH;
			}
			return result;
		}
		public boolean matchesSearchResult(TypeInfo type) {
			if (fCamelCasePattern == null)
				return true;
			/*
			String name= type.getTypeName();
			if (fExactNameMatcher.match(name))
				return true;
			*/
			return matchesCamelCase(type);
		}
		public boolean matchesCachedResult(TypeInfo type) {
			boolean matchesName= matchesName(type);
			boolean matchesPackage= matchesPackage(type);
			if (!(matchesName && matchesPackage))
				return false;
			return matchesSearchResult(type);
		}
		public boolean matchesCamelCase(TypeInfo type) {
			if (fCamelCasePattern == null)
				return true;
			String name= type.getTypeName();
			int camelCaseIndex= 0;
			int lastUpperCase= Integer.MAX_VALUE;
			for (int i= 0; camelCaseIndex < fCamelCasePattern.length() && i < name.length(); i++) {
				char c= name.charAt(i);
				if (Character.isUpperCase(c)) {
					lastUpperCase= i;
					if (c != fCamelCasePattern.charAt(camelCaseIndex))
						return false;
					camelCaseIndex++;
				}
			}
			if (camelCaseIndex < fCamelCasePattern.length())
				return false;
			if (lastUpperCase == name.length() - 1 || fCamelCaseTailMatcher == null)
				return true;
			return fCamelCaseTailMatcher.match(name.substring(lastUpperCase + 1));
		}
		public boolean matchesName(TypeInfo type) {
			return fNameMatcher.match(type.getTypeName());
		}
		public boolean matchesNameExact(TypeInfo type) {
			return fExactNameMatcher.match(type.getTypeName());
		}
		public boolean matchesPackage(TypeInfo type) {
			if (fPackageMatcher == null)
				return true;
			return fPackageMatcher.match(type.getTypeContainerName());
		}
		public boolean matchesAll(TypeInfo type) {
			if (!matchesCamelCase(type))
				return false;
			if (!fNameMatcher.match(type.getTypeName()))
				return false;

			if (fPackageMatcher == null)
				return true;

			return fPackageMatcher.match(type.getTypeContainerName());
		}
		private void createNamePattern(final String text) {
			int length= text.length();
			fCamelCasePattern= null;
			fNamePattern= text;
			if (length > 0) {
				char c= text.charAt(0);
				if (length > 1 && Character.isUpperCase(c)) {
					StringBuffer patternBuffer= new StringBuffer();
					StringBuffer camelCaseBuffer= new StringBuffer();
					patternBuffer.append(c);
					camelCaseBuffer.append(c);
					int index= 1;
					for (; index < length; index++) {
						c= text.charAt(index);
						if (Character.isUpperCase(c)) {
							patternBuffer.append("*"); //$NON-NLS-1$
							patternBuffer.append(c);
							camelCaseBuffer.append(c);
						} else {
							break;
						}
					}
					if (camelCaseBuffer.length() > 1) {
						if (index == length) {
							fNamePattern= patternBuffer.toString();
							fCamelCasePattern= camelCaseBuffer.toString();
						} else if (restIsLowerCase(text, index)) {
							fNamePattern= patternBuffer.toString();
							fCamelCasePattern= camelCaseBuffer.toString();
							fCamelCaseTailMatcher= new StringMatcher(
								createTextPatternFromText(text.substring(index)), true, false);
						}
					}
				}
				fNamePattern= createTextPatternFromText(fNamePattern);
			}
		}
		private boolean restIsLowerCase(String s, int start) {
			for (int i= start; i < s.length(); i++) {
				if (Character.isUpperCase(s.charAt(i)))
					return false;
			}
			return true;
		}
		private static String createTextPatternFromText(final String text) {
			int length= text.length();
			String result= text;
			switch (text.charAt(length - 1)) {
				case END_SYMBOL:
					return text.substring(0, length - 1);
				case ANY_STRING:
					return result;
				default:
					return text + ANY_STRING;
			}
		}
	}
	
	private static class SearchRequestor extends TypeNameRequestor {
		private volatile boolean fStop;
		
		private TypeInfoFilter fFilter;
		private Set fHistory;
		
		private TypeInfoFactory factory= new TypeInfoFactory();
		private List fResult;
		
		public SearchRequestor(TypeInfoFilter filter) {
			super();
			fFilter= filter;
			fResult= new ArrayList(2048);
		}
		public TypeInfo[] getResult() {
			return (TypeInfo[])fResult.toArray(new TypeInfo[fResult.size()]);
		}
		public void cancel() {
			fStop= true;
		}
		public void setHistory(Set history) {
			fHistory= history;
		}
		public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
			if (fStop)
				return;
			if (TypeFilter.isFiltered(packageName, simpleTypeName))
				return;
			TypeInfo type= factory.create(packageName, simpleTypeName, enclosingTypeNames, modifiers, path);
			if (fHistory.contains(type))
				return;
			if (fFilter.matchesSearchResult(type))
				fResult.add(type);
		}
	}
	
	private static class TypeInfoComparator implements Comparator {
		private TypeInfoFilter fFilter;
		public TypeInfoComparator(TypeInfoFilter filter) {
			fFilter= filter;
		}
	    public int compare(Object left, Object right) {
	     	TypeInfo leftInfo= (TypeInfo)left;
	     	TypeInfo rightInfo= (TypeInfo)right;
	     	int leftCategory= getCategory(leftInfo);
	     	int rightCategory= getCategory(rightInfo);
	     	if (leftCategory < rightCategory)
	     		return -1;
	     	if (leftCategory > rightCategory)
	     		return +1;
	     	int result= compareName(leftInfo.getTypeName(), rightInfo.getTypeName());
	     	if (result == 0) {
	     		return comparePackageName(leftInfo.getPackageName(), rightInfo.getPackageName());
	     	}
	     	return result;
	    }
		private int compareName(String leftString, String rightString) {
			int result= leftString.compareToIgnoreCase(rightString);
			if (result != 0) {
				return result;
			} else if (Strings.isLowerCase(leftString.charAt(0)) && 
				!Strings.isLowerCase(rightString.charAt(0))) {
	     		return +1;
			} else if (Strings.isLowerCase(rightString.charAt(0)) &&
	     		!Strings.isLowerCase(leftString.charAt(0))) {
	     		return -1;
			} else {
				return leftString.compareTo(rightString);
			}
		}
		private int comparePackageName(String leftString, String rightString) {
			int leftLength= leftString.length();
			int rightLength= rightString.length();
			if (leftLength == 0 && rightLength > 0)
				return -1;
			if (leftLength == 0 && rightLength == 0)
				return 0;
			if (leftLength > 0 && rightLength == 0)
				return +1;
			return compareName(leftString, rightString);
		}
		private int getCategory(TypeInfo type) {
			if (fFilter == null)
				return 0;
			if (!fFilter.isCamcelCasePattern())
				return 0;
			return fFilter.matchesNameExact(type) ? 0 : 1;
		}
	}
	
	private static class TypeInfoLabelProvider extends LabelProvider {
		
		public static final int PACKAGE_QUALIFICATION= 1;
		public static final int ROOT_QUALIFICATION= 2;
		
		private static final Image CLASS_ICON= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_CLASS);
		private static final Image ANNOTATION_ICON= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ANNOTATION);
		private static final Image INTERFACE_ICON= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_INTERFACE);
		private static final Image ENUM_ICON= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ENUM);

		public String getText(Object element) {
			return ((TypeInfo)element).getTypeName();
		}
		public String getFullyQualifiedText(TypeInfo type) {
			StringBuffer result= new StringBuffer();
			result.append(type.getTypeName());
			String containerName= type.getTypeContainerName();
			if (containerName.length() > 0) {
				result.append(JavaElementLabels.CONCAT_STRING);
				result.append(containerName);
			}
			result.append(JavaElementLabels.CONCAT_STRING);
			result.append(type.getPackageFragmentRootPath().toOSString());
			return result.toString();
		}
		public String getText(TypeInfo last, TypeInfo current, TypeInfo next) {
			StringBuffer result= new StringBuffer();
			int qualifications= 0;
			String currentTN= current.getTypeName();
			result.append(currentTN);
			String currentTCN= current.getTypeContainerName();
			if (last != null) {
				String lastTN= last.getTypeName();
				String lastTCN= last.getTypeContainerName();
				if (currentTCN.equals(lastTCN)) {
					if (currentTN.equals(lastTN)) {
						result.append(JavaElementLabels.CONCAT_STRING);
						result.append(currentTCN);
						result.append(JavaElementLabels.CONCAT_STRING);
						result.append(current.getPackageFragmentRootPath().toOSString());
						return result.toString();
					}
				} else if (currentTN.equals(lastTN)) {
					qualifications= 1;
				}
			}
			if (next != null) {
				String nextTN= next.getTypeName();
				String nextTCN= next.getTypeContainerName();
				if (currentTCN.equals(nextTCN)) {
					if (currentTN.equals(nextTN)) {
						result.append(JavaElementLabels.CONCAT_STRING);
						result.append(currentTCN);
						result.append(JavaElementLabels.CONCAT_STRING);
						result.append(current.getPackageFragmentRootPath().toOSString());
						return result.toString();
					}
				} else if (currentTN.equals(nextTN)) {
					qualifications= 1;
				}
			}
			if (qualifications > 0) {
				result.append(JavaElementLabels.CONCAT_STRING);
				result.append(currentTCN);
			}
			return result.toString();
		}
		
		public Image getImage(Object element) {
			int modifiers= ((TypeInfo)element).getModifiers();
			if (Flags.isAnnotation(modifiers)) {
				return ANNOTATION_ICON;
			} else if (Flags.isEnum(modifiers)) {
				return ENUM_ICON;
			} else if (Flags.isInterface(modifiers)) {
				return INTERFACE_ICON;
			}
			return CLASS_ICON;
		}
	}
	
	private static class ProgressMonitor extends ProgressMonitorWrapper {
		private TypeInfoViewer fViewer;
		private String fName;
		private int fTotalWork;
		private double fWorked;
		private long fLastUpdate= -1;
		
		public ProgressMonitor(IProgressMonitor monitor, TypeInfoViewer viewer) {
			super(monitor);
			fViewer= viewer;
		}
		public void setTaskName(String name) {
			super.setTaskName(name);
			fName= name;
		}
		public void beginTask(String name, int totalWork) {
			super.beginTask(name, totalWork);
			if (fName == null)
				fName= name;
			fTotalWork= totalWork;
			fLastUpdate= System.currentTimeMillis();
		}
		public void worked(int work) {
			super.worked(work);
			internalWorked(work);
		}
		public void done() {
			fViewer.progressDone();
			super.done();
		}
		public void internalWorked(double work) {
			fWorked= fWorked + work;
			if (System.currentTimeMillis() - fLastUpdate >= 200) {
				showProgress();
				fLastUpdate= System.currentTimeMillis();
			}
		}
		public void showProgress() {
			String message;
			if (fTotalWork == 0) {
				message= fName;
			} else {
				message= Messages.format(
					"{0} ({1}%)",
					new Object[] { fName, new Integer((int)((fWorked * 100) / fTotalWork)) });
			}
			fViewer.showProgress(message);
		}
	}

	private static abstract class AbstractSearchJob extends Job {
		private int fMode;
		
		protected int fTicket;
		protected TypeInfoViewer fViewer;
		protected TypeInfoLabelProvider fLabelProvider;
		
		protected TypeInfo[] fHistory;
		protected TypeInfoFilter fFilter;
		
		protected AbstractSearchJob(int ticket, TypeInfoViewer viewer, TypeInfoFilter filter, TypeInfo[] history, int numberOfVisibleItems, int mode) {
			super(JavaUIMessages.TypeInfoViewer_job_label);
			fMode= mode;
			fTicket= ticket;
			fViewer= viewer;
			fLabelProvider= fViewer.getLabelProvider();
			fFilter= filter;
			fHistory= history;
			setSystem(true);
		}
		public void stop() {
			cancel();
		}
		protected IStatus run(IProgressMonitor monitor) {
			try {
				if (VIRTUAL) { 
					return internalRunVirtual(monitor);
				} else {
					return internalRun(monitor);
				}
			} catch (CoreException e) {
				fViewer.failed(fTicket, e);
				return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR, JavaUIMessages.TypeInfoViewer_job_error, e);
			} catch (InterruptedException e) {
				return canceled(e, true);
			} catch (OperationCanceledException e) {
				return canceled(e, false);
			}
		}
		protected abstract TypeInfo[] getSearchResult(Set filteredHistory, IProgressMonitor monitor) throws CoreException;
		
		private IStatus internalRun(IProgressMonitor monitor) throws CoreException, InterruptedException {
			if (monitor.isCanceled())
				return canceled(null, false);
			
			fViewer.clear(fTicket);

			// local vars to speed up rendering
			TypeInfo last= null;
			TypeInfo type= null;
			TypeInfo next= null;
			List elements= new ArrayList();
			List images= new ArrayList();
			List labels= new ArrayList();
			
			Set filteredHistory= new HashSet();
			List matchingTypes= getFilteredHistory();
			int size= matchingTypes.size();
			if (size > 0) {
				type= (TypeInfo)matchingTypes.get(0);
				int i= 1;
				while(type != null) {
					next= (i == size) ? null : (TypeInfo)matchingTypes.get(i);
					filteredHistory.add(type);
					elements.add(type);
					images.add(fLabelProvider.getImage(type));
					labels.add(fLabelProvider.getText(last, type, next));
					last= type;
					type= next;
					i++;
				}
			}
			matchingTypes= null;
			fViewer.addAll(fTicket, elements, images, labels);
			
			if ((fMode & INDEX) == 0) {
				fViewer.done(fTicket);
				return ok();
			}
			TypeInfo[] result= getSearchResult(filteredHistory, monitor);
			if (result.length == 0) {
				fViewer.done(fTicket);
				return ok();
			}
			if (monitor.isCanceled())
				return canceled(null, false);			
			if (filteredHistory.size() > 0) {
				fViewer.addDashLine(fTicket);
			}
			int processed= 0;
			int nextIndex= 1;
			type= result[0];
			while (true) {
				long startTime= System.currentTimeMillis();
				elements.clear();
				images.clear();
				labels.clear();
	            int delta = Math.min(nextIndex == 1 ? fViewer.getNumberOfVisibleItems() : 10, result.length - processed);
				if (delta == 0)
					break;
				processed= processed + delta;
				while(delta > 0) {
					next= (nextIndex == result.length) ? null : result[nextIndex];
					elements.add(type);
					labels.add(fLabelProvider.getText(last, type, next));
					images.add(fLabelProvider.getImage(type));
					last= type;
					type= next;
					nextIndex++;
					delta--;
				}
				fViewer.addAll(fTicket, elements, images, labels);
				long sleep= 100 - (System.currentTimeMillis() - startTime);
				if (false)
					System.out.println("Sleeping for: " + sleep); //$NON-NLS-1$
				
				if (sleep > 0)
					Thread.sleep(sleep);
				
				if (monitor.isCanceled())
					return canceled(null, false);
			}
			fViewer.done(fTicket);
			return ok();
		}
		private IStatus internalRunVirtual(IProgressMonitor monitor) throws CoreException, InterruptedException {
			if (monitor.isCanceled())
				return canceled(null, false);
			
			fViewer.clear(fTicket);

			List matchingTypes= getFilteredHistory();
			fViewer.setHistoryResult(fTicket, (TypeInfo[])matchingTypes.toArray(new TypeInfo[matchingTypes.size()]));
			if ((fMode & INDEX) == 0) {
				fViewer.done(fTicket);
				return ok();
			}
			TypeInfo[] result= getSearchResult(new HashSet(matchingTypes), monitor);
			if (monitor.isCanceled())
				return canceled(null, false);			
			fViewer.setSearchResult(fTicket, result);
			fViewer.done(fTicket);
			return ok();
		}
		private List getFilteredHistory() {
			List matchingTypes= new ArrayList(fHistory.length);
			for (int i= 0; i < fHistory.length; i++) {
				TypeInfo type= fHistory[i];
				if (fFilter.matchesAll(type) && !TypeFilter.isFiltered(type.getFullyQualifiedName())) {
					matchingTypes.add(type);
				}
			}
			return matchingTypes;
		}
		private IStatus canceled(Exception e, boolean removePendingItems) {
			fViewer.canceled(fTicket, removePendingItems);
			return new Status(IStatus.CANCEL, JavaPlugin.getPluginId(), IStatus.CANCEL, JavaUIMessages.TypeInfoViewer_job_cancel, e);
		}
		private IStatus ok() {
			return new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
		}
	}
	
	private static class SearchEngineJob extends AbstractSearchJob {
		private IJavaSearchScope fScope;
		private int fElementKind;
		private SearchRequestor fReqestor;
		
		public SearchEngineJob(int ticket, TypeInfoViewer viewer, TypeInfoFilter filter, TypeInfo[] history, int numberOfVisibleItems, int mode, 
				IJavaSearchScope scope, int elementKind) {
			super(ticket, viewer, filter, history, numberOfVisibleItems, mode);
			fScope= scope;
			fElementKind= elementKind;
			fReqestor= new SearchRequestor(filter);
		}
		public void stop() {
			fReqestor.cancel();
			super.stop();
		}
		protected TypeInfo[] getSearchResult(Set filteredHistory, IProgressMonitor parent) throws CoreException {
			long start= System.currentTimeMillis();
			fReqestor.setHistory(filteredHistory);
			SearchEngine engine= new SearchEngine();
			String packPattern= fFilter.getPackagePattern();
			ProgressMonitor monitor= new ProgressMonitor(parent, fViewer);
			monitor.setTaskName("Searching...");
			engine.searchAllTypeNames(
				packPattern == null ? null : packPattern.toCharArray(), 
				fFilter.getNamePattern().toCharArray(), 
				fFilter.getSearchFlags(), 
				fElementKind, 
				fScope, 
				fReqestor, 
				IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, 
				monitor);
			if (DEBUG)
				System.out.println("Time needed until search has finished: " + (System.currentTimeMillis() - start)); //$NON-NLS-1$
			TypeInfo[] result= fReqestor.getResult();
			Arrays.sort(result, new TypeInfoComparator(fFilter));
			if (DEBUG)
				System.out.println("Time needed until sort has finished: " + (System.currentTimeMillis() - start)); //$NON-NLS-1$
			fViewer.rememberResult(fTicket, result);
			return result;
		}
	}
	
	private static class CachedResultJob extends AbstractSearchJob {
		private TypeInfo[] fLastResult;
		public CachedResultJob(int ticket, TypeInfo[] lastResult, TypeInfoViewer viewer, TypeInfoFilter filter, TypeInfo[] history, int numberOfVisibleItems, int mode) {
			super(ticket, viewer, filter, history, numberOfVisibleItems, mode);
			fLastResult= lastResult;
		}
		protected TypeInfo[] getSearchResult(Set filteredHistory, IProgressMonitor monitor) throws CoreException {
			List result= new ArrayList(2048);
			for (int i= 0; i < fLastResult.length; i++) {
				TypeInfo type= fLastResult[i];
				if (filteredHistory.contains(type))
					continue;
				if (fFilter.matchesCachedResult(type))
					result.add(type);
			}
			// we have to sort if the filter is a camel case filter.
			TypeInfo[] types= (TypeInfo[])result.toArray(new TypeInfo[result.size()]);
			if (fFilter.isCamcelCasePattern()) {
				Arrays.sort(types, new TypeInfoComparator(fFilter));
			}
			return types;
		}
	}
	
	private static class SyncJob extends Job {
		private TypeInfoViewer fViewer;
		public SyncJob(TypeInfoViewer viewer) {
			super("Synchronizing tables");
			fViewer= viewer;
		}
		public void stop() {
			cancel();
		}
		protected IStatus run(IProgressMonitor parent) {
			try {
				ProgressMonitor monitor= new ProgressMonitor(parent, fViewer);
				monitor.setTaskName("Refreshing...");
				monitor.showProgress();
				new SearchEngine().searchAllTypeNames(
					null, 
					// make sure we search a concrete name. This is faster according to Kent  
					"_______________".toCharArray(), //$NON-NLS-1$
					SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE, 
					IJavaSearchConstants.ENUM,
					SearchEngine.createWorkspaceScope(), 
					new TypeNameRequestor() {}, 
					IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, 
					monitor);
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
				return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR, JavaUIMessages.TypeInfoViewer_job_error, e);
			} catch (OperationCanceledException e) {
				return new Status(IStatus.CANCEL, JavaPlugin.getPluginId(), IStatus.CANCEL, JavaUIMessages.TypeInfoViewer_job_cancel, e);
			} finally {
				fViewer.syncJobDone();
			}
			return new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
		}
	}

	private static class DashLine {
		public int fCharWidth;
		public String getText(int width) {
			StringBuffer buffer= new StringBuffer();
			for (int i= 0; i < width / fCharWidth; i++) {
				buffer.append(SEPARATOR);
			}
			return buffer.toString();
		}
		public void setCharWidth(int width) {
			fCharWidth= width;
		}
	}
	
	private Display fDisplay;
	
	private Label fProgressLabel;
	
	private TypeInfoHistory fHistory;

	private int fNextElement;
	private List fItems;
	private int fNumberOfVisibleItems;
	private TypeInfoLabelProvider fLabelProvider;
	private Color fDashLineColor; 
	private DashLine fDashLine= new DashLine();
	/* remembers the last selection to restore unqualified labels */
	private TableItem[] fLastSelection;
	private String[] fLastLabels;
	private Table fTable;
	
	private SyncJob fSyncJob;
	
	private TypeInfoFilter fTypeInfoFilter;
	private TypeInfo[] fLastCompletedResult;
	private TypeInfoFilter fLastCompletedFilter;
	
	private IJavaSearchScope fSearchScope;
	private int fElementKind;
	private int fSearchJobTicket;
	
	private Object[] fElements;
	private int fNumberOfHistroyElements;
	
	private AbstractSearchJob fSearchJob;

	private static final int HISTORY= 1;
	private static final int INDEX= 2;
	private static final int FULL= HISTORY | INDEX;
	
	// private static final char MDASH= '—';
	// private static final char MDASH= '\u2012';    // figure dash  
	// private static final char MDASH= '\u2013';    // en dash      
	// private static final char MDASH= '\u2014';    // em dash <<=== works      
	// private static final char MDASH= '\u2015';    // horizontal bar
	private static final char SEPARATOR= '-'; 
	
	private static final boolean DEBUG= true;	
	private static final boolean VIRTUAL= false;
	
	public TypeInfoViewer(Composite parent, int flags, Label progressLabel, IJavaSearchScope scope, int elementKind) {
		Assert.isNotNull(scope);
		fDisplay= parent.getDisplay();
		fProgressLabel= progressLabel;
		fSearchScope= scope;
		fElementKind= elementKind;
		fTable= new Table(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.FLAT | flags | (VIRTUAL ? SWT.VIRTUAL : SWT.NONE));
		fTable.setFont(parent.getFont());
		fLabelProvider= new TypeInfoLabelProvider();
		fItems= new ArrayList(500);
		fTable.setHeaderVisible(false);
		fTable.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent event) {
				int itemHeight= fTable.getItemHeight();
				Rectangle clientArea= fTable.getClientArea();
				fNumberOfVisibleItems= (clientArea.height / itemHeight) + 1;
			}
		});
		fTable.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.DEL) {
					int index= fTable.getSelectionIndex();
					if (index == -1)
						return;
					TableItem item= fTable.getItem(index);
					Object element= item.getData();
					if (!(element instanceof TypeInfo))
						return;
					if (fHistory.remove((TypeInfo)element) != null) {
						item.dispose();
						fItems.remove(index);
						int count= fTable.getItemCount();
						if (count > 0) {
							if (index >= count) {
								index= count - 1;
							}
							fTable.setSelection(index);
			                fTable.notifyListeners(SWT.Selection, new Event());
						}
					}
				}
				
			}
		});
		fTable.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (fLastSelection != null) {
					for (int i= 0; i < fLastSelection.length; i++) {
						TableItem item= fLastSelection[i];
						// could be disposed by deleting element from 
						// type inof history
						if (!item.isDisposed())
							item.setText(fLastLabels[i]);
					}
				}
				TableItem[] items= fTable.getSelection();
				fLastSelection= new TableItem[items.length];
				fLastLabels= new String[items.length];
				for (int i= 0; i < items.length; i++) {
					TableItem item= items[i];
					fLastSelection[i]= item;
					fLastLabels[i]= item.getText();
					Object data= item.getData();
					if (data instanceof TypeInfo)
						item.setText(fLabelProvider.getFullyQualifiedText((TypeInfo)data));
				}
			}
		});
		fTable.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				fDashLineColor.dispose();
			}
		});
		if (VIRTUAL) {
			fTable.addListener(SWT.SetData, new Listener() {
				public void handleEvent(Event event) {
					TableItem item= (TableItem)event.item;
					setData(item);
				}
			});
		}
		GC gc= null;
		try {
			gc= new GC(fTable);
			gc.setFont(fTable.getFont());
			fDashLine.setCharWidth(gc.getAdvanceWidth(SEPARATOR));
		} finally {
			gc.dispose();
		}
		fDashLineColor= computeDashLineColor();
		fHistory= TypeInfoHistory.getInstance();
		fSyncJob= new SyncJob(this);
		fSyncJob.schedule();
	}
	
	public Table getTable() {
		return fTable;
	}
	
	private TypeInfoLabelProvider getLabelProvider() {
		return fLabelProvider;
	}
	
	private int getNumberOfVisibleItems() {
		return fNumberOfVisibleItems;
	}
	
	public void setFocus() {
		fTable.setFocus();
	}
	
	public TypeInfo[] getSelection() {
		TableItem[] items= fTable.getSelection();
		List result= new ArrayList(items.length);
		for (int i= 0; i < items.length; i++) {
			Object data= items[i].getData();
			if (data instanceof TypeInfo) {
				result.add(data);
			}
		}
		return (TypeInfo[])result.toArray(new TypeInfo[result.size()]);
	}
	
	public void stop() {
		stop(true);
	}
	
	public void stop(boolean stopSyncJob) {
		if (fSyncJob != null && stopSyncJob) {
			fSyncJob.stop();
		}
		if (fSearchJob != null) {
			fSearchJob.stop();
		}
	}
	
	public void setSearchPattern(String text) {
		stop(false);
		if (text.length() == 0 || "*".equals(text)) { //$NON-NLS-1$
			reset();
		} else {
			fTypeInfoFilter= new TypeInfoFilter(text);
			scheduleSearchJob(isSyncJobRunning() ? HISTORY : FULL);
		}
	}
	
	public void reset() {
		if (VIRTUAL) {
			TypeInfo[] types= fHistory.getTypeInfos();
			fElements= new Object[types.length];
			System.arraycopy(types, 0, fElements, 0, types.length);
			fTable.setItemCount(fElements.length);
			// bug under windows.
			if (fElements.length == 0) {
				fTable.redraw();
			}
			fTable.clear(0, fElements.length - 1);
		} else {
			fNextElement= 0;
			fLastSelection= null;
			fLastLabels= null;
			fTypeInfoFilter= null;
			TypeInfo[] historyItems= fHistory.getTypeInfos();
			if (historyItems.length == 0) {
				shortenTable();
				return;
			}
			int lastIndex= historyItems.length - 1;
			TypeInfo last= null;
			TypeInfo type= historyItems[0];
			for (int i= 0; i < historyItems.length; i++) {
				TypeInfo next= i == lastIndex ? null : historyItems[i + 1];
				addSingleElement(type,
					fLabelProvider.getImage(type),
					fLabelProvider.getText(last, type, next));
				last= type;
				type= next;
			}
			shortenTable();
		}
	}
	
	private void rememberResult(int ticket, final TypeInfo[] result) {
		syncExec(ticket, new Runnable() {
			public void run() {
				if (fLastCompletedResult == null) {
					fLastCompletedFilter= fTypeInfoFilter;
					fLastCompletedResult= result;
				}
			}
		});
	}
	
	private void scheduleSearchJob(int mode) {
		fSearchJobTicket++;
		if (fLastCompletedFilter != null && fTypeInfoFilter.isSubFilter(fLastCompletedFilter.getText())) {
			fSearchJob= new CachedResultJob(fSearchJobTicket, fLastCompletedResult, this, fTypeInfoFilter, 
				fHistory.getTypeInfos(), fNumberOfVisibleItems, 
				mode);
		} else {
			fSearchJob= new SearchEngineJob(fSearchJobTicket, this, fTypeInfoFilter, 
				fHistory.getTypeInfos(), fNumberOfVisibleItems, 
				mode, fSearchScope, fElementKind);
		}
		fSearchJob.schedule();
	}
	
	//-- Search result updating ----------------------------------------------------
	
	private void clear(int ticket) {
		syncExec(ticket, new Runnable() {
			public void run() {
				fNextElement= 0;
				fLastSelection= null;
				fLastLabels= null;
			}
		});
	}
	
	private void add(int ticket, final Object element) {
		syncExec(ticket, new Runnable() {
			public void run() {
				addSingleElement(element);
			}
		});
	}
	
	private void addAll(int ticket, final List elements, final List images, final List labels) {
		syncExec(ticket, new Runnable() {
			public void run() {
				int size= elements.size();
				for(int i= 0; i < size; i++) {
					addSingleElement(elements.get(i), (Image)images.get(i), (String)labels.get(i));
				}
			}
		});
	}
	
	private void addDashLine(int ticket) {
		add(ticket, fDashLine);
	}
	
	private void addSingleElement(final Object element) {
		TableItem item= null;
		if (fItems.size() > fNextElement) {
			item= (TableItem)fItems.get(fNextElement);
		} else {
			item= new TableItem(fTable, SWT.NONE);
			fItems.add(item);
		}
		fillItem(item, element);
		item.setData(element);
		fNextElement++;
		if (fNextElement == 1) {
			fTable.setSelection(0);
            fTable.notifyListeners(SWT.Selection, new Event());
		}
	}
	
	private void addSingleElement(Object element, Image image, String label) {
		TableItem item= null;
		if (fItems.size() > fNextElement) {
			item= (TableItem)fItems.get(fNextElement);
			item.setForeground(fTable.getForeground());
		} else {
			item= new TableItem(fTable, SWT.NONE);
			fItems.add(item);
		}
		item.setImage(image);
		item.setText(label);
		item.setData(element);
		fNextElement++;
		if (fNextElement == 1) {
			fTable.setSelection(0);
            fTable.notifyListeners(SWT.Selection, new Event());
		}
	}
	
	private void done(int ticket) {
		syncExec(ticket, new Runnable() {
			public void run() {
				fProgressLabel.setText(""); //$NON-NLS-1$
				shortenTable();
				fSearchJob= null;
			}
		});
	}
	
	private void canceled(int ticket, final boolean removePendingItems) {
		syncExec(ticket, new Runnable() {
			public void run() {
				fProgressLabel.setText(""); //$NON-NLS-1$
				if (removePendingItems) {
					shortenTable();
				}
				fSearchJob= null;
			}
		});
	}
	
	private synchronized void failed(int ticket, CoreException e) {
		syncExec(ticket, new Runnable() {
			public void run() {
				fProgressLabel.setText(""); //$NON-NLS-1$
				shortenTable();
				fSearchJob= null;
			}
		});
	}
	
	//-- virtual table support -------------------------------------------------------
	
	private void setHistoryResult(int ticket, final TypeInfo[] types) {
		syncExec(ticket, new Runnable() {
			public void run() {
				fElements= new Object[types.length];
				System.arraycopy(types, 0, fElements, 0, types.length);
				fTable.setItemCount(fElements.length);
				// bug under windows.
				if (fElements.length == 0) {
					fTable.redraw();
				}
				fTable.clear(0, fElements.length - 1);
			}
		});
	}
	
	private void setSearchResult(int ticket, final TypeInfo[] types) {
		syncExec(ticket, new Runnable() {
			public void run() {
				Object[] currentElements= fElements;
				int dash= (currentElements.length > 0 && types.length > 0) ? 1 : 0;
				fElements= new Object[currentElements.length + types.length + dash];
				System.arraycopy(currentElements, 0, fElements, 0, currentElements.length);
				if (dash != 0)
					fElements[currentElements.length]= fDashLine;
				System.arraycopy(types, 0, fElements, currentElements.length + dash, types.length);
				fTable.setItemCount(fElements.length);
				// bug under windows.
				if (fElements.length == 0) {
					fTable.redraw();
				}
				fTable.clear(currentElements.length, fElements.length - 1);
			}
		});
	}
	
	private void setData(TableItem item) {
		int index= fTable.indexOf(item);
		Object element= fElements[index];
		item.setData(element);
		if (element instanceof DashLine) {
			fillDashLine(item);
		} else {
			item.setImage(fLabelProvider.getImage(element));
			item.setText(fLabelProvider.getText(
				getLastTypeInfo(index), 
				(TypeInfo)element, 
				getNextTypeInfo(index)));
			item.setForeground(fTable.getForeground());
		}
	}
	
	private TypeInfo getLastTypeInfo(int index) {
		if (index <= 0)
			return null;
		Object result= fElements[--index];
		if (result instanceof DashLine) {
			result= fElements[--index];
		}
		return (TypeInfo)result;
	}
	
	private TypeInfo getNextTypeInfo(int index) {
		if (++index >= fElements.length) 
			return null;
		Object result= fElements[index];
		if (result instanceof DashLine) {
			result= fElements[++index];
		}
		return (TypeInfo)result;
	}
	
	//-- Sync Job updates ------------------------------------------------------------
	
	private void syncJobDone() {
		syncExec(new Runnable() {
			public void run() {
				fSyncJob= null;
				fProgressLabel.setText(""); //$NON-NLS-1$
				if (fTypeInfoFilter != null) {
					scheduleSearchJob(FULL);
				}
			}
		});
	}

	private boolean isSyncJobRunning() {
		return fSyncJob != null;
	}
	
	//-- progress monitor updates -----------------------------------------------------
	
	private void showProgress(final String text) {
		syncExec(new Runnable() {
			public void run() {
				fProgressLabel.setText(text);
			}
		});
	}
	
	private void progressDone() {
		syncExec(new Runnable() {
			public void run() {
				fProgressLabel.setText(""); //$NON-NLS-1$
			}
		});
	}
	
	//-- Helper methods --------------------------------------------------------------
	
	private void syncExec(final Runnable runnable) {
		if (fDisplay.isDisposed()) 
			return;
		fDisplay.syncExec(new Runnable() {
			public void run() {
				if (fTable.isDisposed())
					return;
				runnable.run();
			}
		});
	}
	
	private void syncExec(final int ticket, final Runnable runnable) {
		if (fDisplay.isDisposed()) 
			return;
		fDisplay.syncExec(new Runnable() {
			public void run() {
				if (fTable.isDisposed() || ticket != fSearchJobTicket)
					return;
				runnable.run();
			}
		});
	}
	
	private void fillItem(TableItem item, Object element) {
		if (element instanceof DashLine) {
			fillDashLine(item);
		} else {
			item.setImage(fLabelProvider.getImage(element));
			item.setText(fLabelProvider.getText(element));
			item.setForeground(fTable.getForeground());
		}
		item.setData(element);
	}

	private void fillDashLine(TableItem item) {
		Rectangle bounds= item.getImageBounds(0);
		Rectangle clientArea= fTable.getClientArea();
		item.setText(fDashLine.getText(clientArea.width - bounds.x - bounds.width - 20));
		item.setImage((Image)null);
		item.setForeground(fDashLineColor);
	}

	private void shortenTable() {
        if (fNextElement < fItems.size()) {
            fTable.setRedraw(false);
            fTable.remove(fNextElement, fItems.size() - 1);
            fTable.setRedraw(true);
        }
		for (int i= fItems.size() - 1; i >= fNextElement; i--) {
			fItems.remove(i);
		}
	}
	
	private Color computeDashLineColor() {
		Color fg= fTable.getForeground();
		int fGray= (int)(0.3*fg.getRed() + 0.59*fg.getGreen() + 0.11*fg.getBlue());
		Color bg= fTable.getBackground();
		int bGray= (int)(0.3*bg.getRed() + 0.59*bg.getGreen() + 0.11*bg.getBlue());
		int gray= (int)((fGray + bGray) * 0.66);
		return new Color(fDisplay, gray, gray, gray);
	}
	
	private int computeScrollBarWidth() {
		Composite t= new Composite(fTable.getShell(), SWT.V_SCROLL);            
		int result= t.computeTrim(0, 0, 0, 0).width;
		t.dispose();
		return result;
	}
	
	private static int computeFlags(int elementKind) {
		int result= 0;
		switch (elementKind) {
			case IJavaSearchConstants.TYPE:
				return Flags.AccAnnotation | Flags.AccEnum | Flags.AccInterface;
			case IJavaSearchConstants.CLASS:
				return 0;
				
		}
		return result;
	}
}