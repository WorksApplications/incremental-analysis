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
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.Constants;

/**
 * A mojo to generate {@code -onlyAnalyze} parameter for spotbugs-maven-plugin. Generated parameter
 * will be set as property of current Maven execution.<br>
 *
 * Execute this mojo before you run spotbugs-maven-plugin, then you can check analysis only for
 * updated Java classes.
 *
 * @author Kengo TODA (toda_k@worksap.co.jp)
 */
@Mojo(
    name = "spotbugs",
    threadSafe = true,
    requiresProject = true,
    defaultPhase = LifecyclePhase.VERIFY)
public class SpotBugsMojo extends AbstractMojo {
  private final GitUpdatedJavaCodeDetector detector;

  @Parameter(property = "project")
  private MavenProject project;

  /**
   * Name of the property to generate value by this plugin, to specify {@code -onlyAnalyze} option to SpotBugs.
   */
  @Parameter(defaultValue = "spotbugs.onlyAnalyze")
  private String propertyToAnalyze;

  /**
   * Name of the property to generate value by this plugin, to decide that spotbugs-maven-plugin needs to skip analysis or not.
   */
  @Parameter(defaultValue = "spotbugs.skip")
  private String propertyToSkip;

  /**
   * Git ref of the source of pull-request or merge-request.
   */
  @Parameter(defaultValue = Constants.HEAD, property = "incremental.spotbugs.source")
  private String source;

  /**
   * Git ref of the target of pull-request or merge-request.
   */
  @Parameter(defaultValue = "refs/heads/master", property = "incremental.spotbugs.target")
  private String target;

  /**
   * Flag to skip execution of this incremental-analysis plugin.
   */
  @Parameter(defaultValue = "false", property = "incremental.spotbugs.skip")
  private boolean skip;

  /** Constructor for production */
  public SpotBugsMojo() {
    this.detector = new GitUpdatedJavaCodeDetector();
  }

  /** Constructor for unit test */
  SpotBugsMojo(GitUpdatedJavaCodeDetector detector) {
    this.detector = Objects.requireNonNull(detector);
  }

  @Override
  public void execute() throws MojoExecutionException {
    Log log = getLog();
    if (skip) {
      log.info("Skip generating list of target classes for SpotBugs.");
      return;
    }
    Stream<Path> updatedJavaCodes;

    try {
      if (!detector.detectDifference(project.getBasedir().toPath(), target, source)) {
        log.info(
            String.format(
                "No commit found between %s and %s, so skip generating list of target classes for SpotBugs",
                target, source));
        return;
      }
      log.info("Start generating list of target classes for SpotBugs...");
      updatedJavaCodes = detector.detectUpdatedCode(project.getBasedir().toPath(), target, source);
    } catch (IOException e) {
      throw new MojoExecutionException("Failed to list updated Java code", e);
    }

    List<String> targetClasses =
        codeToClass(getCompileSourceRoots(), updatedJavaCodes).collect(Collectors.toList());
    String targetClassList = targetClasses.stream().collect(Collectors.joining(","));

    if (targetClassList.isEmpty()) {
      project.getModel().addProperty(propertyToSkip, "true");
      log.info("No updated Java class found, static analysis will be skipped.");
    } else {
      project.getModel().addProperty(propertyToAnalyze, targetClassList);
      if (log.isDebugEnabled()) {
        targetClasses.forEach(
            className -> {
              log.debug("Updated class: " + className);
            });
      }
      log.info("Successfully generated list of target classes for SpotBugs.");
    }
  }

  /**
   * @param compileSourceRoots A non-null collection of compile source root.
   * @param updatedJavaCodes A non-null stream of updated Java codes.
   * @return {@code Stream} of name of updated classes, such as {@literal com.worksap.ClassName}
   */
  @VisibleForTesting
  Stream<String> codeToClass(Collection<Path> compileSourceRoots, Stream<Path> updatedJavaCodes) {
    assert compileSourceRoots != null;
    assert updatedJavaCodes != null;

    Stream<String> relativeJavaCodePaths =
        updatedJavaCodes
            .map(
                javaCode ->
                    compileSourceRoots.stream()
                        .filter(javaCode::startsWith)
                        .map(root -> root.relativize(javaCode))
                        .findFirst())
            .flatMap(this::streamFrom)
            .map(Path::toString);

    ClassSearcher searcher = new ClassSearcher(Paths.get(project.getBuild().getOutputDirectory()));
    return searcher.search(relativeJavaCodePaths);
  }

  /** @return A non-null set of compile source roots. Each entry should be absolute {@link Path}. */
  private Set<Path> getCompileSourceRoots() {
    return project.getCompileSourceRoots().stream()
        .map(Paths::get)
        .map(Path::toAbsolutePath)
        .collect(Collectors.toSet());
  }

  /**
   * A missing part in Java8: map {@link Optional} to {@link Stream}.
   *
   * @param optional A non-null {@link Optional} to map.
   * @return A non-null mapped {@link Stream}.
   */
  private <T> Stream<T> streamFrom(Optional<T> optional) {
    if (optional.isPresent()) {
      return Stream.of(optional.get());
    } else {
      return Stream.empty();
    }
  }
}
