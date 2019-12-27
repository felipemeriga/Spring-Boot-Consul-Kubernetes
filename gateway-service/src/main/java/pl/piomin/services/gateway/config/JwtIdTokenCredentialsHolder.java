package pl.piomin.services.gateway.config;

/**
 *
 * Request scoped bean that holds the IDToken associated with the user.
 *
 */
public class JwtIdTokenCredentialsHolder {

    public String getIdToken() {
        return idToken;
    }

    public JwtIdTokenCredentialsHolder setIdToken(String idToken) {
        this.idToken = idToken;
        return this;
    }

    private String idToken;

}
