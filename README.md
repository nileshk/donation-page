# Donation Page #

This is a Java [Spring Boot](https://projects.spring.io/spring-boot/) app that provides a donation page. Currently uses Stripe for payment processing, using [Checkout](https://stripe.com/docs/checkout) for credit card processing and supporting [Apple Pay](https://stripe.com/apple-pay).

On successful donation payment, this will redirect to a thank you page with a link to sign-up for e-mails.

See a live version of it here:

* [Jessica Vaughn for School Board](https://jessicavaughn.us/contribute/)


## Usage ##

* Build fat JAR using `mvn package` command and copy JAR to where it will be used
* In "config" folder relative to the JAR, create "application.properties" file with these properties:
  * **stripe.secretKey** - Stripe's secret key
  * **stripe.publishableKey** - Stripe's publishable key
* These properties are optional but recommended:
  * **server.contextPath**  - path application will be at, e.g. `/contribute`
  * **server.port**  - port embedded Tomcat instance will run on
  * **org.displayName**  - Name that is displayed in Stripe Checkout and Apple Pay. You may want to keep this brief as there is not a lot of room in these dialogs.  Defaults to blank.
  * **org.siteTitle**  - Title of web site.  This can be a longer, more complete name.  Defaults to value specified by `org.displayName` if not provided   
* These properties are optional:
  * **stripe.applyPayEnabled** - `true` to enable Apple Pay, `false` to disable.  Defaults to `true`.
  * **url.mainPage**  - URL for main page of web site (text with `org.siteTitle` links to this). Default is `/`
  * **email.signup.url** - Page where email signup resides.  Default is `/`
  * **app.collectOccupationEnabled** - if `true`, collect contributor's occupation. Default is `true`.
  
* Using Java 8 or later, to run the application:
  * java -jar stripe-payments.jar
* The properties listed above could also be supplied on the command line, for example:
  * java -jar stripe-payments.jar --org.displayName=MyOrg --server.port=9600


## Notes ##

* You can configure Stripe test keys and there will be a very obvious notice that you are in test mode in the web page.
* For Apple Pay to work, you must configure it in Stripe and your web server must serve the provided file at `/.well-known/apple-developer-merchantid-domain-association`.  Currently that means you must, for example, put a reverse-proxy such as Nginx or Apache Web Server in front of these application, or build a WAR and deploy it to a full Tomcat instance.
* Apple Pay for Web only works in Safari. Your visitors will need to know that it doesn't work in embedded browsers such as those built into Facebook and Twitter clients 
* The `/fragment` page serves a version of the site without any JS and CSS files loaded.  This is for use, for example, for loading the HTML another application (e.g. Wordpress) _without_ using an `iframe`.  You will need to load all the JS and CSS files in your parent application's page, including `app.js` file, and resolve any style conflicts your parent application may have with Bootstrap CSS.   

## License ##

Donatation Page is licensed under MIT see `LICENSE` file
