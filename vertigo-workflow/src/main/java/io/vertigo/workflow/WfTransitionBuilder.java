/**
 * vertigo - simple java starter
 *
 * Copyright (C) 2013-2019, KleeGroup, direction.technique@kleegroup.com (http://www.kleegroup.com)
 * KleeGroup, Centre d'affaire la Boursidiere - BP 159 - 92357 Le Plessis Robinson Cedex - France
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.vertigo.workflow;

import io.vertigo.lang.Builder;
import io.vertigo.workflow.domain.model.WfTransitionDefinition;

/**
 * Builder for a transition
 * @author xdurand
 *
 */
public final class WfTransitionBuilder implements Builder<WfTransitionDefinition> {

	private String myName;
	private final Long wfwdId;
	private final Long wfadIdFrom;
	private final Long wfadIdTo;

	/**
	 * Builder for transitions
	 * @param wfwdId
	 * @param wfadIdFrom
	 * @param wfadIdTo
	 */
	public WfTransitionBuilder(final Long wfwdId, final Long wfadIdFrom, final Long wfadIdTo) {
		this.wfwdId = wfwdId;
		this.wfadIdFrom = wfadIdFrom;
		this.wfadIdTo = wfadIdTo;
	}

	/**
	 *
	 * @param name
	 * @return the builder
	 */
	public WfTransitionBuilder withName(final String name) {
		myName = name;
		return this;
	}

	@Override
	public WfTransitionDefinition build() {
		final WfTransitionDefinition wfTransitionDefinition = new WfTransitionDefinition();

		wfTransitionDefinition.setName(myName == null ? WfCodeTransition.DEFAULT.getTransitionName() : myName);
		wfTransitionDefinition.setWfadIdFrom(wfadIdFrom);
		wfTransitionDefinition.setWfadIdTo(wfadIdTo);
		wfTransitionDefinition.setWfwdId(wfwdId);

		return wfTransitionDefinition;
	}

}
