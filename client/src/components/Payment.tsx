import axios from "axios";
import * as React from "react";

interface AppConfig {
  mainPageUrl?: string;
  siteTitle?: string;
  appPreviewImageUrl?: string;
  publishableKey?: string;
  organizationDisplayName?: string;
  applePayEnabled?: boolean;
  clientLoggingEnabled?: boolean;
  collectOccupationEnabled?: boolean;
  collectOccupationThreshold?: boolean;
  donationLimit?: number;
  paypalEnabled?: boolean;
  paypalSandbox?: boolean;
  emailSignupUrl?: string;
  vcsBuildId?: string;
}

interface State {
  appConfig: AppConfig
}

export default class Payment extends React.Component<{}, State> {
  constructor(props: {}) {
    super(props);
    this.state = {appConfig: {}};
  }

  public componentDidMount() {
    axios.get('getConfig')
      .then(res => {
        const data: AppConfig = res.data;
        this.setState({appConfig: data});
      })
  }

  private handleDonate(cents: number, dollars: string) {
    return undefined;
  }

  public render() {
    const app = this.state.appConfig;
    const allowSpecificAmount: boolean = true;
    const collectOccupation: boolean = app.collectOccupationEnabled || false; // XXX This will vary based on configuration

    // TODO Dynamically generate buttons based on configuration

    return (
      <div className="container">
        <div id="alertTop" className="alert-top" role="alert"/>
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
              <button id="donateButton_5" className="col-sm-1 btn btn-primary express-button" onClick={this.handleDonate(500, '5')}>$5</button>
              <button id="donateButton_10" className="col-sm-1 btn btn-primary express-button" onClick={this.handleDonate(1000, '10')}>$10</button>
              <button id="donateButton_18" className="col-sm-1 btn btn-primary express-button" onClick={this.handleDonate(1800, '18')}>$18</button>
              <button id="donateButton_27" className="col-sm-1 btn btn-primary express-button" onClick={this.handleDonate(2700, '27')}>$27</button>
            </div>
            <div className="row express-button-row">
              <button id="donateButton_50" className="col-sm-1 btn btn-primary express-button" onClick={this.handleDonate(5000, '50')}>$50</button>
              <button id="donateButton_100" className="col-sm-1 btn btn-primary express-button" onClick={this.handleDonate(10000, '100')}>$100</button>
              <button id="donateButton_200" className="col-sm-1 btn btn-primary express-button" onClick={this.handleDonate(20000, '200')}>$200</button>
              <button id="donateButton_300" className="col-sm-1 btn btn-primary express-button" onClick={this.handleDonate(30000, '300')}>$300</button>
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
                  <div className="apple-pay-container">
                    <button id="apple-pay-button" className="apple-pay-button"/>
                    <br/>
                  </div>
                  <button id="credit-pay-button" type="button" className="credit-pay-button btn btn-default" aria-label="Center Align">
                    <span className="glyphicon glyphicon-credit-card" aria-hidden="true"/>
                    Credit Card
                  </button>
                  {app.applePayEnabled ?
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
