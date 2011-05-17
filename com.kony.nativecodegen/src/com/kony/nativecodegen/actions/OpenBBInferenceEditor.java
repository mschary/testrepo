package com.kony.nativecodegen.actions;

import org.eclipse.jface.action.IAction;

public class OpenBBInferenceEditor extends AbstractNativeCodeGenAction {

	@Override
	public void run(IAction action) {
		OpenAllInferenceEditors.openEditor(NativeCodeGenerationJob.BB_TYPE_INFERENCE_FILE, selectedElement);
	}

}
