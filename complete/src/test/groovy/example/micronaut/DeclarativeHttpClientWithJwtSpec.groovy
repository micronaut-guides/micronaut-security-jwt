package example.micronaut

import io.micronaut.context.ApplicationContext
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.RxHttpClient
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.security.token.jwt.render.BearerAccessRefreshToken
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

class DeclarativeHttpClientWithJwtSpec extends Specification {

    @Shared
    @AutoCleanup // <1>
    EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer) // <2>

    @Shared
    @AutoCleanup
    RxHttpClient client = embeddedServer.applicationContext.createBean(RxHttpClient, embeddedServer.getURL()) // <3>

    def "Verify JWT authentication works with declarative @Client"() {
        when:
        AppClient appClient = embeddedServer.applicationContext.getBean(AppClient) // <4>

        then:
        noExceptionThrown()

        when: 'Accessing a secured URL without authenticating'
        client.toBlocking().exchange(HttpRequest.GET('/', )) // <5>

        then: 'returns unauthorized'
        HttpClientResponseException e = thrown(HttpClientResponseException)
        e.status == HttpStatus.UNAUTHORIZED

        when: 'Login endpoint is called with valid credentials'
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials("sherlock", "password")
        HttpRequest request = HttpRequest.POST('/login', creds) // <6>
        HttpResponse<BearerAccessRefreshToken> rsp = client.toBlocking().exchange(request, BearerAccessRefreshToken) // <7>

        then: 'the endpoint can be accessed'
        rsp.status == HttpStatus.OK
        rsp.body().accessToken

        when:
        String accessToken = rsp.body().accessToken
        String authorizationValue = "Bearer $accessToken"
        String msg = appClient.home(authorizationValue)

        then:
        msg == 'sherlock' // <8>
    }
}
