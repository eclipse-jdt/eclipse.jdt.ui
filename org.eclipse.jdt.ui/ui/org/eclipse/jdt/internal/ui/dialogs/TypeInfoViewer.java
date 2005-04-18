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
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;

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
		private boolean fIgnoreCase= true;
		
		private StringMatcher fPackageMatcher;
		private String fPackagePattern;
		
		private String fNamePattern;
		private String fCamelCasePattern;
		private StringMatcher fNameMatcher;
		
		private static final char END_SYMBOL= '<';
		private static final char ANY_STRING= '*';
		
		public TypeInfoFilter(String pattern) {
			int index= pattern.lastIndexOf("."); //$NON-NLS-1$
			if (index == -1) {
				createNamePattern(pattern);
			} else {
				fPackagePattern= pattern.substring(0, index);
				fPackageMatcher= new StringMatcher(fPackagePattern, fIgnoreCase, false);
				
				createNamePattern(pattern.substring(index + 1));
			}
			fNameMatcher= new StringMatcher(fNamePattern, fIgnoreCase, false);			
		}
		public String getPackagePattern() {
			return fPackagePattern;
		}
		public String getNamePattern() {
			return fNamePattern;
		}
		public int getSearchFlags() {
			int result= 0;
			if (!fIgnoreCase) {
				result= result | SearchPattern.R_CASE_SENSITIVE;
			}
			if (fNamePattern.indexOf("*") != -1) { //$NON-NLS-1$
				result= result | SearchPattern.R_PATTERN_MATCH;
			}
			return result;
		}
		public boolean matchSearchElement(TypeInfo type) {
			if (fCamelCasePattern == null)
				return true;
			String name= type.getTypeName();
			StringBuffer camcelCase= new StringBuffer();
			for (int i= 0; i < name.length(); i++) {
				char c= name.charAt(i);
				if (Character.isUpperCase(c))
					camcelCase.append(c);
			}
			return fCamelCasePattern.equals(camcelCase.toString());
		}
		public boolean matchHistroyElement(TypeInfo info) {
			if (!fNameMatcher.match(info.getTypeName()))
				return false;

			if (fPackageMatcher == null)
				return true;

			return fPackageMatcher.match(info.getTypeContainerName());
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
					if (index == length) {
						fIgnoreCase= false;
						fNamePattern= patternBuffer.toString();
						fCamelCasePattern= camelCaseBuffer.toString();
					} else if (restIsLowerCase(text, index)) {
						fIgnoreCase= false;
						fNamePattern= patternBuffer.append(text.substring(index)).toString();
						fCamelCasePattern= camelCaseBuffer.toString();
					}
				}
				length= fNamePattern.length();
				switch (fNamePattern.charAt(length - 1)) {
					case END_SYMBOL:
						fNamePattern= fNamePattern.substring(0, length - 1);
						break;
					case ANY_STRING:
						break;
					default:
						fNamePattern= fNamePattern + ANY_STRING;
				}
			}
		}
		private boolean restIsLowerCase(String s, int start) {
			for (int i= start; i < s.length(); i++) {
				if (Character.isUpperCase(s.charAt(i)))
					return false;
			}
			return true;
		}
	}
	
	private static class SearchRequestor extends TypeNameRequestor {
		private Set fHistory;
		private TypeInfoFilter fFilter;
		private volatile boolean fStop;
		
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
			TypeInfo info= factory.create(packageName, simpleTypeName, enclosingTypeNames, modifiers, path);
			if (!fHistory.contains(info) && fFilter.matchSearchElement(info))
				fResult.add(info);
		}
	}
	
	private static class TypeInfoComparator implements Comparator {
	    public int compare(Object left, Object right) {
	     	TypeInfo leftInfo= (TypeInfo)left;
	     	TypeInfo rightInfo= (TypeInfo)right;
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
		private int fTicket;
		private boolean fSyncMode;
		private TypeInfoViewer fViewer;
		private String fName;
		private int fTotalWork;
		private double fWorked;
		private long fLastUpdate= -1;
		
		public ProgressMonitor(IProgressMonitor monitor, TypeInfoViewer viewer, boolean syncMode, int ticket) {
			super(monitor);
			fViewer= viewer;
			fSyncMode= syncMode;
			fTicket= ticket;
		}
		public void beginTask(String name, int totalWork) {
			super.beginTask(name, totalWork);
			fName= name;
			fTotalWork= totalWork;
			fLastUpdate= System.currentTimeMillis();
		}
		public void worked(int work) {
			super.worked(work);
			internalWorked(work);
		}
		public void done() {
			if (fSyncMode) {
				fViewer.syncProgressDone(fTicket);
			} else {
				fViewer.progressDone(fTicket);
			}
			super.done();
		}
		public void internalWorked(double work) {
			fWorked= fWorked + work;
			if (System.currentTimeMillis() - fLastUpdate >= 200) {
				String message= Messages.format(
					"{0} ({1}%)",
					new Object[] { fName, new Integer((int)((fWorked * 100) / fTotalWork)) });
				fViewer.showProgress(fTicket, message);
				fLastUpdate= System.currentTimeMillis();
			}
		}
	}

	private static class SearchJob extends Job {
		private boolean fFullMode;
		
		private int fTicket;
		private TypeInfoViewer fViewer;
		private TypeInfoLabelProvider fLabelProvider;
		
		private TypeInfo[] fHistory;
		private TypeInfoFilter fFilter;
		
		private SearchRequestor fReqestor;
		
		public SearchJob(TypeInfoViewer viewer, TypeInfoFilter filter, TypeInfo[] history, int numberOfVisibleItems, boolean full, int ticket) {
			super(JavaUIMessages.TypeInfoViewer_job_label);
			fFullMode= full;
			fTicket= ticket;
			fViewer= viewer;
			fLabelProvider= fViewer.getLabelProvider();
			fFilter= filter;
			fHistory= history;
			fReqestor= new SearchRequestor(filter);
			setSystem(true);
		}
		public void stop() {
			fReqestor.cancel();
			cancel();
		}
		protected IStatus run(IProgressMonitor monitor) {
			try {
				return internalRun(monitor);
			} catch (CoreException e) {
				fViewer.failed(fTicket, e);
				return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR, JavaUIMessages.TypeInfoViewer_job_error, e);
			} catch (InterruptedException e) {
				return canceled(e, true);
			} catch (OperationCanceledException e) {
				return canceled(e, false);
			}
		}
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
			if (!fFullMode) {
				fViewer.done(fTicket);
				return ok();
			}
			fReqestor.setHistory(filteredHistory);
			TypeInfo[] result= getSearchResult(monitor);
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
				if (DEBUG)
					System.out.println("Sleeping for: " + sleep); //$NON-NLS-1$
				
				if (sleep > 0)
					Thread.sleep(sleep);
				
				if (monitor.isCanceled())
					return canceled(null, false);
			}
			fViewer.done(fTicket);
			return ok();
		}
		private List getFilteredHistory() {
			List matchingTypes= new ArrayList(fHistory.length);
			for (int i= 0; i < fHistory.length; i++) {
				TypeInfo type= fHistory[i];
				if (fFilter.matchHistroyElement(type) && !TypeFilter.isFiltered(type.getFullyQualifiedName())) {
					matchingTypes.add(type);
				}
			}
			return matchingTypes;
		}
		private TypeInfo[] getSearchResult(IProgressMonitor monitor) throws CoreException {
			long start= System.currentTimeMillis();
			SearchEngine engine= new SearchEngine();
			String packPattern= fFilter.getPackagePattern();
			engine.searchAllTypeNames(
				packPattern == null ? null : packPattern.toCharArray(), 
				fFilter.getNamePattern().toCharArray(), 
				fFilter.getSearchFlags(), 
				IJavaSearchConstants.TYPE, 
				SearchEngine.createWorkspaceScope(), 
				fReqestor, 
				IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, 
				new ProgressMonitor(monitor, fViewer, false, fTicket));
			if (DEBUG)
				System.out.println("Time needed until search has finished: " + (System.currentTimeMillis() - start)); //$NON-NLS-1$
			TypeInfo[] result= fReqestor.getResult();
			Arrays.sort(result, new TypeInfoComparator());
			if (DEBUG)
				System.out.println("Time needed until sort has finished: " + (System.currentTimeMillis() - start)); //$NON-NLS-1$
			return result;
		}
		private IStatus canceled(Exception e, boolean removePendingItems) {
			fViewer.canceled(fTicket, removePendingItems);
			return new Status(IStatus.CANCEL, JavaPlugin.getPluginId(), IStatus.CANCEL, JavaUIMessages.TypeInfoViewer_job_cancel, e);
		}
		private IStatus ok() {
			return new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
		}
	}
	
	private static class SyncJob extends Job {
		private int fTicket;
		private TypeInfoViewer fViewer;
		public SyncJob(TypeInfoViewer viewer, int ticket) {
			super("Synchronizing tables");
			fViewer= viewer;
			fTicket= ticket;
		}
		public void stop() {
			super.cancel();
		}
		protected IStatus run(IProgressMonitor parent) {
			ProgressMonitor monitor= new ProgressMonitor(parent, fViewer, true, fTicket);
			try {
				/*
				monitor.beginTask("Synchronizing tables...", 100);
				for (int i= 0; i < 100; i++) {
					monitor.worked(1);
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
					}
				}
				*/
				new SearchEngine().searchAllTypeNames(
					null, 
					// make sure we search a concrete name. This is faster according to Kent  
					"_______________".toCharArray(), //$NON-NLS-1$
					SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE, 
					IJavaSearchConstants.CLASS,
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
				monitor.done();
			}
			return new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
		}
	}

	private static class DashLine {
		public int fCharWidth;
		public String getText(int width) {
			StringBuffer buffer= new StringBuffer();
			for (int i= 0; i < width / fCharWidth; i++) {
				buffer.append(MDASH);
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
	private DashLine fDashLine= new DashLine();
	/* remembers the last selection to restore unqualified labels */
	private TableItem[] fLastSelection;
	private String[] fLastLabels;
	private Table fTable;
	
	private SyncJob fSyncJob;
	
	private TypeInfoFilter fTypeInfoFilter;
	private int fJobTicket;
	private SearchJob fSearchJob;

	// private static final char MDASH= '—';
	private static final char MDASH= '\u2012';    // figure dash  
	// private static final char MDASH= '\u2013';    // en dash      
	// private static final char MDASH= '\u2014';    // em dash <<=== works      
	// private static final char MDASH= '\u2015';    // horizontal bar
	
	private static final boolean DEBUG= false;	
	
	public TypeInfoViewer(Composite parent, Label progressLabel) {
		fDisplay= parent.getDisplay();
		fProgressLabel= progressLabel;
		fTable= new Table(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.FLAT | SWT.MULTI);
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
		GC gc= null;
		try {
			gc= new GC(fTable);
			fDashLine.setCharWidth(gc.getCharWidth(MDASH));
		} finally {
			gc.dispose();
		}
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
		fHistory= TypeInfoHistory.getInstance();
		fSyncJob= new SyncJob(this, ++fJobTicket);
		fSyncJob.schedule();
	}
	
	public Table getTable() {
		return fTable;
	}
	
	/* Method is called from withing the UI thread */
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
	
	/* Method is called from withing the UI thread */
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
	
	/* Method is called from withing the UI thread */
	public void setSearchPattern(String text) {
		stop(false);
		if (text.length() == 0 || "*".equals(text)) { //$NON-NLS-1$
			reset();
		} else {
			fTypeInfoFilter= new TypeInfoFilter(text);
			scheduleSearchJob(!isSyncJobRunning());
		}
	}
	
	public void setFocus() {
		fTable.setFocus();
	}
	
	private TypeInfoLabelProvider getLabelProvider() {
		return fLabelProvider;
	}
	
	private int getNumberOfVisibleItems() {
		return fNumberOfVisibleItems;
	}
	
	private void clear(int ticket) {
		syncExec(ticket, new Runnable() {
			public void run() {
				fNextElement= 0;
				fLastSelection= null;
				fLastLabels= null;
			}
		});
	}
	
	private void addDashLine(int ticket) {
		add(ticket, fDashLine);
	}
	
	private void add(int ticket, final Object element) {
		syncExec(ticket, new Runnable() {
			public void run() {
				addSingleElement(element);
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
	
	private void addSingleElement(Object element, Image image, String label) {
		TableItem item= null;
		if (fItems.size() > fNextElement) {
			item= (TableItem)fItems.get(fNextElement);
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
	
	public void reset() {
		fNextElement= 0;
		fLastSelection= null;
		fLastLabels= null;
		fTypeInfoFilter= null;
		TypeInfo[] historyItems= fHistory.getTypeInfos();
		if (historyItems.length == 0)
			return;
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
	
	private void canceled(int ticket, final boolean removePendingItems) {
		syncExec(ticket, new Runnable() {
			public void run() {
				if (removePendingItems) {
					shortenTable();
				}
				fSearchJob= null;
			}
		});
	}
	
	private void done(int ticket) {
		syncExec(ticket, new Runnable() {
			public void run() {
				shortenTable();
				fSearchJob= null;
			}
		});
	}
	
	private synchronized void failed(int ticket, CoreException e) {
		syncExec(ticket, new Runnable() {
			public void run() {
				shortenTable();
				fSearchJob= null;
			}
		});
	}
	
	private boolean isSyncJobRunning() {
		return fSyncJob != null;
	}
	
	private void showProgress(int ticket, final String text) {
		syncExec(ticket, new Runnable() {
			public void run() {
				fProgressLabel.setText(text);
			}
		});
	}
	
	private void syncProgressDone(int ticket) {
		syncExec(ticket, new Runnable() {
			public void run() {
				fSyncJob= null;
				fProgressLabel.setText(""); //$NON-NLS-1$
				if (fTypeInfoFilter != null) {
					scheduleSearchJob(true);
				}
			}
		});
	}
	
	private void progressDone(int ticket) {
		syncExec(ticket, new Runnable() {
			public void run() {
				fProgressLabel.setText(""); //$NON-NLS-1$
			}
		});
	}
	private void scheduleSearchJob(boolean fullMode) {
		fJobTicket++;
		fSearchJob= new SearchJob(this, fTypeInfoFilter, 
			fHistory.getTypeInfos(), fNumberOfVisibleItems, 
			fullMode, fJobTicket);
		fSearchJob.schedule();
	}
	
	private void syncExec(final int ticket, final Runnable runnable) {
		if (fDisplay.isDisposed()) 
			return;
		fDisplay.syncExec(new Runnable() {
			public void run() {
				if (fTable.isDisposed() || ticket != fJobTicket)
					return;
				runnable.run();
			}
		});
	}
	
	private void fillItem(TableItem item, Object element) {
		if (element instanceof DashLine) {
			Rectangle bounds= item.getImageBounds(0);
			Rectangle clientArea= fTable.getClientArea();
			item.setText(fDashLine.getText(clientArea.width - bounds.x - bounds.width - 20));
			item.setImage((Image)null);
			item.setGrayed(true);
		} else {
			item.setImage(fLabelProvider.getImage(element));
			item.setText(fLabelProvider.getText(element));
		}
		item.setData(element);
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
}

/*
import org.eclipse.swt.SWT; import org.eclipse.swt.widgets.Composite; 
import org.eclipse.swt.widgets.Display; 
import org.eclipse.swt.widgets.Shell; 
public class ScrollBarWidth {             
public static void main(String[] args) {               
Display d= new Display();                       
Shell s= new Shell(d);                          
Composite t= new Composite(s, SWT.V_SCROLL);            
int scrollbarwidth= t.computeTrim(0, 0, 0, 0).width;                            
System.out.println(scrollbarwidth);     } 
}
*/