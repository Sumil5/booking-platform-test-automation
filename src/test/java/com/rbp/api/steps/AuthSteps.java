package com.rbp.api.steps;

import com.rbp.api.auth.TokenVariant;
import com.rbp.api.client.AuthClient;
import com.rbp.api.config.Config;
import com.rbp.api.context.ScenarioContext;
import com.rbp.api.data.TestDataFactory;
import io.cucumber.java.en.When;

public class AuthSteps {

    private final ScenarioContext context;
    private final AuthClient auth;

    public AuthSteps(ScenarioContext context, AuthClient auth) {
        this.context = context;
        this.auth = auth;
    }

    @When("I log in with the configured admin credentials")
    public void iLogInWithTheConfiguredAdminCredentials() {
        Config config = Config.get();
        context.lastResponse(auth.login(config.adminUsername(), config.adminPassword()));
    }

    @When("I log in as the admin user with a wrong password")
    public void iLogInAsTheAdminUserWithAWrongPassword() {
        context.lastResponse(auth.login(Config.get().adminUsername(), "wrong-" + TestDataFactory.uniqueId()));
    }

    @When("I log in as an unknown user")
    public void iLogInAsAnUnknownUser() {
        context.lastResponse(auth.login("unknown-" + TestDataFactory.uniqueId(), Config.get().adminPassword()));
    }

    @When("I log in with an empty username and password")
    public void iLogInWithAnEmptyUsernameAndPassword() {
        context.lastResponse(auth.login("", ""));
    }

    @When("I validate {token} token")
    public void iValidateToken(TokenVariant token) {
        context.lastResponse(auth.validate(token.resolve()));
    }
}
