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
package com.android.tools.idea.logcat;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.logcat.LogCatMessage;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@Ignore
// TODO Update these tests to the new logcat implementation
public class AndroidLogcatServiceTest {
  private static class LogLineReceiver implements AndroidLogcatService.LogLineListener {
    private final String[] EXPECTED_LOGS = {
      "08-18 16:39:11.439: W/DummyFirst(1493): First Line1",
      "08-18 16:39:11.439: W/DummyFirst(1493): First Line2",
      "08-18 16:39:11.439: W/DummyFirst(1493): First Line3",
      "09-20 16:39:11.439: W/DummySecond(1493): Second Line1"};
    private int myCurrentIndex = 0;

    @Override
    public void receiveLogLine(@NotNull LogCatMessage line) {
      assertEquals(EXPECTED_LOGS[myCurrentIndex++], line.toString());
    }

    public void reset() {
      myCurrentIndex = 0;
    }

    public void assertAllReceived() {
      assertEquals(EXPECTED_LOGS.length, myCurrentIndex);
    }

    public void assertNothingReceived() {
      assertEquals(0, myCurrentIndex);
    }
  }

  private LogLineReceiver myLogLineListener;
  private IDevice mockDevice = mock(IDevice.class);
  private AndroidLogcatService myLogcatService;

  private String myBufferSize;

  @Before
  public void setUp() throws Exception {
    //AndroidLogcatService.LogcatRunner logcatRunner = new AndroidLogcatService.LogcatRunner() {
    //
    //  @Override
    //  public void start(@NotNull IDevice device, @NotNull AndroidLogcatReceiver receiver) {
    //    receiver.processNewLine("[ 08-18 16:39:11.439 1493:1595 W/DummyFirst     ]");
    //    receiver.processNewLine("First Line1");
    //    receiver.processNewLine("First Line2");
    //    receiver.processNewLine("First Line3");
    //    receiver.processNewLine("[ 09-20 16:39:11.439 1493:1595 W/DummySecond     ]");
    //    receiver.processNewLine("Second Line1");
    //    //myLogcatService.myDeviceLatches.get(device).countDown();
    //  }
    //};
    //myLogcatService = new AndroidLogcatService(logcatRunner);
    myLogLineListener =  new LogLineReceiver();

    myBufferSize = System.setProperty("idea.cycle.buffer.size", "disabled");
  }

  @After
  public void tearDown() {
    if (myBufferSize != null) {
      System.setProperty("idea.cycle.buffer.size", myBufferSize);
    } else {
      System.clearProperty("idea.cycle.buffer.size");
    }
  }

  @Test
  public void testDeviceConnectedAfterListening() throws Exception {
    when(mockDevice.isOnline()).thenReturn(false);
    myLogcatService.addListener(mockDevice, myLogLineListener);
    when(mockDevice.isOnline()).thenReturn(true);
    myLogcatService.deviceConnected(mockDevice);

    myLogLineListener.assertAllReceived();
    verify(mockDevice, times(2)).isOnline();
    verify(mockDevice, times(2)).getClientName(1493);
    verifyNoMoreInteractions(mockDevice);
  }

  /**
   * Tests {@link AndroidLogcatService} to make sure that it will receive logs from devices which are connected
   * prior to it's initialization. AndroidLogcatService will start receiving logs in this devices whenever some listener starts listening
   */
  @Test
  public void testDeviceConnectedBeforeListening() throws Exception {
    when(mockDevice.isOnline()).thenReturn(true);
    myLogcatService.addListener(mockDevice, myLogLineListener, true);
    myLogLineListener.assertAllReceived();
    verify(mockDevice, times(1)).isOnline();
    verify(mockDevice, times(2)).getClientName(1493);
    verifyNoMoreInteractions(mockDevice);
  }

  /**
   * Tests {@link AndroidLogcatService#addListener(IDevice, AndroidLogcatService.LogLineListener, boolean)},
   * to make sure that it adds correctly old logs from buffer
   */
  @Test
  public void testAddOldLogs() {
    when(mockDevice.isOnline()).thenReturn(true);
    myLogcatService.deviceConnected(mockDevice);
    myLogcatService.addListener(mockDevice, myLogLineListener, true);

    myLogLineListener.assertAllReceived();
    verify(mockDevice, times(2)).isOnline();
    verify(mockDevice, times(2)).getClientName(1493);
    verifyNoMoreInteractions(mockDevice);
  }

  @Test
  public void testDeviceDisconnectedAndConnected() {
    when(mockDevice.isOnline()).thenReturn(false);
    myLogcatService.addListener(mockDevice, myLogLineListener);

    when(mockDevice.isOnline()).thenReturn(true);
    myLogcatService.deviceConnected(mockDevice);
    myLogLineListener.assertAllReceived();

    when(mockDevice.isOnline()).thenReturn(false);
    myLogcatService.deviceDisconnected(mockDevice);

    myLogLineListener.reset();
    when(mockDevice.isOnline()).thenReturn(true);
    myLogcatService.deviceConnected(mockDevice);
    myLogLineListener.assertAllReceived();

    verify(mockDevice, times(3)).isOnline();
    verify(mockDevice, times(4)).getClientName(1493);
    verifyNoMoreInteractions(mockDevice);
  }

  @Test
  public void testDeviceChanged() {
    when(mockDevice.isOnline()).thenReturn(true);
    myLogcatService.deviceConnected(mockDevice);
    myLogcatService.addListener(mockDevice, myLogLineListener, true);
    myLogLineListener.assertAllReceived();

    myLogLineListener.reset();
    when(mockDevice.isOnline()).thenReturn(false);
    myLogcatService.deviceChanged(mockDevice, 0);
    myLogLineListener.assertNothingReceived();

    myLogLineListener.reset();
    when(mockDevice.isOnline()).thenReturn(true);
    myLogcatService.deviceChanged(mockDevice, 0);
    myLogLineListener.assertAllReceived();

    verify(mockDevice, times(4)).isOnline();
    verify(mockDevice, times(4)).getClientName(1493);
    verifyNoMoreInteractions(mockDevice);
  }

  @Test
  public void testRemoveListener() {
    when(mockDevice.isOnline()).thenReturn(true);
    myLogcatService.deviceConnected(mockDevice);
    myLogcatService.addListener(mockDevice, myLogLineListener, true);
    myLogLineListener.assertAllReceived();

    myLogcatService.removeListener(mockDevice, myLogLineListener);
    when(mockDevice.isOnline()).thenReturn(false);
    myLogcatService.deviceDisconnected(mockDevice);

    // Try to reconnect and make sure that it received nothing
    myLogLineListener.reset();
    when(mockDevice.isOnline()).thenReturn(true);
    myLogcatService.deviceConnected(mockDevice);
    myLogLineListener.assertNothingReceived();

    verify(mockDevice, times(3)).isOnline();
    verify(mockDevice, times(4)).getClientName(1493);
    verifyNoMoreInteractions(mockDevice);
  }

  /**
   * Tests {@link AndroidLogcatService} to verify that when no one is interested in a device logs, AndroidLogcatService should
   * stop receiving logs from the device. Subsequently, if a listener will be interested in the device, it should receive all logs
   */
  @Test
  public void testNoDeviceListener() {
    when(mockDevice.isOnline()).thenReturn(true);
    myLogcatService.deviceConnected(mockDevice);
    myLogcatService.addListener(mockDevice, myLogLineListener, true);
    myLogLineListener.assertAllReceived();

    myLogcatService.removeListener(mockDevice, myLogLineListener);
    myLogLineListener.reset();
    myLogcatService.addListener(mockDevice, myLogLineListener, false);
    myLogLineListener.assertAllReceived();

    verify(mockDevice, times(3)).isOnline();
    verify(mockDevice, times(4)).getClientName(1493);
    verifyNoMoreInteractions(mockDevice);
  }
}
