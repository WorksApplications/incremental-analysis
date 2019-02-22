## Run incremental analysis in your Maven project

Refer [the plugin documentation](/plugin-info.html) and modify your `pom.xml` accordingly.

Make sure this plugin is described before the [spotbugs-maven-plugin](https://github.com/spotbugs/spotbugs-maven-plugin/).
Then spotbugs-maven-plugin can use updated property and run incremental analysis.
