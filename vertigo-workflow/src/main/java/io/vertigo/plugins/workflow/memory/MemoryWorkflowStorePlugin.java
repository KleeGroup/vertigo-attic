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
package io.vertigo.plugins.workflow.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.vertigo.impl.workflow.WorkflowStorePlugin;
import io.vertigo.lang.Assertion;
import io.vertigo.rules.domain.RuleConditionDefinition;
import io.vertigo.rules.domain.RuleDefinition;
import io.vertigo.rules.domain.RuleFilterDefinition;
import io.vertigo.rules.domain.SelectorDefinition;
import io.vertigo.rules.services.RuleServices;
import io.vertigo.workflow.WfCodeTransition;
import io.vertigo.workflow.WfTransitionCriteria;
import io.vertigo.workflow.domain.instance.WfActivity;
import io.vertigo.workflow.domain.instance.WfDecision;
import io.vertigo.workflow.domain.instance.WfWorkflow;
import io.vertigo.workflow.domain.model.WfActivityDefinition;
import io.vertigo.workflow.domain.model.WfTransitionDefinition;
import io.vertigo.workflow.domain.model.WfWorkflowDefinition;

/**
 *
 * @author xdurand
 *
 */
public final class MemoryWorkflowStorePlugin implements WorkflowStorePlugin {

	// WorkflowInstance
	private final AtomicLong memoryWorkflowInstanceSequenceGenerator = new AtomicLong(0);
	private final Map<Long, WfWorkflow> inMemoryWorkflowInstanceStore = new ConcurrentHashMap<>();

	// Transition
	private final Map<String, WfTransitionDefinition> transitionsNext = new ConcurrentHashMap<>();
	private final AtomicLong memoryTransactionSequenceGenerator = new AtomicLong(0);

	// Activity
	private final Map<Long, WfActivity> inMemoryActivityStore = new ConcurrentHashMap<>();
	private final AtomicLong memoryActivitySequenceGenerator = new AtomicLong(0);

	// Decision
	private final Map<Long, WfDecision> inMemoryDecisionStore = new ConcurrentHashMap<>();
	private final AtomicLong memoryDecisionSequenceGenerator = new AtomicLong(0);

	// ActivityDefinition
	private final Map<Long, WfActivityDefinition> inMemoryActivityDefinitionStore = new ConcurrentHashMap<>();
	private final AtomicLong memoryActivityDefinitionSequenceGenerator = new AtomicLong(0);

	// WorkflowDefinition
	private final Map<Long, WfWorkflowDefinition> inMemoryWorkflowDefinitionStore = new ConcurrentHashMap<>();
	private final AtomicLong memoryWorkflowDefinitionSequenceGenerator = new AtomicLong(0);

	@Inject
	private RuleServices ruleServices;

	@Override
	public void createWorkflowInstance(final WfWorkflow workflow) {
		Assertion.checkNotNull(workflow);
		Assertion.checkState(workflow.getWfwId() == null, "A new workflow must not have an id");
		// ---
		final long generatedId = memoryWorkflowInstanceSequenceGenerator.addAndGet(1);
		workflow.setWfwId(generatedId);
		inMemoryWorkflowInstanceStore.put(generatedId, workflow);
	}

	@Override
	public WfWorkflow readWorkflowInstanceById(final Long wfwId) {
		Assertion.checkNotNull(wfwId);
		// ---
		return inMemoryWorkflowInstanceStore.get(wfwId);
	}

	@Override
	public void updateWorkflowInstance(final WfWorkflow workflow) {
		Assertion.checkNotNull(workflow);
		Assertion.checkNotNull(workflow.getWfwId());
		Assertion.checkState(inMemoryWorkflowInstanceStore.containsKey(workflow.getWfwId()),
				"This workflow cannot be updated : It does not exist in the store");
		// ---
		inMemoryWorkflowInstanceStore.put(workflow.getWfwId(), workflow);
	}

	@Override
	public List<WfActivity> findActivitiesByWorkflowId(final WfWorkflow wfWorkflow) {
		Assertion.checkNotNull(wfWorkflow);
		// ---
		final List<WfActivity> wfActivities = new ArrayList<>();
		for (final WfActivity wfActivity : inMemoryActivityStore.values()) {
			if (wfWorkflow.getWfwId().equals(wfActivity.getWfwId())) {
				wfActivities.add(wfActivity);
			}
		}

		return wfActivities;
	}

	@Override
	public List<WfDecision> findDecisionsByWorkflowId(final WfWorkflow wfWorkflow) {
		Assertion.checkNotNull(wfWorkflow);
		Assertion.checkNotNull(wfWorkflow.getWfwdId());
		// ---

		final List<WfActivity> wfActivities = findActivitiesByWorkflowId(wfWorkflow);

		final List<Long> wfActivitiesId = wfActivities.stream().map(a -> a.getWfaId()).collect(Collectors.toList());

		final List<WfDecision> wfDecisions = new ArrayList<>();
		for (final WfDecision wfDecision : inMemoryDecisionStore.values()) {
			if (wfActivitiesId.contains(wfDecision.getWfaId())) {
				wfDecisions.add(wfDecision);
			}
		}

		return wfDecisions;
	}

	@Override
	public void updateDecision(final WfDecision wfDecision) {
		Assertion.checkNotNull(wfDecision);
		Assertion.checkNotNull(wfDecision.getWfeId());
		Assertion.checkState(inMemoryDecisionStore.containsKey(wfDecision.getWfeId()),
				"This workflow cannot be updated : It does not exist in the store");
		// ---
		inMemoryDecisionStore.put(wfDecision.getWfeId(), wfDecision);
	}

	@Override
	public List<WfDecision> readDecisionsByActivityId(final Long wfaId) {
		final List<WfDecision> collect = new ArrayList<>();
		for (final WfDecision wfDecision : inMemoryDecisionStore.values()) {
			if (wfaId.equals(wfDecision.getWfaId())) {
				collect.add(wfDecision);
			}
		}

		return collect;
	}

	@Override
	public Optional<WfActivity> findActivityByDefinitionWorkflow(final WfWorkflow wfWorkflow,
			final WfActivityDefinition wfActivityDefinition) {

		for (final WfActivity wfActivity : inMemoryActivityStore.values()) {
			if (wfActivityDefinition.getWfadId().equals(wfActivity.getWfadId())) {
				return Optional.of(wfActivity);
			}
		}

		return Optional.empty();
	}

	@Override
	public void updateTransition(final WfTransitionDefinition transition) {
		String key = null;
		for (final Entry<String, WfTransitionDefinition> entry : transitionsNext.entrySet()) {
			if (entry.getValue().getWftdId().equals(transition.getWftdId())) {
				key = entry.getKey();
				break;
			}
		}

		if (key != null) {
			transitionsNext.remove(key);
		}
		transitionsNext.put(transition.getWfadIdFrom() + "|" + transition.getName(), transition);
	}

	@Override
	public Optional<WfTransitionDefinition> findTransition(final WfTransitionCriteria wfTransitionCriteria) {
		Assertion.checkNotNull(wfTransitionCriteria);
		// ---

		for (final WfTransitionDefinition tr : transitionsNext.values()) {
			final boolean matchFrom = wfTransitionCriteria.getWfadIdFrom() == null
					|| wfTransitionCriteria.getWfadIdFrom().equals(tr.getWfadIdFrom());
			final boolean matchTo = wfTransitionCriteria.getWfadIdTo() == null
					|| wfTransitionCriteria.getWfadIdTo().equals(tr.getWfadIdTo());

			if (matchFrom && matchTo && wfTransitionCriteria.getTransitionName().equals(tr.getName())) {
				return Optional.of(tr);
			}
		}
		return Optional.empty();
	}

	@Override
	public void incrementActivityDefinitionPositionsAfter(final Long wfwdId, final int position) {
		for (final WfActivityDefinition wfActivityDefinition : inMemoryActivityDefinitionStore.values()) {
			if (wfwdId.equals(wfActivityDefinition.getWfwdId()) && wfActivityDefinition.getLevel() >= position) {
				wfActivityDefinition.setLevel(wfActivityDefinition.getLevel() + 1);
			}
		}

	}

	@Override
	public boolean hasNextActivity(final WfActivity activity) {
		return hasNextActivity(activity, WfCodeTransition.DEFAULT.getTransitionName());
	}

	@Override
	public boolean hasNextActivity(final WfActivity activity, final String transitionName) {
		return transitionsNext.containsKey(activity.getWfadId() + "|" + transitionName);
	}

	@Override
	public WfActivity readActivity(final Long wfadId) {
		Assertion.checkNotNull(wfadId);
		// ---
		return inMemoryActivityStore.get(wfadId);
	}

	@Override
	public void createActivity(final WfActivity wfActivity) {
		Assertion.checkNotNull(wfActivity);
		Assertion.checkState(wfActivity.getWfaId() == null, "A new activity must not have an id");
		// ---
		final long generatedId = memoryActivitySequenceGenerator.addAndGet(1);
		wfActivity.setWfaId(generatedId);
		inMemoryActivityStore.put(generatedId, wfActivity);
	}

	@Override
	public void updateActivity(final WfActivity wfActivity) {
		Assertion.checkNotNull(wfActivity);
		Assertion.checkNotNull(wfActivity.getWfaId());
		Assertion.checkState(inMemoryActivityStore.containsKey(wfActivity.getWfaId()),
				"This activity cannot be updated : It does not exist in the store");
		// ---
		inMemoryActivityStore.put(wfActivity.getWfaId(), wfActivity);
	}

	@Override
	public void createDecision(final WfDecision wfDecision) {
		Assertion.checkNotNull(wfDecision);
		Assertion.checkNotNull(wfDecision.getWfaId());
		Assertion.checkState(wfDecision.getWfeId() == null, "A new decision must not have an id");
		// ---
		final long generatedId = memoryDecisionSequenceGenerator.addAndGet(1);
		wfDecision.setWfeId(generatedId);
		inMemoryDecisionStore.put(wfDecision.getWfeId(), wfDecision);
	}

	@Override
	public List<WfDecision> findAllDecisionByActivity(final WfActivity wfActivity) {
		Assertion.checkNotNull(wfActivity);
		Assertion.checkNotNull(wfActivity.getWfaId());
		// ---
		final List<WfDecision> wfDecisions = new ArrayList<>();
		for (final WfDecision wfDecision : inMemoryDecisionStore.values()) {
			if (wfActivity.getWfaId().equals(wfDecision.getWfaId())) {
				wfDecisions.add(wfDecision);
			}
		}

		return wfDecisions;
	}

	// Definition
	@Override
	public int countDefaultTransitions(final WfWorkflowDefinition wfWorkflowDefinition) {
		Assertion.checkNotNull(wfWorkflowDefinition);
		// ---
		final Long idActivityDefinition = wfWorkflowDefinition.getWfadId();
		if (idActivityDefinition == null) {
			// The workflow don't have a starting activity
			return 0;
		}
		WfTransitionDefinition transitionNext = transitionsNext
				.get(idActivityDefinition + "|" + WfCodeTransition.DEFAULT.getTransitionName());

		int count = 0;
		while (transitionNext != null) {
			final WfActivityDefinition wfNextActivityDefinition = inMemoryActivityDefinitionStore
					.get(transitionNext.getWfadIdTo());
			transitionNext = transitionsNext
					.get(wfNextActivityDefinition.getWfadId() + "|" + WfCodeTransition.DEFAULT.getTransitionName());
			count++;
		}

		return count;
	}

	@Override
	public void createWorkflowDefinition(final WfWorkflowDefinition workflowDefinition) {
		Assertion.checkNotNull(workflowDefinition);
		Assertion.checkState(workflowDefinition.getWfwdId() == null, "A new workflow must not have an id");
		// ---
		final long generatedId = memoryWorkflowDefinitionSequenceGenerator.addAndGet(1);
		workflowDefinition.setWfwdId(generatedId);
		inMemoryWorkflowDefinitionStore.put(generatedId, workflowDefinition);
	}

	@Override
	public WfWorkflowDefinition readWorkflowDefinition(final Long wfwdId) {
		Assertion.checkNotNull(wfwdId);
		// ---
		return inMemoryWorkflowDefinitionStore.get(wfwdId);
	}

	@Override
	public WfWorkflowDefinition readWorkflowDefinition(final String definitionName) {
		Assertion.checkNotNull(definitionName);
		// ---
		for (final WfWorkflowDefinition wfWorkflowDefinition : inMemoryWorkflowDefinitionStore.values()) {
			if (definitionName.equals(wfWorkflowDefinition.getName())) {
				return wfWorkflowDefinition;
			}
		}

		return null;
	}

	@Override
	public void updateWorkflowDefinition(final WfWorkflowDefinition wfWorkflowDefinition) {
		Assertion.checkNotNull(wfWorkflowDefinition);
		Assertion.checkNotNull(wfWorkflowDefinition.getWfwdId());
		Assertion.checkState(inMemoryWorkflowDefinitionStore.containsKey(wfWorkflowDefinition.getWfwdId()),
				"This activity cannot be updated : It does not exist in the store");
		// ---
		inMemoryWorkflowDefinitionStore.put(wfWorkflowDefinition.getWfwdId(), wfWorkflowDefinition);
	}

	@Override
	public void createActivityDefinition(final WfWorkflowDefinition wfWorkflowDefinition,
			final WfActivityDefinition wfActivityDefinition) {
		Assertion.checkNotNull(wfWorkflowDefinition);
		Assertion.checkNotNull(wfActivityDefinition);
		Assertion.checkState(wfActivityDefinition.getWfadId() == null, "A new workflow must not have an id");
		// ---
		final long generatedId = memoryActivityDefinitionSequenceGenerator.addAndGet(1);
		wfActivityDefinition.setWfadId(generatedId);

		inMemoryActivityDefinitionStore.put(generatedId, wfActivityDefinition);
	}

	@Override
	public WfActivityDefinition readActivityDefinition(final Long wfadId) {
		Assertion.checkNotNull(wfadId);
		// ---
		return inMemoryActivityDefinitionStore.get(wfadId);
	}

	@Override
	public Optional<WfActivityDefinition> findActivityDefinitionByPosition(final WfWorkflowDefinition wfWorkflowDefinition,
			final int position) {
		Assertion.checkNotNull(wfWorkflowDefinition);
		// ---

		final Long idActivity = wfWorkflowDefinition.getWfadId();

		if (idActivity == null) {
			// The workflow don't have a starting activity
			return Optional.empty();
		}

		if (position == 1) {
			return Optional.of(readActivityDefinition(idActivity));
		}

		WfTransitionDefinition transitionNext = transitionsNext
				.get(idActivity + "|" + WfCodeTransition.DEFAULT.getTransitionName());

		int i = 1;
		while (transitionNext != null && i < (position - 1)) {
			final WfActivityDefinition wfNextActivityDefinition = inMemoryActivityDefinitionStore
					.get(transitionNext.getWfadIdTo());
			transitionNext = transitionsNext
					.get(wfNextActivityDefinition.getWfadId() + "|" + WfCodeTransition.DEFAULT.getTransitionName());
			i++;
		}

		if (transitionNext == null) {
			return Optional.empty();
		}

		return Optional.of(readActivityDefinition(transitionNext.getWfadIdTo()));
	}

	@Override
	public List<WfActivityDefinition> findAllDefaultActivityDefinitions(
			final WfWorkflowDefinition wfWorkflowDefinition) {
		Assertion.checkNotNull(wfWorkflowDefinition);
		// ---
		final Long idStartActivity = wfWorkflowDefinition.getWfadId();
		final List<WfActivityDefinition> retAllDefaultActivities = new ArrayList<>();

		if (idStartActivity != null) {
			final WfActivityDefinition first = inMemoryActivityDefinitionStore.get(idStartActivity);
			retAllDefaultActivities.add(first);

			WfTransitionDefinition transitionNext = transitionsNext
					.get(idStartActivity + "|" + WfCodeTransition.DEFAULT.getTransitionName());

			while (transitionNext != null) {
				final WfActivityDefinition wfNextActivityDefinition = inMemoryActivityDefinitionStore
						.get(transitionNext.getWfadIdTo());
				retAllDefaultActivities.add(wfNextActivityDefinition);
				transitionNext = transitionsNext
						.get(wfNextActivityDefinition.getWfadId() + "|" + WfCodeTransition.DEFAULT.getTransitionName());
			}
		}

		return retAllDefaultActivities;
	}

	@Override
	public void addTransition(final WfTransitionDefinition transition) {
		Assertion.checkNotNull(transition);
		Assertion.checkNotNull(transition.getWfadIdFrom());
		Assertion.checkNotNull(transition.getName());
		// ---
		final long key = memoryTransactionSequenceGenerator.addAndGet(1);
		transition.setWftdId(key);
		transitionsNext.put(transition.getWfadIdFrom() + "|" + transition.getName(), transition);
	}

	@Override
	public WfWorkflow readWorkflowInstanceForUpdateById(final Long wfwId) {
		// No lock for Memory Plugin
		return readWorkflowInstanceById(wfwId);
	}

	@Override
	public WfWorkflow readWorkflowInstanceByItemId(final Long wfwdId, final Long itemId) {
		for (final WfWorkflow wfWorkflow : inMemoryWorkflowInstanceStore.values()) {
			if (itemId.equals(wfWorkflow.getItemId()) && wfwdId.equals(wfWorkflow.getWfwdId())) {
				return wfWorkflow;
			}
		}

		return null;
	}

	@Override
	public WfActivityDefinition findNextActivity(final Long wfadId) {
		return findNextActivity(wfadId, WfCodeTransition.DEFAULT.getTransitionName());
	}

	@Override
	public WfActivityDefinition findNextActivity(final Long wfadId, final String transitionName) {
		final WfTransitionDefinition transitionNext = transitionsNext.get(wfadId + "|" + transitionName);
		return inMemoryActivityDefinitionStore.get(transitionNext.getWfadIdTo());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.vertigo.x.workflow.WorkflowStore#findAllRulesByWorkflowDefinitionId(
	 * long)
	 */
	@Override
	public List<RuleDefinition> findAllRulesByWorkflowDefinitionId(final long wfwdId) {
		final WfWorkflowDefinition wfWorkflowDefinition = inMemoryWorkflowDefinitionStore.get(wfwdId);
		final List<WfActivityDefinition> activities = findAllDefaultActivityDefinitions(wfWorkflowDefinition);
		final List<RuleDefinition> ret = new ArrayList<>();

		for (final WfActivityDefinition activity : activities) {
			ret.addAll(ruleServices.getRulesForItemId(activity.getWfadId()));
		}
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.vertigo.x.workflow.WorkflowStore#
	 * findAllConditionsByWorkflowDefinitionId(long)
	 */
	@Override
	public List<RuleConditionDefinition> findAllConditionsByWorkflowDefinitionId(final long wfwdId) {
		final List<RuleDefinition> rules = findAllRulesByWorkflowDefinitionId(wfwdId);

		final List<RuleConditionDefinition> ret = new ArrayList<>();

		for (final RuleDefinition rule : rules) {
			ret.addAll(ruleServices.getConditionsForRuleId(rule.getId()));
		}

		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.vertigo.x.workflow.WorkflowStore#
	 * findAllSelectorsByWorkflowDefinitionId(long)
	 */
	@Override
	public List<SelectorDefinition> findAllSelectorsByWorkflowDefinitionId(final long wfwdId) {
		final WfWorkflowDefinition wfWorkflowDefinition = inMemoryWorkflowDefinitionStore.get(wfwdId);
		final List<WfActivityDefinition> activities = findAllDefaultActivityDefinitions(wfWorkflowDefinition);
		final List<SelectorDefinition> ret = new ArrayList<>();

		for (final WfActivityDefinition activity : activities) {
			ret.addAll(ruleServices.getSelectorsForItemId(activity.getWfadId()));
		}
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.vertigo.x.workflow.WorkflowStore#findAllFiltersByWorkflowDefinitionId(
	 * long)
	 */
	@Override
	public List<RuleFilterDefinition> findAllFiltersByWorkflowDefinitionId(final long wfwdId) {
		final List<RuleDefinition> rules = findAllRulesByWorkflowDefinitionId(wfwdId);

		final List<RuleFilterDefinition> ret = new ArrayList<>();

		for (final RuleDefinition rule : rules) {
			ret.addAll(ruleServices.getFiltersForSelectorId(rule.getId()));
		}

		return ret;
	}

}
