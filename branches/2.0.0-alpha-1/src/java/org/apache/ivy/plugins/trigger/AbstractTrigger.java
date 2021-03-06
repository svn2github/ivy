/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.plugins.trigger;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.event.IvyEvent;
import org.apache.ivy.core.event.IvyEventFilter;
import org.apache.ivy.core.event.IvyListener;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.util.filter.Filter;

/**
 * Base class for easy trigger implementation.
 * 
 * This base class takes of the event filtering part, the only method to implement in subclasses
 * is {@link IvyListener#progress(IvyEvent)} which should do whatever the trigger needs to do when
 * the event occurs. This method will only be called when an event matching the trigger filter occurs.
 * 
 * 
 * 
 * @since 1.4
 * @author Xavier Hanin
 *
 */
public abstract class AbstractTrigger implements Trigger {
	private Filter _filter;
	
	private String _event;
	private String _filterExpression;
	private String _matcher = PatternMatcher.EXACT;
	
	public Filter getEventFilter() {
		if (_filter == null) {
			_filter = createFilter();
		}
		return _filter;
	}

	private Filter createFilter() {
		return new IvyEventFilter(getEvent(), getFilter(), getPatternMatcher());
	}

	private PatternMatcher getPatternMatcher() {
		return IvyContext.getContext().getSettings().getMatcher(_matcher);
	}

	public String getEvent() {
		return _event;
	}

	public void setEvent(String event) {
		_event = event;
	}

	public String getFilter() {
		return _filterExpression;
	}

	public void setFilter(String filterExpression) {
		_filterExpression = filterExpression;
	}

	public String getMatcher() {
		return _matcher;
	}

	public void setMatcher(String matcher) {
		_matcher = matcher;
	}


}
