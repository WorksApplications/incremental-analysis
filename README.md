# Incremental Analysis Maven Plugin

This Maven Plugin helps developers to run static analysis only for updated codes.
Designed to use in incremental build, like pre-merge build and local build.

[![Build Status](https://travis-ci.com/WorksApplications/incremental-analysis.svg?branch=master)](https://travis-ci.com/WorksApplications/incremental-analysis)
[![Commitizen friendly](https://img.shields.io/badge/commitizen-friendly-brightgreen.svg)](http://commitizen.github.io/cz-cli/)

## How to use

Refer [this plugin's project documentation](https://worksapplications.github.io/incremental-analysis/plugin-info.html) for detail.

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
