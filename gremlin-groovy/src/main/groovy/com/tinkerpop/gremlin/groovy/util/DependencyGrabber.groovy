package com.tinkerpop.gremlin.groovy.util

import com.tinkerpop.gremlin.groovy.plugin.Artifact
import groovy.grape.Grape
import org.apache.commons.lang3.SystemUtils

import java.nio.file.*
import java.util.jar.JarFile
import java.util.jar.Manifest

/**
 * This class is a rough copy of the {@code InstallCommand} in Gremlin Console.  There are far more detailed
 * comments there with respect to the workings of this class.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class DependencyGrabber {

    private final static String fileSep = System.getProperty("file.separator")
    private final ClassLoader classLoaderToUse
    private final String extensionDirectory

    public DependencyGrabber(final ClassLoader cl, final String extensionDirectory) {
        this.classLoaderToUse = cl
        this.extensionDirectory = extensionDirectory
    }

    def void copyDependenciesToPath(final Artifact artifact) {
        final def dep = makeDepsMap(artifact)
        final String extClassPath = getPathFromDependency(dep)
        final File f = new File(extClassPath)

        if (f.exists()) throw new IllegalStateException("a module with the name ${dep.module} is already installed")
        if (!f.mkdirs()) throw new IOException("could not create directory at ${f}")

        new File(extClassPath + fileSep + "plugin-info.txt").withWriter { out -> out << [artifact.group, artifact.artifact, artifact.version].join(":") }

        def fs = FileSystems.default
        def target = fs.getPath(extClassPath)

        def filesAlreadyInPath = []
        def libClassPath
        try {
            libClassPath = fs.getPath(System.getProperty("user.dir") + fileSep + "lib")
            getFileNames(filesAlreadyInPath, libClassPath)
        } catch (Exception ignored) {
            println "Detected a non-standard Gremlin directory structure during install.  Expecting a 'lib' " +
                    "directory sibling to 'ext'. This message does not necessarily imply failure, however " +
                    "the console requires a certain directory structure for proper execution. Altering that " +
                    "structure can lead to unexpected behavior."
        }

        final def dependencyLocations = [] as Set<URI>
        dependencyLocations.addAll(Grape.resolve([classLoader: this.classLoaderToUse], null, dep))

        // if windows then the path contains a starting forward slash that prevents it from being
        // recognized by FileSystem - strip it off
        dependencyLocations.collect {
            def p = SystemUtils.IS_OS_WINDOWS ? it.path.substring(1) : it.path
            return fs.getPath(p)
        }
        .findAll { !(it.fileName.toFile().name ==~ /(slf4j|logback\-classic)-.*\.jar/) }
                .findAll {
            !filesAlreadyInPath.collect { it.getFileName().toString() }.contains(it.fileName.toFile().name)
        }.each {
            def copying = target.resolve(it.fileName)
            Files.copy(it, copying, StandardCopyOption.REPLACE_EXISTING)
            println "Copying - $copying"
        }

        getAdditionalDependencies(target, artifact).collect { fs.getPath(it.path) }
                .findAll { !(it.fileName.toFile().name ==~ /(slf4j|logback\-classic)-.*\.jar/) }
                .findAll {
            !filesAlreadyInPath.collect { it.getFileName().toString() }.contains(it.fileName.toFile().name)
        }.each {
            def copying = target.resolve(it.fileName)
            Files.copy(it, copying, StandardCopyOption.REPLACE_EXISTING)
            println "Copying - $copying"
        }

        alterPaths(target, artifact)
    }

    private Set<URI> getAdditionalDependencies(final Path extPath, final Artifact artifact) {
        try {
            def pathToInstalled = extPath.resolve(artifact.artifact + "-" + artifact.version + ".jar")
            final JarFile jar = new JarFile(pathToInstalled.toFile())
            final Manifest manifest = jar.getManifest()
            def attrLine = manifest.mainAttributes.getValue("Gremlin-Plugin-Dependencies")
            def additionalDependencies = [] as Set<URI>
            if (attrLine != null) {
                def splitLine = attrLine.split(";")
                splitLine.each {
                    def artifactBits = it.split(":")
                    def additional = new Artifact(artifactBits[0], artifactBits[1], artifactBits[2])

                    final def additionalDep = makeDepsMap(additional)
                    additionalDependencies.addAll(Grape.resolve([classLoader: this.classLoaderToUse], null, additionalDep))
                }
            }

            return additionalDependencies
        } catch (Exception ex) {
            throw new RuntimeException(ex)
        }
    }

    private static alterPaths(final Path extPath, final Artifact artifact) {
        try {
            def pathToInstalled = extPath.resolve(artifact.artifact + "-" + artifact.version + ".jar")
            final JarFile jar = new JarFile(pathToInstalled.toFile());
            final Manifest manifest = jar.getManifest()
            def attrLine = manifest.mainAttributes.getValue("Gremlin-Plugin-Paths")
            if (attrLine != null) {
                def splitLine = attrLine.split(";")
                splitLine.each {
                    def kv = it.split("=")
                    Files.move(extPath.resolve(kv[0]), extPath.resolve(kv[1]), StandardCopyOption.REPLACE_EXISTING)
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex)
        }
    }

    private String getPathFromDependency(final Map<String, Object> dep) {
        def fileSep = System.getProperty("file.separator")
        return this.extensionDirectory + fileSep + (String) dep.module
    }

    private def makeDepsMap(final Artifact artifact) {
        final Map<String, Object> map = new HashMap<>()
        map.put("classLoader", this.classLoaderToUse)
        map.put("group", artifact.getGroup())
        map.put("module", artifact.getArtifact())
        map.put("version", artifact.getVersion())
        map.put("changing", false)
        return map
    }

    private static void getFileNames(final List fileNames, final Path dir) {
        final DirectoryStream<Path> stream = Files.newDirectoryStream(dir)
        for (Path path : stream) {
            if (path.toFile().isDirectory()) getFileNames(fileNames, path)
            else {
                fileNames.add(path.toAbsolutePath())
            }
        }
        stream.close()
    }
}
