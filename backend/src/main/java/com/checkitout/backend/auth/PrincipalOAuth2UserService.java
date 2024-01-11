package com.checkitout.backend.auth;

import static com.checkitout.backend.auth.enumstorage.messages.error.oauth2.OAuth2ErrorMessage.INVALID_EMAIL;
import static com.checkitout.backend.auth.enumstorage.messages.error.oauth2.OAuth2ErrorMessage.NOT_VERIFIED_EMAIL;
import static com.checkitout.backend.auth.enumstorage.messages.error.oauth2.OAuth2ErrorMessage.NO_ATTRIBUTE;
import static com.checkitout.backend.auth.enumstorage.messages.error.oauth2.OAuth2ErrorMessage.NO_EMAIL;
import static com.checkitout.backend.auth.enumstorage.messages.error.oauth2.OAuth2ErrorMessage.NO_KAKAO_ACCOUNT;
import static com.checkitout.backend.auth.enumstorage.messages.error.oauth2.OAuth2ErrorMessage.NO_PROVIDER_ID;
import static com.checkitout.backend.auth.enumstorage.messages.error.oauth2.OAuth2ErrorMessage.NO_SUCH_OAUTH_2;

import com.checkitout.backend.auth.entity.OAuth2;
import com.checkitout.backend.auth.exception.oauth2.NoSuchOAuth2Exception;
import com.checkitout.backend.auth.userdetail.PrincipalUserDetails;
import com.checkitout.backend.entity.Member;
import com.checkitout.backend.repository.MemberRepository;
import com.checkitout.backend.repository.OAuth2Repository;
import com.checkitout.backend.util.RandomNickname;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PrincipalOAuth2UserService extends DefaultOAuth2UserService {
    private final MemberRepository memberRepository;
    private final OAuth2Repository oAuth2Repository;
    private final HttpServletRequest request;

    /**
     * OAuth2UserRequest를 받아서,
     * 회원이 있으면 바로 PrincipalDetails를 리턴하고,
     * 없으면 회원을 생성하고 PrincipalDetails를 리턴한다.
     * @param userRequest
     * @return
     * @throws OAuth2AuthenticationException
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = Optional.ofNullable(oAuth2User.getAttributes())
            .orElseThrow(() -> new OAuth2AuthenticationException(NO_ATTRIBUTE.getMessage()));

        Map<String, Object> kakaoAccount = Optional.ofNullable((Map<String, Object>) attributes.get("kakao_account"))
            .orElseThrow(() -> new OAuth2AuthenticationException(NO_KAKAO_ACCOUNT.getMessage()));

        // Email
        String email = getEmail(kakaoAccount);
        request.setAttribute("email", email);

        // providerId는 각각의 provider에서 제공하는 유저의 고유 Id
        Long providerId = Optional.ofNullable((Long) attributes.get("id"))
            .orElseThrow(() -> new OAuth2AuthenticationException(NO_PROVIDER_ID.getMessage()));

        // registrationId는 우리가 등록한 provider의 이름 (Ex. google, kakao, naver)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        try {
            // registrationId와 providerId로 OAuth2를 찾는다.
            OAuth2 oAuth2 = oAuth2Repository.findByRegistrationIdAndProviderId(registrationId, providerId)
                    .orElseThrow(() -> new NoSuchOAuth2Exception(NO_SUCH_OAUTH_2.getMessage()));

            // 있으면, 연관된 Member를 찾는다.
            Member member = oAuth2.getMember();

            // PrincipalDetails를 리턴한다.
            return new PrincipalUserDetails(member, oAuth2User.getAttributes());
        }
        // OAuth2가 없다는건, 처음 로그인하는 회원이라는 뜻이다.
        catch (NoSuchOAuth2Exception exception) {
            // Member를 생성한다.
            Member member = Member.builder()
                    .email(email)
                    .nickname(RandomNickname.getRandomNickname())
                    .build();

            // OAuth2를 생성한다.
            OAuth2 oAuth2 = OAuth2.builder()
                .member(member)
                .registrationId(registrationId)
                .providerId(providerId)
                .accessToken(userRequest.getAccessToken().getTokenValue())
                .build();

            // Member를 저장한다.
            memberRepository.save(member);

            // OAuth2를 저장한다.
            oAuth2Repository.save(oAuth2);

            // PrincipalDetails를 리턴한다.
            return new PrincipalUserDetails(member, oAuth2User.getAttributes());
        }
    }

    private String getEmail(Map<String, Object> kakaoAccount) {
        // Email 존재 확인
        Boolean hasEmail = Optional.ofNullable((Boolean) kakaoAccount.get("has_email"))
            .orElseThrow(() -> new OAuth2AuthenticationException(NO_EMAIL.getMessage()));
        if (!hasEmail) {
            throw new OAuth2AuthenticationException(NO_EMAIL.getMessage());
        }

        // Email 유효 확인
        Boolean isEmailValid = Optional.ofNullable((Boolean) kakaoAccount.get("is_email_valid"))
            .orElseThrow(() -> new OAuth2AuthenticationException(INVALID_EMAIL.getMessage()));
        if (!isEmailValid) {
            throw new OAuth2AuthenticationException(INVALID_EMAIL.getMessage());
        }

        // Email 인증 확인
        Boolean isEmailVerified = Optional.ofNullable((Boolean) kakaoAccount.get("is_email_verified"))
            .orElseThrow(() -> new OAuth2AuthenticationException(NOT_VERIFIED_EMAIL.getMessage()));
        if (!isEmailVerified) {
            throw new OAuth2AuthenticationException(NOT_VERIFIED_EMAIL.getMessage());
        }

        return Optional.ofNullable((String) kakaoAccount.get("email"))
            .orElseThrow(() -> new OAuth2AuthenticationException(NO_EMAIL.getMessage()));
    }
}
