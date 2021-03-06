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
package com.android.tools.idea.welcome.install;

import com.android.repository.api.*;
import com.android.repository.impl.installer.BasicInstallerFactory;
import com.android.sdklib.repository.AndroidSdkHandler;
import com.android.tools.idea.sdk.StudioDownloader;
import com.android.tools.idea.sdk.StudioSettingsController;
import com.android.tools.idea.sdk.progress.StudioLoggerProgressIndicator;
import com.android.tools.idea.sdk.wizard.SdkQuickfixUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * Installs SDK components.
 */
public final class ComponentInstaller {
  private final AndroidSdkHandler mySdkHandler;

  public ComponentInstaller(@NotNull AndroidSdkHandler sdkHandler) {
    mySdkHandler = sdkHandler;
  }

  public List<RemotePackage> getPackagesToInstall(@NotNull Iterable<? extends InstallableComponent> components)
    throws SdkQuickfixUtils.PackageResolutionException {
    // TODO: Prompt about connection in handoff case?
    Set<UpdatablePackage> requests = Sets.newHashSet();
    StudioLoggerProgressIndicator progress = new StudioLoggerProgressIndicator(getClass());
    RepoManager sdkManager = mySdkHandler.getSdkManager(progress);
    for (InstallableComponent component : components) {
      requests.addAll(component.getPackagesToInstall());
    }
    List<UpdatablePackage> resolved = Lists.newArrayList();
    resolved.addAll(SdkQuickfixUtils.resolve(requests, sdkManager.getPackages()));

    List<RemotePackage> result = Lists.newArrayList();
    for (UpdatablePackage p : resolved) {
      result.add(p.getRemote());
    }
    return result;
  }

  public void installPackages(@NotNull List<RemotePackage> packages, ProgressIndicator progress) throws WizardException {
    RepoManager sdkManager = mySdkHandler.getSdkManager(progress);
    for (RemotePackage request : packages) {
      // Intentionally don't register any listeners on the installer, so we don't recurse on haxm
      // TODO: This is a hack. Any future rewrite of this shouldn't require this behavior.
      InstallerFactory factory = new BasicInstallerFactory();
      Installer installer = factory.createInstaller(request, sdkManager, new StudioDownloader(), mySdkHandler.getFileOp());
      if (installer.prepare(progress)) {
        installer.complete(progress);
      }
    }
    sdkManager.loadSynchronously(RepoManager.DEFAULT_EXPIRATION_PERIOD_MS, progress, null, null);
  }
}
