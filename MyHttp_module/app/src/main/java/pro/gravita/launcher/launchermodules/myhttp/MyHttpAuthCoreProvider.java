package pro.gravita.launcher.launchermodules.myhttp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.events.request.GetAvailabilityAuthRequestEvent;
import pro.gravit.launcher.profiles.Texture;
import pro.gravit.launcher.request.RequestException;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.details.AuthPasswordDetails;
import pro.gravit.launcher.request.auth.details.AuthTotpDetails;
import pro.gravit.launcher.request.auth.password.Auth2FAPassword;
import pro.gravit.launcher.request.auth.password.AuthPlainPassword;
import pro.gravit.launcher.request.auth.password.AuthTOTPPassword;
import pro.gravit.launchserver.HttpRequester;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.launchserver.auth.core.User;
import pro.gravit.launchserver.auth.core.UserSession;
import pro.gravit.launchserver.auth.core.interfaces.user.UserSupportTextures;
import pro.gravit.launchserver.auth.texture.JsonTextureProvider;
import pro.gravit.launchserver.helper.HttpHelper;
import pro.gravit.launchserver.manangers.AuthManager;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MyHttpAuthCoreProvider extends AuthCoreProvider {
    private static final int CODE_TOKEN_EXPIRED = 1001;
    private static final int CODE_INVALID_REFRESH_TOKEN = 1002;
    private transient final Logger logger = LogManager.getLogger();
    public String userByUsername;
    public String userByUuid;
    public String refreshAccessToken;
    public String authorize;
    public String checkServer;
    public String joinServer;
    public String bearerToken;
    private transient HttpRequester requester;
    @Override
    public User getUserByUsername(String username) {
        try {
            var response = requester.send(requester.get(userByUsername.replace("%username%", URLEncoder.encode(username, StandardCharsets.UTF_8)), bearerToken),
                    MyHttpUser.class);
            if(response.isSuccessful()) {
                return response.result();
            }
            if(response.statusCode() == 404) {
                return null;
            }
            logger.error("getUserByUsername: {}", response.error().toString());
            return null;
        } catch (Throwable e) {
            logger.error("getUserByUsername", e);
            return null;
        }
    }

    @Override
    public User getUserByUUID(UUID uuid) {
        try {
            var response = requester.send(requester.get(userByUsername.replace("%uuid%", uuid.toString()), bearerToken),
                    MyHttpUser.class);
            if(response.isSuccessful()) {
                return response.result();
            }
            if(response.statusCode() == 404) {
                return null;
            }
            logger.error("getUserByUUID: {}", response.error().toString());
            return null;
        } catch (Throwable e) {
            logger.error("getUserByUUID", e);
            return null;
        }
    }

    @Override
    public UserSession getUserSessionByOAuthAccessToken(String accessToken) throws OAuthAccessTokenExpired {
        HttpHelper.HttpOptional<MyHttpUserSession, HttpRequester.SimpleError> response;
        try {
            response = requester.send(requester.post(userByUuid, new GetUserByAccessTokenRequest(accessToken), bearerToken),
                    MyHttpUserSession.class);
        } catch (Throwable e) {
            logger.error("getUserSessionByOAuthAccessToken", e);
            return null;
        }
        if(response.isSuccessful()) {
            return response.result();
        }
        if(response.error().code == CODE_TOKEN_EXPIRED) {
            throw new OAuthAccessTokenExpired();
        }
        logger.error("getUserSessionByOAuthAccessToken: {}", response.error().toString());
        return null;
    }

    @Override
    public AuthManager.AuthReport refreshAccessToken(String refreshToken, AuthResponse.AuthContext context) {
        HttpHelper.HttpOptional<MyHttpUserSession, HttpRequester.SimpleError> response;
        try {
            response = requester.send(requester.post(refreshAccessToken, new RefreshTokenRequest(refreshToken), bearerToken),
                    MyHttpUserSession.class);
        } catch (Throwable e) {
            logger.error("refreshAccessToken", e);
            return null;
        }
        if(response.isSuccessful()) {
            return response.result().toAuthReport();
        }
        if(response.error().code == CODE_INVALID_REFRESH_TOKEN) {
            return null;
        }
        logger.error("refreshAccessToken: {}", response.error().toString());
        return null;
    }

    @Override
    public AuthManager.AuthReport authorize(String login, AuthResponse.AuthContext context, AuthRequest.AuthPasswordInterface password, boolean minecraftAccess) throws IOException {
        if(login == null || password == null) {
            throw new AuthException("Empty login or password");
        }
        String plainPassword;
        String totpCode;
        if(password instanceof AuthPlainPassword authPlainPassword) {
            plainPassword = authPlainPassword.password;
            totpCode = null;
        } else if(password instanceof Auth2FAPassword auth2FAPassword) {
            if(auth2FAPassword.firstPassword instanceof AuthPlainPassword authPlainPassword) {
                plainPassword = authPlainPassword.password;
            } else {
                throw new AuthException("Unsupported password type (first)");
            }
            if(auth2FAPassword.secondPassword instanceof AuthTOTPPassword authTOTPPassword) {
                totpCode = authTOTPPassword.totp;
            } else {
                throw new AuthException("Unsupported password type (second)");
            }
        } else {
            throw new AuthException("Unsupported password type");
        }
        HttpHelper.HttpOptional<MyHttpUserSession, HttpRequester.SimpleError> response;
        try {
            response = requester.send(requester.post(authorize, new AuthorizeRequest(login, plainPassword, totpCode), bearerToken),
                    MyHttpUserSession.class);
        } catch (Throwable e) {
            logger.error("authorize", e);
            throw new AuthException("Unexpected server error. Please contact administrator");
        }
        if(response.isSuccessful()) {
            return response.result().toAuthReport();
        }
        throw new AuthException(response.error().error);
    }

    @Override
    public void init(LaunchServer server) {
        requester = new HttpRequester();
    }

    @Override
    public User checkServer(Client client, String username, String serverID) throws IOException {
        HttpHelper.HttpOptional<MyHttpUser, HttpRequester.SimpleError> response;
        try {
            response = requester.send(requester.post(checkServer, new CheckServerRequest(username, serverID), bearerToken),
                    MyHttpUser.class);
        } catch (Throwable e) {
            logger.error("refreshAccessToken", e);
            throw new AuthException("Unexpected server error. Please contact administrator");
        }
        if(response.isSuccessful()) {
            return response.result();
        }
        throw new AuthException(response.error().error);
    }

    @Override
    public boolean joinServer(Client client, String username, UUID uuid, String accessToken, String serverID) throws IOException {
        HttpHelper.HttpOptional<Void, HttpRequester.SimpleError> response;
        try {
            response = requester.send(requester.post(joinServer, new JoinServerRequest(username, uuid, accessToken, serverID), bearerToken),
                    Void.class);
        } catch (Throwable e) {
            logger.error("refreshAccessToken", e);
            throw new AuthException("Unexpected server error. Please contact administrator");
        }
        if(response.isSuccessful()) {
            return true;
        }
        throw new AuthException(response.error().error);
    }

    @Override
    public List<GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails> getDetails(Client client) {
        return List.of(new AuthPasswordDetails(), new AuthTotpDetails("UNKNOWN", 6));
    }

    @Override
    public void close() {

    }

    public record JoinServerRequest(String username, UUID uuid, String accessToken, String serverID) {}

    public record CheckServerRequest(String username, String serverID) {}

    public record AuthorizeRequest(String login, String password, String totpCode) {}

    public record RefreshTokenRequest(String refreshToken) {}

    public record GetUserByAccessTokenRequest(String accessToken) {}

    public record MyHttpUserSession(String id, String accessToken, String refreshToken, int expire, MyHttpUser user) implements UserSession {

        @Override
        public String getID() {
            return id;
        }

        @Override
        public User getUser() {
            return user;
        }

        @Override
        public String getMinecraftAccessToken() {
            return accessToken;
        }

        @Override
        public long getExpireIn() {
            return expire;
        }

        public AuthManager.AuthReport toAuthReport() {
            return new AuthManager.AuthReport(accessToken, accessToken, refreshToken, expire, this);
        }
    }

    public record MyHttpUser(String username, UUID uuid, List<String> permissions, List<String> roles, Map<String, JsonTextureProvider.JsonTexture> assets) implements User, UserSupportTextures {

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public UUID getUUID() {
            return uuid;
        }

        @Override
        public ClientPermissions getPermissions() {
            return new ClientPermissions(roles, permissions);
        }

        @Override
        public Texture getSkinTexture() {
            JsonTextureProvider.JsonTexture texture = assets.get("SKIN");
            if(texture != null) {
                return texture.toTexture();
            }
            return null;
        }

        @Override
        public Texture getCloakTexture() {
            JsonTextureProvider.JsonTexture texture = assets.get("CAPE");
            if(texture != null) {
                return texture.toTexture();
            }
            return null;
        }

        @Override
        public Map<String, Texture> getUserAssets() {
            return JsonTextureProvider.JsonTexture.convertMap(assets);
        }
    }
}
