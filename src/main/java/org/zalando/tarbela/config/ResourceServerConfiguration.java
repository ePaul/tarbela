package org.zalando.tarbela.config;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

import javax.annotation.Nonnull;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableResourceServer
public class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

    @Value("${server.port}")
    private int managementPort;

    /**
     * Configure scopes for specific controller/httpmethods/roles here.
     */
    @Override
    public void configure(final HttpSecurity http) throws Exception {
        //J-
        http
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.NEVER)
            .and()
            .authorizeRequests()
                .requestMatchers(forPortAndPath(managementPort, "/info")).permitAll()
                .requestMatchers(forPortAndPath(managementPort, "/health")).permitAll()
                .requestMatchers(forPortAndPath(managementPort, "/metrics")).permitAll()

                .anyRequest().denyAll();
        //J+
    }

    /**
     * Creates a request matcher which only matches requests for a specific local port and path (using an
     * {@link AntPathRequestMatcher} for the path part).
     *
     * @param   port         the port to match
     * @param   pathPattern  the pattern for the path.
     *
     * @return  the new request matcher.
     */
    private RequestMatcher forPortAndPath(final int port, @Nonnull final String pathPattern) {
        return new AndRequestMatcher(forPort(port), new AntPathRequestMatcher(pathPattern));
    }

    /**
     * A request matcher which matches just a port.
     *
     * @param   port  the port to match.
     *
     * @return  the new matcher.
     */
    private RequestMatcher forPort(final int port) {
        return (HttpServletRequest request) -> { return port == request.getLocalPort(); };
    }

    /**
     * A dummy resource server token service bean. Don't use this for any app with real services, as it will accept any
     * token. It is okay for our use case, as our three paths are "permit all" (and should only be available inside the
     * VPN).
     */
    @Bean
    public ResourceServerTokenServices customResourceTokenServices() {
        return new ResourceServerTokenServices() {
            @Override
            public OAuth2AccessToken readAccessToken(final String accessToken) {
                return new DefaultOAuth2AccessToken(accessToken);
            }

            @Override
            public OAuth2Authentication loadAuthentication(final String accessToken) {
                return new OAuth2Authentication(new OAuth2Request(emptyMap(), "dummy", emptySet(), true, emptySet(),
                            emptySet(), "dummyUri", emptySet(), emptyMap()), null);
            }
        };
    }
}
