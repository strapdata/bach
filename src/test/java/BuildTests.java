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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class BuildTests {

  @Test
  void build() {
    Bach bach = new Bach.Builder().build();
    Assertions.assertNotNull(bach);
    Assumptions.assumeFalse(Boolean.getBoolean("BuildTests.running"));
    System.setProperty("BuildTests.running", "true");
    bach.resolve(
        "org.junit.jupiter.api",
        Bach.Util.maven("org.junit.jupiter", "junit-jupiter-api", "5.0.0-M4"));
    bach.resolve(
        "org.junit.platform.commons",
        Bach.Util.maven("org.junit.platform", "junit-platform-commons", "1.0.0-M4"));
    bach.resolve("org.opentest4j", Bach.Util.maven("org.opentest4j", "opentest4j", "1.0.0-M2"));
    bach.build();
    // TODO assert artifacts...
  }
}
