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
package com.android.tools.idea.uibuilder.property.ptable.renderers;

import com.android.tools.idea.uibuilder.property.ptable.PTable;
import com.android.tools.idea.uibuilder.property.ptable.PTableItem;
import com.intellij.ide.ui.search.SearchUtil;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

import static com.android.SdkConstants.TOOLS_URI;
import static icons.AndroidIcons.NeleIcons.DesignProperty;

public class PNameRenderer implements TableCellRenderer {
  private final ColoredTableCellRenderer myRenderer = new Renderer();

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean cellHasFocus, int row, int column) {
    myRenderer.clear();
    PTableItem item = (PTableItem)value;

    myRenderer.getTableCellRendererComponent(table, value, isSelected, cellHasFocus, row, column);
    myRenderer.setBackground(isSelected ? UIUtil.getTableSelectionBackground() : table.getBackground());

    SimpleTextAttributes attr = SimpleTextAttributes.REGULAR_ATTRIBUTES;
    SearchUtil.appendFragments(((PTable)table).getSpeedSearch().getEnteredPrefix(), item.getName(), attr.getStyle(), attr.getFgColor(),
                               attr.getBgColor(), myRenderer);

    myRenderer.setToolTipText(item.getTooltipText());
    return myRenderer;
  }

  private static class Renderer extends ColoredTableCellRenderer {
    @Override
    protected void customizeCellRenderer(JTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
      setIcon((PTableItem)value, selected, hasFocus);
      setPaintFocusBorder(false);
      setFocusBorderAroundIcon(true);
    }

    private void setIcon(PTableItem item, boolean selected, boolean hasFocus) {
      Icon groupIcon = UIUtil.getTreeNodeIcon(item.isExpanded(), selected, hasFocus);
      int beforeGroupIcon = getBeforeIconSpacing(getDepth(item), groupIcon.getIconWidth());
      int afterGroupIcon = getAfterIconSpacing(groupIcon.getIconWidth());

      Icon icon;
      int indent;
      int textGap;
      if (item.hasChildren()) {
        icon = groupIcon;
        indent = beforeGroupIcon;
        textGap = afterGroupIcon;
      }
      else {
        icon = null;
        indent = beforeGroupIcon + groupIcon.getIconWidth() + afterGroupIcon;
        textGap = 0;
      }
      if (TOOLS_URI.equals(item.getNamespace())) {
        if (icon == null) {
          icon = DesignProperty;
        }
        else {
          LayeredIcon layered = new LayeredIcon(icon, DesignProperty);
          layered.setIcon(DesignProperty, 1, afterGroupIcon + icon.getIconWidth(), 0);
          icon = layered;
        }
        textGap = 4;
      }
      setIcon(icon);
      setIconTextGap(textGap);
      setIpad(new Insets(0, indent, 0, 0));
    }
  }

  public static boolean hitTestTreeNodeIcon(@NotNull PTableItem item, int x) {
    Icon icon = UIUtil.getTreeNodeIcon(item.isExpanded(), true, true);
    int beforeIcon = getBeforeIconSpacing(getDepth(item), icon.getIconWidth());
    return x >= beforeIcon && x <= beforeIcon + icon.getIconWidth();
  }

  private static int getBeforeIconSpacing(int depth, int iconWidth) {
    int nodeIndent = UIUtil.getTreeLeftChildIndent() + UIUtil.getTreeRightChildIndent();
    int leftIconOffset = Math.max(0, UIUtil.getTreeLeftChildIndent() - (iconWidth / 2));
    return nodeIndent * depth + leftIconOffset;
  }

  private static int getAfterIconSpacing(int iconWidth) {
    int nodeIndent = UIUtil.getTreeLeftChildIndent() + UIUtil.getTreeRightChildIndent();
    int leftIconOffset = Math.max(0, UIUtil.getTreeLeftChildIndent() - (iconWidth / 2));
    return Math.max(0, nodeIndent - leftIconOffset - iconWidth);
  }

  private static int getDepth(@NotNull PTableItem item) {
    int result = 0;
    while (item.getParent() != null) {
      result++;
      item = item.getParent();
      assert item != null;
    }
    return result;
  }
}
