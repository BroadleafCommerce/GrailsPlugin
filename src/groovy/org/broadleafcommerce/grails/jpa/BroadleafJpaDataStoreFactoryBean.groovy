/*
* Copyright 2008-2009 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.broadleafcommerce.grails.jpa

import groovy.lang.MetaClass;
import javax.persistence.EntityManagerFactory

import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.gorm.jpa.bean.factory.JpaDatastoreFactoryBean;
import org.grails.datastore.mapping.jpa.JpaDatastore
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.orm.jpa.JpaTransactionManager

/**
 * This class overrides the JpaDatastoreFactoryBean.  The JpaDatastoreFactoryBean does not 
 * allow for injection of the entityManagerFactory or the transaction manager.  As a result, 
 * it tries to look them up by type.  Because there are multiple transaction managers registered 
 * with Broadleaf Commerce the default implementation throws an exception. We get around that 
 * by allowing the transactionManager and entityManagerFactory to be injected rather than 
 * looking them up by type.
 * 
 * @author Kelly Tisdell
 *
 */
class BroadleafJpaDataStoreFactoryBean extends JpaDatastoreFactoryBean {
	
	def entityManagerFactory
	def transactionManager

	@Override
	public JpaDatastore getObject() {
		def datastore = new JpaDatastore(mappingContext, entityManagerFactory, transactionManager, (ConfigurableApplicationContext)applicationContext)
		applicationContext.addApplicationListener new DomainEventListener(datastore)
		applicationContext.addApplicationListener new AutoTimestampEventListener(datastore)
		datastore
	}
	
}
