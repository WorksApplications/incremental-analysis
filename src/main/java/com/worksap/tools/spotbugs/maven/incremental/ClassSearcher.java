/*
 * Copyright 2019 (c) Works Applications Co.,Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.worksap.tools.spotbugs.maven.incremental;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.PatternFilenameFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A class which is responsible to search class files compiled from given .java files.
 *
 * @author Kengo TODA (toda_k@worksap.co.jp)
 */
class ClassSearcher {
  private static final FilenameFilter CLASS_FILE_FILTER = new PatternFilenameFilter("^.*\\.class$");
  private final Path outputDir;
  private final String separator;

  ClassSearcher(Path outputDir) {
    Objects.requireNonNull(outputDir);
    this.outputDir = outputDir;
    this.separator = outputDir.getFileSystem().getSeparator();
  }

  /**
   * @param sourceStream A stream of relative .java file path such as {@code
   *     "com/worksap/tools/ClassName.java"}, {@code "com\worksap\tools\ClassName.java"}
   * @return A stream of class names such as {@code "com.worksap.tools.ClassName",
   *     "com.worksap.tools.ClassName$0"}
   */
  Stream<String> search(Stream<String> sourceStream) {
    Objects.requireNonNull(sourceStream);

    /*
     * Key: package path name with trailing slash, such as "com/worksap/tools/"
     * Value: list of .java file path such as "com/worksap/tools/ClassName.java"
     */
    Map<String, List<String>> group =
        sourceStream.collect(Collectors.groupingBy(FilenameUtils::getPath));

    return group.entrySet().stream()
        .flatMap(
            entry -> {
              String relativePackagePath = entry.getKey();

              final Path packageDir;
              if (relativePackagePath.isEmpty()) {
                packageDir = outputDir;
              } else {
                packageDir = outputDir.resolve(relativePackagePath);
              }

              /*
               * Package name with trailing dot, such as "com.worksap.tools."
               */
              final String packageStr = relativePackagePath.replace(separator, ".");

              /*
               * Set of updated file name such as "ClassName.java", to match with "Compiled from" value in class files.
               * https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.10
               */
              Set<String> updatedSourceFiles =
                  entry.getValue().stream().map(FilenameUtils::getName).collect(Collectors.toSet());

              return search(packageDir, updatedSourceFiles).map(packageStr::concat);
            })
        .map(FilenameUtils::removeExtension);
  }

  /**
   * @param packageDir A directory for target package, such as {@code
   *     "/path/to/target/classes/com/worksap/tools"}
   * @param updatedSourceNames A set of name of updated source which belongs to given package, such
   *     as {@code "ClassName.java"}
   * @return Stream of name of updated class files such as {@code "ClassName.class"}, {@code
   *     "ClassName$InnerClass.class"}
   */
  private Stream<String> search(Path packageDir, Set<String> updatedSourceNames) {
    assert packageDir != null;
    assert updatedSourceNames != null;

    String[] classFiles = packageDir.toFile().list(CLASS_FILE_FILTER);
    return Arrays.stream(classFiles)
        .filter(
            classFile -> {
              String compiledFrom = loadCompiledFrom(packageDir.resolve(classFile));
              return compiledFrom != null && updatedSourceNames.contains(compiledFrom);
            });
  }

  /**
   * Parse .class file by ASM, to load registered source file name.
   *
   * @param classFilePath A non-null {@link Path} which identifies target .class file.
   * @return A name of source file such as {@code "ClassName.java"}, or {@code null} if .class file
   *     has no information.
   */
  @VisibleForTesting
  String loadCompiledFrom(Path classFilePath) {
    assert classFilePath != null;

    byte[] data;
    try {
      // Most of class files are smaller than 8KiB,
      // so it's better to load in batch instead of load with BufferedInputStream which consumes
      // 8KiB memory constantly
      data = Files.readAllBytes(classFilePath);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    ClassReader classReader = new ClassReader(data);
    SourceFileNameVisitor visitor = new SourceFileNameVisitor();
    classReader.accept(visitor, 0);
    return visitor.source;
  }

  private static class SourceFileNameVisitor extends ClassVisitor {
    private String source;

    SourceFileNameVisitor() {
      super(Opcodes.ASM7);
    }

    @Override
    public void visitSource(String source, String debug) {
      this.source = source;
    }
  }
}
