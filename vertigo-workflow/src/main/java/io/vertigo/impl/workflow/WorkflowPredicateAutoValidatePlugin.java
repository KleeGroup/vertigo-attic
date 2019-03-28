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
package io.vertigo.impl.workflow;

import io.vertigo.core.component.Plugin;
import io.vertigo.dynamo.domain.model.DtObject;
import io.vertigo.workflow.domain.model.WfActivityDefinition;

/**
 * @author xdurand
 *
 */
public interface WorkflowPredicateAutoValidatePlugin extends Plugin {

	/**
	 * Predicate to determine if the current activityDefinition can be autovalidated for the provided object
	 * @param activityDefinition the activityDefinition to test
	 * @param object the object to test
	 * @return true if the current activity can be auto validated, false otherwise
	 */
	boolean canAutoValidateActivity(final WfActivityDefinition activityDefinition, final DtObject object);

}
