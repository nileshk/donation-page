import axios from "axios";
import * as React from "react";

interface AppConfig {
  publishableKey?: string,
  organizationDisplayName?: string
  applePayEnabled?: boolean
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

  public render() {
    const appConfig = this.state.appConfig;
    return (
      <div>
        <h1>{appConfig.organizationDisplayName}</h1>
        Apply Pay Enabled: {appConfig.applePayEnabled ? 'Yes' : 'No'}<br />
        Stripe key: {appConfig.publishableKey}<br />
      </div>
    );
  }
}
