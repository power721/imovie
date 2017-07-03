package org.har01d.imovie.web.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnClass(EnableAuthorizationServer.class)
@Conditional(OAuth2JwtKeyStoreConfiguration.JwtKeyStoreCondition.class)
@EnableConfigurationProperties(AuthorizationServerJwtProperties.class)
public class OAuth2JwtKeyStoreConfiguration {

    private final ApplicationContext context;
    private final AuthorizationServerJwtProperties properties;

    @Autowired
    public OAuth2JwtKeyStoreConfiguration(AuthorizationServerJwtProperties properties, ApplicationContext context) {
        this.context = context;
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean(TokenStore.class)
    public TokenStore tokenStore() {
        return new JwtTokenStore(accessTokenConverter());
    }

    @Bean
    @ConditionalOnMissingBean(AccessTokenConverter.class)
    public JwtAccessTokenConverter accessTokenConverter() {
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(context.getResource(properties.getKeyStore()),
            properties.getPassword().toCharArray());
        converter.setKeyPair(keyStoreKeyFactory.getKeyPair(properties.getAlias()));
        return converter;
    }

    public static class JwtKeyStoreCondition extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            ConditionMessage.Builder message = ConditionMessage.forCondition("OAuth JWT keystore Condition");
            RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(context.getEnvironment(),
                "security.oauth2.authorization.jwt.");
            String keyStore = resolver.getProperty("key-store");
            String password = resolver.getProperty("password");
            String alias = resolver.getProperty("alias");
            if (StringUtils.hasText(keyStore) && StringUtils.hasText(password) && StringUtils.hasText(alias)) {
                return ConditionOutcome.match(message.foundExactly("provided jwt key store"));
            }
            return ConditionOutcome.noMatch(message.didNotFind("provided jwt key store").atAll());
        }
    }

}
