/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.idea.gradle.customizer.cpp;

import com.android.builder.model.NativeArtifact;
import com.android.builder.model.NativeFile;
import com.android.builder.model.NativeFolder;
import com.android.tools.idea.gradle.NativeAndroidGradleModel;
import com.android.tools.idea.gradle.stubs.android.NativeAndroidProjectStub;
import com.android.tools.idea.gradle.util.Projects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.IdeaTestCase;

import java.io.File;
import java.util.List;
import java.util.Set;

import static com.android.tools.idea.gradle.TestProjects.createNativeProject;
import static com.intellij.openapi.util.io.FileUtil.toSystemIndependentName;
import static com.intellij.openapi.vfs.VfsUtilCore.urlToPath;
import static com.intellij.util.ExceptionUtil.rethrowAllAsUnchecked;
import static org.jetbrains.plugins.gradle.util.GradleConstants.SYSTEM_ID;

/**
 * Tests for {@link ContentRootModuleCustomizer}.
 */
public class ContentRootModuleCustomizerTest extends IdeaTestCase {
  private NativeAndroidProjectStub myNativeAndroidProject;
  private NativeAndroidGradleModel myNativeAndroidGradleModel;

  private ContentRootModuleCustomizer myCustomizer;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    File baseDir = Projects.getBaseDirPath(myProject);
    myNativeAndroidProject = createNativeProject(baseDir, myProject.getName());

    myNativeAndroidGradleModel = new NativeAndroidGradleModel(SYSTEM_ID, myNativeAndroidProject.getName(), baseDir, myNativeAndroidProject);

    addContentEntry();
    myCustomizer = new ContentRootModuleCustomizer();
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      if (myNativeAndroidProject != null) {
        myNativeAndroidProject.dispose();
      }
    }
    finally {
      //noinspection ThrowFromFinallyBlock
      super.tearDown();
    }
  }

  private void addContentEntry() {
    VirtualFile moduleFile = myModule.getModuleFile();
    assertNotNull(moduleFile);
    final VirtualFile moduleDir = moduleFile.getParent();

    WriteCommandAction.runWriteCommandAction(null, () -> {
      ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(myModule);
      ModifiableRootModel model = moduleRootManager.getModifiableModel();
      model.addContentEntry(moduleDir);
      model.commit();
    });
  }

  public void testCustomizeModuleWithDefaultModel() {
    verifyCustomizeModule();
  }

  public void testCustomizeModuleWithModel200() {
    myNativeAndroidProject.setModelVersion("2.0.0");
    verifyCustomizeModule();
  }

  private void verifyCustomizeModule() {
    final IdeModifiableModelsProviderImpl modelsProvider = new IdeModifiableModelsProviderImpl(myProject);
    try {
      myCustomizer.customizeModule(myProject, myModule, modelsProvider, myNativeAndroidGradleModel);
      ApplicationManager.getApplication().runWriteAction(modelsProvider::commit);
    }
    catch (Throwable t) {
      modelsProvider.dispose();
      rethrowAllAsUnchecked(t);
    }

    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(myModule);
    ContentEntry contentEntry = moduleRootManager.getContentEntries()[0];

    SourceFolder[] sourceFolders = contentEntry.getSourceFolders();
    List<String> sourcePaths = Lists.newArrayListWithExpectedSize(sourceFolders.length);

    for (SourceFolder folder : sourceFolders) {
      String path = urlToPath(folder.getUrl());
      sourcePaths.add(toSystemIndependentName(path));
    }

    List<String> allExpectedPaths = Lists.newArrayList();

    Set<File> sourceFolderPaths = Sets.newLinkedHashSet();
    for (NativeArtifact artifact : myNativeAndroidProject.getArtifacts()) {
      if (myNativeAndroidGradleModel.modelVersionIsAtLeast("2.0.0")) {
        for (File headerRoot : artifact.getExportedHeaders()) {
          sourceFolderPaths.add(headerRoot);
        }
      }
      for (NativeFolder sourceFolder : artifact.getSourceFolders()) {
        sourceFolderPaths.add(sourceFolder.getFolderPath());
      }
      for (NativeFile sourceFile : artifact.getSourceFiles()) {
        File parentFile = sourceFile.getFilePath().getParentFile();
        if (parentFile != null) {
          sourceFolderPaths.add(parentFile);
        }
      }
    }

    for (File file : sourceFolderPaths) {
      allExpectedPaths.add(toSystemIndependentName(file.getPath()));
    }

    assertEquals(allExpectedPaths, sourcePaths);
  }
}
