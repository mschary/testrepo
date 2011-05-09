package com.kony.nativecodegen.actions;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;

import com.kony.nativecodegen.ui.NativeCodePlatformSelectionDialog;
import com.pat.tool.keditor.navigation.model.ResourceLeaf;

/**
 * Native code generation is supported only IPhone,Windows Phone, Android and BlackBerry. So for the first time allow to choose the platform from the dialog and create the respective folder under <code>nativecode</code> folder.
 * 
 * @author Rakesh
 */

public class NativeCodeGenAction implements IObjectActionDelegate {

	private IViewPart view;

	private ResourceLeaf selectedElement;

	private String projectName;

	public void run(IAction action) {
		projectName = selectedElement.getName();
		view.getSite().getPage().saveAllEditors(true);
		NativeCodePlatformSelectionDialog dialog = new NativeCodePlatformSelectionDialog(view.getViewSite().getShell());
		dialog.setHelpAvailable(false);
		int option = dialog.open();
		if (option == Window.OK) {
			int selectedState = dialog.getSelectedState();

			if (selectedState == 0)
				return;
			Job job = null;
			int value = selectedState & NativeCodePlatformSelectionDialog.IPHONE_SELECTED;
			if (value == NativeCodePlatformSelectionDialog.IPHONE_SELECTED) {
				job = getExecutionJob(NativeCodeGenerationJob.IPHONE_TYPE_INFERENCE_FILE);
				job.schedule();
			}

			value = selectedState & NativeCodePlatformSelectionDialog.ANDROID_SELECTED;
			if (value == NativeCodePlatformSelectionDialog.ANDROID_SELECTED) {
				job = getExecutionJob(NativeCodeGenerationJob.ANDROID_TYPE_INFERENCE_FILE);
				job.schedule();
			}

			value = selectedState & NativeCodePlatformSelectionDialog.BB_SELECTED;
			if (value == NativeCodePlatformSelectionDialog.BB_SELECTED) {
				job = getExecutionJob(NativeCodeGenerationJob.BB_TYPE_INFERENCE_FILE);
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

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		view = (IViewPart) targetPart;
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() == 1) {
				Object firstElement = ssel.getFirstElement();
				if (firstElement instanceof ResourceLeaf) {
					selectedElement = (ResourceLeaf) ssel.getFirstElement();
				}
			}
		}
	}
}
