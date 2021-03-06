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
package com.android.tools.idea.tests.gui.framework.fixture;

import com.android.tools.idea.tests.gui.framework.GuiTests;
import com.intellij.find.impl.FindDialog;
import com.intellij.openapi.ui.ComboBox;
import org.fest.swing.edt.GuiTask;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static com.google.common.truth.Truth.assertThat;
import static org.fest.swing.edt.GuiActionRunner.execute;
import static org.junit.Assert.assertNotNull;

public class FindDialogFixture extends IdeaDialogFixture<FindDialog> {
  @NotNull
  public static FindDialogFixture find(@NotNull IdeFrameFixture ideFrameFixture) {
    return new FindDialogFixture(ideFrameFixture, find(ideFrameFixture.robot(), FindDialog.class));
  }

  private final IdeFrameFixture myIdeFrameFixture;

  private FindDialogFixture(@NotNull IdeFrameFixture ideFrameFixture, @NotNull DialogAndWrapper<FindDialog> dialogAndWrapper) {
    super(ideFrameFixture.robot(), dialogAndWrapper);
    myIdeFrameFixture = ideFrameFixture;
  }

  @NotNull
  public FindDialogFixture setTextToFind(@NotNull final String text) {
    execute(new GuiTask() {
      @Override
      protected void executeInEDT() throws Throwable {
        JComponent c = getDialogWrapper().getPreferredFocusedComponent();
        assertThat(c).isInstanceOf(ComboBox.class);
        ComboBox input = (ComboBox)c;
        assertNotNull(input);
        input.setSelectedItem(text);
      }
    });
    return this;
  }

  @NotNull
  public FindToolWindowFixture.ContentFixture clickFind() {
    GuiTests.findAndClickButton(this, "Find");
    GuiTests.waitForBackgroundTasks(robot());
    return new FindToolWindowFixture.ContentFixture(myIdeFrameFixture);
  }
}
