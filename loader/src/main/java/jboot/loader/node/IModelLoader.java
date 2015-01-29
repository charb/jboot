package jboot.loader.node;

import java.io.File;

import jboot.loader.model.Model;

public interface IModelLoader {
    public Model load(File file) throws Exception;
}
