package demo

import io.micronaut.context.ApplicationContext
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.security.token.jwt.render.BearerAccessRefreshToken
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import io.micronaut.http.client.RxHttpClient
import io.micronaut.http.HttpRequest

class JwtAuthenticationSpec extends Specification {

    @Shared
    @AutoCleanup
    EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer)

    @Shared
    @AutoCleanup
    RxHttpClient client = embeddedServer.applicationContext.createBean(RxHttpClient, embeddedServer.getURL())

    def "verify JWT authentication works"() {
        when: 'Accessing a secured URL without authenticating'
        client.toBlocking().exchange(HttpRequest.GET('/', ))

        then: 'returns unauthorized'
        HttpClientResponseException e = thrown(HttpClientResponseException)
        e.status == HttpStatus.UNAUTHORIZED

        when: 'login endpoint is called with valid credentials'
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials("sherlock", "password")
        HttpRequest request = HttpRequest.POST('/login', creds)
        HttpResponse<BearerAccessRefreshToken> rsp = client.toBlocking().exchange(request, BearerAccessRefreshToken)

        then: 'the endpoint can be accessed'
        rsp.status == HttpStatus.OK
        rsp.body().username == 'sherlock'

        when:
        String accessToken = rsp.body().accessToken
        HttpRequest requestWithAuthorization = HttpRequest.GET('/' ).header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
        HttpResponse<String> response = client.toBlocking().exchange(requestWithAuthorization, String)

        then:
        response.status == HttpStatus.OK
        response.body() == 'sherlock'
    }
}
