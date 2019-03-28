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

import io.vertigo.account.AccountFeatures;
import io.vertigo.account.plugins.account.store.loader.LoaderAccountStorePlugin;
import io.vertigo.app.config.AppConfig;
import io.vertigo.app.config.AppConfigBuilder;
import io.vertigo.app.config.ModuleConfig;
import io.vertigo.commons.impl.CommonsFeatures;
import io.vertigo.core.param.Param;
import io.vertigo.core.plugins.resource.classpath.ClassPathResourceResolverPlugin;
import io.vertigo.database.DatabaseFeatures;
import io.vertigo.database.impl.sql.vendor.postgresql.PostgreSqlDataBase;
import io.vertigo.database.plugins.sql.connection.c3p0.C3p0ConnectionProviderPlugin;
import io.vertigo.dynamo.impl.DynamoFeatures;
import io.vertigo.dynamo.plugins.store.datastore.sql.SqlDataStorePlugin;
import io.vertigo.impl.workflow.WorkflowFeatures;
import io.vertigo.persona.impl.security.PersonaFeatures;
import io.vertigo.plugins.workflow.memory.MemoryWorkflowStorePlugin;
import io.vertigo.plugins.workflow.validate.RuleWorkflowPredicateAutoValidatePlugin;
import io.vertigo.rules.impl.RulesFeatures;
import io.vertigo.rules.plugins.memory.MemoryRuleConstantsStorePlugin;
import io.vertigo.rules.plugins.memory.MemoryRuleStorePlugin;
import io.vertigo.rules.plugins.selector.SimpleRuleSelectorPlugin;
import io.vertigo.rules.plugins.validator.SimpleRuleValidatorPlugin;
import io.vertigo.workflow.data.MockIdentities;
import io.vertigo.workflow.data.MyDummyDtObjectProvider;
import io.vertigo.workflow.data.TestUserSession;
import io.vertigo.workflow.plugin.MemoryItemStorePlugin;

/**
 * Config for Junit
 *
 * @author xdurand
 *
 */
public class MyAppConfig {

	/**
	 * Configuration de l'application pour Junit
	 *
	 * @return AppConfig for Junit
	 */
	public static AppConfig config() {
		final AppConfigBuilder appConfigBuilder = AppConfig.builder()
				.beginBoot()
				.withLocales("fr")
				.addPlugin(ClassPathResourceResolverPlugin.class)
				.endBoot()
				.addModule(new PersonaFeatures().withUserSession(TestUserSession.class).build())
				.addModule(new CommonsFeatures()
						.withCache(io.vertigo.commons.plugins.cache.memory.MemoryCachePlugin.class)
						.withScript()
						.build())
				.addModule(new DatabaseFeatures()
						.withSqlDataBase()
						.addSqlConnectionProviderPlugin(C3p0ConnectionProviderPlugin.class,
								Param.of("dataBaseClass", PostgreSqlDataBase.class.getName()),
								Param.of("jdbcDriver", org.postgresql.Driver.class.getName()),
								Param.of("jdbcUrl",
										"jdbc:postgresql://laura.dev.klee.lan.net:5432/dgac_blanche?user=blanche&password=blanche"))
						.build())
				.addModule(new DynamoFeatures()
						.withStore()
						.addDataStorePlugin(SqlDataStorePlugin.class)
						.build())
				.addModule(new AccountFeatures()
						.withAccountStorePlugin(LoaderAccountStorePlugin.class,
								Param.of("accountLoaderName", "MockIdentities"),
								Param.of("groupLoaderName", "MockIdentities"))
						.build())
				.addModule(new RulesFeatures()
						.withRuleStorePlugin(MemoryRuleStorePlugin.class)
						.withRuleConstantsStorePlugin(MemoryRuleConstantsStorePlugin.class)
						.withRuleSelectorPlugin(SimpleRuleSelectorPlugin.class)
						.withRuleValidatorPlugin(SimpleRuleValidatorPlugin.class).build())
				.addModule(new WorkflowFeatures()
						.withWorkflowStorePlugin(MemoryWorkflowStorePlugin.class)
						.withWorkflowPredicateAutoValidatePlugin(RuleWorkflowPredicateAutoValidatePlugin.class)
						.withItemStorePlugin(MemoryItemStorePlugin.class).build())
				.addModule(ModuleConfig.builder("dummy")
						.addDefinitionProvider(MyDummyDtObjectProvider.class)
						.addComponent(MockIdentities.class)
						.build());

		return appConfigBuilder.build();
	}

}
