package com.kony.nativecodegen.actions;

import org.eclipse.jface.action.IAction;

public class OpenAndroidInferenceEditor  extends AbstractNativeCodeGenAction {

	@Override
	public void run(IAction action) {
		OpenAllInferenceEditors.openEditor(NativeCodeGenerationJob.ANDROID_TYPE_INFERENCE_FILE, selectedElement);
	}

}