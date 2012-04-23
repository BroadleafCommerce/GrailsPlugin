Broadleaf Grails Plugin
=======================

broadleaf-grails-plugin

Overview
--------

The Broadleaf Grails Plugin allows you run Broadleaf Commerce as a Grails Application.  
[Broadleaf Commerce] [blc] is an enterprise eCommerce framework built on Spring and JPA 2.0 / Hibernate.  Broadleaf Commerce is a highly customizable framework, and as such it allows a developer to extend the framework with additional domain objects, services, DAOs, workflows, and integrations.  Much of this is achieved via configuration. Broadleaf Commerce has a complex, custom merge process that allows an implementor to override or extend the Broadleaf Framework via Spring configuration files that are merged together with specific precedence to allow the implementor to swap out the default components of Broadleaf with custom components and domain classes.  Broadleaf Commerce typically recommends using Spring MVC. However the choice of UI is open to the implementor.
  [blc]: http://www.broadleafcommerce.org

The Broadleaf Grails Plugin is a plugin that integrates Broadleaf's complex merge process within the context of the Grails conventions and lifecycles.

The Broadleaf Grails Plugin currently relies on Broadleaf Commerce version 1.7, Spring 3.1, [GORM JPA Plugin 1.0.0.M1] [gorm_jpa_plugin], and Grails 2.0.3.  This plugin will be published to the [Grails Plugin Repository] [grails_plugins].

  [grails_plugins]: http://grails.org/plugins/
  [gorm_jpa_plugin]: http://grails.org/plugin/gorm-jpa
  
Getting Started
---------------

1. Download and install Maven according to the [Maven documentation] [maven_docs]
2. Download and install Grails 2.0.3
3. Create 2 new Grails projects according to the [Grails documentation] [grails_docs]

  a. The first project should be a Grails Plugin Project for extending Broadleaf's JPA domain (e.g. GORM classes) and should be packaged as binary (see <http://grails.org/doc/latest/guide/plugins.html#binaryPlugins>)
  
  b. The second project should be a standard Grails application for the main web application
  
4. Add a dependency on the broadleaf-grails-plugin (grails install-plugin broadleaf-grails-plugin) to each of the projects.

5. Install the [Grails Release Plugin] [grp] in the domain extension project (grails install-plugin release)

6. Build the domain extension project as a binary (grails package-plugin --binary)

7. Build the domain extension project as a jar and publish it to the local Maven cache (grails maven-install)

6. In the main web application, add a dependency on the domain extension project, which should be available as a jar in the local Maven cache

  [maven_docs]: http://maven.apache.org/download.html
  [grails_docs]: http://grails.org/Quick+Start
  [grp]: http://grails.org/plugin/release
  
Considerations
--------------

The Broadleaf Grails Plugin is very new.  There are a number of know issues currently:

1. The GORM JPA Plugin is required to bridge the gap between GORM and JPA. Technically it only officially supports JPA 1.0. Broadleaf Commerce uses JPA 2.0. To our knowledge, everything works.  However, this is a potential problem that may need to be considered when using this plugin.

2. Broadleaf currently has 3 data source references ( a main datasource, a secure datasource for payment info if you are accepting payment directly, and a content management datasource). They can all be the same physical database, but they are defined separately for PCI considerations and content management. As far as we know, the GORM JPA Plugin only supports 1 data source and 1 transaction manager. Therefore, when using this plugin, you are constrained to one physical datasource and a default transaction manager for GORM-related operations. This will be acceptable for many or most users.

3. The need for 2 projects is required for several reasons:

  a. The way that JPA resolves the classes is relative to the location of the Persistence Unit. Therefore, for the Broadleaf JPA provider to find domain extensions, they must be packaged in a jar in the WEB-INF/lib directory of the main web application.
  
  b. In order to expose the domain extensions to the Broadleaf Admin module, they need to be in a separate jar file, resolvable by Maven.
  
Next Steps
----------

We are working on more detailed documentation for using this plugin.  We will also be providing a sample application that uses this plugin to demonstrate how it works.
