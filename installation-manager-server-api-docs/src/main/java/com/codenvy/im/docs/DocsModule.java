/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.im.docs;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.wordnik.swagger.config.ConfigFactory;
import com.wordnik.swagger.config.ScannerFactory;
import com.wordnik.swagger.config.SwaggerConfig;
import com.wordnik.swagger.jaxrs.config.DefaultJaxrsScanner;
import com.wordnik.swagger.jaxrs.listing.ApiDeclarationProvider;
import com.wordnik.swagger.jaxrs.listing.ApiListingResource;
import com.wordnik.swagger.jaxrs.listing.ResourceListingProvider;
import com.wordnik.swagger.jaxrs.reader.DefaultJaxrsApiReader;
import com.wordnik.swagger.reader.ClassReaders;
import org.eclipse.che.inject.DynaModule;

import javax.annotation.PostConstruct;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * @author andrew00x
 * @author Dmytro Nochevnov
 */
@DynaModule
public class DocsModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(InstallationManagerApiDocsService.class);
        bind(ResourceListingProvider.class);
        bind(ApiDeclarationProvider.class);
        bind(SwaggerBootstrap.class).asEagerSingleton();
        // CodenvyJsonProvider isn't able to serve Scala model objects.
        // Swagger's JSON stack processes all swagger models.
        final Multibinder<Class> ignoredCodenvyJsonClasses =
                Multibinder.newSetBinder(binder(), Class.class, Names.named("codenvy.json.ignored_classes"));
        // com.wordnik.swagger.model.SwaggerModels.scala
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.ResourceListing.class);
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.ApiInfo.class);
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.LoginEndpoint.class);
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.TokenRequestEndpoint.class);
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.TokenEndpoint.class);
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.ApiListingReference.class);
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.AllowableValues.class);
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.AnyAllowableValues.class);
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.AllowableListValues.class);
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.AllowableRangeValues.class);
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.Model.class);
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.ModelProperty.class);
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.ModelRef.class);
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.ApiListing.class);
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.ApiDescription.class);
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.Operation.class);
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.Parameter.class);
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.ResponseMessage.class);
        // com.wordnik.swagger.model.AuthorizationModels.scala
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.AuthorizationType.class);
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.OAuthBuilder.class);
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.Authorization.class);
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.AuthorizationScope.class);
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.OAuth.class);
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.GrantType.class);
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.ApiKey.class);
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.BasicAuth.class);
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.ImplicitGrant.class);
        ignoredCodenvyJsonClasses.addBinding().toInstance(com.wordnik.swagger.model.AuthorizationCodeGrant.class);
    }

    @Path("/docs")
    @Produces("application/json")
    public static class InstallationManagerApiDocsService extends ApiListingResource {
    }

    static class SwaggerBootstrap {
        final String BASE_SERVER_URL = "/im";

        @PostConstruct
        public void init() {
            final SwaggerConfig config = ConfigFactory.config();
            config.setBasePath(BASE_SERVER_URL);
            config.setApiVersion("1.0");
            final com.wordnik.swagger.model.ApiInfo apiInfo = new com.wordnik.swagger.model.ApiInfo(
                "Installation Manager REST API", // title
                "", // description
                "", // termsOfServiceUrl
                "", // contacts
                "Eclipse Public License v1.0", // license
                "http://www.eclipse.org/legal/epl-v10.html"  // license URL
            );
            config.setApiInfo(apiInfo);
            ScannerFactory.setScanner(new DefaultJaxrsScanner());
            ClassReaders.setReader(new DefaultJaxrsApiReader());
        }
    }
}
