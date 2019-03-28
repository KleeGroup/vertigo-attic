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
package io.vertigo.workflow.domain.instance;

import java.io.Serializable;

import io.vertigo.dynamo.domain.model.MasterDataEnum;
import io.vertigo.dynamo.domain.model.URI;
import io.vertigo.dynamo.domain.util.DtObjectUtil;

public enum WfStatusEnum implements MasterDataEnum<io.vertigo.workflow.domain.instance.WfStatus> {

	CRE("CRE"), //
	STA("STA"), //
	PAU("PAU"), //
	END("END")
	;

	private final URI<io.vertigo.workflow.domain.instance.WfStatus> entityUri;

	private WfStatusEnum(final Serializable id) {
		entityUri = DtObjectUtil.createURI(io.vertigo.workflow.domain.instance.WfStatus.class, id);
	}

	@Override
	public URI<io.vertigo.workflow.domain.instance.WfStatus> getEntityUri() {
		return entityUri;
	}

}
