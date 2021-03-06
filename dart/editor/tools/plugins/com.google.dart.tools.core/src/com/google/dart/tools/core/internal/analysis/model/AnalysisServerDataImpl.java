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

package com.google.dart.tools.core.internal.analysis.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.engine.source.Source;
import com.google.dart.server.AnalysisServer;
import com.google.dart.server.HighlightRegion;
import com.google.dart.server.ListSourceSet;
import com.google.dart.server.NavigationRegion;
import com.google.dart.server.NotificationKind;
import com.google.dart.server.Outline;
import com.google.dart.tools.core.analysis.model.AnalysisServerData;
import com.google.dart.tools.core.analysis.model.AnalysisServerHighlightsListener;
import com.google.dart.tools.core.analysis.model.AnalysisServerOutlineListener;

import java.util.Map;
import java.util.Set;

/**
 * Instances of {@code AnalysisServerData} manage and provide access to analysis results reported by
 * {@link AnalysisServer}.
 * 
 * @coverage dart.tools.core.model
 */
public class AnalysisServerDataImpl implements AnalysisServerData {
  private final Map<String, Map<Source, NavigationRegion[]>> navigationData = Maps.newHashMap();
  private final Map<String, Set<Source>> navigationSubscriptions = Maps.newHashMap();
  private final Map<String, Map<Source, Set<AnalysisServerOutlineListener>>> outlineSubscriptions = Maps.newHashMap();
  private final Map<String, Map<Source, Set<AnalysisServerHighlightsListener>>> highlightsSubscriptions = Maps.newHashMap();

  private AnalysisServer server;

  @Override
  public NavigationRegion[] getNavigation(String contextId, Source source) {
    Map<Source, NavigationRegion[]> contextRegions = navigationData.get(contextId);
    if (contextRegions == null) {
      return NavigationRegion.EMPTY_ARRAY;
    }
    NavigationRegion[] sourceRegions = contextRegions.get(source);
    if (sourceRegions == null) {
      return NavigationRegion.EMPTY_ARRAY;
    }
    return sourceRegions;
  }

  /**
   * Deletes all the data associated with the given context.
   */
  public void internalDeleteContext(String contextId) {
    navigationData.remove(contextId);
    navigationSubscriptions.remove(contextId);
    outlineSubscriptions.remove(contextId);
    highlightsSubscriptions.remove(contextId);
  }

  /**
   * Sets the {@link AnalysisServer} to talk to.
   */
  public void setServer(AnalysisServer server) {
    this.server = server;
  }

  @Override
  public void subscribeHighlights(String contextId, Source source,
      AnalysisServerHighlightsListener listener) {
    Map<Source, Set<AnalysisServerHighlightsListener>> sourceSubscriptions = highlightsSubscriptions.get(contextId);
    if (sourceSubscriptions == null) {
      sourceSubscriptions = Maps.newHashMap();
      highlightsSubscriptions.put(contextId, sourceSubscriptions);
    }
    Set<AnalysisServerHighlightsListener> subscriptions = sourceSubscriptions.get(source);
    if (subscriptions == null) {
      subscriptions = Sets.newHashSet();
      sourceSubscriptions.put(source, subscriptions);
    }
    if (subscriptions.add(listener)) {
      Set<Source> sourceSet = sourceSubscriptions.keySet();
      server.subscribe(
          contextId,
          ImmutableMap.of(NotificationKind.HIGHLIGHTS, ListSourceSet.create(sourceSet)));
    }
  }

  @Override
  public void subscribeNavigation(String contextId, Source source) {
    Set<Source> sources = navigationSubscriptions.get(contextId);
    if (sources == null) {
      sources = Sets.newHashSet();
      navigationSubscriptions.put(contextId, sources);
    }
    if (sources.add(source)) {
      server.subscribe(
          contextId,
          ImmutableMap.of(NotificationKind.NAVIGATION, ListSourceSet.create(sources)));
    }
  }

  @Override
  public void subscribeOutline(String contextId, Source source,
      AnalysisServerOutlineListener listener) {
    Map<Source, Set<AnalysisServerOutlineListener>> sourceSubscriptions = outlineSubscriptions.get(contextId);
    if (sourceSubscriptions == null) {
      sourceSubscriptions = Maps.newHashMap();
      outlineSubscriptions.put(contextId, sourceSubscriptions);
    }
    Set<AnalysisServerOutlineListener> subscriptions = sourceSubscriptions.get(source);
    if (subscriptions == null) {
      subscriptions = Sets.newHashSet();
      sourceSubscriptions.put(source, subscriptions);
    }
    if (subscriptions.add(listener)) {
      Set<Source> sourceSet = sourceSubscriptions.keySet();
      server.subscribe(
          contextId,
          ImmutableMap.of(NotificationKind.OUTLINE, ListSourceSet.create(sourceSet)));
    }
  }

  @Override
  public void unsubscribeHighlights(String contextId, Source source,
      AnalysisServerHighlightsListener listener) {
    Map<Source, Set<AnalysisServerHighlightsListener>> sourceSubscriptions = highlightsSubscriptions.get(contextId);
    if (sourceSubscriptions == null) {
      return;
    }
    Set<AnalysisServerHighlightsListener> subscriptions = sourceSubscriptions.get(source);
    if (subscriptions == null) {
      return;
    }
    if (subscriptions.remove(listener)) {
      if (subscriptions.isEmpty()) {
        sourceSubscriptions.remove(source);
        Set<Source> sourceSet = sourceSubscriptions.keySet();
        server.subscribe(
            contextId,
            ImmutableMap.of(NotificationKind.HIGHLIGHTS, ListSourceSet.create(sourceSet)));
      }
    }
  }

  @Override
  public void unsubscribeNavigation(String contextId, Source source) {
    Set<Source> sources = navigationSubscriptions.get(contextId);
    if (sources == null) {
      return;
    }
    if (sources.remove(source)) {
      server.subscribe(
          contextId,
          ImmutableMap.of(NotificationKind.NAVIGATION, ListSourceSet.create(sources)));
    }
  }

  @Override
  public void unsubscribeOutline(String contextId, Source source,
      AnalysisServerOutlineListener listener) {
    Map<Source, Set<AnalysisServerOutlineListener>> sourceSubscriptions = outlineSubscriptions.get(contextId);
    if (sourceSubscriptions == null) {
      return;
    }
    Set<AnalysisServerOutlineListener> subscriptions = sourceSubscriptions.get(source);
    if (subscriptions == null) {
      return;
    }
    if (subscriptions.remove(listener)) {
      if (subscriptions.isEmpty()) {
        sourceSubscriptions.remove(source);
        Set<Source> sourceSet = sourceSubscriptions.keySet();
        server.subscribe(
            contextId,
            ImmutableMap.of(NotificationKind.OUTLINE, ListSourceSet.create(sourceSet)));
      }
    }
  }

  void internalComputedHighlights(String contextId, Source source, HighlightRegion[] highlights) {
    Map<Source, Set<AnalysisServerHighlightsListener>> sourceSubscriptions = highlightsSubscriptions.get(contextId);
    if (sourceSubscriptions == null) {
      return;
    }
    Set<AnalysisServerHighlightsListener> subscriptions = sourceSubscriptions.get(source);
    if (subscriptions == null) {
      return;
    }
    subscriptions = ImmutableSet.copyOf(subscriptions);
    for (AnalysisServerHighlightsListener listener : subscriptions) {
      listener.computedHighlights(contextId, source, highlights);
    }
  }

  void internalComputedNavigation(String contextId, Source source, NavigationRegion[] targets) {
    Map<Source, NavigationRegion[]> contextRegions = navigationData.get(contextId);
    if (contextRegions == null) {
      contextRegions = Maps.newHashMap();
      navigationData.put(contextId, contextRegions);
    }
    contextRegions.put(source, targets);
  }

  void internalComputedOutline(String contextId, Source source, Outline outline) {
    Map<Source, Set<AnalysisServerOutlineListener>> sourceSubscriptions = outlineSubscriptions.get(contextId);
    if (sourceSubscriptions == null) {
      return;
    }
    Set<AnalysisServerOutlineListener> subscriptions = sourceSubscriptions.get(source);
    if (subscriptions == null) {
      return;
    }
    subscriptions = ImmutableSet.copyOf(subscriptions);
    for (AnalysisServerOutlineListener listener : subscriptions) {
      listener.computedOutline(contextId, source, outline);
    }
  }
}
