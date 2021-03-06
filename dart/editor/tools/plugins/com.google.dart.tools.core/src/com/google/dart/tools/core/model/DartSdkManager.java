/*
 * Copyright 2012 Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.dart.tools.core.model;

import com.google.common.collect.ImmutableMap;
import com.google.dart.engine.internal.sdk.LibraryMap;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// http://commondatastorage.googleapis.com/dart-editor-archive-integration/latest/dartsdk-macos-32.zip

// TOOD(devoncarew): refactor the download/unzip/copy code into utility methods (to be
// shared with the update code).

/**
 * The clearing house for getting the current SDK and listening for SDK changes.
 * 
 * @coverage dart.tools.core.model
 */
public class DartSdkManager {

  /**
   * Environment variable key for user-specified update URLs.
   */
  public static final String UPDATE_URL_ENV_VAR = "com.dart.tools.update.core.url";

  private static final String DEFAULT_UPDATE_URL = "http://dartlang.org/editor/update/channels/dev/";

  private static final String USER_DEFINED_SDK_KEY = "dart.sdk";

  private static final String SDK_ZIP = "latest/sdk/dartsdk-{0}-{1}-release.zip";

  /**
   * A special instance of {@link com.google.dart.engine.sdk.DartSdk} representing missing SDK.
   */
  public static final DirectoryBasedDartSdk NONE = new DirectoryBasedDartSdk(new File(
      getEclipseInstallationDirectory(),
      "no-dart-sdk")) {
    @Override
    protected LibraryMap initialLibraryMap(boolean useDart2jsPaths) {
      return new LibraryMap();
    }
  };

  private static final String SDK_DIR_NAME = "dart-sdk";

  private static DartSdkManager manager = new DartSdkManager();

  public static DartSdkManager getManager() {
    return manager;
  }

  static File getEclipseInstallationDirectory() {
    return new File(Platform.getInstallLocation().getURL().getFile());
  }

  /**
   * The Editor looks for "dart-sdk" as a sibling to the installation directory.
   * 
   * @return
   */
  private static File getDefaultEditorSdkDirectory() {
    File parent = getEclipseInstallationDirectory().getParentFile();

    return new File(parent, SDK_DIR_NAME);
  }

  /**
   * The plugins build looks in the installation directory for "dart-sdk".
   * 
   * @return
   */
  private static File getDefaultPluginsSdkDirectory() {
    return new File(getEclipseInstallationDirectory(), SDK_DIR_NAME);
  }

  /**
   * @return one of ia32 or x64
   */
  private static String getPlatformBititude() {
    if (DartCore.is32Bit()) {
      return "ia32";
    } else {
      return "x64";
    }
  }

  /**
   * @return one of macos, windows, or linux
   */
  private static String getPlatformCode() {
    if (DartCore.isWindows()) {
      return "windows";
    } else if (DartCore.isMac()) {
      return "macos";
    } else if (DartCore.isLinux()) {
      return "linux";
    }

    return null;
  }

  private DirectoryBasedDartSdk sdk;
  private String sdkContextId;

  private List<DartSdkListener> listeners = new ArrayList<DartSdkListener>();

  private DartSdkManager() {
    initSdk();
  }

  public void addSdkListener(DartSdkListener lisener) {
    listeners.add(lisener);
  }

  public DirectoryBasedDartSdk getSdk() {
    return sdk;
  }

  public String getSdkContextId() {
    return sdkContextId;
  }

  public boolean hasSdk() {

    //TODO (pquitslund): add a switch to check for analysis engine enablement

    return getSdk() != null && getSdk() != NONE;
  }

  public void removeSdkListener(DartSdkListener listener) {
    listeners.remove(listener);
  }

  /**
   * @param monitor
   */
  public IStatus upgrade(String channel, IProgressMonitor monitor) {
    try {
      upgradeImpl(channel, monitor);

      return Status.OK_STATUS;
    } catch (IOException ioe) {
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, ioe.getMessage(), ioe);
    }
  }

  protected void notifyListeners() {
    for (DartSdkListener listener : listeners) {
      listener.sdkUpdated(getSdk());
    }
  }

  private void copyFile(File fromFile, File toFile, IProgressMonitor monitor) throws IOException {
    byte[] data = new byte[4096];

    InputStream in = new FileInputStream(fromFile);

    toFile.delete();

    OutputStream out = new FileOutputStream(toFile);

    monitor.beginTask("Copy " + fromFile.toString(), (int) fromFile.length());

    int count = in.read(data);

    while (count != -1) {
      out.write(data, 0, count);

      monitor.worked(count);

      count = in.read(data);
    }

    in.close();
    out.close();

    toFile.setLastModified(fromFile.lastModified());

    monitor.done();
  }

  private File copyNewSdk(IProgressMonitor monitor, File newSDK) throws IOException {
    File currentSDK = new File(getEclipseInstallationDirectory(), "dart-sdk.zip");

    copyFile(newSDK, currentSDK, monitor);

    return currentSDK;
  }

  private void copyStream(InputStream in, FileOutputStream out, IProgressMonitor monitor, int length)
      throws IOException {
    byte[] data = new byte[4096];

    int count = in.read(data);

    while (count != -1) {
      out.write(data, 0, count);

      if (length != -1) {
        monitor.worked(count);
      }

      count = in.read(data);
    }

    in.close();
    out.close();
  }

  private void deleteDirectory(File dir) {
    for (File file : dir.listFiles()) {
      if (file.isDirectory()) {
        deleteDirectory(file);
      } else {
        file.delete();
      }
    }

    dir.delete();
  }

  private File downloadFile(URI downloadURI, IProgressMonitor monitor) throws IOException {
    File tempFile = File.createTempFile(SDK_DIR_NAME, ".zip");
    tempFile.deleteOnExit();

    URLConnection connection = downloadURI.toURL().openConnection();

    int length = connection.getContentLength();

    FileOutputStream out = new FileOutputStream(tempFile);

    monitor.beginTask("Download SDK", length);

    copyStream(connection.getInputStream(), out, monitor, length);

    monitor.done();

    if (connection.getLastModified() != 0) {
      tempFile.setLastModified(connection.getLastModified());
    }

    return tempFile;
  }

  private String getSdkUrl(String channel) {

    String sdkZip = NLS.bind(SDK_ZIP, getPlatformCode(), getPlatformBititude());

    if (channel != null) {
      return channel + sdkZip;
    } else {
      String url = getUpdateChannelUrl();
      if (url != null) {
        return url + sdkZip;
      }
    }
    return DEFAULT_UPDATE_URL + sdkZip;
  }

  private String getUpdateChannelUrl() {
    try {
      File file = getUpdatePropertiesFile();
      if (file.exists()) {
        Properties properties = new Properties();
        properties.load(new FileReader(file));
        return properties.getProperty(UPDATE_URL_ENV_VAR);
      }
    } catch (FileNotFoundException e) {
      DartCore.logError(e);
    } catch (IOException e) {
      DartCore.logError(e);
    } catch (URISyntaxException e) {
      DartCore.logError(e);
    }
    return null;
  }

  private File getUpdatePropertiesFile() throws IOException, URISyntaxException {
    Bundle bundle = Platform.getBundle(DartCore.PLUGIN_ID);
    URL url = bundle.getEntry("update.properties");
    URL resolvedUrl = FileLocator.resolve(url);
    // Ensure file system chars are properly escaped
    // (https://bugs.eclipse.org/bugs/show_bug.cgi?id=145096)
    URI fileUri = new URI(resolvedUrl.getProtocol(), resolvedUrl.getPath(), null);
    return new File(fileUri);
  }

  /**
   * Return the user-defined SDK directory.
   * 
   * @return the directory or {@code null} if it is not defined
   */
  private File getUserDefinedSdkDirectory() {
    String sdkPath = DartCore.getUserDefinedProperty(USER_DEFINED_SDK_KEY);
    if (sdkPath != null) {
      sdkPath = sdkPath.trim();
      if (sdkPath.length() > 0) {
        return new File(sdkPath);
      }
    }
    return null;
  }

  private void initSdk() {
    File sdkDir = getUserDefinedSdkDirectory();
    if (sdkDir != null && !sdkDir.exists()) {
      DartCore.logError(USER_DEFINED_SDK_KEY + " defined in " + DartCore.EDITOR_PROPERTIES
          + " but does not exist: " + sdkDir);
      sdkDir = null;
    }
    if (sdkDir == null) {
      sdkDir = getDefaultPluginsSdkDirectory();
      if (!sdkDir.exists()) {
        sdkDir = getDefaultEditorSdkDirectory();
        if (!sdkDir.exists()) {
          sdkDir = null;
        }
      }
    }
    if (sdkDir != null) {
      sdk = new DirectoryBasedDartSdk(sdkDir);
      // create an artificial context for SDK
      if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
        sdkContextId = DartCore.getAnalysisServer().createContext(
            sdkDir.getAbsolutePath(),
            sdkDir.getAbsolutePath(),
            ImmutableMap.<String, String> of());
      }
    } else {
      sdk = NONE;
    }
  }

  private void unzip(File zipFile, File destination, IProgressMonitor monitor) throws IOException {
    monitor.beginTask("Unzip " + zipFile.getName(), (int) zipFile.length());

    final int BUFFER_SIZE = 4096;

    ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
    ZipEntry entry;

    while ((entry = zis.getNextEntry()) != null) {
      int count;
      byte data[] = new byte[BUFFER_SIZE];

      File outFile = new File(destination, entry.getName());

      if (entry.isDirectory()) {
        if (!outFile.exists()) {
          outFile.mkdirs();
        }
      } else {
        if (!outFile.getParentFile().exists()) {
          outFile.getParentFile().mkdirs();
        }

        OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));

        while ((count = zis.read(data, 0, BUFFER_SIZE)) != -1) {
          out.write(data, 0, count);

          monitor.worked(count);
        }

        out.flush();
        out.close();
      }
    }

    zis.close();

    monitor.done();
  }

  private void unzipNewSDK(File newSDK, IProgressMonitor monitor) throws IOException {
    File sdkDirectory = getDefaultPluginsSdkDirectory();

    if (sdkDirectory.exists()) {
      deleteDirectory(sdkDirectory);
    }

    unzip(newSDK, getDefaultPluginsSdkDirectory().getParentFile(), monitor);
  }

  private void upgradeImpl(String channel, IProgressMonitor monitor) throws IOException {
    try {
      // init progress
      SubMonitor mon = SubMonitor.convert(monitor, "Downloading Dart SDK", 100);

      URI downloadURI = URI.create(getSdkUrl(channel));

      // download to a temp file
      File tempFile = downloadFile(downloadURI, mon.newChild(80));

      // copy the new sdk
      File newSdk = copyNewSdk(mon.newChild(3), tempFile);

      tempFile.delete();

      // unzip
      unzipNewSDK(newSdk, mon.newChild(10));

      // swap out the new sdk for the old
      this.sdk = null;
      initSdk();

      // send upgrade notifications
      notifyListeners();

      DartCore.getConsole().printSeparator("Dart SDK update");
      DartCore.getConsole().println(
          "Dart SDK updated to version " + getManager().getSdk().getSdkVersion());

    } finally {
      monitor.done();
    }
  }

}
