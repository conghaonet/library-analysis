package com.zen.plugin.lib.analysis.task

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * FileParser
 *
 * @author znyang 2017/3/3 0003
 */

public class FileParser {

    private Map<String, File> mFiles
    private Map<String, String> mRepeatFileCache = new HashMap<>()

    FileParser(Map<String, File> files) {
        mFiles = files
    }

    public parse(long limitSize) {
        def large = new HashMap<ZipEntry, String>()
        def repeat = new HashMap<String, String>()
        mFiles.findAll { id, file ->

            ZipFile zip = new ZipFile(file)
            def zn = zip.entries()

            while (zn.hasMoreElements()) {
                def entry = zn.nextElement()
                if (entry.isDirectory()) {
                    continue
                }

                if (entry.size >= limitSize) {
                    large.put(entry, id);
                }
                def name = entry.name
                if (mRepeatFileCache.containsKey(name)) {
                    def ids = mRepeatFileCache.get(name) + ",${id}"
                    mRepeatFileCache.put(name, ids)
                    repeat.put(name, ids)
                } else {
                    mRepeatFileCache.put(name, id)
                }
            }
        }
        [large, repeat]
    }

}
