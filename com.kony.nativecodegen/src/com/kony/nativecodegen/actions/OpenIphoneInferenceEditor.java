package com.kony.nativecodegen.actions;

import org.eclipse.jface.action.IAction;

public class OpenIphoneInferenceEditor  extends AbstractNativeCodeGenAction {

	@Override
	public void run(IAction action) {
		OpenAllInferenceEditors.openEditor(NativeCodeGenerationJob.IPHONE_TYPE_INFERENCE_FILE, selectedElement);
	}

}