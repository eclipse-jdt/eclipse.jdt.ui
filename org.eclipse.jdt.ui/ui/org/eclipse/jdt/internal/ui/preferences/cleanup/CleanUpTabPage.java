package org.eclipse.jdt.internal.ui.preferences.cleanup;

import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jdt.internal.ui.fix.ICleanUp;
import org.eclipse.jdt.internal.ui.preferences.formatter.JavaPreview;
import org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialogTabPage;

public abstract class CleanUpTabPage extends ModifyDialogTabPage {

	private final Map fValues;
	private CleanUpPreview fCleanUpPreview;
	private final boolean fIsSaveAction;
	
	public CleanUpTabPage(IModificationListener listener, Map values, boolean isSaveAction) {
		super(listener, values);
		fValues= values;
		fIsSaveAction= isSaveAction;
	}
	
	/**
	 * @return is this tab page shown in the save action dialog
	 */
	public boolean isSaveAction() {
		return fIsSaveAction;
	}
	
	protected abstract ICleanUp[] createPreviewCleanUps(Map values);
	
	protected JavaPreview doCreateJavaPreview(Composite parent) {
        fCleanUpPreview= new CleanUpPreview(parent, createPreviewCleanUps(fValues), false);        
    	return fCleanUpPreview;
    }

	protected void doUpdatePreview() {
		fCleanUpPreview.setWorkingValues(fValues);
		fCleanUpPreview.update();
	}
	
	protected void initializePage() {
		fCleanUpPreview.update();
	}
	
	protected void registerPreference(ButtonPreference preference) {
	}
	
	protected void registerSlavePreference(final CheckboxPreference master, final ButtonPreference[] slaves) {
    	master.addObserver( new Observer() {
    		public void update(Observable o, Object arg) {
    			for (int i= 0; i < slaves.length; i++) {
					slaves[i].setEnabled(master.getChecked());
				}
    		}
    	});
    	
    	for (int i= 0; i < slaves.length; i++) {
			slaves[i].setEnabled(master.getChecked());
		}
	}

	protected void intent(Composite group) {
        Label l= new Label(group, SWT.NONE);
    	GridData gd= new GridData();
    	gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(4);
    	l.setLayoutData(gd);
    }
}