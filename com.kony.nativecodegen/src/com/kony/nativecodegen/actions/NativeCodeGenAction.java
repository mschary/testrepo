package com.kony.nativecodegen.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;

import com.kony.nativecodegen.ui.NativeCodePlatformSelectionDialog;
import com.pat.tool.keditor.console.ConsoleDisplayManager;
import com.pat.tool.keditor.utils.KConstants;
import com.pat.tool.keditor.utils.KUtils;
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
	private int selectedPlatforms;
	private List<String> successList = new ArrayList<String>();
	private List<String> failureList = new ArrayList<String>();

	public void run(IAction action) {
		projectName = selectedElement.getName();
		view.getSite().getPage().saveAllEditors(true);
		NativeCodePlatformSelectionDialog dialog = new NativeCodePlatformSelectionDialog(view.getViewSite().getShell());
		dialog.setHelpAvailable(false);
		int option = dialog.open();
		if (option == Window.OK) {
			selectedPlatforms = 0;
			successList.clear();
			failureList.clear();
			RendererHashMapData selectedPlatforms = dialog.getRendererHashMapData();
			NativeCodeGenerationJob job = null;
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

	private NativeCodeGenerationJob getExecutionJob(String fileName) {
		final String platformName = NativeCodeGenerationJob.getPlatformName(fileName);
		String jobName = "Generating native code for " + platformName;
		final NativeCodeGenerationJob job = new NativeCodeGenerationJob(jobName, projectName, fileName);
		job.setUser(true);
		selectedPlatforms++;

		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if (job.isSuccess()) {
					successList.add(platformName);
				} else {
					failureList.add(platformName);
				}

				if (selectedPlatforms != 1 && selectedPlatforms == (successList.size() + failureList.size())) {
					printInfo(successList, "Native code generation is successfull for the following platforms: ", ConsoleDisplayManager.MSG_SUCCESS);
					printInfo(failureList, "Native code generation has failed for the following platforms: ", ConsoleDisplayManager.MSG_ERROR);
				}
			}
		});

		return job;
	}

	private void printInfo(List<String> list, String initialMsg, int msgType) {
		if (list.size() > 0) {
			StringBuilder builder = new StringBuilder();
			builder.append(initialMsg);
			for (String platName : list) {
				builder.append(platName);
				builder.append(",");
			}
			KUtils.removeCommaAtEndofBuffer(builder);
			ConsoleDisplayManager.getDefault().println(builder.toString(), msgType);
		}
	}

}
