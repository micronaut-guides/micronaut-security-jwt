package example.micronaut

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.RxHttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.security.token.jwt.endpoints.TokenRefreshRequest
import io.micronaut.security.token.jwt.render.AccessRefreshToken
import io.micronaut.security.token.jwt.render.BearerAccessRefreshToken
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Specification

import javax.inject.Inject

@MicronautTest
class OauthAccessTokenSpec extends Specification {

    @Inject
    EmbeddedServer embeddedServer

    @Inject
    @Client("/")
    RxHttpClient client

    def "Verify JWT access token refresh works"() {
        when: 'login endpoint is called with valid credentials'
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials("sherlock", "password")
        HttpRequest request = HttpRequest.POST('/login', creds)
        HttpResponse<BearerAccessRefreshToken> rsp = client.toBlocking().exchange(request, BearerAccessRefreshToken)

        then: 'the endpoint can be accessed'
        rsp.status == HttpStatus.OK

        when:
        sleep(1_000) // sleep for one second to give time for the issued at `iat` Claim to change
        String refreshToken = rsp.body().refreshToken
        String accessToken = rsp.body().accessToken

        HttpResponse<AccessRefreshToken> response = client.toBlocking().exchange(HttpRequest.POST('/oauth/access_token',
                new TokenRefreshRequest("refresh_token", refreshToken)), AccessRefreshToken) // <1>

        then:
        response.status == HttpStatus.OK
        response.body().accessToken
        response.body().accessToken != accessToken // <2>
    }
}
