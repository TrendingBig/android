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
package com.android.tools.idea.ui.resourcechooser;

import com.google.common.collect.Lists;
import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.HideableDecorator;
import com.intellij.ui.ListSpeedSearch;
import com.intellij.ui.speedSearch.FilteringListModel;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

/**
 * A list/grid of items that are split into sections.
 * A AbstractTreeStructure is used to create the gridlist.
 * the children of the root are the sections, and their leaves are the items.
 */
public class TreeGrid<T> extends Box {

  private final @NotNull ArrayList<JList<T>> myLists;
  private final @NotNull ArrayList<HideableDecorator> myHideables;
  private boolean myFiltered;

  public TreeGrid(final @NotNull AbstractTreeStructure model) {
    super(BoxLayout.Y_AXIS);

    // using the AbstractTreeStructure instead of the model as the actual TreeModel when used with IJ components
    // works in a very strange way, each time you expand or contract a node it will add or remove all its children.
    Object root = model.getRootElement();
    Object[] sections = model.getChildElements(root);

    myLists = Lists.newArrayListWithCapacity(sections.length);
    myHideables = Lists.newArrayListWithCapacity(sections.length);

    ListSelectionListener listSelectionListener = e -> {
      if (e.getValueIsAdjusting()) {
        return;
      }
      ListSelectionModel sourceSelectionModel = (ListSelectionModel)e.getSource();
      if (!sourceSelectionModel.isSelectionEmpty()) {
        for (JList<T> aList : myLists) {
          if (sourceSelectionModel != aList.getSelectionModel()) {
            aList.clearSelection();
          }
        }
      }
    };

    for (Object section : sections) {
      JPanel panel = new JPanel(new BorderLayout()) {// must be borderlayout for HideableDecorator to work
        @Override
        public Dimension getMaximumSize() {
          return new Dimension(super.getMaximumSize().width, super.getPreferredSize().height);
        }
      };
      String name = section.toString();
      HideableDecorator hidyPanel = new HideableDecorator(panel, name, false);

      FilteringListModel<T> listModel = new FilteringListModel<>(new AbstractListModel() {
        @Override
        public int getSize() {
          return model.getChildElements(section).length;
        }

        @Override
        public Object getElementAt(int index) {
          return model.getChildElements(section)[index];
        }
      });
      listModel.refilter(); // Needed as otherwise the filtered list does not show any content.

      // JBList does not work with HORIZONTAL_WRAP
      //noinspection UndesirableClassUsage,unchecked
      final JList<T> list = new JList<>(listModel);
      list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      list.setVisibleRowCount(-1);
      list.getSelectionModel().addListSelectionListener(listSelectionListener);
      list.setName(name); // for tests to find the right list
      list.addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
          // Allow jumping between lists
          int keyCode = e.getKeyCode();
          if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_UP) {
            Object source = e.getSource();
            if (source instanceof JList) {
              //noinspection unchecked
              JList<T> list = (JList<T>)source;
              int selectedIndex = list.getSelectedIndex();
              if (keyCode == KeyEvent.VK_DOWN) {
                // Try to jump to the next list. (If it's empty, continue jumping until we find a list with items.)
                int size = list.getModel().getSize();
                if (selectedIndex == size - 1) {
                  for (int index = ContainerUtil.indexOf(myLists, list) + 1; index != 0 && index < myLists.size(); index++) {
                    //noinspection unchecked
                    JList<T> nextList = myLists.get(index);
                    if (nextList.getModel().getSize() > 0) {
                      list.clearSelection();
                      nextList.setSelectedIndex(0);
                      ensureIndexVisible(nextList, 0);
                      IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
                        IdeFocusManager.getGlobalInstance().requestFocus(nextList, true);
                      });
                      e.consume();
                      break;
                    }
                  }
                }
              }
              else {
                //noinspection ConstantConditions
                assert keyCode == KeyEvent.VK_UP : keyCode;
                // Try to jump up to the previous list (until we find a nonempty one where we can jump to the last item)
                if (selectedIndex == 0) {
                  for (int index = ContainerUtil.indexOf(myLists, list) - 1; index >= 0; index--) {
                    //noinspection unchecked
                    JList<T> prevList = myLists.get(index);
                    int count = prevList.getModel().getSize();
                    if (count > 0) {
                      list.clearSelection();
                      prevList.setSelectedIndex(count - 1);
                      ensureIndexVisible(prevList, count -1);
                      IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
                        IdeFocusManager.getGlobalInstance().requestFocus(prevList, true);
                      });
                      e.consume();
                      break;
                    }
                  }
                }
              }
            }
          }
        }
      });

      new ListSpeedSearch(list);

      myLists.add(list);
      myHideables.add(hidyPanel);
      hidyPanel.setContentComponent(list);
      add(panel);

    }
  }

  public void addListSelectionListener(@NotNull ListSelectionListener lsl) {
    for (JList<T> list : myLists) {
      list.getSelectionModel().addListSelectionListener(lsl);
    }
  }

  public void setCellRenderer(@NotNull ListCellRenderer<T> cellRenderer) {
    for (JList<T> list : myLists) {
      list.setCellRenderer(cellRenderer);
    }
  }

  public void setFixedCellWidth(int width) {
    for (JList<T> list : myLists) {
      list.setFixedCellWidth(width);
    }
  }

  public void setFixedCellHeight(int height) {
    for (JList<T> list : myLists) {
      list.setFixedCellHeight(height);
    }
  }

  public void expandAll() {
    for (HideableDecorator hidyPanel : myHideables) {
      hidyPanel.setOn(true);
    }
  }

  public @Nullable T getSelectedElement() {
    for (JList<T> list : myLists) {
      if (list.getSelectedIndex() > -1) {
        return list.getSelectedValue();
      }
    }
    return null;
  }

  public void setSelectedElement(@Nullable T selectedElement) {
    for (JList<T> list : myLists) {
      if (selectedElement == null) {
        list.clearSelection();
      }
      else {
        for (int i = 0; i < list.getModel().getSize(); i++) {
          if (list.getModel().getElementAt(i) == selectedElement) {
            list.setSelectedIndex(i);
            ensureIndexVisible(list, i);
            return;
          }
        }
      }
    }
  }

  // we do this in invokeLater to make sure things like expandAll() have had their effect.
  private void ensureIndexVisible(@NotNull JList<T> list, int index) {
    // Use an invokeLater to ensure that layout has been performed (such that
    // the coordinate math of looking up the list position correctly gets
    // the offset of the list containing the match)
    ApplicationManager.getApplication().invokeLater(() -> {
      //list.ensureIndexIsVisible(index);
      Rectangle cellBounds = list.getCellBounds(index, index);
      if (cellBounds != null) {
        list.getBounds();
        Rectangle rectangle = SwingUtilities.convertRectangle(list, cellBounds, this);
        scrollRectToVisible(rectangle);
      }
    }, ModalityState.any());
  }

  @Override
  @SuppressWarnings("NonSynchronizedMethodOverridesSynchronizedMethod")
  public void addMouseListener(@NotNull MouseListener l) {
    for (JList<T> list : myLists) {
      list.addMouseListener(l);
    }
  }

  @Override
  @SuppressWarnings("NonSynchronizedMethodOverridesSynchronizedMethod")
  public void removeMouseListener(@NotNull MouseListener l) {
    for (JList<T> list : myLists) {
      list.removeMouseListener(l);
    }
  }

  public void setLayoutOrientation(int mode) {
    for (JList<T> list : myLists) {
      list.setLayoutOrientation(mode);
    }
  }

  public void setFilter(@Nullable Condition<T> condition) {
    myFiltered = condition != null;
    for (JList<T> list : myLists) {
      //noinspection unchecked
      ((FilteringListModel<T>)list.getModel()).setFilter(condition);
    }
  }

  public boolean isFiltered() {
    return myFiltered;
  }

  public void selectIfUnique() {
    T single = findSingleItem();
    if (single != null) {
      setSelectedElement(single);
    }
  }

  @Nullable
  private T findSingleItem() {
    T singleMatch = null;
    boolean found = false;

    for (JList<T> list : myLists) {
      ListModel<T> model = list.getModel();
      int size = model.getSize();
      if (size == 1) {
        if (found) {
          return null;
        } else {
          found = true;
          singleMatch = model.getElementAt(0);
        }
      } else if (size > 1) {
        return null;
      }
    }

    return singleMatch;
  }

  void selectFirst() {
    for (JList<T> list : myLists) {
      ListModel<T> model = list.getModel();
      int size = model.getSize();
      if (size > 0) {
        T item = model.getElementAt(0);
        setSelectedElement(item);
        IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
          IdeFocusManager.getGlobalInstance().requestFocus(list, true);
        });
        ensureIndexVisible(list, 0);
        return;
      }
    }
  }
}
