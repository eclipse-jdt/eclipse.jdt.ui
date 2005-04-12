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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;

import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.TypeInfo;
import org.eclipse.jdt.internal.corext.util.TypeInfoFactory;
import org.eclipse.jdt.internal.corext.util.TypeInfoHistory;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.StringMatcher;

public class TypeInfoViewer2 {
	
	private static class TypeInfoFilter {
		private boolean fIgnoreCase= true;
		
		private StringMatcher fPackageMatcher;
		private String fPackagePattern;
		
		private String fNamePattern;
		private StringMatcher fNameMatcher;
		
		private static final char END_SYMBOL= '<';
		private static final char ANY_STRING= '*';
		
		public TypeInfoFilter(String pattern) {
			int qualifierIndex= pattern.lastIndexOf("."); //$NON-NLS-1$
			if (qualifierIndex == -1) {
				fNamePattern= createPattern(pattern);
			} else {
				fPackagePattern= pattern.substring(0, qualifierIndex);
				fNamePattern= createPattern(pattern.substring(qualifierIndex + 1));
				fPackageMatcher= new StringMatcher(fPackagePattern, fIgnoreCase, false);
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
		public boolean matchHistroyElement(TypeInfo info) {
			if (!fNameMatcher.match(info.getTypeName()))
				return false;

			if (fPackageMatcher == null)
				return true;

			return fPackageMatcher.match(info.getTypeContainerName());
		}
		private String createPattern(final String text) {
			int length= text.length();
			String result= text;
			if (length > 0) {
				if (length > 1) {
					char last= text.charAt(0);
					if (Character.isUpperCase(last)) {
						StringBuffer buffer= new StringBuffer();
						buffer.append(last);
						for (int i= 1; i < length; i++) {
							char c= text.charAt(i);
							if (Character.isUpperCase(last) && Character.isUpperCase(c)) {
								buffer.append("*"); //$NON-NLS-1$
							} 
							buffer.append(c);
							last= c;
						}
						String s= buffer.toString();
						if (!text.equals(s)) {
							fIgnoreCase= false;
							result= s;
						}
					}
				}
				length= result.length();
				switch (result.charAt(length - 1)) {
					case END_SYMBOL:
						result= result.substring(0, length - 1);
						break;
					case ANY_STRING:
						break;
					default:
						result= result + ANY_STRING;
				}
			}
			return result;
		}
	}
	
	private static class Data {
		public int modifier;
		public char[] name;
		public char[] packageName;
		public char[][] enclosingNames;
		public String path;
		public Data(int modifier, char[] name, char[] packageName, char[][] enclosingNames, String path) {
			super();
			this.modifier= modifier;
			this.name= name;
			this.packageName= packageName;
			this.enclosingNames= enclosingNames;
			this.path= path;
		}
	}
	
	/* 
	 * No need to synchronize this class. The synchronization takes place via the 
	 * add/done/cancel/failure methods on the viewer.
	 */
	private static class SearchRequestor extends TypeNameRequestor {
		private List fResult;
		
		private TypeInfoFactory factory= new TypeInfoFactory();
		private volatile boolean fStop;
		private Set fHistory;
		
		public SearchRequestor() {
			super();
			fResult= new ArrayList(500);
		}
//		public TypeInfo[] getResult() {
//			return (TypeInfo[])fResult.toArray(new TypeInfo[fResult.size()]);
//		}
		public Data[] getResult() {
			return (Data[])fResult.toArray(new Data[fResult.size()]);
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
			fResult.add(new Data(modifiers, simpleTypeName, packageName, enclosingTypeNames, path));
//			TypeInfo info= factory.create(packageName, simpleTypeName, enclosingTypeNames, modifiers, path);
//			if (!fHistory.contains(info) && !TypeFilter.isFiltered(packageName, simpleTypeName))
//				fResult.add(info);
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
		
		private static final Image CLASS_ICON= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_CLASS);
		private static final Image ANNOTATION_ICON= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ANNOTATION);
		private static final Image INTERFACE_ICON= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_INTERFACE);
		private static final Image ENUM_ICON= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ENUM);

		private String fLastLabel;
		
		public void reset() {
			fLastLabel= null;
		}
		public String getText(Object element) {
			TypeInfo info= (TypeInfo)element;
			StringBuffer result= new StringBuffer();
			result.append(info.getTypeName());
			String pack= info.getPackageName();
			if (pack != null && pack.length() > 0) {
				result.append(JavaElementLabels.CONCAT_STRING);
				result.append(pack);
			}
			String current= result.toString();
			if (fLastLabel != null && fLastLabel.equals(current)) {
				result.append(JavaElementLabels.CONCAT_STRING);
				result.append(info.getPackageFragmentRootPath().toString());
				fLastLabel= result.toString();
			} else {
				fLastLabel= current;
			}
			return fLastLabel;
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
	
	private static class SearchJob extends Job {
		private TypeInfoViewer2 fViewer;
		private ILabelProvider fLabelProvider;
		
		
		private TypeInfo[] fHistory;
		private TypeInfoFilter fFilter;
		
		private SearchRequestor fReqestor;
		
		public SearchJob(TypeInfoViewer2 viewer, TypeInfoFilter filter, TypeInfo[] history, int numberOfVisibleItems) {
			super("Searching for all types");
			fViewer= viewer;
			fLabelProvider= fViewer.getLabelProvider();
			fFilter= filter;
			fHistory= history;
			
			fReqestor= new SearchRequestor();
			setSystem(true);
		}
		public void stop() {
			fReqestor.cancel();
			cancel();
		}
		protected IStatus run(IProgressMonitor monitor) {
			if (monitor.isCanceled()) {
				return canceled(null, false);
			}
			fViewer.clear();
			Set fFilteredHistory= new HashSet();
			List elements= new ArrayList();
			List images= new ArrayList();
			List labels= new ArrayList();
			for (int i= 0; i < fHistory.length; i++) {
				TypeInfo type= fHistory[i];
				if (fFilter.matchHistroyElement(type)) {
					fFilteredHistory.add(type);
					elements.add(type);
					images.add(fLabelProvider.getImage(type));
					labels.add(fLabelProvider.getText(type));
				}
			}
			fViewer.addAll(elements, images, labels);
			fReqestor.setHistory(fFilteredHistory);
			long start= System.currentTimeMillis();
			SearchEngine engine= new SearchEngine();
			try {
				String packPattern= fFilter.getPackagePattern();
				engine.searchAllTypeNames(
					packPattern == null ? null : packPattern.toCharArray(), 
					fFilter.getNamePattern().toCharArray(), 
					fFilter.getSearchFlags(), 
					IJavaSearchConstants.TYPE, 
					SearchEngine.createWorkspaceScope(), 
					fReqestor, 
					IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, 
					monitor);
			} catch (OperationCanceledException e) {
				return canceled(e, false);
			} catch (JavaModelException e) {
				fViewer.failed(e);
				return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR, "Searching for all types in workspace failed", e);
			}
			System.out.println("Time needed until search has finished: " + (System.currentTimeMillis() - start));
			Data[] datas= fReqestor.getResult();
			TypeInfoFactory factory= new TypeInfoFactory();
			TypeInfo[] result= new TypeInfo[datas.length];
			for (int i= 0; i < datas.length; i++) {
				Data data= datas[i];
				result[i]= factory.create(data.packageName, data.name, data.enclosingNames, data.modifier, data.path); 
			}
			System.out.println("Time needed until type info has been created: " + (System.currentTimeMillis() - start));
			Arrays.sort(result, new TypeInfoComparator());
			System.out.println("Time needed until sort has finished: " + (System.currentTimeMillis() - start));
			if (monitor.isCanceled())
				return canceled(null, false);			
			if (result.length > 0 && fFilteredHistory.size() > 0) {
				fViewer.addDashLine();
			}
			int currentIndex= 0;
			while (true) {
				long startTime= System.currentTimeMillis();
				elements.clear();
				images.clear();
				labels.clear();
	            int delta = Math.min(currentIndex == 0 ? fViewer.getNumberOfVisibleItems() : 10, result.length - currentIndex);
				if (delta == 0)
					break;
				int end= currentIndex + delta;
				for (int i= currentIndex; i < end; i++) {
					TypeInfo type= result[i];
					elements.add(type);
					images.add(fLabelProvider.getImage(type));
					labels.add(fLabelProvider.getText(type));
				}
				fViewer.addAll(elements, images, labels);
				currentIndex= end;
				if (monitor.isCanceled())
					return canceled(null, false);
				try {
					long sleep= 100 - (System.currentTimeMillis() - startTime);
					if (DEBUG) {
						System.out.println("Sleeping for: " + sleep); //$NON-NLS-1$
					}
					if (sleep > 0)
						Thread.sleep(sleep);
				} catch (InterruptedException e) {
					return canceled(e, true);
				}
				if (monitor.isCanceled())
					return canceled(null, false);
			}
			fViewer.done();
			return new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, "", null);
		}
		private IStatus canceled(Exception e, boolean removePendingItems) {
			fViewer.canceled(removePendingItems);
			return new Status(IStatus.CANCEL, JavaPlugin.getPluginId(), IStatus.CANCEL, "", e);
		}
	}
	
	private Table fTable;
	private TypeInfoLabelProvider fLabelProvider;
	
	private int fNumberOfVisibleItems;
	private int fNextElement;
	private List fItems;
	
	private TypeInfoHistory fHistory;

	private SearchJob fSearchJob;
	
	private static final char MDASH= '—';
	private static final boolean DEBUG= false;	
	
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
	private DashLine fDashLine= new DashLine();
	
	public TypeInfoViewer2(Composite parent) {
		fTable= new Table(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.FLAT);
		fLabelProvider= new TypeInfoLabelProvider();
		fItems= new ArrayList(500);
		fTable.setHeaderVisible(false);
		fTable.addControlListener(new ControlListener() {
			public void controlResized(ControlEvent event) {
				int itemHeight= fTable.getItemHeight();
				Rectangle clientArea= fTable.getClientArea();
				fNumberOfVisibleItems= (clientArea.height / itemHeight) + 1;
			}
			public void controlMoved(ControlEvent e) {
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
		fHistory= TypeInfoHistory.getInstance();
		reset();
	}
	
	public Table getTable() {
		return fTable;
	}
	
	/* Method is called from withing the UI thread */
	public void stop(boolean join) {
		if (fSearchJob != null) {
			fSearchJob.stop();
			if (join) {
				boolean joined= false;
				while (!joined) {
					try {
						fSearchJob.join();
						joined= true;
					} catch (InterruptedException e) {
						joined= false;
					}
				}
			}
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
		stop(true);
		if (text.length() == 0 || "*".equals(text)) { //$NON-NLS-1$
			reset();
		} else {
			fSearchJob= new SearchJob(this, new TypeInfoFilter(text), fHistory.getTypeInfos(), fNumberOfVisibleItems);
			fSearchJob.schedule();
		}
	}
	
	public void setFocus() {
		fTable.setFocus();
	}
	
	private ILabelProvider getLabelProvider() {
		return fLabelProvider;
	}
	
	private int getNumberOfVisibleItems() {
		return fNumberOfVisibleItems;
	}
	
	private void clear() {
		fTable.getDisplay().syncExec(new Runnable() {
			public void run() {
				fNextElement= 0;
				fLabelProvider.reset();
			}
		});
	}
	
	private void addDashLine() {
		add(fDashLine);
	}
	
	private void add(final Object element) {
		fTable.getDisplay().syncExec(new Runnable() {
			public void run() {
				addSingleElement(element);
			}
		});
	}
	
	private void addAll(final List elements) {
		fTable.getDisplay().syncExec(new Runnable() {
			public void run() {
				for (Iterator iter= elements.iterator(); iter.hasNext();) {
					addSingleElement(iter.next());
				}
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
		}
	}
	
	private void addAll(final List elements, final List images, final List labels) {
		fTable.getDisplay().syncExec(new Runnable() {
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
		}
	}
	
	private void reset() {
		fNextElement= 0;
		TypeInfo[] historyItems= fHistory.getTypeInfos();
		for (int i= 0; i < historyItems.length; i++) {
			add(historyItems[i]);
		}
		shortenTable();
	}
	
	private void canceled(final boolean removePendingItems) {
		fTable.getDisplay().syncExec(new Runnable() {
			public void run() {
				if (removePendingItems) {
					shortenTable();
				}
				fSearchJob= null;
			}
		});
	}
	
	private void done() {
		fTable.getDisplay().syncExec(new Runnable() {
			public void run() {
				shortenTable();
				fSearchJob= null;
			}
		});
	}
	
	private synchronized void failed(JavaModelException e) {
		fTable.getDisplay().syncExec(new Runnable() {
			public void run() {
				shortenTable();
				fSearchJob= null;
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