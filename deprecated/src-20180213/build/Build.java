/*
 * Bach - Java Shell Builder
 * Copyright (C) 2017 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

interface Build {

  Path TOOLS = Paths.get(".bach", "tools");
  Path SOURCE_BACH = Paths.get("src", "bach");
  Path SOURCE_TEST = Paths.get("src", "test");
  Path TARGET = Paths.get("target", "build");
  Path TARGET_MAIN = TARGET.resolve("classes/main");
  Path TARGET_TEST = TARGET.resolve("classes/test");
  Path JAVADOC = TARGET.resolve("javadoc");
  Path ARTIFACTS = TARGET.resolve("artifacts");
  Path BACH_JAVA = SOURCE_BACH.resolve("Bach.java");

  String JUNIT_JUPITER = "5.1.0-RC1";
  String JUNIT_PLATFORM = "1.1.0-RC1";
  String OPENTEST4J = "1.0.0";
  String API_GUARDIAN = "1.0.0";

  static void main(String... args) {
    System.setProperty("bach.verbose", "true");
    try {
      format();
      clean();
      compile();
      test();
      javadoc();
      jar();
      jdeps();
    } catch (Throwable throwable) {
      System.err.println("build failed due to: " + throwable);
      throwable.printStackTrace();
      System.exit(1);
    }
  }

  static void format() throws IOException {
    System.out.printf("%n[format]%n%n");
    /*
    String repo = "https://jitpack.io";
    String user = "com/github/google";
    String name = "google-java-format";
    String version = "master-SNAPSHOT";
    String file = name + "-" + version + "-all-deps.jar";
    URI uri = URI.create(String.join("/", repo, user, name, name, version, file));
    */
    String version = "1.5";
    String base = "https://github.com/google/";
    String name = "google-java-format";
    String file = name + "-" + version + "-all-deps.jar";
    URI uri = URI.create(base + name + "/releases/download/" + name + "-" + version + "/" + file);
    Path jar = Bach.Basics.download(uri, TOOLS.resolve(name));
    Bach.JdkTool.Java java = new Bach.JdkTool.Java();
    java.jar = jar;
    java.toCommand().add("--version").run();
    Bach.Command format = java.toCommand();
    if (Boolean.getBoolean("bach.format.replace")) {
      format.add("--replace");
    } else {
      format.add("--dry-run");
      format.add("--set-exit-if-changed");
    }
    format.mark(5);
    List<Path> roots = List.of(Paths.get("src"), Paths.get("demo"));
    format.addAll(roots, Bach.Basics::isJavaFile);
    format.run();
  }

  static void clean() throws IOException {
    System.out.printf("%n[clean]%n%n");

    Bach.Basics.treeDelete(TARGET);
    System.out.println("deleted " + TARGET);
  }

  static void compile() throws IOException {
    System.out.printf("%n[compile]%n%n");

    // main
    Bach.JdkTool.Javac javac = new Bach.JdkTool.Javac();
    javac.generateAllDebuggingInformation = true;
    javac.destination = TARGET_MAIN;
    javac.classSourcePath = List.of(SOURCE_BACH);
    javac.run();

    // test
    javac.destination = TARGET_TEST;
    javac.classSourcePath = List.of(SOURCE_TEST);
    javac.classPath =
        List.of(
            TARGET_MAIN,
            Bach.Basics.resolve("org.junit.jupiter", "junit-jupiter-api", JUNIT_JUPITER),
            Bach.Basics.resolve("org.junit.platform", "junit-platform-commons", JUNIT_PLATFORM),
            Bach.Basics.resolve("org.apiguardian", "apiguardian-api", API_GUARDIAN),
            Bach.Basics.resolve("org.opentest4j", "opentest4j", OPENTEST4J));
    javac.run();
    // TODO exclude .java files
    Bach.Basics.treeCopy(SOURCE_TEST, TARGET_TEST, path -> !Bach.Basics.isJavaFile(path));
  }

  static void javadoc() throws IOException {
    System.out.printf("%n[javadoc]%n%n");

    Files.createDirectories(JAVADOC);
    Bach.run(
        "javadoc",
        "-html5",
        "-quiet",
        "-Xdoclint:all,-missing",
        "-package",
        "-linksource",
        "-link",
        "https://docs.oracle.com/javase/9/docs/api",
        "-d",
        JAVADOC,
        BACH_JAVA);
  }

  static void jar() throws IOException {
    System.out.printf("%n[jar]%n%n");

    Files.createDirectories(ARTIFACTS);
    jar("bach.jar", TARGET_MAIN);
    jar("bach-sources.jar", SOURCE_BACH);
    jar("bach-javadoc.jar", JAVADOC);
  }

  static void jar(String artifact, Path path) {
    Bach.JdkTool.Jar jar = new Bach.JdkTool.Jar();
    jar.file = ARTIFACTS.resolve(artifact);
    jar.path = path;
    jar.run();
  }

  static void jdeps() {
    System.out.printf("%n[jdeps]%n%n");

    Bach.JdkTool.Jdeps jdeps = new Bach.JdkTool.Jdeps();
    jdeps.summary = true;
    jdeps.recursive = true;
    jdeps.toCommand().add(ARTIFACTS.resolve("bach.jar")).run();
  }

  static void test() throws IOException {
    System.out.printf("%n[test]%n%n");

    String name = "junit-platform-console-standalone";
    /*
    String repo = "http://repo1.maven.org/maven2";
    String user = "org/junit/platform";
    String file = name + "-" + JUNIT_PLATFORM + ".jar";
    URI uri = URI.create(String.join("/", repo, user, name, JUNIT_PLATFORM, file));
    Path jar = Bach.Basics.download(uri, TOOLS.resolve(name), file, p -> true);
    */
    Path jar =
        new Bach.Basics.Resolvable("org.junit.platform", name, JUNIT_PLATFORM)
            .resolve(TOOLS.resolve(name), Bach.Basics.Resolvable.REPOSITORIES);
    Bach.run(
        "java",
        "-ea",
        "-Dbach.offline=" + System.getProperty("bach.offline", "false"),
        "-jar",
        jar,
        "--class-path",
        TARGET_TEST,
        "--class-path",
        TARGET_MAIN,
        "--scan-classpath");
  }
}
