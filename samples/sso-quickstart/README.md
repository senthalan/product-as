Quick Start Guide - SSO
========================

Introduction
------------

This sample will demonstrate the single sign on functionality using WSO2 Application Server and WSO2 Identity Server.

Requirements
-------------

1. JDK 1.8 or higher
2. A JavaScript compatible web browser
3. An active Internet connection

How to run the sample
----------------------

1. Download **wso2is-5.1.0.zip** from [here](http://wso2.com/products/identity-server/)
2. Copy that to <AS_HOME>/samples/sso-quickstart/packs directory or you can set path manually when running the script.
   (`-Dwso2is.zip.path=`"path to wso2is-5.1.0.zip")
    * Note: Create a "packs" directory inside <AS_HOME>/samples/sso-quickstart if not present.
3. Then run the script file.
    * For Linux - run `sso-quickstart.sh`
    * For windows - run the `sso-quickstart.bat`
4. Go to the given webapp urls to check the sso functionality.
5. Press `ctrl+c` to exit from the sample.
6. Run `clean.sh` or `clean.bat` file according to your operating system to revert the changes after you exit from the sample.

Description
------------

This will first extract the WSO2 Identity Server zip file and then it will deploy two sample webapps in WSO2 Application
 server(Book Store app and Music Store app). After that it will make relevant configuration file changes in the WSO2
 Application server and WSO2 Identity server. Finally it will start two servers and then you can observe sso
 functionality by check on the given urls.
