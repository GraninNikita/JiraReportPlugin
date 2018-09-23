## Jira report plugin
#### Some features:

* Report should be available only for Jira project role – Project-manager.

##### When project manager opens a report in Jira report section – he should be able to select only Due Date (Date Picker field).
##### After clicking “Next” button – report should generate apache velocity view (.vm format) with table (Jira fields):
* Issue key
* Issue summary
* Due Date
* Assignee

Table should contains only issues where ```Due Date < Selected Due Date (if empty then Now() Date)```

Here are the SDK commands you'll use immediately:

* atlas-run   -- installs this plugin into the product and starts it on localhost
* atlas-debug -- same as atlas-run, but allows a debugger to attach at port 5005
* atlas-cli   -- after atlas-run or atlas-debug, opens a Maven command line window:
                 - 'pi' reinstalls the plugin into the running product instance
* atlas-mvn package -- use for quick reloading
* atlas-create-home-zip -- Creates a test-resources zip of the current application’s home directory
* atlas-help  -- prints description for all commands in the SDK

