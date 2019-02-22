## Run incremental analysis in local

By default, this plugin runs to detect changes between your `HEAD` and `refs/heads/master`.
This fits [the GitHub Flow](https://githubflow.github.io/) and [the GitLab Flow](https://docs.gitlab.com/ee/workflow/gitlab_flow.html).

If you want to full analysis in local, use `<skip>` configuration then Maven run full analysis by default.
You may overwrite this configuration by profile activated in the CI build.

```xml
<plugin>
  <groupId>com.worksap.tools</groupId>
  <artifactId>incremental-analysis-maven-plugin</artifactId>
  <configuration>
    <skip>true</skip>
  </configuration>
</plugin>
```
