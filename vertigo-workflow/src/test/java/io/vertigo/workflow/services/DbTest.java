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

import javax.inject.Inject;

import org.junit.Assert;

import io.vertigo.commons.transaction.VTransactionManager;
import io.vertigo.commons.transaction.VTransactionWritable;

/**
 * Base Class for Database unit test
 * @author xdurand
 *
 */
public class DbTest {

	@Inject
	private VTransactionManager transactionManager;

	private VTransactionWritable transaction;

	protected void doSetUp() throws Exception {
		Assert.assertFalse("the previous test hasn't correctly close its transaction.",
				transactionManager.hasCurrentTransaction());
		// manage transactions
		transaction = transactionManager.createCurrentTransaction();
	}

	protected void doTearDown() throws Exception {
		Assert.assertTrue("All tests must rollback a transaction.", transactionManager.hasCurrentTransaction());
		// close transaction
		transaction.rollback();
	}

}
