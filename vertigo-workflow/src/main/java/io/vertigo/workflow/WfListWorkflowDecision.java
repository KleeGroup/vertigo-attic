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

import java.util.ArrayList;
import java.util.List;

import io.vertigo.workflow.domain.instance.WfWorkflow;

/**
 * 
 * @author xdurand
 *
 */
public final class WfListWorkflowDecision {
	private WfWorkflow wfWorkflow;

	private List<WfWorkflowDecision> workflowDecisions = new ArrayList<>();

	/**
	 * @return the wfWorkflow
	 */
	public WfWorkflow getWfWorkflow() {
		return wfWorkflow;
	}

	/**
	 * @param wfWorkflow
	 *            the wfWorkflow to set
	 */
	public void setWfWorkflow(final WfWorkflow wfWorkflow) {
		this.wfWorkflow = wfWorkflow;
	}

	/**
	 * @return the workflowDecisions
	 */
	public List<WfWorkflowDecision> getWorkflowDecisions() {
		return workflowDecisions;
	}

	/**
	 * @param workflowDecisions
	 *            the workflowDecisions to set
	 */
	public void setWorkflowDecisions(final List<WfWorkflowDecision> workflowDecisions) {
		this.workflowDecisions = workflowDecisions;
	}

}
