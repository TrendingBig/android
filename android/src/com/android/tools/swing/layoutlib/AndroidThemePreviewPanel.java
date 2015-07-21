/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.tools.swing.layoutlib;

import com.android.ide.common.rendering.api.ILayoutPullParser;
import com.android.tools.idea.configurations.Configuration;
import com.android.tools.idea.rendering.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiFile;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;

/**
 * UI component that renders a theme.
 */
public class AndroidThemePreviewPanel extends JComponent {
  private static final Logger LOG = Logger.getInstance(AndroidThemePreviewPanel.class.getName());
  private static final String THEME_PREVIEW_LAYOUT = "/themeEditor/sample_layout.xml";

  private final AndroidFacet myFacet;
  private final PsiFile myPsiFile;
  private GraphicsLayoutRenderer myGraphicsLayoutRenderer;
  private ILayoutPullParser myParser;

  public AndroidThemePreviewPanel(PsiFile psiFile, Configuration configuration) throws IOException, SAXException {
    super();

    myPsiFile = psiFile;
    myFacet = AndroidFacet.getInstance(myPsiFile);
    final Document document =
      DomPullParser.createNewDocumentBuilder().parse(LayoutPullParserFactory.class.getResourceAsStream(THEME_PREVIEW_LAYOUT));
    myParser = new DomPullParser(document.getDocumentElement());
    updateConfiguration(configuration);
  }

  @Override
  public void setBounds(int x, int y, int width, int height) {
    super.setBounds(x, y, width, height);

    if (myGraphicsLayoutRenderer != null) {
      myGraphicsLayoutRenderer.setSize(getSize());
    }
  }

  /**
   * Updates the current configuration. You need to call this method is you change the configuration and want to update the rendered view.
   *
   * <p/>This will re-inflate the sample view with the new parameters in the configuration.
   * @param configuration
   */
  public void updateConfiguration(@NotNull Configuration configuration) {
    try {
      myGraphicsLayoutRenderer = GraphicsLayoutRenderer.create(myPsiFile, configuration, myParser);
    }
    catch (InitializationException e) {
      LOG.error(e);
    }
  }

  @Override
  public void paintComponent(final Graphics graphics) {
    super.paintComponent(graphics);

    if (myGraphicsLayoutRenderer != null) {
      myGraphicsLayoutRenderer.render((Graphics2D)graphics);
    }
  }
}