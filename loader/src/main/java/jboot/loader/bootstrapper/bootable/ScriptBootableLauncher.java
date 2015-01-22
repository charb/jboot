package jboot.loader.bootstrapper.bootable;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jboot.loader.bootstrapper.bootable.model.BootableInfo;
import jboot.loader.bootstrapper.bootable.model.ScriptBootableInfo;

public class ScriptBootableLauncher extends AbstractBootableLauncher<ScriptBootableInfo> {

	private ScriptEngineManager scriptEngineManager;

	public ScriptBootableLauncher(ClassLoader classLoader) {
		super(classLoader);
		scriptEngineManager = new ScriptEngineManager(this.getClassLoader());
	}

	@Override
	protected ScriptBootableInfo inheritParents(ScriptBootableInfo bootable, Set<BootableInfo> inherited) throws Exception {
		if (!inherited.contains(bootable)) {
			inherited.add(bootable);
			ScriptBootableInfo parentBootable = (ScriptBootableInfo) bootable.getParent();
			if (parentBootable != null) {
				parentBootable = inheritParents(parentBootable, inherited);
				if (bootable.getType() == null || bootable.getType().trim().isEmpty()) {
					bootable.setType(parentBootable.getType());
				}
				if (bootable.getScript() == null || bootable.getScript().trim().isEmpty()) {
					bootable.setScript(parentBootable.getScript());
				}
			}
		}
		return bootable;
	}

	@Override
	public void launch(ScriptBootableInfo scriptBootable, String[] args) throws Exception {
		//find appropriate script engine
		ScriptEngine scriptEngine = scriptEngineManager.getEngineByName(scriptBootable.getType());
		if (scriptEngine == null) {
			throw new Exception("Unable to find a script engine for type: " + scriptBootable.getType());
		}
		scriptEngine.getBindings(ScriptContext.GLOBAL_SCOPE).put("args", args);
		scriptEngine.getBindings(ScriptContext.GLOBAL_SCOPE).put("scriptEngine", scriptEngine);
		scriptEngine.getBindings(ScriptContext.GLOBAL_SCOPE).put("scriptEngineManager", scriptEngineManager);

		//find script
		InputStream in = this.getClassLoader().getResourceAsStream(scriptBootable.getScript());
		if (in == null) {
			throw new Exception("Unable to find the script at: " + scriptBootable.getScript());
		}

		//launch the script
		scriptEngine.eval(new BufferedReader(new InputStreamReader(in)));
	}

}
