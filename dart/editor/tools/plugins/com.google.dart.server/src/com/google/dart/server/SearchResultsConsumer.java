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
package com.google.dart.server;

import com.google.dart.engine.source.Source;

/**
 * The interface {@code SearchReferencesConsumer} defines the behavior of objects that consume
 * {@link SearchResult}s.
 * 
 * @coverage dart.server
 */
public interface SearchResultsConsumer {
  /**
   * {@link SearchResult}s have been computed.
   * 
   * @param contextId the identifier of the context to search within
   * @param source the {@link Source} with element
   * @param offset the offset within the {@code source}
   * @param searchResults an array of {@link SearchResult}s computed so far
   * @param isLastResult is {@code true} if this is the last set of results
   */
  public void computedReferences(String contextId, Source source, int offset,
      SearchResult[] searchResults, boolean isLastResult);
}
