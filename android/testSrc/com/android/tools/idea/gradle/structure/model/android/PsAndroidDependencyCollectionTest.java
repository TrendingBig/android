/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.gradle.structure.model.android;

import com.android.ide.common.repository.GradleVersion;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link PsAndroidDependencyCollection}.
 */
public class PsAndroidDependencyCollectionTest {
  @Test
  public void testCompareWithPerfectMatch() {
    assertEquals(0, PsAndroidDependencyCollection.compare(GradleVersion.parse("1.0.0"), GradleVersion.parse("1.0.0")));
  }

  @Test
  public void testCompareWithPlusSignInVersion() {
    GradleVersion versionFromGradle = GradleVersion.parse("1.0.0");
    GradleVersion parsedVersion = GradleVersion.parse("+");
    assertEquals(0, PsAndroidDependencyCollection.compare(parsedVersion, versionFromGradle));
    parsedVersion = GradleVersion.parse("1.+");
    assertEquals(0, PsAndroidDependencyCollection.compare(parsedVersion, versionFromGradle));
    parsedVersion = GradleVersion.parse("1.0.+");
    assertEquals(0, PsAndroidDependencyCollection.compare(parsedVersion, versionFromGradle));
  }
}