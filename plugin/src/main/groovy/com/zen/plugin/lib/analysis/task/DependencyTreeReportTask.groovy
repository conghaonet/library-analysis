package com.zen.plugin.lib.analysis.task

import com.zen.plugin.lib.analysis.ext.LibraryAnalysisExtension
import com.zen.plugin.lib.analysis.model.DependencyDictionary
import com.zen.plugin.lib.analysis.model.Node
import com.zen.plugin.lib.analysis.render.HtmlRenderer
import com.zen.plugin.lib.analysis.render.OutputModuleList
import com.zen.plugin.lib.analysis.util.Logger
import com.zen.plugin.lib.analysis.util.PackageChecker
import com.zen.plugin.lib.analysis.util.ResourceUtils
import com.zen.plugin.lib.analysis.util.Timer
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.diagnostics.AbstractReportTask
import org.gradle.api.tasks.diagnostics.internal.ReportRenderer
import org.gradle.api.tasks.diagnostics.internal.dependencies.AsciiDependencyReportRenderer
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.RenderableModuleResult

import java.util.zip.ZipEntry

/**
 * @author zen
 * @version 2016/9/9
 */
class DependencyTreeReportTask extends AbstractReportTask {
    def renderer = new AsciiDependencyReportRenderer()

    Configuration configuration
    LibraryAnalysisExtension extension

    @Override
    protected ReportRenderer getRenderer() {
        return renderer
    }

    @Override
    protected void generate(Project project) throws IOException {
        outputHtml()

        if (extension.showTree) {
            renderer.startConfiguration(configuration)
            renderer.render(configuration)
            renderer.completeConfiguration(configuration)
        }
    }

    private void outputHtml() {
        def output = prepareOutputPath()
        ResourceUtils.copyResources(output)

        // 用于记录耗时
        def timer = new Timer()
        Node root = buildRootNode()
        timer.mark(Logger.W, "create nodes")

        def (PackageChecker packageChecker, DependencyDictionary dictionary) = supplyNodeInfo(root)
        timer.mark(Logger.W, "supply info")

        def (Map<ZipEntry, String> largeFiles, Map<String, String> repeatFiles) =
        new FileParser(dictionary.cacheFiles).parse(extension.limit.fileSize)

        def ext = new StringBuilder()
        ext.append("\nLarge Files:\n")
        largeFiles.findAll {
            key, value -> ext.append("${key.name} ${key.size} in: ${value}").append("\n")
        }

        ext.append("\nAll Files:\n")
        repeatFiles.findAll {
            key, value -> ext.append("${key} in: ${value}").append('\n')
        }

        def result = renderHtml(packageChecker, dictionary, output, root, ext.toString())
        timer.mark(Logger.W, "render html")

        println "output result: ${result}"
    }

    /**
     *
     * @param packageChecker
     * @param dictionary
     * @param output 输出文件的路径
     * @param root 根节点
     * @param ext 额外文本信息
     * @return
     */
    private static renderHtml(PackageChecker packageChecker, DependencyDictionary dictionary,
                              String output, Node root, String ext) {
        // 输出module name重复的列表
        def list = outputModuleList(dictionary, packageChecker)
        list.modules.each {
            Logger.D?.log("module: ${it.name}")
        }

        def msg = packageChecker.outputPackageRepeatList()
        def result = new HtmlRenderer(output).render(root, list, ext)
        if (msg && !msg.isEmpty()) {
            println msg
        }
        result
    }

    private Node buildRootNode() {
        // 依赖树输出结果
        def resolutionResult = configuration.getIncoming().getResolutionResult()
        def dep = new RenderableModuleResult(resolutionResult.getRoot())
        // 转化为自定义节点结构
        def root = Node.create(dep)
        root
    }

    private List supplyNodeInfo(Node root) {
        // 用于检测module name重复
        def packageChecker = new PackageChecker()
        // 通过依赖文件创建依赖字典
        def dictionary = new DependencyDictionary(configuration.getIncoming().getFiles())
        // 使用依赖字典分析节点包大小
        root.supplyInfo(extension, dictionary, packageChecker)
        [packageChecker, dictionary]
    }

    static OutputModuleList outputModuleList(DependencyDictionary dictionary, PackageChecker checker) {
        OutputModuleList list = new OutputModuleList()
        dictionary.cacheInfoMap.each {
            key, value ->
                def pkgName = checker.parseModuleName(key, value.file)
                def isRepeat = checker.isRepeatPackage(pkgName)
                list.addModule(new OutputModuleList.DependencyOutput(key, value.size, pkgName,
                        value.type, isRepeat ? "package name repeat" : "", isRepeat ? "danger" : ""))
        }
        list.sortModules()
        list
    }

    private String prepareOutputPath() {
        def path = "${project.buildDir}/${extension.outputPath}/${configuration.name}"
        def file = new File(path)
        if (!file.exists()) {
            file.mkdirs()
        }
        path
    }

}