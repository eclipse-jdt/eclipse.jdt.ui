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
import org.eclipse.jdt.internal.corext.util.TypeInfoFilter;
import org.eclipse.jdt.internal.corext.util.TypeInfoHistory;
import org.eclipse.jdt.internal.corext.util.UnresolvableTypeInfo;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIMessages;

/**
 * A viewer to present type queried form the type history and form the
 * search engine. All viewer updating takes place in the UI thread.
 * Therefore no synchronization of the methods is necessary.
 * 
 * @since 3.1
 */
public class TypeInfoViewer {
	
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
		
		protected TypeInfoFilter fFilter;
		protected TypeInfoHistory fHistory;
		
		protected AbstractSearchJob(int ticket, TypeInfoViewer viewer, TypeInfoFilter filter, TypeInfoHistory history, int numberOfVisibleItems, int mode) {
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
			TypeInfo[] matchingTypes= fHistory.getFilteredTypeInfos(fFilter);
			if (matchingTypes.length > 0) {
				type= matchingTypes[0];
				int i= 1;
				while(type != null) {
					next= (i == matchingTypes.length) ? null : matchingTypes[i];
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
			fViewer.fExpectedItemCount= elements.size();
			fViewer.addAll(fTicket, elements, images, labels);
			
			if ((fMode & INDEX) == 0) {
				fViewer.done(fTicket);
				return ok();
			}
			TypeInfo[] result= getSearchResult(filteredHistory, monitor);
			fViewer.fExpectedItemCount+= result.length;
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

			TypeInfo[] matchingTypes= fHistory.getFilteredTypeInfos(fFilter);
			fViewer.setHistoryResult(fTicket, matchingTypes);
			if ((fMode & INDEX) == 0) {
				fViewer.done(fTicket);
				return ok();
			}
			TypeInfo[] result= getSearchResult(new HashSet(Arrays.asList(matchingTypes)), monitor);
			if (monitor.isCanceled())
				return canceled(null, false);			
			fViewer.setSearchResult(fTicket, result);
			fViewer.done(fTicket);
			return ok();
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
		
		public SearchEngineJob(int ticket, TypeInfoViewer viewer, TypeInfoFilter filter, TypeInfoHistory history, int numberOfVisibleItems, int mode, 
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
		public CachedResultJob(int ticket, TypeInfo[] lastResult, TypeInfoViewer viewer, TypeInfoFilter filter, TypeInfoHistory history, int numberOfVisibleItems, int mode) {
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
			for (int i= 0; i < (width / fCharWidth) - 2; i++) {
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

	/* non virtual table */
	private int fNextElement;
	private List fItems;
	
	/* virtual table */
	private TypeInfo[] fHistoryMatches;
	private TypeInfo[] fSearchMatches;
	
	private int fNumberOfVisibleItems;
	private int fExpectedItemCount;
	private Color fDashLineColor; 
	private int fScrollbarWidth;
	private int fTableWidthDelta;
	private DashLine fDashLine= new DashLine();
	
	/* remembers the last selection to restore unqualified labels */
	private TableItem[] fLastSelection;
	private String[] fLastLabels;
	
	private TypeInfoLabelProvider fLabelProvider;
	private Table fTable;
	
	private SyncJob fSyncJob;
	
	private TypeInfoFilter fTypeInfoFilter;
	private TypeInfo[] fLastCompletedResult;
	private TypeInfoFilter fLastCompletedFilter;
	
	private IJavaSearchScope fSearchScope;
	private int fElementKind;
	private int fSearchJobTicket;
	
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
	
	private static final boolean DEBUG= false;	
	private static final boolean VIRTUAL= false;
	
	private static final TypeInfo[] EMTPY_TYPE_INFO_ARRAY= new TypeInfo[0];
	private static final TypeInfo DASH_LINE= new UnresolvableTypeInfo(null, null, null, 0, null);
	
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
			fHistoryMatches= EMTPY_TYPE_INFO_ARRAY;
			fSearchMatches= EMTPY_TYPE_INFO_ARRAY;
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
		fScrollbarWidth= computeScrollBarWidth();
		fTableWidthDelta= fTable.computeTrim(0, 0, 0, 0).width - fScrollbarWidth;
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
		fLastSelection= null;
		fLastLabels= null;
		fTypeInfoFilter= null;
		fExpectedItemCount= 0;
		if (VIRTUAL) {
			fHistoryMatches= fHistory.getFilteredTypeInfos(fTypeInfoFilter);
			fExpectedItemCount= fHistoryMatches.length;
			fTable.setItemCount(fHistoryMatches.length);
			// bug under windows.
			if (fHistoryMatches.length == 0) {
				fTable.redraw();
			}
			fTable.clear(0, fHistoryMatches.length - 1);
		} else {
			fNextElement= 0;
			TypeInfo[] historyItems= fHistory.getFilteredTypeInfos(fTypeInfoFilter);
			if (historyItems.length == 0) {
				shortenTable();
				return;
			}
			fExpectedItemCount= historyItems.length;
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
				fHistory, fNumberOfVisibleItems, 
				mode);
		} else {
			fSearchJob= new SearchEngineJob(fSearchJobTicket, this, fTypeInfoFilter, 
				fHistory, fNumberOfVisibleItems, 
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
				fExpectedItemCount= 0;
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
		syncExec(ticket, new Runnable() {
			public void run() {
				addSingleElement(fDashLine);
			}
		});
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
				fExpectedItemCount= types.length;
				int lastHistoryLength= fHistoryMatches.length;
				fHistoryMatches= types;
				int length= fHistoryMatches.length + fSearchMatches.length; 
				int dash= (fHistoryMatches.length > 0 && fSearchMatches.length > 0) ? 1 : 0;
				fTable.setItemCount(length + dash);
				if (length == 0) {
					// bug under windows.
					fTable.redraw();
					return;
				} 
				int update= Math.max(lastHistoryLength, fHistoryMatches.length);
				if (update > 0) {
					fTable.clear(0, update + dash - 1);
				}
			}
		});
	}
	
	private void setSearchResult(int ticket, final TypeInfo[] types) {
		syncExec(ticket, new Runnable() {
			public void run() {
				fExpectedItemCount+= types.length;
				fSearchMatches= types;
				int length= fHistoryMatches.length + fSearchMatches.length; 
				int dash= (fHistoryMatches.length > 0 && fSearchMatches.length > 0) ? 1 : 0;
				fTable.setItemCount(length + dash);
				if (length == 0) {
					// bug under windows.
					fTable.redraw();
					return;
				}
				if (fHistoryMatches.length == 0) {
					fTable.clear(0, length + dash - 1);
				} else {
					fTable.clear(fHistoryMatches.length - 1, length + dash - 1);
				}
			}
		});
	}
	
	private void setData(TableItem item) {
		int index= fTable.indexOf(item);
		TypeInfo type= getTypeInfo(index);
		if (type == DASH_LINE) {
			item.setData(fDashLine);
			fillDashLine(item);
		} else {
			item.setData(type);
			item.setImage(fLabelProvider.getImage(type));
			item.setText(fLabelProvider.getText(
				getTypeInfo(index - 1), 
				type, 
				getTypeInfo(index + 1)));
			item.setForeground(fTable.getForeground());
		}
	}
	
	private TypeInfo getTypeInfo(int index) {
		if (index < 0)
			return null;
		if (index < fHistoryMatches.length) {
			return fHistoryMatches[index];
		}
		int dash= (fHistoryMatches.length > 0 && fSearchMatches.length > 0) ? 1 : 0;
		if (index == fHistoryMatches.length && dash == 1) {
			return DASH_LINE;
		}
		index= index - fHistoryMatches.length - dash;
		if (index >= fSearchMatches.length)
			return null;
		return fSearchMatches[index];
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
		Rectangle area= fTable.getBounds();
		boolean willHaveScrollBar= fExpectedItemCount + 1 > fNumberOfVisibleItems;
		item.setText(fDashLine.getText(area.width - bounds.x - bounds.width - fTableWidthDelta - 
			(willHaveScrollBar ? fScrollbarWidth : 0)));
		item.setImage((Image)null);
		item.setForeground(fDashLineColor);
	}

	private void shortenTable() {
		if (VIRTUAL)
			return;
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