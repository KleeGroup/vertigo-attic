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
/**
 *
 */
package io.vertigo.plugins.workflow.sql;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import io.vertigo.dynamo.criteria.Criteria;
import io.vertigo.dynamo.criteria.Criterions;
import io.vertigo.impl.workflow.WorkflowStorePlugin;
import io.vertigo.rules.dao.RuleConditionDefinitionDAO;
import io.vertigo.rules.dao.RuleDefinitionDAO;
import io.vertigo.rules.dao.RuleFilterDefinitionDAO;
import io.vertigo.rules.dao.SelectorDefinitionDAO;
import io.vertigo.rules.domain.RuleConditionDefinition;
import io.vertigo.rules.domain.RuleDefinition;
import io.vertigo.rules.domain.RuleFilterDefinition;
import io.vertigo.rules.domain.SelectorDefinition;
import io.vertigo.workflow.WfCodeTransition;
import io.vertigo.workflow.WfTransitionCriteria;
import io.vertigo.workflow.dao.instance.WfActivityDAO;
import io.vertigo.workflow.dao.instance.WfDecisionDAO;
import io.vertigo.workflow.dao.instance.WfWorkflowDAO;
import io.vertigo.workflow.dao.model.WfActivityDefinitionDAO;
import io.vertigo.workflow.dao.model.WfTransitionDefinitionDAO;
import io.vertigo.workflow.dao.model.WfWorkflowDefinitionDAO;
import io.vertigo.workflow.dao.workflow.WorkflowPAO;
import io.vertigo.workflow.domain.DtDefinitions.WfDecisionFields;
import io.vertigo.workflow.domain.DtDefinitions.WfWorkflowDefinitionFields;
import io.vertigo.workflow.domain.DtDefinitions.WfWorkflowFields;
import io.vertigo.workflow.domain.instance.WfActivity;
import io.vertigo.workflow.domain.instance.WfDecision;
import io.vertigo.workflow.domain.instance.WfWorkflow;
import io.vertigo.workflow.domain.model.WfActivityDefinition;
import io.vertigo.workflow.domain.model.WfTransitionDefinition;
import io.vertigo.workflow.domain.model.WfWorkflowDefinition;

/**
 * @author OHJAJI
 */
public class SQLWorkflowStorePlugin implements WorkflowStorePlugin {

	@Inject
	private WorkflowPAO workflowPAO;
	@Inject
	private WfTransitionDefinitionDAO wfTransitionDefinitionDAO;
	@Inject
	private WfActivityDefinitionDAO wfActivityDefinitionDAO;
	@Inject
	private WfWorkflowDefinitionDAO wfWorkflowDefinitionDAO;
	@Inject
	private RuleDefinitionDAO ruleDefinitionDAO;
	@Inject
	private RuleConditionDefinitionDAO ruleConditionDefinitionDAO;
	@Inject
	private SelectorDefinitionDAO selectorDefinitionDAO;
	@Inject
	private RuleFilterDefinitionDAO ruleFilterDefinitionDAO;
	@Inject
	private WfActivityDAO wfActivityDAO;
	@Inject
	private WfDecisionDAO wfDecisionDAO;
	@Inject
	private WfWorkflowDAO wfWorkflowDAO;

	/** {@inheritDoc} */
	@Override
	public void createWorkflowInstance(final WfWorkflow workflow) {
		wfWorkflowDAO.save(workflow);
	}

	/** {@inheritDoc} */
	@Override
	public WfWorkflow readWorkflowInstanceById(final Long wfwId) {
		return wfWorkflowDAO.get(wfwId);
	}

	/** {@inheritDoc} */
	@Override
	public void updateWorkflowInstance(final WfWorkflow workflow) {
		wfWorkflowDAO.save(workflow);
	}

	/** {@inheritDoc} */
	@Override
	public WfActivity readActivity(final Long wfadId) {
		return wfActivityDAO.get(wfadId);
	}

	/** {@inheritDoc} */
	@Override
	public void createActivity(final WfActivity wfActivity) {
		wfActivityDAO.save(wfActivity);
	}

	/** {@inheritDoc} */
	@Override
	public void updateActivity(final WfActivity wfActivity) {
		wfActivityDAO.update(wfActivity);
	}

	/** {@inheritDoc} */
	@Override
	public void createDecision(final WfDecision wfDecision) {
		wfDecisionDAO.save(wfDecision);
	}

	/** {@inheritDoc} */
	@Override
	public List<WfDecision> findAllDecisionByActivity(final WfActivity wfActivity) {
		return wfDecisionDAO.getListByDtFieldName(WfDecisionFields.WFA_ID, wfActivity.getWfaId(), Integer.MAX_VALUE);
	}

	/** {@inheritDoc} */
	@Override
	public boolean hasNextActivity(final WfActivity activity) {
		return hasNextActivity(activity, WfCodeTransition.DEFAULT.getTransitionName());

	}

	/** {@inheritDoc} */
	@Override
	public boolean hasNextActivity(final WfActivity activity, final String transitionName) {
		return workflowPAO.hasNextTransition(activity.getWfadId(), transitionName) > 0;
	}

	/** {@inheritDoc} */
	@Override
	public int countDefaultTransitions(final WfWorkflowDefinition wfWorkflowDefinition) {
		return workflowPAO.countDefaultTransactions(wfWorkflowDefinition.getWfwdId());
	}

	/** {@inheritDoc} */
	@Override
	public void createWorkflowDefinition(final WfWorkflowDefinition workflowDefinition) {
		wfWorkflowDefinitionDAO.save(workflowDefinition);
	}

	/** {@inheritDoc} */
	@Override
	public WfWorkflowDefinition readWorkflowDefinition(final Long wfwdId) {
		return wfWorkflowDefinitionDAO.get(wfwdId);
	}

	/** {@inheritDoc} */
	@Override
	public WfWorkflowDefinition readWorkflowDefinition(final String definitionName) {
		final Criteria<WfWorkflowDefinition> criteria = Criterions.isEqualTo(WfWorkflowDefinitionFields.NAME, definitionName);
		return wfWorkflowDefinitionDAO.find(criteria);
	}

	/** {@inheritDoc} */
	@Override
	public void updateWorkflowDefinition(final WfWorkflowDefinition wfWorkflowDefinition) {
		wfWorkflowDefinitionDAO.save(wfWorkflowDefinition);
	}

	/** {@inheritDoc} */
	@Override
	public void createActivityDefinition(final WfWorkflowDefinition wfWorkflowDefinition,
			final WfActivityDefinition wfActivityDefinition) {
		wfActivityDefinition.setWfwdId(wfWorkflowDefinition.getWfwdId());
		wfActivityDefinitionDAO.save(wfActivityDefinition);
	}

	/** {@inheritDoc} */
	@Override
	public WfActivityDefinition readActivityDefinition(final Long wfadId) {
		return wfActivityDefinitionDAO.get(wfadId);
	}

	/** {@inheritDoc} */
	@Override
	public Optional<WfActivityDefinition> findActivityDefinitionByPosition(final WfWorkflowDefinition wfWorkflowDefinition,
			final int position) {
		return wfActivityDefinitionDAO
				.findActivityDefinitionByPosition(wfWorkflowDefinition.getWfwdId(), position);
	}

	/** {@inheritDoc} */
	@Override
	public List<WfActivityDefinition> findAllDefaultActivityDefinitions(
			final WfWorkflowDefinition wfWorkflowDefinition) {
		return wfActivityDefinitionDAO.findAllDefaultActivityDefinitions(wfWorkflowDefinition.getWfwdId(),
				WfCodeTransition.DEFAULT.getTransitionName());
	}

	/** {@inheritDoc} */
	@Override
	public void addTransition(final WfTransitionDefinition transition) {
		wfTransitionDefinitionDAO.save(transition);
	}

	/** {@inheritDoc} */
	@Override
	public List<WfActivity> findActivitiesByWorkflowId(final WfWorkflow wfWorkflow) {
		return wfActivityDAO.getListByDtFieldName(WfWorkflowFields.WFW_ID, wfWorkflow.getWfwId(), Integer.MAX_VALUE);

	}

	/** {@inheritDoc} */
	@Override
	public Optional<WfActivity> findActivityByDefinitionWorkflow(final WfWorkflow wfWorkflow, final WfActivityDefinition wfActivityDefinition) {
		return wfActivityDAO.findActivityByDefinitionWorkflow(wfWorkflow.getWfwId(),
				wfActivityDefinition.getWfadId());
	}

	/** {@inheritDoc} */
	@Override
	public List<WfDecision> findDecisionsByWorkflowId(final WfWorkflow wfWorkflow) {
		return wfDecisionDAO.findDecisionsByWorkflowId(wfWorkflow.getWfwId());
	}

	/** {@inheritDoc} */
	@Override
	public WfActivityDefinition findNextActivity(final Long wfadId) {
		return findNextActivity(wfadId, WfCodeTransition.DEFAULT.getTransitionName());
	}

	/** {@inheritDoc} */
	@Override
	public WfActivityDefinition findNextActivity(final Long wfadId, final String transitionName) {
		final WfTransitionDefinition wfTransitionDefinition = wfTransitionDefinitionDAO.findNextActivity(wfadId, transitionName);
		return wfActivityDefinitionDAO.get(wfTransitionDefinition.getWfadIdTo());
	}

	/** {@inheritDoc} */
	@Override
	public Optional<WfTransitionDefinition> findTransition(final WfTransitionCriteria wfTransitionCriteria) {
		return wfTransitionDefinitionDAO.findTransition(
				wfTransitionCriteria.getTransitionName(), Optional.ofNullable(wfTransitionCriteria.getWfadIdTo()),
				Optional.ofNullable(wfTransitionCriteria.getWfadIdFrom()));
	}

	/** {@inheritDoc} */
	@Override
	public void incrementActivityDefinitionPositionsAfter(final Long wfwdId, final int position) {
		throw new UnsupportedOperationException();
	}

	/** {@inheritDoc} */
	@Override
	public List<WfDecision> readDecisionsByActivityId(final Long wfaId) {
		return wfDecisionDAO.getListByDtFieldName(WfDecisionFields.WFA_ID, wfaId, Integer.MAX_VALUE);
	}

	/** {@inheritDoc} */
	@Override
	public WfWorkflow readWorkflowInstanceByItemId(final Long wfwdId, final Long itemId) {
		return wfWorkflowDAO.readWorkflowInstanceByItemId(wfwdId, itemId);
	}

	/** {@inheritDoc} */
	@Override
	public WfWorkflow readWorkflowInstanceForUpdateById(final Long wfwId) {
		return wfWorkflowDAO.readWorkflowForUpdate(wfwId);
	}

	/** {@inheritDoc} */
	@Override
	public void updateDecision(final WfDecision wfDecision) {
		wfDecisionDAO.save(wfDecision);
	}

	/** {@inheritDoc} */
	@Override
	public void updateTransition(final WfTransitionDefinition wfTransitionDefinition) {
		wfTransitionDefinitionDAO.save(wfTransitionDefinition);
	}

	/** {@inheritDoc} */
	@Override
	public List<RuleDefinition> findAllRulesByWorkflowDefinitionId(final long wfwdId) {
		return ruleDefinitionDAO.findAllRulesByWorkflowDefinitionId(wfwdId);
	}

	/** {@inheritDoc} */
	@Override
	public List<RuleConditionDefinition> findAllConditionsByWorkflowDefinitionId(final long wfwdId) {
		return ruleConditionDefinitionDAO.findAllConditionsByWorkflowDefinitionId(wfwdId);
	}

	/** {@inheritDoc} */
	@Override
	public List<SelectorDefinition> findAllSelectorsByWorkflowDefinitionId(final long wfwdId) {
		return selectorDefinitionDAO.findAllSelectorsByWorkflowDefinitionId(wfwdId);
	}

	/** {@inheritDoc} */
	@Override
	public List<RuleFilterDefinition> findAllFiltersByWorkflowDefinitionId(final long wfwdId) {
		return ruleFilterDefinitionDAO.findAllFiltersByWorkflowDefinitionId(wfwdId);
	}

}
