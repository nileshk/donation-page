# Donation Page #

This is a Java [Spring Boot](https://projects.spring.io/spring-boot/) app that provides a donation page. Integrates with Stripe and PayPal for payment processing.

#### Specifics on payment services integration: ####
* [Stripe Checkout](https://stripe.com/docs/checkout) is used for credit card payments
* [Stripe](https://stripe.com/) is also used for [Apple Pay on the Web](https://stripe.com/apple-pay)
* PayPal is available via [PayPal Express Checkout](https://developer.paypal.com/docs/classic/products/express-checkout/) integration

**TODO** Allow PayPal alone without Stripe (currently Stripe Checkout is required, but PayPal is optional)

On successful donation payment, this will redirect to a thank you page with a link to the main web site (which can be configured).  Transaction history is stored in a sqlite database.

#### Optional integrations: ####
 
* [Google Sheets](https://www.google.com/sheets/about/) for transaction history 
* [MailChimp](https://mailchimp.com/) - e-mails addresses can be added to a MailChimp list
* [Google Analytics](https://analytics.google.com/) can be can configured
* [Mailgun](https://www.mailgun.com/) for notification e-mails (for donation recipient) sent to one or more addresses 
* [PushBullet](https://www.pushbullet.com/) for push notifications

See a live version of it here:

* [Jessica Vaughn for School Board](https://jessicavaughn.us/contribute/)


## Usage ##

* Build fat JAR using `mvn package` command and copy JAR to where it will be used
* In "config" folder relative to the JAR, create "application.properties" file with these properties:
  * **stripe.secretKey** - Stripe's secret key
  * **stripe.publishableKey** - Stripe's publishable key
* To enable PayPal, use these properties:
  * **paypal.enabled** - Set to `true` to enable PayPal (defaults to `false`).
  * **paypal.clientId** - PayPal API client ID.
  * **paypal.secret** - PayPal API secret.
  * **paypal.returnUrl** - PayPal API return URL.
  * **paypal.cancelUrl** - PayPal API cancel URL.
  * **paypal.sandbox** - Set to `true` to use PayPal in sandbox mode (for testing). Default is `false`.
  * **paypal.sandboxClientId** - PayPal API Sandbox client ID.
  * **paypal.sandboxSecret** - PayPal API Sandbox secret.
* These properties are optional but recommended:
  * **server.contextPath**  - path application will be at, e.g. `/contribute`
  * **server.port**  - port embedded Tomcat instance will run on
  * **app.url** - Canonical URL for donation page.  Used in og:url meta tag.
  * **app.previewImageUrl** - URL to image used for previews (used in og:image meta tag).
  * **org.displayName**  - Name that is displayed in Stripe Checkout and Apple Pay. You may want to keep this brief as there is not a lot of room in these dialogs.  Defaults to blank.
  * **org.siteTitle**  - Title of web site.  This can be a longer, more complete name.  Defaults to value specified by `org.displayName` if not provided   
* These properties are optional:
  * **stripe.applyPayEnabled** - `true` to enable Apple Pay, `false` to disable.  Defaults to `true`.
  * **url.mainPage**  - URL for main page of web site (text with `org.siteTitle` links to this). Default is `/`
  * **email.signup.url** - Page where email signup resides.  Default is `/`
  * **app.collectOccupationEnabled** - If `true`, collect contributor's occupation. Default is `true`.
  * **app.collectOccupationThreshold** - If contribution amount (in dollars) exceeds this value, occupation is required (if app.collectOccupationEnabled = `true`).
  * **app.donationLimit** - Limit donations to a specified dollar amount (disabled by default by being set to -1)
  * **app.duesYear** - Dues Year
  * **app.googleAnalyticsTrackingId** - Google Analytics Tracking Id (populating this enables Google Analytics).
  * **app.googleSheetId** - Google Sheet Id to write log to (populating this enables it). **WORK IN PROGRESS**
  * **app.clientLoggingEnabled** - Log client messages to server. Default is `true`.
  * **mailchimp.apiKey** - MailChimp API key. If this, and mailchimp.listId is set, e-mails will be stored to a MailChimp list.
  * **mailchimp.listId** - MailChimp List ID. If this, and mailchimp.apiKey is set, e-mails will be stored to a MailChimp list.
  * **mailchimp.donationsOnly** - If true, only store to MailChimp for donations. Default is `true`.
  * **mailchimp.allFields** - If true, store all available fields in MailChimp (ones that are not default to a MailChimp List). Default is `false`.  The MailChimp list must have the required tags for the additional fields, otherwise writing to the list will fail.
  * **app.emailNotificationRecipients** - Comma separated list of email address to send notifications to.
* To use Mailgun for email notifications set **app.emailNotificationRecipients** and all of the following settings:
  * **mailgun.domain** - Email domain configured in Mailgun.
  * **mailgun.apiKey** - Mailgun API key.
  * **mailgun.fromName** - Mailgun e-mail sender name.
  * **mailgun.fromEmail** - Mailgun e-mail sender address.
  * **pushbullet.apiTokens** - Comma-separated list of PushBullet API tokens.
* Using Java 8 or later, to run the application:
  * `java -jar stripe-payments.jar`
* The properties listed above could also be supplied on the command line, for example:
  * `java -jar stripe-payments.jar --org.displayName=MyOrg --server.port=9600`

## Notes ##

* You can configure Stripe test keys and there will be a very obvious notice that you are in test mode in the web page.
* For Apple Pay to work, you must configure it in Stripe and your web server must serve the provided file at `/.well-known/apple-developer-merchantid-domain-association`.  Currently that means you must, for example, put a reverse-proxy such as Nginx or Apache Web Server in front of these application, or build a WAR and deploy it to a full Tomcat instance.
* Apple Pay for Web only works in Safari. Your visitors will need to know that it doesn't work in embedded browsers such as those built into Facebook and Twitter clients
* PayPal support requires a Business account and REST API credentials.
* The `/fragment` page serves a version of the site without any JS and CSS files loaded.  This is for use, for example, for loading the HTML another application (e.g. Wordpress) _without_ using an `iframe`.  You will need to load all the JS and CSS files in your parent application's page, including `app.js` file, and resolve any style conflicts your parent application may have with Bootstrap CSS.   
* Currently only handling United States currency (dollars).

### MailChimp Tags ###

* The MailChimp list should retain these default tags (and their tags) in any case:
  * **FNAME** - `text`
  * **LNAME** - `text`

* These are the additional tags used if `emailchimp.allFields=true` with their types:
  * **DATEPAID** - `date`
  * **AMOUNTSTR** - `text`
  * **AMOUNTCENT** - `number`
  * **FULLNAME** - `text`
  * **ADDRESS1** - `text`
  * **ADDRESS2** - `text`
  * **CITY** - `text`
  * **STATE** - `text`
  * **ZIP** - `zip code (US only)`
  * **COUNTRY** - `text`
  * **OCCUPATION** - `text`
  * **PURPOSE** - `text`

## Testing ##
Testing of multiple devices and browsers for this project is being done with:

<a href="https://www.browserstack.com"><img src="https://nileshk.github.io/donation-page/doc/img/BrowserStack.svg" height="50"/></a>

## License ##

Donatation Page is licensed under MIT see `LICENSE` file
