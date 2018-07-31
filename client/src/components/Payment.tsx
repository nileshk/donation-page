import axios from "axios";
import * as React from "react";

interface AppConfig {
  mainPageUrl?: string;
  siteTitle?: string;
  appPreviewImageUrl?: string;
  publishableKey: string;
  organizationDisplayName?: string;
  applePayEnabled?: boolean;
  clientLoggingEnabled?: boolean;
  collectOccupationEnabled?: boolean;
  collectOccupationThreshold: number;
  donationLimit: number;
  paypalEnabled?: boolean;
  paypalSandbox?: boolean;
  emailSignupUrl?: string;
  vcsBuildId?: string;
}

interface State {
  appConfig: AppConfig;
  testMode: boolean;
  stripeHandler: any;
  submittedAmount: number;
  submittedAmountStr: string;
  occupation: string;
  pagePurpose: string;
  stripePaymentsBaseUrl: string;
}

export default class Payment extends React.Component<{}, State> {
  constructor(props: {}) {
    super(props);
    this.state = {
      appConfig: {publishableKey: '', donationLimit: 0, collectOccupationThreshold: 100},
      testMode: false,
      stripeHandler: null,
      submittedAmount: 0,
      submittedAmountStr: '0',
      occupation: '',
      pagePurpose: 'donation',
      stripePaymentsBaseUrl: ''
    };
  }

  public componentDidMount() {
    axios.get('getConfig')
      .then(res => {
        const data: AppConfig = res.data;
        this.init(data);
      })
  }

  private init = (app: AppConfig) => {
    // @ts-ignore
    Stripe.setPublishableKey(app.publishableKey);
    const stripePaymentsBaseUrl = this.state.stripePaymentsBaseUrl;

    const that: any = this;

    const tokenFunc = (token: any) => {
      // You can access the token ID with `token.id`.
      // Get the token ID to your server-side code for use.
      const param = {
        id: token.id,
        currency: 'usd',
        amount: that.state.submittedAmount,
        description: token.email,
        occupation: that.state.occupation,
        collectOccupationEnabled: app.collectOccupationEnabled,
        pagePurpose: that.state.pagePurpose,
        token
      };
      // console.log(param);
      // @ts-ignore
      $('#processingPaymentDialog').modal();
      // @ts-ignore
      $.ajax({
        type: 'POST',
        url: stripePaymentsBaseUrl + 'submitPayment',
        data: JSON.stringify(param),
        contentType: "application/json",
        dataType: 'json'
      }).done((data: any) => {
        that.hideProcessingPayment();
        // console.log(data);
        if (!data.error) {
          that.doSuccess(data.amount, data.email);
        } else {
          that.errorDialog(data.errorMessage);
        }
      }).fail((jqXHR: any, textStatus: any, errorThrown: any) => {
        that.hideProcessingPayment();
        that.errorDialog(textStatus + ": " + errorThrown);
      }).always(() => {
        that.hideProcessingPayment();
      });
    };

    // @ts-ignore
    const handler = StripeCheckout.configure({
      key: app.publishableKey,
      image: 'https://stripe.com/img/documentation/checkout/marketplace.png',
      locale: 'auto',
      currency: 'usd',
      billingAddress: true,
      allowRememberMe: true,
      token: tokenFunc
    });

    const testMode: boolean = app.publishableKey.startsWith('pk_test');
    if (testMode) {
      console.log("TEST MODE - No money will be transferred");
    };

    this.setState({appConfig: app, testMode, stripeHandler: handler});
  };

  private static isEmpty(str: string) {
    return (!str || 0 === str.length);
  }

  private handleDonate(amount: number, amountStr: string) {
    // Open Checkout with further options:
    const submittedAmount: number = amount;
    const submittedAmountStr: string = amountStr;

    // @ts-ignore
    const occupation: string = $('#occupationInput').val();

    this.setState({submittedAmount, submittedAmountStr, occupation});

    let shouldReturn = false;
    this.log('Selected amount: $' + submittedAmountStr);

    const app = this.state.appConfig;
    if (app.collectOccupationEnabled && submittedAmount > (app.collectOccupationThreshold * 100) && Payment.isEmpty(occupation)) {
      // @ts-ignore
      $('#alertOccupationText').text("Please provide your occupation");
      // @ts-ignore
      $('#alertOccupation').removeClass("hidden");
      shouldReturn = true;
      this.log('Occupation not provided');
    } else {
      // @ts-ignore
      $('#alertOccupation').addClass("hidden");
      if (!Payment.isEmpty(occupation)) {
        this.log("Occupation: " + occupation);
      }
    }
    if (app.donationLimit > 0 && submittedAmount > (app.donationLimit * 100)) {
      const donationLimitErrorText = "Donation exceeds limit of $" + app.donationLimit + ".";
      // @ts-ignore
      $('#alertCustomDonationText').text(donationLimitErrorText);
      // @ts-ignore
      $('#alertCustomDonation').removeClass("hidden");
      shouldReturn = true;
      this.log(donationLimitErrorText);
    } else {
      // @ts-ignore
      $('#alertCustomDonation').addClass("hidden");
    }
    if (shouldReturn) {
      return;
    }

    if (!app.applePayEnabled && !app.paypalEnabled) {
      this.doCreditCardDonate();
    } else {
      // @ts-ignore
      $('#donationAmountAlert').text("Amount: $" + submittedAmountStr);
      // $('.donation-selection').hide();
      // @ts-ignore
      $('#multi-pay-options').modal();
      this.log('Showing multiple pay options');
    }
  }

  // @ts-ignore
  private hideProcessingPayment = () => {
    // @ts-ignore
    $('#processingPaymentDialog').modal('hide');
  };

  // @ts-ignore
  private errorDialog = (errorMessage) => {
    this.hideProcessingPayment();
    // @ts-ignore
    $("#errorMessage").text(errorMessage);
    // @ts-ignore
    $('#errorDialog').modal();
    this.hideMultiPay();
    // @ts-ignore
    $('.donation-selection').show();
  };

  private doCreditCardDonate = () => {
    this.hideMultiPay();
    const app = this.state.appConfig;
    this.state.stripeHandler.open({
      name: app.organizationDisplayName,
      description: 'Donate $' + this.state.submittedAmountStr,
      amount: this.state.submittedAmount
    });
    this.log('Doing credit card donate');
  };

  private hideMultiPay = () => {
    // @ts-ignore
    $('#multi-pay-options').modal('hide');
  };

  // @ts-ignore
  private doSuccess = (amount, email) => {
    let loc = "successfulPayment?amount=" + amount;
    if (email) {
      loc += "&email=" + email;
    }
    // @ts-ignore
    window.location = loc;
  };

  private log = (logObject: any) => {
    setTimeout(() => {
      if (this.state.appConfig.clientLoggingEnabled) {
        // @ts-ignore
        $.ajax({
          type: 'POST',
          url: this.state.stripePaymentsBaseUrl + 'log',
          data: JSON.stringify(logObject),
          contentType: "application/json",
          dataType: 'json'
        }).fail((jqXHR: any, textStatus: any, errorThrown: any) => {
          if (window.console) {
            console.log(textStatus + ": " + errorThrown);
            console.log("Failed to log: ");
            console.log(logObject);
          }
        });
      }
      if (window.console) {
        console.log(logObject);
      }
    }, 10);
  };

  public render() {
    const app = this.state.appConfig;
    const allowSpecificAmount: boolean = true;
    const collectOccupation: boolean = app.collectOccupationEnabled || false; // XXX This will vary based on configuration

    // TODO Dynamically generate buttons based on configuration

    return (
      <div className="container">
        { this.state.testMode ?
          <div id="alertTop" className="alert-top alert alert-danger" role="alert">! TEST MODE - No money will be transferred !</div>
        : ''}
        <div className="donation-selection">
          <div className="well well-sm" style={{textAlign: 'center'}}>
            <h4 className="display-3">Contribute to</h4>
            <a href={app.mainPageUrl} className="site-title">
              <h4>{app.siteTitle}</h4>
            </a>
          </div>

          {collectOccupation ?
            <div>
              <h5>Occupation or Business Type:</h5>
              <form>
                <div id="alertOccupation" role="alert" className="hidden alert alert-danger">
                  <span className="glyphicon glyphicon-exclamation-sign" aria-hidden="true"/>
                  <span className="sr-only">Error:</span>
                  <span id="alertOccupationText"/>
                </div>
                <input className="form-control" id="occupationInput" placeholder="Enter occupation or business type" style={{width: '250px'}}/>
              </form>
            </div>
            : ""
          }

          <div>
            <h5>Amount:</h5>
            <div className="row express-button-row">
              <button id="donateButton_5" className="col-sm-1 btn btn-primary express-button" onClick={(e) => this.handleDonate(500, '5')}>$5</button>
              <button id="donateButton_10" className="col-sm-1 btn btn-primary express-button" onClick={(e) => this.handleDonate(1000, '10')}>$10</button>
              <button id="donateButton_18" className="col-sm-1 btn btn-primary express-button" onClick={(e) => this.handleDonate(1800, '18')}>$18</button>
              <button id="donateButton_27" className="col-sm-1 btn btn-primary express-button" onClick={(e) => this.handleDonate(2700, '27')}>$27</button>
            </div>
            <div className="row express-button-row">
              <button id="donateButton_50" className="col-sm-1 btn btn-primary express-button" onClick={(e) => this.handleDonate(5000, '50')}>$50</button>
              <button id="donateButton_100" className="col-sm-1 btn btn-primary express-button" onClick={(e) => this.handleDonate(10000, '100')}>$100</button>
              <button id="donateButton_200" className="col-sm-1 btn btn-primary express-button" onClick={(e) => this.handleDonate(20000, '200')}>$200</button>
              <button id="donateButton_300" className="col-sm-1 btn btn-primary express-button" onClick={(e) => this.handleDonate(30000, '300')}>$300</button>
            </div>
          </div>

          {allowSpecificAmount ?
            <div>
              <h5>Or a specific amount:</h5>
              <form>
                <div id="alertCustomDonation" role="alert" className="hidden alert alert-danger">
                  <span className="glyphicon glyphicon-exclamation-sign" aria-hidden="true"/>
                  <span className="sr-only">Error:</span>
                  <span id="alertCustomDonationText"/>
                </div>
                <fieldset className="form-group">
                  <div className="input-group">
                    <span className="input-group-addon">$</span>
                    <input type="number" className="form-control" id="donationAmount" name="amount" placeholder="Enter amount" style={{width: '150px'}} min="1"/>
                    <button id="donateButton_custom" className="btn btn-primary" style={{marginLeft: '-2px'}}>Donate</button>
                  </div>
                </fieldset>
              </form>
            </div>
            : ""}
        </div>
        <br/>
        <br/>
        <div>
          <p className="text-muted">
            <small>Apple Pay available in Safari web browser only (on supported devices and operating system versions).</small>
          </p>
        </div>

        <div>
          <div id="errorDialog" className="modal fade" tabIndex={-1} role="dialog">
            <div className="modal-dialog" role="document">
              <div className="modal-content">
                <div className="modal-header">
                  <button type="button" className="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                  <h4 className="modal-title">Payment Failed</h4>
                </div>
                <div className="modal-body">
                  <p id="errorMessage"/>
                </div>
                <div className="modal-footer">
                  <button type="button" className="btn btn-default" data-dismiss="modal">Close</button>
                </div>
              </div>
            </div>
          </div>

          <div id="processingPaymentDialog" className="modal fade" tabIndex={-1} role="dialog">
            <div className="modal-dialog" role="document">
              <div className="modal-content">
                <div className="modal-header">
                  <button type="button" className="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                  <h4 className="modal-title">Processing your payment...</h4>
                </div>
              </div>
            </div>
          </div>

          <div id="multi-pay-options" className="modal fade" tabIndex={-1} role="dialog">
            <div className="modal-dialog" role="document">
              <div className="modal-content">
                <div className="modal-header">
                  <h3 style={{textAlign: 'center'}}>Choose payment method</h3>
                  <br/>
                  {app.applePayEnabled ?
                    <div className="apple-pay-container">
                      <button id="apple-pay-button" className="apple-pay-button"/>
                      <br/>
                    </div>
                    : ""}
                  <button id="credit-pay-button" type="button" className="credit-pay-button btn btn-default" aria-label="Center Align" onClick={(e) => this.doCreditCardDonate()}>
                    <span className="glyphicon glyphicon-credit-card" aria-hidden="true"/>
                    Credit Card
                  </button>
                  {app.paypalEnabled ?
                    <div>
                      <br/>
                      <div id="paypal-button-container"/>
                    </div>
                    : ""}
                  <br/>
                  <button id="cancel-button" type="button" className="credit-pay-button btn btn-default" aria-label="Center Align">
                    Cancel
                  </button>
                  <br/>
                  <br/>
                  <div id="donationAmountAlert" className="alert alert-info" role="alert"/>

                </div>
              </div>
            </div>
          </div>
        </div>

      </div>
    );
  }
}
