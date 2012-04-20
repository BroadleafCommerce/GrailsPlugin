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

import java.io.IOException
import java.io.InputStream

import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.io.Resource
import org.springframework.web.context.support.ServletContextResource
import org.springframework.util.StringUtils

import org.broadleafcommerce.common.extensibility.context.MergeApplicationContextXmlConfigResource
import org.broadleafcommerce.common.extensibility.context.ResourceInputStream
import org.broadleafcommerce.common.extensibility.context.StandardConfigLocations
import org.broadleafcommerce.grails.jpa.BroadleafJpaDataStoreFactoryBean;
import org.codehaus.groovy.grails.commons.ConfigurationHolder;
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.grails.datastore.gorm.jpa.plugin.support.JpaMethodsConfigurer;

/**
 * This Grails plugin allows Broadleaf Commerce and its dependencies to be configured and 
 * set up as part of the Grails lifecycle.  In particular, it provides a mechanism for Broadleaf's 
 * standard merge process to be invoked and for the Broadleaf beans and dependencies to be 
 * configured as part of the standard Grails application context.
 * 
 * Because Broadleaf Commerce depends on and configures JPA entity managers, this plugin depends on 
 * the gorm-jpa plugin and the hibernate-jpa-provider.
 * 
 * @author Kelly Tisdell
 */
class BroadleafGrailsPluginGrailsPlugin {
	// the maven groupId
	def groupId = "org.broadleafcommerce.grails"
	// the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.0.3 > *"
    // the other plugins this plugin depends on
    def dependsOn = [ "gorm-jpa" : "1.0.0.M1 > *", 
		"hibernate-jpa-provider" : "1.0.0.M1 > *" ]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    def title = "Broadleaf Grails Plugin" // Headline display name of the plugin
    def author = "Kelly Tisdell"
    def authorEmail = "ktisdell@broadleafcommerce.org"
    def description = "A Grails Plugin for Broadleaf Commerce"

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/broadleaf-grails-plugin"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
	def license = "APACHE"

    // Details of company behind the plugin (if there is one)
	def organization = [ name: "Broadleaf Commerce", url: "http://www.broadleafcommerce.org" ]

    // Any additional developers beyond the author specified above.
	//def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
	//def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
	//def scm = [ url: "http://svn.grails-plugins.codehaus.org/browse/grails-plugins/" ]

    def doWithWebDescriptor = { xml ->
        //Consider adding an CustomerStateFilter here.  Or, create a CustomerStateInterceptor...
        //Also consider adding any additional tag libs here since we need tags for content.
    }

    def doWithSpring = {
		//Using logic taken from MergeXmlWebApplicationContext of Broadleaf Commerce
		//Get the default Broadleaf configurations
		def broadleafConfigLocations = StandardConfigLocations.retrieveAll(StandardConfigLocations.APPCONTEXTTYPE)
		def sources = []
		broadleafConfigLocations.each { 
			def source = MergeApplicationContextXmlConfigResource.class.getClassLoader().getResourceAsStream(it)
			if (source) {
				//Add the input stream to the list
				sources << new ResourceInputStream(source, it)
			} else {
				println "WARNING!!! The following resource was not found: '${it}'"
			}
		}
		
		//These are our backup patch locations in case someone wanted to use defaults.
		def staticPatchLocations = ["/WEB-INF/broadleaf-override-applicationContext.xml",
			                  "classpath*:broadleaf-override-applicationContext.xml"] as String[]
	    //Resolve configured patch locations.
	    //These can be configured in Config.groovy with the key "broadleaf.patchLocations"
		//The values have to be a comma-delimited list of Spring application context XML files
		def patchLocation = application.config.broadleaf.patchLocations
		
		def patchLocations
		if (patchLocation) {
			patchLocations = StringUtils.tokenizeToStringArray(patchLocation, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS)
		} else {
			patchLocations = staticPatchLocations
		}
		
		def patches = []
		def foundPatch = false
		patchLocations.each {
			def patch = null
			if (it.startsWith("classpath")) {
				def is = BroadleafGrailsPluginGrailsPlugin.class.getClassLoader().getResourceAsStream(it.substring("classpath*:".length(), it.length()))
				if (is){
					patch = new ResourceInputStream(is, it)
					patches << patch
					foundPatch = true
				} else {
					println "Didn't find resource: ${it}"
				}
			} else {
				def resource = new ServletContextResource(ServletContextHolder.getServletContext(), it);
				if (resource.exists()) {
					patch = new ResourceInputStream(resource.getInputStream(), it)
					patches << patch
					foundPatch = true
				} else {
					println "Didn't find resource: ${it}"
				}
			}
		}
		
		if (! foundPatch) {
			println "Did not find any Broadleaf patch files. No merge will occur, and Broadleaf Commerce might not function properly."
			println "Consider adding a Spring application context file to ${patchLocations[0]} or ${patchLocations[1]} with the necessary overrides, or see the Broadleaf documentation."
		}
		
		//mergedBroadleafContext will be a org.springframework.core.io.Resource[], which contails a single Resource backed by a byte array
		//def mergedBroadleafContext = ...
		def mergedBroadleafContext = 
			new MergeApplicationContextXmlConfigResource().getConfigResources(sources as ResourceInputStream[], patches as ResourceInputStream[])
		
		def tempFile
		try {
			//The BeanBuilder takes a Resource array in the loadBeans(Resource[] resources) method, but that method assumes they are Groovy code
			//Luckily, there is an importBeans method that takes a file name as a string and consumes XML as long as it has an XML file extension.
			//So, take the byte array from the merged context and write it to a temp file so that it can be consumed by the BeanBuilder
			tempFile = File.createTempFile("broadleaf-merge-context",".xml")
			tempFile.setBytes(mergedBroadleafContext[0].getByteArray())
			def mergePath = tempFile.getAbsolutePath()
			importBeans("file:///${mergePath}")
			
			//Now, override / define additional beans here...
			//Need to override the JpaDataStoreFactoryBean. It tries to find a transaction manager by 
			//type.  Broadleaf registers two of them.
			jpaDatastore(BroadleafJpaDataStoreFactoryBean) {
				entityManagerFactory = ref("entityManagerFactory")
				transactionManager = ref("blTransactionManager")
				mappingContext = ref("jpaMappingContext")
			}
			
		} finally {
			//Clean up
			tempFile?.delete()
		}
		
    }

    def doWithDynamicMethods = { ctx ->
		//Since Broadleaf registers at least 2 JPA transaction managers, and the GORM / JPA plugin registers 1, 
		//this closure breaks in the GORM / JPA plugin because of the way the transaction manager is 
		//looked up (by type).
		
        //For now, let's implement this closure the same way, except let's find the transaction manager by 
		//name instead of by type... This will have to be changed if/when the GORM / JPA plugin 
		//supports multiple persistence units and transaction managers.
		def datastore = ctx.jpaDatastore
		def transactionManager = ctx.blTransactionManager  //This is Broadleaf's default configured transaction manager
		def methodsConfigurer = new JpaMethodsConfigurer(datastore, transactionManager)
		def foe = application?.config?.grails?.gorm?.failOnError
		methodsConfigurer.failOnError = foe instanceof Boolean ? foe : false
		methodsConfigurer.configure()
    }

    def doWithApplicationContext = { applicationContext ->
        // Not implemented
    }

    def onChange = { event ->
        // Not implemented
    }

    def onConfigChange = { event ->
        // Not implemented
    }

    def onShutdown = { event ->
        // Not implemented
    }
}
