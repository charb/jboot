package jboot.loader.boot.node;

import java.io.File;

import jboot.loader.boot.model.Model;

public interface IModelLoader {
    public Model load(File file) throws Exception;
}
