package com.zen.plugin.lib.analysis.render

import java.util.zip.ZipEntry

/**
 * OutputLargeFileList
 *
 * @author znyang 2017/3/3 0003
 */

public class OutputLargeFileList {

    List<LargeFileInfo> files = new ArrayList<>()

    public static OutputLargeFileList create(Map<ZipEntry, String> data) {
        OutputLargeFileList list = new OutputLargeFileList()
        data.findAll { entry, id ->
            list.files.add(new LargeFileInfo(entry.name, id))
        }
        return list
    }

    public static class LargeFileInfo {

        String name
        String dependency

        LargeFileInfo(String name, String dependency) {
            this.name = name
            this.dependency = dependency
        }
    }

}
