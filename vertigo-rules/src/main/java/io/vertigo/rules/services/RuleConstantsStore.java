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
package io.vertigo.rules.services;

/**
 * This class defines the storage of rules constants.
 * @author xdurand
 */
public interface RuleConstantsStore {

	/**
	 * Add constants
	 * @param key
	 * @param ruleConstants
	 */
	void addConstants(Long key, RuleConstants ruleConstants);

	/**
	 * Remove constants
	 * @param key
	 */
	void removeConstants(Long key);

	/**
	 * Update constants
	 * @param key
	 * @param ruleConstants
	 */
	void updateConstants(Long key, RuleConstants ruleConstants);

	/**
	 * Get constants
	 * @param key
	 * @return the rule constants matching the key
	 *
	 */
	RuleConstants readConstants(Long key);

}
