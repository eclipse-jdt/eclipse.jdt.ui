package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.AppearancePreferencePage;

public class StandardJavaUILabelProvider extends JavaUILabelProvider implements IPropertyChangeListener {

	public final static int DEFAULT_TEXTFLAGS= JavaElementLabels.ROOT_VARIABLE | JavaElementLabels.M_PARAMETER_TYPES |  JavaElementLabels.M_APP_RETURNTYPE;
	public final static int DEFAULT_IMAGEFLAGS= JavaElementImageProvider.OVERLAY_ICONS;
	
	private int fTextFlagMask;
	private int fImageFlagMask;

	/**
	 * Constructor for StandardJavaUILabelProvider.
	 * @param textFlags possible text flags
	 * @param imageFlags possible image flags
	 */
	public StandardJavaUILabelProvider(int textFlags, int imageFlags) {
		super(textFlags, imageFlags);
		initMasks();
		JavaPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
	}
	
	public StandardJavaUILabelProvider() {
		this(DEFAULT_TEXTFLAGS, DEFAULT_IMAGEFLAGS);
	}	
	
	private void initMasks() {
		fTextFlagMask= -1;
		if (!AppearancePreferencePage.showMethodReturnType()) {
			fTextFlagMask ^= JavaElementLabels.M_APP_RETURNTYPE;
		}
		fImageFlagMask= -1;
		if (!AppearancePreferencePage.showOverrideIndicators()) {
			fImageFlagMask ^= JavaElementImageProvider.OVERRIDE_INDICATORS;
		}		
	}

	/*
	 * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		String property= event.getProperty();
		if (property.equals(AppearancePreferencePage.PREF_METHOD_RETURNTYPE)
				|| property.equals(AppearancePreferencePage.PREF_OVERRIDE_INDICATOR)) {
			initMasks();
			LabelProviderChangedEvent lpEvent= new LabelProviderChangedEvent(this, null); // refresh all
			fireLabelProviderChanged(lpEvent);
		}		
	}

	/*
	 * @see IBaseLabelProvider#dispose()
	 */
	public void dispose() {
		JavaPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
		super.dispose();
	}

	/*
	 * @see JavaUILabelProvider#getImageFlags()
	 */
	protected int getImageFlags() {
		return super.getImageFlags() & fImageFlagMask;
	}

	/*
	 * @see JavaUILabelProvider#getTextFlags()
	 */
	protected int getTextFlags() {
		return super.getTextFlags() & fTextFlagMask;
	}

}
