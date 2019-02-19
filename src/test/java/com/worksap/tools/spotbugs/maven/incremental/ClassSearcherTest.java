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

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ClassSearcherTest {
  @Rule public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void test() throws IOException {
    ClassSearcher searcher = createClassFiles("com.worksap", "ClassName", "AnotherClassName");
    List<String> result =
        searcher
            .search(Stream.of(Paths.get("com", "worksap", "ClassName.java").toString()))
            .collect(Collectors.toList());

    assertThat(result, hasSize(1));
    assertThat(result, hasItem("com.worksap.ClassName"));
  }

  @Test
  public void testInternalClass() throws IOException {
    ClassSearcher searcher =
        createClassFiles(
            "com.worksap.test", "ClassName", "ClassName$InternalClassName", "AnotherClassName");
    List<String> result =
        searcher
            .search(Stream.of(Paths.get("com", "worksap", "test", "ClassName.java").toString()))
            .collect(Collectors.toList());

    assertThat(result, hasSize(2));
    assertThat(
        result,
        both(hasItem("com.worksap.test.ClassName"))
            .and(hasItem("com.worksap.test.ClassName$InternalClassName")));
  }

  @Test
  public void testAnonymousClass() throws IOException {
    ClassSearcher searcher =
        createClassFiles("com.worksap.verify", "ClassName", "ClassName$1", "AnotherClassName");
    List<String> result =
        searcher
            .search(Stream.of(Paths.get("com", "worksap", "verify", "ClassName.java").toString()))
            .collect(Collectors.toList());

    assertThat(result, hasSize(2));
    assertThat(
        result,
        both(hasItem("com.worksap.verify.ClassName"))
            .and(hasItem("com.worksap.verify.ClassName$1")));
  }

  private ClassSearcher createClassFiles(String packageName, String... classNames)
      throws IOException {
    File classesDir = folder.newFolder("classes");
    ClassSearcher searcher = spy(new ClassSearcher(classesDir.toPath()));
    File packageDir = Paths.get(classesDir.getAbsolutePath(), packageName.split("\\.")).toFile();
    assertTrue(packageDir.mkdirs());

    for (String className : classNames) {
      File classFile = new File(packageDir, className + ".class");
      String sourceFileName = className;
      if (className.contains("$")) {
        sourceFileName = className.substring(0, className.indexOf("$"));
      }
      doReturn(sourceFileName + ".java").when(searcher).loadCompiledFrom(classFile.toPath());
      assertTrue(classFile.createNewFile());
    }

    return searcher;
  }
}
