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
package com.android.tools.idea.gradle.structure.configurables.ui.dependencies;

import com.android.tools.idea.gradle.AndroidGradleModel;
import com.android.tools.idea.gradle.structure.configurables.PsContext;
import com.android.tools.idea.gradle.structure.configurables.ui.PsUISettings;
import com.android.tools.idea.gradle.structure.configurables.ui.dependencies.AbstractDeclaredDependenciesTableModel.DependencyCellRenderer;
import com.android.tools.idea.gradle.structure.model.PsArtifactDependencySpec;
import com.android.tools.idea.gradle.structure.model.PsProject;
import com.android.tools.idea.gradle.structure.model.android.PsAndroidDependency;
import com.android.tools.idea.gradle.structure.model.android.PsAndroidModule;
import com.android.tools.idea.gradle.structure.model.android.PsLibraryAndroidDependency;
import com.google.common.collect.Lists;
import com.intellij.testFramework.IdeaTestCase;
import com.intellij.util.ui.ColumnInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

import static com.android.tools.idea.gradle.structure.model.PsDependency.TextType.PLAIN_TEXT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AbstractDeclaredDependenciesTableModel}.
 */
public class AbstractDeclaredDependenciesTableModelTest extends IdeaTestCase {
  private boolean myOriginalShowGroupId;
  private PsLibraryAndroidDependency myLibraryDependency;

  private AbstractDeclaredDependenciesTableModel<PsAndroidDependency> myTableModel;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    myOriginalShowGroupId = PsUISettings.getInstance().DECLARED_DEPENDENCIES_SHOW_GROUP_ID;
    myLibraryDependency = mock(PsLibraryAndroidDependency.class);

    List<PsAndroidDependency> dependencies = Lists.newArrayList();
    dependencies.add(myLibraryDependency);
    PsAndroidModuleStub module = new PsAndroidModuleStub(dependencies);
    myTableModel = new AbstractDeclaredDependenciesTableModel<PsAndroidDependency>(module, mock(PsContext.class)) {};
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      PsUISettings.getInstance().DECLARED_DEPENDENCIES_SHOW_GROUP_ID = myOriginalShowGroupId;
    }
    finally {
      //noinspection ThrowFromFinallyBlock
      super.tearDown();
    }
  }

  public void testShowArtifactDependencySpec() {
    PsArtifactDependencySpec spec = new PsArtifactDependencySpec("appcompat-v7", "com.android.support", "23.1.0");
    when(myLibraryDependency.getResolvedSpec()).thenReturn(spec);
    when(myLibraryDependency.getDeclaredSpec()).thenReturn(spec);
    when(myLibraryDependency.toText(PLAIN_TEXT)).thenReturn("com.android.support:appcompat-v7:23.1.0");

    ColumnInfo[] columnInfos = myTableModel.getColumnInfos();

    PsUISettings.getInstance().DECLARED_DEPENDENCIES_SHOW_GROUP_ID = true;

    //noinspection unchecked
    ColumnInfo<PsAndroidDependency, String> specColumnInfo = columnInfos[0];
    DependencyCellRenderer renderer = (DependencyCellRenderer)specColumnInfo.getRenderer(myLibraryDependency);
    assertNotNull(renderer);

    String text = renderer.getText();
    assertEquals("com.android.support:appcompat-v7:23.1.0", text);

    PsUISettings.getInstance().DECLARED_DEPENDENCIES_SHOW_GROUP_ID = false;

    text = renderer.getText();
    assertEquals("appcompat-v7:23.1.0", text);
  }

  public void testShowConfigurationName() {
    when(myLibraryDependency.getJoinedConfigurationNames()).thenReturn("compile");

    ColumnInfo[] columnInfos = myTableModel.getColumnInfos();
    //noinspection unchecked
    ColumnInfo<PsAndroidDependency, String> scopeColumnInfo = columnInfos[1];
    String columnText = scopeColumnInfo.valueOf(myLibraryDependency);
    assertEquals("compile", columnText);
  }

  private class PsAndroidModuleStub extends PsAndroidModule {
    @NotNull private final List<PsAndroidDependency> myDeclaredDependencies;

    public PsAndroidModuleStub(@NotNull List<PsAndroidDependency> declaredDependencies) {
      super(new PsProject(myProject), myModule, "", mock(AndroidGradleModel.class));
      myDeclaredDependencies = declaredDependencies;
    }

    @Override
    public void forEachDeclaredDependency(@NotNull Consumer<PsAndroidDependency> consumer) {
      myDeclaredDependencies.forEach(consumer);
    }
  }
}