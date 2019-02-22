# Incremental Analysis Maven Plugin

This Maven Plugin helps developers to run static analysis only for updated codes.
Designed to use in incremental build, like pre-merge build and local build.

[![Build Status](https://travis-ci.com/WorksApplications/incremental-analysis.svg?branch=master)](https://travis-ci.com/WorksApplications/incremental-analysis)
[![Commitizen friendly](https://img.shields.io/badge/commitizen-friendly-brightgreen.svg)](http://commitizen.github.io/cz-cli/)

## How to use

### Introduce incremental-analysis plugin to your Maven project

Add the following `<plugin>` element into your `pom.xml`:

```xml
<plugin>
  <groupId>com.worksap.tools</groupId>
  <artifactId>incremental-analysis-maven-plugin</artifactId>
  <version>1.0.1</version>
</plugin>
```

Make sure it runs before the [spotbugs-maven-plugin](https://github.com/spotbugs/spotbugs-maven-plugin/).

### Run incremental analysis in local

By default, this plugin runs to detect changes between your `HEAD` and `refs/heads/master`.
This fits [the GitHub Flow](https://githubflow.github.io/) and [the GitLab Flow](https://docs.gitlab.com/ee/workflow/gitlab_flow.html).

If you want to full analysis in local, use `<skip>` configuration then Maven run full analysis by default.
You may overwrite this configuration by profile activated in the CI build.

```xml
<plugin>
  <groupId>com.worksap.tools</groupId>
  <artifactId>incremental-analysis-maven-plugin</artifactId>
  <version>1.0.1</version>
  <configuration>
    <skip>true</skip>
  </configuration>
</plugin>
```

## Known problems

* No support for checkstyle, PMD and other tools.
* No support for other programming languages.
* This plugin does not ensure that the target branch has no potential bugs. It is possible to merge buggy code in several cases:
    1. You added `@CheckForNull` to a method defined in interface. Then SpotBugs may find potential bug in its implementation, but it cannot be found by incremental analysis because it scans updated classes only.
    2. You added `@CheckForNull` to a method. Then SpotBugs may find potential bug in its caller, but it cannot be found by incremental analysis because it scans updated classes only.

## Copyright

Copyright 2019 &copy; Works Applications Co.,Ltd.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
