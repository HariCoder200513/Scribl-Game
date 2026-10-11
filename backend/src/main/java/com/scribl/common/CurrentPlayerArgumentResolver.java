package com.scribl.common;

import com.scribl.player.Player;
import com.scribl.player.PlayerRepository;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves {@link CurrentPlayer} parameters to the {@link Player} whose token
 * is in {@link RequestHeaders#PLAYER_TOKEN}, or fails with 401. This is how the
 * server derives player identity instead of trusting client-supplied ids.
 */
@Component
public class CurrentPlayerArgumentResolver implements HandlerMethodArgumentResolver {

    private final PlayerRepository players;

    public CurrentPlayerArgumentResolver(PlayerRepository players) {
        this.players = players;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentPlayer.class)
                && parameter.getParameterType().equals(Player.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String token = webRequest.getHeader(RequestHeaders.PLAYER_TOKEN);
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("missing player token");
        }
        return players.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("invalid player token"));
    }
}