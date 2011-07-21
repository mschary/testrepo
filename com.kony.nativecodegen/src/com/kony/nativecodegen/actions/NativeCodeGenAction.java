package com.kony.nativecodegen.actions;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;

import com.kony.nativecodegen.ui.NativeCodePlatformSelectionDialog;
import com.pat.tool.keditor.utils.KConstants;
import com.pat.tool.keditor.utils.RendererHashMapData;
import com.pat.tool.keditor.widgets.RichClientChannel;
import com.pat.tool.keditor.widgets.TabletRichClientChannel;

/**
 * Native code generation is supported only IPhone,Windows Phone, Android and BlackBerry. So for the first time allow to choose the platform from the dialog and create the respective folder under <code>nativecode</code> folder.
 * 
 * @author Rakesh
 */

public class NativeCodeGenAction extends AbstractNativeCodeGenAction {

	private String projectName;

	public void run(IAction action) {
		projectName = selectedElement.getName();
		view.getSite().getPage().saveAllEditors(true);
		NativeCodePlatformSelectionDialog dialog = new NativeCodePlatformSelectionDialog(view.getViewSite().getShell());
		dialog.setHelpAvailable(false);
		int option = dialog.open();
		if (option == Window.OK) {
			RendererHashMapData selectedPlatforms = dialog.getRendererHashMapData();

			Job job = null;
			if (selectedPlatforms.getValue(RichClientChannel.IPHONE)) {
				job = getExecutionJob(KConstants.IPHONE_TYPE_INFERENCE_FILE);
				job.schedule();
			}
			
			if (selectedPlatforms.getValue(TabletRichClientChannel.IPAD)) {
				job = getExecutionJob(KConstants.IPAD_TYPE_INFERENCE_FILE);
				job.schedule();
			}

			if (selectedPlatforms.getValue(RichClientChannel.ANDROID)) {
				job = getExecutionJob(KConstants.ANDROID_TYPE_INFERENCE_FILE);
				job.schedule();
			}

			if (selectedPlatforms.getValue(RichClientChannel.BLACKBERRY)) {
				job = getExecutionJob(KConstants.BB_TYPE_INFERENCE_FILE);
				job.schedule();
			}

		}
	}

	private Job getExecutionJob(String fileName) {
		String jobName = "Generating native code for " + NativeCodeGenerationJob.getPlatformName(fileName);
		NativeCodeGenerationJob job = new NativeCodeGenerationJob(jobName, projectName, fileName);
		job.setUser(true);
		return job;
	}

}
