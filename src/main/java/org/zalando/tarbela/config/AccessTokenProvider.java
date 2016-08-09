package org.zalando.tarbela.config;

import java.io.File;

import java.net.URI;

import java.util.List;
import java.util.Map;

import org.zalando.stups.tokens.AccessTokenConfiguration;
import org.zalando.stups.tokens.AccessTokens;
import org.zalando.stups.tokens.AccessTokensBuilder;
import org.zalando.stups.tokens.JsonFileBackedClientCredentialsProvider;
import org.zalando.stups.tokens.JsonFileBackedUserCredentialsProvider;
import org.zalando.stups.tokens.Tokens;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AccessTokenProvider {
    private static final String CLIENT_CREDENTIAL_FILE_NAME = "client.json";

    private static final String USER_CREDENTIAL_FILE_NAME = "user.json";

    private Map<String, List<String>> producerToScopesMap;

    private AccessTokens accessTokens;

    private final String credentialDirectory;

    private final String authenticationEndpointUrl;

    public AccessTokenProvider(final Map<String, List<String>> producerToScopesMap, final String credentialDirectory,
            final String authenticationEndpointUrl) {
        this.producerToScopesMap = producerToScopesMap;
        this.credentialDirectory = credentialDirectory;
        this.authenticationEndpointUrl = authenticationEndpointUrl;

        accessTokens = initAccessTokens();
    }

    private AccessTokens initAccessTokens() {
        log.info("init token rotation for credential directory: {}", credentialDirectory);

        final JsonFileBackedClientCredentialsProvider clientCredentialsProvider =
            new JsonFileBackedClientCredentialsProvider(new File(credentialDirectory, CLIENT_CREDENTIAL_FILE_NAME));
        final JsonFileBackedUserCredentialsProvider userCredentialsProvider = new JsonFileBackedUserCredentialsProvider(
                new File(credentialDirectory, USER_CREDENTIAL_FILE_NAME));

        final AccessTokensBuilder accessTokenBuilder = createTokensBuilder(authenticationEndpointUrl)
                .usingClientCredentialsProvider(clientCredentialsProvider).usingUserCredentialsProvider(
                                                                                                         userCredentialsProvider);

        for (final Map.Entry<String, List<String>> scopeEntry : producerToScopesMap.entrySet()) {
            log.info("requested sopes: {}", scopeEntry);

            final AccessTokenConfiguration scopeConfiguration = accessTokenBuilder.manageToken(scopeEntry.getKey());
            scopeConfiguration.addScopes(scopeEntry.getValue());

            scopeConfiguration.done();
        }

        return accessTokenBuilder.start();
    }

    // @VisibleForTesting
    AccessTokensBuilder createTokensBuilder(final String endpointUrl) {
        return Tokens.createAccessTokensWithUri(URI.create(endpointUrl));
    }

    public AccessTokens getAccessTokens() {
        return accessTokens;
    }
}
