package example.micronaut

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.RxHttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.security.token.jwt.render.BearerAccessRefreshToken
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Specification

import javax.inject.Inject

@MicronautTest // <1>
class DeclarativeHttpClientWithJwtSpec extends Specification {

    @Inject
    EmbeddedServer embeddedServer // <2>

    @Inject
    @Client("/")
    RxHttpClient client // <3>

    def "Verify JWT authentication works with declarative @Client"() {
        when:
        AppClient appClient = embeddedServer.applicationContext.getBean(AppClient) // <4>

        then:
        noExceptionThrown()

        when: 'Accessing a secured URL without authenticating'
        client.toBlocking().exchange(HttpRequest.GET('/', ))

        then: 'returns unauthorized'
        HttpClientResponseException e = thrown(HttpClientResponseException)
        e.status == HttpStatus.UNAUTHORIZED

        when: 'Login endpoint is called with valid credentials'
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials("sherlock", "password")
        HttpRequest request = HttpRequest.POST('/login', creds) // <5>
        HttpResponse<BearerAccessRefreshToken> rsp = client.toBlocking().exchange(request, BearerAccessRefreshToken) // <6>

        then: 'the endpoint can be accessed'
        rsp.status == HttpStatus.OK
        rsp.body().accessToken

        when:
        String accessToken = rsp.body().accessToken
        String authorizationValue = "Bearer $accessToken"
        String msg = appClient.home(authorizationValue) // <7>

        then:
        msg == 'sherlock' // <8>
    }
}
