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
package io.vertigo.rules;

import io.vertigo.dynamo.domain.model.DtObject;
import io.vertigo.dynamo.domain.stereotype.Field;
import io.vertigo.dynamo.domain.util.DtObjectUtil;
import io.vertigo.lang.Generated;

/**
 * This class is automatically generated.
 * DO NOT EDIT THIS FILE DIRECTLY.
 */
@Generated
public final class RuleCriteria implements DtObject {
	private static final long serialVersionUID = 1L;

	private Long wfwdId;
	private io.vertigo.rules.RuleConditionCriteria conditionCriteria1;
	private io.vertigo.rules.RuleConditionCriteria conditionCriteria2;
	
	/**
	 * Champ : DATA.
	 * Récupère la valeur de la propriété 'id'.
	 * @return Long wfwdId <b>Obligatoire</b>
	 */
	@Field(domain = "DO_RULES_WEAK_ID", required = true, label = "id")
	public Long getWfwdId() {
		return wfwdId;
	}

	/**
	 * Champ : DATA.
	 * Définit la valeur de la propriété 'id'.
	 * @param wfwdId Long <b>Obligatoire</b>
	 */
	public void setWfwdId(final Long wfwdId) {
		this.wfwdId = wfwdId;
	}
	
	/**
	 * Champ : DATA.
	 * Récupère la valeur de la propriété 'Field 1'.
	 * @return io.vertigo.rules.RuleConditionCriteria conditionCriteria1
	 */
	@Field(domain = "DO_DT_RULE_CONDITION_CRITERIA_DTO", label = "Field 1")
	public io.vertigo.rules.RuleConditionCriteria getConditionCriteria1() {
		return conditionCriteria1;
	}

	/**
	 * Champ : DATA.
	 * Définit la valeur de la propriété 'Field 1'.
	 * @param conditionCriteria1 io.vertigo.rules.RuleConditionCriteria
	 */
	public void setConditionCriteria1(final io.vertigo.rules.RuleConditionCriteria conditionCriteria1) {
		this.conditionCriteria1 = conditionCriteria1;
	}
	
	/**
	 * Champ : DATA.
	 * Récupère la valeur de la propriété 'Field 2'.
	 * @return io.vertigo.rules.RuleConditionCriteria conditionCriteria2
	 */
	@Field(domain = "DO_DT_RULE_CONDITION_CRITERIA_DTO", label = "Field 2")
	public io.vertigo.rules.RuleConditionCriteria getConditionCriteria2() {
		return conditionCriteria2;
	}

	/**
	 * Champ : DATA.
	 * Définit la valeur de la propriété 'Field 2'.
	 * @param conditionCriteria2 io.vertigo.rules.RuleConditionCriteria
	 */
	public void setConditionCriteria2(final io.vertigo.rules.RuleConditionCriteria conditionCriteria2) {
		this.conditionCriteria2 = conditionCriteria2;
	}
	
	/** {@inheritDoc} */
	@Override
	public String toString() {
		return DtObjectUtil.toString(this);
	}
}
