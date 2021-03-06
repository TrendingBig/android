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

package com.android.tools.adtui.visual;

import com.android.annotations.NonNull;
import com.android.tools.adtui.*;
import com.android.tools.adtui.chart.linechart.LineChart;
import com.android.tools.adtui.common.AdtUiUtils;
import com.android.tools.adtui.model.*;
import com.intellij.util.containers.ImmutableList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LineChartVisualTest extends VisualTest {

  @NonNull
  private LineChart mLineChart;

  @NonNull
  private List<RangedContinuousSeries> mRangedData;

  @NonNull
  private List<DefaultDataSeries<Long>> mData;

  @NonNull
  private AnimatedTimeRange mAnimatedTimeRange;

  @Override
  protected List<Animatable> createComponentsList() {
    mRangedData = new ArrayList<>();
    mData = new ArrayList<>();

    long now = System.currentTimeMillis();
    Range xRange = new Range(now, now + 60000);
    mLineChart = new LineChart();
    mAnimatedTimeRange = new AnimatedTimeRange(xRange, 0);

    List<Animatable> componentsList = new ArrayList<>();

    // Add the scene components to the list
    componentsList.add(mAnimatedTimeRange);
    componentsList.add(xRange);
    componentsList.add(mLineChart);

    Range mYRange = new Range(0.0, 100.0);
    for (int i = 0; i < 4; i++) {
      if (i % 2 == 0) {
        mYRange = new Range(0.0, 100.0);
        componentsList.add(mYRange);
      }
      DefaultDataSeries<Long> series = new DefaultDataSeries<>();
      RangedContinuousSeries ranged =
        new RangedContinuousSeries("Widgets", xRange, mYRange, series);
      mRangedData.add(ranged);
      mData.add(series);
    }
    mLineChart.addLines(mRangedData);

    return componentsList;
  }

  @Override
  public String getName() {
    return "LineChart";
  }

  @Override
  protected void populateUi(@NonNull JPanel panel) {
    JPanel controls = VisualTest.createControlledPane(panel, mLineChart);
    mLineChart.setBorder(BorderFactory.createLineBorder(AdtUiUtils.DEFAULT_BORDER_COLOR));

    final AtomicInteger variance = new AtomicInteger(10);
    final AtomicInteger delay = new AtomicInteger(100);
    Thread mUpdateDataThread = new Thread() {
      @Override
      public void run() {
        super.run();
        try {
          while (true) {
            int v = variance.get();
            long now = System.currentTimeMillis();
            for (DefaultDataSeries<Long> series : mData) {
              ImmutableList<SeriesData<Long>> data = series.getAllData();
              long last = data.isEmpty() ? 0 : data.get(data.size() - 1).value;
              float delta = ((float)Math.random() - 0.45f) * v;
              // Make sure not to add negative numbers.
              long current = Math.max(last + (long)delta, 0);
              series.add(now, current);
            }
            Thread.sleep(delay.get());
          }
        }
        catch (InterruptedException e) {
        }
      }
    };

    mUpdateDataThread.start();
    controls.add(VisualTest.createVariableSlider("Delay", 10, 5000, new VisualTests.Value() {
      @Override
      public void set(int v) {
        delay.set(v);
      }

      @Override
      public int get() {
        return delay.get();
      }
    }));
    controls.add(VisualTest.createVariableSlider("Variance", 0, 50, new VisualTests.Value() {
      @Override
      public void set(int v) {
        variance.set(v);
      }

      @Override
      public int get() {
        return variance.get();
      }
    }));
    controls.add(VisualTest.createCheckbox("Shift xRange Min", itemEvent ->
      mAnimatedTimeRange.setShift(itemEvent.getStateChange() == ItemEvent.SELECTED)));
    controls.add(VisualTest.createCheckbox("Stepped chart", itemEvent -> {
      boolean isStepped = itemEvent.getStateChange() == ItemEvent.SELECTED;
      // Make only some lines stepped
      for (int i = 0; i < mRangedData.size(); i += 2) {
        RangedContinuousSeries series = mRangedData.get(i);
        mLineChart.getLineConfig(series).setStepped(isStepped);
      }
    }));
    controls.add(VisualTest.createCheckbox("Dashed lines", itemEvent -> {
      boolean isDashed = itemEvent.getStateChange() == ItemEvent.SELECTED;
      // Dash only some lines
      for (int i = 0; i < mRangedData.size(); i += 2) {
        RangedContinuousSeries series = mRangedData.get(i);
        mLineChart.getLineConfig(series).setDashed(isDashed);
      }
    }));
    controls.add(VisualTest.createCheckbox("Filled lines", itemEvent -> {
      boolean isFilled = itemEvent.getStateChange() == ItemEvent.SELECTED;
      // Fill only some lines
      for (int i = 0; i < mRangedData.size(); i += 2) {
        RangedContinuousSeries series = mRangedData.get(i);
        mLineChart.getLineConfig(series).setFilled(isFilled);
      }
    }));
    controls.add(VisualTest.createCheckbox("Stacked lines", itemEvent -> {
      boolean isStacked = itemEvent.getStateChange() == ItemEvent.SELECTED;
      // Stack only some lines
      for (int i = 0; i < mRangedData.size(); i += 2) {
        RangedContinuousSeries series = mRangedData.get(i);
        mLineChart.getLineConfig(series).setStacked(isStacked);
      }
    }));

    controls.add(
      new Box.Filler(new Dimension(0, 0), new Dimension(300, Integer.MAX_VALUE),
                     new Dimension(300, Integer.MAX_VALUE)));
  }
}
