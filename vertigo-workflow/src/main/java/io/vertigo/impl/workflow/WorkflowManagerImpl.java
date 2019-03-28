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
package io.vertigo.impl.workflow;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.vertigo.account.account.Account;
import io.vertigo.account.account.AccountGroup;
import io.vertigo.dynamo.domain.model.DtObject;
import io.vertigo.lang.Assertion;
import io.vertigo.rules.RuleCriteria;
import io.vertigo.rules.domain.RuleConditionDefinition;
import io.vertigo.rules.domain.RuleDefinition;
import io.vertigo.rules.domain.RuleFilterDefinition;
import io.vertigo.rules.domain.SelectorDefinition;
import io.vertigo.rules.services.RuleConstants;
import io.vertigo.rules.services.RuleContext;
import io.vertigo.rules.services.RuleServices;
import io.vertigo.workflow.WfCodeTransition;
import io.vertigo.workflow.WfTransitionBuilder;
import io.vertigo.workflow.WfTransitionCriteria;
import io.vertigo.workflow.WfWorkflowDecision;
import io.vertigo.workflow.WorkflowManager;
import io.vertigo.workflow.domain.instance.WfActivity;
import io.vertigo.workflow.domain.instance.WfDecision;
import io.vertigo.workflow.domain.instance.WfStatusEnum;
import io.vertigo.workflow.domain.instance.WfWorkflow;
import io.vertigo.workflow.domain.model.WfActivityDefinition;
import io.vertigo.workflow.domain.model.WfMultiplicityDefinitionEnum;
import io.vertigo.workflow.domain.model.WfTransitionDefinition;
import io.vertigo.workflow.domain.model.WfWorkflowDefinition;

/**
 * @author xdurand
 */
public final class WorkflowManagerImpl implements WorkflowManager {

	private final WorkflowStorePlugin workflowStorePlugin;
	private final ItemStorePlugin itemStorePlugin;
	private final RuleServices ruleServices;
	private final WorkflowPredicateAutoValidatePlugin workflowPredicateAutoValidatePlugin;

	private static final String USER_AUTO = "<AUTO>";
	private static final String TRANSITION_BACK_NAME = "back";

	/**
	 * Construct a new Workflow manager
	 *
	 * @param workflowStorePlugin
	 * @param itemStorePlugin
	 * @param ruleServices
	 * @param workflowPredicateAutoValidatePlugin
	 */
	@Inject
	public WorkflowManagerImpl(final WorkflowStorePlugin workflowStorePlugin, final ItemStorePlugin itemStorePlugin,
			final RuleServices ruleServices, final WorkflowPredicateAutoValidatePlugin workflowPredicateAutoValidatePlugin) {
		this.workflowStorePlugin = workflowStorePlugin;
		this.itemStorePlugin = itemStorePlugin;
		this.ruleServices = ruleServices;
		this.workflowPredicateAutoValidatePlugin = workflowPredicateAutoValidatePlugin;
	}

	// Instance

	@Override
	public WfWorkflow createWorkflowInstance(final String definitionName, final String username, final boolean userLogic,
			final Long item) {
		Assertion.checkNotNull(definitionName);
		Assertion.checkNotNull(username);
		// ---
		final WfWorkflowDefinition wfWorkflowDefinition = workflowStorePlugin.readWorkflowDefinition(definitionName);

		return createWorkflowInstance(wfWorkflowDefinition.getWfwdId(), username, userLogic, item);
	}

	@Override
	public WfWorkflow createWorkflowInstance(final Long wfwdId, final String username, final boolean userLogic,
			final Long item) {
		Assertion.checkNotNull(item);
		// ---
		final WfWorkflow wfWorkflow = new WfWorkflow();
		wfWorkflow.setCreationDate(new Date());
		wfWorkflow.setItemId(item);
		wfWorkflow.wfStatus().setEnumValue(WfStatusEnum.CRE);
		wfWorkflow.setWfwdId(wfwdId);
		wfWorkflow.setUserLogic(userLogic);
		wfWorkflow.setUsername(username);

		workflowStorePlugin.createWorkflowInstance(wfWorkflow);

		return wfWorkflow;
	}

	@Override
	public WfWorkflow getWorkflowInstance(final Long wfwId) {
		Assertion.checkNotNull(wfwId);
		// ---
		return workflowStorePlugin.readWorkflowInstanceById(wfwId);
	}

	@Override
	public void startInstance(final WfWorkflow wfWorkflow) {
		Assertion.checkNotNull(wfWorkflow);
		Assertion.checkState(WfStatusEnum.CRE == wfWorkflow.wfStatus().getEnumValue(),
				"A workflow must be created before starting");
		// ---
		wfWorkflow.wfStatus().setEnumValue(WfStatusEnum.STA);

		final WfWorkflowDefinition wfWorkflowDefinition = workflowStorePlugin
				.readWorkflowDefinition(wfWorkflow.getWfwdId());

		final WfActivity wfActivityCurrent = new WfActivity();
		wfActivityCurrent.setCreationDate(new Date());
		wfActivityCurrent.setWfadId(wfWorkflowDefinition.getWfadId());
		wfActivityCurrent.setWfwId(wfWorkflow.getWfwId());
		workflowStorePlugin.createActivity(wfActivityCurrent);
		wfWorkflow.setWfaId2(wfActivityCurrent.getWfaId());
		workflowStorePlugin.updateWorkflowInstance(wfWorkflow);

		autoValidateNextActivities(wfWorkflow, wfActivityCurrent, wfWorkflowDefinition.getWfadId(), WfCodeTransition.DEFAULT.getTransitionName());
	}

	@Override
	public void endInstance(final WfWorkflow wfWorkflow) {
		Assertion.checkNotNull(wfWorkflow);
		final WfStatusEnum wcsw = wfWorkflow.wfStatus().getEnumValue();
		Assertion.checkState(wcsw == WfStatusEnum.STA || wcsw == WfStatusEnum.PAU,
				"A workflow must be started or paused before ending");
		// ---
		wfWorkflow.wfStatus().setEnumValue(WfStatusEnum.END);
		workflowStorePlugin.updateWorkflowInstance(wfWorkflow);
	}

	@Override
	public void pauseInstance(final WfWorkflow wfWorkflow) {
		Assertion.checkNotNull(wfWorkflow);
		Assertion.checkState(wfWorkflow.wfStatus().getEnumValue() == WfStatusEnum.STA, "A workflow must be started before pausing");
		// ---
		wfWorkflow.wfStatus().setEnumValue(WfStatusEnum.PAU);
		workflowStorePlugin.updateWorkflowInstance(wfWorkflow);
	}

	@Override
	public void resumeInstance(final WfWorkflow wfWorkflow) {
		Assertion.checkNotNull(wfWorkflow);
		Assertion.checkState(wfWorkflow.wfStatus().getEnumValue() == WfStatusEnum.PAU, "A workflow must be paused before resuming");
		// ---
		wfWorkflow.wfStatus().setEnumValue(WfStatusEnum.STA);
		workflowStorePlugin.updateWorkflowInstance(wfWorkflow);

	}

	@Override
	public List<WfActivityDefinition> getActivityDefinitions(final WfWorkflow wfWorkflow) {

		final WfWorkflowDefinition wfDefinition = workflowStorePlugin.readWorkflowDefinition(wfWorkflow.getWfwdId());
		final List<WfActivityDefinition> activities = workflowStorePlugin.findAllDefaultActivityDefinitions(wfDefinition);

		final DtObject obj = itemStorePlugin.readItem(wfWorkflow.getItemId());

		final List<WfActivityDefinition> ret = new ArrayList<>();
		for (final WfActivityDefinition activity : activities) {
			if (!canAutoValidateActivity(activity, obj)) {
				ret.add(activity);
			}
		}

		return ret;
	}

	private static WfActivity getNewActivity(final WfActivityDefinition activityDefinition, final WfWorkflow wfWorkflow) {
		final WfActivity wfActivity = new WfActivity();
		wfActivity.setCreationDate(new Date());
		wfActivity.setWfadId(activityDefinition.getWfadId());
		wfActivity.setWfwId(wfWorkflow.getWfwId());
		return wfActivity;
	}

	private WfActivity createActivity(final WfActivityDefinition activityDefinition, final WfWorkflow wfWorkflow) {
		final WfActivity wfActivity = getNewActivity(activityDefinition, wfWorkflow);
		workflowStorePlugin.createActivity(wfActivity);
		return wfActivity;
	}

	/**
	 * Auto-validate all the next activities that can be autovalidated. An
	 * activity can be autovalidated when no rule is defined AND no user is
	 * attached for this validation
	 *
	 * @param wfWorkflow
	 * @param currentActivity
	 * @param wfActivityDefinitionId
	 * @param transitionName
	 * @return true if the end is reached, false otherwise
	 */
	public boolean autoValidateNextActivities(final WfWorkflow wfWorkflow, final WfActivity currentActivity,
			final Long wfActivityDefinitionId, final String transitionName) {

		WfActivityDefinition activityDefinition = workflowStorePlugin.readActivityDefinition(wfActivityDefinitionId);

		final DtObject object = itemStorePlugin.readItem(wfWorkflow.getItemId());
		Long wfCurrentActivityId = null;
		boolean endReached = false;
		WfActivity wfActivityCurrent = currentActivity;

		while (canAutoValidateActivity(activityDefinition, object)) {

			autoValidateDecision(wfActivityCurrent);

			if (!workflowStorePlugin.hasNextActivity(wfActivityCurrent)) {
				endReached = true;
				break;
			}
			activityDefinition = workflowStorePlugin.findNextActivity(wfActivityCurrent.getWfadId(), transitionName);

			final Optional<WfActivity> nextActivity = workflowStorePlugin.findActivityByDefinitionWorkflow(wfWorkflow,
					activityDefinition);
			if (!nextActivity.isPresent()) {
				wfActivityCurrent = createActivity(activityDefinition, wfWorkflow);
			} else {
				wfActivityCurrent = nextActivity.get();
			}

			wfCurrentActivityId = wfActivityCurrent.getWfaId();
		}

		// Remove this workflow update ?
		if (wfCurrentActivityId != null) {
			wfWorkflow.setWfaId2(wfCurrentActivityId);
			workflowStorePlugin.updateWorkflowInstance(wfWorkflow);
		}
		return endReached;
	}

	/**
	 * Autovalidate a decision
	 *
	 * @param activityDefinition
	 * @param object
	 */
	private void autoValidateDecision(final WfActivity wfActivityCurrent) {
		final WfDecision decision = new WfDecision();
		decision.setUsername(USER_AUTO);
		decision.setDecisionDate(new Date());
		decision.setWfaId(wfActivityCurrent.getWfaId());

		workflowStorePlugin.createDecision(decision);
	}

	private boolean canAutoValidateActivity(final WfActivityDefinition activityDefinition, final DtObject object) {
		return workflowPredicateAutoValidatePlugin.canAutoValidateActivity(activityDefinition, object);
	}

	/**
	 *
	 * @param wfWorkflow
	 * @param wfDecision
	 */
	@Override
	public void saveDecision(final WfWorkflow wfWorkflow, final WfDecision wfDecision) {
		Assertion.checkState(wfWorkflow.wfStatus().getEnumValue() == WfStatusEnum.STA, "A workflow must be started before saving decision");
		// ---
		final WfWorkflow wfWorkflowFetch = workflowStorePlugin.readWorkflowInstanceForUpdateById(wfWorkflow.getWfwId());

		if (wfWorkflowFetch.getWfaId2() != null && !wfWorkflow.getWfaId2().equals(wfWorkflow.getWfaId2())) {
			throw new IllegalStateException("Concurrent workflow modification");
		}

		final WfActivity currentActivity = workflowStorePlugin.readActivity(wfWorkflow.getWfaId2());

		wfDecision.setWfaId(currentActivity.getWfaId());
		if (wfDecision.getWfeId() == null) {
			workflowStorePlugin.createDecision(wfDecision);
		} else {
			workflowStorePlugin.updateDecision(wfDecision);
		}
	}

	@Override
	public Optional<WfDecision> getDecision(final WfActivity wfActivity) {
		Assertion.checkNotNull(wfActivity);
		// ---
		final WfActivityDefinition wfActivityDefinition = workflowStorePlugin.readActivityDefinition(wfActivity.getWfadId());

		if (wfActivityDefinition.wfMultiplicityDefinition().getEnumValue() != WfMultiplicityDefinitionEnum.SIN) {
			throw new IllegalArgumentException();
		}
		return workflowStorePlugin.readDecisionsByActivityId(wfActivity.getWfaId())
				.stream()
				.findFirst();
	}

	@Override
	public List<WfDecision> getDecisions(final WfActivity wfActivity) {
		Assertion.checkNotNull(wfActivity);
		// ---
		final WfActivityDefinition wfActivityDefinition = workflowStorePlugin.readActivityDefinition(wfActivity.getWfadId());

		if (wfActivityDefinition.wfMultiplicityDefinition().getEnumValue() != WfMultiplicityDefinitionEnum.MUL) {
			throw new IllegalStateException();
		}
		return workflowStorePlugin.readDecisionsByActivityId(wfActivity.getWfaId());
	}

	@Override
	public boolean canGoToNextActivity(final WfWorkflow wfWorkflow) {
		final WfActivity currentActivity = workflowStorePlugin.readActivity(wfWorkflow.getWfaId2());
		final WfActivityDefinition currentActivityDefinition = workflowStorePlugin
				.readActivityDefinition(currentActivity.getWfadId());

		Optional<WfDecision> wfDecision;
		if (currentActivityDefinition.wfMultiplicityDefinition().getEnumValue() == WfMultiplicityDefinitionEnum.SIN) {
			wfDecision = getDecision(currentActivity);
			if (!wfDecision.isPresent()) {
				return false;
			}
		}

		return canGoToNextActivity(wfWorkflow, currentActivity);
	}

	private boolean canGoToNextActivity(final WfWorkflow wfWorkflow, final WfActivity currentActivity) {
		final WfActivityDefinition currentActivityDefinition = workflowStorePlugin
				.readActivityDefinition(currentActivity.getWfadId());

		boolean canGoToNextActivity = false;

		if (currentActivityDefinition.wfMultiplicityDefinition().getEnumValue() == WfMultiplicityDefinitionEnum.MUL) {
			final List<WfDecision> wfDecisions = workflowStorePlugin.findAllDecisionByActivity(currentActivity);
			final DtObject obj = itemStorePlugin.readItem(wfWorkflow.getItemId());
			final RuleConstants ruleConstants = ruleServices.getConstants(wfWorkflow.getWfwdId());
			final RuleContext ruleContext = new RuleContext(obj, ruleConstants);
			final List<Account> accounts = ruleServices.selectAccounts(currentActivity.getWfadId(), ruleContext);

			final int match = (int) accounts.stream()
					.filter(filterAccountEqualsDecisionUsername(wfDecisions))
					.count();
			if (match == accounts.size()) {
				canGoToNextActivity = true;
			}

		} else {
			canGoToNextActivity = true;
		}

		return canGoToNextActivity;
	}

	private static Predicate<Account> filterAccountEqualsDecisionUsername(final List<WfDecision> wfDecisions) {
		return account -> wfDecisions.stream().anyMatch(decision -> account.getId().equals(decision.getUsername()));
	}

	@Override
	public void goToNextActivity(final WfWorkflow wfWorkflow, final String transitionName) {
		final WfActivity currentActivity = workflowStorePlugin.readActivity(wfWorkflow.getWfaId2());

		final boolean canGoToNext = canGoToNextActivity(wfWorkflow);
		if (!canGoToNext) {
			throw new IllegalStateException("Can't go to the next activity");
		}

		goToNextActivity(wfWorkflow, currentActivity, transitionName);
	}

	private void goToNextActivity(final WfWorkflow wfWorkflow, final WfActivity currentActivity, final String transitionName) {
		WfActivity nextActivity;
		if (workflowStorePlugin.hasNextActivity(currentActivity, transitionName)) {
			final WfActivityDefinition nextActivityDefinition = workflowStorePlugin
					.findNextActivity(currentActivity.getWfadId(), transitionName);

			final Optional<WfActivity> nextActivityOpt = workflowStorePlugin.findActivityByDefinitionWorkflow(wfWorkflow,
					nextActivityDefinition);
			if (!nextActivityOpt.isPresent()) {
				nextActivity = new WfActivity();
			} else {
				nextActivity = nextActivityOpt.get();
			}
			// Creating the next activity to validate.
			nextActivity.setCreationDate(new Date());
			nextActivity.setWfadId(nextActivityDefinition.getWfadId());
			nextActivity.setWfwId(wfWorkflow.getWfwId());
			if (nextActivity.getWfaId() == null) {
				workflowStorePlugin.createActivity(nextActivity);
			} else {
				workflowStorePlugin.updateActivity(nextActivity);
			}

			wfWorkflow.setWfaId2(nextActivity.getWfaId());
			workflowStorePlugin.updateWorkflowInstance(wfWorkflow);

			// Autovalidating next activities
			final boolean endReached = autoValidateNextActivities(wfWorkflow, nextActivity,
					nextActivityDefinition.getWfadId(), transitionName);

			if (endReached) {
				endInstance(wfWorkflow);
			}

		} else {
			endInstance(wfWorkflow);
		}
	}

	@Override
	public void saveDecisionAndGoToNextActivity(final WfWorkflow wfWorkflow, final String transitionName, final WfDecision wfDecision) {
		Assertion.checkState(wfWorkflow.wfStatus().getEnumValue() == WfStatusEnum.STA, "A workflow must be started before saving decision");
		// ---
		final WfActivity currentActivity = workflowStorePlugin.readActivity(wfWorkflow.getWfaId2());

		// Updating the decision
		saveDecision(wfWorkflow, wfDecision);

		final boolean canGoToNextActivity = canGoToNextActivity(wfWorkflow, currentActivity);

		if (canGoToNextActivity) {
			goToNextActivity(wfWorkflow, currentActivity, transitionName);
		}
	}

	/**
	 * Find the workflow by itemId
	 *
	 * @param wfwdId
	 * @param itemId
	 * @return the matching workflow
	 */
	@Override
	public WfWorkflow getWorkflowInstanceByItemId(final Long wfwdId, final Long itemId) {
		return workflowStorePlugin.readWorkflowInstanceByItemId(wfwdId, itemId);
	}

	/**
	 * Find the workflowDefinition by name
	 *
	 * @param definitionName
	 * @return the matching workflow
	 */
	@Override
	public WfWorkflowDefinition getWorkflowDefinitionByName(final String definitionName) {
		return workflowStorePlugin.readWorkflowDefinition(definitionName);
	}

	/**
	 * Find activities matching the criteria in parameters
	 *
	 * @param criteria
	 * @return the mathcinf activity definitions
	 */
	public List<WfActivityDefinition> findActivitiesByCriteria(final RuleCriteria criteria) {
		final WfWorkflowDefinition workflow = new WfWorkflowDefinition();
		workflow.setWfwdId(criteria.getWfwdId());

		final List<WfActivityDefinition> activities = getAllDefaultActivities(workflow);
		final Map<Long, WfActivityDefinition> mapAct = activities.stream()
				.collect(Collectors.toMap(WfActivityDefinition::getWfadId, Function.identity()));

		final List<Long> matchingActivities = ruleServices.findItemsByCriteria(criteria, new ArrayList<>(mapAct.keySet()));

		return matchingActivities.stream().map(mapAct::get).collect(Collectors.toList());
	}

	// Definition
	@Override
	public void createWorkflowDefinition(final WfWorkflowDefinition wfWorkflowDefinition) {
		workflowStorePlugin.createWorkflowDefinition(wfWorkflowDefinition);
	}

	@Override
	public void addActivity(final WfWorkflowDefinition wfWorkflowDefinition,
			final WfActivityDefinition wfActivityDefinitionToAdd, final int position) {

		final Optional<WfActivityDefinition> wfActivityDefinition = workflowStorePlugin
				.findActivityDefinitionByPosition(wfWorkflowDefinition, position);

		wfActivityDefinitionToAdd.setLevel(position);

		if (!wfActivityDefinition.isPresent()) {
			// Inserting a activity in trail
			final int size = workflowStorePlugin.countDefaultTransitions(wfWorkflowDefinition);
			Assertion.checkState(size == Math.max(0, position - 2), "Position is not valid");

			wfActivityDefinitionToAdd.setLevel(position);

			workflowStorePlugin.createActivityDefinition(wfWorkflowDefinition, wfActivityDefinitionToAdd);

			// Find the previous activity to add a link to the newly created
			if (position == 2) {
				final WfTransitionDefinition wfTransitionDefinition = new WfTransitionBuilder(
						wfWorkflowDefinition.getWfwdId(), wfWorkflowDefinition.getWfadId(),
						wfActivityDefinitionToAdd.getWfadId()).build();
				final WfTransitionDefinition wfTransitionDefinitionBack = new WfTransitionBuilder(
						wfWorkflowDefinition.getWfwdId(), wfActivityDefinitionToAdd.getWfadId(),
						wfWorkflowDefinition.getWfadId()).withName(TRANSITION_BACK_NAME).build();
				workflowStorePlugin.addTransition(wfTransitionDefinitionBack);
				workflowStorePlugin.addTransition(wfTransitionDefinition);
			} else if (position > 2) {
				final WfActivityDefinition wfActivityDefinitionPrevious = workflowStorePlugin
						.findActivityDefinitionByPosition(wfWorkflowDefinition, position - 1)
						.orElseThrow(() -> new IllegalArgumentException("No ActivityDefiniyion found for " + wfWorkflowDefinition.getName() + "at Postion : " + (position - 1)));
				final WfTransitionDefinition wfTransitionDefinition = new WfTransitionBuilder(
						wfWorkflowDefinition.getWfwdId(), wfActivityDefinitionPrevious.getWfadId(),
						wfActivityDefinitionToAdd.getWfadId()).build();
				final WfTransitionDefinition wfTransitionDefinitionBack = new WfTransitionBuilder(
						wfWorkflowDefinition.getWfwdId(), wfActivityDefinitionToAdd.getWfadId(),
						wfActivityDefinitionPrevious.getWfadId()).withName(TRANSITION_BACK_NAME).build();
				workflowStorePlugin.addTransition(wfTransitionDefinitionBack);
				workflowStorePlugin.addTransition(wfTransitionDefinition);
			} else {
				// Saving starting activity
				wfWorkflowDefinition.setWfadId(wfActivityDefinitionToAdd.getWfadId());
				workflowStorePlugin.updateWorkflowDefinition(wfWorkflowDefinition);
			}

		} else {
			workflowStorePlugin.incrementActivityDefinitionPositionsAfter(wfWorkflowDefinition.getWfwdId(), position);

			// Inserting an activity inside the default activities "linked list"
			workflowStorePlugin.createActivityDefinition(wfWorkflowDefinition, wfActivityDefinitionToAdd);
			if (position > 1) {
				// Automatically move the next activity after the newly created
				insertActivityBefore(wfWorkflowDefinition, wfActivityDefinitionToAdd, wfActivityDefinition.get());
			} else {
				// position == 1
				final WfTransitionDefinition wfTransitionDefinition = new WfTransitionBuilder(
						wfWorkflowDefinition.getWfwdId(), wfActivityDefinitionToAdd.getWfadId(),
						wfActivityDefinition.get().getWfadId()).build();
				workflowStorePlugin.addTransition(wfTransitionDefinition);
				wfWorkflowDefinition.setWfadId(wfActivityDefinitionToAdd.getWfadId());
				workflowStorePlugin.updateWorkflowDefinition(wfWorkflowDefinition);
			}
		}

	}

	private void insertActivityBefore(final WfWorkflowDefinition wfWorkflowDefinition, final WfActivityDefinition wfActivityToAdd,
			final WfActivityDefinition wfActivityReferential) {
		final WfTransitionCriteria wfTransitionCriteria = new WfTransitionCriteria();
		wfTransitionCriteria.setTransitionName(WfCodeTransition.DEFAULT.getTransitionName());
		wfTransitionCriteria.setWfadIdTo(wfActivityReferential.getWfadId());

		final WfTransitionDefinition transition = workflowStorePlugin.findTransition(wfTransitionCriteria)
				.orElseThrow(() -> new IllegalArgumentException("No transition found for " + wfTransitionCriteria.getTransitionName()));
		transition.setWfadIdTo(wfActivityToAdd.getWfadId());

		workflowStorePlugin.updateTransition(transition);

		final WfTransitionDefinition wfTransitionDefinition = new WfTransitionBuilder(wfWorkflowDefinition.getWfwdId(),
				wfActivityToAdd.getWfadId(), wfActivityReferential.getWfadId()).build();
		workflowStorePlugin.addTransition(wfTransitionDefinition);
	}

	@Override
	public void addRule(final WfActivityDefinition wfActivity, final RuleDefinition ruleDefinition,
			final List<RuleConditionDefinition> conditions) {
		Assertion.checkNotNull(wfActivity);
		Assertion.checkNotNull(ruleDefinition);
		Assertion.checkNotNull(conditions);
		// --
		ruleDefinition.setItemId(wfActivity.getWfadId());
		ruleServices.addRule(ruleDefinition);

		for (final RuleConditionDefinition ruleConditionDefinition : conditions) {
			ruleConditionDefinition.setRudId(ruleDefinition.getId());
			ruleServices.addCondition(ruleConditionDefinition);
		}
	}

	@Override
	public void addSelector(final WfActivityDefinition wfActivity, final SelectorDefinition selector,
			final List<RuleFilterDefinition> filters) {
		Assertion.checkNotNull(wfActivity);
		Assertion.checkNotNull(selector);
		Assertion.checkNotNull(filters);
		// --
		selector.setItemId(wfActivity.getWfadId());
		ruleServices.addSelector(selector);

		for (final RuleFilterDefinition ruleFilterDefinition : filters) {
			ruleFilterDefinition.setSelId(selector.getId());
			ruleServices.addFilter(ruleFilterDefinition);
		}
	}

	private List<WfActivityDefinition> getAllDefaultActivities(final WfWorkflowDefinition wfWorkflowDefinition) {
		return workflowStorePlugin.findAllDefaultActivityDefinitions(wfWorkflowDefinition);
	}

	@Override
	public WfActivity getActivity(final Long wfaId) {
		return workflowStorePlugin.readActivity(wfaId);
	}

	private Map<Long, List<RuleDefinition>> constructDicRulesForWorkflowDefinition(final long wfwdId) {
		final List<RuleDefinition> rules = workflowStorePlugin.findAllRulesByWorkflowDefinitionId(wfwdId);
		// Build a dictionary from the rules: WfadId => List<RuleDefinition>
		return rules.stream()
				.collect(Collectors.groupingBy(RuleDefinition::getItemId));
	}

	private Map<Long, List<RuleConditionDefinition>> constructDicConditionsForWorkflowDefinition(final long wfwdId) {
		final List<RuleConditionDefinition> conditions = workflowStorePlugin.findAllConditionsByWorkflowDefinitionId(wfwdId);
		// Build a dictionary from the conditions: RudId => List<RuleConditionDefinition>
		return conditions.stream().collect(Collectors.groupingBy(RuleConditionDefinition::getRudId));
	}

	private Map<Long, List<SelectorDefinition>> constructDicSelectorsForWorkflowDefinition(final long wfwdId) {
		final List<SelectorDefinition> selectors = workflowStorePlugin.findAllSelectorsByWorkflowDefinitionId(wfwdId);
		// Build a dictionary from the selectors: WfadId => List<SelectorDefinition>
		return selectors.stream()
				.collect(Collectors.groupingBy(SelectorDefinition::getItemId));
	}

	private Map<Long, List<RuleFilterDefinition>> constructDicFiltersForWorkflowDefinition(final long wfwdId) {
		final List<RuleFilterDefinition> filters = workflowStorePlugin.findAllFiltersByWorkflowDefinitionId(wfwdId);
		// Build a dictionary from the filters: SelId => List<RuleFilterDefinition>
		return filters.stream()
				.collect(Collectors.groupingBy(RuleFilterDefinition::getSelId));
	}

	@Override
	public List<WfWorkflowDecision> getWorkflowDecision(final long wfwId) {
		// Get the workflow from id
		final WfWorkflow wfWorkflow = workflowStorePlugin.readWorkflowInstanceById(wfwId);

		final long wfwdId = wfWorkflow.getWfwdId();
		// Get the definition
		final WfWorkflowDefinition wfDefinition = workflowStorePlugin.readWorkflowDefinition(wfwdId);

		// Get all the activity definitions for the workflow definition
		final List<WfActivityDefinition> activityDefinitions = workflowStorePlugin
				.findAllDefaultActivityDefinitions(wfDefinition);

		// Build a map : WfadId => WfActivity
		final List<WfActivity> activities = workflowStorePlugin.findActivitiesByWorkflowId(wfWorkflow);
		final Map<Long, WfActivity> dicActivities = activities.stream()
				.collect(Collectors.toMap(WfActivity::getWfadId, Function.identity()));

		// Get all decisions for the workflow instance
		final List<WfDecision> allDecisions = workflowStorePlugin.findDecisionsByWorkflowId(wfWorkflow);
		// Build a dictionary from the decisions: WfaId => List<WfDecision>
		final Map<Long, List<WfDecision>> dicDecision = allDecisions.stream()
				.collect(Collectors.groupingBy(WfDecision::getWfaId));

		final Map<Long, List<RuleDefinition>> dicRules = constructDicRulesForWorkflowDefinition(wfwdId);
		final Map<Long, List<RuleConditionDefinition>> dicConditions = constructDicConditionsForWorkflowDefinition(wfwdId);
		final Map<Long, List<SelectorDefinition>> dicSelectors = constructDicSelectorsForWorkflowDefinition(wfwdId);
		final Map<Long, List<RuleFilterDefinition>> dicFilters = constructDicFiltersForWorkflowDefinition(wfwdId);

		// Fetch the object linked to the workflow instance.
		final DtObject obj = itemStorePlugin.readItem(wfWorkflow.getItemId());

		final RuleConstants ruleConstants = ruleServices.getConstants(wfwdId);
		final RuleContext ruleContext = new RuleContext(obj, ruleConstants);

		final List<WfWorkflowDecision> workflowDecisions = new ArrayList<>();

		for (final WfActivityDefinition activityDefinition : activityDefinitions) {
			final long actDefId = activityDefinition.getWfadId();
			final boolean ruleValid = ruleServices.isRuleValid(actDefId, ruleContext, dicRules, dicConditions);

			if (ruleValid) {
				final List<AccountGroup> groups = ruleServices.selectGroups(actDefId, ruleContext, dicSelectors,
						dicFilters);

				final WfWorkflowDecision wfWorkflowDecision = new WfWorkflowDecision();
				wfWorkflowDecision.setActivityDefinition(activityDefinition);
				final WfActivity wfActivity = dicActivities.get(activityDefinition.getWfadId());
				wfWorkflowDecision.setActivity(wfActivity);
				wfWorkflowDecision.setGroups(groups);
				List<WfDecision> decisions;
				if (wfActivity != null) {
					decisions = dicDecision.get(wfActivity.getWfaId());
					wfWorkflowDecision.setDecisions(decisions);
				}
				workflowDecisions.add(wfWorkflowDecision);
			}
		}

		return workflowDecisions;
	}

}
