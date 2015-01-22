package jboot.loader.bootstrapper.bootable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import jboot.loader.bootstrapper.bootable.model.BootableInfo;
import jboot.loader.bootstrapper.bootable.model.JavaBootableInfo;

public class JavaBootableLauncher extends AbstractBootableLauncher<JavaBootableInfo> {

	public JavaBootableLauncher(ClassLoader classLoader) {
		super(classLoader);
	}

	@Override
	protected JavaBootableInfo inheritParents(JavaBootableInfo bootable, Set<BootableInfo> inherited) throws Exception {
		if (!inherited.contains(bootable)) {
			inherited.add(bootable);
			JavaBootableInfo parentBootable = (JavaBootableInfo) bootable.getParent();
			if (parentBootable != null) {
				parentBootable = inheritParents(parentBootable, inherited);
				if (bootable.getClassName() == null || bootable.getClassName().trim().isEmpty()) {
					bootable.setClassName(parentBootable.getClassName());
				}
			}
		}
		return bootable;
	}

	@Override
	public void launch(JavaBootableInfo javaBootable, String[] args) throws Exception {
		String mainClassName = javaBootable.getClassName();
		if (mainClassName != null && !mainClassName.trim().isEmpty()) {
			//find class
			Class<?> mainClass = null;
			try {
				mainClass = this.getClassLoader().loadClass(mainClassName);
			} catch (ClassNotFoundException ex) {
				throw new Exception("The java bootable target " + javaBootable.getName() + ":" + javaBootable.getVersion() + " specifies a class " + javaBootable.getClassName() + " that does not exist.", ex);
			}

			Method mainMethod = null;
			try {
				//find main method that accepts a String[], String[], Map as arguments (progArgs, bootArgs, parsedProgArgs)
				mainMethod = mainClass.getMethod("main", String[].class, String[].class, Map.class);
				mainMethod.invoke(null, (Object) args, (Object) getBootConfigArgs(), null);//TODO put the args into Map
			} catch (NoSuchMethodException ex) {
				try {
					//find main method that accepts a String[], String[] as arguments (progArgs, bootArgs)
					mainMethod = mainClass.getMethod("main", String[].class, String[].class);
					mainMethod.invoke(null, (Object) args, (Object) getBootConfigArgs());
				} catch (NoSuchMethodException ex2) {
					try {
						//find main method that accepts a String[] as argument (progArgs)
						mainMethod = mainClass.getMethod("main", String[].class);
						mainMethod.invoke(null, (Object) args);
					} catch (NoSuchMethodException ex3) {
						//when no main method exists check if the class implements Runnable
						if (Runnable.class.isAssignableFrom(mainClass)) {
							Constructor<?> constructor = null;
							Runnable runnable = null;
							try {
								//find constructor that accepts a String[], String[], Map as arguments (progArgs, bootArgs, parsedProgArgs)
								constructor = mainClass.getConstructor(String[].class, String[].class, Map.class);
								runnable = (Runnable) constructor.newInstance((Object) args, (Object) getBootConfigArgs(), null);//TODO transform args into Map ;
							} catch (NoSuchMethodException ex4) {
								try {
									//find constructor that accepts a String[], String[] as arguments (progArgs, bootArgs)
									constructor = mainClass.getConstructor(String[].class, String[].class);
									runnable = (Runnable) constructor.newInstance((Object) args, (Object) getBootConfigArgs());
								} catch (NoSuchMethodException ex5) {
									try {
										//find constructor that accepts a String[] as an argument (progArgs)
										constructor = mainClass.getConstructor(String[].class);
										runnable = (Runnable) constructor.newInstance((Object) args);
									} catch (NoSuchMethodException ex6) {
										//get default constructor
										constructor = mainClass.getConstructor();
										runnable = (Runnable) constructor.newInstance();
									}
								}
							}
							if (runnable == null) {
								throw new Exception("No supported constructor in runnable class " + mainClass.getName() + " in java bootable target " + javaBootable.getName() + ":" + javaBootable.getVersion());
							}
							runnable.run();
						} else {
							throw new Exception("The java bootable target " + javaBootable.getName() + ":" + javaBootable.getVersion() + " specifies a class " + mainClass.getName() + " that has no main method and/or does not implement the interface java.lang.Runnable");
						}
					}
				}
			}
		} else {
			throw new Exception("Invalid class name in bootable " + javaBootable.getName() + ":" + javaBootable.getVersion());
		}
	}

}
