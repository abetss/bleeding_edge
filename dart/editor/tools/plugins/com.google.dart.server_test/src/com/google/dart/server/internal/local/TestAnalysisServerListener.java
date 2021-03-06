/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.server.internal.local;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.source.Source;
import com.google.dart.server.AnalysisServerError;
import com.google.dart.server.AnalysisServerErrorCode;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.HighlightRegion;
import com.google.dart.server.NavigationRegion;
import com.google.dart.server.Outline;
import com.google.dart.server.internal.local.asserts.NavigationRegionsAssert;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;
import java.util.Map;

/**
 * Mock implementation of {@link AnalysisServerListener}.
 */
public class TestAnalysisServerListener implements AnalysisServerListener {
  private final Map<Source, AnalysisError[]> sourcesErrors = Maps.newHashMap();
  private final List<AnalysisServerError> serverErrors = Lists.newArrayList();
  private final Map<String, Map<Source, NavigationRegion[]>> navigationMap = Maps.newHashMap();
  private final Map<String, Map<Source, Outline>> outlineMap = Maps.newHashMap();
  private final Map<String, Map<Source, HighlightRegion[]>> highlightsMap = Maps.newHashMap();

  /**
   * Assert that the number of errors that have been gathered matches the number of errors that are
   * given and that they have the expected error codes. The order in which the errors were gathered
   * is ignored.
   * 
   * @param source the source to check errors for
   * @param expectedErrorCodes the error codes of the errors that should have been gathered
   * @throws AssertionFailedError if a different number of errors have been gathered than were
   *           expected
   */
  public synchronized void assertErrorsWithCodes(Source source, ErrorCode... expectedErrorCodes) {
    GatheringErrorListener listener = new GatheringErrorListener();
    AnalysisError[] errors = sourcesErrors.get(source);
    if (errors != null) {
      for (AnalysisError error : errors) {
        listener.onError(error);
      }
    }
    listener.assertErrorsWithCodes(expectedErrorCodes);
  }

  /**
   * Returns {@link NavigationRegionsAssert} for the given context and {@link Source}.
   */
  public NavigationRegionsAssert assertNavigationRegions(String contextId, Source source) {
    return new NavigationRegionsAssert(getNavigationRegions(contextId, source));
  }

  /**
   * Asserts that there was no {@link AnalysisServerError} reported.
   */
  public void assertNoServerErrors() {
    assertThat(serverErrors).isEmpty();
  }

  /**
   * Assert that the number of {@link #serverErrors} that have been gathered matches the number of
   * errors that are given and that they have the expected error codes. The order in which the
   * errors were gathered is ignored.
   * 
   * @param expectedErrorCodes the error codes of the errors that should have been gathered
   * @throws AssertionFailedError if a different number of errors have been gathered than were
   *           expected
   */
  public synchronized void assertServerErrorsWithCodes(
      AnalysisServerErrorCode... expectedErrorCodes) {
    StringBuilder builder = new StringBuilder();
    //
    // Verify that the expected error codes have a non-empty message.
    //
    for (AnalysisServerErrorCode errorCode : expectedErrorCodes) {
      Assert.assertFalse("Empty error code message", errorCode.getMessage().isEmpty());
    }
    //
    // Compute the expected number of each type of error.
    //
    Map<AnalysisServerErrorCode, Integer> expectedCounts = Maps.newHashMap();
    for (AnalysisServerErrorCode code : expectedErrorCodes) {
      Integer count = expectedCounts.get(code);
      if (count == null) {
        count = Integer.valueOf(1);
      } else {
        count = Integer.valueOf(count.intValue() + 1);
      }
      expectedCounts.put(code, count);
    }
    //
    // Compute the actual number of each type of error.
    //
    Map<AnalysisServerErrorCode, List<AnalysisServerError>> errorsByCode = Maps.newHashMap();
    for (AnalysisServerError error : serverErrors) {
      AnalysisServerErrorCode code = error.getErrorCode();
      List<AnalysisServerError> list = errorsByCode.get(code);
      if (list == null) {
        list = Lists.newArrayList();
        errorsByCode.put(code, list);
      }
      list.add(error);
    }
    //
    // Compare the expected and actual number of each type of error.
    //
    for (Map.Entry<AnalysisServerErrorCode, Integer> entry : expectedCounts.entrySet()) {
      AnalysisServerErrorCode code = entry.getKey();
      int expectedCount = entry.getValue().intValue();
      int actualCount;
      List<AnalysisServerError> list = errorsByCode.remove(code);
      if (list == null) {
        actualCount = 0;
      } else {
        actualCount = list.size();
      }
      if (actualCount != expectedCount) {
        if (builder.length() == 0) {
          builder.append("Expected ");
        } else {
          builder.append("; ");
        }
        builder.append(expectedCount);
        builder.append(" errors of type ");
        builder.append(code.getClass().getSimpleName() + "." + code);
        builder.append(", found ");
        builder.append(actualCount);
      }
    }
    //
    // Check that there are no more errors in the actual-errors map, otherwise, record message.
    //
    for (Map.Entry<AnalysisServerErrorCode, List<AnalysisServerError>> entry : errorsByCode.entrySet()) {
      AnalysisServerErrorCode code = entry.getKey();
      List<AnalysisServerError> actualErrors = entry.getValue();
      int actualCount = actualErrors.size();
      if (builder.length() == 0) {
        builder.append("Expected ");
      } else {
        builder.append("; ");
      }
      builder.append("0 errors of type ");
      builder.append(code.getClass().getSimpleName() + "." + code);
      builder.append(", found ");
      builder.append(actualCount);
    }
    if (builder.length() > 0) {
      Assert.fail(builder.toString());
    }
  }

  /**
   * Removes all of reported {@link NavigationRegion}s.
   */
  public void clearNavigationRegions() {
    navigationMap.clear();
  }

  @Override
  public synchronized void computedErrors(String contextId, Source source, AnalysisError[] errors) {
    sourcesErrors.put(source, errors);
  }

  @Override
  public synchronized void computedHighlights(String contextId, Source source,
      HighlightRegion[] highlights) {
    Map<Source, HighlightRegion[]> sourceHighlights = highlightsMap.get(contextId);
    if (sourceHighlights == null) {
      sourceHighlights = Maps.newHashMap();
      highlightsMap.put(contextId, sourceHighlights);
    }
    sourceHighlights.put(source, highlights);
  }

  @Override
  public synchronized void computedNavigation(String contextId, Source source,
      NavigationRegion[] targets) {
    Map<Source, NavigationRegion[]> navigations = navigationMap.get(contextId);
    if (navigations == null) {
      navigations = Maps.newHashMap();
      navigationMap.put(contextId, navigations);
    }
    navigations.put(source, targets);
  }

  @Override
  public synchronized void computedOutline(String contextId, Source source, Outline outline) {
    Map<Source, Outline> outlines = outlineMap.get(contextId);
    if (outlines == null) {
      outlines = Maps.newHashMap();
      outlineMap.put(contextId, outlines);
    }
    outlines.put(source, outline);
  }

  /**
   * Returns {@link HighlightRegion}s for the given context and {@link Source}, maybe {@code null}
   * if have not been ever notified.
   */
  public synchronized HighlightRegion[] getHighlightRegions(String contextId, Source source) {
    Map<Source, HighlightRegion[]> sourceHighlights = highlightsMap.get(contextId);
    if (sourceHighlights == null) {
      return null;
    }
    return sourceHighlights.get(source);
  }

  /**
   * Returns {@link NavigationRegion}s for the given context and {@link Source}, maybe {@code null}
   * if have not been ever notified.
   */
  public synchronized NavigationRegion[] getNavigationRegions(String contextId, Source source) {
    Map<Source, NavigationRegion[]> navigations = navigationMap.get(contextId);
    if (navigations == null) {
      return null;
    }
    return navigations.get(source);
  }

  /**
   * Returns {@link Outline} for the given context and {@link Source}, maybe {@code null} if have
   * not been ever notified.
   */
  public synchronized Outline getOutline(String contextId, Source source) {
    Map<Source, Outline> outlines = outlineMap.get(contextId);
    if (outlines == null) {
      return null;
    }
    return outlines.get(source);
  }

  @Override
  public synchronized void onServerError(AnalysisServerError error) {
    serverErrors.add(error);
  }
}
