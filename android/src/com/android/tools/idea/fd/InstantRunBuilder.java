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
package com.android.tools.idea.fd;

import com.android.annotations.VisibleForTesting;
import com.android.builder.model.OptionalCompilationStep;
import com.android.ddmlib.Client;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.NullOutputReceiver;
import com.android.sdklib.AndroidVersion;
import com.android.tools.fd.client.AppState;
import com.android.tools.fd.client.InstantRunBuildInfo;
import com.android.tools.fd.client.InstantRunClient;
import com.android.tools.idea.gradle.invoker.GradleInvoker;
import com.android.tools.idea.gradle.run.BeforeRunBuilder;
import com.android.tools.idea.gradle.run.GradleTaskRunner;
import com.android.tools.idea.run.AndroidRunConfigContext;
import com.android.tools.idea.run.InstalledApkCache;
import com.android.tools.idea.run.InstalledPatchCache;
import com.android.tools.idea.run.util.MultiUserUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.android.builder.model.AndroidProject.PROPERTY_OPTIONAL_COMPILATION_STEPS;

public class InstantRunBuilder implements BeforeRunBuilder {
  private static final Logger LOG = Logger.getInstance(InstantRunBuilder.class);

  @Nullable private final IDevice myDevice;
  private final InstantRunContext myInstantRunContext;
  private final AndroidRunConfigContext myRunContext;
  private final InstantRunTasksProvider myTasksProvider;
  private final RunAsValidator myRunAsValidator;
  private final InstalledApkCache myInstalledApkCache;
  private final InstantRunClientDelegate myInstantRunClientDelegate;
  private final boolean myFlightRecorderEnabled;

  public InstantRunBuilder(@Nullable IDevice device,
                           @NotNull InstantRunContext instantRunContext,
                           @NotNull AndroidRunConfigContext runConfigContext,
                           @NotNull InstantRunTasksProvider tasksProvider,
                           @NotNull RunAsValidator runAsValidator) {
    this(device,
         instantRunContext,
         runConfigContext,
         tasksProvider,
         InstantRunSettings.isRecorderEnabled(),
         runAsValidator,
         ServiceManager.getService(InstalledApkCache.class),
         new InstantRunClientDelegate() {
         });
  }

  @VisibleForTesting
  InstantRunBuilder(@Nullable IDevice device,
                    @NotNull InstantRunContext instantRunContext,
                    @NotNull AndroidRunConfigContext runConfigContext,
                    @NotNull InstantRunTasksProvider tasksProvider,
                    boolean enableFlightRecorder,
                    @NotNull RunAsValidator runAsValidator,
                    @NotNull InstalledApkCache installedApkCache,
                    @NotNull InstantRunClientDelegate delegate) {
    myDevice = device;
    myInstantRunContext = instantRunContext;
    myRunContext = runConfigContext;
    myTasksProvider = tasksProvider;
    myFlightRecorderEnabled = enableFlightRecorder;
    myRunAsValidator = runAsValidator;
    myInstalledApkCache = installedApkCache;
    myInstantRunClientDelegate = delegate;
  }

  @Override
  public boolean build(@NotNull GradleTaskRunner taskRunner, @NotNull List<String> commandLineArguments) throws InterruptedException,
                                                                                                                InvocationTargetException {
    BuildSelection buildSelection = getBuildSelection();
    myInstantRunContext.setBuildSelection(buildSelection);
    if (buildSelection.mode != BuildMode.HOT) {
      LOG.info(buildSelection.mode + ": " + buildSelection.why);
    }

    List<String> args = new ArrayList<>(commandLineArguments);
    args.addAll(myInstantRunContext.getCustomBuildArguments());

    FileChangeListener.Changes fileChanges = myInstantRunContext.getFileChangesAndReset();
    args.addAll(getInstantRunArguments(buildSelection.mode, fileChanges));
    args.addAll(getFlightRecorderArguments());

    List<String> tasks = new LinkedList<>();
    if (buildSelection.mode == BuildMode.CLEAN) {
      tasks.addAll(myTasksProvider.getCleanAndGenerateSourcesTasks());
    }
    tasks.addAll(myTasksProvider.getFullBuildTasks());
    return taskRunner.run(tasks, null, args);
  }

  @NotNull
  private BuildSelection getBuildSelection() {
    BuildCause buildCause = needsCleanBuild(myDevice);
    if (buildCause != null) {
      return new BuildSelection(BuildMode.CLEAN, buildCause, hasMultiUser(myDevice));
    }

    buildCause = needsFullBuild(myDevice);
    if (buildCause != null) {
      return new BuildSelection(BuildMode.FULL, buildCause, hasMultiUser(myDevice));
    }

    buildCause = needsColdswapPatches(myDevice);
    if (buildCause != null) {
      return new BuildSelection(BuildMode.COLD, buildCause, hasMultiUser(myDevice));
    }

    return new BuildSelection(BuildMode.HOT, BuildCause.INCREMENTAL_BUILD);
  }

  private static boolean hasMultiUser(@Nullable IDevice device) {
    return MultiUserUtils.hasMultipleUsers(device, 200, TimeUnit.MILLISECONDS, false);
  }

  @Nullable
  @Contract("null -> !null")
  private BuildCause needsCleanBuild(@Nullable IDevice device) {
    if (device == null) { // i.e. emulator is still launching..
      return BuildCause.NO_DEVICE;
    }

    if (myRunContext.isCleanRerun()) {
      return BuildCause.USER_REQUESTED_CLEAN_BUILD;
    }

    // We assume that the deployment happens to the default user, and in here, we check whether it is still installed for the default user
    // (Note: this could be done in a better way if we knew the user for whom the installation actually took place).
    int defaultUserId = 0;
    if (!isAppInstalledForUser(device, myInstantRunContext.getApplicationId(), defaultUserId)) {
      return BuildCause.APP_NOT_INSTALLED;
    }

    if (!buildTimestampsMatch(device, defaultUserId)) {
      if (myInstalledApkCache.getInstallState(device, myInstantRunContext.getApplicationId()) == null) {
        return BuildCause.FIRST_INSTALLATION_TO_DEVICE;
      }
      else {
        return BuildCause.MISMATCHING_TIMESTAMPS;
      }
    }

    return null;
  }

  @Nullable
  private BuildCause needsFullBuild(@NotNull IDevice device) {
    AndroidVersion deviceVersion = device.getVersion();
    if (!InstantRunManager.isInstantRunCapableDeviceVersion(deviceVersion)) {
      return BuildCause.API_TOO_LOW_FOR_INSTANT_RUN;
    }

    if (!InstantRunManager.hasLocalCacheOfDeviceData(device, myInstantRunContext)) {
      return BuildCause.FIRST_INSTALLATION_TO_DEVICE;
    }

    // Normally, all files are saved when Gradle runs (in GradleInvoker#executeTasks). However, we need to save the files
    // a bit earlier than that here (turning the Gradle file save into a no-op) because the we need to check whether the
    // manifest file or a resource referenced from the manifest has changed since the last build.
    if (ApplicationManager.getApplication() != null) { // guard against invoking this in unit tests
      GradleInvoker.saveAllFilesSafely();
    }

    if (manifestResourceChanged(device)) {
      return BuildCause.MANIFEST_RESOURCE_CHANGED;
    }

    if (!isAppRunning(device)) { // freeze-swap scenario
      if (!deviceVersion.isGreaterOrEqualThan(21)) { // don't support cold swap on API < 21
        return BuildCause.FREEZE_SWAP_REQUIRES_API21;
      }

      if (!myRunAsValidator.hasWorkingRunAs(device, myInstantRunContext.getApplicationId())) {
        return BuildCause.FREEZE_SWAP_REQUIRES_WORKING_RUN_AS;
      }
    }

    return null;
  }

  @Nullable
  private BuildCause needsColdswapPatches(@NotNull IDevice device) {
    if (!isAppRunning(device)) {
      return BuildCause.APP_NOT_RUNNING;
    }

    if (device.supportsFeature(IDevice.HardwareFeature.TV) ||
        // Sadly the current emulator image for Android TV doesn't define the above hardware feature,
        // so look for the Android TV SDK model name specifically until these images are obsolete
        StringUtil.notNullize(device.getProperty(IDevice.PROP_DEVICE_MODEL)).contains("sdk_google_atv_")) {
      // Android TV currently only supports coldswap. Many Android TV apps,
      // such as the sample app bundled with Android Studio (and various others)
      // crash if you attempt to invoke Activity#restartActivity. It looks like
      // the Leanback library (and possibly others) make the assumption that
      // activities will never be restarted, perhaps because configuration changes
      // such as an orientation change never happen on Android TV.
      return BuildCause.ANDROID_TV_UNSUPPORTED;
    }

    if (myInstantRunContext.usesMultipleProcesses()) {
      return BuildCause.APP_USES_MULTIPLE_PROCESSES;
    }

    return null;
  }

  private boolean isAppRunning(@NotNull IDevice device) {
    // for an app to considered running, in addition to it actually running, it also must be launched by the same executor
    // (i.e. run followed by run, or debug followed by debug). If the last session was for running, and this session is for debugging,
    // then very likely the process is in the middle of being terminated since we don't reuse the same run session.
    boolean isAppRunning;
    try {
      isAppRunning = myInstantRunClientDelegate.isAppInForeground(device, myInstantRunContext);
    }
    catch (IOException e) {
      InstantRunManager.LOG.warn("IOException while attempting to determine if app is in foreground, assuming app not alive");
      isAppRunning = false;

      // Such an assumption could be fatal if the app is indeed running, so we force kill the app in the off chance that it was running.
      // See https://code.google.com/p/android/issues/detail?id=218593
      InstantRunManager.LOG.warn("Force killing app");
      try {
        device.executeShellCommand("am force-stop " + myInstantRunContext.getApplicationId(), new NullOutputReceiver());
      }
      catch (Exception ignore) {
      }
    }

    return isAppRunning && myRunContext.isSameExecutorAsPreviousSession();
  }

  private static List<String> getInstantRunArguments(@NotNull BuildMode buildMode, @Nullable FileChangeListener.Changes changes) {
    // TODO: Add a user-level setting to disable this?
    // TODO: Use constants from AndroidProject once we import the new model jar.

    StringBuilder sb = new StringBuilder(50);
    sb.append("-P");
    sb.append(PROPERTY_OPTIONAL_COMPILATION_STEPS);
    sb.append("=");
    sb.append(OptionalCompilationStep.INSTANT_DEV.name());

    if (buildMode == BuildMode.HOT) {
      appendChangeInfo(sb, changes);
    }
    else if (buildMode == BuildMode.COLD) {
      sb.append(",").append(OptionalCompilationStep.RESTART_ONLY.name());
    }
    else {
      sb.append(",").append("FULL_APK"); //TODO: Replace with enum reference after next model drop.
    }

    return Collections.singletonList(sb.toString());
  }

  @NotNull
  private List<String> getFlightRecorderArguments() {
    return myFlightRecorderEnabled ? ImmutableList.of("--info") : ImmutableList.of();
  }

  private static void appendChangeInfo(@NotNull StringBuilder sb, @Nullable FileChangeListener.Changes changes) {
    if (changes == null) {
      return;
    }

    //https://code.google.com/p/android/issues/detail?id=213205
    // If users change the manifest inside Gradle, then Gradle doesn't handle this situation very well
    // (i.e. if we say only Java changed, but Gradle finds that the merged manifest has also changed, then it gets confused)
    // So for now, we just remove this.
    //if (!changes.nonSourceChanges) {
    //  if (changes.localResourceChanges) {
    //    sb.append(",LOCAL_RES_ONLY");
    //  }
    //  if (changes.localJavaChanges) {
    //    sb.append(",LOCAL_JAVA_ONLY");
    //  }
    //}
  }

  /**
   * Returns whether the device has the same timestamp as the existing build on disk.
   */
  private boolean buildTimestampsMatch(@NotNull IDevice device, @Nullable Integer userId) {
    InstantRunBuildInfo instantRunBuildInfo = myInstantRunContext.getInstantRunBuildInfo();
    String localTimestamp = instantRunBuildInfo == null ? null : instantRunBuildInfo.getTimeStamp();
    if (StringUtil.isEmpty(localTimestamp)) {
      InstantRunManager.LOG.info("Local build timestamp is empty!");
      return false;
    }

    if (InstantRunClient.USE_BUILD_ID_TEMP_FILE) {
      // If the build id is saved in /data/local/tmp, then the build id isn't cleaned when the package is uninstalled.
      // So we first check that the app is still installed. Note: this doesn't yet guarantee that you have uninstalled and then
      // re-installed a different apk with the same package name..
      // https://code.google.com/p/android/issues/detail?id=198715
      if (!isAppInstalledForUser(device, myInstantRunContext.getApplicationId(), userId)) {
        return false;
      }
    }

    String deviceBuildTimestamp = myInstantRunClientDelegate.getDeviceBuildTimestamp(device, myInstantRunContext);

    InstantRunManager.LOG.info(String.format("Build timestamps: Local: %1$s, Device: %2$s", localTimestamp, deviceBuildTimestamp));
    return localTimestamp.equals(deviceBuildTimestamp);
  }

  private boolean isAppInstalledForUser(@NotNull IDevice device, @NotNull String pkgName, @Nullable Integer userId) {
    // check whether the package is installed on the device: we do this by checking the package manager for whether the app exists,
    // but we could potentially simplify this to just checking whether the package folder exists
    InstalledApkCache.InstallState installState = myInstalledApkCache.getInstallState(device, pkgName);
    if (installState == null) {
      InstantRunManager.LOG.info("Package " + pkgName + " was not detected on the device.");
      return false;
    }

    // Note: the installState.users is not always available, so the check below computes whether it is installed for some users, but
    // not the default user.
    if (userId != null && !installState.users.isEmpty() && !installState.users.contains(userId)) {
      LOG.info("Package " + pkgName + " was not installed for default user.");
      return false;
    }

    return true;
  }

  /**
   * Returns true if a resource referenced from the manifest has changed since the last manifest push to the device.
   */
  public boolean manifestResourceChanged(@NotNull IDevice device) {
    InstalledPatchCache cache = myInstantRunContext.getInstalledPatchCache();

    // See if the resources have changed.
    // Since this method can be called before we've built, we're looking at the previous
    // manifest now. However, manifest edits are treated separately (see manifestChanged()),
    // so the goal here is to look for the referenced resources from the manifest
    // (when the manifest itself hasn't been edited) and see if any of *them* have changed.
    HashCode currentHash = myInstantRunContext.getManifestResourcesHash();
    HashCode installedHash = cache.getInstalledManifestResourcesHash(device, myInstantRunContext.getApplicationId());
    if (installedHash != null && !installedHash.equals(currentHash)) {
      return true;
    }

    return false;
  }

  /**
   * {@link InstantRunClientDelegate} adds a level of indirection to accessing instant run specific data from the device,
   * in order to facilitate mocking it out in tests.
   */
  interface InstantRunClientDelegate {
    default String getDeviceBuildTimestamp(@NotNull IDevice device, @NotNull InstantRunContext instantRunContext) {
      return InstantRunClient.getDeviceBuildTimestamp(device, instantRunContext.getApplicationId(), InstantRunManager.ILOGGER);
    }

    /**
     * @return whether the app associated with the given module is already running on the given device and listening for IR updates
     */
    default boolean isAppInForeground(@NotNull IDevice device, @NotNull InstantRunContext context) throws IOException {
      InstantRunClient instantRunClient = InstantRunManager.getInstantRunClient(context);
      try {
        return instantRunClient != null && instantRunClient.getAppState(device) == AppState.FOREGROUND;
      }
      catch (IOException e) {
        Client client = device.getClient(context.getApplicationId());
        if (client == null) {
          InstantRunManager.LOG.info("Application not running");
          return false;
        }

        throw e;
      }
    }
  }
}
