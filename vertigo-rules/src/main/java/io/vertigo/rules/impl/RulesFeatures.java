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
package io.vertigo.rules.impl;

import io.vertigo.app.config.DefinitionProviderConfig;
import io.vertigo.app.config.Features;
import io.vertigo.core.param.Param;
import io.vertigo.dynamo.plugins.environment.DynamoDefinitionProvider;
import io.vertigo.rules.dao.RuleConditionDefinitionDAO;
import io.vertigo.rules.dao.RuleDefinitionDAO;
import io.vertigo.rules.dao.RuleFilterDefinitionDAO;
import io.vertigo.rules.dao.SelectorDefinitionDAO;
import io.vertigo.rules.domain.DtDefinitions;
import io.vertigo.rules.plugins.sql.SQLRuleStorePlugin;
import io.vertigo.rules.services.RuleServices;

/**
 * Defines the 'workflow' extension
 *
 * @author xdurand
 */
public final class RulesFeatures extends Features {

	/**
	 * Constructor.
	 */
	public RulesFeatures() {
		super("x-rules");
	}

	/**
	 * Specifies the ruleStorePlugin.
	 *
	 * @param ruleStorePluginClass
	 *            the type of plugin to use
	 * @param params
	 *            the params
	 * @return these features
	 */
	public RulesFeatures withRuleStorePlugin(final Class<? extends RuleStorePlugin> ruleStorePluginClass,
			final Param... params) {
		getModuleConfigBuilder().addPlugin(ruleStorePluginClass, params);
		return this;
	}

	/**
	 * Specifies the ruleStorePlugin.
	 *
	 * @return these features
	 */
	public RulesFeatures withDAOSupportRuleStorePlugin() {
		getModuleConfigBuilder()
				.addComponent(RuleConditionDefinitionDAO.class)//
				.addComponent(RuleDefinitionDAO.class)//
				.addComponent(SelectorDefinitionDAO.class)//
				.addComponent(RuleFilterDefinitionDAO.class)//
				.addPlugin(SQLRuleStorePlugin.class);
		return this;
	}

	/**
	 * Specifies the ruleConstantsStorePlugin.
	 *
	 * @param ruleConstantsStorePluginClass
	 *            the type of plugin to use
	 * @param params
	 *            the params
	 * @return these features
	 */
	public RulesFeatures withRuleConstantsStorePlugin(
			final Class<? extends RuleConstantsStorePlugin> ruleConstantsStorePluginClass, final Param... params) {
		getModuleConfigBuilder().addPlugin(ruleConstantsStorePluginClass, params);
		return this;
	}

	/**
	 * Specifies the ruleSelectorPlugin.
	 *
	 * @param ruleSelectorPluginClass
	 *            the type of plugin to use
	 * @param params
	 *            the params
	 * @return these features
	 */
	public RulesFeatures withRuleSelectorPlugin(final Class<? extends RuleSelectorPlugin> ruleSelectorPluginClass,
			final Param... params) {
		getModuleConfigBuilder().addPlugin(ruleSelectorPluginClass, params);
		return this;
	}

	/**
	 * Specifies the ruleValidatorPlugin.
	 *
	 * @param ruleValidatorPluginClass
	 *            the type of plugin to use
	 * @param params
	 *            the params
	 * @return these features
	 */
	public RulesFeatures withRuleValidatorPlugin(final Class<? extends RuleValidatorPlugin> ruleValidatorPluginClass,
			final Param... params) {
		getModuleConfigBuilder().addPlugin(ruleValidatorPluginClass, params);
		return this;
	}

	/** {@inheritDoc} */
	@Override
	protected void buildFeatures() {
		getModuleConfigBuilder()
				.addDefinitionProvider(DefinitionProviderConfig.builder(DynamoDefinitionProvider.class)
						.addDefinitionResource("kpr", "io/vertigo/rules/definitions/application-rules.kpr")
						.addDefinitionResource("classes", DtDefinitions.class.getName())
						.build())
				.addComponent(RuleServices.class, RuleServicesImpl.class);
	}

}
