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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

class GitUpdatedJavaCodeDetector {
  boolean detectDifference(Path projectRoot, String target, String source) throws IOException {
    Optional<Path> optionalGitRoot = findGitRoot(projectRoot);
    if (!optionalGitRoot.isPresent()) {
      throw new IllegalArgumentException("Git repository not found at " + projectRoot);
    }
    Path gitRoot = optionalGitRoot.get();
    try (Git git = Git.open(gitRoot.toFile())) {
      Repository repository = git.getRepository();
      Ref targetRef = repository.exactRef(target);
      if (targetRef == null) {
        throw new IllegalArgumentException(target + " does not exist in this Git repo");
      }
      Ref sourceRef = repository.exactRef(source);
      if (sourceRef == null) {
        throw new IllegalArgumentException(source + " does not exist in this Git repo");
      }
      return !sourceRef.equals(targetRef);
    }
  }

  Stream<Path> detectUpdatedCode(Path projectRoot, String target, String source)
      throws IOException {
    Optional<Path> optionalGitRoot = findGitRoot(projectRoot);
    if (!optionalGitRoot.isPresent()) {
      throw new IllegalArgumentException("Git repository not found at " + projectRoot);
    }

    Path gitRoot = optionalGitRoot.get();
    try (Git git = Git.open(gitRoot.toFile())) {
      Repository repository = git.getRepository();
      List<DiffEntry> updated =
          git.diff()
              .setOldTree(prepareTreeParser(repository, target))
              .setNewTree(prepareTreeParser(repository, source))
              .setShowNameAndStatusOnly(true)
              .call();
      return updated.stream()
          .filter(
              diff -> {
                return diff.getChangeType() != DiffEntry.ChangeType.DELETE;
              })
          .map(DiffEntry::getNewPath)
          .filter(path -> path.endsWith(".java")) // TODO support other languages like Scala
          .map(gitRoot::resolve);
    } catch (GitAPIException e) {
      throw new IOException("Failed to execute Git API", e);
    }
  }

  private Optional<Path> findGitRoot(Path projectRoot) {
    Path path = projectRoot;
    do {
      File gitDir = new File(path.toFile(), ".git");
      if (gitDir.isDirectory()) {
        return Optional.of(path);
      }
      path = path.getParent();
    } while (path != null);
    return Optional.empty();
  }

  private AbstractTreeIterator prepareTreeParser(Repository repository, String ref)
      throws IOException {
    // from the commit we can build the tree which allows us to construct the TreeParser
    Ref head = repository.exactRef(ref);
    if (head == null) {
      throw new IllegalArgumentException(ref + " does not exist in this Git repo");
    }
    try (RevWalk walk = new RevWalk(repository)) {
      RevCommit commit = walk.parseCommit(head.getObjectId());
      RevTree tree = walk.parseTree(commit.getTree().getId());

      CanonicalTreeParser treeParser = new CanonicalTreeParser();
      try (ObjectReader reader = repository.newObjectReader()) {
        treeParser.reset(reader, tree.getId());
      }

      walk.dispose();
      return treeParser;
    }
  }
}
