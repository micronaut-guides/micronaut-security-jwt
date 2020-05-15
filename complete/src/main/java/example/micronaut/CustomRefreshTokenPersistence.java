package example.micronaut;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.security.authentication.UserDetails;
import io.micronaut.security.token.event.RefreshTokenGeneratedEvent;
import io.micronaut.security.token.refresh.RefreshTokenPersistence;
import io.micronaut.security.errors.OauthErrorResponseException;
import io.micronaut.security.errors.IssuingAnAccessTokenErrorCode;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Optional;

@Singleton // <1>
public class CustomRefreshTokenPersistence implements RefreshTokenPersistence {
    private final RefreshTokenRepository refreshTokenRepository;

    public CustomRefreshTokenPersistence(RefreshTokenRepository refreshTokenRepository) {  // <2>
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    @EventListener  // <3>
    public void persistToken(RefreshTokenGeneratedEvent event) {
        if (event != null &&
                event.getRefreshToken() != null &&
                event.getUserDetails() != null &&
                event.getUserDetails().getUsername() != null) {
            refreshTokenRepository.save(event.getUserDetails() .getUsername(), event.getRefreshToken(), Boolean.FALSE); // <4>
        }
    }

    @Override
    public Publisher<UserDetails> getUserDetails(String refreshToken) {
        Optional<RefreshTokenEntity> tokenOpt = refreshTokenRepository.findByRefreshToken(refreshToken);
        if (tokenOpt.isPresent()) {
            RefreshTokenEntity token = tokenOpt.get();
            if (token.getRevoked()) {
                return Publishers.just(new OauthErrorResponseException(IssuingAnAccessTokenErrorCode.INVALID_GRANT, "refresh token revoked", null)); // <5>
            }
             return Flowable.just(new UserDetails(token.getUsername(), new ArrayList<>())); // <6>
        }
        return Publishers.just(new OauthErrorResponseException(IssuingAnAccessTokenErrorCode.INVALID_GRANT, "refresh token not found", null)); // <7>
    }
}
