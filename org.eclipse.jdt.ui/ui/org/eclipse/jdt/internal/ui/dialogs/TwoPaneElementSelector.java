/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.jdt.internal.ui.util.StringMatcher;

public class TwoPaneElementSelector extends SelectionStatusDialog {
	
	// construction parameters
	private ILabelProvider fElementRenderer;
	private ILabelProvider fQualifierRenderer;
	private Object[] fElements;
	private final boolean fIgnoreCase;
	private boolean fMatchEmtpyString;

	private String fUpperListLabel;
	private String fLowerListLabel;

	// SWT widgets
	private Table fUpperList;
	private Table fLowerList;
	private Text fText;
	
	private String[] fRenderedStrings;
	private int[] fElementMap;
	private Integer[] fQualifierMap;
	
	private final TwoArrayQuickSorter fSorter;
	
	/**
	 *
	 */
	public TwoPaneElementSelector(Shell parent, String title, Image image, ILabelProvider elementRenderer, 
			ILabelProvider qualifierRenderer, boolean ignoreCase, boolean matchEmtpyString)
	{
		super(parent);
		
		setTitle(title);
		setImage(image);
		setMessage(""); //$NON-NLS-1$
		
		fElementRenderer= elementRenderer;
		fQualifierRenderer= qualifierRenderer;
		fIgnoreCase= ignoreCase;
		fSorter= new TwoArrayQuickSorter(ignoreCase);
		fMatchEmtpyString= matchEmtpyString;
	}			

	public void setUpperListLabel(String label) {
		fUpperListLabel= label;
	}

	public void setLowerListLabel(String label) {
		fLowerListLabel= label;
	}
	
	public void setElements(Object[] elements) {
		fElements= elements;
	}
	
	public int open() {
		BusyIndicator.showWhile(null, new Runnable() {
			public void run() {
				fElementMap= new int[fElements.length+1];
				fRenderedStrings= renderStrings(fElements);	
				access$superOpen();
			}
		});
		
		return getReturnCode();
	}
		
	private void access$superOpen() {
		super.open();
	}	

	/**
	 * @deprecated Use getPrimaryResult instead.
	 */
	public Object getSelectedElement() {
		Object[] result= getResult();
		if (result == null || result.length == 0)
			return null;
			
		return result[0];
	}

	/**
	 * getUIComponent method comment.
	 */
	public Control createDialogArea(Composite parent) {
		Composite contents= (Composite)super.createDialogArea(parent);

		createMessageArea(contents);
		fText= createText(contents);
		fUpperList= createUpperList(contents);
		fLowerList= createLowerList(contents);

		//a little trick to make the window come up faster
      	String initialFilter= (String)getPrimaryInitialSelection();
      	if (initialFilter != null) {
			fText.setText(initialFilter);
			fText.selectAll();
		}
		
		return contents;
	}
	/**
	 * Creates the list widget and sets layout data.
	 * @return org.eclipse.swt.widgets.List
	 */
	private Table createUpperList(Composite parent) {
		if (fUpperListLabel != null)
			(new Label(parent, SWT.NONE)).setText(fUpperListLabel);
			
		Table list= new Table(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		
		list.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event evt) {
				handleUpperSelectionChanged();
			}
		});
		
		list.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(Event evt) {
				handleUpperDoubleClick();
			}
		});
		
		list.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				fElementRenderer.dispose();
			}
		});
		
		GridData data= new GridData();
		data.widthHint= convertWidthInCharsToPixels(50);
		data.heightHint= convertHeightInCharsToPixels(15);
		data.grabExcessVerticalSpace= true;
		data.grabExcessHorizontalSpace= true;
		data.horizontalAlignment= GridData.FILL;
		data.verticalAlignment= GridData.FILL;
		
		list.setLayoutData(data);
		
		return list;
	}
	
	/**
	 * Creates the list widget and sets layout data.
	 * @return org.eclipse.swt.widgets.List
	 */
	private Table createLowerList(Composite parent) {
		if (fLowerListLabel != null)
			(new Label(parent, SWT.NONE)).setText(fLowerListLabel);

		Table list= new Table(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		
		list.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event evt) {
				handleLowerSelectionChanged();
			}
		});
		
		list.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(Event evt) {
				handleLowerDoubleClick();
			}
		});
		
		list.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				fQualifierRenderer.dispose();
			}
		});
		
		GridData data= new GridData();
		data.widthHint= convertWidthInCharsToPixels(50);
		data.heightHint= convertHeightInCharsToPixels(5);
		data.grabExcessVerticalSpace= true;
		data.grabExcessHorizontalSpace= true;
		data.horizontalAlignment= GridData.FILL;
		data.verticalAlignment= GridData.FILL;
		list.setLayoutData(data);
		
		return list;
	}

	/**
	 * Creates the text widget and sets layout data.
	 * @return org.eclipse.swt.widgets.Text
	 */
	private Text createText(Composite parent) {
		Text text= new Text(parent, SWT.BORDER);

		GridData data= new GridData();
		data.grabExcessVerticalSpace= false;
		data.grabExcessHorizontalSpace= true;
		data.horizontalAlignment= GridData.FILL;
		data.verticalAlignment= GridData.BEGINNING;
		text.setLayoutData(data);
		
		Listener listener= new Listener() {
			public void handleEvent(Event evt) {
				rematch(fText.getText());
			}
		};
		text.addListener(SWT.Modify, listener);
		        
		return text;
	}

	/**
	 * 
	 * @return java.lang.String[]
	 * @param p org.eclipse.jface.elements.IIndexedProperty
	 */
	private String[] renderStrings(Object[] p) {
		String[] strings= new String[p.length];
		int size= strings.length;
		for (int i= 0; i < size; i++) {
			strings[i]= fElementRenderer.getText(p[i]);
		}
		fSorter.sort(strings, p);
		return strings;
	}
	
	/**
	 * update the list to reflect a new match string.
	 * @param matchString java.lang.String
	 */
	private void rematch(String matchString) {
		if ("".equals(matchString) && !fMatchEmtpyString) { //$NON-NLS-1$
			updateUpperListWidget(fElementMap, 0);
		} else {
			int k= 0;
			StringMatcher matcher= new StringMatcher(matchString + "*", fIgnoreCase, false); //$NON-NLS-1$
			String lastString= null;
			int length= fElements.length;
			for (int i= 0; i < length; i++) {
				String curr= fRenderedStrings[i];
				if (!curr.equals(lastString)) {
					lastString= curr;
					if (matcher.match(curr)) {
						fElementMap[k]= i;
						k++;
					}
				}
			}
			fElementMap[k]= -1;
		
			updateUpperListWidget(fElementMap, k);
		}
	}

	private void updateUpperListWidget(int[] indices, int size) {
		fUpperList.setRedraw(false);
		int itemCount= fUpperList.getItemCount();
		if (size < itemCount)
			fUpperList.remove(0, itemCount - size - 1);

		TableItem[] items= fUpperList.getItems();
		for (int i= 0; i < size; i++) {
			TableItem item= null;
			if (i < itemCount) {
				item= items[i];
			} else {
				item= new TableItem(fUpperList, i);
			}
			item.setText(fRenderedStrings[indices[i]]);
			// XXX: 1G65LDG: JFUIF:WIN2000 - ILabelProvider used outside a viewer
			Image image= fElementRenderer.getImage(fElements[indices[i]]);
			if (image != null)
				item.setImage(image);
		}

		if (fUpperList.getItemCount() > 0)
			fUpperList.setSelection(0);

		fUpperList.setRedraw(true);
		handleUpperSelectionChanged();
	}
		
	public void create() {
		super.create();
		fText.setFocus();
		setResult(null);
		updateOkState();
	}	

	/**
	 * @private
	 */
	protected void cancelPressed() {
		setResult(null);
		super.cancelPressed();
	}	

	/**
	 * @private
	 */
	protected void computeResult() {
		List result= new ArrayList(1);
		result.add(getWidgetSelection());
		setResult(result);
	}

	private void handleUpperSelectionChanged() {
		int selection= fUpperList.getSelectionIndex();
		if (selection >= 0) {
			int i = fElementMap[selection];
			int k= i;
			int length= fRenderedStrings.length;
			while (k < length && fRenderedStrings[k].equals(fRenderedStrings[i])) {
				k++;
			}
			updateLowerListWidget(i, k);
		} else 
			updateLowerListWidget(0, 0);
	}
	
	/**
	 * @return the ID of the button that is 'pressed' on doubleClick in the lists.
	 * By default it is the OK button.
	 * Override to change this setting.
	 */
	protected int getDefaultButtonID(){
		return IDialogConstants.OK_ID;
	}
	
	private void handleUpperDoubleClick() {
		if (getWidgetSelection() != null)
			buttonPressed(getDefaultButtonID());
	}
	
	private void handleLowerDoubleClick() {
		if (getWidgetSelection() != null)
			buttonPressed(getDefaultButtonID());
	}	

	private void handleLowerSelectionChanged() {
		updateOkState();
	}
	
	protected Object getWidgetSelection() {
		int i= fLowerList.getSelectionIndex();
		if (fQualifierMap != null) {
			if (fQualifierMap.length == 1)
				i= 0;
			if (i < 0) {
				return null;
			} else {
				Integer index= fQualifierMap[i];
				return fElements[index.intValue()];
			} 
		}
		return null;	
	}
	
	private void updateOkState() {
		Button okButton= getOkButton();
		if (okButton != null)
			okButton.setEnabled(getWidgetSelection() != null);
	}
	
	private void updateLowerListWidget(int from, int to) {
		fLowerList.removeAll();
		fQualifierMap= new Integer[to - from];
		String[] qualifiers= new String[to - from];
		
		for (int i= from; i < to; i++) {
			// XXX: 1G65LDG: JFUIF:WIN2000 - ILabelProvider used outside a viewer
			qualifiers[i-from]= fQualifierRenderer.getText(fElements[i]);
			fQualifierMap[i-from]= new Integer(i);
		}
		fSorter.sort(qualifiers, fQualifierMap);
		
		for (int i= 0; i < to - from; i++) {
			TableItem item= new TableItem(fLowerList, i);
			item.setText(qualifiers[i]);

			// XXX: 1G65LDG: JFUIF:WIN2000 - ILabelProvider used outside a viewer
			Image image= fQualifierRenderer.getImage(fElements[from + i]);
			if (image != null)
				item.setImage(image);
		}
		
		if (fLowerList.getItemCount() > 0)
			fLowerList.setSelection(0);

		updateOkState();
	}
	
}