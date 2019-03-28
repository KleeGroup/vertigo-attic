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
package io.vertigo.workflow.services;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertigo.account.account.Account;
import io.vertigo.account.account.AccountGroup;
import io.vertigo.app.AutoCloseableApp;
import io.vertigo.core.component.di.injector.DIInjector;
import io.vertigo.dynamo.domain.model.URI;
import io.vertigo.dynamo.domain.util.DtObjectUtil;
import io.vertigo.impl.workflow.ItemStorePlugin;
import io.vertigo.rules.domain.RuleConditionDefinition;
import io.vertigo.rules.domain.RuleDefinition;
import io.vertigo.rules.domain.RuleFilterDefinition;
import io.vertigo.rules.domain.SelectorDefinition;
import io.vertigo.workflow.MyAppConfig;
import io.vertigo.workflow.WfActivityDefinitionBuilder;
import io.vertigo.workflow.WfCodeTransition;
import io.vertigo.workflow.WfWorkflowDecision;
import io.vertigo.workflow.WfWorkflowDefinitionBuilder;
import io.vertigo.workflow.WorkflowManager;
import io.vertigo.workflow.data.MockIdentities;
import io.vertigo.workflow.data.MyDummyDtObject;
import io.vertigo.workflow.domain.instance.WfActivity;
import io.vertigo.workflow.domain.instance.WfDecision;
import io.vertigo.workflow.domain.instance.WfStatusEnum;
import io.vertigo.workflow.domain.instance.WfWorkflow;
import io.vertigo.workflow.domain.model.WfActivityDefinition;
import io.vertigo.workflow.domain.model.WfWorkflowDefinition;

/**
 * Tests unitaires pour le Workflow Manager
 *
 * @author xdurand
 *
 */
public class WorkflowManagerTest extends DbTest {

	private AutoCloseableApp app;

	@Inject
	private WorkflowManager workflowManager;

	@Inject
	private MockIdentities mockIdentities;

	@Inject
	private ItemStorePlugin itemStorePlugin;

	/**
	 * @throws Exception
	 *
	 */
	@Before
	public void setUp() throws Exception {
		app = new AutoCloseableApp(MyAppConfig.config());
		DIInjector.injectMembers(this, app.getComponentSpace());
		doSetUp();
	}

	/**
	 * @throws Exception
	 *
	 */
	@After
	public void tearDown() throws Exception {
		if (app != null) {
			app.close();
		}
		doTearDown();
	}

	private MyDummyDtObject createDummyDtObject(final long itemId) {
		final MyDummyDtObject myDummyDtObject = new MyDummyDtObject();
		myDummyDtObject.setId(itemId);
		myDummyDtObject.setDivision("DIV");
		myDummyDtObject.setEntity("ENT");
		itemStorePlugin.addItem(myDummyDtObject.getId(), myDummyDtObject);
		return myDummyDtObject;
	}

	/**
	 *
	 */
	@Test
	public void testWorkflowStateChanges() {
		final WfWorkflowDefinition wfWorkflowDefinition = new WfWorkflowDefinitionBuilder("WorkflowRules").build();
		workflowManager.createWorkflowDefinition(wfWorkflowDefinition);

		final WfActivityDefinition firstActivity = new WfActivityDefinitionBuilder("Step 1",
				wfWorkflowDefinition.getWfwdId()).build();

		final AccountGroup accountGroup = new AccountGroup("1", "dummy group");
		final Account account = Account.builder("Acc1").build();
		mockIdentities.saveGroup(accountGroup);
		mockIdentities.saveAccounts(Arrays.asList(account));
		final URI<Account> accountUri = DtObjectUtil.createURI(Account.class, account.getId());
		final URI<AccountGroup> accountGroupUri = DtObjectUtil.createURI(AccountGroup.class, accountGroup.getId());
		mockIdentities.attach(accountUri, accountGroupUri);

		// Step 1 : 1 rule, 1 condition
		workflowManager.addActivity(wfWorkflowDefinition, firstActivity, 1);
		final RuleDefinition rule1Act1 = new RuleDefinition();
		rule1Act1.setItemId(firstActivity.getWfadId());

		final RuleConditionDefinition condition1Rule1Act1 = new RuleConditionDefinition();
		condition1Rule1Act1.setField("DIVISION");
		condition1Rule1Act1.setOperator("=");
		condition1Rule1Act1.setExpression("DIV");

		workflowManager.addRule(firstActivity, rule1Act1, Arrays.asList(condition1Rule1Act1));
		// Selector/filter to validate the activity (preventing auto
		// validation
		// when no one is linked to an activity)

		final SelectorDefinition selector1 = new SelectorDefinition();
		selector1.setItemId(firstActivity.getWfadId());
		selector1.setGroupId(accountGroup.getId());

		workflowManager.addSelector(firstActivity, selector1, Collections.emptyList());

		final MyDummyDtObject myDummyDtObject = createDummyDtObject(1);

		final WfWorkflow wfWorkflow = workflowManager.createWorkflowInstance("WorkflowRules", "JUnit", false,
				myDummyDtObject.getId());

		assertThat(wfWorkflow, is(not(nullValue())));
		assertThat(wfWorkflow.wfStatus().getEnumValue(), is(WfStatusEnum.CRE));

		try {
			workflowManager.resumeInstance(wfWorkflow);
			fail("Cannot resume an instance that is not started");
		} catch (final IllegalStateException iae) {
			// We should enter in this exeption case
		}

		try {
			workflowManager.endInstance(wfWorkflow);
			fail("Cannot end instance that is not started");
		} catch (final IllegalStateException iae) {
			// We should enter in this exeption case
		}

		// Starting the workflow
		workflowManager.startInstance(wfWorkflow);
		assertThat(wfWorkflow.wfStatus().getEnumValue(), is(WfStatusEnum.STA));

		try {
			workflowManager.resumeInstance(wfWorkflow);
			fail("Cannot resume an instance that is not paused");
		} catch (final IllegalStateException iae) {
			// We should enter in this exeption case
		}

		// Pausing the workflow
		workflowManager.pauseInstance(wfWorkflow);
		assertThat(wfWorkflow.wfStatus().getEnumValue(), is(WfStatusEnum.PAU));

		final WfDecision wfDecision = new WfDecision();
		wfDecision.setChoice(1);
		wfDecision.setUsername("junit");
		try {
			workflowManager.saveDecisionAndGoToNextActivity(wfWorkflow, WfCodeTransition.DEFAULT.getTransitionName(), wfDecision);
			fail("Cannot go to next activity while the workflow is paused");
		} catch (final IllegalStateException iae) {
			// We should enter in this exeption case
		}

		try {
			workflowManager.startInstance(wfWorkflow);
			fail("Cannot start an already started workflow");
		} catch (final IllegalStateException iae) {
			// We should enter in this exeption case
		}

		// A workflow in pause can be resumed
		workflowManager.resumeInstance(wfWorkflow);
		assertThat(wfWorkflow.wfStatus().getEnumValue(), is(WfStatusEnum.STA));

		// A workflow started can be ended
		workflowManager.endInstance(wfWorkflow);

		final WfWorkflow wfWorkflow2 = workflowManager.createWorkflowInstance("WorkflowRules", "JUnit", false,
				myDummyDtObject.getId());

		assertThat(wfWorkflow2, is(not(nullValue())));
		assertThat(wfWorkflow2.wfStatus().getEnumValue(), is(WfStatusEnum.CRE));

		// A workflow created can be started.
		workflowManager.startInstance(wfWorkflow2);
		assertThat(wfWorkflow2.wfStatus().getEnumValue(), is(WfStatusEnum.STA));

		// A workflow started can be paused.
		workflowManager.pauseInstance(wfWorkflow2);
		assertThat(wfWorkflow2.wfStatus().getEnumValue(), is(WfStatusEnum.PAU));

		// A workflow paused can be ended.
		workflowManager.endInstance(wfWorkflow2);
		assertThat(wfWorkflow2.wfStatus().getEnumValue(), is(WfStatusEnum.END));

	}

	private static void assertHasOneDecision(final WfWorkflowDecision wfWorkflowDecision) {
		assertNotNull(wfWorkflowDecision.getDecisions());
		assertThat(wfWorkflowDecision.getDecisions().size(), is(1));
	}

	private static void assertActivityExist(final WfActivityDefinition activityDefinition, final WfWorkflowDecision wfWorkflowDecision) {
		assertThat(activityDefinition.getWfadId(), is(wfWorkflowDecision.getActivityDefinition().getWfadId()));
		assertNotNull(wfWorkflowDecision.getActivity());
		assertNotNull(wfWorkflowDecision.getActivity().getWfaId());
		assertThat(activityDefinition.getWfadId(), is(wfWorkflowDecision.getActivity().getWfadId()));
	}

	private static void assertFirstDecisionEquals(final WfDecision wfDecisionAct, final WfWorkflowDecision wfWorkflowDecision) {
		assertThat(wfDecisionAct.getWfaId(), is(wfWorkflowDecision.getDecisions().get(0).getWfaId()));
		assertThat(wfDecisionAct.getChoice(), is(wfWorkflowDecision.getDecisions().get(0).getChoice()));
		assertThat(wfDecisionAct.getComments(), is(wfWorkflowDecision.getDecisions().get(0).getComments()));
		assertThat(wfDecisionAct.getDecisionDate(), is(wfWorkflowDecision.getDecisions().get(0).getDecisionDate()));
	}

	private static void assertHasOneGroup(final AccountGroup accountGroup, final WfWorkflowDecision wfWorkflowDecision) {
		assertNotNull(wfWorkflowDecision.getGroups());
		assertThat(1, is(wfWorkflowDecision.getGroups().size()));
		assertThat(accountGroup.getId(), is(wfWorkflowDecision.getGroups().get(0).getId()));
	}

	/**
	 *
	 */
	@Test
	public void testWorkflowRulesManualValidationActivities() {

		final WfWorkflowDefinition wfWorkflowDefinition = new WfWorkflowDefinitionBuilder("WorkflowRules").build();
		workflowManager.createWorkflowDefinition(wfWorkflowDefinition);

		final WfActivityDefinition firstActivity = new WfActivityDefinitionBuilder("Step 1", wfWorkflowDefinition.getWfwdId())
				.build();

		final AccountGroup accountGroup = new AccountGroup("1", "dummy group");
		final Account account = Account.builder("Acc1").build();
		mockIdentities.saveGroup(accountGroup);
		mockIdentities.saveAccounts(Arrays.asList(account));
		final URI<Account> accountUri = DtObjectUtil.createURI(Account.class, account.getId());
		final URI<AccountGroup> accountGroupUri = DtObjectUtil.createURI(AccountGroup.class, accountGroup.getId());
		mockIdentities.attach(accountUri, accountGroupUri);

		// Step 1 : 1 rule, 1 condition
		workflowManager.addActivity(wfWorkflowDefinition, firstActivity, 1);
		final RuleDefinition rule1Act1 = new RuleDefinition();
		rule1Act1.setItemId(firstActivity.getWfadId());

		final RuleConditionDefinition condition1Rule1Act1 = new RuleConditionDefinition();
		condition1Rule1Act1.setField("ENTITY");
		condition1Rule1Act1.setOperator("IN");
		condition1Rule1Act1.setExpression("ENT,FED,GFE");

		workflowManager.addRule(firstActivity, rule1Act1, Arrays.asList(condition1Rule1Act1));
		// Selector/filter to validate the activity (preventing auto
		// validation
		// when no one is linked to an activity)

		final SelectorDefinition selector1 = new SelectorDefinition();
		selector1.setItemId(firstActivity.getWfadId());
		selector1.setGroupId(accountGroup.getId());

		final RuleFilterDefinition filter1 = new RuleFilterDefinition();
		filter1.setField("ENTITY");
		filter1.setOperator("=");
		filter1.setExpression("ENT");

		workflowManager.addSelector(firstActivity, selector1, Arrays.asList(filter1));

		// Step 2 : No rules/condition
		final WfActivityDefinition secondActivity = new WfActivityDefinitionBuilder("Step 2",
				wfWorkflowDefinition.getWfwdId()).build();
		workflowManager.addActivity(wfWorkflowDefinition, secondActivity, 2);
		// Selector/filter to validate the activity (preventing auto
		// validation
		// when no one is linked to an activity)

		final SelectorDefinition selector2 = new SelectorDefinition();
		selector2.setItemId(secondActivity.getWfadId());
		selector2.setGroupId(accountGroup.getId());

		workflowManager.addSelector(secondActivity, selector2, new ArrayList<RuleFilterDefinition>());

		// Step 3 : 1 rule, 2 conditions
		final WfActivityDefinition thirdActivity = new WfActivityDefinitionBuilder("Step 3", wfWorkflowDefinition.getWfwdId())
				.build();
		workflowManager.addActivity(wfWorkflowDefinition, thirdActivity, 3);
		final RuleDefinition rule1Act3 = new RuleDefinition();
		rule1Act3.setItemId(thirdActivity.getWfadId());

		final RuleConditionDefinition condition1Rule1Act3 = new RuleConditionDefinition();
		condition1Rule1Act3.setField("ENTITY");
		condition1Rule1Act3.setOperator("=");
		condition1Rule1Act3.setExpression("ENT");
		final RuleConditionDefinition condition2Rule1Act3 = new RuleConditionDefinition();
		condition2Rule1Act3.setField("ENTITY");
		condition2Rule1Act3.setOperator("=");
		condition2Rule1Act3.setExpression("ENT");

		workflowManager.addRule(thirdActivity, rule1Act3, Arrays.asList(condition1Rule1Act3, condition2Rule1Act3));
		// Selector/filter to validate the activity (preventing auto
		// validation
		// when no one is linked to an activity)

		final SelectorDefinition selector3 = new SelectorDefinition();
		selector3.setItemId(thirdActivity.getWfadId());
		selector3.setGroupId(accountGroup.getId());

		final RuleFilterDefinition filter3 = new RuleFilterDefinition();
		filter3.setField("ENTITY");
		filter3.setOperator("=");
		filter3.setExpression("ENT");

		workflowManager.addSelector(thirdActivity, selector3, Arrays.asList(filter3));

		// Step 4 : 2 rules, 1 condition
		final WfActivityDefinition fourthActivity = new WfActivityDefinitionBuilder("Step 4",
				wfWorkflowDefinition.getWfwdId()).build();
		workflowManager.addActivity(wfWorkflowDefinition, fourthActivity, 4);

		final RuleDefinition rule1Act4 = new RuleDefinition();
		rule1Act4.setItemId(fourthActivity.getWfadId());
		final RuleConditionDefinition condition1Rule1Act4 = new RuleConditionDefinition();
		condition1Rule1Act4.setField("ENTITY");
		condition1Rule1Act4.setOperator("=");
		condition1Rule1Act4.setExpression("ENT");
		final RuleConditionDefinition condition1Rule2Act4 = new RuleConditionDefinition();
		condition1Rule2Act4.setField("ENTITY");
		condition1Rule2Act4.setOperator("=");
		condition1Rule2Act4.setExpression("ENT");

		final RuleDefinition rule2Act4 = new RuleDefinition();
		rule2Act4.setItemId(fourthActivity.getWfadId());

		workflowManager.addRule(fourthActivity, rule1Act4, Arrays.asList(condition1Rule1Act4));
		workflowManager.addRule(fourthActivity, rule2Act4, Arrays.asList(condition1Rule2Act4));
		// Selector/filter to validate the activity (preventing auto
		// validation
		// when no one is linked to an activity)

		final SelectorDefinition selector41 = new SelectorDefinition();
		selector41.setItemId(fourthActivity.getWfadId());
		selector41.setGroupId(accountGroup.getId());

		final RuleFilterDefinition filter4 = new RuleFilterDefinition();
		filter4.setField("ENTITY");
		filter4.setOperator("=");
		filter4.setExpression("ENT");

		workflowManager.addSelector(fourthActivity, selector41, Arrays.asList(filter4));

		final MyDummyDtObject myDummyDtObject = createDummyDtObject(1);

		final WfWorkflow wfWorkflow = workflowManager.createWorkflowInstance(wfWorkflowDefinition.getWfwdId(), "JUnit", false,
				myDummyDtObject.getId());

		// Starting the workflow
		workflowManager.startInstance(wfWorkflow);

		List<WfWorkflowDecision> workflowDecisions = workflowManager.getWorkflowDecision(wfWorkflow.getWfwId());

		// Step 1,3,4 should be Manual, Step 2 should be auto
		// No decisons for now
		assertNotNull(workflowDecisions);
		assertThat(workflowDecisions.size(), is(3));
		// Check Step 1
		assertActivityExist(firstActivity, workflowDecisions.get(0));
		assertNull(workflowDecisions.get(0).getDecisions());
		assertHasOneGroup(accountGroup, workflowDecisions.get(0));
		// Check Step 3
		assertThat(thirdActivity.getWfadId(), is(workflowDecisions.get(1).getActivityDefinition().getWfadId()));
		assertNull(workflowDecisions.get(1).getActivity());
		assertNull(workflowDecisions.get(1).getDecisions());
		assertHasOneGroup(accountGroup, workflowDecisions.get(1));
		// Check Step 4
		assertThat(fourthActivity.getWfadId(), is(workflowDecisions.get(2).getActivityDefinition().getWfadId()));
		assertNull(workflowDecisions.get(2).getActivity());
		assertNull(workflowDecisions.get(2).getDecisions());
		assertHasOneGroup(accountGroup, workflowDecisions.get(2));

		// Entry actions should NOT validate all activities.
		long currentActivityId = wfWorkflow.getWfaId2();
		WfActivity currentActivity = workflowManager.getActivity(currentActivityId);
		assertThat(currentActivity.getWfadId(), is(firstActivity.getWfadId()));

		final WfWorkflow wfWorkflowFetched = workflowManager.getWorkflowInstance(wfWorkflow.getWfwId());
		assertNotNull(wfWorkflowFetched);

		currentActivityId = wfWorkflow.getWfaId2();
		currentActivity = workflowManager.getActivity(currentActivityId);
		assertThat(currentActivity.getWfadId(), is(firstActivity.getWfadId()));

		final WfDecision decision = new WfDecision();
		decision.setChoice(1);
		decision.setComments("abc");
		decision.setUsername("AA");
		decision.setDecisionDate(new Date());

		workflowManager.saveDecisionAndGoToNextActivity(wfWorkflow, WfCodeTransition.DEFAULT.getTransitionName(), decision);

		workflowDecisions = workflowManager.getWorkflowDecision(wfWorkflow.getWfwId());

		// Step 1,3,4 should be Manual, Step 2 should be auto
		// 1 Decisions for Step 1
		assertNotNull(workflowDecisions);
		assertThat(workflowDecisions.size(), is(3));
		// Check Step 1
		assertActivityExist(firstActivity, workflowDecisions.get(0));
		// 1 Decision
		assertHasOneDecision(workflowDecisions.get(0));
		assertFirstDecisionEquals(decision, workflowDecisions.get(0));
		assertHasOneGroup(accountGroup, workflowDecisions.get(0));
		// Check Step 3
		assertActivityExist(thirdActivity, workflowDecisions.get(1));
		assertNull(workflowDecisions.get(1).getDecisions());
		assertHasOneGroup(accountGroup, workflowDecisions.get(1));
		// Check Step 4
		assertThat(fourthActivity.getWfadId(), is(workflowDecisions.get(2).getActivityDefinition().getWfadId()));
		assertNull(workflowDecisions.get(2).getActivity());
		assertNull(workflowDecisions.get(2).getDecisions());
		assertHasOneGroup(accountGroup, workflowDecisions.get(2));

		// Activity 1 should now be validated.
		// No rule defined for activity 2. Activity 2 should be
		// autovalidated
		// The current activity should be now activity 3
		currentActivityId = wfWorkflow.getWfaId2();
		currentActivity = workflowManager.getActivity(currentActivityId);
		assertThat(currentActivity.getWfadId(), is(thirdActivity.getWfadId()));

		final WfWorkflow wfWorkflowFetched2 = workflowManager.getWorkflowInstance(wfWorkflow.getWfwId());
		assertNotNull(wfWorkflowFetched2);

		currentActivityId = wfWorkflow.getWfaId2();
		currentActivity = workflowManager.getActivity(currentActivityId);
		assertThat(currentActivity.getWfadId(), is(thirdActivity.getWfadId()));

		// Manually validating activity 3
		final WfDecision wfDecisionAct3 = new WfDecision();
		wfDecisionAct3.setChoice(1);
		wfDecisionAct3.setUsername(account.getId());
		wfDecisionAct3.setWfaId(currentActivity.getWfaId());

		// Using CanGo, SaveDecision and GoToNext
		boolean canGo = workflowManager.canGoToNextActivity(wfWorkflow);
		assertFalse(canGo);

		workflowManager.saveDecision(wfWorkflow, wfDecisionAct3);
		canGo = workflowManager.canGoToNextActivity(wfWorkflow);
		assertTrue(canGo);
		workflowManager.canGoToNextActivity(wfWorkflow);
		workflowManager.goToNextActivity(wfWorkflow, WfCodeTransition.DEFAULT.getTransitionName());

		workflowDecisions = workflowManager.getWorkflowDecision(wfWorkflow.getWfwId());

		// Step 1,3,4 should be Manual, Step 2 should be auto
		// Decisions for Step 1, Step 3
		assertNotNull(workflowDecisions);
		assertThat(workflowDecisions.size(), is(3));
		// Check Step 1
		assertActivityExist(firstActivity, workflowDecisions.get(0));
		// 1 Decision
		assertHasOneDecision(workflowDecisions.get(0));
		assertFirstDecisionEquals(decision, workflowDecisions.get(0));
		assertHasOneGroup(accountGroup, workflowDecisions.get(0));
		// Check Step 3
		assertActivityExist(thirdActivity, workflowDecisions.get(1));
		// Decisions for Step 3
		assertHasOneDecision(workflowDecisions.get(1));
		assertFirstDecisionEquals(wfDecisionAct3, workflowDecisions.get(1));
		assertHasOneGroup(accountGroup, workflowDecisions.get(1));
		// Check Step 4
		assertThat(fourthActivity.getWfadId(), is(workflowDecisions.get(2).getActivityDefinition().getWfadId()));
		assertNotNull(workflowDecisions.get(2).getActivity());
		assertNull(workflowDecisions.get(2).getDecisions());
		assertHasOneGroup(accountGroup, workflowDecisions.get(2));

		// Activity 3 should now be validated.
		// The current activity should be now activity 4
		currentActivityId = wfWorkflow.getWfaId2();
		currentActivity = workflowManager.getActivity(currentActivityId);
		assertThat(currentActivity.getWfadId(), is(fourthActivity.getWfadId()));

		final WfWorkflow wfWorkflowFetched3 = workflowManager.getWorkflowInstance(wfWorkflow.getWfwId());
		assertNotNull(wfWorkflowFetched3);

		currentActivityId = wfWorkflow.getWfaId2();
		currentActivity = workflowManager.getActivity(currentActivityId);
		assertThat(currentActivity.getWfadId(), is(fourthActivity.getWfadId()));

		// Manually validating activity 4
		final WfDecision wfDecisionAct4 = new WfDecision();
		wfDecisionAct4.setChoice(1);
		wfDecisionAct4.setUsername(account.getId());
		workflowManager.saveDecisionAndGoToNextActivity(wfWorkflow, WfCodeTransition.DEFAULT.getTransitionName(), wfDecisionAct4);

		workflowDecisions = workflowManager.getWorkflowDecision(wfWorkflow.getWfwId());

		// Step 1,3,4 should be Manual, Step 2 should be auto
		// Decisions for Step 1, Step 3
		assertNotNull(workflowDecisions);
		assertThat(workflowDecisions.size(), is(3));
		// Check Step 1
		assertActivityExist(firstActivity, workflowDecisions.get(0));
		// 1 Decision
		assertHasOneDecision(workflowDecisions.get(0));
		assertFirstDecisionEquals(decision, workflowDecisions.get(0));
		assertHasOneGroup(accountGroup, workflowDecisions.get(0));
		// Check Step 3
		assertActivityExist(thirdActivity, workflowDecisions.get(1));
		// Decisions for Step 3
		assertHasOneDecision(workflowDecisions.get(1));
		assertFirstDecisionEquals(wfDecisionAct3, workflowDecisions.get(1));
		assertHasOneGroup(accountGroup, workflowDecisions.get(1));
		// Check Step 4
		assertActivityExist(fourthActivity, workflowDecisions.get(2));
		// Decisions for Step 4
		assertHasOneDecision(workflowDecisions.get(2));
		assertFirstDecisionEquals(wfDecisionAct4, workflowDecisions.get(2));

		assertNotNull(workflowDecisions.get(2).getGroups());
		assertThat(workflowDecisions.get(2).getGroups().size(), is(1));
		assertThat(accountGroup.getId(), is(workflowDecisions.get(2).getGroups().get(0).getId()));

		// Activity 4 should now be validated. The current activity is now
		// activity 4, with the end status
		currentActivityId = wfWorkflow.getWfaId2();
		currentActivity = workflowManager.getActivity(currentActivityId);
		assertThat(currentActivity.getWfadId(), is(fourthActivity.getWfadId()));

		// Automatic ending.
		assertThat(wfWorkflow.wfStatus().getEnumValue(), is(WfStatusEnum.END));

		final WfWorkflow wfWorkflowFetched5 = workflowManager.getWorkflowInstance(wfWorkflow.getWfwId());
		assertThat(wfWorkflowFetched5.wfStatus().getEnumValue(), is(WfStatusEnum.END));

	}

	/**
	 *
	 */
	@Test
	public void testWorkflowRulesAutoValidationNoSelectorAllActivities() {

		final WfWorkflowDefinition wfWorkflowDefinition = new WfWorkflowDefinitionBuilder("WorkflowRules").build();
		workflowManager.createWorkflowDefinition(wfWorkflowDefinition);

		final WfActivityDefinition firstActivity = new WfActivityDefinitionBuilder("Step 1",
				wfWorkflowDefinition.getWfwdId()).build();

		// Step 1 : 1 rule, 1 condition (NO Selector)
		workflowManager.addActivity(wfWorkflowDefinition, firstActivity, 1);

		final RuleDefinition rule1Act1 = new RuleDefinition();
		rule1Act1.setItemId(firstActivity.getWfadId());

		final RuleConditionDefinition condition1Rule1Act1 = new RuleConditionDefinition();
		condition1Rule1Act1.setField("DIVISION");
		condition1Rule1Act1.setOperator("=");
		condition1Rule1Act1.setExpression("DIV");

		workflowManager.addRule(firstActivity, rule1Act1, Arrays.asList(condition1Rule1Act1));

		// Step 2 : No rules/condition (NO Selector)
		final WfActivityDefinition secondActivity = new WfActivityDefinitionBuilder("Step 2",
				wfWorkflowDefinition.getWfwdId()).build();
		workflowManager.addActivity(wfWorkflowDefinition, secondActivity, 2);

		// Step 3 : 1 rule, 2 conditions (NO Selector)
		final WfActivityDefinition thirdActivity = new WfActivityDefinitionBuilder("Step 3",
				wfWorkflowDefinition.getWfwdId()).build();
		workflowManager.addActivity(wfWorkflowDefinition, thirdActivity, 3);

		final RuleDefinition rule1Act3 = new RuleDefinition();
		rule1Act3.setItemId(thirdActivity.getWfadId());

		final RuleConditionDefinition condition1Rule1Act3 = new RuleConditionDefinition();
		condition1Rule1Act3.setField("DIVISION");
		condition1Rule1Act3.setOperator("=");
		condition1Rule1Act3.setExpression("DIV");

		final RuleConditionDefinition condition2Rule1Act3 = new RuleConditionDefinition();
		condition2Rule1Act3.setField("ENTITY");
		condition2Rule1Act3.setOperator("=");
		condition2Rule1Act3.setExpression("ENT");

		workflowManager.addRule(thirdActivity, rule1Act3, Arrays.asList(condition1Rule1Act3, condition2Rule1Act3));

		// Step 4 : 2 rules, 1 condition (NO Selector)
		final WfActivityDefinition fourthActivity = new WfActivityDefinitionBuilder("Step 4",
				wfWorkflowDefinition.getWfwdId()).build();
		workflowManager.addActivity(wfWorkflowDefinition, fourthActivity, 4);

		final RuleDefinition rule1Act4 = new RuleDefinition();
		rule1Act4.setItemId(fourthActivity.getWfadId());

		final RuleConditionDefinition condition1Rule1Act4 = new RuleConditionDefinition();
		condition1Rule1Act4.setField("DIVISION");
		condition1Rule1Act4.setOperator("=");
		condition1Rule1Act4.setExpression("DIV");

		final RuleDefinition rule2Act4 = new RuleDefinition();
		rule2Act4.setItemId(fourthActivity.getWfadId());

		final RuleConditionDefinition condition1Rule2Act4 = new RuleConditionDefinition();
		condition1Rule2Act4.setField("DIVISION");
		condition1Rule2Act4.setOperator("=");
		condition1Rule2Act4.setExpression("ABC");

		workflowManager.addRule(fourthActivity, rule1Act4, Arrays.asList(condition1Rule1Act4));
		workflowManager.addRule(fourthActivity, rule2Act4, Arrays.asList(condition1Rule2Act4));

		// Creating an object
		final MyDummyDtObject myDummyDtObject = createDummyDtObject(1);

		final WfWorkflow wfWorkflow = workflowManager.createWorkflowInstance("WorkflowRules", "JUnit", false,
				myDummyDtObject.getId());

		// Starting the workflow
		workflowManager.startInstance(wfWorkflow);

		final List<WfWorkflowDecision> workflowDecisions1 = workflowManager.getWorkflowDecision(wfWorkflow.getWfwId());
		assertNotNull(workflowDecisions1);
		assertThat(workflowDecisions1.size(), is(3));

		final WfActivity currentActivity = workflowManager.getActivity(wfWorkflow.getWfaId2());
		assertThat(currentActivity.getWfadId(), is(firstActivity.getWfadId()));

		final WfWorkflow wfWorkflowFetched = workflowManager.getWorkflowInstance(wfWorkflow.getWfwId());
		assertThat(wfWorkflowFetched, is(not(nullValue())));
		final WfActivity currentActivityFetched = workflowManager.getActivity(wfWorkflowFetched.getWfaId2());
		assertThat(currentActivityFetched.getWfadId(), is(firstActivity.getWfadId()));

	}
}
