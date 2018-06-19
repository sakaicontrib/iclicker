i>clicker integrate for Sakai
=============================
The >clicker integrate for Sakai plug-in allows instructors to easily synchronize their i>clicker and i>grader data with 
their campus Sakai server. Learn more about i>clicker products and features from the i>clicker website (http://www.iclicker.com)

Features
--------
Students
   * Register and manage their remotes from within Sakai
Instructors
   * View reports within Sakai showing the status of student registrations in their classes
   * Download their Sakai roster and registrations directly into i>grader
   * Upload i>clicker scores directly into Sakai's Gradebook
Administrators
   * Configurable for SSO
   * View and manage remote registrations


Sakai Compatibility
-------------------
This version of the plug-in works with Sakai 11 and supports single sign-on. Installations of Sakai older than version 11 are not supported by the i>clicker integrate plug-in.

Build
-----
The source code for this plugin is located at https://github.com/sakaicontrib/iclicker

Build the default version of the app by running this command from the location of this README ::

    mvn -e clean install


Install
-------
There are 2 options for installation.

A.  Execute the maven sakai deployment command from the location of this README ::

        mvn sakai:deploy

    This should copy the war file into the sakai tomcat instance.

B.  Manually copy the war file into your servlet container ::

        cp target/iclicker.war {your servlet container war file location}


Configuration
-------------
Configuration is stored in the sakai.properties file (normally in {TOMCAT_HOME}/sakai).
All the properties are optional. If they are not included the default values are used as shown.

    ## i>clicker Tool configuration
    # the iclicker tool title in the workspaces, DEFAULT: i>clicker
    #iclicker.workspace.title={title}
    # the email addresses to send notifications to on failures, DEFAULT: system admin email (e.g. admin@server.edu)
    #iclicker.notify.emails={email (comma separated list)}
    # disable the alternate remote id handling, DEFAULT: false
    #iclicker.turn.off.alternate.remote.id={true|false}
    # Control the default max limit of 100 courses to fetch from Sakai
    # WARNING: making this number too large could cause performance issues
    #iclicker.max.courses=100
    # enable SSO support by setting a shared key (must be at least 10 chars long)
    #iclicker.sso.shared.key={key string}
    ## Domain configuration
    # the iclicker domain URL, DEFAULT: the Sakai server URL (e.g. http://your.server.edu)
    #iclicker.domainurl={server url}
    #iclicker REST service /courses to return only published sites, DEFAULT: false
    #iclicker.get.published.courses.only=false

NOTE on Single Sign-On::

    The single sign-on support allows Sakai installations with CAS, Shib, or other single sign-on (SSO)
    systems to still authenticate from the i>clicker client to the Sakai LMS without using normal username
    and password authentication (which usually will not work when using an SSO system). Once the SSO shared key
    is set, the normal username and password authentication will stop working and only the SSO authentication
    will function.
    Admins will need to ensure the shared key is set to the same value in both the Sakai config and the i>clicker
    admin setup and is at least 10 chars long.


Usage
-----
Start Sakai and you will see the new app appear as a tool which can be added to your site.
This tool should be added to the workspace template for all Sakai users so it is accessible to 
everyone in the institution.

Adding the plug-in as a tool in a site:

    1. Log in to Sakai and enter the site where you want to add the i>clicker plug-in
    2. Click on Site Info on the left
    3. Click on Edit Tools (or just Tools in older versions)
    4. Check the box next to the "i>clicker" tool in the list and click Continue
    5. Click Continue to confirm, the tool is now installed in your site and ready for use

Configuring Sakai so the plug-in is added to all new workspaces (adding to the workspace template):

    1. Log in to Sakai as admin and enter the administrator site
    2. Click on the Sites tool
    3. Click on the !user site
    4. Click on the Pages button at the bottom
    5. Click on New Page at the top
    6. Enter a title of i>clicker (or whatever you want to call it) and click the Tools button
    7. Click the New Tool link at the top
    8. Select the i>clicker (sakai.iclicker) tool from the list
    9. Click the Save button at the bottom to complete the template update

Adding the plug-in to all existing workspaces (steps are same to remove from all):

    1. Log in to Sakai as admin and enter any site which has the i>clicker plug-in installed
    2. Click on the Admin Tools link
    3. Click the button in the upper right to install the tool in all workspaces (may take awhile)
    4. The tool will now be added to all workspaces


REST data feeds
---------------

    There is a complete set of REST data feeds for interacting with the i>clicker data so that institutions can
    automate or control the i>clicker registrations and data as desired. The feeds are also used by the
    i>clicker tools. The feeds are documented and located at ::
    
        http://{SAKAI_SERVER_HOSTNAME}/direct/iclicker/describe
