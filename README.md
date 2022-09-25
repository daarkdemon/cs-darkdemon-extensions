# `Cloudstream3 Plugin Repository`
 > Used to load providers for [Cloudstream](https://recloudstream.github.io/)

<!--
## Steps to install this repo:
1. Open [Cloudstream](https://recloudstream.github.io/) and go to Settings and click on extensions.
2. Click on add repository and enter name and this url.
-->

## Getting started with writing your first plugin

1. Open the root build.gradle.kts, read the comments and replace all the placeholders
2. Familiarize yourself with the project structure. Most files are commented
3. Build or deploy your first plugin using:
    - Windows: `.\gradlew.bat ExampleProvider:make` or `.\gradlew.bat ExampleProvider:deployWithAdb`
    - Linux & Mac: `./gradlew ExampleProvider:make` or `./gradlew ExampleProvider:deployWithAdb`
